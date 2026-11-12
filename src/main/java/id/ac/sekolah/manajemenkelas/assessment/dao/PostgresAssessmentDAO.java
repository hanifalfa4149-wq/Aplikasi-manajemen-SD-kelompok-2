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
        List<Assessment> daftarPenilaian = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_FIND_ALL_ASSESSMENTS);
             ResultSet hasilQuery = pernyataan.executeQuery()) {

            while (hasilQuery.next()) {
                daftarPenilaian.add(petakanPenilaian(hasilQuery));
            }
            return daftarPenilaian;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar penilaian.", e);
        }
    }

    @Override
    public Optional<Assessment> findAssessmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_FIND_ASSESSMENT_BY_ID)) {

            pernyataan.setLong(1, id);
            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    return Optional.of(petakanPenilaian(hasilQuery));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari data penilaian berdasarkan ID.", e);
        }
    }

    @Override
    public List<Assessment> findAssessmentsByTeachingAssignmentId(Long idPenugasanMengajar) {
        if (idPenugasanMengajar == null) {
            return List.of();
        }
        List<Assessment> daftarPenilaian = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_FIND_ASSESSMENTS_BY_TEACHING)) {

            pernyataan.setLong(1, idPenugasanMengajar);
            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarPenilaian.add(petakanPenilaian(hasilQuery));
                }
            }
            return daftarPenilaian;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil penilaian berdasarkan penugasan mengajar.", e);
        }
    }

    @Override
    public void saveAssessment(Assessment penilaian) {
        if (penilaian == null) {
            throw new IllegalArgumentException("Data penilaian tidak boleh null.");
        }
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_INSERT_ASSESSMENT)) {

            pernyataan.setLong(1, penilaian.getTeachingAssignmentId());
            pernyataan.setString(2, penilaian.getTitle().trim());
            pernyataan.setString(3, penilaian.getAssessmentType().trim().toUpperCase());
            pernyataan.setDate(4, Date.valueOf(penilaian.getAssessmentDate()));
            pernyataan.setBigDecimal(5, penilaian.getMaxScore());
            JdbcUtils.setNullableString(pernyataan, 6, penilaian.getDescription());

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    penilaian.setId(hasilQuery.getLong("id"));
                }
            }
        } catch (SQLException e) {
            throw terjemahkanEksepsiSql("Gagal menyimpan data penilaian.", e);
        }
    }

    @Override
    public void updateAssessment(Assessment penilaian) {
        if (penilaian == null || penilaian.getId() == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi untuk pembaruan.");
        }
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_UPDATE_ASSESSMENT)) {

            pernyataan.setLong(1, penilaian.getTeachingAssignmentId());
            pernyataan.setString(2, penilaian.getTitle().trim());
            pernyataan.setString(3, penilaian.getAssessmentType().trim().toUpperCase());
            pernyataan.setDate(4, Date.valueOf(penilaian.getAssessmentDate()));
            pernyataan.setBigDecimal(5, penilaian.getMaxScore());
            JdbcUtils.setNullableString(pernyataan, 6, penilaian.getDescription());
            pernyataan.setLong(7, penilaian.getId());

            int barisTerpengaruh = pernyataan.executeUpdate();
            if (barisTerpengaruh == 0) {
                throw new IllegalArgumentException("Data penilaian tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw terjemahkanEksepsiSql("Gagal memperbarui data penilaian.", e);
        }
    }

    @Override
    public Optional<StudentScore> findScoreById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_FIND_SCORE_BY_ID)) {

            pernyataan.setLong(1, id);
            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    return Optional.of(petakanNilaiSiswa(hasilQuery));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari nilai siswa berdasarkan ID.", e);
        }
    }

    @Override
    public Optional<StudentScore> findScoreByAssessmentAndStudent(Long idPenilaian, Long idSiswa) {
        if (idPenilaian == null || idSiswa == null) {
            return Optional.empty();
        }
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_FIND_SCORE_BY_ASSESSMENT_AND_STUDENT)) {

            pernyataan.setLong(1, idPenilaian);
            pernyataan.setLong(2, idSiswa);
            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    return Optional.of(petakanNilaiSiswa(hasilQuery));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari nilai siswa pada penilaian.", e);
        }
    }

    @Override
    public List<StudentScore> findScoresByAssessmentId(Long idPenilaian) {
        if (idPenilaian == null) {
            return List.of();
        }
        return cariDaftarNilaiBerdasarkanLong(
                SQL_FIND_SCORES_BY_ASSESSMENT,
                idPenilaian,
                "Gagal mengambil daftar nilai berdasarkan penilaian.");
    }

    @Override
    public List<StudentScore> findScoresByStudentId(Long idSiswa) {
        if (idSiswa == null) {
            return List.of();
        }
        return cariDaftarNilaiBerdasarkanLong(
                SQL_FIND_SCORES_BY_STUDENT,
                idSiswa,
                "Gagal mengambil daftar nilai berdasarkan siswa.");
    }

    @Override
    public void saveScore(StudentScore nilaiSiswa) {
        if (nilaiSiswa == null) {
            throw new IllegalArgumentException("Data nilai siswa tidak boleh null.");
        }
        try (Connection koneksi = DatabaseConfig.getConnection()) {
            sisipkanSatuNilaiSiswa(koneksi, nilaiSiswa);
        } catch (SQLException e) {
            throw terjemahkanEksepsiSql("Gagal menyimpan nilai siswa.", e);
        }
    }

    @Override
    public void saveAllScores(List<StudentScore> daftarNilaiSiswa) {
        if (daftarNilaiSiswa == null || daftarNilaiSiswa.isEmpty()) {
            return;
        }
        try (Connection koneksi = DatabaseConfig.getConnection()) {
            boolean statusAutoCommitAwal = koneksi.getAutoCommit();
            koneksi.setAutoCommit(false);
            try {
                for (StudentScore nilaiSiswa : daftarNilaiSiswa) {
                    sisipkanSatuNilaiSiswa(koneksi, nilaiSiswa);
                }
                koneksi.commit();
            } catch (SQLException e) {
                JdbcUtils.lakukanRollbackDenganAman(koneksi, e);
                throw terjemahkanEksepsiSql("Gagal menyimpan nilai siswa secara massal.", e);
            } catch (RuntimeException e) {
                JdbcUtils.lakukanRollbackDenganAman(koneksi, e);
                throw e;
            } finally {
                JdbcUtils.pulihkanAutoCommitDenganAman(koneksi, statusAutoCommitAwal);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengelola transaksi database.", e);
        }
    }

    @Override
    public void updateScore(StudentScore nilaiSiswa) {
        if (nilaiSiswa == null || nilaiSiswa.getId() == null) {
            throw new IllegalArgumentException("ID nilai siswa wajib diisi untuk pembaruan.");
        }
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(SQL_UPDATE_SCORE)) {

            pernyataan.setLong(1, nilaiSiswa.getAssessmentId());
            pernyataan.setLong(2, nilaiSiswa.getStudentId());
            pernyataan.setBigDecimal(3, nilaiSiswa.getScore());
            JdbcUtils.setNullableString(pernyataan, 4, nilaiSiswa.getNotes());
            if (nilaiSiswa.getRecordedBy() != null) {
                pernyataan.setLong(5, nilaiSiswa.getRecordedBy());
            } else {
                pernyataan.setNull(5, Types.BIGINT);
            }
            pernyataan.setLong(6, nilaiSiswa.getId());

            int barisTerpengaruh = pernyataan.executeUpdate();
            if (barisTerpengaruh == 0) {
                throw new IllegalArgumentException("Data nilai siswa tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw terjemahkanEksepsiSql("Gagal memperbarui nilai siswa.", e);
        }
    }

    private void sisipkanSatuNilaiSiswa(Connection koneksi, StudentScore nilaiSiswa) throws SQLException {
        try (PreparedStatement pernyataan = koneksi.prepareStatement(SQL_INSERT_SCORE)) {
            pernyataan.setLong(1, nilaiSiswa.getAssessmentId());
            pernyataan.setLong(2, nilaiSiswa.getStudentId());
            pernyataan.setBigDecimal(3, nilaiSiswa.getScore());
            JdbcUtils.setNullableString(pernyataan, 4, nilaiSiswa.getNotes());
            if (nilaiSiswa.getRecordedBy() != null) {
                pernyataan.setLong(5, nilaiSiswa.getRecordedBy());
            } else {
                pernyataan.setNull(5, Types.BIGINT);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    nilaiSiswa.setId(hasilQuery.getLong("id"));
                }
            }
        }
    }

    private List<StudentScore> cariDaftarNilaiBerdasarkanLong(String sql, Long parameterId, String pesanGalat) {
        List<StudentScore> daftarNilai = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {

            pernyataan.setLong(1, parameterId);
            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarNilai.add(petakanNilaiSiswa(hasilQuery));
                }
            }
            return daftarNilai;
        } catch (SQLException e) {
            throw new IllegalStateException(pesanGalat, e);
        }
    }

    private Assessment petakanPenilaian(ResultSet hasilQuery) throws SQLException {
        Date tanggalSql = hasilQuery.getDate("tanggal_penilaian");
        return new Assessment(
                hasilQuery.getLong("id"),
                hasilQuery.getLong("id_penugasan_mengajar"),
                hasilQuery.getString("judul"),
                hasilQuery.getString("jenis_penilaian"),
                tanggalSql != null ? tanggalSql.toLocalDate() : null,
                hasilQuery.getBigDecimal("nilai_maksimal"),
                JdbcUtils.emptyIfNull(hasilQuery.getString("deskripsi")));
    }

    private StudentScore petakanNilaiSiswa(ResultSet hasilQuery) throws SQLException {
        long dicatatOlehMentah = hasilQuery.getLong("dicatat_oleh");
        Long dicatatOleh = hasilQuery.wasNull() ? null : dicatatOlehMentah;
        return new StudentScore(
                hasilQuery.getLong("id"),
                hasilQuery.getLong("id_penilaian"),
                hasilQuery.getLong("id_siswa"),
                hasilQuery.getBigDecimal("nilai"),
                JdbcUtils.emptyIfNull(hasilQuery.getString("keterangan")),
                dicatatOleh);
    }

    private RuntimeException terjemahkanEksepsiSql(String pesan, SQLException e) {
        String kodeSqlState = e.getSQLState();
        if ("23505".equals(kodeSqlState)) {
            return new IllegalArgumentException("Nilai siswa untuk penilaian ini sudah tercatat.", e);
        }
        if ("23503".equals(kodeSqlState)) {
            return new IllegalArgumentException(
                    "Referensi penugasan mengajar, penilaian, siswa, atau pengguna tidak valid.", e);
        }
        if ("23514".equals(kodeSqlState)) {
            return new IllegalArgumentException(
                    "Data penilaian atau nilai melanggar batasan nilai/jenis yang diizinkan.", e);
        }
        if ("22003".equals(kodeSqlState)) {
            return new IllegalArgumentException("Nilai angka melebihi kapasitas kolom NUMERIC(7,2).", e);
        }
        return new IllegalStateException(pesan, e);
    }
}
