package id.ac.sekolah.manajemenkelas.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.attendance.dao.PostgresAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.model.AttendanceSummary;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
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

class PostgresAttendanceDAOIntegrationTest {

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
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_P4_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM absensi WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P4_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P4_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM riwayat_kelas_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P4_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P4_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P4_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P4_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_wali_kelas WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P4_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P4_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM semester WHERE id_tahun_ajaran IN "
                            + "(SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P4_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_P4_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P4_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_P4_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_P4_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pengguna WHERE nama_pengguna LIKE 'test_p4_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_P4_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM siswa WHERE nis LIKE 'TEST_P4_%'")) {
                ps.executeUpdate();
            }
        }
    }

    private AttendanceService createAttendanceService() {
        return new AttendanceService(
                new PostgresAttendanceDAO(),
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
    void recordBatchUpdateSummaryAndRestartPersistenceWorkWithPostgres() {
        User adminUser = loginAsSeededAdmin();

        StudentService studentService = new StudentService(new PostgresStudentDAO());
        AcademicService academicService = createAcademicService();
        AttendanceService attendanceService = createAttendanceService();

        Student s1 = studentService.createStudent(
                "TEST_P4_NIS_01", "TEST_P4_NISN_01", "Ani Lestari", "P",
                LocalDate.of(2016, 3, 10), "Pekanbaru", "Jl. Melati", "Bapak Andi", "08120001");
        Student s2 = studentService.createStudent(
                "TEST_P4_NIS_02", "TEST_P4_NISN_02", "Budi Pratama", "L",
                LocalDate.of(2016, 5, 14), "Pekanbaru", "Jl. Mawar", "Ibu Sari", "08120002");

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P4_2026/2027", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P4_1A", 1, 30);

        academicService.placeStudentInClass(s1.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(s2.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));

        LocalDate day1 = LocalDate.of(2026, 8, 3);
        LocalDate day2 = LocalDate.of(2026, 8, 4);

        // Single record + lower-case status normalization
        Attendance a1 = attendanceService.recordAttendance(
                s1.getId(), class1A.getId(), day1, "hadir", "Tepat waktu");
        assertNotNull(a1.getId());
        assertEquals(Attendance.STATUS_HADIR, a1.getStatus());
        assertEquals(adminUser.getId(), a1.getRecordedBy());

        // Single record for s2 on day1
        Attendance a2 = attendanceService.recordAttendance(
                s2.getId(), class1A.getId(), day1, "SAKIT", "Demam");
        assertEquals(Attendance.STATUS_SAKIT, a2.getStatus());

        // Batch transactional recording for day2
        List<Attendance> day2Records = attendanceService.recordClassAttendance(
                class1A.getId(),
                day2,
                List.of(
                        new AttendanceService.StudentAttendanceEntry(s1.getId(), "IZIN", "Urusan keluarga"),
                        new AttendanceService.StudentAttendanceEntry(s2.getId(), "ALPA", "Tanpa keterangan")
                ));
        assertEquals(2, day2Records.size());

        // Update s2 on day2 from ALPA -> HADIR
        Attendance updated = attendanceService.updateAttendance(
                day2Records.get(1).getId(), Attendance.STATUS_HADIR, "Koreksi hadir terlambat");
        assertEquals(Attendance.STATUS_HADIR, updated.getStatus());
        assertEquals("Koreksi hadir terlambat", updated.getNotes());

        // Date range summary
        AttendanceSummary summary = attendanceService.getClassAttendanceSummary(
                class1A.getId(), day1, day2);
        assertEquals(4, summary.getTotalRecords());
        assertEquals(2, summary.getHadirCount());
        assertEquals(1, summary.getSakitCount());
        assertEquals(1, summary.getIzinCount());
        assertEquals(0, summary.getAlpaCount());

        // Simulate application restart with fresh DAO & Service instances
        AttendanceService restartedService = createAttendanceService();
        List<Attendance> persistedClassRows = restartedService.getAttendanceByClass(class1A.getId());
        assertEquals(4, persistedClassRows.size());
        assertEquals(2, restartedService.getAttendanceByClassAndDate(class1A.getId(), day1).size());
        assertEquals(2, restartedService.getAttendanceByStudent(s1.getId()).size());
        Attendance reloaded = restartedService.getAttendanceById(updated.getId()).orElseThrow();
        assertEquals(Attendance.STATUS_HADIR, reloaded.getStatus());
        assertEquals("Koreksi hadir terlambat", reloaded.getNotes());
        assertEquals(adminUser.getId(), reloaded.getRecordedBy());
    }

    @Test
    void validationPreventsInvalidStatusDuplicateAndWrongClassPlacement() {
        loginAsSeededAdmin();

        StudentService studentService = new StudentService(new PostgresStudentDAO());
        AcademicService academicService = createAcademicService();
        AttendanceService attendanceService = createAttendanceService();

        Student studentIn1A = studentService.createStudent(
                "TEST_P4_NIS_03", "TEST_P4_NISN_03", "Citra Dewi", "P",
                LocalDate.of(2016, 6, 1), "Pekanbaru", "Jl. Dahlia", "Ibu Rina", "08120003");
        Student unplacedStudent = studentService.createStudent(
                "TEST_P4_NIS_04", "TEST_P4_NISN_04", "Dodi Kurniawan", "L",
                LocalDate.of(2016, 7, 2), "Pekanbaru", "Jl. Kenanga", "Bapak Eko", "08120004");

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P4_2026/2027_VAL", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P4_1A_VAL", 1, 30);
        SchoolClass class1B = academicService.createClass(year.getId(), "TEST_P4_1B_VAL", 1, 30);

        academicService.placeStudentInClass(
                studentIn1A.getId(), class1A.getId(), LocalDate.of(2026, 7, 15));

        LocalDate validDate = LocalDate.of(2026, 8, 5);

        // Invalid status
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                studentIn1A.getId(), class1A.getId(), validDate, "BOLOS", ""));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                studentIn1A.getId(), class1A.getId(), validDate, "  ", ""));

        // Unplaced / wrong class relationship
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                unplacedStudent.getId(), class1A.getId(), validDate, Attendance.STATUS_HADIR, ""));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                studentIn1A.getId(), class1B.getId(), validDate, Attendance.STATUS_HADIR, ""));
        // Date before student entry date
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                studentIn1A.getId(), class1A.getId(), LocalDate.of(2026, 7, 10), Attendance.STATUS_HADIR, ""));

        // Valid insert then duplicate rejection on same student & date
        attendanceService.recordAttendance(
                studentIn1A.getId(), class1A.getId(), validDate, Attendance.STATUS_HADIR, "Hadir");
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                studentIn1A.getId(), class1A.getId(), validDate, Attendance.STATUS_IZIN, "Duplikat"));

        // Transfer to class1B on 2026-08-10: old class 1A rejected on/after transfer date
        academicService.promoteOrTransferStudent(
                studentIn1A.getId(), class1B.getId(), LocalDate.of(2026, 8, 10), StudentPlacement.STATUS_PINDAH);
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                studentIn1A.getId(), class1A.getId(), LocalDate.of(2026, 8, 10), Attendance.STATUS_HADIR, ""));
        Attendance moved = attendanceService.recordAttendance(
                studentIn1A.getId(), class1B.getId(), LocalDate.of(2026, 8, 10), Attendance.STATUS_HADIR, "Di 1B");
        assertEquals(class1B.getId(), moved.getClassId());
    }

    @Test
    void teacherAuthorizationAllowsAssignedClassesAndRejectsUnauthorizedClasses() {
        loginAsSeededAdmin();

        TeacherService teacherService = new TeacherService(new PostgresTeacherDAO());
        StudentService studentService = new StudentService(new PostgresStudentDAO());
        AcademicService academicService = createAcademicService();
        AttendanceService attendanceService = createAttendanceService();

        Teacher assignedTeacher = teacherService.createTeacher(
                "TEST_P4_NIP_01", "Guru Kelas 1A", "P", "", "", "Jl. Guru 1", "S1", LocalDate.of(2020, 1, 1));
        Teacher otherTeacher = teacherService.createTeacher(
                "TEST_P4_NIP_02", "Guru Kelas 1B", "L", "", "", "Jl. Guru 2", "S1", LocalDate.of(2021, 1, 1));

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P4_2026/2027_AUTH", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        Semester sem1 = academicService.createSemester(
                year.getId(), 1, "Ganjil P4", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P4_1A_AUTH", 1, 30);
        SchoolClass class1B = academicService.createClass(year.getId(), "TEST_P4_1B_AUTH", 1, 30);
        Subject subject = academicService.createSubject("TEST_P4_BI", "Bahasa Indonesia", "Wajib");

        Student student1A = studentService.createStudent(
                "TEST_P4_NIS_05", "TEST_P4_NISN_05", "Eko Saputra", "L",
                LocalDate.of(2016, 8, 5), "Pekanbaru", "Jl. Flamboyan", "Bapak Teguh", "08120005");
        Student student1B = studentService.createStudent(
                "TEST_P4_NIS_06", "TEST_P4_NISN_06", "Fitri Handayani", "P",
                LocalDate.of(2016, 9, 9), "Pekanbaru", "Jl. Teratai", "Ibu Wati", "08120006");

        academicService.placeStudentInClass(student1A.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        academicService.placeStudentInClass(student1B.getId(), class1B.getId(), LocalDate.of(2026, 7, 10));

        // assignedTeacher is homeroom for 1A; otherTeacher teaches 1B
        academicService.assignHomeroomTeacher(class1A.getId(), assignedTeacher.getId(), LocalDate.of(2026, 7, 1));
        academicService.createTeachingAssignment(otherTeacher.getId(), class1B.getId(), subject.getId(), sem1.getId());

        LocalDate date = LocalDate.of(2026, 8, 6);
        attendanceService.recordAttendance(
                student1B.getId(), class1B.getId(), date, Attendance.STATUS_HADIR, "Admin catat 1B");

        // Switch session to assignedTeacher (linked via pengguna.id_guru)
        User teacherUser = createAndLoginTeacherUser(
                assignedTeacher.getId(), "test_p4_guru_1a", "Guru Kelas 1A");
        assertEquals(assignedTeacher.getId(), SessionContext.getInstance().getTeacherId());

        // Allowed for class1A
        Attendance recordedByTeacher = attendanceService.recordAttendance(
                student1A.getId(), class1A.getId(), date, Attendance.STATUS_HADIR, "Wali kelas 1A");
        assertEquals(teacherUser.getId(), recordedByTeacher.getRecordedBy());
        assertEquals(1, attendanceService.getAttendanceByClass(class1A.getId()).size());

        // getAttendanceByDate filters out class1B for assignedTeacher
        List<Attendance> dateRowsForTeacher = attendanceService.getAttendanceByDate(date);
        assertEquals(1, dateRowsForTeacher.size());
        assertEquals(class1A.getId(), dateRowsForTeacher.get(0).getClassId());

        // Unauthorized for class1B and student1B
        assertThrows(IllegalArgumentException.class, () -> attendanceService.recordAttendance(
                student1B.getId(), class1B.getId(), LocalDate.of(2026, 8, 7), Attendance.STATUS_HADIR, ""));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.getAttendanceByClass(class1B.getId()));
        assertThrows(IllegalArgumentException.class, () -> attendanceService.getAttendanceByStudent(student1B.getId()));

        // Teacher user without id_guru cannot manage attendance
        createAndLoginTeacherUser(null, "test_p4_guru_unlinked", "Guru Tanpa Link");
        assertThrows(IllegalArgumentException.class, () -> attendanceService.getAttendanceByClass(class1A.getId()));
    }
}
