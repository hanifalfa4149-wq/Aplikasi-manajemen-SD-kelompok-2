package id.ac.sekolah.manajemenkelas.presentation.cli.menu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.dashboard.service.DashboardService;
import id.ac.sekolah.manajemenkelas.presentation.cli.academic.AcademicMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.assessment.AssessmentMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.attendance.AttendanceMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.auth.AuthMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.report.ReportMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.student.StudentMenu;
import id.ac.sekolah.manajemenkelas.presentation.cli.teacher.TeacherMenu;
import id.ac.sekolah.manajemenkelas.report.service.ReportService;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

public class MainMenu {

    private final Scanner scanner;
    private final SessionContext sessionContext;
    private final AuthService authService;
    private final AuthMenu authMenu;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AcademicService academicService;
    private final AttendanceService attendanceService;
    private final AssessmentService assessmentService;
    private final DashboardService dashboardService;
    private final ReportService reportService;

    public MainMenu(
            Scanner scanner,
            AuthService authService,
            AuthMenu authMenu,
            StudentService studentService,
            TeacherService teacherService,
            AcademicService academicService,
            AttendanceService attendanceService,
            AssessmentService assessmentService,
            DashboardService dashboardService,
            ReportService reportService) {
        this.scanner = scanner;
        this.sessionContext = SessionContext.getInstance();
        this.authService = authService;
        this.authMenu = authMenu;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.academicService = academicService;
        this.attendanceService = attendanceService;
        this.assessmentService = assessmentService;
        this.dashboardService = dashboardService;
        this.reportService = reportService;
    }

    /**
     * Menampilkan menu utama berbasis peran dan mengembalikan true jika aplikasi terus berjalan.
     */
    public boolean show() {
        while (sessionContext.isLoggedIn()) {
            if (MenuNavigator.isAdmin()) {
                boolean keepRunning = showAdminMenu();
                if (!keepRunning) {
                    return false;
                }
            } else if (MenuNavigator.isTeacher()) {
                boolean keepRunning = showTeacherMenu();
                if (!keepRunning) {
                    return false;
                }
            } else {
                System.out.println("Peran pengguna tidak dikenali. Logout otomatis.");
                logout();
                return true;
            }
        }
        return true;
    }

    private boolean showAdminMenu() {
        printHeader("MENU UTAMA ADMIN");
        System.out.println("1. Dashboard Ringkasan");
        System.out.println("2. Manajemen Siswa");
        System.out.println("3. Manajemen Guru");
        System.out.println("4. Struktur Akademik & Penugasan");
        System.out.println("5. Absensi Harian");
        System.out.println("6. Penilaian & Nilai Siswa");
        System.out.println("7. Laporan & Rekapitulasi");
        System.out.println("8. Informasi Sesi");
        System.out.println("9. Logout");
        System.out.println("0. Keluar");
        System.out.print("Pilih menu: ");

        String choice = MenuNavigator.readMenuChoice(scanner);
        switch (choice) {
            case "1" -> MenuNavigator.executeSafely(() ->
                    new ReportMenu(scanner, dashboardService, reportService).displayDashboard());
            case "2" -> MenuNavigator.executeSafely(() ->
                    new StudentMenu(scanner, studentService, academicService).show());
            case "3" -> MenuNavigator.executeSafely(() ->
                    new TeacherMenu(scanner, teacherService).show());
            case "4" -> MenuNavigator.executeSafely(() ->
                    new AcademicMenu(scanner, academicService, studentService, teacherService).show());
            case "5" -> MenuNavigator.executeSafely(() ->
                    new AttendanceMenu(scanner, attendanceService, academicService, studentService).show());
            case "6" -> MenuNavigator.executeSafely(() ->
                    new AssessmentMenu(scanner, assessmentService, academicService, studentService, teacherService)
                            .show());
            case "7" -> MenuNavigator.executeSafely(() ->
                    new ReportMenu(scanner, dashboardService, reportService).show());
            case "8" -> authMenu.displaySessionInfo();
            case "9" -> logout();
            case "0" -> {
                return false;
            }
            default -> System.out.println("Pilihan tidak valid.");
        }
        return true;
    }

