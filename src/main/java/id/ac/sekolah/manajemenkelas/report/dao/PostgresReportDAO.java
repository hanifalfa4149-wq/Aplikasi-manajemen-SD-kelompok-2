package id.ac.sekolah.manajemenkelas.report.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.util.JdbcUtils;
import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;

/**
 * Implementasi JDBC PostgreSQL untuk ReportDAO yang menjalankan agregasi SQL langsung di database.
 */
public class PostgresReportDAO implements ReportDAO {

    private static final String SQL_ID_KELAS_GURU = """
            SELECT id_kelas FROM penugasan_wali_kelas WHERE id_guru = ? AND aktif = TRUE
            UNION
            SELECT id_kelas FROM penugasan_mengajar WHERE id_guru = ? AND aktif = TRUE
            """;

    private static final String SQL_JUMLAH_SISWA_KELAS_ADMIN = """
            SELECT k.id,
                   k.nama,
                   k.tingkat_kelas,
                   COALESCE(ta.nama, '-') AS tahun_ajaran_nama,
                   k.kapasitas,
                   COUNT(DISTINCT s.id) AS active_student_count
            FROM kelas k
            LEFT JOIN tahun_ajaran ta ON ta.id = k.id_tahun_ajaran
            LEFT JOIN riwayat_kelas_siswa rks
                   ON rks.id_kelas = k.id
                  AND rks.status = 'AKTIF'
                  AND rks.tanggal_keluar IS NULL
            LEFT JOIN siswa s
                   ON s.id = rks.id_siswa
                  AND s.aktif = TRUE
            WHERE k.aktif = TRUE
            GROUP BY k.id, k.nama, k.tingkat_kelas, ta.nama, k.kapasitas
            ORDER BY k.tingkat_kelas ASC, k.nama ASC, k.id ASC
            """;

    private static final String SQL_JUMLAH_SISWA_KELAS_GURU = """
            SELECT k.id,
                   k.nama,
                   k.tingkat_kelas,
                   COALESCE(ta.nama, '-') AS tahun_ajaran_nama,
                   k.kapasitas,
                   COUNT(DISTINCT s.id) AS active_student_count
            FROM kelas k
            LEFT JOIN tahun_ajaran ta ON ta.id = k.id_tahun_ajaran
            LEFT JOIN riwayat_kelas_siswa rks
                   ON rks.id_kelas = k.id
                  AND rks.status = 'AKTIF'
                  AND rks.tanggal_keluar IS NULL
            LEFT JOIN siswa s
                   ON s.id = rks.id_siswa
                  AND s.aktif = TRUE
            WHERE k.aktif = TRUE
              AND k.id IN (
                  SELECT id_kelas FROM penugasan_wali_kelas WHERE id_guru = ? AND aktif = TRUE
                  UNION
                  SELECT id_kelas FROM penugasan_mengajar WHERE id_guru = ? AND aktif = TRUE
              )
            GROUP BY k.id, k.nama, k.tingkat_kelas, ta.nama, k.kapasitas
            ORDER BY k.tingkat_kelas ASC, k.nama ASC, k.id ASC
            """;

    private static final String SQL_JUMLAH_GENDER_ADMIN = """
            WITH genders(db_val, code, label, sort_ord) AS (
                VALUES ('LAKI_LAKI', 'L', 'Laki-laki', 1),
                       ('PEREMPUAN', 'P', 'Perempuan', 2)
            )
            SELECT g.code,
                   g.label,
                   COUNT(s.id) AS active_student_count
            FROM genders g
            LEFT JOIN siswa s
                   ON s.jenis_kelamin = g.db_val
                  AND s.aktif = TRUE
            GROUP BY g.code, g.label, g.sort_ord
            ORDER BY g.sort_ord ASC
            """;

    private static final String SQL_JUMLAH_GENDER_GURU = """
            WITH genders(db_val, code, label, sort_ord) AS (
                VALUES ('LAKI_LAKI', 'L', 'Laki-laki', 1),
                       ('PEREMPUAN', 'P', 'Perempuan', 2)
            ),
            scoped_students AS (
                SELECT DISTINCT s.id, s.jenis_kelamin
                FROM siswa s
                JOIN riwayat_kelas_siswa rks
                  ON rks.id_siswa = s.id
                 AND rks.status = 'AKTIF'
                 AND rks.tanggal_keluar IS NULL
                WHERE s.aktif = TRUE
                  AND rks.id_kelas IN (
                      SELECT id_kelas FROM penugasan_wali_kelas WHERE id_guru = ? AND aktif = TRUE
                      UNION
                      SELECT id_kelas FROM penugasan_mengajar WHERE id_guru = ? AND aktif = TRUE
                  )
            )
            SELECT g.code,
                   g.label,
                   COUNT(ss.id) AS active_student_count
            FROM genders g
            LEFT JOIN scoped_students ss
                   ON ss.jenis_kelamin = g.db_val
            GROUP BY g.code, g.label, g.sort_ord
            ORDER BY g.sort_ord ASC
            """;

