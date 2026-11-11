package id.ac.sekolah.manajemenkelas.presentation.swing.module.report;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;

/**
 * Layar Laporan Nilai: ringkasan penilaian, rata-rata mata pelajaran, dan capaian nilai siswa.
 */
public class ScoreReportPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final BotanicalCharts.BarChartPanel subjectChart = new BotanicalCharts.BarChartPanel();

    private final DefaultTableModel assessmentModel = new DefaultTableModel(
            new String[]{"Judul Penilaian", "Kelas", "Mapel", "Jenis", "Tanggal", "Dinilai", "Rata-rata", "Tertinggi", "Terendah", "%"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel subjectModel = new DefaultTableModel(
            new String[]{"Kode", "Mata Pelajaran", "Jml Penilaian", "Entri Nilai", "Rata-rata Skor", "Rata-rata (%)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel studentModel = new DefaultTableModel(
            new String[]{"NIS", "Nama Siswa", "Kelas", "Jml Penilaian", "Total Skor", "Total Maks.", "Rata-rata", "Capaian (%)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StateDeckPanel stateDeck;

    public ScoreReportPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel layoutPanel = new JPanel(new BorderLayout(0, 16));
        layoutPanel.setOpaque(false);

        ClayCard chartCard = new ClayCard(new BorderLayout(0, 8));
        chartCard.setPreferredSize(new Dimension(700, 230));
        JLabel chartTitle = new JLabel("Rata-rata Persentase Nilai per Mata Pelajaran");
        chartTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        chartCard.add(chartTitle, BorderLayout.NORTH);
        chartCard.add(subjectChart, BorderLayout.CENTER);
        layoutPanel.add(chartCard, BorderLayout.NORTH);

        ClayCard tabsCard = new ClayCard(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BotanicalTheme.FONT_BODY_BOLD);

        StyledTable assTable = new StyledTable(assessmentModel);
        assTable.setStatusColumns(3);
        assTable.setRightAlignedColumns(5, 6, 7, 8, 9);
        tabs.addTab("Ringkasan per Penilaian", assTable.wrapInScrollPane());

        StyledTable subTable = new StyledTable(subjectModel);
        subTable.setRightAlignedColumns(2, 3, 4, 5);
        tabs.addTab("Rata-rata per Mata Pelajaran", subTable.wrapInScrollPane());

        StyledTable stuTable = new StyledTable(studentModel);
        stuTable.setRightAlignedColumns(3, 4, 5, 6, 7);
        tabs.addTab("Capaian Nilai Siswa", stuTable.wrapInScrollPane());

        tabsCard.add(tabs, BorderLayout.CENTER);
        layoutPanel.add(tabsCard, BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                layoutPanel,
                "Belum ada data penilaian dan nilai",
                "Tambahkan agenda penilaian dan nilai siswa untuk melihat analisis statistik nilai.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        AsyncTaskRunner.run(
                () -> new Object[]{
                        container.getReportService().getAssessmentSummaries(),
                        container.getReportService().getAverageScoresBySubject(),
                        container.getReportService().getStudentScoreSummaries()
                },
                () -> stateDeck.showLoading("Menyusun laporan nilai..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<AssessmentReport> assessments = (List<AssessmentReport>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<SubjectScoreReport> subjects = (List<SubjectScoreReport>) arr[1];
                    @SuppressWarnings("unchecked")
                    List<StudentScoreReport> students = (List<StudentScoreReport>) arr[2];

                    if (assessments.isEmpty() && subjects.isEmpty() && students.isEmpty()) {
                        stateDeck.showEmpty();
                        return;
                    }

                    assessmentModel.setRowCount(0);
                    for (AssessmentReport a : assessments) {
                        assessmentModel.addRow(new Object[]{
                                a.getTitle(),
                                a.getClassName(),
                                a.getSubjectName(),
                                a.getAssessmentType(),
                                String.valueOf(a.getAssessmentDate()),
                                a.getScoredStudentsCount(),
                                a.hasScores() ? a.getAverageScore() : "-",
                                a.hasScores() ? a.getHighestScore() : "-",
                                a.hasScores() ? a.getLowestScore() : "-",
                                a.hasScores() ? a.getAveragePercentage() + "%" : "-"
                        });
                    }

                    List<BotanicalCharts.ChartItem> chartItems = new ArrayList<>();
                    subjectModel.setRowCount(0);
                    for (SubjectScoreReport s : subjects) {
                        subjectModel.addRow(new Object[]{
                                s.getSubjectCode(),
                                s.getSubjectName(),
                                s.getAssessmentCount(),
                                s.getScoredEntriesCount(),
                                s.hasScores() ? s.getAverageScore() : "-",
                                s.hasScores() ? s.getAveragePercentage() + "%" : "-"
                        });
                        double val = s.hasScores() ? s.getAveragePercentage().doubleValue() : 0;
                        chartItems.add(new BotanicalCharts.ChartItem(s.getSubjectName(), val, BotanicalTheme.EVERGREEN));
                    }
                    subjectChart.setData(chartItems, "%", 100);

                    studentModel.setRowCount(0);
                    for (StudentScoreReport r : students) {
                        studentModel.addRow(new Object[]{
                                r.getNis(),
                                r.getStudentName(),
                                r.getCurrentClassName() == null ? "-" : r.getCurrentClassName(),
                                r.getAssessmentCount(),
                                r.hasScores() ? r.getTotalEarnedScore() : "-",
                                r.hasScores() ? r.getTotalMaxScore() : "-",
                                r.hasScores() ? r.getAverageScore() : "-",
                                r.hasScores() ? r.getOverallPercentage() + "%" : "-"
                        });
                    }
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }
}

