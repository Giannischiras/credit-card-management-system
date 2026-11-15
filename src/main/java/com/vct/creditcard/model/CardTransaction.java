package com.vct.creditcard.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A purchase or refund. {@code employeeName} is {@code null} unless an employee used a company card. */
public record CardTransaction(
        long id,
        LocalDate date,
        String customerName,
        String merchantName,
        String employeeName,
        TransactionType type,
        BigDecimal amount) {
}
