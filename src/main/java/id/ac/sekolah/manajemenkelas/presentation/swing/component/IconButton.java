package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;
import javax.swing.JButton;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Tombol ikon ringkas untuk aksi baris tabel dan toolbar (Edit, Nonaktifkan, Detail, dll.).
 */
public class IconButton extends JButton {

    public enum Tone {
        NEUTRAL(BotanicalTheme.PALE_MINT, BotanicalTheme.SOFT_SAGE, BotanicalTheme.EVERGREEN),
        SUCCESS(BotanicalTheme.PALE_MINT, BotanicalTheme.MINT, BotanicalTheme.SUCCESS),
        WARNING(BotanicalTheme.WARNING_BG, BotanicalTheme.SOFT_SAGE, BotanicalTheme.WARNING.darker()),
        DANGER(BotanicalTheme.DANGER_BG, new Color(0xF3, 0xD8, 0xD6), BotanicalTheme.DANGER);

        private final Color bg;
        private final Color hoverBg;
        private final Color fg;

        Tone(Color bg, Color hoverBg, Color fg) {
            this.bg = bg;
            this.hoverBg = hoverBg;
            this.fg = fg;
        }
    }

    private Tone tone;
    private boolean hovered = false;

    public IconButton(Icon icon, String tooltip) {
        this(null, icon, tooltip, Tone.NEUTRAL);
    }

    public IconButton(String text, Icon icon, String tooltip, Tone tone) {
        super(text, icon);
        this.tone = tone;
        setToolTipText(tooltip);
        initStyle(text != null && !text.isBlank());
    }

    public void setTone(Tone tone) {
        this.tone = tone;
        setForeground(tone.fg);
        repaint();
    }

    private void initStyle(boolean hasText) {
        setFont(BotanicalTheme.FONT_SMALL_BOLD);
        setForeground(tone.fg);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(6);
        setMargin(hasText ? new Insets(6, 11, 6, 11) : new Insets(6, 8, 6, 8));
        if (!hasText) {
            setPreferredSize(new Dimension(34, 34));
        } else {
            setPreferredSize(new Dimension(getPreferredSize().width + 4, 34));
        }

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    hovered = true;
                    repaint();
                }
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
            int w = getWidth();
            int h = getHeight();

            Color fill = !isEnabled()
                    ? BotanicalTheme.SOFT_IVORY
                    : (hovered || getModel().isPressed() ? tone.hoverBg : tone.bg);

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 9, 9));

            g2.setColor(isFocusOwner() ? tone.fg : BotanicalTheme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, 9, 9));
        } finally {
            g2.dispose();
        }
        setForeground(isEnabled() ? tone.fg : BotanicalTheme.TEXT_SECONDARY);
        super.paintComponent(g);
    }
}

