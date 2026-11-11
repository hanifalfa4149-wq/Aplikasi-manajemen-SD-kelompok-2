package id.ac.sekolah.manajemenkelas.assessment.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.util.JdbcUtils;

/**
 * Implementasi JDBC PostgreSQL untuk AssessmentDAO yang mengakses tabel penilaian dan nilai_siswa.
 */
public class PostgresAssessmentDAO implements AssessmentDAO {

    private static final String SQL_ASSESSMENT_BASE = """
            SELECT id, id_penugasan_mengajar, judul, jenis_penilaian,
                   tanggal_penilaian, nilai_maksimal, deskripsi
            FROM penilaian
            """;

    private static final String SQL_FIND_ALL_ASSESSMENTS = SQL_ASSESSMENT_BASE
            + " ORDER BY tanggal_penilaian DESC, id DESC";

    private static final String SQL_FIND_ASSESSMENT_BY_ID = SQL_ASSESSMENT_BASE
            + " WHERE id = ?";

    private static final String SQL_FIND_ASSESSMENTS_BY_TEACHING = SQL_ASSESSMENT_BASE
            + " WHERE id_penugasan_mengajar = ? ORDER BY tanggal_penilaian DESC, id DESC";

    private static final String SQL_INSERT_ASSESSMENT = """
            INSERT INTO penilaian (
                id_penugasan_mengajar, judul, jenis_penilaian,
                tanggal_penilaian, nilai_maksimal, deskripsi
            ) VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_ASSESSMENT = """
            UPDATE penilaian
            SET id_penugasan_mengajar = ?,
                judul = ?,
                jenis_penilaian = ?,
                tanggal_penilaian = ?,
                nilai_maksimal = ?,
                deskripsi = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    private static final String SQL_SCORE_BASE = """
            SELECT id, id_penilaian, id_siswa, nilai, keterangan, dicatat_oleh
            FROM nilai_siswa
            """;

    private static final String SQL_FIND_SCORE_BY_ID = SQL_SCORE_BASE
            + " WHERE id = ?";

    private static final String SQL_FIND_SCORE_BY_ASSESSMENT_AND_STUDENT = SQL_SCORE_BASE
            + " WHERE id_penilaian = ? AND id_siswa = ?";

    private static final String SQL_FIND_SCORES_BY_ASSESSMENT = SQL_SCORE_BASE
            + " WHERE id_penilaian = ? ORDER BY id_siswa ASC";

    private static final String SQL_FIND_SCORES_BY_STUDENT = SQL_SCORE_BASE
            + " WHERE id_siswa = ? ORDER BY id DESC";

    private static final String SQL_INSERT_SCORE = """
            INSERT INTO nilai_siswa (
                id_penilaian, id_siswa, nilai, keterangan, dicatat_oleh
            ) VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_SCORE = """
            UPDATE nilai_siswa
            SET id_penilaian = ?,
                id_siswa = ?,
                nilai = ?,
                keterangan = ?,
                dicatat_oleh = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    @Override
    public List<Assessment> findAllAssessments() {
        List<Assessment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_ASSESSMENTS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapAssessment(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar penilaian.", e);
        }
    }

