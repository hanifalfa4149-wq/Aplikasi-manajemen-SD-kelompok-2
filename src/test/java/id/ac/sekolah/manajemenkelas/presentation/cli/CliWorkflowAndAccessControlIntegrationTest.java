package id.ac.sekolah.manajemenkelas.presentation.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import id.ac.sekolah.manajemenkelas.assessment.dao.PostgresAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.attendance.dao.PostgresAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.dashboard.service.DashboardService;
import id.ac.sekolah.manajemenkelas.presentation.cli.academic.AcademicMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MenuNavigator;
import id.ac.sekolah.manajemenkelas.presentation.cli.student.StudentMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.teacher.TeacherMenu;
import id.ac.sekolah.manajemenkelas.report.dao.PostgresReportDAO;
import id.ac.sekolah.manajemenkelas.report.service.ReportService;
import id.ac.sekolah.manajemenkelas.student.dao.InMemoryStudentDAO;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.dao.InMemoryTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

class CliWorkflowAndAccessControlIntegrationTest {

    private Long previouslyActiveYearId;
    private PrintStream originalOut;

    @BeforeEach
    void setup() throws SQLException {
        originalOut = System.out;
        SessionContext.getInstance().endSession();
        cleanupTestRows();
        previouslyActiveYearId = findActiveNonTestYearId();
    }

