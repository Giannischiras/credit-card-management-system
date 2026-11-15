package com.vct.creditcard.service;

import com.vct.creditcard.db.ConnectionProvider;
import com.vct.creditcard.db.SchemaInitializer;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.sql.DriverManager;
import java.util.UUID;

/** Gives every test a fresh in-memory H2 database running in MySQL compatibility mode. */
abstract class DatabaseTest {

    protected ConnectionProvider db;
    protected Services services;

    @BeforeEach
    void createDatabase() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        db = () -> DriverManager.getConnection(url, "sa", "");
        SchemaInitializer.createSchema(db);
        services = Services.create(db);
    }

    protected static BigDecimal money(String amount) {
        return new BigDecimal(amount).setScale(2);
    }
}
