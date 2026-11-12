package id.ac.sekolah.manajemenkelas.presentation.swing.module.operational;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.AssessmentSummary;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScoreSummary;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FormField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.IconButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ModalFormDialog;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SecondaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SelectItem;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Layar Operasional Nilai Siswa: input nilai massal per penilaian, input tunggal,
 * ubah nilai, dan ringkasan capaian per penilaian maupun per siswa.
 */
public class StudentScoresPanel extends JPanel implements ScreenPage {

    private enum Mode {
        BY_ASSESSMENT("Per Penilaian"),
        BY_STUDENT("Riwayat per Siswa");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final JComboBox<Mode> modeCombo = new JComboBox<>(Mode.values());
    private final JComboBox<SelectItem<Assessment>> assessmentCombo = new JComboBox<>();
    private final JComboBox<SelectItem<Student>> studentCombo = new JComboBox<>();
    private final SecondaryButton loadButton = new SecondaryButton("Tampilkan");

    private final PrimaryButton batchButton = new PrimaryButton("Input Nilai Kelas", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton singleButton = new IconButton("Input Tunggal", BotanicalIcons.get(BotanicalIcons.Type.SCORES, 15, BotanicalTheme.EVERGREEN), "Catat nilai satu siswa", IconButton.Tone.NEUTRAL);
    private final IconButton editButton = new IconButton("Ubah Nilai", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah nilai siswa terpilih", IconButton.Tone.NEUTRAL);

    private final JLabel stat1 = new JLabel("-");
    private final JLabel stat2 = new JLabel("-");
    private final JLabel stat3 = new JLabel("-");
    private final JLabel stat4 = new JLabel("-");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID Nilai", "Judul / Siswa", "Detail", "Nilai", "Keterangan"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<Assessment> allAssessments = List.of();
    private List<Student> activeStudents = List.of();
    private Map<Long, Student> studentMap = Map.of();
    private Map<Long, Assessment> assessmentMap = Map.of();
    private Map<Long, TeachingAssignment> taMap = Map.of();
    private List<StudentScore> currentScores = List.of();
    private Long pendingAssessmentId;
    private boolean initializedMeta = false;

    public StudentScoresPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        feedbackBanner.setAlignmentX(LEFT_ALIGNMENT);
        top.add(feedbackBanner);
        top.add(Box.createVerticalStrut(6));

        ClayCard toolbar = buildToolbar();
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        top.add(toolbar);
        top.add(Box.createVerticalStrut(10));

        ClayCard strip = buildSummaryStrip();
        strip.setAlignmentX(LEFT_ALIGNMENT);
        top.add(strip);
        add(top, BorderLayout.NORTH);

        ClayCard tableCard = new ClayCard(new BorderLayout());
        table.setCenterAlignedColumns(0);
        table.setRightAlignedColumns(3);
        table.setColumnWidths(72, 230, 160, 96, 260);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada nilai tercatat",
                "Belum ada nilai untuk penilaian atau siswa ini. Klik Input Nilai Kelas untuk mengisi skor.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        modeCombo.addActionListener(e -> syncModeVisibility());
        loadButton.addActionListener(e -> loadScores());
        batchButton.addActionListener(e -> onBatchScore());
        singleButton.addActionListener(e -> onSingleScore());
        editButton.addActionListener(e -> onEditScore());
        syncModeVisibility();
        updateButtons();
    }

    public void focusAssessment(Long assessmentId) {
        this.pendingAssessmentId = assessmentId;
        modeCombo.setSelectedItem(Mode.BY_ASSESSMENT);
        syncModeVisibility();
    }

    private ClayCard buildToolbar() {
        ClayCard card = new ClayCard(new BorderLayout(10, 8));
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        FormField.styleInputComponent(modeCombo);
        modeCombo.setPreferredSize(new Dimension(165, BotanicalTheme.INPUT_HEIGHT));
        FormField.styleInputComponent(assessmentCombo);
        assessmentCombo.setPreferredSize(new Dimension(320, BotanicalTheme.INPUT_HEIGHT));
        FormField.styleInputComponent(studentCombo);
        studentCombo.setPreferredSize(new Dimension(280, BotanicalTheme.INPUT_HEIGHT));

        filters.add(modeCombo);
        filters.add(assessmentCombo);
        filters.add(studentCombo);
        filters.add(loadButton);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(singleButton);
        actions.add(batchButton);

        card.add(filters, BorderLayout.CENTER);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private ClayCard buildSummaryStrip() {
        ClayCard card = new ClayCard(new GridLayout(1, 4, 12, 0));
        card.setSurfaceColor(BotanicalTheme.PALE_MINT);
        for (JLabel lbl : new JLabel[]{stat1, stat2, stat3, stat4}) {
            lbl.setFont(BotanicalTheme.FONT_BODY_BOLD);
            lbl.setForeground(BotanicalTheme.TEXT_PRIMARY);
            card.add(lbl);
        }
        return card;
    }

    private void syncModeVisibility() {
        Mode mode = (Mode) modeCombo.getSelectedItem();
        assessmentCombo.setVisible(mode == Mode.BY_ASSESSMENT);
        studentCombo.setVisible(mode == Mode.BY_STUDENT);
        revalidate();
        repaint();
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> {
                    SessionContext session = container.getSessionContext();
                    List<Assessment> assessments = container.getAssessmentService().getAllAssessments();
                    List<Student> allActiveStudents = new ArrayList<>();
                    for (Student siswa : container.getStudentService().getAllStudents()) {
                        if (siswa.isActive()) {
                            allActiveStudents.add(siswa);
                        }
                    }
                    List<TeachingAssignment> tas = container.getAcademicService().getAllTeachingAssignments();

                    List<Student> scopedStudents;
                    if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
                        scopedStudents = allActiveStudents;
                    } else {
                        Long idGuru = session.resolveTeacherScopeOrNullForAdmin("nilai siswa");
                        Set<Long> idKelasMengajar = new HashSet<>();
                        for (TeachingAssignment ta : tas) {
                            if (ta.isActive() && idGuru.equals(ta.getTeacherId())) {
                                idKelasMengajar.add(ta.getClassId());
                            }
                        }
                        Set<Long> idSiswaCakupan = new HashSet<>();
                        for (Long idKelas : idKelasMengajar) {
                            for (StudentPlacement p : container.getAcademicService().getActivePlacementsByClass(idKelas)) {
                                idSiswaCakupan.add(p.getStudentId());
                            }
                        }
                        scopedStudents = new ArrayList<>();
                        for (Student siswa : allActiveStudents) {
                            if (idSiswaCakupan.contains(siswa.getId())) {
                                scopedStudents.add(siswa);
                            }
                        }
                    }
                    return new Object[]{assessments, allActiveStudents, scopedStudents, tas};
                },
                () -> stateDeck.showLoading("Memuat data penilaian dan siswa..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<Assessment> assessments = (List<Assessment>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<Student> allActiveStudents = (List<Student>) arr[1];
                    @SuppressWarnings("unchecked")
                    List<Student> scopedStudents = (List<Student>) arr[2];
                    @SuppressWarnings("unchecked")
                    List<TeachingAssignment> tas = (List<TeachingAssignment>) arr[3];

                    this.allAssessments = assessments;
                    this.activeStudents = scopedStudents;
                    Map<Long, Student> sMap = new HashMap<>();
                    for (Student s : allActiveStudents) {
                        sMap.put(s.getId(), s);
                    }
                    this.studentMap = sMap;

                    Map<Long, Assessment> aMap = new HashMap<>();
                    for (Assessment a : assessments) {
                        aMap.put(a.getId(), a);
                    }
                    this.assessmentMap = aMap;

                    Map<Long, TeachingAssignment> tMap = new HashMap<>();
                    for (TeachingAssignment t : tas) {
                        tMap.put(t.getId(), t);
                    }
                    this.taMap = tMap;

                    assessmentCombo.removeAllItems();
                    for (Assessment a : assessments) {
                        assessmentCombo.addItem(new SelectItem<>(a,
                                a.getTitle() + " [" + a.getAssessmentType() + "] (Maks: " + a.getMaxScore() + ")"));
                    }
                    if (pendingAssessmentId != null) {
                        for (int i = 0; i < assessmentCombo.getItemCount(); i++) {
                            if (assessmentCombo.getItemAt(i).getValue().getId().equals(pendingAssessmentId)) {
                                assessmentCombo.setSelectedIndex(i);
                                break;
                            }
                        }
                        pendingAssessmentId = null;
                    }

                    studentCombo.removeAllItems();
                    for (Student s : scopedStudents) {
                        studentCombo.addItem(new SelectItem<>(s, s.getNis() + " - " + s.getFullName()));
                    }

                    initializedMeta = true;
                    updateButtons();
                    loadScores();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void loadScores() {
        if (!initializedMeta) {
            refreshData();
            return;
        }
        Mode mode = (Mode) modeCombo.getSelectedItem();
        if (mode == Mode.BY_ASSESSMENT && allAssessments.isEmpty()) {
            currentScores = List.of();
            stat1.setText("Jumlah Dinilai: 0");
            stat2.setText("Rata-rata: -");
            stat3.setText("Tertinggi: -");
            stat4.setText("Terendah: -");
            stateDeck.showEmpty("Belum ada agenda penilaian", "Buat agenda penilaian terlebih dahulu pada menu Penilaian.");
            updateButtons();
            return;
        }
        if (mode == Mode.BY_STUDENT && activeStudents.isEmpty()) {
            currentScores = List.of();
            stat1.setText("Total Penilaian: 0");
            stat2.setText("Skor Total: -");
            stat3.setText("Rata-rata: -");
            stat4.setText("Persentase: -");
            stateDeck.showEmpty(
                    "Belum ada siswa dalam cakupan",
                    "Belum ada siswa aktif pada kelas yang dapat diakses untuk nilai ini.");
            updateButtons();
            return;
        }

        Assessment selectedAssessment = getSelectedAssessment();
        Student selectedStudent = getSelectedStudent();

        AsyncTaskRunner.run(
                () -> {
                    AssessmentService service = container.getAssessmentService();
                    if (mode == Mode.BY_ASSESSMENT) {
                        List<StudentScore> scores = service.getScoresByAssessment(selectedAssessment.getId());
                        AssessmentSummary summary = service.getAssessmentScoreSummary(selectedAssessment.getId());
                        return new Object[]{scores, summary, null};
                    } else {
                        List<StudentScore> scores = service.getScoresByStudent(selectedStudent.getId());
                        StudentScoreSummary summary = service.getStudentScoreSummary(selectedStudent.getId());
                        return new Object[]{scores, null, summary};
                    }
                },
                () -> stateDeck.showLoading("Memuat daftar nilai siswa..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<StudentScore> scores = (List<StudentScore>) arr[0];
                    AssessmentSummary aSum = (AssessmentSummary) arr[1];
                    StudentScoreSummary sSum = (StudentScoreSummary) arr[2];
                    this.currentScores = scores;

                    if (aSum != null) {
                        stat1.setText("Siswa Dinilai: " + aSum.getTotalStudentsScored());
                        stat2.setText("Rata-rata: " + aSum.getAverageScore() + " (" + aSum.getAveragePercentage() + "%)");
                        stat3.setText("Tertinggi: " + aSum.getHighestScore());
                        stat4.setText("Terendah: " + aSum.getLowestScore());
                    } else if (sSum != null) {
                        stat1.setText("Total Penilaian: " + sSum.getTotalAssessments());
                        stat2.setText("Skor Total: " + sSum.getTotalEarnedScore() + " / " + sSum.getTotalMaxScore());
                        stat3.setText("Rata-rata: " + sSum.getAverageScore());
                        stat4.setText("Persentase: " + sSum.getOverallPercentage() + "%");
                    }

                    if (scores.isEmpty()) {
                        stateDeck.showEmpty();
                        updateButtons();
                        return;
                    }
                    tableModel.setRowCount(0);
                    for (StudentScore sc : scores) {
                        if (mode == Mode.BY_ASSESSMENT) {
                            Student st = studentMap.get(sc.getStudentId());
                            tableModel.addRow(new Object[]{
                                    sc.getId(),
                                    st != null ? st.getFullName() : "Siswa #" + sc.getStudentId(),
                                    st != null ? "NIS: " + st.getNis() : "-",
                                    sc.getScore(),
                                    sc.getNotes() == null || sc.getNotes().isBlank() ? "-" : sc.getNotes()
                            });
                        } else {
                            Assessment as = assessmentMap.get(sc.getAssessmentId());
                            tableModel.addRow(new Object[]{
                                    sc.getId(),
                                    as != null ? as.getTitle() : "Penilaian #" + sc.getAssessmentId(),
                                    as != null ? as.getAssessmentType() + " (" + as.getAssessmentDate() + ")" : "-",
                                    sc.getScore(),
                                    sc.getNotes() == null || sc.getNotes().isBlank() ? "-" : sc.getNotes()
                            });
                        }
                    }
                    updateButtons();
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private Assessment getSelectedAssessment() {
        @SuppressWarnings("unchecked")
        SelectItem<Assessment> item = (SelectItem<Assessment>) assessmentCombo.getSelectedItem();
        return item != null ? item.getValue() : null;
    }

    private Student getSelectedStudent() {
        @SuppressWarnings("unchecked")
        SelectItem<Student> item = (SelectItem<Student>) studentCombo.getSelectedItem();
        return item != null ? item.getValue() : null;
    }

    private StudentScore getSelectedScore() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < currentScores.size()) ? currentScores.get(row) : null;
    }

    private void updateButtons() {
        boolean hasAssessments = !allAssessments.isEmpty();
        batchButton.setEnabled(hasAssessments);
        singleButton.setEnabled(hasAssessments);
        editButton.setEnabled(getSelectedScore() != null);
    }

    private void onBatchScore() {
        Assessment selected = getSelectedAssessment();
        if (selected == null) {
            return;
        }
        TeachingAssignment ta = taMap.get(selected.getTeachingAssignmentId());
        if (ta == null) {
            feedbackBanner.showError("Penugasan mengajar untuk penilaian ini tidak ditemukan.");
            return;
        }
        BatchScoreDialog dialog = new BatchScoreDialog(this, container, selected, ta.getClassId(), studentMap);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Nilai siswa berhasil disimpan.");
            loadScores();
        }
    }

    private void onSingleScore() {
        Assessment selected = getSelectedAssessment();
        if (selected == null) {
            return;
        }
        TeachingAssignment ta = taMap.get(selected.getTeachingAssignmentId());
        Long classId = ta != null ? ta.getClassId() : null;
        SingleScoreDialog dialog = new SingleScoreDialog(this, container, selected, classId, studentMap);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Nilai siswa berhasil dicatat.");
            loadScores();
        }
    }

    private void onEditScore() {
        StudentScore selected = getSelectedScore();
        if (selected == null) {
            return;
        }
        Assessment ass = assessmentMap.get(selected.getAssessmentId());
        BigDecimal maxScore = ass != null ? ass.getMaxScore() : new BigDecimal("100");
        Student st = studentMap.get(selected.getStudentId());
        String sName = st != null ? st.getFullName() : "Siswa #" + selected.getStudentId();
        EditScoreDialog dialog = new EditScoreDialog(this, container, selected, sName, maxScore);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Nilai \"" + sName + "\" berhasil diperbarui.");
            loadScores();
        }
    }

