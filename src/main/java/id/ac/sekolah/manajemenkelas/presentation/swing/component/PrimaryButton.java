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
 * Tombol utama bergaya Claymorphism Evergreen untuk aksi utama / simpan.
 */
public class PrimaryButton extends JButton {

    private Color baseColor = BotanicalTheme.EVERGREEN;
    private Color hoverColor = BotanicalTheme.DEEP_FOREST;
    private boolean hovered = false;

    public PrimaryButton(String text) {
        this(text, null);
    }

    public PrimaryButton(String text, Icon icon) {
        super(text, icon);
        initStyle();
    }

    public static PrimaryButton danger(String text) {
        PrimaryButton button = new PrimaryButton(text);
        button.setColors(BotanicalTheme.DANGER, BotanicalTheme.DANGER.darker());
        return button;
    }

    public void setColors(Color baseColor, Color hoverColor) {
        this.baseColor = baseColor;
        this.hoverColor = hoverColor;
        repaint();
    }

    private void initStyle() {
        setFont(BotanicalTheme.FONT_BODY_BOLD);
        setForeground(BotanicalTheme.WHITE);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(8, 16, 8, 16));
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
                fill = BotanicalTheme.SAGE;
            } else if (getModel().isPressed() || hovered) {
                fill = hoverColor;
            } else {
                fill = baseColor;
            }

            if (isEnabled()) {
                g2.setColor(BotanicalTheme.CLAY_SHADOW);
                g2.fill(new RoundRectangle2D.Float(1, 2, w - 2, h - 2, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));
            }

            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 2, BotanicalTheme.RADIUS_CONTROL, BotanicalTheme.RADIUS_CONTROL));

            if (isFocusOwner()) {
                g2.setColor(BotanicalTheme.MINT);
                g2.draw(new RoundRectangle2D.Float(1.5f, 1.5f, w - 4, h - 5, BotanicalTheme.RADIUS_CONTROL - 1, BotanicalTheme.RADIUS_CONTROL - 1));
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

