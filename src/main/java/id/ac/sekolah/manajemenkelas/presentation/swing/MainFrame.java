package id.ac.sekolah.manajemenkelas.presentation.swing;

import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.auth.LoginPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.AcademicYearPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.HomeroomPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.SchoolClassPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.SemesterPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.StudentPlacementPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.SubjectPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.TeacherScopePanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.academic.TeachingAssignmentPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.dashboard.DashboardPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.master.StudentPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.master.TeacherPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.operational.AssessmentPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.operational.AttendancePanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.operational.StudentScoresPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.report.AcademicReportPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.report.AttendanceReportPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.module.report.ScoreReportPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.AppShellPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.NavRoute;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Jendela utama aplikasi desktop Java Swing Sistem Manajemen Kelas SD.
 * Mengatur transisi Login -> AppShell dan seluruh modul layar menggunakan CardLayout.
 */
public class MainFrame extends JFrame {

    private static final String CARD_LOGIN = "LOGIN";
    private static final String CARD_APP = "APP";

    private final ApplicationContainer container;
    private final CardLayout rootLayout = new CardLayout();
    private final JPanel rootDeck = new JPanel(rootLayout);
    private final LoginPanel loginPanel;
    private final AppShellPanel appShellPanel;

    public MainFrame() {
        this(ApplicationContainer.createDefault());
    }

    public MainFrame(ApplicationContainer container) {
        this.container = container;
        setTitle(AppConfig.APPLICATION_NAME + " v" + AppConfig.APPLICATION_VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 700));
        setSize(1340, 820);
        setLocationRelativeTo(null);

        this.appShellPanel = new AppShellPanel(container, this::showLoginView);
        registerAllModules();

        this.loginPanel = new LoginPanel(container, this::showAppView);

        rootDeck.add(loginPanel, CARD_LOGIN);
        rootDeck.add(appShellPanel, CARD_APP);
        setContentPane(rootDeck);

        showLoginView();
    }

    private void registerAllModules() {
        StudentScoresPanel studentScoresPanel = new StudentScoresPanel(container);

        appShellPanel.registerPage(NavRoute.DASHBOARD, new DashboardPanel(container, appShellPanel::navigateTo));
        appShellPanel.registerPage(NavRoute.STUDENTS, new StudentPanel(container));
        appShellPanel.registerPage(NavRoute.TEACHERS, new TeacherPanel(container));

        appShellPanel.registerPage(NavRoute.ACADEMIC_YEARS, new AcademicYearPanel(container));
        appShellPanel.registerPage(NavRoute.SEMESTERS, new SemesterPanel(container));
        appShellPanel.registerPage(NavRoute.CLASSES, new SchoolClassPanel(container));
        appShellPanel.registerPage(NavRoute.SUBJECTS, new SubjectPanel(container));
        appShellPanel.registerPage(NavRoute.HOMEROOM, new HomeroomPanel(container));
        appShellPanel.registerPage(NavRoute.TEACHING_ASSIGNMENTS, new TeachingAssignmentPanel(container));
        appShellPanel.registerPage(NavRoute.STUDENT_PLACEMENTS, new StudentPlacementPanel(container));
        appShellPanel.registerPage(NavRoute.TEACHER_SCOPE, new TeacherScopePanel(container));

        appShellPanel.registerPage(NavRoute.ATTENDANCE, new AttendancePanel(container));
        appShellPanel.registerPage(NavRoute.ASSESSMENTS, new AssessmentPanel(container, assessmentId -> {
            studentScoresPanel.focusAssessment(assessmentId);
            appShellPanel.navigateTo(NavRoute.STUDENT_SCORES);
        }));
        appShellPanel.registerPage(NavRoute.STUDENT_SCORES, studentScoresPanel);

        appShellPanel.registerPage(NavRoute.REPORT_ACADEMIC, new AcademicReportPanel(container));
        appShellPanel.registerPage(NavRoute.REPORT_ATTENDANCE, new AttendanceReportPanel(container));
        appShellPanel.registerPage(NavRoute.REPORT_SCORES, new ScoreReportPanel(container));
    }

    public void showLoginView() {
        rootLayout.show(rootDeck, CARD_LOGIN);
        getRootPane().setDefaultButton(loginPanel.getDefaultButton());
        loginPanel.resetForm();
    }

    public void showAppView() {
        getRootPane().setDefaultButton(null);
        rootLayout.show(rootDeck, CARD_APP);
        appShellPanel.onSessionStarted();
    }

    public ApplicationContainer getContainer() {
        return container;
    }

    public AppShellPanel getAppShellPanel() {
        return appShellPanel;
    }

    public static void main(String[] args) {
        BotanicalTheme.installDefaults();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
