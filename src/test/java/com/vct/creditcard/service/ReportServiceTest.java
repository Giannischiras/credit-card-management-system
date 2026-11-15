package com.vct.creditcard.service;

import com.vct.creditcard.db.DemoData;
import com.vct.creditcard.model.AccountType;
import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.CustomerRanking;
import com.vct.creditcard.model.MerchantSales;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportServiceTest extends DatabaseTest {

    @Test
    void rankingsAndMerchantOfTheMonth() throws Exception {
        CardAccount paysOnTime = services.accounts().register(AccountType.INDIVIDUAL, "Paid Up", money("1000"));
        CardAccount bigSpender = services.accounts().register(AccountType.INDIVIDUAL, "Big Spender", money("1000"));
        CardAccount maxedOut = services.accounts().register(AccountType.INDIVIDUAL, "Maxed Out", money("100"));
        services.merchants().register("Shop A");
        services.merchants().register("Shop B");
        LocalDate march = LocalDate.of(2026, 3, 10);

        services.transactions().purchase(paysOnTime, "Shop A", money("300"), march, null);
        services.accounts().payDebt(paysOnTime, money("300"));
        services.transactions().purchase(bigSpender, "Shop B", money("500"), march, null);
        services.transactions().purchase(maxedOut, "Shop A", money("95"), march.plusMonths(1), null);

        List<CustomerRanking> best = services.reports().bestCustomers(10);
        assertEquals("Paid Up", best.get(0).name());
        assertEquals(money("300"), best.get(0).totalSpent());

        List<CustomerRanking> worst = services.reports().worstCustomers(10);
        assertEquals("Maxed Out", worst.get(0).name());
        assertEquals("Big Spender", worst.get(1).name());

        List<MerchantSales> marchSales = services.reports().merchantSales(YearMonth.of(2026, 3));
        assertEquals("Shop B", marchSales.get(0).name());
        assertEquals(money("500"), marchSales.get(0).netSales());
        assertEquals(1, services.reports().merchantSales(YearMonth.of(2026, 4)).size());
        assertTrue(services.reports().merchantSales(YearMonth.of(2026, 5)).isEmpty());
    }

    @Test
    void demoDataIsConsistentAndOnlyLoadedOnce() throws Exception {
        assertTrue(DemoData.seedIfEmpty(db));
        assertFalse(DemoData.seedIfEmpty(db));

        CardAccount eleni = services.accounts().find(AccountType.INDIVIDUAL, "Eleni Kostaki").orElseThrow();
        assertEquals(money("2300"), eleni.debt());
        assertFalse(services.reports().bestCustomers(10).isEmpty());
        assertFalse(services.reports().worstCustomers(10).isEmpty());
    }
}
