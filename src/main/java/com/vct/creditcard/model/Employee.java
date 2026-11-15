package com.vct.creditcard.model;

/** An employee who may make purchases with their company's card. */
public record Employee(int id, String name, int companyAccount) {
}
