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
 * Tombol sekunder lembut untuk aksi batal, muat ulang, atau filter tambahan.
 */
public class SecondaryButton extends JButton {

    private boolean hovered = false;

    public SecondaryButton(String text) {
        this(text, null);
    }

    public SecondaryButton(String text, Icon icon) {
        super(text, icon);
        initStyle();
    }

    private void initStyle() {
        setFont(BotanicalTheme.FONT_BODY_BOLD);
        setForeground(BotanicalTheme.EVERGREEN);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 14, 8, 14));
        setIconTextGap(8);
        setPreferredSize(new Dimension(getPreferredSize().width + 8, BotanicalTheme.INPUT_HEIGHT));

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

            Color fill;
            if (!isEnabled()) {
                fill = BotanicalTheme.SOFT_IVORY;
            } else if (getModel().isPressed()) {
                fill = BotanicalTheme.MINT;
            } else if (hovered) {
                fill = BotanicalTheme.SOFT_SAGE;
            } else {
                fill = BotanicalTheme.PALE_MINT;
            }

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));

            g2.setColor(isFocusOwner() ? BotanicalTheme.EVERGREEN : BotanicalTheme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
        } finally {
            g2.dispose();
        }
        setForeground(isEnabled() ? BotanicalTheme.EVERGREEN : BotanicalTheme.TEXT_SECONDARY);
        super.paintComponent(g);
    }
}