    @AfterEach
    void teardown() throws SQLException {
        System.setOut(originalOut);
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
    void endToEndCliWorkflowsAdminTeacherAttendanceAssessmentReportsAndLogout() throws Exception {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PostgresStudentDAO studentDAO = new PostgresStudentDAO();
        PostgresTeacherDAO teacherDAO = new PostgresTeacherDAO();
        PostgresAcademicDAO academicDAO = new PostgresAcademicDAO();
        PostgresAttendanceDAO attendanceDAO = new PostgresAttendanceDAO();
        PostgresAssessmentDAO assessmentDAO = new PostgresAssessmentDAO();
        PostgresReportDAO reportDAO = new PostgresReportDAO();

        new AuthBootstrapSeeder(authDAO, new PasswordHasher()).seedDefaultUsers();

        LineQueueInputStream cliInput = new LineQueueInputStream();
        SyncCaptureOutputStream capture = new SyncCaptureOutputStream();
        PrintStream testOut = new PrintStream(capture, true, StandardCharsets.UTF_8);
        System.setOut(testOut);

        CliApplication cli = new CliApplication(
                new Scanner(cliInput, StandardCharsets.UTF_8),
                authDAO,
                studentDAO,
                teacherDAO,
                academicDAO,
                attendanceDAO,
                assessmentDAO,
                reportDAO
        );

        AtomicReference<Throwable> cliError = new AtomicReference<>();
        Thread cliThread = new Thread(() -> {
            try {
                cli.start();
            } catch (Throwable t) {
                cliError.set(t);
            }
        }, "cli-e2e-runner");
        cliThread.start();

        try {
            // ----------------------------------------------------------
            // WORKFLOW 1: Admin login -> create teacher -> create year/semester/class/subject/TA -> create student -> place student
            // ----------------------------------------------------------
            int pos = waitForMarker(capture, 0, "Pilih menu: ");
            sendLines(cliInput, "1", AppConfig.DEFAULT_ADMIN_USERNAME, AppConfig.DEFAULT_ADMIN_PASSWORD);
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");

            // Create teacher via TeacherMenu (3 -> 1)
            sendLines(cliInput,
                    "3", "1",
                    "TEST_P7_G01",
                    "Guru P7 Satu",
                    "P",
                    "gurup7@sd.sch.id",
                    "0812000011",
                    "Jl. Melati No 1",
                    "S1 PGSD",
                    "2022-01-10",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Guru berhasil ditambahkan. ID:");
            Long teacher1Id = extractLastId(capture.sliceFrom(0), "Guru berhasil ditambahkan\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");

            // Create AcademicYear via AcademicMenu (4 -> 1 -> 1)
            sendLines(cliInput,
                    "4",
                    "1", "1",
                    "TEST_P7_2026/2027",
                    "2026-07-01",
                    "2027-06-30",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Tahun ajaran berhasil dibuat. ID:");
            Long yearId = extractLastId(capture.sliceFrom(0), "Tahun ajaran berhasil dibuat\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "STRUKTUR AKADEMIK");

            // Create Semester via AcademicMenu (2 -> 1)
            sendLines(cliInput,
                    "2", "1",
                    String.valueOf(yearId),
                    "1",
                    "Ganjil P7",
                    "2026-07-01",
                    "2026-12-20",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Semester berhasil dibuat. ID:");
            Long semesterId = extractLastId(capture.sliceFrom(0), "Semester berhasil dibuat\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "STRUKTUR AKADEMIK");

            // Create Class via AcademicMenu (3 -> 1)
            sendLines(cliInput,
                    "3", "1",
                    String.valueOf(yearId),
                    "TEST_P7_1A",
                    "1",
                    "30",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Kelas berhasil dibuat. ID:");
            Long class1Id = extractLastId(capture.sliceFrom(0), "Kelas berhasil dibuat\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "STRUKTUR AKADEMIK");

            // Create Subject via AcademicMenu (4 -> 1)
            sendLines(cliInput,
                    "4", "1",
                    "TEST_P7_MTK",
                    "TEST_P7 Matematika",
                    "Mata pelajaran inti P7",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Mata pelajaran berhasil dibuat. ID:");
            Long subject1Id = extractLastId(capture.sliceFrom(0), "Mata pelajaran berhasil dibuat\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "STRUKTUR AKADEMIK");

            // Create TeachingAssignment via AcademicMenu (6 -> 1)
            sendLines(cliInput,
                    "6", "1",
                    String.valueOf(teacher1Id),
                    String.valueOf(class1Id),
                    String.valueOf(subject1Id),
                    String.valueOf(semesterId),
                    "0",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Penugasan mengajar berhasil dibuat. ID:");
            Long ta1Id = extractLastId(capture.sliceFrom(0), "Penugasan mengajar berhasil dibuat\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");

            // Create Student via StudentMenu (2 -> 1 -> 0)
            sendLines(cliInput,
                    "2", "1",
                    "TEST_P7_S01",
                    "TEST_P7_NISN_01",
                    "Budi Santoso P7",
                    "L",
                    "2016-05-12",
                    "Pekanbaru",
                    "Jl. Anggrek No 2",
                    "Slamet P7",
                    "0812000022",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Siswa berhasil ditambahkan. ID:");
            Long student1Id = extractLastId(capture.sliceFrom(0), "Siswa berhasil ditambahkan\\. ID: (\\d+)");
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");

            // Place Student into Class via AcademicMenu (4 -> 7 -> 1 -> 0 -> 0)
            sendLines(cliInput,
                    "4", "7", "1",
                    String.valueOf(student1Id),
                    String.valueOf(class1Id),
                    "2026-07-13",
                    "0",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Siswa berhasil ditempatkan.");
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");

            // Setup secondary unassigned teacher/class/student via services (still logged in as Admin)
            // to prove strict scoping when Teacher 1 logs in next
            Teacher teacher2 = new TeacherService(teacherDAO).createTeacher(
                    "TEST_P7_G02", "Guru P7 Dua", "L", "", "", "Jl. Lain", "S1", LocalDate.of(2022, 1, 1));
            AcademicService academicSvc = new AcademicService(academicDAO, studentDAO, teacherDAO);
            SchoolClass class2 = academicSvc.createClass(yearId, "TEST_P7_1B", 1, 25);
            Student student2 = new StudentService(studentDAO).createStudent(
                    "TEST_P7_S02", "TEST_P7_NISN_02", "Siti P7 Outside", "P",
                    LocalDate.of(2016, 8, 8), "Pekanbaru", "Jl. Luar", "Ani", "0813");
            academicSvc.placeStudentInClass(student2.getId(), class2.getId(), LocalDate.of(2026, 7, 13));
            Subject ipa = academicSvc.createSubject("TEST_P7_IPA", "TEST_P7 IPA", "Luar cakupan");
            academicSvc.createTeachingAssignment(teacher2.getId(), class2.getId(), ipa.getId(), semesterId);

            // Create teacher1 user account linked to teacher1Id
            createUserAccount(authDAO, teacher1Id, "test_p7_guru", "GuruP7#2026", "Guru P7 Satu");

            // Admin Logout (option 9) -> Workflow 5 part 1
            sendLines(cliInput, "9");
            pos = waitForMarker(capture, pos, "Logout berhasil.");
            assertFalse(SessionContext.getInstance().isLoggedIn());
            assertNull(SessionContext.getInstance().getUserId());
            assertNull(SessionContext.getInstance().getRole());

            // ----------------------------------------------------------
            // WORKFLOW 2: Teacher login -> view assigned class -> record attendance
            // ----------------------------------------------------------
            pos = waitForMarker(capture, pos, "Pilih menu: ");
            sendLines(cliInput, "1", "test_p7_guru", "GuruP7#2026");
            int teacherMenuStart = capture.length();
            pos = waitForMarker(capture, pos, "MENU UTAMA GURU");
            String teacherMenuText = capture.sliceFrom(teacherMenuStart);
            assertFalse(teacherMenuText.contains("Manajemen Siswa"));
            assertFalse(teacherMenuText.contains("Manajemen Guru"));
            assertFalse(teacherMenuText.contains("Struktur Akademik"));

            // View assigned classes & students (option 2)
            int scopeViewStart = capture.length();
            sendLines(cliInput, "2");
            pos = waitForMarker(capture, pos, "MENU UTAMA GURU");
            String scopeOutput = capture.sliceFrom(scopeViewStart);
            assertTrue(scopeOutput.contains("TEST_P7_1A"));
            assertTrue(scopeOutput.contains("TEST_P7 Matematika"));
            assertTrue(scopeOutput.contains("Budi Santoso P7"));
            assertFalse(scopeOutput.contains("TEST_P7_1B"));
            assertFalse(scopeOutput.contains("Siti P7 Outside"));

            // Record attendance via AttendanceMenu (option 3 -> 1 -> 0)
            sendLines(cliInput,
                    "3", "1",
                    String.valueOf(class1Id),
                    String.valueOf(student1Id),
                    "2036-09-01",
                    Attendance.STATUS_HADIR,
                    "Hadir via CLI",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Absensi berhasil dicatat.");
            pos = waitForMarker(capture, pos, "MENU UTAMA GURU");

            // ----------------------------------------------------------
            // WORKFLOW 3: Teacher creates assessment -> records score -> views summary
            // ----------------------------------------------------------
            int assessmentStart = capture.length();
            sendLines(cliInput,
                    "4",
                    "1",
                    String.valueOf(ta1Id),
                    "Kuis 1 P7",
                    "KUIS",
                    "2026-09-02",
                    "100",
                    "Latihan CLI"
            );
            pos = waitForMarker(capture, pos, "Penilaian berhasil dibuat. ID:");
            Long assessmentId = extractLastId(capture.sliceFrom(assessmentStart),
                    "Penilaian berhasil dibuat\\. ID: (\\d+)");

            sendLines(cliInput,
                    "4",
                    String.valueOf(assessmentId),
                    String.valueOf(student1Id),
                    "90",
                    "Sangat baik"
            );
            pos = waitForMarker(capture, pos, "Nilai berhasil dicatat. ID:");

            int summaryStart = capture.length();
            sendLines(cliInput,
                    "9",
                    "1",
                    String.valueOf(assessmentId),
                    "0"
            );
            pos = waitForMarker(capture, pos, "--- Ringkasan Nilai Penilaian ---");
            pos = waitForMarker(capture, pos, "MENU UTAMA GURU");
            String assessmentSummaryText = capture.sliceFrom(summaryStart);
            assertTrue(assessmentSummaryText.contains("Kuis 1 P7"));
            assertTrue(assessmentSummaryText.contains("90"));

            // Teacher logout (option 7)
            sendLines(cliInput, "7");
            pos = waitForMarker(capture, pos, "Logout berhasil.");
            assertFalse(SessionContext.getInstance().isLoggedIn());
            assertNull(SessionContext.getInstance().getUserId());
            assertNull(SessionContext.getInstance().getTeacherId());

            // ----------------------------------------------------------
            // WORKFLOW 4 & 5: Admin re-login -> Reports & Dashboard -> Session info -> Logout -> Exit
            // ----------------------------------------------------------
            pos = waitForMarker(capture, pos, "Pilih menu: ");
            sendLines(cliInput, "1", AppConfig.DEFAULT_ADMIN_USERNAME, AppConfig.DEFAULT_ADMIN_PASSWORD);
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");

            int reportStart = capture.length();
            sendLines(cliInput,
                    "7",
                    "1",
                    "2",
                    "3",
                    "5", "",
                    "6",
                    "7", "",
                    "8",
                    "0"
            );
            pos = waitForMarker(capture, pos, "Rata-rata Nilai per Mata Pelajaran");
            pos = waitForMarker(capture, pos, "MENU UTAMA ADMIN");
            String reportText = capture.sliceFrom(reportStart);
            assertTrue(reportText.contains("TEST_P7_1A"));
            assertTrue(reportText.contains("Kuis 1 P7"));
            assertTrue(reportText.contains("Budi Santoso P7"));
            assertTrue(reportText.contains("90.00"));
            assertTrue(reportText.contains("TEST_P7 Matematika"));

            // Check session info (8), logout (9), exit app (0)
            int sessionStart = capture.length();
            sendLines(cliInput, "8", "9", "0");
            pos = waitForMarker(capture, pos, "Terima kasih telah menggunakan aplikasi.");
            String closingText = capture.sliceFrom(sessionStart);
            assertTrue(closingText.contains("Username   : " + AppConfig.DEFAULT_ADMIN_USERNAME));
            assertTrue(closingText.contains("Role       : ADMIN"));
            assertTrue(closingText.contains("Logout berhasil."));
            assertFalse(SessionContext.getInstance().isLoggedIn());

            cliInput.close();
            cliThread.join(5000);
            assertFalse(cliThread.isAlive());
            assertNull(cliError.get());
        } finally {
            cliInput.close();
            cliThread.join(2000);
        }
    }

    @Test
    void teacherMenuAccessControlEmptyListsInvalidInputsAndDbErrorHandlingDoNotCrash() {
        SessionContext.getInstance().startSession(
                99L, 77L, "guru_cli", "Guru CLI", RoleMapper.ROLE_TEACHER);

        // Role-scoped StudentMenu/TeacherMenu/AcademicMenu reject admin actions without crashing,
        // and invalid inputs + empty datasets produce readable messages.
        String scriptedInput = String.join("\n",
                "1",          // StudentMenu choice 1 (unauthorized for TEACHER)
                "bad_choice", // invalid choice
                "2",          // list empty students
                "0",          // exit StudentMenu
                "1",          // TeacherMenu choice 1 (unauthorized for TEACHER)
                "2",          // list empty teachers
                "0",          // exit TeacherMenu
                "1", "1",     // AcademicMenu -> Tahun Ajaran -> 1 (unauthorized)
                "0", "0"      // exit AcademicMenu
        ) + "\n";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(new ByteArrayInputStream(scriptedInput.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);

        StudentService emptyStudentService = new StudentService(new InMemoryStudentDAO());
        TeacherService emptyTeacherService = new TeacherService(new InMemoryTeacherDAO());
        AcademicService emptyAcademicService = new AcademicService(new InMemoryAcademicDAO());

        new StudentMenu(scanner, emptyStudentService, emptyAcademicService).show();
        new TeacherMenu(scanner, emptyTeacherService).show();
        new AcademicMenu(scanner, emptyAcademicService, emptyStudentService, emptyTeacherService).show();

        // Simulate DB / IllegalStateException caught cleanly by MenuNavigator.executeSafely
        MenuNavigator.executeSafely(() -> {
            throw new IllegalStateException("Simulasi koneksi PostgreSQL terputus.");
        });

        String out = baos.toString(StandardCharsets.UTF_8);
        assertFalse(out.contains("1. Tambah Siswa"));
        assertFalse(out.contains("1. Tambah Guru"));
        assertTrue(out.contains("Gagal: Akses ditolak: hanya ADMIN yang dapat mengubah data siswa."));
        assertTrue(out.contains("Gagal: Akses ditolak: hanya ADMIN yang dapat mengubah data guru."));
        assertTrue(out.contains("Gagal: Akses ditolak: fitur ini hanya dapat diakses oleh ADMIN."));
        assertTrue(out.contains("Pilihan tidak valid."));
        assertTrue(out.contains("Belum ada data siswa."));
        assertTrue(out.contains("Belum ada data guru."));
        assertTrue(out.contains("Kesalahan sistem/database: Simulasi koneksi PostgreSQL terputus."));
    }

    @Test
    void servicesRejectUnauthorizedCallsDirectly() {
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

        // Setup data without login (matches existing integration test pattern)
        Teacher t1 = teacherService.createTeacher(
                "TEST_P7_G10", "Guru Sepuluh", "L", "", "", "Jl. A", "S1", LocalDate.of(2020, 1, 1));
        Teacher t2 = teacherService.createTeacher(
                "TEST_P7_G11", "Guru Sebelas", "P", "", "", "Jl. B", "S1", LocalDate.of(2020, 1, 1));
        Student s2 = studentService.createStudent(
                "TEST_P7_S10", "TEST_P7_NISN_10", "Siswa Sepuluh", "L",
                LocalDate.of(2016, 1, 1), "Pekanbaru", "Jl. S", "Ortu", "0811");
        AcademicYear year = academicService.createAcademicYear(
                "TEST_P7_2027/2028", LocalDate.of(2027, 7, 1), LocalDate.of(2028, 6, 30));
        Semester sem = academicService.createSemester(
                year.getId(), 1, "Ganjil 27", LocalDate.of(2027, 7, 1), LocalDate.of(2027, 12, 20));
        SchoolClass c2 = academicService.createClass(year.getId(), "TEST_P7_2B", 2, 25);
        Subject sbj = academicService.createSubject("TEST_P7_IND", "TEST_P7 Bahasa", "Wajib");
        academicService.placeStudentInClass(s2.getId(), c2.getId(), LocalDate.of(2027, 7, 10));
        TeachingAssignment ta2 = academicService.createTeachingAssignment(
                t2.getId(), c2.getId(), sbj.getId(), sem.getId());

        // 1. Unauthenticated calls must fail on session-guarded services
        SessionContext.getInstance().endSession();
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.recordAttendance(s2.getId(), c2.getId(), LocalDate.of(2036, 9, 5), Attendance.STATUS_HADIR, ""));
        assertThrows(IllegalArgumentException.class, () ->
                assessmentService.createAssessment(ta2.getId(), "UTS P7", Assessment.TYPE_UTS,
                        LocalDate.of(2036, 9, 6), BigDecimal.valueOf(100), ""));
        assertThrows(IllegalArgumentException.class, dashboardService::getDashboardSummary);
        assertThrows(IllegalArgumentException.class, reportService::getAttendanceSummaryByClass);

        // 2. Authenticated TEACHER (t1) cannot invoke admin CRUD or touch t2's class/TA/student
        SessionContext.getInstance().startSession(
                501L, t1.getId(), "test_p7_t1", "Guru Sepuluh", RoleMapper.ROLE_TEACHER);

        assertThrows(IllegalArgumentException.class, () ->
                studentService.createStudent("X1", "X2", "Blocked", "L", LocalDate.of(2016, 1, 1), "PKU", "Jl", "Ortu", "08"));
        assertThrows(IllegalArgumentException.class, () ->
                studentService.updateStudent(s2.getId(), "X1", "X2", "Blocked", "L", LocalDate.of(2016, 1, 1), "PKU", "Jl", "Ortu", "08"));
        assertThrows(IllegalArgumentException.class, () -> studentService.deleteStudent(s2.getId()));

        assertThrows(IllegalArgumentException.class, () ->
                teacherService.createTeacher("TG1", "Blocked", "L", "", "", "Jl", "S1", LocalDate.of(2022, 1, 1)));
        assertThrows(IllegalArgumentException.class, () ->
                teacherService.updateTeacher(t2.getId(), "TG1", "Blocked", "L", "", "", "Jl", "S1", LocalDate.of(2022, 1, 1)));
        assertThrows(IllegalArgumentException.class, () -> teacherService.deleteTeacher(t2.getId()));

        assertThrows(IllegalArgumentException.class, () ->
                academicService.createAcademicYear("TEST_P7_BLK", LocalDate.of(2028, 7, 1), LocalDate.of(2029, 6, 30)));
        assertThrows(IllegalArgumentException.class, () ->
                academicService.createClass(year.getId(), "TEST_P7_BLK", 3, 20));
        assertThrows(IllegalArgumentException.class, () ->
                attendanceService.recordAttendance(s2.getId(), c2.getId(), LocalDate.of(2036, 9, 5), Attendance.STATUS_HADIR, ""));
        assertThrows(IllegalArgumentException.class, () ->
                assessmentService.createAssessment(ta2.getId(), "Blocked UTS", Assessment.TYPE_UTS,
                        LocalDate.of(2036, 9, 6), BigDecimal.valueOf(100), ""));
        assertThrows(IllegalArgumentException.class, () ->
                reportService.getStudentScoreSummary(s2.getId()));
    }

    private void createUserAccount(
            PostgresAuthDAO authDAO,
            Long teacherId,
            String username,
            String rawPassword,
            String fullName) {
        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword(rawPassword, salt);
        User user = new User(null, teacherId, username, hash, salt, fullName, RoleMapper.ROLE_TEACHER, true, null);
        authDAO.save(user);
        User saved = authDAO.findByUsername(username).orElseThrow();
        assertNotNull(saved.getId());
        assertEquals(teacherId, saved.getTeacherId());
    }

    private static void sendLines(LineQueueInputStream input, String... lines) {
        for (String line : lines) {
            input.pushLine(line);
        }
    }

    private static int waitForMarker(SyncCaptureOutputStream capture, int fromIndex, String marker)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15000;
        while (System.currentTimeMillis() < deadline) {
            String slice = capture.sliceFrom(fromIndex);
            int idx = slice.indexOf(marker);
            if (idx >= 0) {
                return fromIndex + idx + marker.length();
            }
            Thread.sleep(15);
        }
        throw new AssertionError("Timeout menunggu output CLI: '" + marker + "'\nOutput sejauh ini:\n"
                + capture.sliceFrom(fromIndex));
    }

    private static Long extractLastId(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        Long found = null;
        while (matcher.find()) {
            found = Long.parseLong(matcher.group(1));
        }
        assertNotNull(found, "Tidak menemukan ID dengan pola '" + regex + "' pada output:\n" + text);
        return found;
    }

    private Long findActiveNonTestYearId() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_P7_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM nilai_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P7_%') "
                            + "OR id_penilaian IN (SELECT id FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P7_%')))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penilaian WHERE id_penugasan_mengajar IN "
                            + "(SELECT id FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P7_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P7_%'))")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM absensi WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P7_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P7_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM riwayat_kelas_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P7_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P7_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P7_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P7_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_wali_kelas WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P7_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P7_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM semester WHERE id_tahun_ajaran IN "
                            + "(SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P7_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_P7_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P7_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_P7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_P7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pengguna WHERE nama_pengguna LIKE 'test_p7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_P7_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM siswa WHERE nis LIKE 'TEST_P7_%'")) {
                ps.executeUpdate();
            }
        }
    }

    private static final class LineQueueInputStream extends InputStream {
        private static final byte[] EOF_MARKER = new byte[0];
        private final BlockingQueue<byte[]> queue = new LinkedBlockingQueue<>();
        private byte[] current;
        private int cursor;
        private volatile boolean closed;

        void pushLine(String line) {
            if (!closed) {
                queue.add((line + "\n").getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                queue.add(EOF_MARKER);
            }
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int count = read(one, 0, 1);
            return count == -1 ? -1 : (one[0] & 0xFF);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            while (current == null || cursor >= current.length) {
                if (current == EOF_MARKER) {
                    return -1;
                }
                try {
                    byte[] next = queue.poll(15, TimeUnit.SECONDS);
                    if (next == null) {
                        return -1;
                    }
                    if (next == EOF_MARKER) {
                        current = EOF_MARKER;
                        return -1;
                    }
                    current = next;
                    cursor = 0;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
            int toCopy = Math.min(len, current.length - cursor);
            System.arraycopy(current, cursor, b, off, toCopy);
            cursor += toCopy;
            return toCopy;
        }
    }

    private static final class SyncCaptureOutputStream extends java.io.OutputStream {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public synchronized void write(int b) {
            buffer.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            buffer.write(b, off, len);
        }

        synchronized int length() {
            return buffer.toString(StandardCharsets.UTF_8).length();
        }

        synchronized String sliceFrom(int index) {
            String full = buffer.toString(StandardCharsets.UTF_8);
            return index >= full.length() ? "" : full.substring(index);
        }
    }
}
