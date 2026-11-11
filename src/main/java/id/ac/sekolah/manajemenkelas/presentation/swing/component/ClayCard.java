package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Kontainer rounded dengan efek claymorphism lembut (bayangan tipis + sorotan atas halus).
 */
public class ClayCard extends JPanel {

    private Color surfaceColor = BotanicalTheme.WHITE;
    private Color borderColor = BotanicalTheme.BORDER;
    private int cornerRadius = BotanicalTheme.RADIUS_CARD;
    private boolean drawShadow = true;

    public ClayCard() {
        this(null, new Insets(18, 20, 18, 20));
    }

    public ClayCard(LayoutManager layout) {
        this(layout, new Insets(18, 20, 18, 20));
    }

    public ClayCard(LayoutManager layout, Insets padding) {
        if (layout != null) {
            setLayout(layout);
        }
        setOpaque(false);
        setPadding(padding);
    }

    public void setSurfaceColor(Color surfaceColor) {
        this.surfaceColor = surfaceColor;
        repaint();
    }

    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
        repaint();
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
        repaint();
    }

    public void setDrawShadow(boolean drawShadow) {
        this.drawShadow = drawShadow;
        repaint();
    }

    public void setPadding(Insets padding) {
        int shadowPad = drawShadow ? 3 : 1;
        setBorder(BorderFactory.createEmptyBorder(
                padding.top + 1,
                padding.left + 1,
                padding.bottom + shadowPad,
                padding.right + shadowPad));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int padRight = drawShadow ? 3 : 1;
            int padBottom = drawShadow ? 3 : 1;
            int cardW = Math.max(4, w - padRight - 1);
            int cardH = Math.max(4, h - padBottom - 1);

            if (drawShadow) {
                g2.setColor(BotanicalTheme.CLAY_SHADOW_SOFT);
                g2.fill(new RoundRectangle2D.Float(3, 4, cardW, cardH, cornerRadius, cornerRadius));
                g2.setColor(BotanicalTheme.CLAY_SHADOW);
                g2.fill(new RoundRectangle2D.Float(1.5f, 2.5f, cardW, cardH, cornerRadius, cornerRadius));
            }

            g2.setColor(surfaceColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, cardW, cardH, cornerRadius, cornerRadius));

            // Sorotan lembut bagian atas untuk kesan clay tactile
            g2.setColor(BotanicalTheme.CLAY_HIGHLIGHT);
            g2.draw(new RoundRectangle2D.Float(1, 1, cardW - 2, cardH - 2, cornerRadius - 1, cornerRadius - 1));

            g2.setColor(borderColor);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, cardW - 1, cardH - 1, cornerRadius, cornerRadius));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}

