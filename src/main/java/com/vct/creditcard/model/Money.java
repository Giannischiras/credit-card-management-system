package com.vct.creditcard.model;

import com.vct.creditcard.service.BankingException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Helpers for amounts of money, which are always handled as {@link BigDecimal} with two decimals. */
public final class Money {

    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(Locale.GERMANY);

    private Money() {
    }

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /** Parses user input such as {@code "120"}, {@code "99.90"} or {@code "99,90"} into a positive amount. */
    public static BigDecimal parse(String text) throws BankingException {
        if (text == null || text.isBlank()) {
            throw new BankingException("Please enter an amount.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(text.strip().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new BankingException("\"" + text.strip() + "\" is not a valid amount.");
        }
        if (amount.signum() <= 0) {
            throw new BankingException("The amount must be greater than zero.");
        }
        return round(amount);
    }

    public static String format(BigDecimal amount) {
        synchronized (FORMAT) {
            return FORMAT.format(amount);
        }
    }
}
