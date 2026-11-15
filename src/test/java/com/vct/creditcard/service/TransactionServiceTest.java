package com.vct.creditcard.service;

import com.vct.creditcard.model.AccountType;
import com.vct.creditcard.model.CardAccount;
import com.vct.creditcard.model.CardTransaction;
import com.vct.creditcard.model.Merchant;
import com.vct.creditcard.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionServiceTest extends DatabaseTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 15);

    private CardAccount maria;
    private CardAccount acme;

    @BeforeEach
    void setUp() throws Exception {
        maria = services.accounts().register(AccountType.INDIVIDUAL, "Maria", money("1000"));
        acme = services.accounts().register(AccountType.COMPANY, "Acme", money("50000"));
        services.accounts().addEmployee(acme, 7, "Nikos");
        services.merchants().register("Book Corner", new BigDecimal("0.1500"));
    }

    @Test
    void purchaseChargesCustomerAndSplitsAmountForMerchant() throws Exception {
        services.transactions().purchase(maria, "Book Corner", money("200"), TODAY, null);

        CardAccount account = services.accounts().find(AccountType.INDIVIDUAL, "Maria").orElseThrow();
        Merchant merchant = services.merchants().find("Book Corner").orElseThrow();
        assertEquals(money("200"), account.debt());
        assertEquals(money("800"), account.availableBalance());
        assertEquals(money("170"), merchant.totalProfit());
        assertEquals(money("30"), merchant.debt());
    }

    @Test
    void purchaseOverTheAvailableCreditChangesNothing() throws Exception {
        assertThrows(BankingException.class,
                () -> services.transactions().purchase(maria, "Book Corner", money("1000.01"), TODAY, null));

        assertEquals(money("0"), services.accounts().find(AccountType.INDIVIDUAL, "Maria").orElseThrow().debt());
        assertEquals(money("0"), services.merchants().find("Book Corner").orElseThrow().totalProfit());
        assertEquals(List.of(), services.transactions().customerHistory(maria, null, null));
    }

    @Test
    void purchaseFromUnknownMerchantIsRejected() {
        assertThrows(BankingException.class,
                () -> services.transactions().purchase(maria, "Nowhere", money("10"), TODAY, null));
    }

    @Test
    void refundReversesAPurchase() throws Exception {
        services.transactions().purchase(maria, "Book Corner", money("200"), TODAY, null);

        services.transactions().refund(maria, "Book Corner", money("200"), TODAY);

        assertEquals(money("0"), services.accounts().find(AccountType.INDIVIDUAL, "Maria").orElseThrow().debt());
        Merchant merchant = services.merchants().find("Book Corner").orElseThrow();
        assertEquals(money("0"), merchant.totalProfit());
        assertEquals(money("0"), merchant.debt());
    }

    @Test
    void canNotRefundMoreThanWasBought() throws Exception {
        services.transactions().purchase(maria, "Book Corner", money("50"), TODAY, null);

        assertThrows(BankingException.class,
                () -> services.transactions().refund(maria, "Book Corner", money("60"), TODAY));
    }

    @Test
    void historyIsFilteredByDate() throws Exception {
        services.transactions().purchase(maria, "Book Corner", money("10"), TODAY.minusDays(10), null);
        services.transactions().purchase(maria, "Book Corner", money("20"), TODAY, null);

        List<CardTransaction> recent = services.transactions().customerHistory(maria, TODAY.minusDays(1), TODAY);

        assertEquals(1, recent.size());
        assertEquals(money("20"), recent.get(0).amount());
        assertEquals(TransactionType.PURCHASE, recent.get(0).type());
        assertNull(recent.get(0).employeeName());
        assertEquals(2, services.transactions().customerHistory(maria, null, null).size());
    }

    @Test
    void employeePurchasesAreRecordedForTheCompany() throws Exception {
        services.transactions().purchase(acme, "Book Corner", money("500"), TODAY, "Nikos");
        services.transactions().purchase(acme, "Book Corner", money("100"), TODAY, null);

        List<CardTransaction> byNikos = services.transactions().employeeHistory(acme, "Nikos");

        assertEquals(1, byNikos.size());
        assertEquals("Nikos", byNikos.get(0).employeeName());
        assertEquals(money("600"), services.accounts().find(AccountType.COMPANY, "Acme").orElseThrow().debt());
    }

    @Test
    void strangersCanNotUseTheCompanyCard() {
        assertThrows(BankingException.class,
                () -> services.transactions().purchase(acme, "Book Corner", money("10"), TODAY, "Somebody Else"));
    }

    @Test
    void individualsCanNotHaveEmployees() {
        assertThrows(BankingException.class,
                () -> services.transactions().purchase(maria, "Book Corner", money("10"), TODAY, "Nikos"));
    }

    @Test
    void merchantPaysCommission() throws Exception {
        services.transactions().purchase(maria, "Book Corner", money("200"), TODAY, null);
        Merchant merchant = services.merchants().find("Book Corner").orElseThrow();

        Merchant after = services.merchants().payDebt(merchant, money("30"));

        assertEquals(money("0"), after.debt());
        assertThrows(BankingException.class, () -> services.merchants().payDebt(after, money("1")));
    }
}
