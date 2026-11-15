package com.vct.creditcard.model;

import java.math.BigDecimal;

/**
 * A merchant that accepts VCT cards. For every sale the merchant owes the company
 * {@code commissionRate} of the amount; the rest is the merchant's profit.
 */
public record Merchant(
        int accountNumber,
        String name,
        BigDecimal commissionRate,
        BigDecimal totalProfit,
        BigDecimal debt) {
}
