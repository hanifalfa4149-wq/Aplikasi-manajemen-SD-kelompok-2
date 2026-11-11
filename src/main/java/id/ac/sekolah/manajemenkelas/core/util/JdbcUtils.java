package id.ac.sekolah.manajemenkelas.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;

/**
 * Helper JDBC untuk normalisasi string opsional/nullable, parameter nullable,
 * serta eksekusi transaksi atomik pada PostgreSQL.
 */
public final class JdbcUtils {

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T connection) throws SQLException;
    }

    private JdbcUtils() {
    }

    public static String normalizeNullableString(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    public static void setNullableString(PreparedStatement ps, int index, String value) throws SQLException {
        String normalized = normalizeNullableString(value);
        if (normalized == null) {
            ps.setNull(index, Types.VARCHAR);
        } else {
            ps.setString(index, normalized);
        }
    }

    public static <R> R inTransaction(SqlFunction<Connection, R> action) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                R result = action.apply(conn);
                conn.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(conn, e);
                if (e instanceof RuntimeException runtimeEx) {
                    throw runtimeEx;
                }
                throw new IllegalStateException("Transaksi database gagal dijalankan.", e);
            } finally {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException ignored) {
                    // Koneksi akan segera ditutup oleh try-with-resources.
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal membuka transaksi database.", e);
        }
    }

    public static void inTransactionVoid(SqlConsumer<Connection> action) {
        inTransaction(conn -> {
            action.accept(conn);
            return null;
        });
    }

    private static void rollbackQuietly(Connection conn, Exception originalException) {
        try {
            conn.rollback();
        } catch (SQLException rollbackEx) {
            originalException.addSuppressed(rollbackEx);
        }
    }
}

