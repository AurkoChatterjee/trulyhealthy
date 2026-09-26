package com.trulyhealthy.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Loads db.properties (from src/main/resources, packaged into the jar) and
 * opens a fresh JDBC connection per call. A pool would be nicer, but a plain
 * DriverManager connection keeps this project dependency-free and easy to
 * explain in a viva.
 */
public final class DBConfig {

    private static final Properties PROPS = new Properties();
    public static final int SERVER_PORT;

    static {
        try (InputStream in = DBConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on classpath");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        }
        SERVER_PORT = Integer.parseInt(PROPS.getProperty("server.port", "8080"));
    }

    private DBConfig() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user"),
                PROPS.getProperty("db.password")
        );
    }
}
