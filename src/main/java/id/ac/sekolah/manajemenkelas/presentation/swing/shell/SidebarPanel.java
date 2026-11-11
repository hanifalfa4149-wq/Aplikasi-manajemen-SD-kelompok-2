package id.ac.sekolah.manajemenkelas.presentation.swing.shell;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Sidebar navigasi Evergreen dengan pengelompokan kategori, sorotan item aktif Mint,
 * status hover, filter berbasis peran, serta kartu profil pengguna + Logout.
 */
public class SidebarPanel extends JPanel {

    private final JPanel navContainer = new JPanel();
    private final Map<NavRoute, SidebarNavItem> navItems = new EnumMap<>(NavRoute.class);
    private final JLabel userNameLabel = new JLabel("-");
    private final JLabel userRoleLabel = new JLabel("-");
    private final JButton logoutButton;
    private NavRoute activeRoute = NavRoute.DASHBOARD;
    private final Consumer<NavRoute> onNavigate;

    public SidebarPanel(Consumer<NavRoute> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        setBackground(BotanicalTheme.EVERGREEN);
        setPreferredSize(new Dimension(BotanicalTheme.SIDEBAR_WIDTH, 760));
        setLayout(new BorderLayout(0, 0));

        add(buildBrandHeader(), BorderLayout.NORTH);

        navContainer.setBackground(BotanicalTheme.EVERGREEN);
        navContainer.setLayout(new BoxLayout(navContainer, BoxLayout.Y_AXIS));
        navContainer.setBorder(BorderFactory.createEmptyBorder(8, 12, 16, 12));

        JScrollPane scroll = new JScrollPane(navContainer);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BotanicalTheme.EVERGREEN);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(5, 0));
        add(scroll, BorderLayout.CENTER);

        logoutButton = createLogoutButton(onLogout);
        add(buildProfileFooter(), BorderLayout.SOUTH);
    }

    private JPanel buildBrandHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(BotanicalTheme.EVERGREEN);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BotanicalTheme.SIDEBAR_DIVIDER),
                BorderFactory.createEmptyBorder(18, 18, 16, 18)));

        JPanel badge = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BotanicalTheme.MINT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(38, 38));
        badge.add(new JLabel(BotanicalIcons.get(BotanicalIcons.Type.BRAND_LEAF, 20, BotanicalTheme.EVERGREEN), SwingConstants.CENTER));
        header.add(badge, BorderLayout.WEST);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel appTitle = new JLabel("Manajemen Kelas SD");
        appTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        appTitle.setForeground(BotanicalTheme.WHITE);
        JLabel appSub = new JLabel("Sistem Administrasi Sekolah");
        appSub.setFont(BotanicalTheme.FONT_SMALL);
        appSub.setForeground(BotanicalTheme.MINT);
        titleBox.add(appTitle);
        titleBox.add(appSub);
        header.add(titleBox, BorderLayout.CENTER);

        return header;
    }

    private JPanel buildProfileFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setBackground(BotanicalTheme.SIDEBAR_PROFILE_BG);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BotanicalTheme.SIDEBAR_DIVIDER),
                BorderFactory.createEmptyBorder(14, 16, 16, 16)));

        JPanel userInfo = new JPanel(new java.awt.GridLayout(2, 1, 0, 2));
        userInfo.setOpaque(false);
        userNameLabel.setFont(BotanicalTheme.FONT_BODY_BOLD);
        userNameLabel.setForeground(BotanicalTheme.WHITE);
        userRoleLabel.setFont(BotanicalTheme.FONT_SMALL);
        userRoleLabel.setForeground(BotanicalTheme.MINT);
        userInfo.add(userNameLabel);
        userInfo.add(userRoleLabel);

        footer.add(userInfo, BorderLayout.CENTER);
        footer.add(logoutButton, BorderLayout.SOUTH);
        return footer;
    }

    private JButton createLogoutButton(Runnable onLogout) {
        JButton btn = new JButton("Keluar Sesi", BotanicalIcons.get(BotanicalIcons.Type.LOGOUT, 16, BotanicalTheme.WHITE)) {
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(hovered ? BotanicalTheme.DANGER : new Color(255, 255, 255, 24));
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        btn.setFont(BotanicalTheme.FONT_BODY_BOLD);
        btn.setForeground(BotanicalTheme.WHITE);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(224, 36));
        btn.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        return btn;
    }

    /**
     * Membangun ulang item navigasi sesuai hak akses sesi saat ini.
     */
    public void refreshForSession(SessionContext session) {
        navContainer.removeAll();
        navItems.clear();

        String role = session.getRole();
        userNameLabel.setText(session.getFullName() != null ? session.getFullName() : "Pengguna");
        String roleText = RoleMapper.ROLE_ADMIN.equalsIgnoreCase(role)
                ? "Administrator Sekolah"
                : (session.getTeacherId() != null ? "Guru (Profil #" + session.getTeacherId() + ")" : "Guru (Belum tertaut)");
        userRoleLabel.setText(roleText + "  •  @" + (session.getUsername() == null ? "-" : session.getUsername()));

        Map<String, List<NavRoute>> grouped = new LinkedHashMap<>();
        for (NavRoute route : NavRoute.values()) {
            if (route.isAuthorizedForRole(role)) {
                grouped.computeIfAbsent(route.getCategory(), k -> new java.util.ArrayList<>()).add(route);
            }
        }

        boolean firstCategory = true;
        for (Map.Entry<String, List<NavRoute>> entry : grouped.entrySet()) {
            if (!firstCategory) {
                navContainer.add(Box.createVerticalStrut(10));
            }
            firstCategory = false;

            JLabel catLabel = new JLabel(entry.getKey().toUpperCase());
            catLabel.setFont(BotanicalTheme.FONT_CAPTION);
            catLabel.setForeground(BotanicalTheme.SAGE);
            catLabel.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 8));
            catLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            navContainer.add(catLabel);

            for (NavRoute route : entry.getValue()) {
                SidebarNavItem item = new SidebarNavItem(route);
                item.setAlignmentX(Component.LEFT_ALIGNMENT);
                item.addActionListener(e -> {
                    setActiveRoute(route);
                    if (onNavigate != null) {
                        onNavigate.accept(route);
                    }
                });
                navItems.put(route, item);
                navContainer.add(item);
                navContainer.add(Box.createVerticalStrut(3));
            }
        }

        navContainer.add(Box.createVerticalGlue());
        setActiveRoute(activeRoute);
        navContainer.revalidate();
        navContainer.repaint();
        revalidate();
        repaint();
    }

    public void setActiveRoute(NavRoute route) {
        this.activeRoute = route;
        for (Map.Entry<NavRoute, SidebarNavItem> entry : navItems.entrySet()) {
            entry.getValue().setActive(entry.getKey() == route);
        }
    }

    private static final class SidebarNavItem extends JButton {

        private final NavRoute route;
        private boolean active = false;
        private boolean hovered = false;

        private SidebarNavItem(NavRoute route) {
            super(route.getTitle());
            this.route = route;
            setFont(BotanicalTheme.FONT_BODY);
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setIconTextGap(10);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            setPreferredSize(new Dimension(224, 38));
            updateVisuals();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        private void setActive(boolean active) {
            this.active = active;
            updateVisuals();
            repaint();
        }

        private void updateVisuals() {
            setFont(active ? BotanicalTheme.FONT_BODY_BOLD : BotanicalTheme.FONT_BODY);
            Color fg = active ? BotanicalTheme.EVERGREEN : BotanicalTheme.PALE_MINT;
            setForeground(fg);
            setIcon(BotanicalIcons.get(route.getIconType(), 17, active ? BotanicalTheme.EVERGREEN : BotanicalTheme.MINT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(BotanicalTheme.MINT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 11, 11));
                } else if (hovered) {
                    g2.setColor(BotanicalTheme.DEEP_FOREST);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 11, 11));
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}

