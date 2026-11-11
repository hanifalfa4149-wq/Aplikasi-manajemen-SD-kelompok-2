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

    private static final String TEACHER_CLASS_IDS_SQL = """
            SELECT id_kelas FROM penugasan_wali_kelas WHERE id_guru = ? AND aktif = TRUE
            UNION
            SELECT id_kelas FROM penugasan_mengajar WHERE id_guru = ? AND aktif = TRUE
            """;

    private static final String SQL_CLASS_STUDENT_COUNTS_ADMIN = """
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

    private static final String SQL_CLASS_STUDENT_COUNTS_TEACHER = """
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

    private static final String SQL_GENDER_COUNTS_ADMIN = """
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

    private static final String SQL_GENDER_COUNTS_TEACHER = """
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

    private static final String SQL_ATTENDANCE_RANGE_ADMIN = """
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

    private static final String SQL_ATTENDANCE_RANGE_TEACHER = """
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

    private static final String SQL_ASSESSMENT_SUMMARIES_ADMIN = """
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

    private static final String SQL_ASSESSMENT_SUMMARIES_TEACHER = """
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

    private static final String SQL_SUBJECT_AVERAGE_ADMIN = """
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

    private static final String SQL_SUBJECT_AVERAGE_TEACHER = """
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
    public DashboardSummary getDashboardSummary(Long teacherIdScope) {
        try (Connection conn = DatabaseConfig.getConnection()) {
            int activeStudents = 0;
            int inactiveStudents = 0;
            if (teacherIdScope == null) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT COUNT(*) FILTER (WHERE aktif = TRUE) AS active_cnt,
                               COUNT(*) FILTER (WHERE aktif = FALSE) AS inactive_cnt
                        FROM siswa
                        """);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeStudents = rs.getInt("active_cnt");
                        inactiveStudents = rs.getInt("inactive_cnt");
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
                        WHERE rks.id_kelas IN (""" + TEACHER_CLASS_IDS_SQL + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, teacherIdScope);
                    ps.setLong(2, teacherIdScope);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            activeStudents = rs.getInt("active_cnt");
                            inactiveStudents = rs.getInt("inactive_cnt");
                        }
                    }
                }
            }

            int activeTeachers = 0;
            int inactiveTeachers = 0;
            if (teacherIdScope == null) {
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT COUNT(*) FILTER (WHERE aktif = TRUE) AS active_cnt,
                               COUNT(*) FILTER (WHERE aktif = FALSE) AS inactive_cnt
                        FROM guru
                        """);
                     ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        activeTeachers = rs.getInt("active_cnt");
                        inactiveTeachers = rs.getInt("inactive_cnt");
                    }
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT COUNT(*) FILTER (WHERE aktif = TRUE) AS active_cnt,
                               COUNT(*) FILTER (WHERE aktif = FALSE) AS inactive_cnt
                        FROM guru
                        WHERE id = ?
                        """)) {
                    ps.setLong(1, teacherIdScope);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            activeTeachers = rs.getInt("active_cnt");
                            inactiveTeachers = rs.getInt("inactive_cnt");
                        }
                    }
                }
            }

            int activeClasses = 0;
            if (teacherIdScope == null) {
                activeClasses = querySingleInt(conn, "SELECT COUNT(*) FROM kelas WHERE aktif = TRUE");
            } else {
                String sql = "SELECT COUNT(*) FROM kelas WHERE aktif = TRUE AND id IN ("
                        + TEACHER_CLASS_IDS_SQL + ")";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setLong(1, teacherIdScope);
                    ps.setLong(2, teacherIdScope);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            activeClasses = rs.getInt(1);
                        }
                    }
                }
            }

            int activeSubjects;
            if (teacherIdScope == null) {
                activeSubjects = querySingleInt(conn, "SELECT COUNT(*) FROM mata_pelajaran WHERE aktif = TRUE");
            } else {
                try (PreparedStatement ps = conn.prepareStatement("""
                        SELECT COUNT(DISTINCT mp.id)
                        FROM mata_pelajaran mp
                        JOIN penugasan_mengajar pm ON pm.id_mata_pelajaran = mp.id
                        WHERE mp.aktif = TRUE AND pm.aktif = TRUE AND pm.id_guru = ?
                        """)) {
                    ps.setLong(1, teacherIdScope);
                    try (ResultSet rs = ps.executeQuery()) {
                        activeSubjects = rs.next() ? rs.getInt(1) : 0;
                    }
                }
            }

            String activeYearName = "-";
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT nama FROM tahun_ajaran WHERE aktif = TRUE LIMIT 1");
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    activeYearName = rs.getString("nama");
                }
            }

            int activeSemesters = querySingleInt(conn, "SELECT COUNT(*) FROM semester WHERE aktif = TRUE");

            int homeroomCount;
            int teachingCount;
            int assessmentsCount;
            if (teacherIdScope == null) {
                homeroomCount = querySingleInt(conn,
                        "SELECT COUNT(*) FROM penugasan_wali_kelas WHERE aktif = TRUE");
                teachingCount = querySingleInt(conn,
                        "SELECT COUNT(*) FROM penugasan_mengajar WHERE aktif = TRUE");
                assessmentsCount = querySingleInt(conn, "SELECT COUNT(*) FROM penilaian");
            } else {
                homeroomCount = querySingleIntWithLong(conn,
                        "SELECT COUNT(*) FROM penugasan_wali_kelas WHERE aktif = TRUE AND id_guru = ?",
                        teacherIdScope);
                teachingCount = querySingleIntWithLong(conn,
                        "SELECT COUNT(*) FROM penugasan_mengajar WHERE aktif = TRUE AND id_guru = ?",
                        teacherIdScope);
                assessmentsCount = querySingleIntWithLong(conn, """
                        SELECT COUNT(*)
                        FROM penilaian p
                        JOIN penugasan_mengajar pm ON pm.id = p.id_penugasan_mengajar
                        WHERE pm.id_guru = ?
                        """, teacherIdScope);
            }

            int totalAttendanceRecords = 0;
            BigDecimal overallAttPct = null;
            String attSql = teacherIdScope == null
                    ? """
                      SELECT COUNT(*) AS total_cnt,
                             CASE WHEN COUNT(*) > 0
                                  THEN ROUND(COUNT(*) FILTER (WHERE status = 'HADIR') * 100.0 / COUNT(*), 2)
                                  ELSE NULL
                             END AS pct
                      FROM absensi
                      """
                    : """
                      SELECT COUNT(*) AS total_cnt,
                             CASE WHEN COUNT(*) > 0
                                  THEN ROUND(COUNT(*) FILTER (WHERE status = 'HADIR') * 100.0 / COUNT(*), 2)
                                  ELSE NULL
                             END AS pct
                      FROM absensi
                      WHERE id_kelas IN (""" + TEACHER_CLASS_IDS_SQL + ")";

            try (PreparedStatement ps = conn.prepareStatement(attSql)) {
                if (teacherIdScope != null) {
                    ps.setLong(1, teacherIdScope);
                    ps.setLong(2, teacherIdScope);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        totalAttendanceRecords = rs.getInt("total_cnt");
                        overallAttPct = rs.getBigDecimal("pct");
                    }
                }
            }

            String scopeLabel = teacherIdScope == null ? "Sekolah (Admin)" : "Guru (Kelas Diampu)";
            return new DashboardSummary(
                    scopeLabel,
                    activeStudents,
                    inactiveStudents,
                    activeTeachers,
                    inactiveTeachers,
                    activeClasses,
                    activeSubjects,
                    activeYearName,
                    activeSemesters,
                    homeroomCount,
                    teachingCount,
                    assessmentsCount,
                    totalAttendanceRecords,
                    overallAttPct);
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil ringkasan dashboard dari database.", e);
        }
    }

    @Override
    public List<ClassStudentCountReport> findActiveStudentCountsByClass(Long teacherIdScope) {
        String sql = teacherIdScope == null
                ? SQL_CLASS_STUDENT_COUNTS_ADMIN
                : SQL_CLASS_STUDENT_COUNTS_TEACHER;

        List<ClassStudentCountReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (teacherIdScope != null) {
                ps.setLong(1, teacherIdScope);
                ps.setLong(2, teacherIdScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ClassStudentCountReport(
                            rs.getLong("id"),
                            rs.getString("nama"),
                            rs.getInt("tingkat_kelas"),
                            rs.getString("tahun_ajaran_nama"),
                            rs.getInt("kapasitas"),
                            rs.getInt("active_student_count")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil laporan jumlah siswa per kelas.", e);
        }
    }

    @Override
    public List<GenderStudentCountReport> findActiveStudentCountsByGender(Long teacherIdScope) {
        String sql = teacherIdScope == null
                ? SQL_GENDER_COUNTS_ADMIN
                : SQL_GENDER_COUNTS_TEACHER;

        List<GenderStudentCountReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (teacherIdScope != null) {
                ps.setLong(1, teacherIdScope);
                ps.setLong(2, teacherIdScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new GenderStudentCountReport(
                            rs.getString("code"),
                            rs.getString("label"),
                            rs.getInt("active_student_count")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil laporan jumlah siswa per jenis kelamin.", e);
        }
    }

    @Override
    public AttendanceRangeReport findAttendanceSummaryByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Long teacherIdScope) {

        String sql = teacherIdScope == null
                ? SQL_ATTENDANCE_RANGE_ADMIN
                : SQL_ATTENDANCE_RANGE_TEACHER;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(startDate));
            ps.setDate(2, Date.valueOf(endDate));
            if (teacherIdScope != null) {
                ps.setLong(3, teacherIdScope);
                ps.setLong(4, teacherIdScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AttendanceRangeReport(
                            startDate,
                            endDate,
                            rs.getInt("total_records"),
                            rs.getInt("hadir_count"),
                            rs.getInt("sakit_count"),
                            rs.getInt("izin_count"),
                            rs.getInt("alpa_count"),
                            rs.getBigDecimal("attendance_pct"));
                }
                return new AttendanceRangeReport(startDate, endDate, 0, 0, 0, 0, 0, null);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rekapitulasi absensi berdasarkan rentang tanggal.", e);
        }
    }

    @Override
    public List<ClassAttendanceReport> findAttendanceSummariesByClass(
            LocalDate startDate,
            LocalDate endDate,
            Long teacherIdScope) {

        boolean hasDateRange = startDate != null && endDate != null;
        StringBuilder sql = new StringBuilder("""
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
        if (hasDateRange) {
            sql.append(" AND a.tanggal_absensi BETWEEN ? AND ?\n");
        }
        sql.append(" WHERE k.aktif = TRUE\n");
        if (teacherIdScope != null) {
            sql.append(" AND k.id IN (").append(TEACHER_CLASS_IDS_SQL).append(")\n");
        }
        sql.append("""
                GROUP BY k.id, k.nama, k.tingkat_kelas
                ORDER BY k.tingkat_kelas ASC, k.nama ASC, k.id ASC
                """);

        List<ClassAttendanceReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (hasDateRange) {
                ps.setDate(paramIdx++, Date.valueOf(startDate));
                ps.setDate(paramIdx++, Date.valueOf(endDate));
            }
            if (teacherIdScope != null) {
                ps.setLong(paramIdx++, teacherIdScope);
                ps.setLong(paramIdx, teacherIdScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new ClassAttendanceReport(
                            rs.getLong("id"),
                            rs.getString("nama"),
                            rs.getInt("tingkat_kelas"),
                            rs.getInt("total_records"),
                            rs.getInt("hadir_count"),
                            rs.getInt("sakit_count"),
                            rs.getInt("izin_count"),
                            rs.getInt("alpa_count"),
                            rs.getBigDecimal("attendance_pct")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rekapitulasi absensi per kelas.", e);
        }
    }

    @Override
    public List<AssessmentReport> findAssessmentSummaries(Long teacherIdScope) {
        String sql = teacherIdScope == null
                ? SQL_ASSESSMENT_SUMMARIES_ADMIN
                : SQL_ASSESSMENT_SUMMARIES_TEACHER;

        List<AssessmentReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (teacherIdScope != null) {
                ps.setLong(1, teacherIdScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Date date = rs.getDate("tanggal_penilaian");
                    list.add(new AssessmentReport(
                            rs.getLong("id"),
                            rs.getLong("id_penugasan_mengajar"),
                            rs.getString("class_name"),
                            rs.getString("subject_name"),
                            rs.getString("judul"),
                            rs.getString("jenis_penilaian"),
                            date != null ? date.toLocalDate() : null,
                            rs.getBigDecimal("nilai_maksimal"),
                            rs.getInt("scored_cnt"),
                            rs.getBigDecimal("avg_score"),
                            rs.getBigDecimal("highest_score"),
                            rs.getBigDecimal("lowest_score"),
                            rs.getBigDecimal("avg_pct")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil ringkasan penilaian.", e);
        }
    }

    @Override
    public List<StudentScoreReport> findStudentScoreSummaries(Long teacherIdScope) {
        return queryStudentScoreSummaries(null, teacherIdScope);
    }

    @Override
    public Optional<StudentScoreReport> findStudentScoreSummaryByStudentId(Long studentId, Long teacherIdScope) {
        if (studentId == null) {
            return Optional.empty();
        }
        List<StudentScoreReport> rows = queryStudentScoreSummaries(studentId, teacherIdScope);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public List<SubjectScoreReport> findAverageScoresBySubject(Long teacherIdScope) {
        String sql = teacherIdScope == null
                ? SQL_SUBJECT_AVERAGE_ADMIN
                : SQL_SUBJECT_AVERAGE_TEACHER;

        List<SubjectScoreReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (teacherIdScope != null) {
                ps.setLong(1, teacherIdScope);
                ps.setLong(2, teacherIdScope);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SubjectScoreReport(
                            rs.getLong("id"),
                            rs.getString("kode"),
                            rs.getString("nama"),
                            rs.getInt("assessment_cnt"),
                            rs.getInt("scored_cnt"),
                            rs.getBigDecimal("avg_score"),
                            rs.getBigDecimal("avg_pct")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil rata-rata nilai per mata pelajaran.", e);
        }
    }

    private List<StudentScoreReport> queryStudentScoreSummaries(Long studentIdFilter, Long teacherIdScope) {
        StringBuilder sql = new StringBuilder("""
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

        if (teacherIdScope == null) {
            sql.append("""
                    LEFT JOIN (
                        SELECT ns.id AS score_id, ns.id_siswa, ns.nilai, p.nilai_maksimal
                        FROM nilai_siswa ns
                        JOIN penilaian p ON p.id = ns.id_penilaian
                    ) sc ON sc.id_siswa = s.id
                    WHERE s.aktif = TRUE
                    """);
        } else {
            sql.append("""
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

        if (studentIdFilter != null) {
            sql.append(" AND s.id = ?\n");
        }

        sql.append("""
                GROUP BY s.id, s.nis, s.nama_lengkap
                ORDER BY s.nama_lengkap ASC, s.id ASC
                """);

        List<StudentScoreReport> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (teacherIdScope != null) {
                ps.setLong(paramIdx++, teacherIdScope);
                ps.setLong(paramIdx++, teacherIdScope);
                ps.setLong(paramIdx++, teacherIdScope);
            }
            if (studentIdFilter != null) {
                ps.setLong(paramIdx, studentIdFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new StudentScoreReport(
                            rs.getLong("id"),
                            rs.getString("nis"),
                            rs.getString("nama_lengkap"),
                            JdbcUtils.emptyIfNull(rs.getString("current_class_name")),
                            rs.getInt("assessment_cnt"),
                            rs.getBigDecimal("total_earned"),
                            rs.getBigDecimal("total_max"),
                            rs.getBigDecimal("avg_score"),
                            rs.getBigDecimal("overall_pct")));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil ringkasan nilai siswa.", e);
        }
    }

    private int querySingleInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int querySingleIntWithLong(Connection conn, String sql, Long param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}

