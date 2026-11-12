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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FormField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SelectItem;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StatusBadge;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Layar login khusus bergaya Evergreen + Soft Ivory dengan toggle visibilitas password,
 * validasi input, dan autentikasi melalui AuthService via SwingWorker.
 */
public class LoginPanel extends JPanel {

    private enum AuthMode {
        LOGIN,
        SIGNUP
    }

    private final ApplicationContainer container;
    private final Runnable onLoginSuccess;

    private final JButton loginTabBtn = new JButton("Masuk");
    private final JButton signupTabBtn = new JButton("Daftar");
    private final JLabel titleLabel = new JLabel("Masuk ke Sistem");
    private final JLabel subtitleLabel = new JLabel("Pilih peran dan gunakan kredensial akun Anda.");

    private final JComboBox<SelectItem<String>> roleCombo = new JComboBox<>();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final FormField roleFormField;
    private final FormField usernameFormField;
    private final FormField passwordFormField;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final PrimaryButton submitButton = new PrimaryButton("Masuk ke Aplikasi");
    private final JButton togglePasswordBtn = new JButton();
    private char defaultEchoChar;
    private boolean passwordVisible = false;
    private AuthMode activeMode = AuthMode.LOGIN;

    public LoginPanel(ApplicationContainer container, Runnable onLoginSuccess) {
        this.container = container;
        this.onLoginSuccess = onLoginSuccess;

        setLayout(new GridLayout(1, 2));
        setBackground(BotanicalTheme.SOFT_IVORY);

        add(buildBrandShowcase());

        roleCombo.addItem(new SelectItem<>(RoleMapper.ROLE_TEACHER, "Guru"));
        roleCombo.addItem(new SelectItem<>(RoleMapper.ROLE_ADMIN, "Admin"));
        roleFormField = new FormField("Peran Akun", roleCombo, true, "Pilih peran untuk masuk atau mendaftar");
        usernameFormField = new FormField("Username", usernameField, true, "Masukkan username akun Anda");
        passwordFormField = buildPasswordFormField();
        add(buildFormArea());
        applyModeUi();
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

        StatusBadge dbBadge = new StatusBadge("TERHUBUNG KE POSTGRESQL", StatusBadge.Style.MINT);
        dbBadge.setAlignmentX(LEFT_ALIGNMENT);
        content.add(dbBadge);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(content, gbc);
        return panel;
    }

