package id.ac.sekolah.manajemenkelas.presentation.swing.app;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.assessment.dao.AssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.dao.PostgresAssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.attendance.dao.AttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.dao.PostgresAttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.dashboard.service.DashboardService;
import id.ac.sekolah.manajemenkelas.report.dao.PostgresReportDAO;
import id.ac.sekolah.manajemenkelas.report.dao.ReportDAO;
import id.ac.sekolah.manajemenkelas.report.service.ReportService;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

/**
 * Kontainer dependensi layanan untuk antarmuka desktop Swing.
 * Menghubungkan UI ke layer Service dan DAO PostgreSQL.
 */
public class ApplicationContainer {

    private final SessionContext sessionContext;
    private final AuthService authService;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AcademicService academicService;
    private final AttendanceService attendanceService;
    private final AssessmentService assessmentService;
    private final DashboardService dashboardService;
    private final ReportService reportService;

    public ApplicationContainer(
            AuthDAO authDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO,
            AttendanceDAO attendanceDAO,
            AssessmentDAO assessmentDAO,
            ReportDAO reportDAO) {
        this.sessionContext = SessionContext.getInstance();
        PasswordHasher passwordHasher = new PasswordHasher();
        this.authService = new AuthService(authDAO, passwordHasher);
        this.studentService = new StudentService(studentDAO);
        this.teacherService = new TeacherService(teacherDAO);
        this.academicService = new AcademicService(academicDAO, studentDAO, teacherDAO);
        this.attendanceService = new AttendanceService(attendanceDAO, academicDAO, studentDAO);
        this.assessmentService = new AssessmentService(assessmentDAO, academicDAO, studentDAO);
        this.dashboardService = new DashboardService(reportDAO);
        this.reportService = new ReportService(reportDAO, studentDAO);
    }

    /**
     * Membuat ApplicationContainer default yang terhubung ke PostgreSQL.
     */
    public static ApplicationContainer createDefault() {
        boolean terhubung = false;
        try {
            terhubung = DatabaseConfig.isConnectionAvailable();
        } catch (RuntimeException | LinkageError ex) {
            throw new IllegalStateException(
                    "Konfigurasi database PostgreSQL tidak tersedia: " + ex.getMessage(), ex);
        }

        if (!terhubung) {
            throw new IllegalStateException(
                    "Koneksi database PostgreSQL tidak tersedia. Pastikan server PostgreSQL aktif dan konfigurasi sudah benar.");
        }

        PasswordHasher pengacakSandi = new PasswordHasher();
        AuthDAO authDAO = new PostgresAuthDAO();
        StudentDAO studentDAO = new PostgresStudentDAO();
        TeacherDAO teacherDAO = new PostgresTeacherDAO();
        new AuthBootstrapSeeder(authDAO, pengacakSandi).seedDefaultUsers(teacherDAO);
        AcademicDAO academicDAO = new PostgresAcademicDAO();
        AttendanceDAO attendanceDAO = new PostgresAttendanceDAO();
        AssessmentDAO assessmentDAO = new PostgresAssessmentDAO();
        ReportDAO reportDAO = new PostgresReportDAO();

        return new ApplicationContainer(
                authDAO, studentDAO, teacherDAO, academicDAO,
                attendanceDAO, assessmentDAO, reportDAO);
    }

    public SessionContext getSessionContext() {
        return sessionContext;
    }

    public AuthService getAuthService() {
        return authService;
    }

    public StudentService getStudentService() {
        return studentService;
    }

    public TeacherService getTeacherService() {
        return teacherService;
    }

    public AcademicService getAcademicService() {
        return academicService;
    }

    public AttendanceService getAttendanceService() {
        return attendanceService;
    }

    public AssessmentService getAssessmentService() {
        return assessmentService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public ReportService getReportService() {
        return reportService;
    }
}

