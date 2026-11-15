package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

/** Small JDBC helpers shared by the services. */
final class Jdbc {

    private static final Random RANDOM = new Random();
    private static final int MAX_ACCOUNT_NUMBER_ATTEMPTS = 1000;

    @FunctionalInterface
    interface Work<T> {
        T run(Connection con) throws SQLException, BankingException;
    }

    private Jdbc() {
    }

    /** Runs {@code work} in a single database transaction: everything is committed or nothing is. */
    static <T> T inTransaction(ConnectionProvider db, Work<T> work) throws SQLException, BankingException {
        try (Connection con = db.getConnection()) {
            con.setAutoCommit(false);
            try {
                T result = work.run(con);
                con.commit();
                return result;
            } catch (SQLException | BankingException | RuntimeException e) {
                con.rollback();
                throw e;
            }
        }
    }

    /** Picks a random, unused 4-digit account number in {@code table} (an internal table name, never user input). */
    static int newAccountNumber(Connection con, String table) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE account_number = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_ATTEMPTS; attempt++) {
                int candidate = 1000 + RANDOM.nextInt(9000);
                ps.setInt(1, candidate);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return candidate;
                    }
                }
            }
        }
        throw new SQLException("No free account numbers left in " + table);
    }

    static String requireName(String name, String what) throws BankingException {
        if (name == null || name.isBlank()) {
            throw new BankingException("Please enter " + what + ".");
        }
        return name.strip();
    }

    static void requirePositive(BigDecimal amount) throws BankingException {
        if (amount == null || amount.signum() <= 0) {
            throw new BankingException("The amount must be greater than zero.");
        }
    }
}
