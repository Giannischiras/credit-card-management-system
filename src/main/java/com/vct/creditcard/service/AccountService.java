package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;
import com.vct.creditcard.model.AccountType;
import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.Employee;
import com.vct.creditcard.model.Money;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Registration, lookup, debt repayment and deletion of individual and company card accounts. */
public class AccountService {

    static final int CARD_VALIDITY_YEARS = 4;
    private static final DateTimeFormatter EXPIRATION_FORMAT = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final String SELECT_ACCOUNT =
            "SELECT account_number, name, account_type, expiration_date, credit_limit, debt FROM card_account ";

    private final ConnectionProvider db;

    public AccountService(ConnectionProvider db) {
        this.db = db;
    }

    public Optional<CardAccount> find(AccountType type, String name) throws SQLException {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try (Connection con = db.getConnection()) {
            return find(con, type, name.strip(), false);
        }
    }

    public CardAccount register(AccountType type, String name, BigDecimal creditLimit)
            throws BankingException, SQLException {
        String cleanName = Jdbc.requireName(name, "a name");
        if (creditLimit == null || creditLimit.signum() <= 0) {
            throw new BankingException("The credit limit must be greater than zero.");
        }
        BigDecimal limit = Money.round(creditLimit);
        return Jdbc.inTransaction(db, con -> {
            if (find(con, type, cleanName, false).isPresent()) {
                throw new BankingException("There is already a " + type.label().toLowerCase()
                        + " account named \"" + cleanName + "\".");
            }
            int accountNumber = Jdbc.newAccountNumber(con, "card_account");
            String expiration = YearMonth.now().plusYears(CARD_VALIDITY_YEARS).format(EXPIRATION_FORMAT);
            String sql = "INSERT INTO card_account (account_number, name, account_type, expiration_date, credit_limit, debt) "
                    + "VALUES (?, ?, ?, ?, ?, 0)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, accountNumber);
                ps.setString(2, cleanName);
                ps.setString(3, type.name());
                ps.setString(4, expiration);
                ps.setBigDecimal(5, limit);
                ps.executeUpdate();
            }
            return new CardAccount(accountNumber, cleanName, type, expiration, limit, Money.round(BigDecimal.ZERO));
        });
    }

    /** Repays part or all of the debt and returns the updated account. */
    public CardAccount payDebt(CardAccount account, BigDecimal amount) throws BankingException, SQLException {
        Jdbc.requirePositive(amount);
        return Jdbc.inTransaction(db, con -> {
            CardAccount current = lockExisting(con, account);
            if (amount.compareTo(current.debt()) > 0) {
                throw new BankingException("You do not owe that much. Your current debt is "
                        + Money.format(current.debt()) + ".");
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE card_account SET debt = debt - ? WHERE account_number = ?")) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, current.accountNumber());
                ps.executeUpdate();
            }
            return find(con, current.type(), current.name(), false).orElseThrow();
        });
    }

    /** Deletes the account. Only allowed when the balance is exactly zero. */
    public void delete(CardAccount account) throws BankingException, SQLException {
        Jdbc.inTransaction(db, con -> {
            CardAccount current = lockExisting(con, account);
            if (current.debt().signum() != 0) {
                throw new BankingException("You cannot delete your account while your balance is "
                        + Money.format(current.debt()) + ". Please settle it first.");
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM card_account WHERE account_number = ?")) {
                ps.setInt(1, current.accountNumber());
                ps.executeUpdate();
            }
            return null;
        });
    }

    public Employee addEmployee(CardAccount company, int employeeId, String name)
            throws BankingException, SQLException {
        if (!company.isCompany()) {
            throw new BankingException("Only company accounts can have employees.");
        }
        String cleanName = Jdbc.requireName(name, "the employee's name");
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "INSERT INTO employee (employee_id, name, company_account) VALUES (?, ?, ?)")) {
            ps.setInt(1, employeeId);
            ps.setString(2, cleanName);
            ps.setInt(3, company.accountNumber());
            ps.executeUpdate();
        }
        return new Employee(employeeId, cleanName, company.accountNumber());
    }

    public List<Employee> employees(CardAccount company) throws SQLException {
        List<Employee> result = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT employee_id, name, company_account FROM employee WHERE company_account = ? ORDER BY name")) {
            ps.setInt(1, company.accountNumber());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Employee(rs.getInt("employee_id"), rs.getString("name"), rs.getInt("company_account")));
                }
            }
        }
        return result;
    }

    static Optional<CardAccount> find(Connection con, AccountType type, String name, boolean forUpdate)
            throws SQLException {
        String sql = SELECT_ACCOUNT + "WHERE account_type = ? AND name = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, type.name());
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    static CardAccount lockExisting(Connection con, CardAccount account) throws SQLException, BankingException {
        return find(con, account.type(), account.name(), true)
                .orElseThrow(() -> new BankingException("The account of " + account.name() + " no longer exists."));
    }

    private static CardAccount map(ResultSet rs) throws SQLException {
        return new CardAccount(
                rs.getInt("account_number"),
                rs.getString("name"),
                AccountType.valueOf(rs.getString("account_type")),
                rs.getString("expiration_date"),
                rs.getBigDecimal("credit_limit"),
                rs.getBigDecimal("debt"));
    }
}
