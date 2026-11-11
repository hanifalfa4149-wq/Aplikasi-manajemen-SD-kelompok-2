package id.ac.sekolah.manajemenkelas.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.academic.dao.InMemoryAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.assessment.dao.InMemoryAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.dao.PostgresAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.attendance.dao.InMemoryAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.dao.PostgresAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.dashboard.service.DashboardService;
import id.ac.sekolah.manajemenkelas.report.dao.InMemoryReportDAO;
import id.ac.sekolah.manajemenkelas.report.dao.PostgresReportDAO;
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;
import id.ac.sekolah.manajemenkelas.report.service.ReportService;
import id.ac.sekolah.manajemenkelas.student.dao.InMemoryStudentDAO;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.dao.InMemoryTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

class PostgresReportDAOIntegrationTest {

    private Long previouslyActiveYearId;

    @BeforeEach
    void setupAndClean() throws SQLException {
        SessionContext.getInstance().endSession();
        cleanupTestRows();
        previouslyActiveYearId = findActiveNonTestYearId();
    }

    @AfterEach
    void teardownAndRestore() throws SQLException {
        SessionContext.getInstance().endSession();
        cleanupTestRows();
        if (previouslyActiveYearId != null) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE tahun_ajaran SET aktif = TRUE WHERE id = ?")) {
                ps.setLong(1, previouslyActiveYearId);
                ps.executeUpdate();
            }
        }
    }

    private Long findActiveNonTestYearId() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_P6_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM nilai_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P6_%') "
                            + "OR id_penilaian IN (SELECT id FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P6_%')))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P6_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P6_%'))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM absensi WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P6_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P6_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM riwayat_kelas_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P6_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P6_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P6_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P6_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_wali_kelas WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P6_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P6_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM semester WHERE id_tahun_ajaran IN "
                            + "(SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P6_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_P6_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P6_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_P6_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_P6_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pengguna WHERE nama_pengguna LIKE 'test_p6_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_P6_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM siswa WHERE nis LIKE 'TEST_P6_%'")) {
                ps.executeUpdate();
            }
        }
    }

    private void loginAsSeededAdmin() {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        new AuthBootstrapSeeder(authDAO, hasher).seedDefaultUsers();
        AuthService authService = new AuthService(authDAO, hasher);
        assertTrue(authService.login("admin", "admin123"));
    }

    private void createAndLoginTeacherUser(Long teacherId, String username, String fullName) {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PasswordHasher hasher = new PasswordHasher("PBKDF2WithHmacSHA256", 1000, 256, 16);
        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("guru123", salt);
        User user = new User(null, teacherId, username, hash, salt, fullName, RoleMapper.ROLE_TEACHER, true, null);
        authDAO.save(user);

        AuthService authService = new AuthService(authDAO, hasher);
        assertTrue(authService.login(username, "guru123"));
    }

    @Test
    void postgresDashboardAndAllReportsWorkWithKnownDataAndRoleVisibility() {
        loginAsSeededAdmin();

        PostgresStudentDAO studentDAO = new PostgresStudentDAO();
        PostgresTeacherDAO teacherDAO = new PostgresTeacherDAO();
        PostgresAcademicDAO academicDAO = new PostgresAcademicDAO();
        PostgresAttendanceDAO attendanceDAO = new PostgresAttendanceDAO();
        PostgresAssessmentDAO assessmentDAO = new PostgresAssessmentDAO();
        PostgresReportDAO reportDAO = new PostgresReportDAO();

        StudentService studentService = new StudentService(studentDAO);
        TeacherService teacherService = new TeacherService(teacherDAO);
        AcademicService academicService = new AcademicService(academicDAO, studentDAO, teacherDAO);
        AttendanceService attendanceService = new AttendanceService(attendanceDAO, academicDAO, studentDAO);
        AssessmentService assessmentService = new AssessmentService(assessmentDAO, academicDAO, studentDAO);
        DashboardService dashboardService = new DashboardService(reportDAO);
        ReportService reportService = new ReportService(reportDAO, studentDAO);

        // Baseline counts before adding TEST_P6 records
        DashboardSummary beforeDashboard = dashboardService.getDashboardSummary();
        List<GenderStudentCountReport> beforeGender = reportService.getStudentCountsByGender();
        int beforeMale = beforeGender.get(0).getActiveStudentCount();
        int beforeFemale = beforeGender.get(1).getActiveStudentCount();

        // Setup known test dataset
        Teacher teacherA = teacherService.createTeacher(
                "TEST_P6_NIP_01", "Guru A Report", "P", "", "", "Jl. A", "S1", LocalDate.of(2020, 1, 1));
        Teacher teacherB = teacherService.createTeacher(
                "TEST_P6_NIP_02", "Guru B Report", "L", "", "", "Jl. B", "S1", LocalDate.of(2021, 1, 1));
        Teacher inactiveTeacher = teacherService.createTeacher(
                "TEST_P6_NIP_03", "Guru Nonaktif", "L", "", "", "Jl. C", "S1", LocalDate.of(2019, 1, 1));
        teacherService.deleteTeacher(inactiveTeacher.getId());

        Student s1 = studentService.createStudent(
                "TEST_P6_NIS_01", "TEST_P6_NISN_01", "Ahmad Fajar", "L",
                LocalDate.of(2016, 1, 10), "Pekanbaru", "Jl. 1", "Bapak A", "0811");
        Student s2 = studentService.createStudent(
                "TEST_P6_NIS_02", "TEST_P6_NISN_02", "Bunga Citra", "P",
                LocalDate.of(2016, 2, 10), "Pekanbaru", "Jl. 2", "Ibu B", "0812");
        Student s3 = studentService.createStudent(
                "TEST_P6_NIS_03", "TEST_P6_NISN_03", "Deni Pratama", "L",
                LocalDate.of(2016, 3, 10), "Pekanbaru", "Jl. 3", "Bapak D", "0813");
        Student inactiveStudent = studentService.createStudent(
                "TEST_P6_NIS_04", "TEST_P6_NISN_04", "Siswa Nonaktif", "P",
                LocalDate.of(2016, 4, 10), "Pekanbaru", "Jl. 4", "Ibu X", "0814");
        studentService.deleteStudent(inactiveStudent.getId());

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P6_2026/2027", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        Semester sem1 = academicService.createSemester(
                year.getId(), 1, "Ganjil P6", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P6_1A", 1, 30);
        SchoolClass class1B = academicService.createClass(year.getId(), "TEST_P6_1B", 1, 25);
        Subject math = academicService.createSubject("TEST_P6_MTK", "TEST_P6 Matematika", "Wajib");
        Subject ipa = academicService.createSubject("TEST_P6_IPA", "TEST_P6 IPA", "Wajib");

        academicService.placeStudentInClass(s1.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(s2.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(s3.getId(), class1B.getId(), LocalDate.of(2026, 7, 10));

        TeachingAssignment taA = academicService.createTeachingAssignment(
                teacherA.getId(), class1A.getId(), math.getId(), sem1.getId());
        TeachingAssignment taB = academicService.createTeachingAssignment(
                teacherB.getId(), class1B.getId(), ipa.getId(), sem1.getId());

        // Attendance in unique future test range (2035-09-01..2035-09-02):
        // class1A: 3 HADIR, 1 SAKIT => 4 total, 75.00%
        // class1B: 1 HADIR, 1 ALPA  => 2 total, 50.00%
        LocalDate d1 = LocalDate.of(2035, 9, 1);
        LocalDate d2 = LocalDate.of(2035, 9, 2);
        attendanceService.recordAttendance(s1.getId(), class1A.getId(), d1, Attendance.STATUS_HADIR, "");
        attendanceService.recordAttendance(s2.getId(), class1A.getId(), d1, Attendance.STATUS_SAKIT, "");
        attendanceService.recordAttendance(s1.getId(), class1A.getId(), d2, Attendance.STATUS_HADIR, "");
        attendanceService.recordAttendance(s2.getId(), class1A.getId(), d2, Attendance.STATUS_HADIR, "");
        attendanceService.recordAttendance(s3.getId(), class1B.getId(), d1, Attendance.STATUS_HADIR, "");
        attendanceService.recordAttendance(s3.getId(), class1B.getId(), d2, Attendance.STATUS_ALPA, "");

        // Assessments & Scores:
        // taA Assessment 1 (max 100): s1=80, s2=90 -> avg 85.00, min 80.00, max 90.00, 85.00%
        // taA Assessment 2 (max 50): unscored -> 0 scored, null avg/pct
        // taB Assessment 1 (max 100): s3=70 -> avg 70.00, 70.00%
        Assessment a1 = assessmentService.createAssessment(
                taA.getId(), "TEST_P6 Kuis Mtk", Assessment.TYPE_KUIS,
                LocalDate.of(2026, 8, 10), new BigDecimal("100"), "");
        Assessment a2Empty = assessmentService.createAssessment(
                taA.getId(), "TEST_P6 Tugas Kosong", Assessment.TYPE_TUGAS,
                LocalDate.of(2026, 8, 15), new BigDecimal("50"), "");
        Assessment b1 = assessmentService.createAssessment(
                taB.getId(), "TEST_P6 UTS IPA", Assessment.TYPE_UTS,
                LocalDate.of(2026, 10, 5), new BigDecimal("100"), "");

        assessmentService.recordScore(a1.getId(), s1.getId(), new BigDecimal("80"), "");
        assessmentService.recordScore(a1.getId(), s2.getId(), new BigDecimal("90"), "");
        assessmentService.recordScore(b1.getId(), s3.getId(), new BigDecimal("70"), "");

        // 1-3. Admin Dashboard deltas
        DashboardSummary afterDashboard = dashboardService.getDashboardSummary();
        assertEquals(beforeDashboard.getTotalActiveStudents() + 3, afterDashboard.getTotalActiveStudents());
        assertEquals(beforeDashboard.getTotalInactiveStudents() + 1, afterDashboard.getTotalInactiveStudents());
        assertEquals(beforeDashboard.getTotalActiveTeachers() + 2, afterDashboard.getTotalActiveTeachers());
        assertEquals(beforeDashboard.getTotalInactiveTeachers() + 1, afterDashboard.getTotalInactiveTeachers());
        assertEquals(beforeDashboard.getTotalActiveClasses() + 2, afterDashboard.getTotalActiveClasses());
        assertEquals("TEST_P6_2026/2027", afterDashboard.getActiveAcademicYearName());

        // 4. Students grouped by class
        List<ClassStudentCountReport> byClass = reportService.getStudentCountsByClass();
        ClassStudentCountReport rep1A = byClass.stream()
                .filter(c -> c.getClassId().equals(class1A.getId())).findFirst().orElseThrow();
        ClassStudentCountReport rep1B = byClass.stream()
                .filter(c -> c.getClassId().equals(class1B.getId())).findFirst().orElseThrow();
        assertEquals(2, rep1A.getActiveStudentCount());
        assertEquals(1, rep1B.getActiveStudentCount());

        // 5. Students grouped by gender (2 active boys: s1, s3; 1 active girl: s2)
        List<GenderStudentCountReport> afterGender = reportService.getStudentCountsByGender();
        assertEquals("L", afterGender.get(0).getGenderCode());
        assertEquals(beforeMale + 2, afterGender.get(0).getActiveStudentCount());
        assertEquals("P", afterGender.get(1).getGenderCode());
        assertEquals(beforeFemale + 1, afterGender.get(1).getActiveStudentCount());

        // 6-8. Attendance by date range (6 records: 4 HADIR, 1 SAKIT, 0 IZIN, 1 ALPA => 66.67%)
        AttendanceRangeReport rangeReport = reportService.getAttendanceSummaryByDateRange(d1, d2);
        assertEquals(6, rangeReport.getTotalRecords());
        assertEquals(4, rangeReport.getHadirCount());
        assertEquals(1, rangeReport.getSakitCount());
        assertEquals(0, rangeReport.getIzinCount());
        assertEquals(1, rangeReport.getAlpaCount());
        assertTrue(rangeReport.hasSufficientData());
        assertEquals(0, new BigDecimal("66.67").compareTo(rangeReport.getAttendancePercentage()));

        // Empty date range -> no division by zero, percentage is null
        AttendanceRangeReport emptyRange = reportService.getAttendanceSummaryByDateRange(
                LocalDate.of(2040, 1, 1), LocalDate.of(2040, 1, 31));
        assertEquals(0, emptyRange.getTotalRecords());
        assertFalse(emptyRange.hasSufficientData());
        assertNull(emptyRange.getAttendancePercentage());

        // Attendance summary by class in date range
        List<ClassAttendanceReport> classAtt = reportService.getAttendanceSummaryByClass(d1, d2);
        ClassAttendanceReport att1A = classAtt.stream()
                .filter(c -> c.getClassId().equals(class1A.getId())).findFirst().orElseThrow();
        ClassAttendanceReport att1B = classAtt.stream()
                .filter(c -> c.getClassId().equals(class1B.getId())).findFirst().orElseThrow();
        assertEquals(4, att1A.getTotalRecords());
        assertEquals(0, new BigDecimal("75.00").compareTo(att1A.getAttendancePercentage()));
        assertEquals(2, att1B.getTotalRecords());
        assertEquals(0, new BigDecimal("50.00").compareTo(att1B.getAttendancePercentage()));

        // 9. Assessment summaries (scored & unscored)
        List<AssessmentReport> adminAssessments = reportService.getAssessmentSummaries();
        AssessmentReport repA1 = adminAssessments.stream()
                .filter(a -> a.getAssessmentId().equals(a1.getId())).findFirst().orElseThrow();
        AssessmentReport repA2 = adminAssessments.stream()
                .filter(a -> a.getAssessmentId().equals(a2Empty.getId())).findFirst().orElseThrow();
        assertEquals(2, repA1.getScoredStudentsCount());
        assertEquals(0, new BigDecimal("85.00").compareTo(repA1.getAverageScore()));
        assertEquals(0, new BigDecimal("90.00").compareTo(repA1.getHighestScore()));
        assertEquals(0, new BigDecimal("80.00").compareTo(repA1.getLowestScore()));
        assertFalse(repA2.hasScores());
        assertNull(repA2.getAverageScore());

        // 10. Student score summary
        StudentScoreReport s1Report = reportService.getStudentScoreSummary(s1.getId());
        assertEquals(1, s1Report.getAssessmentCount());
        assertEquals(0, new BigDecimal("80.00").compareTo(s1Report.getAverageScore()));
        assertEquals(0, new BigDecimal("80.00").compareTo(s1Report.getOverallPercentage()));

        // 11. Average score per subject
        List<SubjectScoreReport> subjectReports = reportService.getAverageScoresBySubject();
        SubjectScoreReport mathReport = subjectReports.stream()
                .filter(sub -> sub.getSubjectId().equals(math.getId())).findFirst().orElseThrow();
        assertEquals(2, mathReport.getAssessmentCount());
        assertEquals(2, mathReport.getScoredEntriesCount());
        assertEquals(0, new BigDecimal("85.00").compareTo(mathReport.getAverageScore()));
        assertEquals(0, new BigDecimal("85.00").compareTo(mathReport.getAveragePercentage()));

        // ============================================================
        // TEACHER ROLE-BASED DATA VISIBILITY (teacherA owns 1A & math)
        // ============================================================
        createAndLoginTeacherUser(teacherA.getId(), "test_p6_guru_a", "Guru A Report");

        DashboardSummary teacherDash = dashboardService.getDashboardSummary();
        assertEquals(2, teacherDash.getTotalActiveStudents());
        assertEquals(1, teacherDash.getTotalActiveTeachers());
        assertEquals(1, teacherDash.getTotalActiveClasses());
        assertEquals(1, teacherDash.getTotalActiveSubjects());
        assertEquals(1, teacherDash.getActiveTeachingAssignmentCount());
        assertEquals(2, teacherDash.getTotalAssessmentsCount());
        assertEquals(4, teacherDash.getTotalAttendanceRecords());
        assertEquals(0, new BigDecimal("75.00").compareTo(teacherDash.getOverallAttendancePercentage()));

        // Class student count & gender scoped to 1A (s1: L, s2: P)
        List<ClassStudentCountReport> teacherByClass = reportService.getStudentCountsByClass();
        assertEquals(1, teacherByClass.size());
        assertEquals(class1A.getId(), teacherByClass.get(0).getClassId());

        List<GenderStudentCountReport> teacherByGender = reportService.getStudentCountsByGender();
        assertEquals(1, teacherByGender.get(0).getActiveStudentCount());
        assertEquals(1, teacherByGender.get(1).getActiveStudentCount());

        // Attendance summary scoped to 1A
        AttendanceRangeReport teacherRange = reportService.getAttendanceSummaryByDateRange(d1, d2);
        assertEquals(4, teacherRange.getTotalRecords());
        assertEquals(0, new BigDecimal("75.00").compareTo(teacherRange.getAttendancePercentage()));

        List<ClassAttendanceReport> teacherClassAtt = reportService.getAttendanceSummaryByClass(d1, d2);
        assertEquals(1, teacherClassAtt.size());
        assertEquals(class1A.getId(), teacherClassAtt.get(0).getClassId());

        // Assessments, Subject Averages & Student Score Summaries scoped to teacherA
        assertEquals(2, reportService.getAssessmentSummaries().size());
        assertEquals(1, reportService.getAverageScoresBySubject().size());
        assertEquals(2, reportService.getStudentScoreSummaries().size());
        assertNotNull(reportService.getStudentScoreSummary(s1.getId()));
        assertThrows(IllegalArgumentException.class, () -> reportService.getStudentScoreSummary(s3.getId()));

        // Unlinked teacher rejected
        createAndLoginTeacherUser(null, "test_p6_guru_unlinked", "Guru Tanpa Link");
        assertThrows(IllegalArgumentException.class, () -> dashboardService.getDashboardSummary());
        assertThrows(IllegalArgumentException.class, () -> reportService.getStudentCountsByClass());
    }

    @Test
    void emptyInMemoryDatasetsAndZeroDivisionAreHandledDeterministically() {
        SessionContext.getInstance().startSession(1L, null, "admin", "Administrator", RoleMapper.ROLE_ADMIN);

        InMemoryStudentDAO studentDAO = new InMemoryStudentDAO();
        InMemoryTeacherDAO teacherDAO = new InMemoryTeacherDAO();
        InMemoryAcademicDAO academicDAO = new InMemoryAcademicDAO();
        InMemoryAttendanceDAO attendanceDAO = new InMemoryAttendanceDAO();
        InMemoryAssessmentDAO assessmentDAO = new InMemoryAssessmentDAO();
        InMemoryReportDAO reportDAO = new InMemoryReportDAO(
                studentDAO, teacherDAO, academicDAO, attendanceDAO, assessmentDAO);

        DashboardService dashboardService = new DashboardService(reportDAO);
        ReportService reportService = new ReportService(reportDAO, studentDAO);

        // Completely empty dataset checks
        DashboardSummary emptyDash = dashboardService.getDashboardSummary();
        assertEquals(0, emptyDash.getTotalActiveStudents());
        assertEquals(0, emptyDash.getTotalActiveTeachers());
        assertEquals(0, emptyDash.getTotalActiveClasses());
        assertFalse(emptyDash.hasAttendanceData());
        assertNull(emptyDash.getOverallAttendancePercentage());

        assertTrue(reportService.getStudentCountsByClass().isEmpty());
        List<GenderStudentCountReport> genders = reportService.getStudentCountsByGender();
        assertEquals(2, genders.size());
        assertEquals(0, genders.get(0).getActiveStudentCount());
        assertEquals(0, genders.get(1).getActiveStudentCount());

        AttendanceRangeReport emptyAtt = reportService.getAttendanceSummaryByDateRange(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
        assertEquals(0, emptyAtt.getTotalRecords());
        assertFalse(emptyAtt.hasSufficientData());
        assertNull(emptyAtt.getAttendancePercentage());

        assertTrue(reportService.getAttendanceSummaryByClass().isEmpty());
        assertTrue(reportService.getAssessmentSummaries().isEmpty());
        assertTrue(reportService.getStudentScoreSummaries().isEmpty());
        assertTrue(reportService.getAverageScoresBySubject().isEmpty());

        // Invalid date range rejected cleanly
        assertThrows(IllegalArgumentException.class, () -> reportService.getAttendanceSummaryByDateRange(
                LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 1)));
    }
}
