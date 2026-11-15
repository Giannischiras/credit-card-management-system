package com.vct.creditcard.model;

import java.math.BigDecimal;

/** A credit card account owned by an individual or a company. */
public record CardAccount(
        int accountNumber,
        String name,
        AccountType type,
        String expirationDate,
        BigDecimal creditLimit,
        BigDecimal debt) {

    public BigDecimal availableBalance() {
        return creditLimit.subtract(debt);
    }

    public boolean isCompany() {
        return type == AccountType.COMPANY;
    }
}
