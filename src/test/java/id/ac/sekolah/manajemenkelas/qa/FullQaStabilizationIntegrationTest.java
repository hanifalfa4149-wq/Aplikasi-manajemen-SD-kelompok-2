package id.ac.sekolah.manajemenkelas.qa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.assessment.dao.PostgresAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.attendance.dao.PostgresAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.core.util.JdbcUtils;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

class FullQaStabilizationIntegrationTest {

    private Long previouslyActiveYearId;

    @BeforeEach
    void setup() throws SQLException {
        SessionContext.getInstance().endSession();
        cleanupTestRows();
        previouslyActiveYearId = findActiveNonTestYearId();
    }

    @AfterEach
    void teardown() throws SQLException {
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

    @Test
    void authBootstrapSeederAndPostgresAuthConstraintsVerified() {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PasswordHasher hasher = new PasswordHasher("PBKDF2WithHmacSHA256", 1000, 256, 16);

        assertThrows(IllegalArgumentException.class, () -> new AuthBootstrapSeeder(null, hasher));
        assertThrows(IllegalArgumentException.class, () -> new AuthBootstrapSeeder(authDAO, null));

        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("PassQA#2026", salt);

        User adminUser = new User(null, null, "test_qa_admin1", hash, salt, "QA Admin", RoleMapper.ROLE_ADMIN, true, null);
        authDAO.save(adminUser);
        assertNotNull(adminUser.getId());

        User reloaded = authDAO.findByUsername("test_qa_admin1").orElseThrow();
        assertNull(reloaded.getTeacherId());
        assertNull(reloaded.getLastLogin());

        // Duplicate username (SQLState 23505)
        User duplicateUser = new User(null, null, "test_qa_admin1", hash, salt, "Dup", RoleMapper.ROLE_ADMIN, true, null);
        assertThrows(IllegalArgumentException.class, () -> authDAO.save(duplicateUser));

        // Nonexistent teacher_id FK (SQLState 23503)
        User badTeacherFk = new User(null, 999_999_999L, "test_qa_badfk", hash, salt, "Bad FK", RoleMapper.ROLE_TEACHER, true, null);
        assertThrows(IllegalArgumentException.class, () -> authDAO.save(badTeacherFk));
    }

    @Test
    void databaseForeignKeysPartialUniqueIndexesVarcharTruncationAndNullRoundTripsVerified() {
        PostgresStudentDAO studentDAO = new PostgresStudentDAO();
        PostgresTeacherDAO teacherDAO = new PostgresTeacherDAO();
        PostgresAcademicDAO academicDAO = new PostgresAcademicDAO();
        PostgresAttendanceDAO attendanceDAO = new PostgresAttendanceDAO();
        PostgresAssessmentDAO assessmentDAO = new PostgresAssessmentDAO();

        StudentService studentSvc = new StudentService(studentDAO);
        TeacherService teacherSvc = new TeacherService(teacherDAO);
        AcademicService academicSvc = new AcademicService(academicDAO, studentDAO, teacherDAO);

        Teacher t1 = teacherSvc.createTeacher(
                "TEST_QA_G01", "Guru QA 1", "L", "", "", "Jl. A", "S1", LocalDate.of(2020, 1, 1));
        Teacher t2 = teacherSvc.createTeacher(
                "TEST_QA_G02", "Guru QA 2", "P", "", "", "Jl. B", "S1", LocalDate.of(2020, 1, 1));
        assertEquals("", t1.getEmail());
        assertEquals("", t2.getEmail());

        // Unique non-null teacher email directly at DAO level
        Teacher emailDup = new Teacher(
                null, "TEST_QA_G03", "Guru Email Dup", "L", "qa_dup@sd.id", "081", "Jl", "S1", LocalDate.of(2020, 1, 1), true);
        teacherDAO.save(emailDup);
        Teacher emailDup2 = new Teacher(
                null, "TEST_QA_G04", "Guru Email Dup 2", "L", "qa_dup@sd.id", "082", "Jl", "S1", LocalDate.of(2020, 1, 1), true);
        assertThrows(IllegalArgumentException.class, () -> teacherDAO.save(emailDup2));

        Student s1 = studentSvc.createStudent(
                "TEST_QA_S01", "TEST_QA_NISN_01", "Siswa QA 1", "L",
                LocalDate.of(2016, 1, 1), "PKU", "Jl. S", "Ortu", "0811");
        AcademicYear y1 = academicSvc.createAcademicYear(
                "TEST_QA_2030/2031", LocalDate.of(2030, 7, 1), LocalDate.of(2031, 6, 30));
        Semester sem1 = academicSvc.createSemester(
                y1.getId(), 1, "TEST_QA_Ganjil", LocalDate.of(2030, 7, 1), LocalDate.of(2030, 12, 20));
        SchoolClass c1 = academicSvc.createClass(y1.getId(), "TEST_QA_1A", 1, 30);
        Subject sbj1 = academicSvc.createSubject("TEST_QA_MTK", "TEST_QA Matematika", "");
        assertEquals("", sbj1.getDescription());

        // Direct FK violations across all child tables (SQLState 23503 -> IllegalArgumentException)
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveSemester(
                new Semester(null, 999_999_999L, 1, "TEST_QA_BAD", LocalDate.of(2030, 7, 1), LocalDate.of(2030, 12, 20), true)));
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveHomeroomAssignment(
                new HomeroomAssignment(null, c1.getId(), 999_999_999L, LocalDate.of(2030, 7, 1), null, true)));
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveTeachingAssignment(
                new TeachingAssignment(null, t1.getId(), c1.getId(), 999_999_999L, sem1.getId(), true)));
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveStudentPlacement(
                new StudentPlacement(null, 999_999_999L, c1.getId(), y1.getId(), LocalDate.of(2030, 7, 5), null, StudentPlacement.STATUS_AKTIF)));
        assertThrows(IllegalArgumentException.class, () -> attendanceDAO.save(
                new Attendance(null, 999_999_999L, c1.getId(), LocalDate.of(2030, 9, 1), Attendance.STATUS_HADIR, null, null)));
        assertThrows(IllegalArgumentException.class, () -> assessmentDAO.saveAssessment(
                new Assessment(null, 999_999_999L, "TEST_QA_BAD", Assessment.TYPE_KUIS, LocalDate.of(2030, 9, 1), BigDecimal.TEN, "")));
        assertThrows(IllegalArgumentException.class, () -> assessmentDAO.saveScore(
                new StudentScore(null, 999_999_999L, s1.getId(), BigDecimal.TEN, "", null)));

        // Direct partial unique indexes & unique constraints (SQLState 23505 -> IllegalArgumentException)
        academicDAO.saveHomeroomAssignment(
                new HomeroomAssignment(null, c1.getId(), t1.getId(), LocalDate.of(2030, 7, 1), null, true));
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveHomeroomAssignment(
                new HomeroomAssignment(null, c1.getId(), t2.getId(), LocalDate.of(2030, 7, 2), null, true)));

        StudentPlacement placement = new StudentPlacement(
                null, s1.getId(), c1.getId(), y1.getId(), LocalDate.of(2030, 7, 5), null, StudentPlacement.STATUS_AKTIF);
        academicDAO.saveStudentPlacement(placement);
        assertNull(academicDAO.findActivePlacementByStudentId(s1.getId()).orElseThrow().getExitDate());
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveStudentPlacement(
                new StudentPlacement(null, s1.getId(), c1.getId(), y1.getId(), LocalDate.of(2030, 7, 6), null, StudentPlacement.STATUS_AKTIF)));

        // Direct attendance & assessment + NULL field round-trips
        Attendance att = new Attendance(null, s1.getId(), c1.getId(), LocalDate.of(2030, 9, 10), Attendance.STATUS_HADIR, "", null);
        attendanceDAO.save(att);
        Attendance attReloaded = attendanceDAO.findById(att.getId()).orElseThrow();
        assertNull(attReloaded.getRecordedBy());
        assertEquals("", attReloaded.getNotes());
        assertThrows(IllegalArgumentException.class, () -> attendanceDAO.save(
                new Attendance(null, s1.getId(), c1.getId(), LocalDate.of(2030, 9, 10), Attendance.STATUS_SAKIT, "", null)));

        TeachingAssignment ta = new TeachingAssignment(null, t1.getId(), c1.getId(), sbj1.getId(), sem1.getId(), true);
        academicDAO.saveTeachingAssignment(ta);
        Assessment assessment = new Assessment(
                null, ta.getId(), "Kuis QA NULL", Assessment.TYPE_KUIS, LocalDate.of(2030, 9, 12), BigDecimal.valueOf(100), "");
        assessmentDAO.saveAssessment(assessment);
        assertEquals("", assessmentDAO.findAssessmentById(assessment.getId()).orElseThrow().getDescription());

        StudentScore score = new StudentScore(null, assessment.getId(), s1.getId(), BigDecimal.valueOf(88), "", null);
        assessmentDAO.saveScore(score);
        assertEquals("", assessmentDAO.findScoreById(score.getId()).orElseThrow().getNotes());
        assertThrows(IllegalArgumentException.class, () -> assessmentDAO.saveScore(
                new StudentScore(null, assessment.getId(), s1.getId(), BigDecimal.valueOf(90), "", null)));

        // Direct SQLState 22001 string truncation handling
        String tooLong31 = "TEST_QA_1234567890123456789012345";
        assertThrows(IllegalArgumentException.class, () -> studentDAO.save(
                new Student(null, tooLong31, "TEST_QA_N99", "Long NIS", "L",
                        LocalDate.of(2016, 1, 1), "PKU", "Jl", "Ortu", "08", true)));
        assertThrows(IllegalArgumentException.class, () -> teacherDAO.save(
                new Teacher(null, tooLong31, "Long NIP", "L", "", "", "Jl", "S1",
                        LocalDate.of(2020, 1, 1), true)));
        assertThrows(IllegalArgumentException.class, () -> academicDAO.saveSubject(
                new Subject(null, tooLong31, "Long Code", "", true)));
    }

    @Test
    void databaseTransactionRollbackAndConnectionErrorHandlingVerified() throws SQLException {
        assertTrue(DatabaseConfig.isConnectionAvailable());

        // 1. Direct JdbcUtils transaction rollback test on PostgreSQL
        PostgresAcademicDAO academicDAO = new PostgresAcademicDAO();
        assertThrows(IllegalStateException.class, () -> JdbcUtils.inTransactionVoid(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO mata_pelajaran (kode, nama, deskripsi, aktif) VALUES (?, ?, ?, TRUE)")) {
                ps.setString(1, "TEST_QA_TX_ROLLBACK");
                ps.setString(2, "Should Rollback");
                ps.setString(3, "Temp");
                ps.executeUpdate();
            }
            try (PreparedStatement bad = conn.prepareStatement("INSERT INTO mata_pelajaran (kode) VALUES (NULL)")) {
                bad.executeUpdate();
            }
        }));
        assertTrue(academicDAO.findAllSubjects().stream()
                .noneMatch(s -> "TEST_QA_TX_ROLLBACK".equals(s.getCode())));

        // 2. Closed connection inside JdbcUtils.inTransaction propagates IllegalStateException cleanly
        assertThrows(IllegalStateException.class, () -> JdbcUtils.inTransaction(conn -> {
            conn.close();
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
                ps.executeQuery();
            }
            return null;
        }));

        // 3. Batch rollback on AttendanceService & AssessmentService & transitionStudentPlacement
        PostgresStudentDAO studentDAO = new PostgresStudentDAO();
        PostgresTeacherDAO teacherDAO = new PostgresTeacherDAO();
        PostgresAttendanceDAO attendanceDAO = new PostgresAttendanceDAO();
        PostgresAssessmentDAO assessmentDAO = new PostgresAssessmentDAO();
        PostgresAuthDAO authDAO = new PostgresAuthDAO();

        StudentService studentSvc = new StudentService(studentDAO);
        TeacherService teacherSvc = new TeacherService(teacherDAO);
        AcademicService academicSvc = new AcademicService(academicDAO, studentDAO, teacherDAO);
        AttendanceService attendanceSvc = new AttendanceService(attendanceDAO, academicDAO, studentDAO);
        AssessmentService assessmentSvc = new AssessmentService(assessmentDAO, academicDAO, studentDAO);

        Teacher t = teacherSvc.createTeacher(
                "TEST_QA_G20", "Guru Batch TX", "L", "", "", "Jl", "S1", LocalDate.of(2020, 1, 1));
        Student s1 = studentSvc.createStudent(
                "TEST_QA_S20", "TEST_QA_NISN_20", "Siswa TX 1", "L",
                LocalDate.of(2016, 1, 1), "PKU", "Jl", "Ortu", "08");
        Student s2 = studentSvc.createStudent(
                "TEST_QA_S21", "TEST_QA_NISN_21", "Siswa TX 2", "P",
                LocalDate.of(2016, 2, 2), "PKU", "Jl", "Ortu", "08");
        AcademicYear y = academicSvc.createAcademicYear(
                "TEST_QA_2031/2032", LocalDate.of(2031, 7, 1), LocalDate.of(2032, 6, 30));
        Semester sem = academicSvc.createSemester(
                y.getId(), 1, "TEST_QA_S1_TX", LocalDate.of(2031, 7, 1), LocalDate.of(2031, 12, 20));
        SchoolClass cls = academicSvc.createClass(y.getId(), "TEST_QA_TX_1A", 1, 30);
        Subject sbj = academicSvc.createSubject("TEST_QA_TX_IPA", "TEST_QA IPA TX", "");
        StudentPlacement initialPlacement = academicSvc.placeStudentInClass(
                s1.getId(), cls.getId(), LocalDate.of(2031, 7, 5));
        academicSvc.placeStudentInClass(s2.getId(), cls.getId(), LocalDate.of(2031, 7, 5));
        TeachingAssignment ta = academicSvc.createTeachingAssignment(
                t.getId(), cls.getId(), sbj.getId(), sem.getId());

        // Login as Admin in SessionContext (with valid saved Admin user ID for recordedBy FK)
        PasswordHasher hasher = new PasswordHasher("PBKDF2WithHmacSHA256", 1000, 256, 16);
        String salt = hasher.generateSalt();
        User admin = new User(null, null, "test_qa_admin_tx", hasher.hashPassword("TxPass#1", salt),
                salt, "Admin TX", RoleMapper.ROLE_ADMIN, true, null);
        authDAO.save(admin);
        SessionContext.getInstance().startSession(admin.getId(), null, admin.getUsername(), admin.getFullName(), admin.getRole());

        // Batch attendance with duplicate s1 in second entry must fail atomically -> 0 rows stored
        LocalDate txDate = LocalDate.of(2031, 9, 20);
        assertThrows(IllegalArgumentException.class, () -> attendanceSvc.recordClassAttendance(
                cls.getId(),
                txDate,
                List.of(
                        new AttendanceService.StudentAttendanceEntry(s1.getId(), Attendance.STATUS_HADIR, "First"),
                        new AttendanceService.StudentAttendanceEntry(s1.getId(), Attendance.STATUS_SAKIT, "Dup"))));
        assertTrue(attendanceDAO.findByClassAndDate(cls.getId(), txDate).isEmpty());

        // Batch assessment scores with out-of-range score in second entry must fail atomically -> 0 scores stored
        Assessment assessment = assessmentSvc.createAssessment(
                ta.getId(), "UTS TX", Assessment.TYPE_UTS, LocalDate.of(2031, 10, 10), BigDecimal.valueOf(100), "");
        assertThrows(IllegalArgumentException.class, () -> assessmentSvc.recordAssessmentScores(
                assessment.getId(),
                List.of(
                        new AssessmentService.StudentScoreEntry(s1.getId(), BigDecimal.valueOf(85), "OK"),
                        new AssessmentService.StudentScoreEntry(s2.getId(), BigDecimal.valueOf(150), "OverMax"))));
        assertTrue(assessmentDAO.findScoresByAssessmentId(assessment.getId()).isEmpty());

        // Direct transitionStudentPlacement rollback when new placement violates FK (classId = invalid)
        StudentPlacement prevClosed = new StudentPlacement(
                initialPlacement.getId(), s1.getId(), cls.getId(), y.getId(),
                initialPlacement.getEntryDate(), LocalDate.of(2031, 11, 1), StudentPlacement.STATUS_PINDAH);
        StudentPlacement badTarget = new StudentPlacement(
                null, s1.getId(), 999_999_999L, y.getId(),
                LocalDate.of(2031, 11, 1), null, StudentPlacement.STATUS_AKTIF);
        assertThrows(IllegalArgumentException.class, () -> academicDAO.transitionStudentPlacement(prevClosed, badTarget));
        StudentPlacement stillActive = academicDAO.findActivePlacementByStudentId(s1.getId()).orElseThrow();
        assertNull(stillActive.getExitDate());
        assertEquals(StudentPlacement.STATUS_AKTIF, stillActive.getStatus());
    }

    @Test
    void crudUpdatesAndValidationsVerified() {
        PostgresStudentDAO studentDAO = new PostgresStudentDAO();
        PostgresTeacherDAO teacherDAO = new PostgresTeacherDAO();
        PostgresAcademicDAO academicDAO = new PostgresAcademicDAO();
        PostgresAssessmentDAO assessmentDAO = new PostgresAssessmentDAO();

        StudentService studentSvc = new StudentService(studentDAO);
        TeacherService teacherSvc = new TeacherService(teacherDAO);
        AcademicService academicSvc = new AcademicService(academicDAO, studentDAO, teacherDAO);
        AssessmentService assessmentSvc = new AssessmentService(assessmentDAO, academicDAO, studentDAO);

        // Year & Semester update/deactivate/activate round-trip
        AcademicYear year = academicSvc.createAcademicYear(
                "TEST_QA_2032/2033", LocalDate.of(2032, 7, 1), LocalDate.of(2033, 6, 30));
        AcademicYear updatedYear = academicSvc.updateAcademicYear(
                year.getId(), "TEST_QA_2032/2033_REV", LocalDate.of(2032, 7, 2), LocalDate.of(2033, 6, 29));
        assertEquals("TEST_QA_2032/2033_REV", academicSvc.getAcademicYearById(updatedYear.getId()).orElseThrow().getName());

        Semester sem = academicSvc.createSemester(
                year.getId(), 1, "TEST_QA_Ganjil_32", LocalDate.of(2032, 7, 2), LocalDate.of(2032, 12, 20));
        academicSvc.updateSemester(
                sem.getId(), year.getId(), 2, "TEST_QA_Genap_32", LocalDate.of(2033, 1, 5), LocalDate.of(2033, 6, 25));
        assertEquals(2, academicSvc.getSemesterById(sem.getId()).orElseThrow().getSemesterNumber());

        academicSvc.deactivateSemester(sem.getId());
        assertFalse(academicSvc.getSemesterById(sem.getId()).orElseThrow().isActive());
        academicSvc.activateSemester(sem.getId());
        assertTrue(academicSvc.getSemesterById(sem.getId()).orElseThrow().isActive());

        academicSvc.deactivateAcademicYear(year.getId());
        assertFalse(academicSvc.getAcademicYearById(year.getId()).orElseThrow().isActive());
        academicSvc.activateAcademicYear(year.getId());
        assertTrue(academicSvc.getAcademicYearById(year.getId()).orElseThrow().isActive());

        // Class capacity reduction check below active student placements
        SchoolClass cls = academicSvc.createClass(year.getId(), "TEST_QA_3A", 3, 2);
        Student s1 = studentSvc.createStudent(
                "TEST_QA_S30", "TEST_QA_NISN_30", "Siswa Cap 1", "L",
                LocalDate.of(2016, 1, 1), "PKU", "Jl", "Ortu", "08");
        Student s2 = studentSvc.createStudent(
                "TEST_QA_S31", "TEST_QA_NISN_31", "Siswa Cap 2", "P",
                LocalDate.of(2016, 2, 1), "PKU", "Jl", "Ortu", "08");
        Student s3 = studentSvc.createStudent(
                "TEST_QA_S32", "TEST_QA_NISN_32", "Siswa Cap 3", "L",
                LocalDate.of(2016, 3, 1), "PKU", "Jl", "Ortu", "08");
        academicSvc.placeStudentInClass(s1.getId(), cls.getId(), LocalDate.of(2032, 7, 10));
        academicSvc.placeStudentInClass(s2.getId(), cls.getId(), LocalDate.of(2032, 7, 10));
        // Capacity full -> third placement rejected
        assertThrows(IllegalArgumentException.class, () ->
                academicSvc.placeStudentInClass(s3.getId(), cls.getId(), LocalDate.of(2032, 7, 10)));
        // Reducing capacity below 2 active placements rejected
        assertThrows(IllegalArgumentException.class, () ->
                academicSvc.updateClass(cls.getId(), year.getId(), "TEST_QA_3A", 3, 1));
        academicSvc.updateClass(cls.getId(), year.getId(), "TEST_QA_3A_UPD", 3, 5);
        assertEquals("TEST_QA_3A_UPD", academicSvc.getClassById(cls.getId()).orElseThrow().getName());

        // Assessment update round-trip & service validation checks
        Teacher teacher = teacherSvc.createTeacher(
                "TEST_QA_G30", "Guru Cap 30", "P", "guru30@qa.id", "0812", "Jl", "S1", LocalDate.of(2020, 1, 1));
        Subject sbj = academicSvc.createSubject("TEST_QA_SBJ30", "TEST_QA IPS", "Desc");
        academicSvc.updateSubject(sbj.getId(), "TEST_QA_SBJ30_U", "TEST_QA IPS Rev", "Desc 2");
        assertEquals("TEST_QA IPS Rev", academicSvc.getSubjectById(sbj.getId()).orElseThrow().getName());
        TeachingAssignment ta = academicSvc.createTeachingAssignment(teacher.getId(), cls.getId(), sbj.getId(), sem.getId());

        SessionContext.getInstance().startSession(1L, null, "admin", "Admin", RoleMapper.ROLE_ADMIN);
        Assessment createdAssessment = assessmentSvc.createAssessment(
                ta.getId(), "Tugas QA 1", Assessment.TYPE_TUGAS, LocalDate.of(2033, 2, 10), BigDecimal.valueOf(100), "Awal");
        Assessment updatedAssessment = assessmentSvc.updateAssessment(
                createdAssessment.getId(), "Tugas QA 1 Rev", Assessment.TYPE_PRAKTIK, LocalDate.of(2033, 2, 11),
                BigDecimal.valueOf(80), "Revisi");
        assertEquals("Tugas QA 1 Rev", updatedAssessment.getTitle());
        assertEquals(Assessment.TYPE_PRAKTIK, updatedAssessment.getAssessmentType());

        // Student/Teacher/Academic invalid validation edge cases
        assertThrows(IllegalArgumentException.class, () -> studentSvc.createStudent(
                "", "N1", "Name", "L", LocalDate.of(2016, 1, 1), "PKU", "Jl", "Ortu", "08"));
        assertThrows(IllegalArgumentException.class, () -> studentSvc.createStudent(
                "TEST_QA_FUT", "TEST_QA_FUT_N", "Future", "X", LocalDate.now().plusDays(5), "PKU", "Jl", "Ortu", "08"));
        assertThrows(IllegalArgumentException.class, () -> teacherSvc.createTeacher(
                "TEST_QA_BAD_EMAIL", "Bad Email", "L", "not-an-email", "08", "Jl", "S1", LocalDate.of(2020, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> academicSvc.createClass(year.getId(), "TEST_QA_7A", 7, 20));
        assertThrows(IllegalArgumentException.class, () -> academicSvc.promoteOrTransferStudent(
                s1.getId(), cls.getId(), LocalDate.of(2033, 1, 10), StudentPlacement.STATUS_NAIK_KELAS));
        assertThrows(IllegalArgumentException.class, () -> academicSvc.completeStudentPlacement(
                s1.getId(), LocalDate.of(2032, 7, 1), StudentPlacement.STATUS_LULUS));

        // AttendanceService unlinked teacher check on getAttendanceByDate
        AttendanceService unlinkedAttSvc = new AttendanceService(new PostgresAttendanceDAO(), academicDAO, studentDAO);
        SessionContext.getInstance().startSession(88L, null, "guru_unlinked", "Guru Unlinked", RoleMapper.ROLE_TEACHER);
        assertThrows(IllegalArgumentException.class, () -> unlinkedAttSvc.getAttendanceByDate(LocalDate.of(2033, 2, 10)));
    }

    @Test
    void seedSqlExecutesIdempotentlyTwiceAgainstPostgres() throws Exception {
        String rawSql = Files.readString(Path.of("database/seed.sql"), StandardCharsets.UTF_8);
        String withoutLineComments = rawSql.replaceAll("(?m)^--.*$", "");
        String[] statements = withoutLineComments.split(";");

        try (Connection conn = DatabaseConfig.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (int pass = 0; pass < 2; pass++) {
                    try (Statement stmt = conn.createStatement()) {
                        for (String candidate : statements) {
                            String trimmed = candidate.trim();
                            if (trimmed.isEmpty()
                                    || trimmed.equalsIgnoreCase("BEGIN")
                                    || trimmed.equalsIgnoreCase("COMMIT")) {
                                continue;
                            }
                            stmt.execute(trimmed);
                        }
                    }
                }

                assertEquals(2, countSeedRows(conn, "SELECT COUNT(*) FROM guru WHERE nip IN ('198501012010011001', '198805122014022002')"));
                assertEquals(3, countSeedRows(conn, "SELECT COUNT(*) FROM siswa WHERE nis IN ('202601001', '202601002', '202602001')"));
                assertEquals(1, countSeedRows(conn, "SELECT COUNT(*) FROM tahun_ajaran WHERE nama = '2026/2027'"));
                assertEquals(2, countSeedRows(conn, "SELECT COUNT(*) FROM nilai_siswa ns JOIN penilaian p ON p.id = ns.id_penilaian WHERE p.judul = 'Kuis Penjumlahan 1-20'"));
            } finally {
                conn.rollback();
                conn.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private int countSeedRows(Connection conn, String sql) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Long findActiveNonTestYearId() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_QA_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM nilai_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_QA_%') "
                            + "OR id_penilaian IN (SELECT id FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_QA_%')))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_QA_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_QA_%'))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM absensi WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_QA_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_QA_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM riwayat_kelas_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_QA_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_QA_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_QA_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_QA_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_wali_kelas WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_QA_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_QA_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM semester WHERE id_tahun_ajaran IN "
                            + "(SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_QA_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_QA_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_QA_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_QA_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_QA_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pengguna WHERE nama_pengguna LIKE 'test_qa_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_QA_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM siswa WHERE nis LIKE 'TEST_QA_%'")) {
                ps.executeUpdate();
            }
        }
    }
}
