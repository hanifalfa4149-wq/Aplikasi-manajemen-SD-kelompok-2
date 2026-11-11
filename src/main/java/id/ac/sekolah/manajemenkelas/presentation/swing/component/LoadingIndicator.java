package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Indikator pemuatan non-intrusif untuk operasi SwingWorker ke PostgreSQL.
 */
public class LoadingIndicator extends JPanel {

    private final SpinnerRing ring = new SpinnerRing();
    private final JLabel messageLabel = new JLabel("Memuat data...");

    public LoadingIndicator() {
        this("Memuat data...");
    }

    public LoadingIndicator(String initialText) {
        setOpaque(false);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(6, 12, 10, 12);
        add(ring, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 12, 6, 12);
        messageLabel.setText(initialText);
        messageLabel.setFont(BotanicalTheme.FONT_BODY);
        messageLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
        add(messageLabel, gbc);
    }

    public void start(String message) {
        if (message != null && !message.isBlank()) {
            messageLabel.setText(message);
        }
        setVisible(true);
        ring.start();
    }

    public void stop() {
        ring.stop();
        setVisible(false);
    }

    private static final class SpinnerRing extends JPanel {

        private int angle = 0;
        private final Timer timer;

        private SpinnerRing() {
            setOpaque(false);
            setPreferredSize(new Dimension(36, 36));
            timer = new Timer(40, e -> {
                angle = (angle + 18) % 360;
                repaint();
            });
        }

        private void start() {
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        private void stop() {
            if (timer.isRunning()) {
                timer.stop();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(BotanicalTheme.SOFT_SAGE);
                g2.drawOval(4, 4, 28, 28);
                g2.setColor(BotanicalTheme.EVERGREEN);
                g2.drawArc(4, 4, 28, 28, angle, 110);
            } finally {
                g2.dispose();
            }
        }
    }
}

