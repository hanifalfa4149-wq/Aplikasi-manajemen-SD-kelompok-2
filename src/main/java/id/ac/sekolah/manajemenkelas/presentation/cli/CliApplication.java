package id.ac.sekolah.manajemenkelas.presentation.cli;

import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.dao.InMemoryAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;

import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.dao.InMemoryStudentDAO;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;

import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.dao.InMemoryTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.dao.InMemoryAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;

import id.ac.sekolah.manajemenkelas.attendance.dao.AttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.dao.InMemoryAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.dao.PostgresAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;

import id.ac.sekolah.manajemenkelas.assessment.dao.AssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.dao.InMemoryAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.dao.PostgresAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;

import id.ac.sekolah.manajemenkelas.dashboard.service.DashboardService;
import id.ac.sekolah.manajemenkelas.report.dao.InMemoryReportDAO;
import id.ac.sekolah.manajemenkelas.report.dao.PostgresReportDAO;
import id.ac.sekolah.manajemenkelas.report.dao.ReportDAO;
import id.ac.sekolah.manajemenkelas.report.service.ReportService;

import id.ac.sekolah.manajemenkelas.presentation.cli.auth.AuthMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MainMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MenuNavigator;
import id.ac.sekolah.manajemenkelas.presentation.cli.student.StudentMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.teacher.TeacherMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.academic.AcademicMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.attendance.AttendanceMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.assessment.AssessmentMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.report.ReportMenu;

import java.util.Scanner;

public class CliApplication {

    private final Scanner scanner;
    private final SessionContext sessionContext;

    private final AuthService authService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AcademicService academicService;
    private final AttendanceService attendanceService;
    private final AssessmentService assessmentService;
    private final DashboardService dashboardService;
    private final ReportService reportService;

    private boolean running;

    public CliApplication() {
        scanner = new Scanner(System.in);
        sessionContext = SessionContext.getInstance();

        AuthDAO authDAO;
        StudentDAO studentDAO;
        TeacherDAO teacherDAO;
        AcademicDAO academicDAO;
        AttendanceDAO attendanceDAO;
        AssessmentDAO assessmentDAO;
        ReportDAO reportDAO;

        PasswordHasher passwordHasher = new PasswordHasher();
        if (DatabaseConfig.isConnectionAvailable()) {
            try {
                authDAO = new PostgresAuthDAO();
                new AuthBootstrapSeeder(authDAO, passwordHasher).seedDefaultUsers();
                studentDAO = new PostgresStudentDAO();
                teacherDAO = new PostgresTeacherDAO();
                academicDAO = new PostgresAcademicDAO();
                attendanceDAO = new PostgresAttendanceDAO();
                assessmentDAO = new PostgresAssessmentDAO();
                reportDAO = new PostgresReportDAO();
            } catch (RuntimeException e) {
                System.out.println("[PERINGATAN] Gagal menginisialisasi persistensi PostgreSQL, "
                        + "menggunakan mode fallback InMemory.");
                authDAO = new InMemoryAuthDAO(passwordHasher);
                studentDAO = new InMemoryStudentDAO();
                teacherDAO = new InMemoryTeacherDAO();
                academicDAO = new InMemoryAcademicDAO();
                attendanceDAO = new InMemoryAttendanceDAO();
                assessmentDAO = new InMemoryAssessmentDAO();
                reportDAO = new InMemoryReportDAO(studentDAO, teacherDAO, academicDAO, attendanceDAO, assessmentDAO);
            }
        } else {
            System.out.println("[PERINGATAN] Koneksi PostgreSQL tidak tersedia, "
                    + "menggunakan mode fallback InMemory.");
            authDAO = new InMemoryAuthDAO(passwordHasher);
            studentDAO = new InMemoryStudentDAO();
            teacherDAO = new InMemoryTeacherDAO();
            academicDAO = new InMemoryAcademicDAO();
            attendanceDAO = new InMemoryAttendanceDAO();
            assessmentDAO = new InMemoryAssessmentDAO();
            reportDAO = new InMemoryReportDAO(studentDAO, teacherDAO, academicDAO, attendanceDAO, assessmentDAO);
        }

        authService = new AuthService(authDAO, passwordHasher);
        studentService = new StudentService(studentDAO);
        teacherService = new TeacherService(teacherDAO);
        academicService = new AcademicService(academicDAO, studentDAO, teacherDAO);
        attendanceService = new AttendanceService(attendanceDAO, academicDAO, studentDAO);
        assessmentService = new AssessmentService(assessmentDAO, academicDAO, studentDAO);
        dashboardService = new DashboardService(reportDAO);
        reportService = new ReportService(reportDAO, studentDAO);
    }

