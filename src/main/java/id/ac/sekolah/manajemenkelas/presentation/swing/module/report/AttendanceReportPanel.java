package id.ac.sekolah.manajemenkelas.presentation.swing.module.report;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.BotanicalCharts;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.DateField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SecondaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;

/**
 * Layar Laporan Absensi: analisis rentang tanggal (Hadir/Sakit/Izin/Alpa) dan rekap per kelas.
 */
public class AttendanceReportPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final DateField startField = new DateField(LocalDate.now().withDayOfMonth(1));
    private final DateField endField = new DateField(LocalDate.now());
    private final PrimaryButton applyRangeBtn = new PrimaryButton("Terapkan Rentang");
    private final SecondaryButton allTimeBtn = new SecondaryButton("Semua Catatan");

    private final BotanicalCharts.DonutChartPanel statusDonut = new BotanicalCharts.DonutChartPanel();
    private final BotanicalCharts.BarChartPanel classBar = new BotanicalCharts.BarChartPanel();
    private final JLabel rangeKpiLabel = new JLabel("Persentase Kehadiran: -");

    private final DefaultTableModel classModel = new DefaultTableModel(
            new String[]{"Kelas", "Tingkat", "Total Catatan", "Hadir", "Sakit", "Izin", "Alpa", "Kehadiran (%)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StateDeckPanel stateDeck;
    private boolean useAllTime = true;

    public AttendanceReportPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(feedbackBanner, BorderLayout.NORTH);
        top.add(buildFilterBar(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        JPanel content = new JPanel(new GridLayout(2, 1, 0, 16));
        content.setOpaque(false);

        JPanel chartsRow = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsRow.setOpaque(false);

        ClayCard leftCard = new ClayCard(new BorderLayout(0, 8));
        JPanel leftHead = new JPanel(new BorderLayout());
        leftHead.setOpaque(false);
        JLabel leftTitle = new JLabel("Proporsi Status Kehadiran");
        leftTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        rangeKpiLabel.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        rangeKpiLabel.setForeground(BotanicalTheme.EVERGREEN);
        leftHead.add(leftTitle, BorderLayout.WEST);
        leftHead.add(rangeKpiLabel, BorderLayout.EAST);
        leftCard.add(leftHead, BorderLayout.NORTH);
        leftCard.add(statusDonut, BorderLayout.CENTER);

        ClayCard rightCard = new ClayCard(new BorderLayout(0, 8));
        JLabel rightTitle = new JLabel("Tingkat Kehadiran per Kelas (%)");
        rightTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        rightCard.add(rightTitle, BorderLayout.NORTH);
        rightCard.add(classBar, BorderLayout.CENTER);
        chartsRow.add(leftCard);
        chartsRow.add(rightCard);

        ClayCard tableCard = new ClayCard(new BorderLayout(0, 8));
        JLabel tblTitle = new JLabel("Rekapitulasi Absensi per Kelas");
        tblTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        tableCard.add(tblTitle, BorderLayout.NORTH);
        StyledTable table = new StyledTable(classModel);
        table.setCenterAlignedColumns(1, 2, 3, 4, 5, 6, 7);
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        content.add(chartsRow);
        content.add(tableCard);

        stateDeck = new StateDeckPanel(
                content,
                "Belum ada data absensi",
                "Catat absensi siswa terlebih dahulu atau ubah rentang tanggal.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        applyRangeBtn.addActionListener(e -> {
            useAllTime = false;
            refreshData();
        });
        allTimeBtn.addActionListener(e -> {
            useAllTime = true;
            refreshData();
        });
    }

    private ClayCard buildFilterBar() {
        ClayCard card = new ClayCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel lbl1 = new JLabel("Dari:");
        lbl1.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        JLabel lbl2 = new JLabel("Sampai:");
        lbl2.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        left.add(lbl1);
        left.add(startField);
        left.add(lbl2);
        left.add(endField);
        left.add(applyRangeBtn);
        left.add(allTimeBtn);
        card.add(left, BorderLayout.WEST);
        return card;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        LocalDate start = useAllTime ? LocalDate.of(2000, 1, 1) : startField.getLocalDate();
        LocalDate end = useAllTime ? LocalDate.of(2099, 12, 31) : endField.getLocalDate();

        AsyncTaskRunner.run(
                () -> {
                    AttendanceRangeReport range = container.getReportService().getAttendanceSummaryByDateRange(start, end);
                    List<ClassAttendanceReport> byClass = useAllTime
                            ? container.getReportService().getAttendanceSummaryByClass()
                            : container.getReportService().getAttendanceSummaryByClass(start, end);
                    return new Object[]{range, byClass};
                },
                () -> stateDeck.showLoading("Menyusun laporan absensi..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    AttendanceRangeReport range = (AttendanceRangeReport) arr[0];
                    @SuppressWarnings("unchecked")
                    List<ClassAttendanceReport> byClass = (List<ClassAttendanceReport>) arr[1];

                    if (range.getTotalRecords() == 0 && byClass.isEmpty()) {
                        stateDeck.showEmpty();
                        return;
                    }

                    rangeKpiLabel.setText(range.hasSufficientData()
                            ? (useAllTime ? "Keseluruhan: " : "Rentang: ") + range.getAttendancePercentage() + "%"
                            : "Belum ada catatan pada periode ini");

                    List<BotanicalCharts.ChartItem> donutItems = List.of(
                            new BotanicalCharts.ChartItem("Hadir", range.getHadirCount(), BotanicalTheme.EVERGREEN),
                            new BotanicalCharts.ChartItem("Sakit", range.getSakitCount(), BotanicalTheme.WARNING),
                            new BotanicalCharts.ChartItem("Izin", range.getIzinCount(), BotanicalTheme.SAGE),
                            new BotanicalCharts.ChartItem("Alpa", range.getAlpaCount(), BotanicalTheme.DANGER)
                    );
                    statusDonut.setData(donutItems, "Catatan");

                    List<BotanicalCharts.ChartItem> barItems = new ArrayList<>();
                    classModel.setRowCount(0);
                    for (ClassAttendanceReport r : byClass) {
                        String pctStr = r.hasSufficientData() ? r.getAttendancePercentage() + "%" : "-";
                        classModel.addRow(new Object[]{
                                r.getClassName(),
                                "Kelas " + r.getGradeLevel(),
                                r.getTotalRecords(),
                                r.getHadirCount(),
                                r.getSakitCount(),
                                r.getIzinCount(),
                                r.getAlpaCount(),
                                pctStr
                        });
                        double pct = r.hasSufficientData() ? r.getAttendancePercentage().doubleValue() : 0;
                        Color c = pct >= 85 ? BotanicalTheme.EVERGREEN : (pct >= 70 ? BotanicalTheme.WARNING : BotanicalTheme.DANGER);
                        barItems.add(new BotanicalCharts.ChartItem(r.getClassName(), pct, c));
                    }
                    classBar.setData(barItems, "%", 100);
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }
}

