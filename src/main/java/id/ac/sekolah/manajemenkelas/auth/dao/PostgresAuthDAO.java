package id.ac.sekolah.manajemenkelas.auth.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;

/**
 * Implementasi JDBC PostgreSQL untuk AuthDAO yang mengakses tabel pengguna.
 */
public class PostgresAuthDAO implements AuthDAO {

    private static final String SQL_FIND_BY_USERNAME = """
            SELECT id, id_guru, nama_pengguna, hash_kata_sandi, salt_kata_sandi,
                   nama_lengkap, peran, aktif, terakhir_login
            FROM pengguna
            WHERE LOWER(nama_pengguna) = LOWER(?)
            """;

    private static final String SQL_EXISTS_BY_USERNAME = """
            SELECT 1
            FROM pengguna
            WHERE LOWER(nama_pengguna) = LOWER(?)
            """;

    private static final String SQL_INSERT_USER = """
            INSERT INTO pengguna (
                id_guru,
                nama_pengguna,
                hash_kata_sandi,
                salt_kata_sandi,
                nama_lengkap,
                peran,
                aktif,
                terakhir_login
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_LAST_LOGIN = """
            UPDATE pengguna
            SET terakhir_login = CURRENT_TIMESTAMP,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_USERNAME)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRowToUser(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari pengguna berdasarkan username.", e);
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_USERNAME)) {

            ps.setString(1, username.trim());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal memeriksa keberadaan username pengguna.", e);
        }
    }

    @Override
    public void save(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Data pengguna tidak boleh null.");
        }
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new IllegalArgumentException("Username wajib diisi.");
        }
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new IllegalArgumentException("Hash kata sandi wajib diisi.");
        }
        if (user.getPasswordSalt() == null || user.getPasswordSalt().isBlank()) {
            throw new IllegalArgumentException("Salt kata sandi wajib diisi.");
        }
        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new IllegalArgumentException("Nama lengkap pengguna wajib diisi.");
        }

        String dbRole = RoleMapper.toDatabaseRole(user.getRole());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_USER)) {

            if (user.getTeacherId() != null) {
                ps.setLong(1, user.getTeacherId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, user.getUsername().trim());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPasswordSalt());
            ps.setString(5, user.getFullName().trim());
            ps.setString(6, dbRole);
            ps.setBoolean(7, user.isActive());

            if (user.getLastLogin() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(user.getLastLogin()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateAuthSqlException("Gagal menyimpan akun pengguna ke database.", e);
        }
    }

    @Override
    public void updateLastLogin(Long userId) {
        if (userId == null) {
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_LAST_LOGIN)) {

            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal memperbarui waktu login terakhir.", e);
        }
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        long rawTeacherId = rs.getLong("id_guru");
        Long teacherId = rs.wasNull() ? null : rawTeacherId;
        String username = rs.getString("nama_pengguna");
        String passwordHash = rs.getString("hash_kata_sandi");
        String passwordSalt = rs.getString("salt_kata_sandi");
        String fullName = rs.getString("nama_lengkap");
        String appRole = RoleMapper.fromDatabaseRole(rs.getString("peran"));
        boolean active = rs.getBoolean("aktif");
        Timestamp lastLoginTs = rs.getTimestamp("terakhir_login");
        LocalDateTime lastLogin = lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null;

        return new User(
                id,
                teacherId,
                username,
                passwordHash,
                passwordSalt,
                fullName,
                appRole,
                active,
                lastLogin);
    }

    private static RuntimeException translateAuthSqlException(String contextMessage, SQLException e) {
        String sqlState = e.getSQLState();
        if ("23505".equals(sqlState)) {
            return new IllegalArgumentException("Username sudah terdaftar.", e);
        }
        if ("23503".equals(sqlState)) {
            return new IllegalArgumentException("Referensi profil guru untuk akun tidak ditemukan.", e);
        }
        if ("22001".equals(sqlState)) {
            return new IllegalArgumentException("Panjang data pengguna melebihi batas kolom database.", e);
        }
        return new IllegalStateException(contextMessage, e);
    }
}

