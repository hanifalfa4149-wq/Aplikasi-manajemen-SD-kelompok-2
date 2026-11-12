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

    private static final Properties KONFIGURASI = new Properties();

    static {
        try (InputStream aliranInput = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

            if (aliranInput == null) {
                throw new IllegalStateException("application.properties tidak ditemukan");
            }

            KONFIGURASI.load(aliranInput);
            Class.forName(KONFIGURASI.getProperty("db.driver", "org.postgresql.Driver").trim());
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConfig() {
    }

    public static Connection getConnection() throws SQLException {
        String host = ambilPropertiWajib("db.host");
        String port = ambilPropertiWajib("db.port");
        String namaDatabase = ambilPropertiWajib("db.name");
        String namaPengguna = ambilPropertiWajib("db.username");
        String kataSandi = ambilPropertiWajib("db.password");

        String urlJdbc = "jdbc:postgresql://" + host + ":" + port + "/" + namaDatabase;

        Properties propertiJdbc = new Properties();
        propertiJdbc.setProperty("user", namaPengguna);
        propertiJdbc.setProperty("password", kataSandi);

        String batasWaktuKoneksi = KONFIGURASI.getProperty("db.connection.timeout", "30").trim();
        String batasWaktuSoket = KONFIGURASI.getProperty("db.socket.timeout", "30").trim();
        String namaAplikasi = KONFIGURASI.getProperty("db.application.name", "manajemen-kelas-sd").trim();

        if (!batasWaktuKoneksi.isBlank()) {
            propertiJdbc.setProperty("connectTimeout", batasWaktuKoneksi);
        }
        if (!batasWaktuSoket.isBlank()) {
            propertiJdbc.setProperty("socketTimeout", batasWaktuSoket);
        }
        if (!namaAplikasi.isBlank()) {
            propertiJdbc.setProperty("ApplicationName", namaAplikasi);
        }

        Connection koneksi = DriverManager.getConnection(urlJdbc, propertiJdbc);
        try {
            terapkanPengaturanKoneksi(koneksi);
            return koneksi;
        } catch (SQLException e) {
            try {
                koneksi.close();
            } catch (SQLException exTambahan) {
                e.addSuppressed(exTambahan);
            }
            throw e;
        }
    }

    public static boolean isConnectionAvailable() {
        try (Connection koneksi = getConnection()) {
            return koneksi.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void terapkanPengaturanKoneksi(Connection koneksi) throws SQLException {
        boolean statusAutoCommit = Boolean.parseBoolean(
                KONFIGURASI.getProperty("db.auto.commit", "true").trim());
        koneksi.setAutoCommit(statusAutoCommit);

        String tingkatIsolasi = KONFIGURASI.getProperty("db.transaction.isolation", "READ_COMMITTED")
                .trim()
                .toUpperCase();
        switch (tingkatIsolasi) {
            case "READ_UNCOMMITTED":
                koneksi.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
                break;
            case "REPEATABLE_READ":
                koneksi.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                break;
            case "SERIALIZABLE":
                koneksi.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
                break;
            default:
                koneksi.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                break;
        }
    }

    private static String ambilPropertiWajib(String kunci) {
        String nilai = KONFIGURASI.getProperty(kunci);
        if (nilai == null || nilai.isBlank()) {
            throw new IllegalStateException("Konfigurasi database '" + kunci + "' belum diisi.");
        }
        return nilai.trim();
    }
}