    public CliApplication(
            AuthDAO authDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO) {
        this(authDAO, studentDAO, teacherDAO, academicDAO,
                new InMemoryAttendanceDAO(), new InMemoryAssessmentDAO());
    }

    public CliApplication(
            AuthDAO authDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO,
            AttendanceDAO attendanceDAO) {
        this(authDAO, studentDAO, teacherDAO, academicDAO,
                attendanceDAO, new InMemoryAssessmentDAO());
    }

    public CliApplication(
            AuthDAO authDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO,
            AttendanceDAO attendanceDAO,
            AssessmentDAO assessmentDAO) {
        this(authDAO, studentDAO, teacherDAO, academicDAO, attendanceDAO, assessmentDAO,
                new InMemoryReportDAO(studentDAO, teacherDAO, academicDAO, attendanceDAO, assessmentDAO));
    }

    public CliApplication(
            AuthDAO authDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO,
            AttendanceDAO attendanceDAO,
            AssessmentDAO assessmentDAO,
            ReportDAO reportDAO) {
        this(new Scanner(System.in), authDAO, studentDAO, teacherDAO, academicDAO,
                attendanceDAO, assessmentDAO, reportDAO);
    }

    public CliApplication(
            Scanner scanner,
            AuthDAO authDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO,
            AttendanceDAO attendanceDAO,
            AssessmentDAO assessmentDAO,
            ReportDAO reportDAO) {
        this.scanner = scanner;
        sessionContext = SessionContext.getInstance();

        authService = new AuthService(authDAO);
        studentService = new StudentService(studentDAO);
        teacherService = new TeacherService(teacherDAO);
        academicService = new AcademicService(academicDAO, studentDAO, teacherDAO);
        attendanceService = new AttendanceService(attendanceDAO, academicDAO, studentDAO);
        assessmentService = new AssessmentService(assessmentDAO, academicDAO, studentDAO);
        dashboardService = new DashboardService(reportDAO);
        reportService = new ReportService(reportDAO, studentDAO);
    }

    public void start() {

        running = true;

        AppConfig.displayApplicationInfo();

        MainMenu mainMenu = new MainMenu(
                scanner,
                authService,
                new AuthMenu(),
                studentService,
                teacherService,
                academicService,
                attendanceService,
                assessmentService,
                dashboardService,
                reportService);

        while (running) {

            if (!sessionContext.isLoggedIn()) {
                showLoginMenu();
            } else {
                running = mainMenu.show();
            }
        }

        System.out.println("\nTerima kasih telah menggunakan aplikasi.");
    }

    private void showLoginMenu() {

        System.out.println("\n====================================");
        System.out.println("             LOGIN");
        System.out.println("====================================");
        System.out.println("1. Login");
        System.out.println("0. Keluar");
        System.out.print("Pilih menu: ");

        String choice = MenuNavigator.readMenuChoice(scanner);

        switch (choice) {

            case "1" -> MenuNavigator.executeSafely(this::login);

            case "0" -> running = false;

            default -> System.out.println("Pilihan tidak valid.");
        }
    }

    private void login() {

        System.out.println("\n========== LOGIN ==========");

        System.out.print("Username: ");
        String username = scanner.hasNextLine() ? scanner.nextLine() : "";

        System.out.print("Password: ");
        String password = scanner.hasNextLine() ? scanner.nextLine() : "";

        if (authService.login(username, password)) {

            System.out.println("\nLogin berhasil!");
            System.out.println("Selamat datang, "
                    + sessionContext.getFullName());

        } else {

            System.out.println("\nLogin gagal. Periksa username dan password.");
        }
    }
}