    private static final class BatchScoreDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Assessment assessment;
        private final DefaultTableModel model = new DefaultTableModel(
                new String[]{"ID Siswa", "NIS", "Nama Siswa", "Nilai (0 - Maks)", "Catatan"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4;
            }
        };
        private final StyledTable scoreTable = new StyledTable(model);

        private BatchScoreDialog(
                Component owner,
                ApplicationContainer container,
                Assessment assessment,
                Long classId,
                Map<Long, Student> studentMap) {
            super(owner, "Input Nilai Massal: " + assessment.getTitle(), "Simpan Semua Nilai", 680, 490);
            this.container = container;
            this.assessment = assessment;

            ClayCard box = new ClayCard(new BorderLayout(0, 8));
            box.setPreferredSize(new Dimension(610, 290));
            JLabel info = new JLabel("Masukkan nilai (0 s.d. " + assessment.getMaxScore() + ") langsung pada baris siswa yang belum dinilai:");
            info.setFont(BotanicalTheme.FONT_SMALL);
            info.setForeground(BotanicalTheme.TEXT_SECONDARY);
            box.add(info, BorderLayout.NORTH);
            scoreTable.setColumnWidths(68, 96, 210, 110, 160);
            box.add(scoreTable.wrapInScrollPane(), BorderLayout.CENTER);
            addSectionCard("Daftar Siswa Belum Dinilai", fullWidth(box));

            AsyncTaskRunner.run(
                    () -> {
                        List<StudentPlacement> placements = container.getAcademicService().getActivePlacementsByClass(classId);
                        Set<Long> scored = new HashSet<>();
                        for (StudentScore sc : container.getAssessmentService().getScoresByAssessment(assessment.getId())) {
                            scored.add(sc.getStudentId());
                        }
                        List<Student> list = new ArrayList<>();
                        for (StudentPlacement p : placements) {
                            if (!scored.contains(p.getStudentId())) {
                                Student st = studentMap.get(p.getStudentId());
                                if (st != null && st.isActive()) {
                                    list.add(st);
                                }
                            }
                        }
                        return list;
                    },
                    null,
                    students -> {
                        model.setRowCount(0);
                        for (Student st : students) {
                            model.addRow(new Object[]{st.getId(), st.getNis(), st.getFullName(), "", ""});
                        }
                        if (students.isEmpty()) {
                            feedbackBanner.showInfo("Seluruh siswa aktif pada kelas ini sudah memiliki nilai untuk penilaian ini.");
                        }
                    },
                    error -> feedbackBanner.showError(error));
        }

