package id.ac.sekolah.manajemenkelas.teacher.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.util.GenderMapper;
import id.ac.sekolah.manajemenkelas.core.util.JdbcUtils;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

/**
 * Implementasi JDBC PostgreSQL untuk TeacherDAO yang mengakses tabel guru.
 */
public class PostgresTeacherDAO implements TeacherDAO {

    private static final String SQL_FIND_ALL = """
            SELECT id, nip, nama_lengkap, jenis_kelamin, email, nomor_telepon,
                   alamat, pendidikan_terakhir, tanggal_bergabung, aktif
            FROM guru
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_BY_ID = """
            SELECT id, nip, nama_lengkap, jenis_kelamin, email, nomor_telepon,
                   alamat, pendidikan_terakhir, tanggal_bergabung, aktif
            FROM guru
            WHERE id = ?
            """;

    private static final String SQL_FIND_BY_NIP = """
            SELECT id, nip, nama_lengkap, jenis_kelamin, email, nomor_telepon,
                   alamat, pendidikan_terakhir, tanggal_bergabung, aktif
            FROM guru
            WHERE LOWER(nip) = LOWER(?)
            """;

    private static final String SQL_INSERT = """
            INSERT INTO guru (
                nip, nama_lengkap, jenis_kelamin, email, nomor_telepon,
                alamat, pendidikan_terakhir, tanggal_bergabung, aktif
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE = """
            UPDATE guru
            SET nip = ?,
                nama_lengkap = ?,
                jenis_kelamin = ?,
                email = ?,
                nomor_telepon = ?,
                alamat = ?,
                pendidikan_terakhir = ?,
                tanggal_bergabung = ?,
                aktif = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String SQL_DELETE_BY_ID = """
            DELETE FROM guru
            WHERE id = ?
            """;

    private static final String SQL_EXISTS_BY_NIP = """
            SELECT 1 FROM guru WHERE LOWER(nip) = LOWER(?)
            """;

    @Override
    public List<Teacher> findAll() {
        List<Teacher> teachers = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                teachers.add(mapRow(rs));
            }
            return teachers;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar guru dari database.", e);
        }
    }

    @Override
    public Optional<Teacher> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari guru berdasarkan ID.", e);
        }
    }

    @Override
    public Optional<Teacher> findByNip(String nip) {
        if (nip == null || nip.isBlank()) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_NIP)) {

            ps.setString(1, nip.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari guru berdasarkan NIP.", e);
        }
    }

    @Override
    public void save(Teacher teacher) {
        if (teacher == null) {
            throw new IllegalArgumentException("Data guru tidak boleh null.");
        }

        String dbGender = GenderMapper.toDatabaseGender(teacher.getGender());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, teacher.getNip().trim());
            ps.setString(2, teacher.getFullName().trim());
            ps.setString(3, dbGender);
            // Normalisasi email kosong ke SQL NULL agar tidak melanggar idx_guru_email_unik
            JdbcUtils.setNullableString(ps, 4, teacher.getEmail());
            JdbcUtils.setNullableString(ps, 5, teacher.getPhone());
            JdbcUtils.setNullableString(ps, 6, teacher.getAddress());
            JdbcUtils.setNullableString(ps, 7, teacher.getEducation());
            setNullableDate(ps, 8, teacher.getJoinDate());
            ps.setBoolean(9, teacher.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    teacher.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal menyimpan data guru.", e);
        }
    }

    @Override
    public void update(Teacher teacher) {
        if (teacher == null || teacher.getId() == null) {
            throw new IllegalArgumentException("ID guru wajib diisi untuk pembaruan.");
        }

        String dbGender = GenderMapper.toDatabaseGender(teacher.getGender());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, teacher.getNip().trim());
            ps.setString(2, teacher.getFullName().trim());
            ps.setString(3, dbGender);
            JdbcUtils.setNullableString(ps, 4, teacher.getEmail());
            JdbcUtils.setNullableString(ps, 5, teacher.getPhone());
            JdbcUtils.setNullableString(ps, 6, teacher.getAddress());
            JdbcUtils.setNullableString(ps, 7, teacher.getEducation());
            setNullableDate(ps, 8, teacher.getJoinDate());
            ps.setBoolean(9, teacher.isActive());
            ps.setLong(10, teacher.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data guru tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal memperbarui data guru.", e);
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id == null) {
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE_BY_ID)) {

            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw translateSqlException("Gagal menghapus data guru.", e);
        }
    }

    @Override
    public boolean existsByNip(String nip) {
        if (nip == null || nip.isBlank()) {
            return false;
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_BY_NIP)) {

            ps.setString(1, nip.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal memeriksa keberadaan NIP guru.", e);
        }
    }

    private Teacher mapRow(ResultSet rs) throws SQLException {
        Date joinDateSql = rs.getDate("tanggal_bergabung");
        LocalDate joinDate = joinDateSql != null ? joinDateSql.toLocalDate() : null;

        return new Teacher(
                rs.getLong("id"),
                rs.getString("nip"),
                rs.getString("nama_lengkap"),
                GenderMapper.fromDatabaseGender(rs.getString("jenis_kelamin")),
                JdbcUtils.emptyIfNull(rs.getString("email")),
                JdbcUtils.emptyIfNull(rs.getString("nomor_telepon")),
                JdbcUtils.emptyIfNull(rs.getString("alamat")),
                JdbcUtils.emptyIfNull(rs.getString("pendidikan_terakhir")),
                joinDate,
                rs.getBoolean("aktif"));
    }

    private static void setNullableDate(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        if (date == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(date));
        }
    }

    private static RuntimeException translateSqlException(String contextMessage, SQLException e) {
        String sqlState = e.getSQLState();
        if ("23505".equals(sqlState)) {
            String detail = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (detail.contains("email")) {
                return new IllegalArgumentException("Email sudah terdaftar.", e);
            }
            if (detail.contains("nip")) {
                return new IllegalArgumentException("NIP sudah terdaftar.", e);
            }
            return new IllegalArgumentException("Data guru duplikat.", e);
        }
        if ("23503".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Data guru masih direferensikan oleh akun atau penugasan akademik.", e);
        }
        if ("22001".equals(sqlState)) {
            return new IllegalArgumentException("Panjang data teks guru melebihi batas kolom database.", e);
        }
        return new IllegalStateException(contextMessage, e);
    }
}

