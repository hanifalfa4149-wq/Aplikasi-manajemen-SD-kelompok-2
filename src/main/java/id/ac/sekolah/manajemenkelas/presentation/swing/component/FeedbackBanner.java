package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.ExecutionException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Banner notifikasi inline untuk pesan sukses, peringatan, info, maupun galat database.
 */
public class FeedbackBanner extends JPanel {

    public enum Level {
        SUCCESS(BotanicalTheme.PALE_MINT, BotanicalTheme.SUCCESS, BotanicalIcons.Type.CHECK),
        WARNING(BotanicalTheme.WARNING_BG, new Color(0x9C, 0x6F, 0x1E), BotanicalIcons.Type.WARNING),
        ERROR(BotanicalTheme.DANGER_BG, BotanicalTheme.DANGER.darker(), BotanicalIcons.Type.WARNING),
        INFO(BotanicalTheme.SOFT_SAGE, BotanicalTheme.EVERGREEN, BotanicalIcons.Type.DETAIL);

        private final Color bg;
        private final Color fg;
        private final BotanicalIcons.Type iconType;

        Level(Color bg, Color fg, BotanicalIcons.Type iconType) {
            this.bg = bg;
            this.fg = fg;
            this.iconType = iconType;
        }
    }

    private Level level = Level.INFO;
    private final JLabel iconLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JButton closeButton = new JButton("×");

    public FeedbackBanner() {
        setOpaque(false);
        setLayout(new BorderLayout(10, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 10));
        setVisible(false);

        add(iconLabel, BorderLayout.WEST);

        messageLabel.setFont(BotanicalTheme.FONT_BODY_BOLD);
        add(messageLabel, BorderLayout.CENTER);

        closeButton.setFont(BotanicalTheme.FONT_SUBTITLE);
        closeButton.setOpaque(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> clear());
        add(closeButton, BorderLayout.EAST);
    }

    public void showSuccess(String message) {
        showMessage(Level.SUCCESS, message);
    }

    public void showWarning(String message) {
        showMessage(Level.WARNING, message);
    }

    public void showError(String message) {
        showMessage(Level.ERROR, message);
    }

    public void showError(Throwable error) {
        showMessage(Level.ERROR, extractMessage(error));
    }

    public void showInfo(String message) {
        showMessage(Level.INFO, message);
    }

    public void showMessage(Level level, String message) {
        if (message == null || message.isBlank()) {
            clear();
            return;
        }
        this.level = level;
        iconLabel.setIcon(BotanicalIcons.get(level.iconType, 18, level.fg));
        messageLabel.setForeground(level.fg);
        messageLabel.setText("<html>" + escapeHtml(message) + "</html>");
        closeButton.setForeground(level.fg);
        setVisible(true);
        revalidate();
        repaint();
    }

    public void clear() {
        setVisible(false);
    }

    public static String extractMessage(Throwable throwable) {
        if (throwable == null) {
            return "Terjadi kesalahan yang tidak diketahui.";
        }
        Throwable current = throwable;
        if (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current.getClass().getSimpleName();
        }
        return message;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(level.bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
            g2.setColor(level.fg);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 2, getHeight() - 2, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