    private static final String SQL_ABSENSI_RENTANG_ADMIN = """
            SELECT COUNT(*) AS total_records,
                   COUNT(*) FILTER (WHERE status = 'HADIR') AS hadir_count,
                   COUNT(*) FILTER (WHERE status = 'SAKIT') AS sakit_count,
                   COUNT(*) FILTER (WHERE status = 'IZIN') AS izin_count,
                   COUNT(*) FILTER (WHERE status = 'ALPA') AS alpa_count,
                   CASE
                       WHEN COUNT(*) > 0
                           THEN ROUND(COUNT(*) FILTER (WHERE status = 'HADIR') * 100.0 / COUNT(*), 2)
                       ELSE NULL
                   END AS attendance_pct
            FROM absensi
            WHERE tanggal_absensi BETWEEN ? AND ?
            """;

    private static final String SQL_ABSENSI_RENTANG_GURU = """
            SELECT COUNT(*) AS total_records,
                   COUNT(*) FILTER (WHERE status = 'HADIR') AS hadir_count,
                   COUNT(*) FILTER (WHERE status = 'SAKIT') AS sakit_count,
                   COUNT(*) FILTER (WHERE status = 'IZIN') AS izin_count,
                   COUNT(*) FILTER (WHERE status = 'ALPA') AS alpa_count,
                   CASE
                       WHEN COUNT(*) > 0
                           THEN ROUND(COUNT(*) FILTER (WHERE status = 'HADIR') * 100.0 / COUNT(*), 2)
                       ELSE NULL
                   END AS attendance_pct
            FROM absensi
            WHERE tanggal_absensi BETWEEN ? AND ?
              AND id_kelas IN (
                  SELECT id_kelas FROM penugasan_wali_kelas WHERE id_guru = ? AND aktif = TRUE
                  UNION
                  SELECT id_kelas FROM penugasan_mengajar WHERE id_guru = ? AND aktif = TRUE
              )
            """;

    private static final String SQL_RINGKASAN_PENILAIAN_ADMIN = """
            SELECT p.id,
                   p.id_penugasan_mengajar,
                   k.nama AS class_name,
                   mp.nama AS subject_name,
                   p.judul,
                   p.jenis_penilaian,
                   p.tanggal_penilaian,
                   p.nilai_maksimal,
                   COUNT(ns.id) AS scored_cnt,
                   CASE WHEN COUNT(ns.id) > 0 THEN ROUND(AVG(ns.nilai), 2) ELSE NULL END AS avg_score,
                   MAX(ns.nilai) AS highest_score,
                   MIN(ns.nilai) AS lowest_score,
                   CASE
                       WHEN COUNT(ns.id) > 0 AND p.nilai_maksimal > 0
                           THEN ROUND(AVG(ns.nilai) * 100.0 / p.nilai_maksimal, 2)
                       ELSE NULL
                   END AS avg_pct
            FROM penilaian p
            JOIN penugasan_mengajar pm ON pm.id = p.id_penugasan_mengajar
            JOIN kelas k ON k.id = pm.id_kelas
            JOIN mata_pelajaran mp ON mp.id = pm.id_mata_pelajaran
            LEFT JOIN nilai_siswa ns ON ns.id_penilaian = p.id
            GROUP BY p.id, p.id_penugasan_mengajar, k.nama, mp.nama, p.judul,
                     p.jenis_penilaian, p.tanggal_penilaian, p.nilai_maksimal
            ORDER BY p.tanggal_penilaian DESC, p.id DESC
            """;

