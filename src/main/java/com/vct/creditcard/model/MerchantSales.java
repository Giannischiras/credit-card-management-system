package com.vct.creditcard.model;

import java.math.BigDecimal;

/** Net sales (purchases minus refunds) of a merchant in some period. */
public record MerchantSales(String name, BigDecimal netSales, int transactions) {
}
