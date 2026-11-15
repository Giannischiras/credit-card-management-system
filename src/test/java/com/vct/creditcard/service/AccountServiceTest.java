package com.vct.creditcard.service;

import com.vct.creditcard.model.AccountType;
import com.vct.creditcard.model.CardAccount;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountServiceTest extends DatabaseTest {

    @Test
    void registeredAccountCanBeFound() throws Exception {
        CardAccount created = services.accounts().register(AccountType.INDIVIDUAL, "  Maria  ", money("1000"));

        CardAccount found = services.accounts().find(AccountType.INDIVIDUAL, "Maria").orElseThrow();

        assertEquals(created.accountNumber(), found.accountNumber());
        assertEquals("Maria", found.name());
        assertEquals(money("1000"), found.availableBalance());
        assertTrue(found.expirationDate().matches("\\d{2}/\\d{4}"));
    }

    @Test
    void sameNameCanNotRegisterTwice() throws Exception {
        services.accounts().register(AccountType.COMPANY, "Acme", money("5000"));

        assertThrows(BankingException.class,
                () -> services.accounts().register(AccountType.COMPANY, "Acme", money("100")));
    }

    @Test
    void creditLimitMustBePositive() {
        assertThrows(BankingException.class,
                () -> services.accounts().register(AccountType.INDIVIDUAL, "Nikos", money("0")));
    }

    @Test
    void namesAreNeverConcatenatedIntoSql() throws Exception {
        services.accounts().register(AccountType.INDIVIDUAL, "Maria", money("1000"));

        assertFalse(services.accounts().find(AccountType.INDIVIDUAL, "x' OR '1'='1").isPresent());
    }

    @Test
    void payingDebtLowersIt() throws Exception {
        CardAccount maria = services.accounts().register(AccountType.INDIVIDUAL, "Maria", money("1000"));
        services.merchants().register("Shop");
        services.transactions().purchase(maria, "Shop", money("300"), LocalDate.now(), null);

        CardAccount after = services.accounts().payDebt(maria, money("100"));

        assertEquals(money("200"), after.debt());
        assertEquals(money("800"), after.availableBalance());
    }

    @Test
    void canNotPayMoreThanTheDebt() throws Exception {
        CardAccount maria = services.accounts().register(AccountType.INDIVIDUAL, "Maria", money("1000"));

        assertThrows(BankingException.class, () -> services.accounts().payDebt(maria, money("1")));
    }

    @Test
    void accountWithDebtCanNotBeDeleted() throws Exception {
        CardAccount maria = services.accounts().register(AccountType.INDIVIDUAL, "Maria", money("1000"));
        services.merchants().register("Shop");
        services.transactions().purchase(maria, "Shop", money("50"), LocalDate.now(), null);

        assertThrows(BankingException.class, () -> services.accounts().delete(maria));

        services.accounts().payDebt(maria, money("50"));
        services.accounts().delete(maria);
        assertFalse(services.accounts().find(AccountType.INDIVIDUAL, "Maria").isPresent());
    }
}
