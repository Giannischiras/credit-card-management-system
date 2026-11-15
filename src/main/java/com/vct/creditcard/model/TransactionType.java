package com.vct.creditcard.model;

public enum TransactionType {
    PURCHASE("Purchase"),
    REFUND("Refund");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
