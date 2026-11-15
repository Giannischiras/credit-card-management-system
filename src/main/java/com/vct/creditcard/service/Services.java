package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;

/** All services of the application, wired to one database. */
public record Services(
        AccountService accounts,
        MerchantService merchants,
        TransactionService transactions,
        ReportService reports) {

    public static Services create(ConnectionProvider db) {
        return new Services(
                new AccountService(db),
                new MerchantService(db),
                new TransactionService(db),
                new ReportService(db));
    }
}
