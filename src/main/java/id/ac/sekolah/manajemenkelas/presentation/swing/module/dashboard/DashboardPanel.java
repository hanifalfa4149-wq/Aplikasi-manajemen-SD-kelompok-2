package id.ac.sekolah.manajemenkelas.presentation.swing.module.dashboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.BotanicalCharts;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SecondaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StatusBadge;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.NavRoute;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Halaman Dashboard ringkasan dengan kartu statistik Claymorphism, grafik Java2D berbasis data nyata,
 * aktivitas absensi hari ini, penilaian terbaru, serta pintasan cepat sesuai peran.
 */
public class DashboardPanel extends JPanel implements ScreenPage {

    private record DashboardPayload(
            DashboardSummary summary,
            List<ClassStudentCountReport> classCounts,
            List<GenderStudentCountReport> genderCounts,
            List<ClassAttendanceReport> classAttendance,
            List<Attendance> todayAttendance,
            List<Assessment> recentAssessments,
            Map<Long, String> studentNames,
            Map<Long, String> classNames) {
    }

    private final ApplicationContainer container;
    private final Consumer<NavRoute> navigateTo;

    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final JLabel welcomeTitle = new JLabel("Selamat datang");
    private final JLabel academicBannerInfo = new JLabel("-");
    private final StatusBadge scopeBadge = new StatusBadge("-", StatusBadge.Style.MINT);
    private final JPanel quickActionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

    private final MetricCard studentsCard = new MetricCard("Total Siswa Aktif", BotanicalIcons.Type.STUDENTS);
    private final MetricCard teachersCard = new MetricCard("Total Guru Aktif", BotanicalIcons.Type.TEACHERS);
    private final MetricCard classesCard = new MetricCard("Total Kelas Aktif", BotanicalIcons.Type.CLASSROOM);
    private final MetricCard attendanceCard = new MetricCard("Kehadiran Keseluruhan", BotanicalIcons.Type.ATTENDANCE);

    private final BotanicalCharts.BarChartPanel studentPerClassChart = new BotanicalCharts.BarChartPanel();
    private final BotanicalCharts.BarChartPanel attendancePerClassChart = new BotanicalCharts.BarChartPanel();
    private final JLabel genderSummaryLabel = new JLabel("Distribusi gender: -");

    private final DefaultTableModel attendanceModel = createReadOnlyModel(new String[]{"Siswa", "Kelas", "Status", "Catatan"});
    private final StyledTable attendanceTable = new StyledTable(attendanceModel);
    private final DefaultTableModel assessmentModel = createReadOnlyModel(new String[]{"Judul Penilaian", "Jenis", "Tanggal", "Maks."});
    private final StyledTable assessmentTable = new StyledTable(assessmentModel);

    private final JPanel scrollContent = new JPanel();
    private final StateDeckPanel stateDeck;

    public DashboardPanel(ApplicationContainer container, Consumer<NavRoute> navigateTo) {
        this.container = container;
        this.navigateTo = navigateTo;

        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        add(feedbackBanner, BorderLayout.NORTH);

        scrollContent.setOpaque(false);
        scrollContent.setLayout(new BoxLayout(scrollContent, BoxLayout.Y_AXIS));
        scrollContent.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        scrollContent.add(buildWelcomeCard());
        scrollContent.add(Box.createVerticalStrut(BotanicalTheme.SECTION_GAP));
        scrollContent.add(buildMetricsRow());
        scrollContent.add(Box.createVerticalStrut(BotanicalTheme.SECTION_GAP));
        scrollContent.add(buildChartsRow());
        scrollContent.add(Box.createVerticalStrut(BotanicalTheme.SECTION_GAP));
        scrollContent.add(buildActivityRow());

        JScrollPane scroll = new JScrollPane(scrollContent);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        stateDeck = new StateDeckPanel(
                scroll,
                "Ringkasan belum tersedia",
                "Pastikan database dan profil akun Anda telah siap.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);
    }

    private JPanel buildWelcomeCard() {
        ClayCard card = new ClayCard(new BorderLayout(16, 10));
        card.setSurfaceColor(BotanicalTheme.SOFT_SAGE);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));

        JPanel left = new JPanel(new GridLayout(0, 1, 0, 6));
        left.setOpaque(false);

