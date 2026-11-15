package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;
import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.CardTransaction;
import com.vct.creditcard.model.Merchant;
import com.vct.creditcard.model.Money;
import com.vct.creditcard.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Purchases, refunds and transaction history.
 *
 * <p>Every purchase or refund touches three rows (the card account, the merchant and the
 * transaction log), so each one runs inside a single database transaction with row locks.
 */
public class TransactionService {

    /** Bounds of the MySQL DATE type, used when the user leaves a date filter empty. */
    private static final LocalDate EARLIEST = LocalDate.of(1000, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

    private static final String SELECT_TRANSACTION =
            "SELECT t.id, t.transaction_date, a.name AS customer, m.name AS merchant, e.name AS employee, "
                    + "t.transaction_type, t.amount "
                    + "FROM card_transaction t "
                    + "JOIN card_account a ON a.account_number = t.account_number "
                    + "JOIN merchant m ON m.account_number = t.merchant_account "
                    + "LEFT JOIN employee e ON e.employee_id = t.employee_id ";
    private static final String ORDER = " ORDER BY t.transaction_date, t.id";

    private final ConnectionProvider db;

    public TransactionService(ConnectionProvider db) {
        this.db = db;
    }

    /**
     * Charges {@code amount} to the customer's card. For company cards an employee name can be given;
     * that employee must belong to the company.
     */
    public CardTransaction purchase(CardAccount customer, String merchantName, BigDecimal amount,
                                    LocalDate date, String employeeName) throws BankingException, SQLException {
        Jdbc.requirePositive(amount);
        requireDate(date);
        String merchantKey = Jdbc.requireName(merchantName, "a merchant");
        return Jdbc.inTransaction(db, con -> {
            CardAccount account = AccountService.lockExisting(con, customer);
            Merchant merchant = MerchantService.lockExisting(con, merchantKey);
            Integer employeeId = resolveEmployee(con, account, employeeName);
            if (amount.compareTo(account.availableBalance()) > 0) {
                throw new BankingException("Your available credit balance ("
                        + Money.format(account.availableBalance()) + ") is not enough for this purchase.");
            }
            BigDecimal commission = Money.round(amount.multiply(merchant.commissionRate()));
            updateAccountDebt(con, account, amount);
            updateMerchant(con, merchant, amount.subtract(commission), commission);
            long id = insert(con, account, merchant, employeeId, date, amount, TransactionType.PURCHASE);
            return new CardTransaction(id, date, account.name(), merchant.name(),
                    employeeId == null ? null : employeeName.strip(), TransactionType.PURCHASE, amount);
        });
    }

    /**
     * Returns a product. The refund cannot exceed what the customer has (net) spent at that merchant,
     * otherwise anyone could create credit out of nothing.
     */
    public CardTransaction refund(CardAccount customer, String merchantName, BigDecimal amount, LocalDate date)
            throws BankingException, SQLException {
        Jdbc.requirePositive(amount);
        requireDate(date);
        String merchantKey = Jdbc.requireName(merchantName, "a merchant");
        return Jdbc.inTransaction(db, con -> {
            CardAccount account = AccountService.lockExisting(con, customer);
            Merchant merchant = MerchantService.lockExisting(con, merchantKey);
            BigDecimal refundable = netSpent(con, account, merchant);
            if (amount.compareTo(refundable) > 0) {
                throw new BankingException("You can return at most " + Money.format(refundable)
                        + " to " + merchant.name() + ".");
            }
            BigDecimal commission = Money.round(amount.multiply(merchant.commissionRate()));
            updateAccountDebt(con, account, amount.negate());
            updateMerchant(con, merchant, amount.subtract(commission).negate(), commission.negate());
            long id = insert(con, account, merchant, null, date, amount, TransactionType.REFUND);
            return new CardTransaction(id, date, account.name(), merchant.name(), null, TransactionType.REFUND, amount);
        });
    }

    /** Transactions of a card account between two dates (inclusive); {@code null} means no bound. */
    public List<CardTransaction> customerHistory(CardAccount account, LocalDate from, LocalDate to)
            throws SQLException {
        return query(SELECT_TRANSACTION + "WHERE t.account_number = ? AND t.transaction_date BETWEEN ? AND ?" + ORDER,
                account.accountNumber(), from, to);
    }

    /** Sales and refunds of a merchant between two dates (inclusive); {@code null} means no bound. */
    public List<CardTransaction> merchantHistory(Merchant merchant, LocalDate from, LocalDate to)
            throws SQLException {
        return query(SELECT_TRANSACTION + "WHERE t.merchant_account = ? AND t.transaction_date BETWEEN ? AND ?" + ORDER,
                merchant.accountNumber(), from, to);
    }

    /** Purchases made with a company's card by one of its employees. */
    public List<CardTransaction> employeeHistory(CardAccount company, String employeeName) throws SQLException {
        List<CardTransaction> result = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     SELECT_TRANSACTION + "WHERE t.account_number = ? AND e.name = ?" + ORDER)) {
            ps.setInt(1, company.accountNumber());
            ps.setString(2, employeeName == null ? "" : employeeName.strip());
            readTransactions(ps, result);
        }
        return result;
    }

    private List<CardTransaction> query(String sql, int accountNumber, LocalDate from, LocalDate to)
            throws SQLException {
        List<CardTransaction> result = new ArrayList<>();
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accountNumber);
            ps.setDate(2, Date.valueOf(from == null ? EARLIEST : from));
            ps.setDate(3, Date.valueOf(to == null ? LATEST : to));
            readTransactions(ps, result);
        }
        return result;
    }

    private static void readTransactions(PreparedStatement ps, List<CardTransaction> into) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                into.add(new CardTransaction(
                        rs.getLong("id"),
                        rs.getDate("transaction_date").toLocalDate(),
                        rs.getString("customer"),
                        rs.getString("merchant"),
                        rs.getString("employee"),
                        TransactionType.valueOf(rs.getString("transaction_type")),
                        rs.getBigDecimal("amount")));
            }
        }
    }

    private static Integer resolveEmployee(Connection con, CardAccount account, String employeeName)
            throws SQLException, BankingException {
        if (employeeName == null || employeeName.isBlank()) {
            return null;
        }
        if (!account.isCompany()) {
            throw new BankingException("Only company cards can be used by employees.");
        }
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT employee_id FROM employee WHERE company_account = ? AND name = ?")) {
            ps.setInt(1, account.accountNumber());
            ps.setString(2, employeeName.strip());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new BankingException(employeeName.strip() + " is not an employee of " + account.name() + ".");
                }
                return rs.getInt("employee_id");
            }
        }
    }

    private static BigDecimal netSpent(Connection con, CardAccount account, Merchant merchant) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT COALESCE(SUM(CASE WHEN transaction_type = 'PURCHASE' THEN amount ELSE -amount END), 0) AS net "
                        + "FROM card_transaction WHERE account_number = ? AND merchant_account = ?")) {
            ps.setInt(1, account.accountNumber());
            ps.setInt(2, merchant.accountNumber());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("net");
            }
        }
    }

    private static void updateAccountDebt(Connection con, CardAccount account, BigDecimal change) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE card_account SET debt = debt + ? WHERE account_number = ?")) {
            ps.setBigDecimal(1, change);
            ps.setInt(2, account.accountNumber());
            ps.executeUpdate();
        }
    }

    private static void updateMerchant(Connection con, Merchant merchant, BigDecimal profitChange,
                                       BigDecimal debtChange) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "UPDATE merchant SET total_profit = total_profit + ?, debt = debt + ? WHERE account_number = ?")) {
            ps.setBigDecimal(1, profitChange);
            ps.setBigDecimal(2, debtChange);
            ps.setInt(3, merchant.accountNumber());
            ps.executeUpdate();
        }
    }

    private static long insert(Connection con, CardAccount account, Merchant merchant, Integer employeeId,
                               LocalDate date, BigDecimal amount, TransactionType type) throws SQLException {
        String sql = "INSERT INTO card_transaction "
                + "(account_number, merchant_account, employee_id, transaction_date, amount, transaction_type) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, account.accountNumber());
            ps.setInt(2, merchant.accountNumber());
            if (employeeId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, employeeId);
            }
            ps.setDate(4, Date.valueOf(date));
            ps.setBigDecimal(5, amount);
            ps.setString(6, type.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        }
    }

    private static void requireDate(LocalDate date) throws BankingException {
        if (date == null) {
            throw new BankingException("Please enter the date of the transaction.");
        }
    }
}
