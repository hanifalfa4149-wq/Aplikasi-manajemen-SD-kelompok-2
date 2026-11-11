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
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
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

/**
 * Layar CRUD Data Kelas (tingkat 1-6, kapasitas, okupansi siswa aktif).
 */
public class SchoolClassPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari nama kelas...");
    private final JComboBox<SelectItem<Long>> yearFilter = new JComboBox<>();
    private final PrimaryButton addButton = new PrimaryButton("Tambah Kelas", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah data kelas terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan kelas terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Tahun Ajaran", "Nama Kelas", "Tingkat", "Terisi / Kapasitas", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<AcademicYear> years = List.of();
    private List<SchoolClass> allClasses = List.of();
    private List<SchoolClass> filteredClasses = List.of();
    private Map<Long, String> yearNames = Map.of();
    private Map<Long, Integer> activeOccupancy = Map.of();
    private boolean updatingFilter = false;

    public SchoolClassPanel(ApplicationContainer container) {
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
        table.setColumnWidths(56, 150, 150, 90, 140, 100);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada data kelas",
                "Buat kelas baru pada tahun ajaran aktif (contoh: 1A, 2A).");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        yearFilter.addActionListener(e -> {
            if (!updatingFilter) {
                applyFilterAndRender();
            }
        });
        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());
        deactivateButton.addActionListener(e -> onDeactivate());
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(searchField);
        FormField.styleInputComponent(yearFilter);
        yearFilter.setPreferredSize(new Dimension(210, BotanicalTheme.INPUT_HEIGHT));
        left.add(yearFilter);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(editButton);
        right.add(deactivateButton);
        right.add(addButton);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> {
                    List<AcademicYear> yList = container.getAcademicService().getAllAcademicYears();
                    List<SchoolClass> cList = container.getAcademicService().getAllClasses();
                    Map<Long, Integer> occ = new HashMap<>();
                    for (StudentPlacement p : container.getAcademicService().getAllStudentPlacements()) {
                        if (p.isActive()) {
                            occ.merge(p.getClassId(), 1, Integer::sum);
                        }
                    }
                    return new Object[]{yList, cList, occ};
                },
                () -> stateDeck.showLoading("Memuat data kelas..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<AcademicYear> yList = (List<AcademicYear>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<SchoolClass> cList = (List<SchoolClass>) arr[1];
                    @SuppressWarnings("unchecked")
                    Map<Long, Integer> occ = (Map<Long, Integer>) arr[2];
                    this.years = yList;
                    this.allClasses = cList;
                    this.activeOccupancy = occ;

                    Map<Long, String> yMap = new HashMap<>();
                    updatingFilter = true;
                    yearFilter.removeAllItems();
                    yearFilter.addItem(new SelectItem<>(null, "Semua Tahun Ajaran"));
                    for (AcademicYear y : years) {
                        yMap.put(y.getId(), y.getName());
                        yearFilter.addItem(new SelectItem<>(y.getId(), y.getName()));
                    }
                    this.yearNames = yMap;
                    updatingFilter = false;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        @SuppressWarnings("unchecked")
        SelectItem<Long> selectedYear = (SelectItem<Long>) yearFilter.getSelectedItem();
        Long filterYearId = selectedYear != null ? selectedYear.getValue() : null;

        List<SchoolClass> matched = new ArrayList<>();
        for (SchoolClass c : allClasses) {
            boolean matchesKw = kw.isEmpty() || (c.getName() != null && c.getName().toLowerCase().contains(kw));
            boolean matchesYear = filterYearId == null || filterYearId.equals(c.getAcademicYearId());
            if (matchesKw && matchesYear) {
                matched.add(c);
            }
        }
        this.filteredClasses = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (SchoolClass c : matched) {
            int filled = activeOccupancy.getOrDefault(c.getId(), 0);
            tableModel.addRow(new Object[]{
                    c.getId(),
                    yearNames.getOrDefault(c.getAcademicYearId(), "TA #" + c.getAcademicYearId()),
                    c.getName(),
                    "Kelas " + c.getGradeLevel(),
                    filled + " / " + c.getCapacity(),
                    c.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private SchoolClass getSelectedClass() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredClasses.size()) ? filteredClasses.get(row) : null;
    }

    private void updateButtons() {
        SchoolClass s = getSelectedClass();
        boolean hasActiveYear = years.stream().anyMatch(AcademicYear::isActive);
        addButton.setEnabled(hasActiveYear);
        editButton.setEnabled(s != null);
        deactivateButton.setEnabled(s != null && s.isActive());
    }

    private void onAdd() {
        List<AcademicYear> activeYears = years.stream().filter(AcademicYear::isActive).toList();
        if (activeYears.isEmpty()) {
            feedbackBanner.showWarning("Diperlukan minimal satu Tahun Ajaran aktif untuk membuat kelas.");
            return;
        }
        ClassDialog dialog = new ClassDialog(this, container, activeYears, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Kelas baru berhasil ditambahkan.");
            refreshData();
        }
    }

    private void onEdit() {
        SchoolClass selected = getSelectedClass();
        if (selected == null) {
            return;
        }
        List<AcademicYear> activeYears = years.stream().filter(AcademicYear::isActive).toList();
        if (activeYears.isEmpty()) {
            feedbackBanner.showWarning("Tahun ajaran untuk kelas harus berstatus aktif agar dapat diperbarui.");
            return;
        }
        ClassDialog dialog = new ClassDialog(this, container, activeYears, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Data kelas \"" + selected.getName() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private void onDeactivate() {
        SchoolClass selected = getSelectedClass();
        if (selected == null || !selected.isActive()) {
            return;
        }
        if (!ConfirmationDialog.showConfirm(this, "Nonaktifkan Kelas",
                "Nonaktifkan kelas \"" + selected.getName() + "\"?", "Ya, Nonaktifkan", true)) {
            return;
        }
        AsyncTaskRunner.run(
                () -> {
                    container.getAcademicService().deactivateClass(selected.getId());
                    return null;
                },
                null,
                unused -> {
                    feedbackBanner.showSuccess("Kelas \"" + selected.getName() + "\" berhasil dinonaktifkan.");
                    refreshData();
                },
                error -> feedbackBanner.showError(error));
    }

    private static final class ClassDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final SchoolClass editing;
        private final JComboBox<SelectItem<AcademicYear>> yearCombo = new JComboBox<>();
        private final JTextField nameInput = new JTextField();
        private final JComboBox<Integer> gradeCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
        private final JSpinner capacitySpinner = new JSpinner(new SpinnerNumberModel(28, 1, 100, 1));
        private final FormField yearField = new FormField("Tahun Ajaran Aktif", yearCombo, true);
        private final FormField nameField = new FormField("Nama Kelas", nameInput, true, "Contoh: 1A, 3B");
        private final FormField gradeField = new FormField("Tingkat Kelas (SD 1-6)", gradeCombo, true);
        private final FormField capField = new FormField("Kapasitas Maksimal", capacitySpinner, true);

        private ClassDialog(Component owner, ApplicationContainer container, List<AcademicYear> activeYears, SchoolClass editing) {
            super(owner,
                    editing == null ? "Tambah Kelas" : "Edit Kelas",
                    editing == null ? "Simpan Kelas" : "Perbarui Kelas",
                    540,
                    380);
            this.container = container;
            this.editing = editing;

            for (AcademicYear y : activeYears) {
                yearCombo.addItem(new SelectItem<>(y, y.getName()));
            }
            capacitySpinner.setPreferredSize(new Dimension(220, BotanicalTheme.INPUT_HEIGHT));

            addSectionCard("Informasi Kelas", yearField, nameField, gradeField, capField);

            if (editing != null) {
                for (int i = 0; i < yearCombo.getItemCount(); i++) {
                    if (yearCombo.getItemAt(i).getValue().getId().equals(editing.getAcademicYearId())) {
                        yearCombo.setSelectedIndex(i);
                        break;
                    }
                }
                nameInput.setText(editing.getName());
                gradeCombo.setSelectedItem(editing.getGradeLevel());
                capacitySpinner.setValue(editing.getCapacity());
            }
        }

        @Override
        protected void onSubmit() {
            nameField.clearError();
            @SuppressWarnings("unchecked")
            SelectItem<AcademicYear> yItem = (SelectItem<AcademicYear>) yearCombo.getSelectedItem();
            if (yItem == null) {
                feedbackBanner.showError("Pilih tahun ajaran aktif.");
                return;
            }
            String name = nameInput.getText().trim();
            if (name.isEmpty()) {
                nameField.setError("Nama kelas wajib diisi.");
                return;
            }
            Long yearId = yItem.getValue().getId();
            int grade = (Integer) gradeCombo.getSelectedItem();
            int cap = (Integer) capacitySpinner.getValue();

            executeSave(() -> {
                if (editing == null) {
                    return container.getAcademicService().createClass(yearId, name, grade, cap);
                }
                return container.getAcademicService().updateClass(editing.getId(), yearId, name, grade, cap);
            }, null);
        }
    }
}

