package com.vct.creditcard.db;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Creates the database tables from {@code db/schema.sql} if they do not exist yet. */
public final class SchemaInitializer {

    private static final String SCHEMA_RESOURCE = "/db/schema.sql";

    private SchemaInitializer() {
    }

    public static void createSchema(ConnectionProvider db) throws SQLException {
        try (Connection con = db.getConnection(); Statement statement = con.createStatement()) {
            for (String sql : splitStatements(readResource(SCHEMA_RESOURCE))) {
                statement.execute(sql);
            }
        }
    }

    static List<String> splitStatements(String script) {
        String withoutComments = script.lines()
                .filter(line -> !line.strip().startsWith("--"))
                .collect(Collectors.joining("\n"));
        return Arrays.stream(withoutComments.split(";"))
                .map(String::strip)
                .filter(sql -> !sql.isEmpty())
                .toList();
    }

    private static String readResource(String path) {
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
