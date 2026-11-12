package id.ac.sekolah.manajemenkelas.attendance.dao;

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

import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.util.JdbcUtils;

/**
 * Implementasi JDBC PostgreSQL untuk AttendanceDAO yang mengakses tabel absensi.
 */
public class PostgresAttendanceDAO implements AttendanceDAO {

    private static final String SQL_BASE_SELECT = """
            SELECT id, id_siswa, id_kelas, tanggal_absensi, status, keterangan, dicatat_oleh
            FROM absensi
            """;

    private static final String SQL_FIND_BY_ID = SQL_BASE_SELECT + " WHERE id = ?";

    private static final String SQL_FIND_BY_STUDENT_AND_DATE = SQL_BASE_SELECT
            + " WHERE id_siswa = ? AND tanggal_absensi = ?";

    private static final String SQL_FIND_BY_DATE = SQL_BASE_SELECT
            + " WHERE tanggal_absensi = ? ORDER BY id_kelas ASC, id_siswa ASC";

    private static final String SQL_FIND_BY_CLASS = SQL_BASE_SELECT
            + " WHERE id_kelas = ? ORDER BY tanggal_absensi DESC, id_siswa ASC";

    private static final String SQL_FIND_BY_CLASS_AND_DATE = SQL_BASE_SELECT
            + " WHERE id_kelas = ? AND tanggal_absensi = ? ORDER BY id_siswa ASC";

    private static final String SQL_FIND_BY_CLASS_AND_RANGE = SQL_BASE_SELECT
            + " WHERE id_kelas = ? AND tanggal_absensi BETWEEN ? AND ? ORDER BY tanggal_absensi ASC, id_siswa ASC";

    private static final String SQL_FIND_BY_STUDENT = SQL_BASE_SELECT
            + " WHERE id_siswa = ? ORDER BY tanggal_absensi DESC";

    private static final String SQL_INSERT = """
            INSERT INTO absensi (
                id_siswa, id_kelas, tanggal_absensi, status, keterangan, dicatat_oleh
            ) VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE = """
            UPDATE absensi
            SET id_siswa = ?,
                id_kelas = ?,
                tanggal_absensi = ?,
                status = ?,
                keterangan = ?,
                dicatat_oleh = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    @Override
    public Optional<Attendance> findById(Long id) {
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
            throw new IllegalStateException("Gagal mencari data absensi berdasarkan ID.", e);
        }
    }

    @Override
    public Optional<Attendance> findByStudentAndDate(Long studentId, LocalDate date) {
        if (studentId == null || date == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_STUDENT_AND_DATE)) {

            ps.setLong(1, studentId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari data absensi siswa.", e);
        }
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) {
        if (date == null) {
            return List.of();
        }
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_DATE)) {

            ps.setDate(1, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil data absensi berdasarkan tanggal.", e);
        }
    }

    @Override
    public List<Attendance> findByClassId(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return queryByLong(SQL_FIND_BY_CLASS, classId, "Gagal mengambil data absensi kelas.");
    }

    @Override
    public List<Attendance> findByClassAndDate(Long classId, LocalDate date) {
        if (classId == null || date == null) {
            return List.of();
        }
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS_AND_DATE)) {

            ps.setLong(1, classId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil data absensi kelas per tanggal.", e);
        }
    }

    @Override
    public List<Attendance> findByClassAndDateRange(Long classId, LocalDate startDate, LocalDate endDate) {
        if (classId == null || startDate == null || endDate == null) {
            return List.of();
        }
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_BY_CLASS_AND_RANGE)) {

            ps.setLong(1, classId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rekapitulasi absensi kelas.", e);
        }
    }

    @Override
    public List<Attendance> findByStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return queryByLong(SQL_FIND_BY_STUDENT, studentId, "Gagal mengambil riwayat absensi siswa.");
    }

    @Override
    public void save(Attendance attendance) {
        if (attendance == null) {
            throw new IllegalArgumentException("Data absensi tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            insertSingle(conn, attendance);
        } catch (SQLException e) {
            throw translateSqlException("Gagal menyimpan data absensi.", e);
        }
    }

    @Override
    public void saveAll(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            return;
        }

        Connection koneksi = null;
        boolean autoCommitAwal = true;
        try {
            koneksi = DatabaseConfig.getConnection();
            autoCommitAwal = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);

            for (Attendance attendance : attendances) {
                insertSingle(koneksi, attendance);
            }

            koneksi.commit();
        } catch (SQLException e) {
            JdbcUtils.lakukanRollbackDenganAman(koneksi, e);
            throw translateSqlException("Gagal menyimpan absensi massal kelas.", e);
        } catch (RuntimeException e) {
            JdbcUtils.lakukanRollbackDenganAman(koneksi, e);
            throw e;
        } finally {
            JdbcUtils.pulihkanAutoCommitDenganAman(koneksi, autoCommitAwal);
            if (koneksi != null) {
                try {
                    koneksi.close();
                } catch (SQLException ignored) {
                    // Abaikan kegagalan menutup koneksi saat cleanup
                }
            }
        }
    }

    @Override
    public void update(Attendance attendance) {
        if (attendance == null || attendance.getId() == null) {
            throw new IllegalArgumentException("ID absensi wajib diisi untuk pembaruan.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setLong(1, attendance.getStudentId());
            ps.setLong(2, attendance.getClassId());
            ps.setDate(3, Date.valueOf(attendance.getAttendanceDate()));
            ps.setString(4, attendance.getStatus().trim().toUpperCase());
            JdbcUtils.setNullableString(ps, 5, attendance.getNotes());
            if (attendance.getRecordedBy() != null) {
                ps.setLong(6, attendance.getRecordedBy());
            } else {
                ps.setNull(6, Types.BIGINT);
            }
            ps.setLong(7, attendance.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data absensi tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal memperbarui data absensi.", e);
        }
    }

    private void insertSingle(Connection conn, Attendance attendance) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
            ps.setLong(1, attendance.getStudentId());
            ps.setLong(2, attendance.getClassId());
            ps.setDate(3, Date.valueOf(attendance.getAttendanceDate()));
            ps.setString(4, attendance.getStatus().trim().toUpperCase());
            JdbcUtils.setNullableString(ps, 5, attendance.getNotes());
            if (attendance.getRecordedBy() != null) {
                ps.setLong(6, attendance.getRecordedBy());
            } else {
                ps.setNull(6, Types.BIGINT);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    attendance.setId(rs.getLong(1));
                }
            }
        }
    }

    private List<Attendance> queryByLong(String sql, Long id, String errorMessage) {
        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private Attendance mapRow(ResultSet rs) throws SQLException {
        long rawRecorder = rs.getLong("dicatat_oleh");
        Long recordedBy = rs.wasNull() ? null : rawRecorder;

        return new Attendance(
                rs.getLong("id"),
                rs.getLong("id_siswa"),
                rs.getLong("id_kelas"),
                rs.getDate("tanggal_absensi").toLocalDate(),
                rs.getString("status"),
                JdbcUtils.emptyIfNull(rs.getString("keterangan")),
                recordedBy);
    }

    private static RuntimeException translateSqlException(String contextMessage, SQLException e) {
        String sqlState = e.getSQLState();
        if ("23505".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Absensi siswa pada tanggal tersebut sudah tercatat.", e);
        }
        if ("23503".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Referensi siswa, kelas, atau pengguna pencatat tidak valid.", e);
        }
        if ("23514".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Status absensi harus HADIR, SAKIT, IZIN, atau ALPA.", e);
        }
        return new IllegalStateException(contextMessage, e);
    }
}

