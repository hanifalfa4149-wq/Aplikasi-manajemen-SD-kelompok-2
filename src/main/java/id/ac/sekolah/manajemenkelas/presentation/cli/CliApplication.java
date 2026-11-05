package id.ac.sekolah.manajemenkelas.presentation.cli;

import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.dao.InMemoryAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;

import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.dao.InMemoryStudentDAO;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;

import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.dao.InMemoryTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.dao.InMemoryAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;

import id.ac.sekolah.manajemenkelas.presentation.cli.auth.AuthMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.student.StudentMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.teacher.TeacherMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.academic.AcademicMenu;

import java.util.Scanner;

public class CliApplication {

    private final Scanner scanner;
    private final SessionContext sessionContext;

    private final AuthService authService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AcademicService academicService;

    private boolean running;

    public CliApplication() {

        scanner = new Scanner(System.in);
        sessionContext = SessionContext.getInstance();

        // Initialize DAO
        AuthDAO authDAO = new InMemoryAuthDAO();
        StudentDAO studentDAO = new InMemoryStudentDAO();
        TeacherDAO teacherDAO = new InMemoryTeacherDAO();
        AcademicDAO academicDAO = new InMemoryAcademicDAO();

        // Initialize Service
        authService = new AuthService(authDAO);
        studentService = new StudentService(studentDAO);
        teacherService = new TeacherService(teacherDAO);
        academicService = new AcademicService(academicDAO);
    }

    public void start() {

        running = true;

        AppConfig.displayApplicationInfo();

        while (running) {

            if (!sessionContext.isLoggedIn()) {
                showLoginMenu();
            } else {
                showMainMenu();
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

        String choice = scanner.nextLine().trim();

        switch (choice) {

            case "1" -> login();

            case "0" -> running = false;

            default -> System.out.println("Pilihan tidak valid.");
        }
    }

    private void login() {

        System.out.println("\n========== LOGIN ==========");

        System.out.print("Username: ");
        String username = scanner.nextLine();

        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (authService.login(username, password)) {

            System.out.println("\nLogin berhasil!");
            System.out.println("Selamat datang, "
                    + sessionContext.getFullName());

        } else {

            System.out.println("\nLogin gagal. Periksa username dan password.");
        }
    }

    private void showMainMenu() {

        System.out.println("\n====================================");
        System.out.println("          MENU UTAMA");
        System.out.println("====================================");
        System.out.println("Pengguna : " + sessionContext.getFullName());
        System.out.println("Role     : " + sessionContext.getRole());
        System.out.println("------------------------------------");
        System.out.println("1. Manajemen Siswa");
        System.out.println("2. Manajemen Guru");
        System.out.println("3. Struktur Akademik");
        System.out.println("4. Logout");
        System.out.println("0. Keluar");
        System.out.print("Pilih menu: ");

        String choice = scanner.nextLine().trim();

        switch (choice) {

            case "1" -> {
                if (canManageData()) {
                    new StudentMenu(scanner, studentService).show();
                } else {
                    System.out.println("Anda tidak memiliki akses.");
                }
            }

            case "2" -> {
                if (canManageData()) {
                    new TeacherMenu(scanner, teacherService).show();
                } else {
                    System.out.println("Anda tidak memiliki akses.");
                }
            }

            case "3" -> {
                if (canManageData()) {
                    new AcademicMenu(scanner, academicService).show();
                } else {
                    System.out.println("Anda tidak memiliki akses.");
                }
            }

            case "4" -> logout();

            case "0" -> running = false;

            default -> System.out.println("Pilihan tidak valid.");
        }
    }

    private boolean canManageData() {
        return sessionContext.hasRole("ADMIN")
                || sessionContext.hasRole("TEACHER");
    }

    private void logout() {

        authService.logout();

        System.out.println("\nLogout berhasil.");
    }
}