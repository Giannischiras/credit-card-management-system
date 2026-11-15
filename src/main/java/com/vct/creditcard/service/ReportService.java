package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;
import com.vct.creditcard.model.AccountType;
import com.vct.creditcard.model.CustomerRanking;
import com.vct.creditcard.model.MerchantSales;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** The "best customers", "worst customers" and "merchant of the month" reports. */
public class ReportService {

    private static final String NET_AMOUNT =
            "COALESCE(SUM(CASE WHEN t.transaction_type = 'PURCHASE' THEN t.amount ELSE -t.amount END), 0)";

    private final ConnectionProvider db;

    public ReportService(ConnectionProvider db) {
        this.db = db;
    }

    /** Customers with no outstanding debt, ordered by how much they have spent. */
    public List<CustomerRanking> bestCustomers(int limit) throws SQLException {
        return rankings("WHERE a.debt <= 0 "
                + "GROUP BY a.account_number, a.name, a.account_type, a.debt, a.credit_limit "
                + "ORDER BY total_spent DESC, a.name LIMIT ?", limit);
    }

    /** Customers in debt, ordered by the share of their credit limit they have used. */
    public List<CustomerRanking> worstCustomers(int limit) throws SQLException {
        return rankings("WHERE a.debt > 0 "
                + "GROUP BY a.account_number, a.name, a.account_type, a.debt, a.credit_limit "
                + "ORDER BY a.debt / a.credit_limit DESC, a.name LIMIT ?", limit);
    }

    /** Net sales of every merchant that sold something in {@code month}, best first. */
    public List<MerchantSales> merchantSales(YearMonth month) throws SQLException {
        String sql = "SELECT m.name, " + NET_AMOUNT + " AS net_sales, COUNT(*) AS transactions "
                + "FROM card_transaction t JOIN merchant m ON m.account_number = t.merchant_account "
                + "WHERE t.transaction_date >= ? AND t.transaction_date < ? "
                + "GROUP BY m.account_number, m.name "
                + "ORDER BY net_sales DESC, m.name";
        List<MerchantSales> result = new ArrayList<>();
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(month.atDay(1)));
            ps.setDate(2, Date.valueOf(month.plusMonths(1).atDay(1)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new MerchantSales(rs.getString("name"), rs.getBigDecimal("net_sales"),
                            rs.getInt("transactions")));
                }
            }
        }
        return result;
    }

    private List<CustomerRanking> rankings(String filterAndOrder, int limit) throws SQLException {
        String sql = "SELECT a.name, a.account_type, a.debt, a.credit_limit, " + NET_AMOUNT + " AS total_spent "
                + "FROM card_account a LEFT JOIN card_transaction t ON t.account_number = a.account_number "
                + filterAndOrder;
        List<CustomerRanking> result = new ArrayList<>();
        try (Connection con = db.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new CustomerRanking(
                            rs.getString("name"),
                            AccountType.valueOf(rs.getString("account_type")),
                            rs.getBigDecimal("total_spent"),
                            rs.getBigDecimal("debt"),
                            rs.getBigDecimal("credit_limit")));
                }
            }
        }
        return result;
    }
}