    @Override
    public Optional<Assessment> findAssessmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ASSESSMENT_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapAssessment(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari data penilaian berdasarkan ID.", e);
        }
    }

    @Override
    public List<Assessment> findAssessmentsByTeachingAssignmentId(Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return List.of();
        }
        List<Assessment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ASSESSMENTS_BY_TEACHING)) {

            ps.setLong(1, teachingAssignmentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapAssessment(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil penilaian berdasarkan penugasan mengajar.", e);
        }
    }

    @Override
    public void saveAssessment(Assessment assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("Data penilaian tidak boleh null.");
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_ASSESSMENT)) {

            ps.setLong(1, assessment.getTeachingAssignmentId());
            ps.setString(2, assessment.getTitle().trim());
            ps.setString(3, assessment.getAssessmentType().trim().toUpperCase());
            ps.setDate(4, Date.valueOf(assessment.getAssessmentDate()));
            ps.setBigDecimal(5, assessment.getMaxScore());
            JdbcUtils.setNullableString(ps, 6, assessment.getDescription());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    assessment.setId(rs.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal menyimpan data penilaian.", e);
        }
    }

    @Override
    public void updateAssessment(Assessment assessment) {
        if (assessment == null || assessment.getId() == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi untuk pembaruan.");
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_ASSESSMENT)) {

            ps.setLong(1, assessment.getTeachingAssignmentId());
            ps.setString(2, assessment.getTitle().trim());
            ps.setString(3, assessment.getAssessmentType().trim().toUpperCase());
            ps.setDate(4, Date.valueOf(assessment.getAssessmentDate()));
            ps.setBigDecimal(5, assessment.getMaxScore());
            JdbcUtils.setNullableString(ps, 6, assessment.getDescription());
            ps.setLong(7, assessment.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data penilaian tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal memperbarui data penilaian.", e);
        }
    }

    @Override
    public Optional<StudentScore> findScoreById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_SCORE_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapScore(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari nilai siswa berdasarkan ID.", e);
        }
    }

    @Override
    public Optional<StudentScore> findScoreByAssessmentAndStudent(Long assessmentId, Long studentId) {
        if (assessmentId == null || studentId == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_SCORE_BY_ASSESSMENT_AND_STUDENT)) {

            ps.setLong(1, assessmentId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapScore(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari nilai siswa pada penilaian.", e);
        }
    }

    @Override
    public List<StudentScore> findScoresByAssessmentId(Long assessmentId) {
        if (assessmentId == null) {
            return List.of();
        }
        return queryScoresByLong(
                SQL_FIND_SCORES_BY_ASSESSMENT,
                assessmentId,
                "Gagal mengambil daftar nilai berdasarkan penilaian.");
    }

    @Override
    public List<StudentScore> findScoresByStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return queryScoresByLong(
                SQL_FIND_SCORES_BY_STUDENT,
                studentId,
                "Gagal mengambil daftar nilai berdasarkan siswa.");
    }

    @Override
    public void saveScore(StudentScore score) {
        if (score == null) {
            throw new IllegalArgumentException("Data nilai siswa tidak boleh null.");
        }
        try (Connection conn = DatabaseConfig.getConnection()) {
            insertSingleScore(conn, score);
        } catch (SQLException e) {
            throw translateSqlException("Gagal menyimpan nilai siswa.", e);
        }
    }

    @Override
    public void saveAllScores(List<StudentScore> scores) {
        if (scores == null || scores.isEmpty()) {
            return;
        }
        JdbcUtils.inTransactionVoid(conn -> {
            try {
                for (StudentScore score : scores) {
                    insertSingleScore(conn, score);
                }
            } catch (SQLException e) {
                throw translateSqlException("Gagal menyimpan nilai siswa secara massal.", e);
            }
        });
    }

    @Override
    public void updateScore(StudentScore score) {
        if (score == null || score.getId() == null) {
            throw new IllegalArgumentException("ID nilai siswa wajib diisi untuk pembaruan.");
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_SCORE)) {

            ps.setLong(1, score.getAssessmentId());
            ps.setLong(2, score.getStudentId());
            ps.setBigDecimal(3, score.getScore());
            JdbcUtils.setNullableString(ps, 4, score.getNotes());
            if (score.getRecordedBy() != null) {
                ps.setLong(5, score.getRecordedBy());
            } else {
                ps.setNull(5, Types.BIGINT);
            }
            ps.setLong(6, score.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data nilai siswa tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateSqlException("Gagal memperbarui nilai siswa.", e);
        }
    }

    private void insertSingleScore(Connection conn, StudentScore score) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_SCORE)) {
            ps.setLong(1, score.getAssessmentId());
            ps.setLong(2, score.getStudentId());
            ps.setBigDecimal(3, score.getScore());
            JdbcUtils.setNullableString(ps, 4, score.getNotes());
            if (score.getRecordedBy() != null) {
                ps.setLong(5, score.getRecordedBy());
            } else {
                ps.setNull(5, Types.BIGINT);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    score.setId(rs.getLong("id"));
                }
            }
        }
    }

    private List<StudentScore> queryScoresByLong(String sql, Long idParam, String errorMessage) {
        List<StudentScore> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, idParam);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapScore(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private Assessment mapAssessment(ResultSet rs) throws SQLException {
        Date sqlDate = rs.getDate("tanggal_penilaian");
        return new Assessment(
                rs.getLong("id"),
                rs.getLong("id_penugasan_mengajar"),
                rs.getString("judul"),
                rs.getString("jenis_penilaian"),
                sqlDate != null ? sqlDate.toLocalDate() : null,
                rs.getBigDecimal("nilai_maksimal"),
                JdbcUtils.emptyIfNull(rs.getString("deskripsi")));
    }

    private StudentScore mapScore(ResultSet rs) throws SQLException {
        long recordedByRaw = rs.getLong("dicatat_oleh");
        Long recordedBy = rs.wasNull() ? null : recordedByRaw;
        return new StudentScore(
                rs.getLong("id"),
                rs.getLong("id_penilaian"),
                rs.getLong("id_siswa"),
                rs.getBigDecimal("nilai"),
                JdbcUtils.emptyIfNull(rs.getString("keterangan")),
                recordedBy);
    }

    private RuntimeException translateSqlException(String message, SQLException e) {
        String sqlState = e.getSQLState();
        if ("23505".equals(sqlState)) {
            return new IllegalArgumentException("Nilai siswa untuk penilaian ini sudah tercatat.", e);
        }
        if ("23503".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Referensi penugasan mengajar, penilaian, siswa, atau pengguna tidak valid.", e);
        }
        if ("23514".equals(sqlState)) {
            return new IllegalArgumentException(
                    "Data penilaian atau nilai melanggar batasan nilai/jenis yang diizinkan.", e);
        }
        if ("22003".equals(sqlState)) {
            return new IllegalArgumentException("Nilai angka melebihi kapasitas kolom NUMERIC(7,2).", e);
        }
        return new IllegalStateException(message, e);
    }
}