        @Override
        protected void onSubmit() {
            if (scoreTable.isEditing()) {
                scoreTable.getCellEditor().stopCellEditing();
            }
            List<AssessmentService.StudentScoreEntry> entries = new ArrayList<>();
            for (int r = 0; r < model.getRowCount(); r++) {
                String rawVal = String.valueOf(model.getValueAt(r, 3)).trim();
                if (rawVal.isEmpty()) {
                    continue;
                }
                BigDecimal val;
                try {
                    val = new BigDecimal(rawVal);
                } catch (NumberFormatException ex) {
                    feedbackBanner.showError("Nilai tidak valid pada baris ke-" + (r + 1) + ": \"" + rawVal + "\".");
                    return;
                }
                Long studentId = (Long) model.getValueAt(r, 0);
                String notes = String.valueOf(model.getValueAt(r, 4));
                entries.add(new AssessmentService.StudentScoreEntry(studentId, val, notes));
            }
            if (entries.isEmpty()) {
                feedbackBanner.showError("Isi minimal satu nilai siswa sebelum menyimpan.");
                return;
            }
            executeSave(() -> container.getAssessmentService().recordAssessmentScores(assessment.getId(), entries), null);
        }
    }

    private static final class SingleScoreDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Assessment assessment;
        private final JComboBox<SelectItem<Student>> studentCombo = new JComboBox<>();
        private final JTextField scoreInput = new JTextField();
        private final JTextField notesInput = new JTextField();
        private final FormField scoreField;

