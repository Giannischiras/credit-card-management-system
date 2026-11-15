package com.vct.creditcard.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * MySQL connection settings. Values are read from environment variables so that
 * credentials never live in the source code:
 *
 * <ul>
 *   <li>{@code CCC_DB_HOST} (default {@code localhost})</li>
 *   <li>{@code CCC_DB_PORT} (default {@code 3306})</li>
 *   <li>{@code CCC_DB_NAME} (default {@code credit_card_company})</li>
 *   <li>{@code CCC_DB_USER} (default {@code root})</li>
 *   <li>{@code CCC_DB_PASSWORD} (default empty)</li>
 * </ul>
 */
public final class Database implements ConnectionProvider {

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database fromEnvironment() {
        String host = env("CCC_DB_HOST", "localhost");
        String port = env("CCC_DB_PORT", "3306");
        String name = env("CCC_DB_NAME", "credit_card_company");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + name
                + "?createDatabaseIfNotExist=true&characterEncoding=UTF-8";
        return new Database(url, env("CCC_DB_USER", "root"), env("CCC_DB_PASSWORD", ""));
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public String url() {
        return url;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
