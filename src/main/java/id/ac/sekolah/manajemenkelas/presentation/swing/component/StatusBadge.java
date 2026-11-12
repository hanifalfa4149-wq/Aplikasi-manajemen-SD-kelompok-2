package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Lencana status berbentuk pil untuk tabel, header, dan kartu detail.
 */
public class StatusBadge extends JLabel {

    public enum Style {
        SUCCESS(BotanicalTheme.PALE_MINT, BotanicalTheme.SUCCESS),
        MINT(BotanicalTheme.MINT, BotanicalTheme.EVERGREEN),
        WARNING(BotanicalTheme.WARNING_BG, new Color(0x9C, 0x6F, 0x1E)),
        DANGER(BotanicalTheme.DANGER_BG, BotanicalTheme.DANGER.darker()),
        NEUTRAL(BotanicalTheme.SOFT_SAGE, BotanicalTheme.TEXT_SECONDARY);

        private final Color bg;
        private final Color fg;

        Style(Color bg, Color fg) {
            this.bg = bg;
            this.fg = fg;
        }

        public Color getBg() {
            return bg;
        }

        public Color getFg() {
            return fg;
        }
    }

    private Style style;

    public StatusBadge(String text, Style style) {
        super(text, SwingConstants.CENTER);
        this.style = style;
        setFont(BotanicalTheme.FONT_CAPTION);
        setOpaque(false);
        setForeground(style.fg);
        setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
    }

    public static StatusBadge forBooleanStatus(boolean active) {
        return active
                ? new StatusBadge("AKTIF", Style.SUCCESS)
                : new StatusBadge("NONAKTIF", Style.DANGER);
    }

    public static StatusBadge forSemanticText(String value) {
        if (value == null) {
            return new StatusBadge("-", Style.NEUTRAL);
        }
        String normalized = value.trim().toUpperCase();
        switch (normalized) {
            case "AKTIF":
            case "HADIR":
            case "LULUS":
            case "POSTGRESQL":
                return new StatusBadge(normalized, Style.SUCCESS);
            case "SAKIT":
            case "IZIN":
            case "NAIK_KELAS":
                return new StatusBadge(normalized, Style.WARNING);
            case "NONAKTIF":
            case "ALPA":
            case "KELUAR":
                return new StatusBadge(normalized, Style.DANGER);
            case "PINDAH":
            default:
                return new StatusBadge(normalized, Style.MINT);
        }
    }

    public void setBadge(String text, Style style) {
        this.style = style;
        setText(text);
        setForeground(style.fg);
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + 4, Math.max(24, d.height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(style.bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth() - 1, getHeight() - 1, BotanicalTheme.RADIUS_PILL, BotanicalTheme.RADIUS_PILL));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

