package id.ac.sekolah.manajemenkelas.presentation.swing.auth;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FormField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StatusBadge;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Layar login khusus bergaya Evergreen + Soft Ivory dengan toggle visibilitas password,
 * validasi input, dan autentikasi melalui AuthService via SwingWorker.
 */
public class LoginPanel extends JPanel {

    private final ApplicationContainer container;
    private final Runnable onLoginSuccess;

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final FormField usernameFormField;
    private final FormField passwordFormField;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final PrimaryButton loginButton = new PrimaryButton("Masuk ke Aplikasi");
    private final JButton togglePasswordBtn = new JButton();
    private char defaultEchoChar;
    private boolean passwordVisible = false;

    public LoginPanel(ApplicationContainer container, Runnable onLoginSuccess) {
        this.container = container;
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridLayout(1, 2));
        setBackground(BotanicalTheme.SOFT_IVORY);

        add(buildBrandShowcase());

        usernameFormField = new FormField("Username", usernameField, true, "Masukkan username akun Anda");
        passwordFormField = buildPasswordFormField();
        add(buildFormArea());
    }

    private FormField buildPasswordFormField() {
        defaultEchoChar = passwordField.getEchoChar();

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(BotanicalTheme.WHITE);
        wrapper.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER, 1));
        wrapper.setPreferredSize(new Dimension(280, BotanicalTheme.INPUT_HEIGHT));

        passwordField.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 4));
        passwordField.setFont(BotanicalTheme.FONT_BODY);
        passwordField.setForeground(BotanicalTheme.TEXT_PRIMARY);
        passwordField.setBackground(BotanicalTheme.WHITE);
        wrapper.add(passwordField, BorderLayout.CENTER);

        togglePasswordBtn.setIcon(BotanicalIcons.get(BotanicalIcons.Type.EYE, 18, BotanicalTheme.TEXT_SECONDARY));
        togglePasswordBtn.setToolTipText("Tampilkan/Sembunyikan password");
        togglePasswordBtn.setOpaque(false);
        togglePasswordBtn.setContentAreaFilled(false);
        togglePasswordBtn.setBorderPainted(false);
        togglePasswordBtn.setFocusPainted(false);
        togglePasswordBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        togglePasswordBtn.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 10));
        togglePasswordBtn.addActionListener(e -> togglePasswordVisibility());
        wrapper.add(togglePasswordBtn, BorderLayout.EAST);

        FormField ff = new FormField("Password", wrapper, true, null);
        return ff;
    }

    private JPanel buildBrandShowcase() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BotanicalTheme.EVERGREEN);
        panel.setBorder(BorderFactory.createEmptyBorder(48, 48, 48, 48));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JPanel emblem = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                try {
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BotanicalTheme.MINT);
                    g2.fill(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                } finally {
                    g2.dispose();
                }
                super.paintComponent(g);
            }
        };
        emblem.setOpaque(false);
        emblem.setMaximumSize(new Dimension(56, 56));
        emblem.setPreferredSize(new Dimension(56, 56));
        emblem.setAlignmentX(LEFT_ALIGNMENT);
        emblem.add(new JLabel(BotanicalIcons.get(BotanicalIcons.Type.BRAND_LEAF, 28, BotanicalTheme.EVERGREEN), SwingConstants.CENTER));

        content.add(emblem);
        content.add(Box.createVerticalStrut(22));

        JLabel title = new JLabel("<html>Sistem Manajemen<br>Kelas Sekolah Dasar</html>");
        title.setFont(BotanicalTheme.FONT_DISPLAY);
        title.setForeground(BotanicalTheme.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createVerticalStrut(12));

        JLabel desc = new JLabel(
                "<html><div style='width:340px'>Kelola data siswa, guru, rombongan belajar, absensi harian, penilaian, dan laporan akademik dalam satu aplikasi desktop terpadu.</div></html>");
        desc.setFont(BotanicalTheme.FONT_BODY);
        desc.setForeground(BotanicalTheme.MINT);
        desc.setAlignmentX(LEFT_ALIGNMENT);
        content.add(desc);
        content.add(Box.createVerticalStrut(28));

        StatusBadge dbBadge = container.isUsingPostgres()
                ? new StatusBadge("TERHUBUNG KE POSTGRESQL", StatusBadge.Style.MINT)
                : new StatusBadge("MODE FALLBACK IN-MEMORY", StatusBadge.Style.WARNING);
        dbBadge.setAlignmentX(LEFT_ALIGNMENT);
        content.add(dbBadge);

        if (container.getPersistenceWarning() != null) {
            content.add(Box.createVerticalStrut(10));
            JLabel warn = new JLabel("<html><div style='width:340px'>" + container.getPersistenceWarning() + "</div></html>");
            warn.setFont(BotanicalTheme.FONT_SMALL);
            warn.setForeground(BotanicalTheme.WARNING);
            warn.setAlignmentX(LEFT_ALIGNMENT);
            content.add(warn);
        }

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(content, gbc);
        return panel;
    }

    private JPanel buildFormArea() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BotanicalTheme.SOFT_IVORY);
        outer.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        ClayCard card = new ClayCard(new BorderLayout(0, 18), new InsetsToMargin().toInsets());
        card.setPreferredSize(new Dimension(420, 460));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Masuk ke Sistem");
        title.setFont(BotanicalTheme.FONT_DISPLAY);
        title.setForeground(BotanicalTheme.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Gunakan kredensial akun sekolah untuk melanjutkan.");
        subtitle.setFont(BotanicalTheme.FONT_BODY);
        subtitle.setForeground(BotanicalTheme.TEXT_SECONDARY);
        heading.add(title);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitle);
        heading.add(Box.createVerticalStrut(12));
        heading.add(feedbackBanner);
        card.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(usernameFormField);
        fields.add(Box.createVerticalStrut(10));
        fields.add(passwordFormField);
        fields.add(Box.createVerticalStrut(16));

        loginButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        loginButton.setPreferredSize(new Dimension(340, 42));
        loginButton.addActionListener(e -> submitLogin());

        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(loginButton, BorderLayout.CENTER);
        fields.add(btnWrap);

        card.add(fields, BorderLayout.CENTER);

        JLabel demoHint = new JLabel(
                "<html><div style='width:316px; padding-top:10px; color:#718477'>Akun demo bawaan: <b>admin</b> / <b>admin123</b> (Admin Sekolah) "
                        + "atau akun Guru yang terhubung ke tabel <i>guru</i>.</div></html>");
        demoHint.setFont(BotanicalTheme.FONT_SMALL);
        demoHint.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BotanicalTheme.BORDER));
        card.add(demoHint, BorderLayout.SOUTH);

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> submitLogin());

        outer.add(card);
        return outer;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : defaultEchoChar);
        togglePasswordBtn.setIcon(BotanicalIcons.get(
                passwordVisible ? BotanicalIcons.Type.EYE_OFF : BotanicalIcons.Type.EYE,
                18,
                passwordVisible ? BotanicalTheme.EVERGREEN : BotanicalTheme.TEXT_SECONDARY));
    }

    private void submitLogin() {
        feedbackBanner.clear();
        usernameFormField.clearError();
        passwordFormField.clearError();

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        boolean valid = true;
        if (username.isEmpty()) {
            usernameFormField.setError("Username wajib diisi.");
            valid = false;
        }
        if (password.isEmpty()) {
            passwordFormField.setError("Password wajib diisi.");
            valid = false;
        }
        if (!valid) {
            return;
        }

        AuthService authService = container.getAuthService();
        AsyncTaskRunner.run(
                () -> authService.login(username, password),
                () -> {
                    loginButton.setEnabled(false);
                    loginButton.setText("Memverifikasi...");
                },
                authenticated -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Masuk ke Aplikasi");
                    if (Boolean.TRUE.equals(authenticated)) {
                        passwordField.setText("");
                        if (onLoginSuccess != null) {
                            onLoginSuccess.run();
                        }
                    } else {
                        feedbackBanner.showError("Login gagal. Periksa kembali username, password, atau status aktif akun.");
                        passwordField.selectAll();
                        passwordField.requestFocusInWindow();
                    }
                },
                error -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Masuk ke Aplikasi");
                    feedbackBanner.showError(error);
                });
    }

    public void resetForm() {
        usernameField.setText("");
        passwordField.setText("");
        usernameFormField.clearError();
        passwordFormField.clearError();
        feedbackBanner.clear();
        if (passwordVisible) {
            togglePasswordVisibility();
        }
        usernameField.requestFocusInWindow();
    }

    public JButton getDefaultButton() {
        return loginButton;
    }

    private static final class InsetsToMargin {
        private java.awt.Insets toInsets() {
            return new java.awt.Insets(28, 30, 24, 30);
        }
    }
}

