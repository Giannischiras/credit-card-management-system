package com.vct.creditcard.model;

import java.math.BigDecimal;

/** A row of the best/worst customers reports. */
public record CustomerRanking(
        String name,
        AccountType type,
        BigDecimal totalSpent,
        BigDecimal debt,
        BigDecimal creditLimit) {
}
