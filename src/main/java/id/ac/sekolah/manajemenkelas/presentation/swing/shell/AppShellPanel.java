package id.ac.sekolah.manajemenkelas.presentation.swing.shell;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ConfirmationDialog;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.IconButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StatusBadge;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Shell aplikasi utama: Sidebar Evergreen di kiri + TopHeader & CardLayout konten Soft Ivory di kanan.
 */
public class AppShellPanel extends JPanel {

    private final ApplicationContainer container;
    private final SidebarPanel sidebarPanel;
    private final Runnable onLogoutCompleted;

    private final CardLayout contentLayout = new CardLayout();
    private final JPanel contentDeck = new JPanel(contentLayout);
    private final Map<NavRoute, ScreenPage> pages = new EnumMap<>(NavRoute.class);

    private final JLabel breadcrumbLabel = new JLabel("Overview / Dashboard");
    private final JLabel pageTitleLabel = new JLabel("Dashboard");
    private final JLabel pageSubtitleLabel = new JLabel("");
    private final StatusBadge persistenceBadge;
    private JPanel topHeaderPanel;
    private NavRoute currentRoute = NavRoute.DASHBOARD;

    public AppShellPanel(ApplicationContainer container, Runnable onLogoutCompleted) {
        this.container = container;
        this.onLogoutCompleted = onLogoutCompleted;
        this.persistenceBadge = container.isUsingPostgres()
                ? new StatusBadge("POSTGRESQL", StatusBadge.Style.SUCCESS)
                : new StatusBadge("INMEMORY", StatusBadge.Style.WARNING);

        setLayout(new BorderLayout());
        setBackground(BotanicalTheme.SOFT_IVORY);

        sidebarPanel = new SidebarPanel(this::navigateTo, this::handleLogout);
        add(sidebarPanel, BorderLayout.WEST);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(BotanicalTheme.SOFT_IVORY);
        this.topHeaderPanel = buildTopHeader();
        mainArea.add(this.topHeaderPanel, BorderLayout.NORTH);

        contentDeck.setOpaque(false);
        contentDeck.setBorder(BorderFactory.createEmptyBorder(
                BotanicalTheme.PAGE_PADDING,
                BotanicalTheme.PAGE_PADDING,
                BotanicalTheme.PAGE_PADDING,
                BotanicalTheme.PAGE_PADDING));
        mainArea.add(contentDeck, BorderLayout.CENTER);
        add(mainArea, BorderLayout.CENTER);
    }

    private JPanel buildTopHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(BotanicalTheme.WHITE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BotanicalTheme.BORDER),
                BorderFactory.createEmptyBorder(14, BotanicalTheme.PAGE_PADDING, 14, BotanicalTheme.PAGE_PADDING)));

        JPanel titleBox = new JPanel(new java.awt.GridLayout(0, 1, 0, 2));
        titleBox.setOpaque(false);

        breadcrumbLabel.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        breadcrumbLabel.setForeground(BotanicalTheme.SUCCESS);
        pageTitleLabel.setFont(BotanicalTheme.FONT_TITLE);
        pageTitleLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);
        pageSubtitleLabel.setFont(BotanicalTheme.FONT_SMALL);
        pageSubtitleLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);

        titleBox.add(breadcrumbLabel);
        titleBox.add(pageTitleLabel);
        titleBox.add(pageSubtitleLabel);
        header.add(titleBox, BorderLayout.CENTER);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        rightActions.setOpaque(false);
        rightActions.add(persistenceBadge);

        IconButton refreshButton = new IconButton(
                "Muat Ulang",
                BotanicalIcons.get(BotanicalIcons.Type.REFRESH, 15, BotanicalTheme.EVERGREEN),
                "Muat ulang data pada halaman ini",
                IconButton.Tone.NEUTRAL);
        refreshButton.addActionListener(e -> {
            ScreenPage activePage = pages.get(currentRoute);
            if (activePage != null) {
                activePage.refreshData();
            }
        });
        rightActions.add(refreshButton);
        header.add(rightActions, BorderLayout.EAST);

        return header;
    }

    public <T extends Component & ScreenPage> void registerPage(NavRoute route, T pageComponent) {
        pages.put(route, pageComponent);
        contentDeck.add(pageComponent, route.name());
    }

    public void onSessionStarted() {
        sidebarPanel.refreshForSession(container.getSessionContext());
        navigateTo(NavRoute.DASHBOARD);
    }

    public void navigateTo(NavRoute route) {
        if (route == null || !route.isAuthorizedForRole(container.getSessionContext().getRole())) {
            route = NavRoute.DASHBOARD;
        }
        this.currentRoute = route;
        sidebarPanel.setActiveRoute(route);
        breadcrumbLabel.setText(route.getCategory() + "  /  " + route.getTitle());
        pageTitleLabel.setText(route.getTitle());
        pageSubtitleLabel.setText(route.getSubtitle());
        breadcrumbLabel.revalidate();
        pageTitleLabel.revalidate();
        pageSubtitleLabel.revalidate();
        if (topHeaderPanel != null) {
            topHeaderPanel.revalidate();
            topHeaderPanel.repaint();
        }

        contentLayout.show(contentDeck, route.name());
        ScreenPage page = pages.get(route);
        if (page != null) {
            page.refreshData();
        }
    }

    public ScreenPage getPage(NavRoute route) {
        return pages.get(route);
    }

    private void handleLogout() {
        boolean confirm = ConfirmationDialog.showConfirm(
                this,
                "Konfirmasi Logout",
                "Apakah Anda yakin ingin mengakhiri sesi dan kembali ke layar login?",
                "Ya, Logout",
                false);
        if (confirm) {
            container.getAuthService().logout();
            if (onLogoutCompleted != null) {
                onLogoutCompleted.run();
            }
        }
    }
}

