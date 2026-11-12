package id.ac.sekolah.manajemenkelas.assessment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.assessment.dao.PostgresAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.AssessmentSummary;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScoreSummary;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

class PostgresAssessmentDAOIntegrationTest {

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
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_P5_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM nilai_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P5_%') "
                            + "OR id_penilaian IN (SELECT id FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P5_%')))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P5_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P5_%'))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM riwayat_kelas_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P5_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P5_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P5_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P5_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_wali_kelas WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P5_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P5_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM semester WHERE id_tahun_ajaran IN "
                            + "(SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P5_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_P5_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P5_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_P5_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_P5_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pengguna WHERE nama_pengguna LIKE 'test_p5_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_P5_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM siswa WHERE nis LIKE 'TEST_P5_%'")) {
                ps.executeUpdate();
            }
        }
    }

    private AssessmentService createAssessmentService() {
        return new AssessmentService(
                new PostgresAssessmentDAO(),
                new PostgresAcademicDAO(),
                new PostgresStudentDAO());
    }

    private AcademicService createAcademicService() {
        return new AcademicService(
                new PostgresAcademicDAO(),
                new PostgresStudentDAO(),
                new PostgresTeacherDAO());
    }

    private User loginAsSeededAdmin() {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        new AuthBootstrapSeeder(authDAO, hasher).seedDefaultUsers();
        AuthService authService = new AuthService(authDAO, hasher);
        assertTrue(authService.login("admin", "admin123"));
        return authDAO.findByUsername("admin").orElseThrow();
    }

    private User createAndLoginTeacherUser(Long teacherId, String username, String fullName) {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PasswordHasher hasher = new PasswordHasher("PBKDF2WithHmacSHA256", 1000, 256, 16);
        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("guru123", salt);
        User user = new User(null, teacherId, username, hash, salt, fullName, RoleMapper.ROLE_TEACHER, true, null);
        authDAO.save(user);

        AuthService authService = new AuthService(authDAO, hasher);
        assertTrue(authService.login(username, "guru123"));
        return user;
    }

    @Test
    void assessmentCreationBatchScoresUpdatesSummariesAndRestartWorkWithPostgres() {
        User adminUser = loginAsSeededAdmin();

        TeacherService teacherService = new TeacherService(new PostgresTeacherDAO());
        StudentService studentService = new StudentService(new PostgresStudentDAO());
        AcademicService academicService = createAcademicService();
        AssessmentService assessmentService = createAssessmentService();

        Teacher teacher = teacherService.createTeacher(
                "TEST_P5_NIP_01", "Ibu Kartini", "P", "", "", "Jl. Pendidikan", "S1", LocalDate.of(2020, 1, 1));
        Student s1 = studentService.createStudent(
                "TEST_P5_NIS_01", "TEST_P5_NISN_01", "Arif Rahman", "L",
                LocalDate.of(2016, 4, 5), "Pekanbaru", "Jl. A", "Bapak Adi", "08120011");
        Student s2 = studentService.createStudent(
                "TEST_P5_NIS_02", "TEST_P5_NISN_02", "Bella Safitri", "P",
                LocalDate.of(2016, 7, 19), "Pekanbaru", "Jl. B", "Ibu Rini", "08120012");

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P5_2026/2027", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        Semester sem1 = academicService.createSemester(
                year.getId(), 1, "Ganjil 2026/2027", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P5_1A", 1, 30);
        Subject math = academicService.createSubject("TEST_P5_MTK", "Matematika", "Wajib");

        TeachingAssignment ta = academicService.createTeachingAssignment(
                teacher.getId(), class1A.getId(), math.getId(), sem1.getId());

        academicService.placeStudentInClass(s1.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(s2.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));

        // Create multiple types across all 7 allowed enum types
        Assessment kuis = assessmentService.createAssessment(
                ta.getId(), "Kuis Pecahan", "kuis", LocalDate.of(2026, 8, 12),
                new BigDecimal("100"), "Kuis harian");
        assertNotNull(kuis.getId());
        assertEquals(Assessment.TYPE_KUIS, kuis.getAssessmentType());

        Assessment praktik = assessmentService.createAssessment(
                ta.getId(), "Praktik Pengukuran", Assessment.TYPE_PRAKTIK, LocalDate.of(2026, 9, 1),
                new BigDecimal("50"), "Praktik mistar");
        assertEquals(0, new BigDecimal("50.00").compareTo(praktik.getMaxScore()));

        // Verify other enum values accepted cleanly
        assessmentService.createAssessment(ta.getId(), "Tugas 1", Assessment.TYPE_TUGAS, LocalDate.of(2026, 8, 20), new BigDecimal("100"), "");
        assessmentService.createAssessment(ta.getId(), "UTS", Assessment.TYPE_UTS, LocalDate.of(2026, 10, 5), new BigDecimal("100"), "");
        assessmentService.createAssessment(ta.getId(), "UAS", Assessment.TYPE_UAS, LocalDate.of(2026, 12, 5), new BigDecimal("100"), "");
        assessmentService.createAssessment(ta.getId(), "Proyek", Assessment.TYPE_PROYEK, LocalDate.of(2026, 11, 5), new BigDecimal("100"), "");
        assessmentService.createAssessment(ta.getId(), "Lainnya", Assessment.TYPE_LAINNYA, LocalDate.of(2026, 11, 15), new BigDecimal("100"), "");

        // Batch score entry on kuis
        List<StudentScore> batchSaved = assessmentService.recordAssessmentScores(
                kuis.getId(),
                List.of(
                        new AssessmentService.StudentScoreEntry(s1.getId(), new BigDecimal("80"), "Bagus"),
                        new AssessmentService.StudentScoreEntry(s2.getId(), new BigDecimal("90"), "Sangat bagus")));
        assertEquals(2, batchSaved.size());
        assertEquals(adminUser.getId(), batchSaved.get(0).getRecordedBy());

        // Single score entry on praktik & update
        StudentScore s1Praktik = assessmentService.recordScore(
                praktik.getId(), s1.getId(), new BigDecimal("40"), "Cukup");
        StudentScore updated = assessmentService.updateScore(
                s1Praktik.getId(), new BigDecimal("45"), "Revisi baik");
        assertEquals(0, new BigDecimal("45.00").compareTo(updated.getScore()));

        // Assessment summary on kuis (80 and 90 out of 100)
        AssessmentSummary kuisSummary = assessmentService.getAssessmentScoreSummary(kuis.getId());
        assertEquals(2, kuisSummary.getTotalStudentsScored());
        assertEquals(0, new BigDecimal("85.00").compareTo(kuisSummary.getAverageScore()));
        assertEquals(0, new BigDecimal("90.00").compareTo(kuisSummary.getHighestScore()));
        assertEquals(0, new BigDecimal("80.00").compareTo(kuisSummary.getLowestScore()));
        assertEquals(0, new BigDecimal("85.00").compareTo(kuisSummary.getAveragePercentage()));

        // Student score summary for s1 (kuis: 80/100 + praktik: 45/50 = 125/150 = 83.33%)
        StudentScoreSummary s1Summary = assessmentService.getStudentScoreSummary(s1.getId());
        assertEquals(2, s1Summary.getTotalAssessments());
        assertEquals(0, new BigDecimal("125.00").compareTo(s1Summary.getTotalEarnedScore()));
        assertEquals(0, new BigDecimal("150.00").compareTo(s1Summary.getTotalMaxScore()));
        assertEquals(0, new BigDecimal("62.50").compareTo(s1Summary.getAverageScore()));
        assertEquals(0, new BigDecimal("83.33").compareTo(s1Summary.getOverallPercentage()));

        // Simulate restart with fresh service
        AssessmentService restarted = createAssessmentService();
        assertEquals(7, restarted.getAssessmentsByTeachingAssignment(ta.getId()).size());
        assertEquals(2, restarted.getScoresByAssessment(kuis.getId()).size());
        assertEquals(2, restarted.getScoresByStudent(s1.getId()).size());
    }

    @Test
    void validationPreventsInvalidAssessmentTypeDateBoundsDuplicatesAndWrongClass() {
        loginAsSeededAdmin();

        TeacherService teacherService = new TeacherService(new PostgresTeacherDAO());
        StudentService studentService = new StudentService(new PostgresStudentDAO());
        AcademicService academicService = createAcademicService();
        AssessmentService assessmentService = createAssessmentService();

        Teacher teacher = teacherService.createTeacher(
                "TEST_P5_NIP_02", "Bapak Hasan", "L", "", "", "Jl. Melati", "S1", LocalDate.of(2020, 1, 1));
        Student inClassStudent = studentService.createStudent(
                "TEST_P5_NIS_03", "TEST_P5_NISN_03", "Candra Wijaya", "L",
                LocalDate.of(2016, 2, 11), "Pekanbaru", "Jl. C", "Bapak Budi", "08120013");
        Student outsiderStudent = studentService.createStudent(
                "TEST_P5_NIS_04", "TEST_P5_NISN_04", "Dina Amelia", "P",
                LocalDate.of(2016, 8, 22), "Pekanbaru", "Jl. D", "Ibu Maya", "08120014");

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P5_2026/2027_V", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        Semester sem1 = academicService.createSemester(
                year.getId(), 1, "Ganjil V", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P5_1A_V", 1, 30);
        SchoolClass class1B = academicService.createClass(year.getId(), "TEST_P5_1B_V", 1, 30);
        Subject sci = academicService.createSubject("TEST_P5_IPA", "IPA", "Wajib");

        TeachingAssignment ta = academicService.createTeachingAssignment(
                teacher.getId(), class1A.getId(), sci.getId(), sem1.getId());

        academicService.placeStudentInClass(inClassStudent.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(outsiderStudent.getId(), class1B.getId(), LocalDate.of(2026, 7, 10));

        // Invalid assessment type
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                ta.getId(), "Ujian Invalid", "UJIAN_LIAR", LocalDate.of(2026, 8, 10), new BigDecimal("100"), ""));

        // Invalid maxScore (0 or negative)
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                ta.getId(), "Skor Nol", Assessment.TYPE_KUIS, LocalDate.of(2026, 8, 10), BigDecimal.ZERO, ""));
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                ta.getId(), "Skor Negatif", Assessment.TYPE_KUIS, LocalDate.of(2026, 8, 10), new BigDecimal("-10"), ""));

        // Invalid assessment date (outside semester date range)
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                ta.getId(), "Luar Semester", Assessment.TYPE_UTS, LocalDate.of(2027, 2, 1), new BigDecimal("100"), ""));

        Assessment valid = assessmentService.createAssessment(
                ta.getId(), "UTS IPA", Assessment.TYPE_UTS, LocalDate.of(2026, 10, 10), new BigDecimal("80"), "");

        // Score out of bounds (< 0 or > maxScore)
        assertThrows(IllegalArgumentException.class, () -> assessmentService.recordScore(
                valid.getId(), inClassStudent.getId(), new BigDecimal("-1"), ""));
        assertThrows(IllegalArgumentException.class, () -> assessmentService.recordScore(
                valid.getId(), inClassStudent.getId(), new BigDecimal("81"), ""));

        // Student in wrong class rejected
        assertThrows(IllegalArgumentException.class, () -> assessmentService.recordScore(
                valid.getId(), outsiderStudent.getId(), new BigDecimal("70"), ""));

        // Record valid score then reject duplicate for same assessment
        assessmentService.recordScore(valid.getId(), inClassStudent.getId(), new BigDecimal("75"), "Lulus");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.recordScore(
                valid.getId(), inClassStudent.getId(), new BigDecimal("78"), "Duplikat"));

        // Reject lowering maxScore below existing student score (75)
        assertThrows(IllegalArgumentException.class, () -> assessmentService.updateAssessment(
                valid.getId(), "UTS IPA", Assessment.TYPE_UTS, LocalDate.of(2026, 10, 10), new BigDecimal("70"), ""));
    }

    @Test
    void teacherMayManageScoresOnlyForOwnTeachingAssignments() {
        loginAsSeededAdmin();

        TeacherService teacherService = new TeacherService(new PostgresTeacherDAO());
        StudentService studentService = new StudentService(new PostgresStudentDAO());
        AcademicService academicService = createAcademicService();
        AssessmentService assessmentService = createAssessmentService();

        Teacher teacherA = teacherService.createTeacher(
                "TEST_P5_NIP_03", "Guru A", "P", "", "", "Jl. Guru A", "S1", LocalDate.of(2020, 1, 1));
        Teacher teacherB = teacherService.createTeacher(
                "TEST_P5_NIP_04", "Guru B", "L", "", "", "Jl. Guru B", "S1", LocalDate.of(2021, 1, 1));

        Student studentA = studentService.createStudent(
                "TEST_P5_NIS_05", "TEST_P5_NISN_05", "Eka Putri", "P",
                LocalDate.of(2016, 1, 20), "Pekanbaru", "Jl. E", "Ibu Ida", "08120015");
        Student studentB = studentService.createStudent(
                "TEST_P5_NIS_06", "TEST_P5_NISN_06", "Farhan Hakim", "L",
                LocalDate.of(2016, 3, 25), "Pekanbaru", "Jl. F", "Bapak Ali", "08120016");

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P5_2026/2027_AUTH", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        Semester sem1 = academicService.createSemester(
                year.getId(), 1, "Ganjil Auth", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P5_1A_AUTH", 1, 30);
        SchoolClass class1B = academicService.createClass(year.getId(), "TEST_P5_1B_AUTH", 1, 30);
        Subject subject = academicService.createSubject("TEST_P5_PPKN", "PPKN", "Wajib");

        TeachingAssignment taA = academicService.createTeachingAssignment(
                teacherA.getId(), class1A.getId(), subject.getId(), sem1.getId());
        TeachingAssignment taB = academicService.createTeachingAssignment(
                teacherB.getId(), class1B.getId(), subject.getId(), sem1.getId());

        academicService.placeStudentInClass(studentA.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(studentB.getId(), class1B.getId(), LocalDate.of(2026, 7, 10));

        // Admin creates an assessment on teacherB's assignment
        Assessment assessB = assessmentService.createAssessment(
                taB.getId(), "Tugas 1B", Assessment.TYPE_TUGAS, LocalDate.of(2026, 8, 15),
                new BigDecimal("100"), "Admin setup");
        assessmentService.recordScore(assessB.getId(), studentB.getId(), new BigDecimal("88"), "Baik");

        // Switch session to teacherA
        User teacherAUser = createAndLoginTeacherUser(teacherA.getId(), "test_p5_guru_a", "Guru A");

        // teacherA can create assessment and score on taA
        Assessment assessA = assessmentService.createAssessment(
                taA.getId(), "Kuis 1A", Assessment.TYPE_KUIS, LocalDate.of(2026, 8, 15),
                new BigDecimal("100"), "Kuis mandiri");
        StudentScore scoreA = assessmentService.recordScore(
                assessA.getId(), studentA.getId(), new BigDecimal("92"), "Hebat");
        assertEquals(teacherAUser.getId(), scoreA.getRecordedBy());
        assertEquals(1, assessmentService.getAllAssessments().size());

        // teacherA cannot access or mutate taB, assessB, or studentB
        assertThrows(IllegalArgumentException.class, () -> assessmentService.createAssessment(
                taB.getId(), "Ilegal", Assessment.TYPE_KUIS, LocalDate.of(2026, 8, 16), new BigDecimal("100"), ""));
        assertThrows(IllegalArgumentException.class, () -> assessmentService.recordScore(
                assessB.getId(), studentB.getId(), new BigDecimal("50"), ""));
        assertThrows(IllegalArgumentException.class, () -> assessmentService.getScoresByAssessment(assessB.getId()));
        assertThrows(IllegalArgumentException.class, () -> assessmentService.getScoresByStudent(studentB.getId()));

        // Teacher account without teacherId cannot access
        createAndLoginTeacherUser(null, "test_p5_guru_unlinked", "Guru Tanpa Link");
        assertThrows(IllegalArgumentException.class, () -> assessmentService.getAllAssessments());
    }
}

