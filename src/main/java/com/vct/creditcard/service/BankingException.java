package com.vct.creditcard.service;

/** A business rule was violated. The message is meant to be shown to the user. */
public class BankingException extends Exception {

    private static final long serialVersionUID = 1L;

    public BankingException(String message) {
        super(message);
    }
}
