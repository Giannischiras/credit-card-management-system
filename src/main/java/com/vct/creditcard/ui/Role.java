package com.vct.creditcard.ui;

import com.vct.creditcard.model.AccountType;

/** Who is using the application. */
enum Role {
    INDIVIDUAL("Individual", AccountType.INDIVIDUAL),
    COMPANY("Company", AccountType.COMPANY),
    MERCHANT("Merchant", null);

    private final String label;
    private final AccountType accountType;

    Role(String label, AccountType accountType) {
        this.label = label;
        this.accountType = accountType;
    }

    String label() {
        return label;
    }

    /** The card account type, or {@code null} for merchants. */
    AccountType accountType() {
        return accountType;
    }

    String namePrompt() {
        return this == COMPANY ? "Name of your company:" : "Your name:";
    }
}
