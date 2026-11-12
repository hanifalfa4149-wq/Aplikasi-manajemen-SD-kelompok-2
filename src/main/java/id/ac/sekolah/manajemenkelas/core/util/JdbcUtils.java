package id.ac.sekolah.manajemenkelas.core.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;

/**
 * Helper JDBC untuk normalisasi teks opsional/nullable, parameter nullable,
 * serta pengelolaan transaksi database PostgreSQL.
 */
public final class JdbcUtils {

    @FunctionalInterface
    public interface SqlFunction<T, R> {
        R apply(T koneksi) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer<T> {
        void accept(T koneksi) throws SQLException;
    }

    private JdbcUtils() {
    }

    public static String normalizeNullableString(String nilai) {
        if (nilai == null) {
            return null;
        }
        String teksRapih = nilai.trim();
        return teksRapih.isEmpty() ? null : teksRapih;
    }

    public static String emptyIfNull(String nilai) {
        return nilai == null ? "" : nilai;
    }

    public static void setNullableString(PreparedStatement pernyataan, int indeks, String nilai) throws SQLException {
        String nilaiNormal = normalizeNullableString(nilai);
        if (nilaiNormal == null) {
            pernyataan.setNull(indeks, Types.VARCHAR);
        } else {
            pernyataan.setString(indeks, nilaiNormal);
        }
    }

    public static <R> R inTransaction(SqlFunction<Connection, R> aksiTransaksi) {
        try (Connection koneksi = DatabaseConfig.getConnection()) {
            boolean statusAutoCommitAwal = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try {
                R hasil = aksiTransaksi.apply(koneksi);
                koneksi.commit();
                return hasil;
            } catch (SQLException | RuntimeException e) {
                lakukanRollbackDenganAman(koneksi, e);
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new IllegalStateException("Transaksi database gagal dijalankan.", e);
            } finally {
                pulihkanAutoCommitDenganAman(koneksi, statusAutoCommitAwal);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal membuka transaksi database.", e);
        }
    }

    public static void inTransactionVoid(SqlConsumer<Connection> aksiTransaksi) {
        inTransaction(new SqlFunction<Connection, Void>() {
            @Override
            public Void apply(Connection koneksi) throws SQLException {
                aksiTransaksi.accept(koneksi);
                return null;
            }
        });
    }

    public static void lakukanRollbackDenganAman(Connection koneksi, Exception penyebabAwal) {
        if (koneksi == null) {
            return;
        }
        try {
            koneksi.rollback();
        } catch (SQLException exRollback) {
            if (penyebabAwal != null) {
                penyebabAwal.addSuppressed(exRollback);
            }
        }
    }

    public static void pulihkanAutoCommitDenganAman(Connection koneksi, boolean statusAutoCommitAwal) {
        if (koneksi == null) {
            return;
        }
        try {
            koneksi.setAutoCommit(statusAutoCommitAwal);
        } catch (SQLException diabaikan) {
            // Koneksi akan segera ditutup oleh try-with-resources.
        }
    }
}