    private boolean showTeacherMenu() {
        printHeader("MENU UTAMA GURU");
        System.out.println("1. Dashboard Ringkasan Saya");
        System.out.println("2. Kelas, Mapel & Siswa yang Diampu");
        System.out.println("3. Absensi Harian");
        System.out.println("4. Penilaian & Nilai Siswa");
        System.out.println("5. Laporan & Rekapitulasi");
        System.out.println("6. Informasi Sesi");
        System.out.println("7. Logout");
        System.out.println("0. Keluar");
        System.out.print("Pilih menu: ");

        String choice = MenuNavigator.readMenuChoice(scanner);
        switch (choice) {
            case "1" -> MenuNavigator.executeSafely(() ->
                    new ReportMenu(scanner, dashboardService, reportService).displayDashboard());
            case "2" -> MenuNavigator.executeSafely(this::displayTeacherAssignedClassesAndStudents);
            case "3" -> MenuNavigator.executeSafely(() ->
                    new AttendanceMenu(scanner, attendanceService, academicService, studentService).show());
            case "4" -> MenuNavigator.executeSafely(() ->
                    new AssessmentMenu(scanner, assessmentService, academicService, studentService, teacherService)
                            .show());
            case "5" -> MenuNavigator.executeSafely(() ->
                    new ReportMenu(scanner, dashboardService, reportService).show());
            case "6" -> authMenu.displaySessionInfo();
            case "7" -> logout();
            case "0" -> {
                return false;
            }
            default -> System.out.println("Pilihan tidak valid.");
        }
        return true;
    }

    private void displayTeacherAssignedClassesAndStudents() {
        Long teacherId = sessionContext.getTeacherId();
        if (teacherId == null) {
            throw new IllegalArgumentException(
                    "Akun guru belum terhubung dengan profil guru.");
        }

        Set<Long> assignedClassIds = new LinkedHashSet<>();
        for (HomeroomAssignment hr : academicService.getAllHomeroomAssignments()) {
            if (hr.isActive() && teacherId.equals(hr.getTeacherId())) {
                assignedClassIds.add(hr.getClassId());
            }
        }

        List<TeachingAssignment> teachingAssignments = academicService.getTeachingAssignmentsByTeacher(teacherId)
                .stream()
                .filter(TeachingAssignment::isActive)
                .toList();
        for (TeachingAssignment ta : teachingAssignments) {
            assignedClassIds.add(ta.getClassId());
        }

        System.out.println("\n========== KELAS & SISWA YANG DIAMPU ==========");
        if (assignedClassIds.isEmpty()) {
            System.out.println("Anda belum memiliki penugasan kelas aktif.");
            return;
        }

        System.out.println("-- Daftar Penugasan Mengajar Aktif --");
        if (teachingAssignments.isEmpty()) {
            System.out.println("Belum ada penugasan mengajar mata pelajaran.");
        } else {
            for (TeachingAssignment ta : teachingAssignments) {
                String className = academicService.getClassById(ta.getClassId())
                        .map(SchoolClass::getName)
                        .orElse("Kelas #" + ta.getClassId());
                String subjectName = academicService.getSubjectById(ta.getSubjectId())
                        .map(Subject::getName)
                        .orElse("Mapel #" + ta.getSubjectId());
                System.out.println(" - Penugasan ID " + ta.getId()
                        + " | Kelas: " + className
                        + " | Mapel: " + subjectName);
            }
        }

        System.out.println("\n-- Daftar Kelas & Siswa Aktif --");
        for (Long classId : assignedClassIds) {
            SchoolClass schoolClass = academicService.getClassById(classId).orElse(null);
            String label = schoolClass != null
                    ? schoolClass.getName() + " (Tingkat " + schoolClass.getGradeLevel() + ")"
                    : "Kelas #" + classId;
            System.out.println("\nKelas [" + classId + "] " + label + ":");

            List<StudentPlacement> placements = academicService.getActivePlacementsByClass(classId);
            if (placements.isEmpty()) {
                System.out.println("  Belum ada siswa aktif di kelas ini.");
            } else {
                for (StudentPlacement p : placements) {
                    Student student = studentService.getStudentById(p.getStudentId()).orElse(null);
                    String name = student != null ? student.getNis() + " - " + student.getFullName()
                            : "Siswa #" + p.getStudentId();
                    System.out.println("  - [" + p.getStudentId() + "] " + name);
                }
            }
        }
    }

    private void printHeader(String title) {
        System.out.println("\n====================================");
        System.out.println("          " + title);
        System.out.println("====================================");
        System.out.println("Pengguna : " + sessionContext.getFullName());
        System.out.println("Role     : " + sessionContext.getRole());
        System.out.println("------------------------------------");
    }

    private void logout() {
        authService.logout();
        System.out.println("\nLogout berhasil.");
    }
}
