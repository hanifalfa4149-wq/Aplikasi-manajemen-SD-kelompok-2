package id.ac.sekolah.manajemenkelas.presentation.swing.module.academic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
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
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

/**
 * Layar manajemen Penugasan Wali Kelas: penetapan wali kelas aktif & pengakhiran tugas.
 */
public class HomeroomPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari nama kelas atau nama guru...");
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Semua Status", "Aktif", "Selesai / Nonaktif"});
    private final PrimaryButton assignButton = new PrimaryButton("Tetapkan Wali Kelas", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton endButton = new IconButton("Akhiri Tugas", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Akhiri penugasan wali kelas aktif", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Kelas", "Guru Wali Kelas", "Tgl Mulai", "Tgl Selesai", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<SchoolClass> activeClasses = List.of();
    private List<Teacher> activeTeachers = List.of();
    private Map<Long, String> classNames = Map.of();
    private Map<Long, String> teacherNames = Map.of();
    private List<HomeroomAssignment> allAssignments = List.of();
    private List<HomeroomAssignment> filteredAssignments = List.of();

    public HomeroomPanel(ApplicationContainer container) {
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
        table.setCenterAlignedColumns(0, 3, 4);
        table.setColumnWidths(56, 150, 230, 118, 118, 100);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada penugasan wali kelas",
                "Tetapkan guru aktif sebagai wali kelas untuk suatu rombongan belajar.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        statusFilter.addActionListener(e -> applyFilterAndRender());
        assignButton.addActionListener(e -> onAssign());
        endButton.addActionListener(e -> onEndAssignment());
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(searchField);
        FormField.styleInputComponent(statusFilter);
        statusFilter.setPreferredSize(new Dimension(168, BotanicalTheme.INPUT_HEIGHT));
        left.add(statusFilter);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(endButton);
        right.add(assignButton);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> {
                    List<SchoolClass> classes = container.getAcademicService().getAllClasses();
                    List<Teacher> teachers = container.getTeacherService().getAllTeachers();
                    List<HomeroomAssignment> assignments = container.getAcademicService().getAllHomeroomAssignments();
                    return new Object[]{classes, teachers, assignments};
                },
                () -> stateDeck.showLoading("Memuat data wali kelas..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<SchoolClass> classes = (List<SchoolClass>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<Teacher> teachers = (List<Teacher>) arr[1];
                    @SuppressWarnings("unchecked")
                    List<HomeroomAssignment> assignments = (List<HomeroomAssignment>) arr[2];

                    List<SchoolClass> daftarKelasAktif = new ArrayList<>();
                    for (SchoolClass kelas : classes) {
                        if (kelas.isActive()) {
                            daftarKelasAktif.add(kelas);
                        }
                    }
                    this.activeClasses = daftarKelasAktif;

                    List<Teacher> daftarGuruAktif = new ArrayList<>();
                    for (Teacher guru : teachers) {
                        if (guru.isActive()) {
                            daftarGuruAktif.add(guru);
                        }
                    }
                    this.activeTeachers = daftarGuruAktif;

                    Map<Long, String> cMap = new HashMap<>();
                    for (SchoolClass c : classes) {
                        cMap.put(c.getId(), c.getName() + " (Tingkat " + c.getGradeLevel() + ")");
                    }
                    this.classNames = cMap;

                    Map<Long, String> tMap = new HashMap<>();
                    for (Teacher t : teachers) {
                        tMap.put(t.getId(), t.getFullName() + " (" + t.getNip() + ")");
                    }
                    this.teacherNames = tMap;
                    this.allAssignments = assignments;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        String mode = (String) statusFilter.getSelectedItem();

        List<HomeroomAssignment> matched = new ArrayList<>();
        for (HomeroomAssignment h : allAssignments) {
            String cName = classNames.getOrDefault(h.getClassId(), "Kelas #" + h.getClassId());
            String tName = teacherNames.getOrDefault(h.getTeacherId(), "Guru #" + h.getTeacherId());
            boolean matchKw = kw.isEmpty() || cName.toLowerCase().contains(kw) || tName.toLowerCase().contains(kw);
            boolean matchStatus = "Semua Status".equals(mode)
                    || ("Aktif".equals(mode) && h.isActive())
                    || (!"Aktif".equals(mode) && !h.isActive());
            if (matchKw && matchStatus) {
                matched.add(h);
            }
        }
        this.filteredAssignments = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (HomeroomAssignment h : matched) {
            tableModel.addRow(new Object[]{
                    h.getId(),
                    classNames.getOrDefault(h.getClassId(), "Kelas #" + h.getClassId()),
                    teacherNames.getOrDefault(h.getTeacherId(), "Guru #" + h.getTeacherId()),
                    String.valueOf(h.getStartDate()),
                    h.getEndDate() == null ? "-" : String.valueOf(h.getEndDate()),
                    h.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private HomeroomAssignment getSelected() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredAssignments.size()) ? filteredAssignments.get(row) : null;
    }

    private void updateButtons() {
        HomeroomAssignment s = getSelected();
        assignButton.setEnabled(!activeClasses.isEmpty() && !activeTeachers.isEmpty());
        endButton.setEnabled(s != null && s.isActive());
    }

    private void onAssign() {
        AssignHomeroomDialog dialog = new AssignHomeroomDialog(this, container, activeClasses, activeTeachers);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Penugasan wali kelas berhasil disimpan.");
            refreshData();
        }
    }

    private void onEndAssignment() {
        HomeroomAssignment selected = getSelected();
        if (selected == null || !selected.isActive()) {
            return;
        }
        EndHomeroomDialog dialog = new EndHomeroomDialog(this, container, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Penugasan wali kelas berhasil diakhiri.");
            refreshData();
        }
    }

    private static final class AssignHomeroomDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final JComboBox<SelectItem<SchoolClass>> classCombo = new JComboBox<>();
        private final JComboBox<SelectItem<Teacher>> teacherCombo = new JComboBox<>();
        private final DateField startDateInput = new DateField(LocalDate.now());
        private final FormField classField = fullWidth(new FormField("Pilih Kelas Aktif", classCombo, true));
        private final FormField teacherField = fullWidth(new FormField("Pilih Guru Wali Kelas", teacherCombo, true));
        private final FormField dateField = fullWidth(new FormField("Tanggal Mulai Menjabat", startDateInput, true));

        private AssignHomeroomDialog(Component owner, ApplicationContainer container, List<SchoolClass> classes, List<Teacher> teachers) {
            super(owner, "Tetapkan Wali Kelas", "Simpan Penugasan", 520, 420);
            this.container = container;
            for (SchoolClass c : classes) {
                classCombo.addItem(new SelectItem<>(c, c.getName() + " (Tingkat " + c.getGradeLevel() + ")"));
            }
            for (Teacher t : teachers) {
                teacherCombo.addItem(new SelectItem<>(t, t.getFullName() + " - " + t.getNip()));
            }
            addSectionCard("Penugasan Wali Kelas", classField, teacherField, dateField);
        }

        @Override
        protected void onSubmit() {
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> cItem = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            @SuppressWarnings("unchecked")
            SelectItem<Teacher> tItem = (SelectItem<Teacher>) teacherCombo.getSelectedItem();
            if (cItem == null || tItem == null) {
                feedbackBanner.showError("Kelas dan guru wajib dipilih.");
                return;
            }
            LocalDate startDate = startDateInput.getLocalDate();
            executeSave(() -> container.getAcademicService().assignHomeroomTeacher(
                    cItem.getValue().getId(), tItem.getValue().getId(), startDate), null);
        }
    }

    private static final class EndHomeroomDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final HomeroomAssignment assignment;
        private final DateField endDateInput = new DateField(LocalDate.now());
        private final FormField endDateField = fullWidth(new FormField("Tanggal Selesai Penugasan", endDateInput, true));

        private EndHomeroomDialog(Component owner, ApplicationContainer container, HomeroomAssignment assignment) {
            super(owner, "Akhiri Penugasan Wali Kelas", "Akhiri Penugasan", 460, 290);
            this.container = container;
            this.assignment = assignment;
            addSectionCard("Tanggal Pengakhiran (Mulai: " + assignment.getStartDate() + ")", endDateField);
        }

        @Override
        protected void onSubmit() {
            LocalDate endDate = endDateInput.getLocalDate();
            executeSave(() -> container.getAcademicService().endHomeroomAssignment(assignment.getId(), endDate), null);
        }
    }
}

