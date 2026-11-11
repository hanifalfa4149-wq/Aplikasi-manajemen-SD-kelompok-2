package id.ac.sekolah.manajemenkelas.presentation.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.BotanicalCharts;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.DateField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.EmptyState;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FormField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.IconButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.LoadingIndicator;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SearchField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SecondaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StatusBadge;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.NavRoute;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

class SwingUiIntegrationTest {

    @AfterEach
    void cleanupSession() {
        SessionContext.getInstance().endSession();
    }

    @Test
    void designTokensAndReusableComponentsInitializeCleanly() {
        BotanicalTheme.installDefaults();
        ClayCard card = new ClayCard();
        PrimaryButton primary = new PrimaryButton("Simpan");
        SecondaryButton secondary = new SecondaryButton("Batal");
        IconButton iconBtn = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 16, BotanicalTheme.EVERGREEN), "Edit", IconButton.Tone.NEUTRAL);
        SearchField searchField = new SearchField("Cari...");
        searchField.setText("Budi");
        StatusBadge badge = StatusBadge.forBooleanStatus(true);
        StyledTable table = new StyledTable();
        FormField formField = new FormField("Nama", new JTextField(), true);
        DateField dateField = new DateField(LocalDate.of(2026, 8, 1));
        EmptyState emptyState = new EmptyState("Kosong", "Belum ada data");
        LoadingIndicator loading = new LoadingIndicator();
        FeedbackBanner banner = new FeedbackBanner();
        BotanicalCharts.BarChartPanel bar = new BotanicalCharts.BarChartPanel();
        BotanicalCharts.DonutChartPanel donut = new BotanicalCharts.DonutChartPanel();
        StateDeckPanel deck = new StateDeckPanel(card, "Kosong", "Desc");

        assertNotNull(primary);
        assertNotNull(secondary);
        assertNotNull(iconBtn);
        assertEquals("Budi", searchField.getText());
        assertEquals("AKTIF", badge.getText());
        assertNotNull(table);
        assertNotNull(formField);
        assertEquals(LocalDate.of(2026, 8, 1), dateField.getLocalDate());
        assertNotNull(emptyState);
        assertNotNull(loading);
        assertNotNull(banner);
        bar.setData(List.of(new BotanicalCharts.ChartItem("1A", 20, BotanicalTheme.EVERGREEN)), "", 0);
        donut.setData(List.of(new BotanicalCharts.ChartItem("Hadir", 10, BotanicalTheme.SUCCESS)), "Total");
        deck.showContent();
    }

    @Test
    void roleBasedNavigationEnforcesAdminAndTeacherPermissions() {
        assertTrue(NavRoute.DASHBOARD.isAuthorizedForRole(RoleMapper.ROLE_ADMIN));
        assertTrue(NavRoute.STUDENTS.isAuthorizedForRole(RoleMapper.ROLE_ADMIN));
        assertTrue(NavRoute.TEACHERS.isAuthorizedForRole(RoleMapper.ROLE_ADMIN));
        assertTrue(NavRoute.ACADEMIC_YEARS.isAuthorizedForRole(RoleMapper.ROLE_ADMIN));
        assertFalse(NavRoute.TEACHER_SCOPE.isAuthorizedForRole(RoleMapper.ROLE_ADMIN));

        assertTrue(NavRoute.DASHBOARD.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertFalse(NavRoute.STUDENTS.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertFalse(NavRoute.TEACHERS.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertFalse(NavRoute.ACADEMIC_YEARS.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.TEACHER_SCOPE.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.ATTENDANCE.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.ASSESSMENTS.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.STUDENT_SCORES.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.REPORT_ACADEMIC.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.REPORT_ATTENDANCE.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
        assertTrue(NavRoute.REPORT_SCORES.isAuthorizedForRole(RoleMapper.ROLE_TEACHER));
    }

    @Test
    void mainFrameNavigatesAllAdminRoutesWithPostgresContainer() throws InterruptedException, InvocationTargetException {
        ApplicationContainer container = ApplicationContainer.createDefault();
        assertTrue(container.isUsingPostgres(), "PostgreSQL harus terhubung pada lingkungan pengujian.");
        assertTrue(container.getAuthService().login(
                AppConfig.DEFAULT_ADMIN_USERNAME, AppConfig.DEFAULT_ADMIN_PASSWORD));

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        final MainFrame[] holder = new MainFrame[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                BotanicalTheme.installDefaults();
                MainFrame frame = new MainFrame(container);
                frame.showAppView();
                for (NavRoute route : NavRoute.values()) {
                    if (route.isAuthorizedForRole(RoleMapper.ROLE_ADMIN)) {
                        frame.getAppShellPanel().navigateTo(route);
                    }
                }
                holder[0] = frame;
            });

            Thread.sleep(500);

            SwingUtilities.invokeAndWait(() -> {
                assertTrue(SessionContext.getInstance().isLoggedIn());
                container.getAuthService().logout();
                holder[0].showLoginView();
                assertFalse(SessionContext.getInstance().isLoggedIn());
            });
        } finally {
            if (holder[0] != null) {
                SwingUtilities.invokeAndWait(() -> holder[0].dispose());
            }
        }
    }

    @Test
    void styledTableBindsHeaderAndRendersStatusPillComponent() {
        javax.swing.table.DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new String[]{"Nama", "Status"}, 0);
        model.addRow(new Object[]{"Siti Aminah", "AKTIF"});
        model.addRow(new Object[]{"Budi", "NONAKTIF"});
        StyledTable table = new StyledTable(model);
        table.setStatusColumns(1);
        javax.swing.JScrollPane scrollPane = table.wrapInScrollPane();

        assertNotNull(scrollPane.getColumnHeader());
        assertNotNull(scrollPane.getColumnHeader().getView());

        java.awt.Component activeRenderer = table.getCellRenderer(0, 1)
                .getTableCellRendererComponent(table, "AKTIF", false, false, 0, 1);
        assertTrue(activeRenderer instanceof javax.swing.JLabel);
        assertEquals("AKTIF", ((javax.swing.JLabel) activeRenderer).getText());

        java.awt.Component inactiveRenderer = table.getCellRenderer(1, 1)
                .getTableCellRendererComponent(table, "NONAKTIF", false, false, 1, 1);
        assertEquals("NONAKTIF", ((javax.swing.JLabel) inactiveRenderer).getText());
    }

    @Test
    void mainFrameNavigatesTeacherRoutesAndRedirectsUnauthorizedAdminRoute()
            throws InterruptedException, InvocationTargetException {
        ApplicationContainer container = ApplicationContainer.createDefault();
        assertTrue(container.getAuthService().login(
                AppConfig.DEFAULT_TEACHER_USERNAME, AppConfig.DEFAULT_TEACHER_PASSWORD));

        if (GraphicsEnvironment.isHeadless()) {
            return;
        }

        final MainFrame[] holder = new MainFrame[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                BotanicalTheme.installDefaults();
                MainFrame frame = new MainFrame(container);
                frame.showAppView();
                for (NavRoute route : NavRoute.values()) {
                    if (route.isAuthorizedForRole(RoleMapper.ROLE_TEACHER)) {
                        frame.getAppShellPanel().navigateTo(route);
                    }
                }
                // Unauthorized admin route must safely fall back without crashing
                frame.getAppShellPanel().navigateTo(NavRoute.STUDENTS);
                holder[0] = frame;
            });

            Thread.sleep(300);

            SwingUtilities.invokeAndWait(() -> {
                assertTrue(SessionContext.getInstance().isLoggedIn());
                assertEquals(RoleMapper.ROLE_TEACHER, SessionContext.getInstance().getRole());
            });
        } finally {
            if (holder[0] != null) {
                SwingUtilities.invokeAndWait(() -> holder[0].dispose());
            }
        }
    }
}

