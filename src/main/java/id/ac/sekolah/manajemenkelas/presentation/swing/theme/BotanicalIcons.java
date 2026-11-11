package id.ac.sekolah.manajemenkelas.presentation.swing.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/**
 * Kumpulan ikon vektor Java2D yang bersih dan tajam di segala DPI
 * untuk navigasi sidebar, tombol aksi, dan kartu status.
 */
public final class BotanicalIcons {

    public enum Type {
        BRAND_LEAF,
        DASHBOARD,
        STUDENTS,
        TEACHERS,
        CALENDAR,
        SEMESTER,
        CLASSROOM,
        SUBJECT,
        HOMEROOM,
        TEACHING,
        PLACEMENT,
        ATTENDANCE,
        ASSESSMENT,
        SCORES,
        REPORTS,
        LOGOUT,
        SEARCH,
        PLUS,
        EDIT,
        DEACTIVATE,
        REFRESH,
        EYE,
        EYE_OFF,
        CHECK,
        WARNING,
        DETAIL
    }

    private BotanicalIcons() {
    }

    public static Icon get(Type type, int size, Color color) {
        return new VectorIcon(type, size, color);
    }

    private static final class VectorIcon implements Icon {

        private final Type type;
        private final int size;
        private final Color color;

        private VectorIcon(Type type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                float scale = size / 24.0f;
                g2.scale(scale, scale);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawPath(g2);
            } finally {
                g2.dispose();
            }
        }