    private static final String SQL_RINGKASAN_PENILAIAN_GURU = """
            SELECT p.id,
                   p.id_penugasan_mengajar,
                   k.nama AS class_name,
                   mp.nama AS subject_name,
                   p.judul,
                   p.jenis_penilaian,
                   p.tanggal_penilaian,
                   p.nilai_maksimal,
                   COUNT(ns.id) AS scored_cnt,
                   CASE WHEN COUNT(ns.id) > 0 THEN ROUND(AVG(ns.nilai), 2) ELSE NULL END AS avg_score,
                   MAX(ns.nilai) AS highest_score,
                   MIN(ns.nilai) AS lowest_score,
                   CASE
                       WHEN COUNT(ns.id) > 0 AND p.nilai_maksimal > 0
                           THEN ROUND(AVG(ns.nilai) * 100.0 / p.nilai_maksimal, 2)
                       ELSE NULL
                   END AS avg_pct
            FROM penilaian p
            JOIN penugasan_mengajar pm ON pm.id = p.id_penugasan_mengajar
            JOIN kelas k ON k.id = pm.id_kelas
            JOIN mata_pelajaran mp ON mp.id = pm.id_mata_pelajaran
            LEFT JOIN nilai_siswa ns ON ns.id_penilaian = p.id
            WHERE pm.id_guru = ?
            GROUP BY p.id, p.id_penugasan_mengajar, k.nama, mp.nama, p.judul,
                     p.jenis_penilaian, p.tanggal_penilaian, p.nilai_maksimal
            ORDER BY p.tanggal_penilaian DESC, p.id DESC
            """;

    private static final String SQL_RATA_RATA_MAPEL_ADMIN = """
            SELECT mp.id,
                   mp.kode,
                   mp.nama,
                   COUNT(DISTINCT p.id) AS assessment_cnt,
                   COUNT(ns.id) AS scored_cnt,
                   CASE WHEN COUNT(ns.id) > 0 THEN ROUND(AVG(ns.nilai), 2) ELSE NULL END AS avg_score,
                   CASE
                       WHEN COALESCE(SUM(CASE WHEN ns.id IS NOT NULL THEN p.nilai_maksimal ELSE 0 END), 0) > 0
                           THEN ROUND(SUM(ns.nilai) * 100.0 / SUM(CASE WHEN ns.id IS NOT NULL THEN p.nilai_maksimal ELSE 0 END), 2)
                       ELSE NULL
                   END AS avg_pct
            FROM mata_pelajaran mp
            LEFT JOIN penugasan_mengajar pm ON pm.id_mata_pelajaran = mp.id
            LEFT JOIN penilaian p ON p.id_penugasan_mengajar = pm.id
            LEFT JOIN nilai_siswa ns ON ns.id_penilaian = p.id
            WHERE mp.aktif = TRUE
            GROUP BY mp.id, mp.kode, mp.nama
            ORDER BY mp.nama ASC, mp.id ASC
            """;

    private static final String SQL_RATA_RATA_MAPEL_GURU = """
            SELECT mp.id,
                   mp.kode,
                   mp.nama,
                   COUNT(DISTINCT p.id) AS assessment_cnt,
                   COUNT(ns.id) AS scored_cnt,
                   CASE WHEN COUNT(ns.id) > 0 THEN ROUND(AVG(ns.nilai), 2) ELSE NULL END AS avg_score,
                   CASE
                       WHEN COALESCE(SUM(CASE WHEN ns.id IS NOT NULL THEN p.nilai_maksimal ELSE 0 END), 0) > 0
                           THEN ROUND(SUM(ns.nilai) * 100.0 / SUM(CASE WHEN ns.id IS NOT NULL THEN p.nilai_maksimal ELSE 0 END), 2)
                       ELSE NULL
                   END AS avg_pct
            FROM mata_pelajaran mp
            LEFT JOIN penugasan_mengajar pm ON pm.id_mata_pelajaran = mp.id AND pm.id_guru = ?
            LEFT JOIN penilaian p ON p.id_penugasan_mengajar = pm.id
            LEFT JOIN nilai_siswa ns ON ns.id_penilaian = p.id
            WHERE mp.aktif = TRUE
              AND EXISTS (
                  SELECT 1 FROM penugasan_mengajar pmx
                  WHERE pmx.id_mata_pelajaran = mp.id AND pmx.id_guru = ?
              )
            GROUP BY mp.id, mp.kode, mp.nama
            ORDER BY mp.nama ASC, mp.id ASC
            """;

