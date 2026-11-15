package com.vct.creditcard.service;

import com.vct.creditcard.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void parsesDotsAndCommas() throws Exception {
        assertEquals(new BigDecimal("99.90"), Money.parse("99.9"));
        assertEquals(new BigDecimal("99.90"), Money.parse(" 99,90 "));
    }

    @Test
    void rejectsInvalidAmounts() {
        assertThrows(BankingException.class, () -> Money.parse(""));
        assertThrows(BankingException.class, () -> Money.parse("abc"));
        assertThrows(BankingException.class, () -> Money.parse("-5"));
        assertThrows(BankingException.class, () -> Money.parse("0"));
    }
}
