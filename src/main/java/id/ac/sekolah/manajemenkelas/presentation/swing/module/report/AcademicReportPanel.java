package id.ac.sekolah.manajemenkelas.presentation.swing.module.report;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.BotanicalCharts;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;

/**
 * Layar Laporan Akademik: distribusi siswa aktif per kelas, kapasitas, dan jenis kelamin.
 */
public class AcademicReportPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final BotanicalCharts.BarChartPanel classChart = new BotanicalCharts.BarChartPanel();
    private final BotanicalCharts.DonutChartPanel genderChart = new BotanicalCharts.DonutChartPanel();

    private final DefaultTableModel classModel = new DefaultTableModel(
            new String[]{"ID", "Kelas", "Tingkat", "Tahun Ajaran", "Siswa", "Kap.", "Isi"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel genderModel = new DefaultTableModel(
            new String[]{"Kode", "Jenis Kelamin", "Jumlah Siswa Aktif"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StateDeckPanel stateDeck;

    public AcademicReportPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 16));
        grid.setOpaque(false);

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);
        ClayCard leftChartCard = new ClayCard(new BorderLayout(0, 8));
        JLabel leftTitle = new JLabel("Grafik Siswa Aktif per Kelas");
        leftTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        leftChartCard.add(leftTitle, BorderLayout.NORTH);
        leftChartCard.add(classChart, BorderLayout.CENTER);

        ClayCard rightChartCard = new ClayCard(new BorderLayout(0, 8));
        JLabel rightTitle = new JLabel("Proporsi Jenis Kelamin Siswa Aktif");
        rightTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        rightChartCard.add(rightTitle, BorderLayout.NORTH);
        rightChartCard.add(genderChart, BorderLayout.CENTER);
        chartsRow.add(leftChartCard);
        chartsRow.add(rightChartCard);

        JPanel tablesRow = new JPanel(new GridLayout(1, 2, 16, 0));
        tablesRow.setOpaque(false);
        ClayCard classTableCard = new ClayCard(new BorderLayout(0, 8));
        JLabel classTableTitle = new JLabel("Rincian Siswa & Kapasitas Kelas");
        classTableTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        classTableCard.add(classTableTitle, BorderLayout.NORTH);
        StyledTable classTable = new StyledTable(classModel);
        classTable.setCenterAlignedColumns(0, 2, 4, 5, 6);
        classTable.setColumnWidths(36, 54, 68, 96, 52, 48, 48);
        classTableCard.add(classTable.wrapInScrollPane(), BorderLayout.CENTER);

        ClayCard genderTableCard = new ClayCard(new BorderLayout(0, 8));
        JLabel genderTableTitle = new JLabel("Rekapitulasi Jenis Kelamin");
        genderTableTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        genderTableCard.add(genderTableTitle, BorderLayout.NORTH);
        StyledTable genderTable = new StyledTable(genderModel);
        genderTable.setCenterAlignedColumns(0, 2);
        genderTableCard.add(genderTable.wrapInScrollPane(), BorderLayout.CENTER);

        tablesRow.add(classTableCard);
        tablesRow.add(genderTableCard);

        grid.add(chartsRow);
        grid.add(tablesRow);

        stateDeck = new StateDeckPanel(
                grid,
                "Belum ada data laporan akademik",
                "Belum terdapat kelas aktif atau siswa terdaftar untuk ditampilkan.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        AsyncTaskRunner.run(
                () -> new Object[]{
                        container.getReportService().getStudentCountsByClass(),
                        container.getReportService().getStudentCountsByGender()
                },
                () -> stateDeck.showLoading("Menyusun laporan akademik..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<ClassStudentCountReport> classCounts = (List<ClassStudentCountReport>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<GenderStudentCountReport> genderCounts = (List<GenderStudentCountReport>) arr[1];

                    if (classCounts.isEmpty() && genderCounts.isEmpty()) {
                        stateDeck.showEmpty();
                        return;
                    }

                    List<BotanicalCharts.ChartItem> cItems = new ArrayList<>();
                    Color[] palette = {BotanicalTheme.EVERGREEN, BotanicalTheme.SUCCESS, BotanicalTheme.SAGE};
                    classModel.setRowCount(0);
                    for (int i = 0; i < classCounts.size(); i++) {
                        ClassStudentCountReport r = classCounts.get(i);
                        int occPct = r.getCapacity() > 0
                                ? (int) Math.round((r.getActiveStudentCount() * 100.0) / r.getCapacity())
                                : 0;
                        classModel.addRow(new Object[]{
                                r.getClassId(),
                                r.getClassName(),
                                "Kelas " + r.getGradeLevel(),
                                r.getAcademicYearName(),
                                r.getActiveStudentCount(),
                                r.getCapacity(),
                                occPct + "%"
                        });
                        cItems.add(new BotanicalCharts.ChartItem(r.getClassName(), r.getActiveStudentCount(), palette[i % palette.length]));
                    }
                    classChart.setData(cItems, "", 0);

                    List<BotanicalCharts.ChartItem> gItems = new ArrayList<>();
                    genderModel.setRowCount(0);
                    for (GenderStudentCountReport g : genderCounts) {
                        genderModel.addRow(new Object[]{
                                g.getGenderCode(),
                                g.getGenderLabel(),
                                g.getActiveStudentCount()
                        });
                        Color c = "L".equalsIgnoreCase(g.getGenderCode()) ? BotanicalTheme.EVERGREEN : BotanicalTheme.SAGE;
                        gItems.add(new BotanicalCharts.ChartItem(g.getGenderLabel(), g.getActiveStudentCount(), c));
                    }
                    genderChart.setData(gItems, "Siswa");
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }
}