    @Override
    public DashboardSummary getDashboardSummary(Long idCakupanGuru) {
        try (Connection koneksi = DatabaseConfig.getConnection()) {
            int siswaAktif = 0;
            int siswaTidakAktif = 0;
            if (idCakupanGuru == null) {
                try (PreparedStatement pernyataan = koneksi.prepareStatement("""
                        SELECT COUNT(*) FILTER (WHERE aktif = TRUE) AS active_cnt,
                               COUNT(*) FILTER (WHERE aktif = FALSE) AS inactive_cnt
                        FROM siswa
                        """);
                     ResultSet hasilQuery = pernyataan.executeQuery()) {
                    if (hasilQuery.next()) {
                        siswaAktif = hasilQuery.getInt("active_cnt");
                        siswaTidakAktif = hasilQuery.getInt("inactive_cnt");
                    }
                }
            } else {
                String sql = """
                        SELECT COUNT(DISTINCT s.id) FILTER (WHERE s.aktif = TRUE) AS active_cnt,
                               COUNT(DISTINCT s.id) FILTER (WHERE s.aktif = FALSE) AS inactive_cnt
                        FROM siswa s
                        JOIN riwayat_kelas_siswa rks
                          ON rks.id_siswa = s.id
                         AND rks.status = 'AKTIF'
                         AND rks.tanggal_keluar IS NULL
                        WHERE rks.id_kelas IN (""" + SQL_ID_KELAS_GURU + ")";
                try (PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {
                    pernyataan.setLong(1, idCakupanGuru);
                    pernyataan.setLong(2, idCakupanGuru);
                    try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                        if (hasilQuery.next()) {
                            siswaAktif = hasilQuery.getInt("active_cnt");
                            siswaTidakAktif = hasilQuery.getInt("inactive_cnt");
                        }
                    }
                }
            }

            int guruAktif = 0;
            int guruTidakAktif = 0;
            if (idCakupanGuru == null) {
                try (PreparedStatement pernyataan = koneksi.prepareStatement("""
                        SELECT COUNT(*) FILTER (WHERE aktif = TRUE) AS active_cnt,
                               COUNT(*) FILTER (WHERE aktif = FALSE) AS inactive_cnt
                        FROM guru
                        """);
                     ResultSet hasilQuery = pernyataan.executeQuery()) {
                    if (hasilQuery.next()) {
                        guruAktif = hasilQuery.getInt("active_cnt");
                        guruTidakAktif = hasilQuery.getInt("inactive_cnt");
                    }
                }
            } else {
                try (PreparedStatement pernyataan = koneksi.prepareStatement("""
                        SELECT COUNT(*) FILTER (WHERE aktif = TRUE) AS active_cnt,
                               COUNT(*) FILTER (WHERE aktif = FALSE) AS inactive_cnt
                        FROM guru
                        WHERE id = ?
                        """)) {
                    pernyataan.setLong(1, idCakupanGuru);
                    try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                        if (hasilQuery.next()) {
                            guruAktif = hasilQuery.getInt("active_cnt");
                            guruTidakAktif = hasilQuery.getInt("inactive_cnt");
                        }
                    }
                }
            }

            int kelasAktif = 0;
            if (idCakupanGuru == null) {
                kelasAktif = kueriSatuInteger(koneksi, "SELECT COUNT(*) FROM kelas WHERE aktif = TRUE");
            } else {
                String sql = "SELECT COUNT(*) FROM kelas WHERE aktif = TRUE AND id IN ("
                        + SQL_ID_KELAS_GURU + ")";
                try (PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {
                    pernyataan.setLong(1, idCakupanGuru);
                    pernyataan.setLong(2, idCakupanGuru);
                    try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                        if (hasilQuery.next()) {
                            kelasAktif = hasilQuery.getInt(1);
                        }
                    }
                }
            }

            int mapelAktif = 0;
            if (idCakupanGuru == null) {
                mapelAktif = kueriSatuInteger(koneksi, "SELECT COUNT(*) FROM mata_pelajaran WHERE aktif = TRUE");
            } else {
                try (PreparedStatement pernyataan = koneksi.prepareStatement("""
                        SELECT COUNT(DISTINCT mp.id)
                        FROM mata_pelajaran mp
                        JOIN penugasan_mengajar pm ON pm.id_mata_pelajaran = mp.id
                        WHERE mp.aktif = TRUE AND pm.aktif = TRUE AND pm.id_guru = ?
                        """)) {
                    pernyataan.setLong(1, idCakupanGuru);
                    try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                        if (hasilQuery.next()) {
                            mapelAktif = hasilQuery.getInt(1);
                        }
                    }
                }
            }

