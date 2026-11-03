package com.football.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Provides JDBC connections to the Football Tournament database. */
public final class DBConnection {
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/football_tournament_db"
            + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String URL = getSetting("football.db.url", "FOOTBALL_DB_URL", DEFAULT_URL);
    private static final String USERNAME = getSetting("football.db.username", "FOOTBALL_DB_USERNAME", "root");
    private static final String PASSWORD = getSetting("football.db.password", "FOOTBALL_DB_PASSWORD", "");

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection.isValid(3);
        } catch (SQLException exception) {
            return false;
        }
    }

    private static String getSetting(String systemProperty, String environmentVariable, String defaultValue) {
        String propertyValue = System.getProperty(systemProperty);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String environmentValue = System.getenv(environmentVariable);
        return environmentValue == null || environmentValue.isBlank() ? defaultValue : environmentValue;
    }
}