        JPanel topTags = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        topTags.setOpaque(false);
        topTags.add(scopeBadge);
        academicBannerInfo.setFont(BotanicalTheme.FONT_SMALL_BOLD);
        academicBannerInfo.setForeground(BotanicalTheme.EVERGREEN);
        topTags.add(academicBannerInfo);

        welcomeTitle.setFont(BotanicalTheme.FONT_TITLE);
        welcomeTitle.setForeground(BotanicalTheme.TEXT_PRIMARY);

        left.add(topTags);
        left.add(welcomeTitle);

        quickActionsPanel.setOpaque(false);
        card.add(left, BorderLayout.CENTER);
        card.add(quickActionsPanel, BorderLayout.EAST);
        return card;
    }

    private JPanel buildMetricsRow() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 126));
        grid.add(studentsCard);
        grid.add(teachersCard);
        grid.add(classesCard);
        grid.add(attendanceCard);
        return grid;
    }

    private JPanel buildChartsRow() {
        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 290));

        ClayCard leftCard = new ClayCard(new BorderLayout(0, 8));
        JPanel leftHead = new JPanel(new BorderLayout());
        leftHead.setOpaque(false);
        JLabel leftTitle = new JLabel("Sebaran Siswa per Kelas");
        leftTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        leftHead.add(leftTitle, BorderLayout.WEST);
        genderSummaryLabel.setFont(BotanicalTheme.FONT_SMALL);
        genderSummaryLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);
        leftHead.add(genderSummaryLabel, BorderLayout.EAST);
        leftCard.add(leftHead, BorderLayout.NORTH);
        leftCard.add(studentPerClassChart, BorderLayout.CENTER);

        ClayCard rightCard = new ClayCard(new BorderLayout(0, 8));
        JLabel rightTitle = new JLabel("Persentase Kehadiran per Kelas");
        rightTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        rightCard.add(rightTitle, BorderLayout.NORTH);
        rightCard.add(attendancePerClassChart, BorderLayout.CENTER);

        grid.add(leftCard);
        grid.add(rightCard);
        return grid;
    }

    private JPanel buildActivityRow() {
        JPanel grid = new JPanel(new GridLayout(1, 2, 16, 0));
        grid.setOpaque(false);
        grid.setPreferredSize(new Dimension(760, 280));

        ClayCard attendanceCardPanel = new ClayCard(new BorderLayout(0, 10));
        JPanel attHead = new JPanel(new BorderLayout());
        attHead.setOpaque(false);
        JLabel attTitle = new JLabel("Aktivitas Absensi Hari Ini (" + LocalDate.now() + ")");
        attTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        SecondaryButton openAtt = new SecondaryButton("Lihat Absensi");
        openAtt.setPreferredSize(new Dimension(120, 30));
        openAtt.addActionListener(e -> navigateTo.accept(NavRoute.ATTENDANCE));
        attHead.add(attTitle, BorderLayout.WEST);
        attHead.add(openAtt, BorderLayout.EAST);
        attendanceCardPanel.add(attHead, BorderLayout.NORTH);
        attendanceTable.setStatusColumns(2);
        attendanceCardPanel.add(attendanceTable.wrapInScrollPane(), BorderLayout.CENTER);

        ClayCard assessmentCardPanel = new ClayCard(new BorderLayout(0, 10));
        JPanel assHead = new JPanel(new BorderLayout());
        assHead.setOpaque(false);
        JLabel assTitle = new JLabel("Penilaian Akademik Terbaru");
        assTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        SecondaryButton openAss = new SecondaryButton("Kelola Penilaian");
        openAss.setPreferredSize(new Dimension(138, 30));
        openAss.addActionListener(e -> navigateTo.accept(NavRoute.ASSESSMENTS));
        assHead.add(assTitle, BorderLayout.WEST);
        assHead.add(openAss, BorderLayout.EAST);
        assessmentCardPanel.add(assHead, BorderLayout.NORTH);
        assessmentTable.setStatusColumns(1);
        assessmentTable.setRightAlignedColumns(3);
        assessmentCardPanel.add(assessmentTable.wrapInScrollPane(), BorderLayout.CENTER);

        grid.add(attendanceCardPanel);
        grid.add(assessmentCardPanel);
        return grid;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        rebuildQuickActions();

        AsyncTaskRunner.run(
                this::loadPayload,
                () -> stateDeck.showLoading("Memuat ringkasan dashboard..."),
                payload -> {
                    applyPayload(payload);
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private DashboardPayload loadPayload() {
        DashboardSummary summary = container.getDashboardService().getDashboardSummary();
        List<ClassStudentCountReport> classCounts = container.getReportService().getStudentCountsByClass();
        List<GenderStudentCountReport> genderCounts = container.getReportService().getStudentCountsByGender();
        List<ClassAttendanceReport> classAttendance = container.getReportService().getAttendanceSummaryByClass();
        List<Attendance> todayAttendance = container.getAttendanceService().getAttendanceByDate(LocalDate.now());
        List<Assessment> assessments = container.getAssessmentService().getAllAssessments().stream()
                .sorted(Comparator.comparing(Assessment::getAssessmentDate).reversed()
                        .thenComparing(Assessment::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .toList();

        Map<Long, String> studentNames = new HashMap<>();
        for (Student s : container.getStudentService().getAllStudents()) {
            studentNames.put(s.getId(), s.getFullName());
        }
        Map<Long, String> classNames = new HashMap<>();
        for (SchoolClass c : container.getAcademicService().getAllClasses()) {
            classNames.put(c.getId(), c.getName());
        }

        return new DashboardPayload(
                summary, classCounts, genderCounts, classAttendance,
                todayAttendance, assessments, studentNames, classNames);
    }

    private void applyPayload(DashboardPayload payload) {
        DashboardSummary s = payload.summary();
        String fullName = container.getSessionContext().getFullName();
        welcomeTitle.setText("Selamat datang, " + (fullName == null ? "Pengguna" : fullName));
        scopeBadge.setBadge(s.getScopeLabel() != null ? s.getScopeLabel().toUpperCase() : "DASHBOARD", StatusBadge.Style.MINT);

        String yearInfo = s.getActiveAcademicYearName() == null || s.getActiveAcademicYearName().isBlank()
                ? "Belum ada tahun ajaran aktif"
                : "Tahun Ajaran Aktif: " + s.getActiveAcademicYearName();
        academicBannerInfo.setText(yearInfo + "  •  " + s.getActiveSemestersCount() + " Semester Aktif");
        welcomeTitle.revalidate();
        academicBannerInfo.revalidate();
        scopeBadge.revalidate();
        scrollContent.revalidate();

        studentsCard.setValue(
                String.valueOf(s.getTotalActiveStudents()),
                "Nonaktif: " + s.getTotalInactiveStudents() + " siswa");
        teachersCard.setValue(
                String.valueOf(s.getTotalActiveTeachers()),
                "Mapel aktif: " + s.getTotalActiveSubjects());
        classesCard.setValue(
                String.valueOf(s.getTotalActiveClasses()),
                "Wali kelas: " + s.getActiveHomeroomCount() + " • Mengajar: " + s.getActiveTeachingAssignmentCount());
        attendanceCard.setValue(
                s.hasAttendanceData() ? s.getOverallAttendancePercentage() + "%" : "-",
                "Total catatan: " + s.getTotalAttendanceRecords() + " • Penilaian: " + s.getTotalAssessmentsCount());

        // Chart 1: Siswa per Kelas
        List<BotanicalCharts.ChartItem> classItems = new ArrayList<>();
        Color[] palette = {BotanicalTheme.EVERGREEN, BotanicalTheme.SUCCESS, BotanicalTheme.SAGE, BotanicalTheme.DEEP_FOREST};
        for (int i = 0; i < payload.classCounts().size(); i++) {
            ClassStudentCountReport r = payload.classCounts().get(i);
            classItems.add(new BotanicalCharts.ChartItem(
                    r.getClassName(),
                    r.getActiveStudentCount(),
                    palette[i % palette.length]));
        }
        studentPerClassChart.setData(classItems, " siswa", 0);

        StringBuilder genderText = new StringBuilder();
        for (GenderStudentCountReport g : payload.genderCounts()) {
            if (!genderText.isEmpty()) {
                genderText.append("  |  ");
            }
            genderText.append(g.getGenderLabel()).append(": ").append(g.getActiveStudentCount());
        }
        genderSummaryLabel.setText(genderText.isEmpty() ? "Belum ada data siswa" : genderText.toString());

        // Chart 2: Kehadiran per Kelas
        List<BotanicalCharts.ChartItem> attItems = new ArrayList<>();
        for (ClassAttendanceReport r : payload.classAttendance()) {
            double pct = r.hasSufficientData() ? r.getAttendancePercentage().doubleValue() : 0;
            Color c = pct >= 85 ? BotanicalTheme.EVERGREEN : (pct >= 70 ? BotanicalTheme.WARNING : BotanicalTheme.DANGER);
            attItems.add(new BotanicalCharts.ChartItem(r.getClassName(), pct, c));
        }
        attendancePerClassChart.setData(attItems, "%", 100);

        // Tabel Absensi Hari Ini
        attendanceModel.setRowCount(0);
        for (Attendance a : payload.todayAttendance()) {
            attendanceModel.addRow(new Object[]{
                    payload.studentNames().getOrDefault(a.getStudentId(), "Siswa #" + a.getStudentId()),
                    payload.classNames().getOrDefault(a.getClassId(), "Kelas #" + a.getClassId()),
                    a.getStatus(),
                    a.getNotes() == null || a.getNotes().isBlank() ? "-" : a.getNotes()
            });
        }

        // Tabel Penilaian Terbaru
        assessmentModel.setRowCount(0);
        for (Assessment a : payload.recentAssessments()) {
            assessmentModel.addRow(new Object[]{
                    a.getTitle(),
                    a.getAssessmentType(),
                    String.valueOf(a.getAssessmentDate()),
                    a.getMaxScore()
            });
        }
    }

    private void rebuildQuickActions() {
        quickActionsPanel.removeAll();
        boolean isAdmin = RoleMapper.ROLE_ADMIN.equalsIgnoreCase(container.getSessionContext().getRole());

        PrimaryButton absBtn = new PrimaryButton("Catat Absensi", BotanicalIcons.get(BotanicalIcons.Type.ATTENDANCE, 15, BotanicalTheme.WHITE));
        absBtn.addActionListener(e -> navigateTo.accept(NavRoute.ATTENDANCE));
        quickActionsPanel.add(absBtn);

        if (isAdmin) {
            SecondaryButton studentBtn = new SecondaryButton("Data Siswa");
            studentBtn.addActionListener(e -> navigateTo.accept(NavRoute.STUDENTS));
            quickActionsPanel.add(studentBtn);

            SecondaryButton placeBtn = new SecondaryButton("Penempatan");
            placeBtn.addActionListener(e -> navigateTo.accept(NavRoute.STUDENT_PLACEMENTS));
            quickActionsPanel.add(placeBtn);
        } else {
            SecondaryButton scoreBtn = new SecondaryButton("Input Nilai");
            scoreBtn.addActionListener(e -> navigateTo.accept(NavRoute.STUDENT_SCORES));
            quickActionsPanel.add(scoreBtn);
        }

        quickActionsPanel.revalidate();
        quickActionsPanel.repaint();
    }

    private static DefaultTableModel createReadOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private static final class MetricCard extends ClayCard {

        private final JLabel valueLabel = new JLabel("-");
        private final JLabel footerLabel = new JLabel("-");

        private MetricCard(String title, BotanicalIcons.Type iconType) {
            setLayout(new BorderLayout(0, 8));

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            JLabel titleLbl = new JLabel(title);
            titleLbl.setFont(BotanicalTheme.FONT_SMALL_BOLD);
            titleLbl.setForeground(BotanicalTheme.TEXT_SECONDARY);
            JLabel iconLbl = new JLabel(BotanicalIcons.get(iconType, 18, BotanicalTheme.EVERGREEN));
            top.add(titleLbl, BorderLayout.WEST);
            top.add(iconLbl, BorderLayout.EAST);

            valueLabel.setFont(BotanicalTheme.FONT_METRIC);
            valueLabel.setForeground(BotanicalTheme.TEXT_PRIMARY);

            footerLabel.setFont(BotanicalTheme.FONT_SMALL);
            footerLabel.setForeground(BotanicalTheme.TEXT_SECONDARY);

            add(top, BorderLayout.NORTH);
            add(valueLabel, BorderLayout.CENTER);
            add(footerLabel, BorderLayout.SOUTH);
        }

        private void setValue(String metric, String footer) {
            valueLabel.setText(metric);
            footerLabel.setText(footer);
        }
    }
}