        private void drawPath(Graphics2D g2) {
            switch (type) {
                case BRAND_LEAF -> {
                    GeneralPath leaf = new GeneralPath();
                    leaf.moveTo(4, 20);
                    leaf.curveTo(4, 10, 10, 4, 20, 4);
                    leaf.curveTo(20, 14, 14, 20, 4, 20);
                    leaf.closePath();
                    g2.draw(leaf);
                    g2.draw(new Line2D.Float(4, 20, 14, 10));
                }
                case DASHBOARD -> {
                    g2.draw(new RoundRectangle2D.Float(3, 3, 8, 8, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(13, 3, 8, 5, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(13, 10, 8, 11, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(3, 13, 8, 8, 2, 2));
                }
                case STUDENTS -> {
                    g2.draw(new Ellipse2D.Float(6.5f, 4.5f, 5.5f, 5.5f));
                    g2.draw(new Arc2D.Float(3, 12.5f, 12.5f, 9, 0, 180, Arc2D.OPEN));
                    g2.draw(new Ellipse2D.Float(14.5f, 6, 4.5f, 4.5f));
                    g2.draw(new Arc2D.Float(13.5f, 13.5f, 7.5f, 7, 10, 130, Arc2D.OPEN));
                }
                case TEACHERS -> {
                    g2.draw(new Ellipse2D.Float(8.5f, 4, 7, 7));
                    g2.draw(new Arc2D.Float(4, 13.5f, 16, 10, 0, 180, Arc2D.OPEN));
                }
                case CALENDAR -> {
                    g2.draw(new RoundRectangle2D.Float(3.5f, 5, 17, 15.5f, 2.5f, 2.5f));
                    g2.draw(new Line2D.Float(3.5f, 10, 20.5f, 10));
                    g2.draw(new Line2D.Float(8, 3, 8, 6.5f));
                    g2.draw(new Line2D.Float(16, 3, 16, 6.5f));
                }
                case SEMESTER -> {
                    g2.draw(new RoundRectangle2D.Float(4, 4, 16, 16, 3, 3));
                    g2.draw(new Line2D.Float(12, 4, 12, 20));
                    g2.draw(new Line2D.Float(7, 9, 9.5f, 9));
                    g2.draw(new Line2D.Float(14.5f, 15, 17, 15));
                }
                case CLASSROOM -> {
                    g2.draw(new RoundRectangle2D.Float(3, 4.5f, 18, 12, 2, 2));
                    g2.draw(new Line2D.Float(7, 20, 17, 20));
                    g2.draw(new Line2D.Float(12, 16.5f, 12, 20));
                }
                case SUBJECT -> {
                    g2.draw(new RoundRectangle2D.Float(4.5f, 3.5f, 15, 17, 2, 2));
                    g2.draw(new Line2D.Float(8.5f, 3.5f, 8.5f, 20.5f));
                    g2.draw(new Line2D.Float(11.5f, 8.5f, 16.5f, 8.5f));
                    g2.draw(new Line2D.Float(11.5f, 12.5f, 16.5f, 12.5f));
                }
                case HOMEROOM -> {
                    g2.draw(new Ellipse2D.Float(8, 4.5f, 6, 6));
                    g2.draw(new Arc2D.Float(4, 13, 14, 9, 0, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Float(16.5f, 15.5f, 18.5f, 17.5f));
                    g2.draw(new Line2D.Float(18.5f, 17.5f, 21.5f, 13.5f));
                }
                case TEACHING -> {
                    g2.draw(new Line2D.Float(4, 6, 20, 6));
                    g2.draw(new Line2D.Float(4, 12, 14, 12));
                    g2.draw(new Line2D.Float(4, 18, 12, 18));
                    g2.draw(new Ellipse2D.Float(15, 13, 5.5f, 5.5f));
                }
                case PLACEMENT -> {
                    g2.draw(new Line2D.Float(4, 8, 18, 8));
                    g2.draw(new Line2D.Float(14.5f, 4.5f, 18, 8));
                    g2.draw(new Line2D.Float(14.5f, 11.5f, 18, 8));
                    g2.draw(new Line2D.Float(20, 16, 6, 16));
                    g2.draw(new Line2D.Float(9.5f, 12.5f, 6, 16));
                    g2.draw(new Line2D.Float(9.5f, 19.5f, 6, 16));
                }
                case ATTENDANCE -> {
                    g2.draw(new RoundRectangle2D.Float(4, 4, 16, 16, 3, 3));
                    g2.draw(new Line2D.Float(8, 12, 11, 15));
                    g2.draw(new Line2D.Float(11, 15, 16.5f, 9));
                }
                case ASSESSMENT -> {
                    g2.draw(new RoundRectangle2D.Float(5, 3.5f, 14, 17, 2, 2));
                    g2.draw(new Line2D.Float(8.5f, 8.5f, 15.5f, 8.5f));
                    g2.draw(new Line2D.Float(8.5f, 12.5f, 15.5f, 12.5f));
                    g2.draw(new Line2D.Float(8.5f, 16.5f, 13, 16.5f));
                }
                case SCORES -> {
                    g2.draw(new Rectangle2D.Float(4, 13, 4, 7));
                    g2.draw(new Rectangle2D.Float(10, 8, 4, 12));
                    g2.draw(new Rectangle2D.Float(16, 4, 4, 16));
                }
                case REPORTS -> {
                    g2.draw(new Arc2D.Float(4, 4, 16, 16, 90, 270, Arc2D.PIE));
                    g2.draw(new Arc2D.Float(7, 2, 15, 15, 0, 90, Arc2D.PIE));
                }
                case LOGOUT -> {
                    g2.draw(new Arc2D.Float(4, 4, 12, 16, 90, 180, Arc2D.OPEN));
                    g2.draw(new Line2D.Float(10, 12, 20, 12));
                    g2.draw(new Line2D.Float(16.5f, 8.5f, 20, 12));
                    g2.draw(new Line2D.Float(16.5f, 15.5f, 20, 12));
                }
                case SEARCH -> {
                    g2.draw(new Ellipse2D.Float(4.5f, 4.5f, 11.5f, 11.5f));
                    g2.draw(new Line2D.Float(14.5f, 14.5f, 19.5f, 19.5f));
                }
                case PLUS -> {
                    g2.draw(new Line2D.Float(12, 5, 12, 19));
                    g2.draw(new Line2D.Float(5, 12, 19, 12));
                }
                case EDIT -> {
                    GeneralPath pen = new GeneralPath();
                    pen.moveTo(4, 20);
                    pen.lineTo(5.5f, 14.5f);
                    pen.lineTo(15.5f, 4.5f);
                    pen.lineTo(19.5f, 8.5f);
                    pen.lineTo(9.5f, 18.5f);
                    pen.closePath();
                    g2.draw(pen);
                }
                case DEACTIVATE -> {
                    g2.draw(new Ellipse2D.Float(4, 4, 16, 16));
                    g2.draw(new Line2D.Float(7.5f, 7.5f, 16.5f, 16.5f));
                }
                case REFRESH -> {
                    g2.draw(new Arc2D.Float(4.5f, 4.5f, 15, 15, 35, 290, Arc2D.OPEN));
                    g2.draw(new Line2D.Float(15.5f, 4.5f, 19, 7));
                    g2.draw(new Line2D.Float(19, 7, 15, 9));
                }
                case EYE -> {
                    GeneralPath eye = new GeneralPath();
                    eye.moveTo(2.5f, 12);
                    eye.quadTo(12, 4.5f, 21.5f, 12);
                    eye.quadTo(12, 19.5f, 2.5f, 12);
                    g2.draw(eye);
                    g2.draw(new Ellipse2D.Float(9.5f, 9.5f, 5, 5));
                }
                case EYE_OFF -> {
                    GeneralPath eye = new GeneralPath();
                    eye.moveTo(2.5f, 12);
                    eye.quadTo(12, 4.5f, 21.5f, 12);
                    eye.quadTo(12, 19.5f, 2.5f, 12);
                    g2.draw(eye);
                    g2.draw(new Line2D.Float(4, 4, 20, 20));
                }
                case CHECK -> {
                    g2.draw(new Line2D.Float(5, 12.5f, 10, 17));
                    g2.draw(new Line2D.Float(10, 17, 19.5f, 7));
                }
                case WARNING -> {
                    GeneralPath tri = new GeneralPath();
                    tri.moveTo(12, 4);
                    tri.lineTo(21, 19.5f);
                    tri.lineTo(3, 19.5f);
                    tri.closePath();
                    g2.draw(tri);
                    g2.draw(new Line2D.Float(12, 10, 12, 14.5f));
                    g2.draw(new Ellipse2D.Float(11.3f, 16.8f, 1.4f, 1.4f));
                }
                case DETAIL -> {
                    g2.draw(new Ellipse2D.Float(4, 4, 16, 16));
                    g2.draw(new Line2D.Float(12, 11, 12, 16));
                    g2.draw(new Ellipse2D.Float(11.3f, 7.3f, 1.4f, 1.4f));
                }
            }
        }
    }
}

