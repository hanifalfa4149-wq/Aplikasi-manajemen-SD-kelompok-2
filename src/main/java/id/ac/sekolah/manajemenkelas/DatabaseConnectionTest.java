package id.ac.sekolah.manajemenkelas;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class DatabaseConnectionTest {

    public static void main(String[] args) {

        try (Connection connection = DatabaseConfig.getConnection()) {

            DatabaseMetaData metadata = connection.getMetaData();

            System.out.println("DATABASE CONNECTION SUCCESS!");
            System.out.println("Database: "
                    + metadata.getDatabaseProductName());
            System.out.println("Version: "
                    + metadata.getDatabaseProductVersion());
            System.out.println("URL: "
                    + metadata.getURL());

        } catch (SQLException e) {
            System.err.println("DATABASE CONNECTION FAILED!");
            e.printStackTrace();
        }
    }
}