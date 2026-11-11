package id.ac.sekolah.manajemenkelas.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Pengelola konfigurasi dan pembuatan koneksi JDBC PostgreSQL
 * berdasarkan application.properties.
 */
public final class DatabaseConfig {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (input == null) {
                throw new IllegalStateException("application.properties tidak ditemukan");
            }

            PROPERTIES.load(input);
            Class.forName(PROPERTIES.getProperty("db.driver", "org.postgresql.Driver").trim());
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String host = requireProperty("db.host");
        String port = requireProperty("db.port");
        String database = requireProperty("db.name");
        String username = requireProperty("db.username");
        String password = requireProperty("db.password");

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        Properties jdbcProps = new Properties();
        jdbcProps.setProperty("user", username);
        jdbcProps.setProperty("password", password);

        String connectTimeout = PROPERTIES.getProperty("db.connection.timeout", "30").trim();
        String socketTimeout = PROPERTIES.getProperty("db.socket.timeout", "30").trim();
        String appName = PROPERTIES.getProperty("db.application.name", "manajemen-kelas-sd").trim();

        if (!connectTimeout.isBlank()) {
            jdbcProps.setProperty("connectTimeout", connectTimeout);
        }
        if (!socketTimeout.isBlank()) {
            jdbcProps.setProperty("socketTimeout", socketTimeout);
        }
        if (!appName.isBlank()) {
            jdbcProps.setProperty("ApplicationName", appName);
        }

        Connection connection = DriverManager.getConnection(url, jdbcProps);
        try {
            applyConnectionSettings(connection);
            return connection;
        } catch (SQLException e) {
            try {
                connection.close();
            } catch (SQLException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    public static boolean isConnectionAvailable() {
        try (Connection conn = getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void applyConnectionSettings(Connection connection) throws SQLException {
        boolean autoCommit = Boolean.parseBoolean(
                PROPERTIES.getProperty("db.auto.commit", "true").trim());
        connection.setAutoCommit(autoCommit);

        String isolation = PROPERTIES.getProperty("db.transaction.isolation", "READ_COMMITTED")
                .trim()
                .toUpperCase();
        switch (isolation) {
            case "READ_UNCOMMITTED" -> connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            case "REPEATABLE_READ" -> connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            case "SERIALIZABLE" -> connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            default -> connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        }
    }

    private static String requireProperty(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Konfigurasi database '" + key + "' belum diisi.");
        }
        return value.trim();
    }
}
