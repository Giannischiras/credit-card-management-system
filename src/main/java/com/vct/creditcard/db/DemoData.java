package com.vct.creditcard.db;

import com.vct.creditcard.model.AccountType;
import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.service.BankingException;
import com.vct.creditcard.service.Services;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

/**
 * Fills an empty database with fictional customers, merchants and transactions so the
 * application has something to show on first start. All data goes through the normal
 * services, so balances are always consistent with the transaction log.
 */
public final class DemoData {

    private DemoData() {
    }

    public static boolean seedIfEmpty(ConnectionProvider db) throws SQLException, BankingException {
        try (Connection con = db.getConnection();
             Statement statement = con.createStatement();
             ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM card_account")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return false;
            }
        }
        seed(Services.create(db));
        return true;
    }

    private static void seed(Services s) throws SQLException, BankingException {
        CardAccount maria = s.accounts().register(AccountType.INDIVIDUAL, "Maria Papadopoulou", money("1000"));
        CardAccount nikos = s.accounts().register(AccountType.INDIVIDUAL, "Nikos Georgiou", money("5000"));
        CardAccount eleni = s.accounts().register(AccountType.INDIVIDUAL, "Eleni Kostaki", money("10000"));
        CardAccount dimitris = s.accounts().register(AccountType.INDIVIDUAL, "Dimitris Alexiou", money("3000"));
        CardAccount sofia = s.accounts().register(AccountType.INDIVIDUAL, "Sofia Markaki", money("2000"));
        CardAccount kostas = s.accounts().register(AccountType.INDIVIDUAL, "Kostas Antoniou", money("20000"));

        s.accounts().register(AccountType.COMPANY, "Aegean Foods SA", money("100000"));
        CardAccount cretanTech = s.accounts().register(AccountType.COMPANY, "Cretan Tech Ltd", money("2000000"));
        CardAccount olympus = s.accounts().register(AccountType.COMPANY, "Olympus Logistics", money("1000000"));
        CardAccount minoan = s.accounts().register(AccountType.COMPANY, "Minoan Travel", money("40000"));

        s.merchants().register("Book Corner", new BigDecimal("0.1500"));
        s.merchants().register("Harbour Electronics", new BigDecimal("0.2000"));
        s.merchants().register("Sea Breeze Cafe", new BigDecimal("0.1000"));

        s.accounts().addEmployee(cretanTech, 23, "Markos Ioannou");
        s.accounts().addEmployee(cretanTech, 765, "Ioanna Papapetrou");
        s.accounts().addEmployee(cretanTech, 398, "Lefteris Tzanakis");
        s.accounts().addEmployee(minoan, 143, "Michalis Antoniou");
        s.accounts().addEmployee(minoan, 432, "Eleni Kalogeraki");
        s.accounts().addEmployee(olympus, 213, "Lampros Pitarakis");
        s.accounts().addEmployee(olympus, 267, "Angeliki Markou");

        LocalDate today = LocalDate.now();
        s.transactions().purchase(nikos, "Book Corner", money("500"), today.minusDays(20), null);
        s.transactions().purchase(eleni, "Harbour Electronics", money("1500"), today.minusDays(15), null);
        s.transactions().refund(eleni, "Harbour Electronics", money("200"), today.minusDays(14));
        s.transactions().purchase(eleni, "Sea Breeze Cafe", money("1000"), today.minusDays(3), null);
        s.transactions().purchase(sofia, "Harbour Electronics", money("1998"), today.minusDays(10), null);
        s.transactions().purchase(maria, "Book Corner", money("120"), today.minusDays(12), null);
        s.transactions().purchase(dimitris, "Sea Breeze Cafe", money("80"), today.minusDays(5), null);
        s.transactions().purchase(kostas, "Harbour Electronics", money("2400"), today.minusDays(25), null);
        s.transactions().purchase(cretanTech, "Harbour Electronics", money("30000"), today.minusDays(8), "Markos Ioannou");
        s.transactions().purchase(cretanTech, "Book Corner", money("20000"), today.minusDays(6), "Ioanna Papapetrou");
        s.transactions().purchase(olympus, "Harbour Electronics", money("60500"), today.minusDays(4), "Lampros Pitarakis");
        s.transactions().purchase(minoan, "Sea Breeze Cafe", money("350"), today.minusDays(2), "Eleni Kalogeraki");

        s.accounts().payDebt(maria, money("120"));
        s.accounts().payDebt(dimitris, money("80"));
        s.accounts().payDebt(kostas, money("2400"));
        s.accounts().payDebt(minoan, money("350"));
    }

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2);
    }
}
