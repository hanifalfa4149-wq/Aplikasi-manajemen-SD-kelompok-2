package id.ac.sekolah.manajemenkelas.student.dao;

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
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Implementasi JDBC PostgreSQL untuk StudentDAO yang mengakses tabel siswa.
 */
public class PostgresStudentDAO implements StudentDAO {

    private static final String SQL_FIND_ALL = """
            SELECT id, nis, nisn, nama_lengkap, jenis_kelamin, tanggal_lahir,
                   tempat_lahir, alamat, nama_orang_tua, nomor_telepon_orang_tua, aktif
            FROM siswa
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_BY_ID = """
            SELECT id, nis, nisn, nama_lengkap, jenis_kelamin, tanggal_lahir,
                   tempat_lahir, alamat, nama_orang_tua, nomor_telepon_orang_tua, aktif
            FROM siswa
            WHERE id = ?
            """;

    private static final String SQL_FIND_BY_NIS = """
            SELECT id, nis, nisn, nama_lengkap, jenis_kelamin, tanggal_lahir,
                   tempat_lahir, alamat, nama_orang_tua, nomor_telepon_orang_tua, aktif
            FROM siswa
            WHERE LOWER(nis) = LOWER(?)
            """;

    private static final String SQL_FIND_BY_NISN = """
            SELECT id, nis, nisn, nama_lengkap, jenis_kelamin, tanggal_lahir,
                   tempat_lahir, alamat, nama_orang_tua, nomor_telepon_orang_tua, aktif
            FROM siswa
            WHERE LOWER(nisn) = LOWER(?)
            """;

    private static final String SQL_INSERT = """
            INSERT INTO siswa (
                nis, nisn, nama_lengkap, jenis_kelamin, tanggal_lahir,
                tempat_lahir, alamat, nama_orang_tua, nomor_telepon_orang_tua, aktif
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE = """
            UPDATE siswa
            SET nis = ?,
                nisn = ?,
                nama_lengkap = ?,
                jenis_kelamin = ?,
                tanggal_lahir = ?,
                tempat_lahir = ?,
                alamat = ?,
                nama_orang_tua = ?,
                nomor_telepon_orang_tua = ?,
                aktif = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String SQL_DELETE_BY_ID = """
            DELETE FROM siswa
            WHERE id = ?
            """;

    private static final String SQL_EXISTS_BY_NIS = """
            SELECT 1 FROM siswa WHERE LOWER(nis) = LOWER(?)
            """;

    private static final String SQL_EXISTS_BY_NISN = """
            SELECT 1 FROM siswa WHERE LOWER(nisn) = LOWER(?)
            """;

    @Override
    public List<Student> findAll() {
        List<Student> students = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                students.add(mapRow(rs));
            }
            return students;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar siswa dari database.", e);
        }
    }

    @Override
    public Optional<Student> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return findSingleByLong(SQL_FIND_BY_ID, id, "Gagal mencari siswa berdasarkan ID.");
    }

    @Override
    public Optional<Student> findByNis(String nis) {
        if (nis == null || nis.isBlank()) {
            return Optional.empty();
        }
        return findSingleByString(SQL_FIND_BY_NIS, nis.trim(), "Gagal mencari siswa berdasarkan NIS.");
    }

    @Override
    public Optional<Student> findByNisn(String nisn) {
        if (nisn == null || nisn.isBlank()) {
            return Optional.empty();
        }
        return findSingleByString(SQL_FIND_BY_NISN, nisn.trim(), "Gagal mencari siswa berdasarkan NISN.");
    }

    @Override
    public void save(Student student) {
        if (student == null) {
            throw new IllegalArgumentException("Data siswa tidak boleh null.");
        }

        String dbGender = GenderMapper.toDatabaseGender(student.getGender());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {

            ps.setString(1, student.getNis().trim());
            JdbcUtils.setNullableString(ps, 2, student.getNisn());
            ps.setString(3, student.getFullName().trim());
            ps.setString(4, dbGender);
            setNullableDate(ps, 5, student.getBirthDate());
            JdbcUtils.setNullableString(ps, 6, student.getBirthPlace());
            JdbcUtils.setNullableString(ps, 7, student.getAddress());
            JdbcUtils.setNullableString(ps, 8, student.getParentName());
            JdbcUtils.setNullableString(ps, 9, student.getParentPhone());
            ps.setBoolean(10, student.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    student.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal menyimpan data siswa.", e);
        }
    }

    @Override
    public void update(Student student) {
        if (student == null || student.getId() == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi untuk pembaruan.");
        }

        String dbGender = GenderMapper.toDatabaseGender(student.getGender());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, student.getNis().trim());
            JdbcUtils.setNullableString(ps, 2, student.getNisn());
            ps.setString(3, student.getFullName().trim());
            ps.setString(4, dbGender);
            setNullableDate(ps, 5, student.getBirthDate());
            JdbcUtils.setNullableString(ps, 6, student.getBirthPlace());
            JdbcUtils.setNullableString(ps, 7, student.getAddress());
            JdbcUtils.setNullableString(ps, 8, student.getParentName());
            JdbcUtils.setNullableString(ps, 9, student.getParentPhone());
            ps.setBoolean(10, student.isActive());
            ps.setLong(11, student.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data siswa tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal memperbarui data siswa.", e);
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
            throw translateSqlException("Gagal menghapus data siswa.", e);
        }
    }

    @Override
    public boolean existsByNis(String nis) {
        if (nis == null || nis.isBlank()) {
            return false;
        }
        return existsByString(SQL_EXISTS_BY_NIS, nis.trim(), "Gagal memeriksa keberadaan NIS siswa.");
    }

    @Override
    public boolean existsByNisn(String nisn) {
        if (nisn == null || nisn.isBlank()) {
            return false;
        }
        return existsByString(SQL_EXISTS_BY_NISN, nisn.trim(), "Gagal memeriksa keberadaan NISN siswa.");
    }

    private Optional<Student> findSingleByLong(String sql, Long id, String errorMessage) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private Optional<Student> findSingleByString(String sql, String value, String errorMessage) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private boolean existsByString(String sql, String value, String errorMessage) {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private Student mapRow(ResultSet rs) throws SQLException {
        Date birthDateSql = rs.getDate("tanggal_lahir");
        LocalDate birthDate = birthDateSql != null ? birthDateSql.toLocalDate() : null;

        return new Student(
                rs.getLong("id"),
                rs.getString("nis"),
                JdbcUtils.emptyIfNull(rs.getString("nisn")),
                rs.getString("nama_lengkap"),
                GenderMapper.fromDatabaseGender(rs.getString("jenis_kelamin")),
                birthDate,
                JdbcUtils.emptyIfNull(rs.getString("tempat_lahir")),
                JdbcUtils.emptyIfNull(rs.getString("alamat")),
                JdbcUtils.emptyIfNull(rs.getString("nama_orang_tua")),
                JdbcUtils.emptyIfNull(rs.getString("nomor_telepon_orang_tua")),
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
            if (detail.contains("nisn")) {
                return new IllegalArgumentException("NISN sudah terdaftar.", e);
            }
            if (detail.contains("nis")) {
                return new IllegalArgumentException("NIS sudah terdaftar.", e);
            }
            return new IllegalArgumentException("Data siswa duplikat.", e);
        }
        if ("23503".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Data siswa masih direferensikan oleh data akademik atau absensi/nilai.", e);
        }
        if ("22001".equals(sqlState)) {
            return new IllegalArgumentException("Panjang data teks siswa melebihi batas kolom database.", e);
        }
        return new IllegalStateException(contextMessage, e);
    }
}