    private JPanel buildFormArea() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BotanicalTheme.SOFT_IVORY);
        outer.setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        ClayCard card = new ClayCard(new BorderLayout(0, 16), new InsetsToMargin().toInsets());
        card.setPreferredSize(new Dimension(430, 540));

        JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JPanel modeSwitch = buildModeSwitch();
        modeSwitch.setAlignmentX(LEFT_ALIGNMENT);
        heading.add(modeSwitch);
        heading.add(Box.createVerticalStrut(14));

        titleLabel.setFont(BotanicalTheme.FONT_DISPLAY);
        titleLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        subtitleLabel.setFont(BotanicalTheme.FONT_BODY);
        subtitleLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
        subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
        heading.add(titleLabel);
        heading.add(Box.createVerticalStrut(4));
        heading.add(subtitleLabel);
        heading.add(Box.createVerticalStrut(10));
        feedbackBanner.setAlignmentX(LEFT_ALIGNMENT);
        heading.add(feedbackBanner);
        card.add(heading, BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setOpaque(false);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(roleFormField);
        fields.add(Box.createVerticalStrut(10));
        fields.add(usernameFormField);
        fields.add(Box.createVerticalStrut(10));
        fields.add(passwordFormField);
        fields.add(Box.createVerticalStrut(16));

        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        submitButton.setPreferredSize(new Dimension(340, 42));
        submitButton.addActionListener(e -> onSubmitAuth());

        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        btnWrap.add(submitButton, BorderLayout.CENTER);
        fields.add(btnWrap);

        card.add(fields, BorderLayout.CENTER);

        JLabel demoHint = new JLabel(
                "<html><div style='width:326px; padding-top:10px; color:#718477'>Demo bawaan: <b>admin</b> / <b>admin123</b> (Admin) "
                        + "atau <b>guru</b> / <b>guru123</b> (Guru). Anda juga dapat mendaftar akun baru di tab <b>Daftar</b>.</div></html>");
        demoHint.setFont(BotanicalTheme.FONT_SMALL);
        demoHint.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BotanicalTheme.BORDER));
        card.add(demoHint, BorderLayout.SOUTH);

        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
        passwordField.addActionListener(e -> onSubmitAuth());

        outer.add(card);
        return outer;
    }

    private JPanel buildModeSwitch() {
        JPanel switcher = new JPanel(new GridLayout(1, 2, 8, 0));
        switcher.setOpaque(false);
        switcher.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        switcher.setPreferredSize(new Dimension(360, 38));

        configureTabButton(loginTabBtn, AuthMode.LOGIN);
        configureTabButton(signupTabBtn, AuthMode.SIGNUP);

        switcher.add(loginTabBtn);
        switcher.add(signupTabBtn);
        return switcher;
    }

    private void configureTabButton(JButton btn, AuthMode targetMode) {
        btn.setFont(BotanicalTheme.FONT_BODY_BOLD);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> switchMode(targetMode));
    }

    private void switchMode(AuthMode targetMode) {
        if (this.activeMode == targetMode) {
            return;
        }
        this.activeMode = targetMode;
        feedbackBanner.clear();
        usernameFormField.clearError();
        passwordFormField.clearError();
        applyModeUi();
        usernameField.requestFocusInWindow();
    }

    private void applyModeUi() {
        boolean isLogin = (activeMode == AuthMode.LOGIN);

        styleTabButton(loginTabBtn, isLogin);
        styleTabButton(signupTabBtn, !isLogin);

        if (isLogin) {
            titleLabel.setText("Masuk ke Sistem");
            subtitleLabel.setText("Pilih peran dan gunakan kredensial akun Anda.");
            submitButton.setText("Masuk ke Aplikasi");
        } else {
            titleLabel.setText("Daftar Akun Baru");
            subtitleLabel.setText("Buat akun cukup dengan username, password, dan peran.");
            submitButton.setText("Daftar Akun");
        }
    }

    private static void styleTabButton(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(BotanicalTheme.EVERGREEN);
            btn.setForeground(BotanicalTheme.WHITE);
            btn.setBorder(BorderFactory.createLineBorder(BotanicalTheme.EVERGREEN, 1));
        } else {
            btn.setBackground(BotanicalTheme.PALE_MINT);
            btn.setForeground(BotanicalTheme.TEXT_PRIMARY);
            btn.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER, 1));
        }
        btn.setOpaque(true);
    }

    private String getSelectedRole() {
        @SuppressWarnings("unchecked")
        SelectItem<String> item = (SelectItem<String>) roleCombo.getSelectedItem();
        return item != null ? item.getValue() : RoleMapper.ROLE_TEACHER;
    }

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        passwordField.setEchoChar(passwordVisible ? (char) 0 : defaultEchoChar);
        togglePasswordBtn.setIcon(BotanicalIcons.get(
                passwordVisible ? BotanicalIcons.Type.EYE_OFF : BotanicalIcons.Type.EYE,
                18,
                passwordVisible ? BotanicalTheme.EVERGREEN : BotanicalTheme.TEXT_SECONDARY));
    }

    private void onSubmitAuth() {
        if (activeMode == AuthMode.LOGIN) {
            submitLogin();
        } else {
            submitSignup();
        }
    }

    private void submitLogin() {
        feedbackBanner.clear();
        usernameFormField.clearError();
        passwordFormField.clearError();

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String selectedRole = getSelectedRole();

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
                () -> authService.login(username, password, selectedRole),
                () -> {
                    setControlsEnabled(false);
                    submitButton.setText("Memverifikasi...");
                },
                authenticated -> {
                    setControlsEnabled(true);
                    applyModeUi();
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
                    setControlsEnabled(true);
                    applyModeUi();
                    feedbackBanner.showError(error);
                });
    }

    private void submitSignup() {
        feedbackBanner.clear();
        usernameFormField.clearError();
        passwordFormField.clearError();

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String selectedRole = getSelectedRole();

        boolean valid = true;
        if (username.isEmpty()) {
            usernameFormField.setError("Username wajib diisi.");
            valid = false;
        } else if (username.contains(" ")) {
            usernameFormField.setError("Username tidak boleh mengandung spasi.");
            valid = false;
        } else if (username.length() < 3) {
            usernameFormField.setError("Username minimal 3 karakter.");
            valid = false;
        }

        if (password.isEmpty()) {
            passwordFormField.setError("Password wajib diisi.");
            valid = false;
        } else if (password.length() < 4) {
            passwordFormField.setError("Password minimal 4 karakter.");
            valid = false;
        }

        if (!valid) {
            return;
        }

        AuthService authService = container.getAuthService();
        AsyncTaskRunner.run(
                () -> authService.register(username, password, selectedRole),
                () -> {
                    setControlsEnabled(false);
                    submitButton.setText("Mendaftarkan...");
                },
                akunBaru -> {
                    setControlsEnabled(true);
                    this.activeMode = AuthMode.LOGIN;
                    applyModeUi();
                    passwordField.setText("");
                    feedbackBanner.showSuccess(
                            "Pendaftaran berhasil! Silakan masuk dengan akun \"" + akunBaru.getUsername() + "\".");
                    passwordField.requestFocusInWindow();
                },
                error -> {
                    setControlsEnabled(true);
                    applyModeUi();
                    feedbackBanner.showError(error);
                });
    }

    private void setControlsEnabled(boolean enabled) {
        submitButton.setEnabled(enabled);
        loginTabBtn.setEnabled(enabled);
        signupTabBtn.setEnabled(enabled);
        roleCombo.setEnabled(enabled);
    }

    public void resetForm() {
        this.activeMode = AuthMode.LOGIN;
        applyModeUi();
        roleCombo.setSelectedIndex(0);
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
        return submitButton;
    }

    private static final class InsetsToMargin {
        private java.awt.Insets toInsets() {
            return new java.awt.Insets(28, 30, 24, 30);
        }
    }
}

