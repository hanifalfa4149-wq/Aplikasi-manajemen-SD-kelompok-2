package id.ac.sekolah.manajemenkelas.presentation.swing.module.operational;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.DateField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FormField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.IconButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ModalFormDialog;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SearchField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SelectItem;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Layar Operasional Penilaian: membuat, memfilter, memperbarui agenda penilaian
 * serta melompat langsung ke pengisian Nilai Siswa.
 */
public class AssessmentPanel extends JPanel implements ScreenPage {

    private static final String[] ASSESSMENT_TYPES = {
            Assessment.TYPE_KUIS, Assessment.TYPE_TUGAS, Assessment.TYPE_UTS,
            Assessment.TYPE_UAS, Assessment.TYPE_PRAKTIK, Assessment.TYPE_PROYEK, Assessment.TYPE_LAINNYA
    };

    private final ApplicationContainer container;
    private final Consumer<Long> onOpenScoresForAssessment;

    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari judul penilaian...");
    private final JComboBox<SelectItem<Long>> taFilter = new JComboBox<>();
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final PrimaryButton addButton = new PrimaryButton("Buat Penilaian", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah penilaian terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton scoresButton = new IconButton("Kelola Nilai", BotanicalIcons.get(BotanicalIcons.Type.SCORES, 15, BotanicalTheme.SUCCESS), "Buka daftar nilai siswa untuk penilaian ini", IconButton.Tone.SUCCESS);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Judul Penilaian", "Jenis", "Kelas & Mata Pelajaran", "Tanggal", "Nilai Maks.", "Deskripsi"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<TeachingAssignment> scopedActiveAssignments = List.of();
    private Map<Long, String> assignmentLabels = Map.of();
    private List<Assessment> allAssessments = List.of();
    private List<Assessment> filteredAssessments = List.of();
    private boolean updatingFilters = false;

    public AssessmentPanel(ApplicationContainer container, Consumer<Long> onOpenScoresForAssessment) {
        this.container = container;
        this.onOpenScoresForAssessment = onOpenScoresForAssessment;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        typeFilter.addItem("Semua Jenis");
        for (String t : ASSESSMENT_TYPES) {
            typeFilter.addItem(t);
        }

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(feedbackBanner, BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        ClayCard tableCard = new ClayCard(new BorderLayout());
        table.setStatusColumns(2);
        table.setCenterAlignedColumns(0, 4);
        table.setRightAlignedColumns(5);
        table.setColumnWidths(52, 200, 94, 220, 100, 88, 180);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada data penilaian",
                "Buat agenda penilaian baru (Kuis, Tugas, UTS, UAS, Praktik, Proyek) sesuai penugasan mengajar.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        taFilter.addActionListener(e -> {
            if (!updatingFilters) {
                applyFilterAndRender();
            }
        });
        typeFilter.addActionListener(e -> {
            if (!updatingFilters) {
                applyFilterAndRender();
            }
        });
        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());
        scoresButton.addActionListener(e -> {
            Assessment selected = getSelected();
            if (selected != null && this.onOpenScoresForAssessment != null) {
                this.onOpenScoresForAssessment.accept(selected.getId());
            }
        });
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(10, 8));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        left.setOpaque(false);
        searchField.setPreferredSize(new Dimension(210, BotanicalTheme.INPUT_HEIGHT));
        FormField.styleInputComponent(taFilter);
        taFilter.setPreferredSize(new Dimension(250, BotanicalTheme.INPUT_HEIGHT));
        FormField.styleInputComponent(typeFilter);
        typeFilter.setPreferredSize(new Dimension(135, BotanicalTheme.INPUT_HEIGHT));
        left.add(searchField);
        left.add(taFilter);
        left.add(typeFilter);
        toolbar.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        right.setOpaque(false);
        right.add(scoresButton);
        right.add(editButton);
        right.add(addButton);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> {
                    List<Assessment> assessments = container.getAssessmentService().getAllAssessments();
                    SessionContext session = container.getSessionContext();
                    List<TeachingAssignment> allTas = session.hasRole(RoleMapper.ROLE_ADMIN)
                            ? container.getAcademicService().getAllTeachingAssignments()
                            : container.getAcademicService().getTeachingAssignmentsByTeacher(
                                    session.resolveTeacherScopeOrNullForAdmin("penilaian"));

                    Map<Long, String> cMap = new HashMap<>();
                    for (SchoolClass c : container.getAcademicService().getAllClasses()) {
                        cMap.put(c.getId(), c.getName());
                    }
                    Map<Long, String> subMap = new HashMap<>();
                    for (Subject s : container.getAcademicService().getAllSubjects()) {
                        subMap.put(s.getId(), s.getName());
                    }
                    Map<Long, String> semMap = new HashMap<>();
                    for (Semester s : container.getAcademicService().getAllSemesters()) {
                        semMap.put(s.getId(), s.getName());
                    }
                    Map<Long, String> labels = new HashMap<>();
                    for (TeachingAssignment ta : allTas) {
                        String lbl = cMap.getOrDefault(ta.getClassId(), "Kelas #" + ta.getClassId())
                                + " • " + subMap.getOrDefault(ta.getSubjectId(), "Mapel #" + ta.getSubjectId())
                                + " (" + semMap.getOrDefault(ta.getSemesterId(), "Sem #" + ta.getSemesterId()) + ")";
                        labels.put(ta.getId(), lbl);
                    }
                    return new Object[]{assessments, allTas, labels};
                },
                () -> stateDeck.showLoading("Memuat data penilaian..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<Assessment> assessments = (List<Assessment>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<TeachingAssignment> allTas = (List<TeachingAssignment>) arr[1];
                    @SuppressWarnings("unchecked")
                    Map<Long, String> labels = (Map<Long, String>) arr[2];

                    this.allAssessments = assessments;
                    this.scopedActiveAssignments = allTas.stream().filter(TeachingAssignment::isActive).toList();
                    this.assignmentLabels = labels;

                    updatingFilters = true;
                    taFilter.removeAllItems();
                    taFilter.addItem(new SelectItem<>(null, "Semua Penugasan Mengajar"));
                    for (TeachingAssignment ta : scopedActiveAssignments) {
                        taFilter.addItem(new SelectItem<>(ta.getId(), labels.getOrDefault(ta.getId(), "#" + ta.getId())));
                    }
                    updatingFilters = false;

                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        @SuppressWarnings("unchecked")
        SelectItem<Long> taItem = (SelectItem<Long>) taFilter.getSelectedItem();
        Long filterTaId = taItem != null ? taItem.getValue() : null;
        String filterType = (String) typeFilter.getSelectedItem();

        List<Assessment> matched = new ArrayList<>();
        for (Assessment a : allAssessments) {
            if (filterTaId != null && !filterTaId.equals(a.getTeachingAssignmentId())) {
                continue;
            }
            if (filterType != null && !"Semua Jenis".equals(filterType)
                    && !filterType.equalsIgnoreCase(a.getAssessmentType())) {
                continue;
            }
            if (kw.isEmpty() || a.getTitle().toLowerCase().contains(kw)) {
                matched.add(a);
            }
        }
        this.filteredAssessments = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (Assessment a : matched) {
            tableModel.addRow(new Object[]{
                    a.getId(),
                    a.getTitle(),
                    a.getAssessmentType(),
                    assignmentLabels.getOrDefault(a.getTeachingAssignmentId(), "Penugasan #" + a.getTeachingAssignmentId()),
                    String.valueOf(a.getAssessmentDate()),
                    a.getMaxScore(),
                    a.getDescription() == null || a.getDescription().isBlank() ? "-" : a.getDescription()
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private Assessment getSelected() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredAssessments.size()) ? filteredAssessments.get(row) : null;
    }

    private void updateButtons() {
        Assessment sel = getSelected();
        addButton.setEnabled(!scopedActiveAssignments.isEmpty());
        editButton.setEnabled(sel != null);
        scoresButton.setEnabled(sel != null);
    }

    private void onAdd() {
        AssessmentDialog dialog = new AssessmentDialog(this, container, scopedActiveAssignments, assignmentLabels, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Penilaian baru berhasil dibuat.");
            refreshData();
        }
    }

    private void onEdit() {
        Assessment sel = getSelected();
        if (sel == null) {
            return;
        }
        AssessmentDialog dialog = new AssessmentDialog(this, container, scopedActiveAssignments, assignmentLabels, sel);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Penilaian \"" + sel.getTitle() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private static final class AssessmentDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Assessment editing;
        private final JComboBox<SelectItem<TeachingAssignment>> taCombo = new JComboBox<>();
        private final JTextField titleInput = new JTextField();
        private final JComboBox<String> typeCombo = new JComboBox<>(ASSESSMENT_TYPES);
        private final DateField dateInput = new DateField(LocalDate.now());
        private final JTextField maxScoreInput = new JTextField("100");
        private final JTextArea descInput = new JTextArea(3, 20);
        private final FormField titleField = fullWidth(new FormField("Judul Penilaian", titleInput, true, "Contoh: Kuis Pecahan Sederhana"));
        private final FormField maxScoreField = new FormField("Nilai Maksimal", maxScoreInput, true);

        private AssessmentDialog(
                Component owner,
                ApplicationContainer container,
                List<TeachingAssignment> activeTas,
                Map<Long, String> labels,
                Assessment editing) {
            super(owner,
                    editing == null ? "Buat Penilaian Baru" : "Ubah Data Penilaian",
                    editing == null ? "Simpan Penilaian" : "Perbarui Penilaian",
                    590,
                    500);
            this.container = container;
            this.editing = editing;

            for (TeachingAssignment ta : activeTas) {
                taCombo.addItem(new SelectItem<>(ta, labels.getOrDefault(ta.getId(), "Penugasan #" + ta.getId())));
            }

            addSectionCard(
                    "Rincian Penilaian",
                    fullWidth(new FormField("Penugasan Mengajar", taCombo, true)),
                    titleField,
                    new FormField("Jenis Penilaian", typeCombo, true),
                    new FormField("Tanggal Penilaian", dateInput, true),
                    maxScoreField,
                    fullWidth(new FormField("Deskripsi", FormField.wrapTextArea(descInput, 68), false)));

            if (editing != null) {
                taCombo.setEnabled(false);
                for (int i = 0; i < taCombo.getItemCount(); i++) {
                    if (taCombo.getItemAt(i).getValue().getId().equals(editing.getTeachingAssignmentId())) {
                        taCombo.setSelectedIndex(i);
                        break;
                    }
                }
                titleInput.setText(editing.getTitle());
                typeCombo.setSelectedItem(editing.getAssessmentType());
                dateInput.setLocalDate(editing.getAssessmentDate());
                maxScoreInput.setText(editing.getMaxScore().toPlainString());
                descInput.setText(editing.getDescription());
            }
        }

        @Override
        protected void onSubmit() {
            titleField.clearError();
            maxScoreField.clearError();
            String title = titleInput.getText().trim();
            if (title.isEmpty()) {
                titleField.setError("Judul penilaian wajib diisi.");
                return;
            }
            BigDecimal maxScore;
            try {
                maxScore = new BigDecimal(maxScoreInput.getText().trim());
            } catch (NumberFormatException ex) {
                maxScoreField.setError("Nilai maksimal harus berupa angka.");
                return;
            }

            @SuppressWarnings("unchecked")
            SelectItem<TeachingAssignment> taItem = (SelectItem<TeachingAssignment>) taCombo.getSelectedItem();
            if (editing == null && taItem == null) {
                feedbackBanner.showError("Penugasan mengajar wajib dipilih.");
                return;
            }

            String type = (String) typeCombo.getSelectedItem();
            LocalDate date = dateInput.getLocalDate();
            String desc = descInput.getText().trim();

            executeSave(() -> {
                if (editing == null) {
                    return container.getAssessmentService().createAssessment(
                            taItem.getValue().getId(), title, type, date, maxScore, desc);
                }
                return container.getAssessmentService().updateAssessment(
                        editing.getId(), title, type, date, maxScore, desc);
            }, null);
        }
    }
}