            String namaTahunAktif = "-";
            try (PreparedStatement pernyataan = koneksi.prepareStatement(
                    "SELECT nama FROM tahun_ajaran WHERE aktif = TRUE LIMIT 1");
                 ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    namaTahunAktif = hasilQuery.getString("nama");
                }
            }

            int jumlahSemesterAktif = kueriSatuInteger(koneksi, "SELECT COUNT(*) FROM semester WHERE aktif = TRUE");

            int jumlahWaliKelas;
            int jumlahPenugasanMengajar;
            int jumlahPenilaian;
            if (idCakupanGuru == null) {
                jumlahWaliKelas = kueriSatuInteger(koneksi,
                        "SELECT COUNT(*) FROM penugasan_wali_kelas WHERE aktif = TRUE");
                jumlahPenugasanMengajar = kueriSatuInteger(koneksi,
                        "SELECT COUNT(*) FROM penugasan_mengajar WHERE aktif = TRUE");
                jumlahPenilaian = kueriSatuInteger(koneksi, "SELECT COUNT(*) FROM penilaian");
            } else {
                jumlahWaliKelas = kueriSatuIntegerDenganLong(koneksi,
                        "SELECT COUNT(*) FROM penugasan_wali_kelas WHERE aktif = TRUE AND id_guru = ?",
                        idCakupanGuru);
                jumlahPenugasanMengajar = kueriSatuIntegerDenganLong(koneksi,
                        "SELECT COUNT(*) FROM penugasan_mengajar WHERE aktif = TRUE AND id_guru = ?",
                        idCakupanGuru);
                jumlahPenilaian = kueriSatuIntegerDenganLong(koneksi, """
                        SELECT COUNT(*)
                        FROM penilaian p
                        JOIN penugasan_mengajar pm ON pm.id = p.id_penugasan_mengajar
                        WHERE pm.id_guru = ?
                        """, idCakupanGuru);
            }

            int totalCatatanAbsensi = 0;
            BigDecimal persentaseAbsensi = null;
            String sqlAbsensi;
            if (idCakupanGuru == null) {
                sqlAbsensi = """
                      SELECT COUNT(*) AS total_cnt,
                             CASE WHEN COUNT(*) > 0
                                  THEN ROUND(COUNT(*) FILTER (WHERE status = 'HADIR') * 100.0 / COUNT(*), 2)
                                  ELSE NULL
                             END AS pct
                      FROM absensi
                      """;
            } else {
                sqlAbsensi = """
                      SELECT COUNT(*) AS total_cnt,
                             CASE WHEN COUNT(*) > 0
                                  THEN ROUND(COUNT(*) FILTER (WHERE status = 'HADIR') * 100.0 / COUNT(*), 2)
                                  ELSE NULL
                             END AS pct
                      FROM absensi
                      WHERE id_kelas IN (""" + SQL_ID_KELAS_GURU + ")";
            }

            try (PreparedStatement pernyataan = koneksi.prepareStatement(sqlAbsensi)) {
                if (idCakupanGuru != null) {
                    pernyataan.setLong(1, idCakupanGuru);
                    pernyataan.setLong(2, idCakupanGuru);
                }
                try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                    if (hasilQuery.next()) {
                        totalCatatanAbsensi = hasilQuery.getInt("total_cnt");
                        persentaseAbsensi = hasilQuery.getBigDecimal("pct");
                    }
                }
            }

            String labelCakupan = idCakupanGuru == null ? "Sekolah (Admin)" : "Guru (Kelas Diampu)";
            return new DashboardSummary(
                    labelCakupan,
                    siswaAktif,
                    siswaTidakAktif,
                    guruAktif,
                    guruTidakAktif,
                    kelasAktif,
                    mapelAktif,
                    namaTahunAktif,
                    jumlahSemesterAktif,
                    jumlahWaliKelas,
                    jumlahPenugasanMengajar,
                    jumlahPenilaian,
                    totalCatatanAbsensi,
                    persentaseAbsensi);
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil ringkasan dashboard dari database.", e);
        }
    }

    @Override
    public List<ClassStudentCountReport> findActiveStudentCountsByClass(Long idCakupanGuru) {
        String sql = idCakupanGuru == null
                ? SQL_JUMLAH_SISWA_KELAS_ADMIN
                : SQL_JUMLAH_SISWA_KELAS_GURU;

        List<ClassStudentCountReport> daftarLaporan = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {

            if (idCakupanGuru != null) {
                pernyataan.setLong(1, idCakupanGuru);
                pernyataan.setLong(2, idCakupanGuru);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarLaporan.add(new ClassStudentCountReport(
                            hasilQuery.getLong("id"),
                            hasilQuery.getString("nama"),
                            hasilQuery.getInt("tingkat_kelas"),
                            hasilQuery.getString("tahun_ajaran_nama"),
                            hasilQuery.getInt("kapasitas"),
                            hasilQuery.getInt("active_student_count")));
                }
            }
            return daftarLaporan;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil laporan jumlah siswa per kelas.", e);
        }
    }

    @Override
    public List<GenderStudentCountReport> findActiveStudentCountsByGender(Long idCakupanGuru) {
        String sql = idCakupanGuru == null
                ? SQL_JUMLAH_GENDER_ADMIN
                : SQL_JUMLAH_GENDER_GURU;

        List<GenderStudentCountReport> daftarLaporan = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {

            if (idCakupanGuru != null) {
                pernyataan.setLong(1, idCakupanGuru);
                pernyataan.setLong(2, idCakupanGuru);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarLaporan.add(new GenderStudentCountReport(
                            hasilQuery.getString("code"),
                            hasilQuery.getString("label"),
                            hasilQuery.getInt("active_student_count")));
                }
            }
            return daftarLaporan;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil laporan jumlah siswa per jenis kelamin.", e);
        }
    }

    @Override
    public AttendanceRangeReport findAttendanceSummaryByDateRange(
            LocalDate tanggalMulai,
            LocalDate tanggalSelesai,
            Long idCakupanGuru) {

        String sql = idCakupanGuru == null
                ? SQL_ABSENSI_RENTANG_ADMIN
                : SQL_ABSENSI_RENTANG_GURU;

        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {

            pernyataan.setDate(1, Date.valueOf(tanggalMulai));
            pernyataan.setDate(2, Date.valueOf(tanggalSelesai));
            if (idCakupanGuru != null) {
                pernyataan.setLong(3, idCakupanGuru);
                pernyataan.setLong(4, idCakupanGuru);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    return new AttendanceRangeReport(
                            tanggalMulai,
                            tanggalSelesai,
                            hasilQuery.getInt("total_records"),
                            hasilQuery.getInt("hadir_count"),
                            hasilQuery.getInt("sakit_count"),
                            hasilQuery.getInt("izin_count"),
                            hasilQuery.getInt("alpa_count"),
                            hasilQuery.getBigDecimal("attendance_pct"));
                }
                return new AttendanceRangeReport(tanggalMulai, tanggalSelesai, 0, 0, 0, 0, 0, null);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rekapitulasi absensi berdasarkan rentang tanggal.", e);
        }
    }

    @Override
    public List<ClassAttendanceReport> findAttendanceSummariesByClass(
            LocalDate tanggalMulai,
            LocalDate tanggalSelesai,
            Long idCakupanGuru) {

        boolean punyaRentangTanggal = tanggalMulai != null && tanggalSelesai != null;
        StringBuilder sqlBuilder = new StringBuilder("""
                SELECT k.id,
                       k.nama,
                       k.tingkat_kelas,
                       COUNT(a.id) AS total_records,
                       COUNT(a.id) FILTER (WHERE a.status = 'HADIR') AS hadir_count,
                       COUNT(a.id) FILTER (WHERE a.status = 'SAKIT') AS sakit_count,
                       COUNT(a.id) FILTER (WHERE a.status = 'IZIN') AS izin_count,
                       COUNT(a.id) FILTER (WHERE a.status = 'ALPA') AS alpa_count,
                       CASE
                           WHEN COUNT(a.id) > 0
                               THEN ROUND(COUNT(a.id) FILTER (WHERE a.status = 'HADIR') * 100.0 / COUNT(a.id), 2)
                           ELSE NULL
                       END AS attendance_pct
                FROM kelas k
                LEFT JOIN absensi a ON a.id_kelas = k.id
                """);
        if (punyaRentangTanggal) {
            sqlBuilder.append(" AND a.tanggal_absensi BETWEEN ? AND ?\n");
        }
        sqlBuilder.append(" WHERE k.aktif = TRUE\n");
        if (idCakupanGuru != null) {
            sqlBuilder.append(" AND k.id IN (").append(SQL_ID_KELAS_GURU).append(")\n");
        }
        sqlBuilder.append("""
                GROUP BY k.id, k.nama, k.tingkat_kelas
                ORDER BY k.tingkat_kelas ASC, k.nama ASC, k.id ASC
                """);

        List<ClassAttendanceReport> daftarLaporan = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sqlBuilder.toString())) {

            int indeksParameter = 1;
            if (punyaRentangTanggal) {
                pernyataan.setDate(indeksParameter++, Date.valueOf(tanggalMulai));
                pernyataan.setDate(indeksParameter++, Date.valueOf(tanggalSelesai));
            }
            if (idCakupanGuru != null) {
                pernyataan.setLong(indeksParameter++, idCakupanGuru);
                pernyataan.setLong(indeksParameter, idCakupanGuru);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarLaporan.add(new ClassAttendanceReport(
                            hasilQuery.getLong("id"),
                            hasilQuery.getString("nama"),
                            hasilQuery.getInt("tingkat_kelas"),
                            hasilQuery.getInt("total_records"),
                            hasilQuery.getInt("hadir_count"),
                            hasilQuery.getInt("sakit_count"),
                            hasilQuery.getInt("izin_count"),
                            hasilQuery.getInt("alpa_count"),
                            hasilQuery.getBigDecimal("attendance_pct")));
                }
            }
            return daftarLaporan;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rekapitulasi absensi per kelas.", e);
        }
    }

    @Override
    public List<AssessmentReport> findAssessmentSummaries(Long idCakupanGuru) {
        String sql = idCakupanGuru == null
                ? SQL_RINGKASAN_PENILAIAN_ADMIN
                : SQL_RINGKASAN_PENILAIAN_GURU;

        List<AssessmentReport> daftarLaporan = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {

            if (idCakupanGuru != null) {
                pernyataan.setLong(1, idCakupanGuru);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    Date tanggalSql = hasilQuery.getDate("tanggal_penilaian");
                    daftarLaporan.add(new AssessmentReport(
                            hasilQuery.getLong("id"),
                            hasilQuery.getLong("id_penugasan_mengajar"),
                            hasilQuery.getString("class_name"),
                            hasilQuery.getString("subject_name"),
                            hasilQuery.getString("judul"),
                            hasilQuery.getString("jenis_penilaian"),
                            tanggalSql != null ? tanggalSql.toLocalDate() : null,
                            hasilQuery.getBigDecimal("nilai_maksimal"),
                            hasilQuery.getInt("scored_cnt"),
                            hasilQuery.getBigDecimal("avg_score"),
                            hasilQuery.getBigDecimal("highest_score"),
                            hasilQuery.getBigDecimal("lowest_score"),
                            hasilQuery.getBigDecimal("avg_pct")));
                }
            }
            return daftarLaporan;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil ringkasan penilaian.", e);
        }
    }

    @Override
    public List<StudentScoreReport> findStudentScoreSummaries(Long idCakupanGuru) {
        return kueriRingkasanNilaiSiswa(null, idCakupanGuru);
    }

    @Override
    public Optional<StudentScoreReport> findStudentScoreSummaryByStudentId(Long idSiswa, Long idCakupanGuru) {
        if (idSiswa == null) {
            return Optional.empty();
        }
        List<StudentScoreReport> baris = kueriRingkasanNilaiSiswa(idSiswa, idCakupanGuru);
        if (baris.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(baris.get(0));
    }

    @Override
    public List<SubjectScoreReport> findAverageScoresBySubject(Long idCakupanGuru) {
        String sql = idCakupanGuru == null
                ? SQL_RATA_RATA_MAPEL_ADMIN
                : SQL_RATA_RATA_MAPEL_GURU;

        List<SubjectScoreReport> daftarLaporan = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {

            if (idCakupanGuru != null) {
                pernyataan.setLong(1, idCakupanGuru);
                pernyataan.setLong(2, idCakupanGuru);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarLaporan.add(new SubjectScoreReport(
                            hasilQuery.getLong("id"),
                            hasilQuery.getString("kode"),
                            hasilQuery.getString("nama"),
                            hasilQuery.getInt("assessment_cnt"),
                            hasilQuery.getInt("scored_cnt"),
                            hasilQuery.getBigDecimal("avg_score"),
                            hasilQuery.getBigDecimal("avg_pct")));
                }
            }
            return daftarLaporan;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rata-rata nilai per mata pelajaran.", e);
        }
    }

    private List<StudentScoreReport> kueriRingkasanNilaiSiswa(Long filterIdSiswa, Long idCakupanGuru) {
        StringBuilder sqlBuilder = new StringBuilder("""
                SELECT s.id,
                       s.nis,
                       s.nama_lengkap,
                       COALESCE(MAX(k.nama), '-') AS current_class_name,
                       COUNT(sc.score_id) AS assessment_cnt,
                       COALESCE(SUM(sc.nilai), 0) AS total_earned,
                       COALESCE(SUM(sc.nilai_maksimal), 0) AS total_max,
                       CASE WHEN COUNT(sc.score_id) > 0 THEN ROUND(AVG(sc.nilai), 2) ELSE NULL END AS avg_score,
                       CASE
                           WHEN COALESCE(SUM(sc.nilai_maksimal), 0) > 0
                               THEN ROUND(SUM(sc.nilai) * 100.0 / SUM(sc.nilai_maksimal), 2)
                           ELSE NULL
                       END AS overall_pct
                FROM siswa s
                LEFT JOIN riwayat_kelas_siswa rks
                       ON rks.id_siswa = s.id
                      AND rks.status = 'AKTIF'
                      AND rks.tanggal_keluar IS NULL
                LEFT JOIN kelas k ON k.id = rks.id_kelas
                """);

        if (idCakupanGuru == null) {
            sqlBuilder.append("""
                    LEFT JOIN (
                        SELECT ns.id AS score_id, ns.id_siswa, ns.nilai, p.nilai_maksimal
                        FROM nilai_siswa ns
                        JOIN penilaian p ON p.id = ns.id_penilaian
                    ) sc ON sc.id_siswa = s.id
                    WHERE s.aktif = TRUE
                    """);
        } else {
            sqlBuilder.append("""
                    LEFT JOIN (
                        SELECT ns.id AS score_id, ns.id_siswa, ns.nilai, p.nilai_maksimal
                        FROM nilai_siswa ns
                        JOIN penilaian p ON p.id = ns.id_penilaian
                        JOIN penugasan_mengajar pm ON pm.id = p.id_penugasan_mengajar
                        WHERE pm.id_guru = ?
                    ) sc ON sc.id_siswa = s.id
                    WHERE s.aktif = TRUE
                      AND (
                          rks.id_kelas IN (
                              SELECT id_kelas FROM penugasan_wali_kelas WHERE id_guru = ? AND aktif = TRUE
                              UNION
                              SELECT id_kelas FROM penugasan_mengajar WHERE id_guru = ? AND aktif = TRUE
                          )
                          OR sc.score_id IS NOT NULL
                      )
                    """);
        }

        if (filterIdSiswa != null) {
            sqlBuilder.append(" AND s.id = ?\n");
        }

        sqlBuilder.append("""
                GROUP BY s.id, s.nis, s.nama_lengkap
                ORDER BY s.nama_lengkap ASC, s.id ASC
                """);

        List<StudentScoreReport> daftarLaporan = new ArrayList<>();
        try (Connection koneksi = DatabaseConfig.getConnection();
             PreparedStatement pernyataan = koneksi.prepareStatement(sqlBuilder.toString())) {

            int indeksParameter = 1;
            if (idCakupanGuru != null) {
                pernyataan.setLong(indeksParameter++, idCakupanGuru);
                pernyataan.setLong(indeksParameter++, idCakupanGuru);
                pernyataan.setLong(indeksParameter++, idCakupanGuru);
            }
            if (filterIdSiswa != null) {
                pernyataan.setLong(indeksParameter, filterIdSiswa);
            }

            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                while (hasilQuery.next()) {
                    daftarLaporan.add(new StudentScoreReport(
                            hasilQuery.getLong("id"),
                            hasilQuery.getString("nis"),
                            hasilQuery.getString("nama_lengkap"),
                            JdbcUtils.emptyIfNull(hasilQuery.getString("current_class_name")),
                            hasilQuery.getInt("assessment_cnt"),
                            hasilQuery.getBigDecimal("total_earned"),
                            hasilQuery.getBigDecimal("total_max"),
                            hasilQuery.getBigDecimal("avg_score"),
                            hasilQuery.getBigDecimal("overall_pct")));
                }
            }
            return daftarLaporan;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil ringkasan nilai siswa.", e);
        }
    }

    private int kueriSatuInteger(Connection koneksi, String sql) throws SQLException {
        try (PreparedStatement pernyataan = koneksi.prepareStatement(sql);
             ResultSet hasilQuery = pernyataan.executeQuery()) {
            if (hasilQuery.next()) {
                return hasilQuery.getInt(1);
            }
            return 0;
        }
    }

    private int kueriSatuIntegerDenganLong(Connection koneksi, String sql, Long parameter) throws SQLException {
        try (PreparedStatement pernyataan = koneksi.prepareStatement(sql)) {
            pernyataan.setLong(1, parameter);
            try (ResultSet hasilQuery = pernyataan.executeQuery()) {
                if (hasilQuery.next()) {
                    return hasilQuery.getInt(1);
                }
                return 0;
            }
        }
    }
}
