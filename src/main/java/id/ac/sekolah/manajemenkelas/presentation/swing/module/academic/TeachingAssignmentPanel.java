package id.ac.sekolah.manajemenkelas.presentation.swing.module.academic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ConfirmationDialog;
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
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

/**
 * Layar manajemen Penugasan Mengajar (Guru - Mata Pelajaran - Kelas - Semester).
 */
public class TeachingAssignmentPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari guru, mapel, atau kelas...");
    private final JComboBox<SelectItem<Long>> semesterFilter = new JComboBox<>();
    private final PrimaryButton addButton = new PrimaryButton("Tambah Penugasan", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan penugasan terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Guru Pengampu", "Kelas", "Mata Pelajaran", "Semester", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<Teacher> activeTeachers = List.of();
    private List<SchoolClass> activeClasses = List.of();
    private List<Subject> activeSubjects = List.of();
    private List<Semester> activeSemesters = List.of();
    private Map<Long, String> teacherNames = Map.of();
    private Map<Long, String> classNames = Map.of();
    private Map<Long, String> subjectNames = Map.of();
    private Map<Long, String> semesterNames = Map.of();
    private List<TeachingAssignment> allAssignments = List.of();
    private List<TeachingAssignment> filteredAssignments = List.of();
    private boolean updatingFilter = false;

    public TeachingAssignmentPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(feedbackBanner, BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        ClayCard tableCard = new ClayCard(new BorderLayout());
        table.setStatusColumns(5);
        table.setCenterAlignedColumns(0);
        table.setColumnWidths(54, 210, 110, 185, 165, 100);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada penugasan mengajar",
                "Buat penugasan mengajar untuk menghubungkan guru, kelas, mata pelajaran, dan semester.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        semesterFilter.addActionListener(e -> {
            if (!updatingFilter) {
                applyFilterAndRender();
            }
        });
        addButton.addActionListener(e -> onAdd());
        deactivateButton.addActionListener(e -> onDeactivate());
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(searchField);
        FormField.styleInputComponent(semesterFilter);
        semesterFilter.setPreferredSize(new Dimension(210, BotanicalTheme.INPUT_HEIGHT));
        left.add(semesterFilter);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(deactivateButton);
        right.add(addButton);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> new Object[]{
                        container.getTeacherService().getAllTeachers(),
                        container.getAcademicService().getAllClasses(),
                        container.getAcademicService().getAllSubjects(),
                        container.getAcademicService().getAllSemesters(),
                        container.getAcademicService().getAllTeachingAssignments()
                },
                () -> stateDeck.showLoading("Memuat data penugasan mengajar..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<Teacher> teachers = (List<Teacher>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<SchoolClass> classes = (List<SchoolClass>) arr[1];
                    @SuppressWarnings("unchecked")
                    List<Subject> subjects = (List<Subject>) arr[2];
                    @SuppressWarnings("unchecked")
                    List<Semester> semesters = (List<Semester>) arr[3];
                    @SuppressWarnings("unchecked")
                    List<TeachingAssignment> assignments = (List<TeachingAssignment>) arr[4];

                    List<Teacher> daftarGuruAktif = new ArrayList<>();
                    Map<Long, String> tMap = new HashMap<>();
                    for (Teacher t : teachers) {
                        if (t.isActive()) {
                            daftarGuruAktif.add(t);
                        }
                        tMap.put(t.getId(), t.getFullName());
                    }
                    this.activeTeachers = daftarGuruAktif;
                    this.teacherNames = tMap;

                    List<SchoolClass> daftarKelasAktif = new ArrayList<>();
                    Map<Long, String> cMap = new HashMap<>();
                    for (SchoolClass c : classes) {
                        if (c.isActive()) {
                            daftarKelasAktif.add(c);
                        }
                        cMap.put(c.getId(), c.getName());
                    }
                    this.activeClasses = daftarKelasAktif;
                    this.classNames = cMap;

                    List<Subject> daftarMapelAktif = new ArrayList<>();
                    Map<Long, String> subMap = new HashMap<>();
                    for (Subject s : subjects) {
                        if (s.isActive()) {
                            daftarMapelAktif.add(s);
                        }
                        subMap.put(s.getId(), s.getCode() + " - " + s.getName());
                    }
                    this.activeSubjects = daftarMapelAktif;
                    this.subjectNames = subMap;

                    List<Semester> daftarSemesterAktif = new ArrayList<>();
                    for (Semester s : semesters) {
                        if (s.isActive()) {
                            daftarSemesterAktif.add(s);
                        }
                    }
                    this.activeSemesters = daftarSemesterAktif;

                    Map<Long, String> semMap = new HashMap<>();
                    updatingFilter = true;
                    semesterFilter.removeAllItems();
                    semesterFilter.addItem(new SelectItem<>(null, "Semua Semester"));
                    for (Semester sem : semesters) {
                        semMap.put(sem.getId(), sem.getName());
                        semesterFilter.addItem(new SelectItem<>(sem.getId(), sem.getName()));
                    }
                    this.semesterNames = semMap;
                    updatingFilter = false;

                    this.allAssignments = assignments;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        @SuppressWarnings("unchecked")
        SelectItem<Long> semSel = (SelectItem<Long>) semesterFilter.getSelectedItem();
        Long semId = semSel != null ? semSel.getValue() : null;

        List<TeachingAssignment> matched = new ArrayList<>();
        for (TeachingAssignment a : allAssignments) {
            if (semId != null && !semId.equals(a.getSemesterId())) {
                continue;
            }
            String t = teacherNames.getOrDefault(a.getTeacherId(), "");
            String c = classNames.getOrDefault(a.getClassId(), "");
            String s = subjectNames.getOrDefault(a.getSubjectId(), "");
            if (kw.isEmpty() || t.toLowerCase().contains(kw) || c.toLowerCase().contains(kw) || s.toLowerCase().contains(kw)) {
                matched.add(a);
            }
        }
        this.filteredAssignments = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (TeachingAssignment a : matched) {
            tableModel.addRow(new Object[]{
                    a.getId(),
                    teacherNames.getOrDefault(a.getTeacherId(), "Guru #" + a.getTeacherId()),
                    classNames.getOrDefault(a.getClassId(), "Kelas #" + a.getClassId()),
                    subjectNames.getOrDefault(a.getSubjectId(), "Mapel #" + a.getSubjectId()),
                    semesterNames.getOrDefault(a.getSemesterId(), "Semester #" + a.getSemesterId()),
                    a.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private TeachingAssignment getSelected() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredAssignments.size()) ? filteredAssignments.get(row) : null;
    }

    private void updateButtons() {
        TeachingAssignment sel = getSelected();
        boolean canCreate = !activeTeachers.isEmpty() && !activeClasses.isEmpty()
                && !activeSubjects.isEmpty() && !activeSemesters.isEmpty();
        addButton.setEnabled(canCreate);
        deactivateButton.setEnabled(sel != null && sel.isActive());
    }

    private void onAdd() {
        CreateTeachingDialog dialog = new CreateTeachingDialog(
                this, container, activeTeachers, activeClasses, activeSubjects, activeSemesters);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Penugasan mengajar berhasil dibuat.");
            refreshData();
        }
    }

    private void onDeactivate() {
        TeachingAssignment selected = getSelected();
        if (selected == null || !selected.isActive()) {
            return;
        }
        if (!ConfirmationDialog.showConfirm(this, "Nonaktifkan Penugasan Mengajar",
                "Nonaktifkan penugasan mengajar ID #" + selected.getId() + "?", "Ya, Nonaktifkan", true)) {
            return;
        }
        AsyncTaskRunner.run(
                () -> {
                    container.getAcademicService().deactivateTeachingAssignment(selected.getId());
                    return null;
                },
                null,
                unused -> {
                    feedbackBanner.showSuccess("Penugasan mengajar berhasil dinonaktifkan.");
                    refreshData();
                },
                error -> feedbackBanner.showError(error));
    }

    private static final class CreateTeachingDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final JComboBox<SelectItem<Teacher>> teacherCombo = new JComboBox<>();
        private final JComboBox<SelectItem<SchoolClass>> classCombo = new JComboBox<>();
        private final JComboBox<SelectItem<Subject>> subjectCombo = new JComboBox<>();
        private final JComboBox<SelectItem<Semester>> semesterCombo = new JComboBox<>();

        private CreateTeachingDialog(
                Component owner,
                ApplicationContainer container,
                List<Teacher> teachers,
                List<SchoolClass> classes,
                List<Subject> subjects,
                List<Semester> semesters) {
            super(owner, "Tambah Penugasan Mengajar", "Simpan Penugasan", 580, 420);
            this.container = container;

            for (Teacher t : teachers) {
                teacherCombo.addItem(new SelectItem<>(t, t.getFullName() + " (" + t.getNip() + ")"));
            }
            for (SchoolClass c : classes) {
                classCombo.addItem(new SelectItem<>(c, c.getName() + " (Tingkat " + c.getGradeLevel() + ")"));
            }
            for (Subject s : subjects) {
                subjectCombo.addItem(new SelectItem<>(s, s.getCode() + " - " + s.getName()));
            }
            for (Semester s : semesters) {
                semesterCombo.addItem(new SelectItem<>(s, s.getName()));
            }

            addSectionCard(
                    "Rincian Penugasan Mengajar",
                    new FormField("Guru Pengampu", teacherCombo, true),
                    new FormField("Kelas", classCombo, true),
                    new FormField("Mata Pelajaran", subjectCombo, true),
                    new FormField("Semester", semesterCombo, true, "Harus satu tahun ajaran dengan kelas"));
        }

        @Override
        protected void onSubmit() {
            @SuppressWarnings("unchecked")
            SelectItem<Teacher> t = (SelectItem<Teacher>) teacherCombo.getSelectedItem();
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> c = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            @SuppressWarnings("unchecked")
            SelectItem<Subject> s = (SelectItem<Subject>) subjectCombo.getSelectedItem();
            @SuppressWarnings("unchecked")
            SelectItem<Semester> sem = (SelectItem<Semester>) semesterCombo.getSelectedItem();
            if (t == null || c == null || s == null || sem == null) {
                feedbackBanner.showError("Seluruh kolom penugasan wajib dipilih.");
                return;
            }
            executeSave(() -> container.getAcademicService().createTeachingAssignment(
                    t.getValue().getId(), c.getValue().getId(), s.getValue().getId(), sem.getValue().getId()), null);
        }
    }
}

