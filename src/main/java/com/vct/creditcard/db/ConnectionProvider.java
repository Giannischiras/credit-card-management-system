package com.vct.creditcard.db;

import java.sql.Connection;
import java.sql.SQLException;

/** Supplies new JDBC connections. Callers are responsible for closing them. */
@FunctionalInterface
public interface ConnectionProvider {

    Connection getConnection() throws SQLException;
}