        private SingleScoreDialog(
                Component owner,
                ApplicationContainer container,
                Assessment assessment,
                Long classId,
                Map<Long, Student> studentMap) {
            super(owner, "Input Nilai Siswa", "Simpan Nilai", 520, 380);
            this.container = container;
            this.assessment = assessment;
            this.scoreField = new FormField("Nilai (0 s.d. " + assessment.getMaxScore() + ")", scoreInput, true);

            addSectionCard(
                    assessment.getTitle(),
                    fullWidth(new FormField("Siswa", studentCombo, true)),
                    scoreField,
                    new FormField("Catatan", notesInput, false));

            if (classId != null) {
                AsyncTaskRunner.run(
                        () -> container.getAcademicService().getActivePlacementsByClass(classId),
                        null,
                        placements -> {
                            studentCombo.removeAllItems();
                            for (StudentPlacement p : placements) {
                                Student s = studentMap.get(p.getStudentId());
                                if (s != null) {
                                    studentCombo.addItem(new SelectItem<>(s, s.getNis() + " - " + s.getFullName()));
                                }
                            }
                        },
                        error -> feedbackBanner.showError(error));
            }
        }

        @Override
        protected void onSubmit() {
            scoreField.clearError();
            @SuppressWarnings("unchecked")
            SelectItem<Student> sItem = (SelectItem<Student>) studentCombo.getSelectedItem();
            if (sItem == null) {
                feedbackBanner.showError("Pilih siswa terlebih dahulu.");
                return;
            }
            BigDecimal score;
            try {
                score = new BigDecimal(scoreInput.getText().trim());
            } catch (NumberFormatException ex) {
                scoreField.setError("Nilai harus berupa angka.");
                return;
            }
            executeSave(() -> container.getAssessmentService().recordScore(
                    assessment.getId(), sItem.getValue().getId(), score, notesInput.getText()), null);
        }
    }

    private static final class EditScoreDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final StudentScore editing;
        private final JTextField scoreInput = new JTextField();
        private final JTextField notesInput = new JTextField();
        private final FormField scoreField;

        private EditScoreDialog(Component owner, ApplicationContainer container, StudentScore editing, String studentName, BigDecimal maxScore) {
            super(owner, "Ubah Nilai Siswa", "Perbarui Nilai", 470, 330);
            this.container = container;
            this.editing = editing;
            this.scoreInput.setText(editing.getScore().toPlainString());
            this.notesInput.setText(editing.getNotes());
            this.scoreField = fullWidth(new FormField("Nilai Baru (0 s.d. " + maxScore + ")", scoreInput, true));
            addSectionCard(studentName, scoreField, fullWidth(new FormField("Catatan", notesInput, false)));
        }

        @Override
        protected void onSubmit() {
            scoreField.clearError();
            BigDecimal newScore;
            try {
                newScore = new BigDecimal(scoreInput.getText().trim());
            } catch (NumberFormatException ex) {
                scoreField.setError("Nilai harus berupa angka.");
                return;
            }
            executeSave(() -> container.getAssessmentService().updateScore(editing.getId(), newScore, notesInput.getText()), null);
        }
    }
}

