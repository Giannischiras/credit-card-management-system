package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;
import com.vct.creditcard.model.Merchant;
import com.vct.creditcard.model.Money;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Registration, lookup, commission repayment and deletion of merchants. */
public class MerchantService {

    /** Share of every sale that a newly registered merchant owes to VCT. */
    public static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.0200");

    private static final String SELECT_MERCHANT =
            "SELECT account_number, name, commission_rate, total_profit, debt FROM merchant ";

    private final ConnectionProvider db;

    public MerchantService(ConnectionProvider db) {
        this.db = db;
    }

    public Optional<Merchant> find(String name) throws SQLException {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try (Connection con = db.getConnection()) {
            return find(con, name.strip(), false);
        }
    }

    public List<String> names() throws SQLException {
        List<String> names = new ArrayList<>();
        try (Connection con = db.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT name FROM merchant ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString("name"));
            }
        }
        return names;
    }

    public Merchant register(String name) throws BankingException, SQLException {
        return register(name, DEFAULT_COMMISSION_RATE);
    }

    public Merchant register(String name, BigDecimal commissionRate) throws BankingException, SQLException {
        String cleanName = Jdbc.requireName(name, "a name");
        if (commissionRate.signum() < 0 || commissionRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new BankingException("The commission rate must be between 0 and 1.");
        }
        return Jdbc.inTransaction(db, con -> {
            if (find(con, cleanName, false).isPresent()) {
                throw new BankingException("There is already a merchant named \"" + cleanName + "\".");
            }
            int accountNumber = Jdbc.newAccountNumber(con, "merchant");
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO merchant (account_number, name, commission_rate, total_profit, debt) VALUES (?, ?, ?, 0, 0)")) {
                ps.setInt(1, accountNumber);
                ps.setString(2, cleanName);
                ps.setBigDecimal(3, commissionRate);
                ps.executeUpdate();
            }
            BigDecimal zero = Money.round(BigDecimal.ZERO);
            return new Merchant(accountNumber, cleanName, commissionRate, zero, zero);
        });
    }

    /** Pays (part of) the commission the merchant owes to VCT and returns the updated merchant. */
    public Merchant payDebt(Merchant merchant, BigDecimal amount) throws BankingException, SQLException {
        Jdbc.requirePositive(amount);
        return Jdbc.inTransaction(db, con -> {
            Merchant current = lockExisting(con, merchant.name());
            if (amount.compareTo(current.debt()) > 0) {
                throw new BankingException("You do not owe that much. Your current debt is "
                        + Money.format(current.debt()) + ".");
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE merchant SET debt = debt - ? WHERE account_number = ?")) {
                ps.setBigDecimal(1, amount);
                ps.setInt(2, current.accountNumber());
                ps.executeUpdate();
            }
            return find(con, current.name(), false).orElseThrow();
        });
    }

    public void delete(Merchant merchant) throws BankingException, SQLException {
        Jdbc.inTransaction(db, con -> {
            Merchant current = lockExisting(con, merchant.name());
            if (current.debt().signum() != 0) {
                throw new BankingException("You cannot delete your account while you owe "
                        + Money.format(current.debt()) + " in commissions.");
            }
            try (PreparedStatement ps = con.prepareStatement("DELETE FROM merchant WHERE account_number = ?")) {
                ps.setInt(1, current.accountNumber());
                ps.executeUpdate();
            }
            return null;
        });
    }

    static Optional<Merchant> find(Connection con, String name, boolean forUpdate) throws SQLException {
        String sql = SELECT_MERCHANT + "WHERE name = ?" + (forUpdate ? " FOR UPDATE" : "");
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new Merchant(
                        rs.getInt("account_number"),
                        rs.getString("name"),
                        rs.getBigDecimal("commission_rate"),
                        rs.getBigDecimal("total_profit"),
                        rs.getBigDecimal("debt")));
            }
        }
    }

    static Merchant lockExisting(Connection con, String name) throws SQLException, BankingException {
        return find(con, name, true)
                .orElseThrow(() -> new BankingException("Unknown merchant \"" + name + "\"."));
    }
}
