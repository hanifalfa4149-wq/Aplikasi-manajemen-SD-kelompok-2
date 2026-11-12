package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Komponen grafik Java2D ringan yang selaras dengan palet Botanical Green
 * (menggunakan data aplikasi nyata, tanpa library eksternal).
 */
public final class BotanicalCharts {

    /**
     * Item data untuk grafik batang maupun donut.
     */
    public static final class ChartItem {
        private final String label;
        private final double value;
        private final Color color;

        public ChartItem(String label, double value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public double getValue() {
            return value;
        }

        public Color getColor() {
            return color;
        }

        public String label() {
            return label;
        }

        public double value() {
            return value;
        }

        public Color color() {
            return color;
        }
    }

    private BotanicalCharts() {
    }

    /**
     * Grafik batang vertikal untuk distribusi siswa, kehadiran per kelas, maupun nilai mapel.
     */
    public static final class BarChartPanel extends JPanel {

        private List<ChartItem> items = List.of();
        private String valueSuffix = "";
        private double fixedMax = 0;

        public BarChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(360, 220));
        }

        public void setData(List<ChartItem> items, String valueSuffix, double fixedMax) {
            this.items = items == null ? List.of() : new ArrayList<>(items);
            this.valueSuffix = valueSuffix == null ? "" : valueSuffix;
            this.fixedMax = fixedMax;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                if (items.isEmpty()) {
                    drawEmptyMessage(g2, w, h, "Belum ada data grafik.");
                    return;
                }

                int leftPad = 14;
                int rightPad = 14;
                int topPad = 24;
                int bottomPad = 36;
                int chartW = Math.max(40, w - leftPad - rightPad);
                int chartH = Math.max(40, h - topPad - bottomPad);
                int baseY = topPad + chartH;

                double maxVal = fixedMax;
                if (maxVal <= 0) {
                    for (ChartItem item : items) {
                        if (item.value() > maxVal) {
                            maxVal = item.value();
                        }
                    }
                }
                if (maxVal <= 0) {
                    maxVal = 1;
                }

                // Garis referensi horizontal halus
                g2.setColor(BotanicalTheme.BORDER);
                for (int step = 0; step <= 2; step++) {
                    int y = topPad + (chartH * step) / 2;
                    g2.drawLine(leftPad, y, leftPad + chartW, y);
                }

                int count = items.size();
                int slotW = chartW / count;
                int barW = Math.min(44, Math.max(18, slotW - 18));

                g2.setFont(BotanicalTheme.FONT_SMALL_BOLD);
                FontMetrics fm = g2.getFontMetrics();

                for (int i = 0; i < count; i++) {
                    ChartItem item = items.get(i);
                    double clamped = Math.max(0, item.value());
                    int barH = (int) Math.round((clamped / maxVal) * (chartH - 6));
                    if (clamped > 0 && barH < 4) {
                        barH = 4;
                    }
                    int x = leftPad + i * slotW + (slotW - barW) / 2;
                    int y = baseY - barH;

                    Color barColor = item.color() != null ? item.color() : BotanicalTheme.EVERGREEN;
                    g2.setColor(barColor);
                    g2.fill(new RoundRectangle2D.Float(x, y, barW, barH, 8, 8));

                    // Label nilai di atas batang
                    String valLabel = formatValue(clamped) + valueSuffix;
                    g2.setColor(BotanicalTheme.TEXT_PRIMARY);
                    int valX = x + (barW - fm.stringWidth(valLabel)) / 2;
                    g2.drawString(valLabel, Math.max(4, valX), Math.max(14, y - 6));

                    // Label kategori di bawah sumbu X
                    String cat = truncate(item.label(), Math.max(6, (slotW - 4) / 7));
                    g2.setFont(BotanicalTheme.FONT_SMALL);
                    g2.setColor(BotanicalTheme.TEXT_SECONDARY);
                    FontMetrics catFm = g2.getFontMetrics();
                    int catX = x + (barW - catFm.stringWidth(cat)) / 2;
                    g2.drawString(cat, Math.max(4, catX), baseY + 18);
                    g2.setFont(BotanicalTheme.FONT_SMALL_BOLD);
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /**
     * Grafik donut ringkas dengan legenda proporsional untuk gender / status absensi.
     */
    public static final class DonutChartPanel extends JPanel {

        private List<ChartItem> items = List.of();
        private String centerCaption = "Total";

        public DonutChartPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(320, 200));
        }

        public void setData(List<ChartItem> items, String centerCaption) {
            this.items = items == null ? List.of() : new ArrayList<>(items);
            this.centerCaption = centerCaption == null ? "Total" : centerCaption;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();
                double total = 0;
                for (ChartItem item : items) {
                    total += Math.max(0, item.value());
                }
                if (items.isEmpty() || total <= 0) {
                    drawEmptyMessage(g2, w, h, "Belum ada data proporsi.");
                    return;
                }

                int diameter = Math.max(96, Math.min(h - 24, (int) (w * 0.44)));
                int ringX = 16;
                int ringY = (h - diameter) / 2;

                double startAngle = 90;
                for (ChartItem item : items) {
                    double val = Math.max(0, item.value());
                    if (val <= 0) {
                        continue;
                    }
                    double sweep = -(val / total) * 360.0;
                    g2.setColor(item.color() != null ? item.color() : BotanicalTheme.EVERGREEN);
                    g2.fill(new Arc2D.Double(ringX, ringY, diameter, diameter, startAngle, sweep, Arc2D.PIE));
                    startAngle += sweep;
                }

                int innerSize = (int) (diameter * 0.58);
                int innerOffset = (diameter - innerSize) / 2;
                g2.setColor(BotanicalTheme.WHITE);
                g2.fillOval(ringX + innerOffset, ringY + innerOffset, innerSize, innerSize);

                // Teks tengah donut
                String totalStr = formatValue(total);
                g2.setFont(BotanicalTheme.FONT_SUBTITLE);
                g2.setColor(BotanicalTheme.TEXT_PRIMARY);
                FontMetrics fmVal = g2.getFontMetrics();
                int cx = ringX + diameter / 2;
                int cy = ringY + diameter / 2;
                g2.drawString(totalStr, cx - fmVal.stringWidth(totalStr) / 2, cy + 2);

                g2.setFont(BotanicalTheme.FONT_SMALL);
                g2.setColor(BotanicalTheme.TEXT_SECONDARY);
                FontMetrics fmCap = g2.getFontMetrics();
                g2.drawString(centerCaption, cx - fmCap.stringWidth(centerCaption) / 2, cy + 18);

                // Legenda di sisi kanan
                int legendX = ringX + diameter + 22;
                int legendY = Math.max(24, ringY + 14);
                for (ChartItem item : items) {
                    if (legendY > h - 14) {
                        break;
                    }
                    g2.setColor(item.color() != null ? item.color() : BotanicalTheme.EVERGREEN);
                    g2.fillRoundRect(legendX, legendY - 10, 12, 12, 4, 4);

                    double pct = (Math.max(0, item.value()) * 100.0) / total;
                    String text = item.label() + ": " + formatValue(item.value())
                            + " (" + String.format("%.0f%%", pct) + ")";
                    g2.setFont(BotanicalTheme.FONT_SMALL_BOLD);
                    g2.setColor(BotanicalTheme.TEXT_PRIMARY);
                    g2.drawString(text, legendX + 18, legendY);
                    legendY += 26;
                }
            } finally {
                g2.dispose();
            }
        }
    }

    private static void drawEmptyMessage(Graphics2D g2, int w, int h, String message) {
        g2.setFont(BotanicalTheme.FONT_BODY);
        g2.setColor(BotanicalTheme.TEXT_SECONDARY);
        FontMetrics fm = g2.getFontMetrics();
        int x = (w - fm.stringWidth(message)) / 2;
        int y = h / 2;
        g2.drawString(message, Math.max(8, x), y);
    }

    private static String formatValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.005) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format("%.1f", value);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "-";
        }
        if (text.length() <= maxLen || maxLen < 4) {
            return text;
        }
        return text.substring(0, maxLen - 2) + "..";
    }
}

