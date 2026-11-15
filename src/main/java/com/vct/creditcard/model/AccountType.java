package com.vct.creditcard.model;

/** The two kinds of card holders. Merchants are modelled separately. */
public enum AccountType {
    INDIVIDUAL("Individual"),
    COMPANY("Company");

    private final String label;

    AccountType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
