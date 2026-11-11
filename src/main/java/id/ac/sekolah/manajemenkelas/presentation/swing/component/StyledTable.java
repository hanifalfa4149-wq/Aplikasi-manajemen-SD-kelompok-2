package id.ac.sekolah.manajemenkelas.presentation.swing.component;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Tabel bergaya Botanical Green dengan kustomisasi TableHeaderRenderer dan TableCellRenderer.
 * Dirancang tetap padat informasi, jelas dibaca, dan mendukung rendering lencana status otomatis.
 */
public class StyledTable extends JTable {

    private final Set<Integer> statusColumns = new HashSet<>();
    private final Set<Integer> rightAlignedColumns = new HashSet<>();
    private final Set<Integer> centerAlignedColumns = new HashSet<>();

    public StyledTable() {
        initAppearance();
    }

    public StyledTable(TableModel model) {
        super(model);
        initAppearance();
    }

    private void initAppearance() {
        setFont(BotanicalTheme.FONT_BODY);
        setForeground(BotanicalTheme.TEXT_PRIMARY);
        setBackground(BotanicalTheme.WHITE);
        setRowHeight(38);
        setShowVerticalLines(false);
        setShowHorizontalLines(true);
        setGridColor(BotanicalTheme.BORDER);
        setIntercellSpacing(new Dimension(0, 1));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        getTableHeader().setReorderingAllowed(false);

        JTableHeader header = getTableHeader();
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 40));
        header.setDefaultRenderer(new BotanicalHeaderRenderer());

        setDefaultRenderer(Object.class, new BotanicalCellRenderer());
        setDefaultRenderer(Number.class, new BotanicalCellRenderer());
    }

    public void setStatusColumns(int... columns) {
        statusColumns.clear();
        for (int col : columns) {
            statusColumns.add(col);
        }
        repaint();
    }

    public void setRightAlignedColumns(int... columns) {
        rightAlignedColumns.clear();
        for (int col : columns) {
            rightAlignedColumns.add(col);
        }
        repaint();
    }

    public void setCenterAlignedColumns(int... columns) {
        centerAlignedColumns.clear();
        for (int col : columns) {
            centerAlignedColumns.add(col);
        }
        repaint();
    }

    public void setColumnWidths(int... widths) {
        for (int i = 0; i < widths.length && i < getColumnModel().getColumnCount(); i++) {
            if (widths[i] > 0) {
                getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
        }
    }

    /**
     * Membungkus StyledTable dalam JScrollPane berbingkai halus sesuai tema.
     */
    public JScrollPane wrapInScrollPane() {
        JScrollPane scrollPane = new JScrollPane(this);
        scrollPane.setColumnHeaderView(getTableHeader());
        scrollPane.setBorder(BorderFactory.createLineBorder(BotanicalTheme.BORDER, 1));
        scrollPane.getViewport().setBackground(BotanicalTheme.WHITE);
        scrollPane.setBackground(BotanicalTheme.WHITE);
        return scrollPane;
    }

    private final class BotanicalHeaderRenderer implements TableCellRenderer {

        private final Border padding = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BotanicalTheme.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12));

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            JLabel label = new JLabel(value == null ? "" : value.toString());
            label.setOpaque(true);
            label.setBackground(BotanicalTheme.PALE_MINT);
            label.setForeground(BotanicalTheme.TEXT_PRIMARY);
            label.setFont(BotanicalTheme.FONT_SMALL_BOLD);
            label.setBorder(padding);
            if (rightAlignedColumns.contains(column)) {
                label.setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (centerAlignedColumns.contains(column) || statusColumns.contains(column)) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                label.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return label;
        }
    }

    private final class BotanicalCellRenderer extends DefaultTableCellRenderer {

        private final PillCellLabel pillLabel = new PillCellLabel();

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            Color rowBg = isSelected
                    ? BotanicalTheme.MINT
                    : (row % 2 == 1 ? BotanicalTheme.ROW_ALT : BotanicalTheme.WHITE);

            if (statusColumns.contains(column)) {
                String text = value == null ? "-" : value.toString();
                if ("Aktif".equalsIgnoreCase(text) || "AKTIF".equalsIgnoreCase(text)) {
                    pillLabel.configure(rowBg, "AKTIF", StatusBadge.Style.SUCCESS);
                } else if ("Nonaktif".equalsIgnoreCase(text) || "NONAKTIF".equalsIgnoreCase(text)) {
                    pillLabel.configure(rowBg, "NONAKTIF", StatusBadge.Style.DANGER);
                } else {
                    StatusBadge semantic = StatusBadge.forSemanticText(text);
                    pillLabel.configure(rowBg, semantic.getText(), resolveStyle(text));
                }
                return pillLabel;
            }

            JLabel cell = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            cell.setBackground(rowBg);
            cell.setForeground(BotanicalTheme.TEXT_PRIMARY);
            cell.setFont(BotanicalTheme.FONT_BODY);
            cell.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

            if (rightAlignedColumns.contains(column) || value instanceof Number) {
                cell.setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (centerAlignedColumns.contains(column)) {
                cell.setHorizontalAlignment(SwingConstants.CENTER);
            } else {
                cell.setHorizontalAlignment(SwingConstants.LEFT);
            }
            return cell;
        }

        private StatusBadge.Style resolveStyle(String text) {
            if (text == null) {
                return StatusBadge.Style.NEUTRAL;
            }
            return switch (text.trim().toUpperCase()) {
                case "AKTIF", "HADIR", "LULUS", "POSTGRESQL" -> StatusBadge.Style.SUCCESS;
                case "SAKIT", "IZIN", "NAIK_KELAS", "INMEMORY" -> StatusBadge.Style.WARNING;
                case "NONAKTIF", "ALPA", "KELUAR" -> StatusBadge.Style.DANGER;
                default -> StatusBadge.Style.MINT;
            };
        }
    }

    private static final class PillCellLabel extends JLabel {

        private Color rowBg = BotanicalTheme.WHITE;
        private StatusBadge.Style badgeStyle = StatusBadge.Style.NEUTRAL;

        private PillCellLabel() {
            super("", SwingConstants.CENTER);
            setOpaque(false);
            setFont(BotanicalTheme.FONT_CAPTION);
        }

        private void configure(Color rowBg, String text, StatusBadge.Style badgeStyle) {
            this.rowBg = rowBg;
            this.badgeStyle = badgeStyle;
            setText(text);
            setForeground(badgeStyle.getFg());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(rowBg);
                g2.fillRect(0, 0, getWidth(), getHeight());

                java.awt.FontMetrics fm = g2.getFontMetrics(getFont());
                int textWidth = fm.stringWidth(getText());
                int pillWidth = Math.min(Math.max(textWidth + 20, 56), Math.max(24, getWidth() - 12));
                int pillHeight = 24;
                int x = (getWidth() - pillWidth) / 2;
                int y = (getHeight() - pillHeight) / 2;

                g2.setColor(badgeStyle.getBg());
                g2.fill(new RoundRectangle2D.Float(x, y, pillWidth, pillHeight,
                        BotanicalTheme.RADIUS_PILL, BotanicalTheme.RADIUS_PILL));
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    /**
     * Panel paginasi ringan untuk tabel data master dan laporan besar.
     */
    public static final class PaginationBar extends JPanel {

        private final JLabel infoLabel = new JLabel();
        private final SecondaryButton prevButton = new SecondaryButton("Sebelumnya");
        private final SecondaryButton nextButton = new SecondaryButton("Berikutnya");
        private int currentPage = 1;
        private int totalPages = 1;
        private Runnable onPageChange;

        public PaginationBar() {
            setOpaque(false);
            setLayout(new java.awt.BorderLayout(12, 0));
            infoLabel.setFont(BotanicalTheme.FONT_SMALL);
            infoLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
            add(infoLabel, java.awt.BorderLayout.WEST);

            JPanel controls = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 0));
            controls.setOpaque(false);
            prevButton.setPreferredSize(new Dimension(112, 32));
            nextButton.setPreferredSize(new Dimension(106, 32));
            controls.add(prevButton);
            controls.add(nextButton);
            add(controls, java.awt.BorderLayout.EAST);

            prevButton.addActionListener(e -> {
                if (currentPage > 1) {
                    currentPage--;
                    if (onPageChange != null) {
                        onPageChange.run();
                    }
                }
            });
            nextButton.addActionListener(e -> {
                if (currentPage < totalPages) {
                    currentPage++;
                    if (onPageChange != null) {
                        onPageChange.run();
                    }
                }
            });
        }

        public void setOnPageChange(Runnable onPageChange) {
            this.onPageChange = onPageChange;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void resetToFirstPage() {
            this.currentPage = 1;
        }

        public void update(int totalItems, int pageSize) {
            this.totalPages = Math.max(1, (int) Math.ceil((double) totalItems / Math.max(1, pageSize)));
            if (currentPage > totalPages) {
                currentPage = totalPages;
            }
            if (totalItems == 0) {
                infoLabel.setText("Menampilkan 0 data");
            } else {
                int from = (currentPage - 1) * pageSize + 1;
                int to = Math.min(totalItems, currentPage * pageSize);
                infoLabel.setText("Menampilkan " + from + "-" + to + " dari " + totalItems
                        + " data (Hal. " + currentPage + "/" + totalPages + ")");
            }
            prevButton.setEnabled(currentPage > 1);
            nextButton.setEnabled(currentPage < totalPages);
        }
    }
}

