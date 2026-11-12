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
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ConfirmationDialog;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.DateField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FormField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.IconButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ModalFormDialog;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.PrimaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SelectItem;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Layar manajemen Semester yang terhubung ke Tahun Ajaran.
 */
public class SemesterPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final JComboBox<SelectItem<Long>> yearFilter = new JComboBox<>();
    private final PrimaryButton addButton = new PrimaryButton("Tambah Semester", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah semester terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton activateButton = new IconButton("Aktifkan", BotanicalIcons.get(BotanicalIcons.Type.CHECK, 15, BotanicalTheme.SUCCESS), "Aktifkan semester terpilih", IconButton.Tone.SUCCESS);
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan semester terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Tahun Ajaran", "No.", "Nama Semester", "Tanggal Mulai", "Tanggal Selesai", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<AcademicYear> years = List.of();
    private List<Semester> allSemesters = List.of();
    private List<Semester> filteredSemesters = List.of();
    private Map<Long, String> yearNames = Map.of();
    private boolean updatingFilter = false;

    public SemesterPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(feedbackBanner, BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        ClayCard tableCard = new ClayCard(new BorderLayout());
        table.setStatusColumns(6);
        table.setCenterAlignedColumns(0, 2, 4, 5);
        table.setColumnWidths(54, 140, 50, 200, 118, 118, 100);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada data semester",
                "Tambahkan semester ganjil atau genap yang berada di dalam rentang tahun ajaran.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        yearFilter.addActionListener(e -> {
            if (!updatingFilter) {
                applyFilterAndRender();
            }
        });
        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());
        activateButton.addActionListener(e -> onToggleActive(true));
        deactivateButton.addActionListener(e -> onToggleActive(false));
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        FormField.styleInputComponent(yearFilter);
        yearFilter.setPreferredSize(new Dimension(240, BotanicalTheme.INPUT_HEIGHT));
        left.add(yearFilter);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(editButton);
        right.add(activateButton);
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
                    List<Semester> sList = container.getAcademicService().getAllSemesters();
                    return new Object[]{yList, sList};
                },
                () -> stateDeck.showLoading("Memuat data semester..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<AcademicYear> yList = (List<AcademicYear>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<Semester> sList = (List<Semester>) arr[1];
                    this.years = yList;
                    this.allSemesters = sList;

                    Map<Long, String> map = new HashMap<>();
                    updatingFilter = true;
                    yearFilter.removeAllItems();
                    yearFilter.addItem(new SelectItem<>(null, "Semua Tahun Ajaran"));
                    for (AcademicYear y : years) {
                        map.put(y.getId(), y.getName());
                        yearFilter.addItem(new SelectItem<>(y.getId(), y.getName() + (y.isActive() ? " (Aktif)" : "")));
                    }
                    this.yearNames = map;
                    updatingFilter = false;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        @SuppressWarnings("unchecked")
        SelectItem<Long> selectedYear = (SelectItem<Long>) yearFilter.getSelectedItem();
        Long filterYearId = selectedYear != null ? selectedYear.getValue() : null;

        List<Semester> matched = new ArrayList<>();
        for (Semester s : allSemesters) {
            if (filterYearId == null || filterYearId.equals(s.getAcademicYearId())) {
                matched.add(s);
            }
        }
        this.filteredSemesters = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (Semester s : matched) {
            tableModel.addRow(new Object[]{
                    s.getId(),
                    yearNames.getOrDefault(s.getAcademicYearId(), "TA #" + s.getAcademicYearId()),
                    s.getSemesterNumber(),
                    s.getName(),
                    String.valueOf(s.getStartDate()),
                    String.valueOf(s.getEndDate()),
                    s.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private Semester getSelectedSemester() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredSemesters.size()) ? filteredSemesters.get(row) : null;
    }

    private void updateButtons() {
        Semester s = getSelectedSemester();
        addButton.setEnabled(!years.isEmpty());
        editButton.setEnabled(s != null);
        activateButton.setEnabled(s != null && !s.isActive());
        deactivateButton.setEnabled(s != null && s.isActive());
    }

    private void onAdd() {
        if (years.isEmpty()) {
            feedbackBanner.showWarning("Buat Tahun Ajaran terlebih dahulu sebelum menambah Semester.");
            return;
        }
        SemesterDialog dialog = new SemesterDialog(this, container, years, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Semester baru berhasil ditambahkan.");
            refreshData();
        }
    }

    private void onEdit() {
        Semester selected = getSelectedSemester();
        if (selected == null) {
            return;
        }
        SemesterDialog dialog = new SemesterDialog(this, container, years, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Data semester \"" + selected.getName() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private void onToggleActive(boolean activate) {
        Semester selected = getSelectedSemester();
        if (selected == null) {
            return;
        }
        String label = activate ? "Aktifkan" : "Nonaktifkan";
        if (!ConfirmationDialog.showConfirm(this, label + " Semester",
                label + " semester \"" + selected.getName() + "\"?", "Ya, " + label, !activate)) {
            return;
        }
        AsyncTaskRunner.run(
                () -> {
                    if (activate) {
                        container.getAcademicService().activateSemester(selected.getId());
                    } else {
                        container.getAcademicService().deactivateSemester(selected.getId());
                    }
                    return null;
                },
                null,
                unused -> {
                    feedbackBanner.showSuccess("Status semester berhasil diperbarui.");
                    refreshData();
                },
                error -> feedbackBanner.showError(error));
    }

    private static final class SemesterDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Semester editing;
        private final JComboBox<SelectItem<AcademicYear>> yearCombo = new JComboBox<>();
        private final JComboBox<Integer> numCombo = new JComboBox<>(new Integer[]{1, 2});
        private final JTextField nameInput = new JTextField();
        private final DateField startInput = new DateField(LocalDate.now());
        private final DateField endInput = new DateField(LocalDate.now().plusMonths(5));

        private final FormField yearField = new FormField("Tahun Ajaran", yearCombo, true);
        private final FormField numField = new FormField("Nomor Semester (1/2)", numCombo, true);
        private final FormField nameField = fullWidth(new FormField("Nama Semester", nameInput, true, "Contoh: Ganjil 2026/2027"));
        private final FormField startField = new FormField("Tanggal Mulai", startInput, true);
        private final FormField endField = new FormField("Tanggal Selesai", endInput, true);

        private SemesterDialog(Component owner, ApplicationContainer container, List<AcademicYear> years, Semester editing) {
            super(owner,
                    editing == null ? "Tambah Semester" : "Edit Semester",
                    editing == null ? "Simpan" : "Perbarui",
                    560,
                    440);
            this.container = container;
            this.editing = editing;

            for (AcademicYear y : years) {
                yearCombo.addItem(new SelectItem<>(y, y.getName() + " (" + y.getStartDate() + " s.d. " + y.getEndDate() + ")"));
            }

            addSectionCard("Informasi Semester", yearField, numField, nameField, startField, endField);

            if (editing != null) {
                for (int i = 0; i < yearCombo.getItemCount(); i++) {
                    if (yearCombo.getItemAt(i).getValue().getId().equals(editing.getAcademicYearId())) {
                        yearCombo.setSelectedIndex(i);
                        break;
                    }
                }
                numCombo.setSelectedItem(editing.getSemesterNumber());
                nameInput.setText(editing.getName());
                startInput.setLocalDate(editing.getStartDate());
                endInput.setLocalDate(editing.getEndDate());
            } else if (!years.isEmpty()) {
                AcademicYear first = years.get(0);
                for (AcademicYear tahun : years) {
                    if (tahun.isActive()) {
                        first = tahun;
                        break;
                    }
                }
                for (int i = 0; i < yearCombo.getItemCount(); i++) {
                    if (yearCombo.getItemAt(i).getValue().getId().equals(first.getId())) {
                        yearCombo.setSelectedIndex(i);
                        break;
                    }
                }
                startInput.setLocalDate(first.getStartDate());
                endInput.setLocalDate(first.getEndDate());
            }
        }

        @Override
        protected void onSubmit() {
            nameField.clearError();
            @SuppressWarnings("unchecked")
            SelectItem<AcademicYear> selected = (SelectItem<AcademicYear>) yearCombo.getSelectedItem();
            if (selected == null) {
                feedbackBanner.showError("Pilih tahun ajaran terlebih dahulu.");
                return;
            }
            Long yearId = selected.getValue().getId();
            int semNum = (Integer) numCombo.getSelectedItem();
            String name = nameInput.getText().trim();
            LocalDate start = startInput.getLocalDate();
            LocalDate end = endInput.getLocalDate();
            if (name.isEmpty()) {
                nameField.setError("Nama semester wajib diisi.");
                return;
            }
            executeSave(() -> {
                if (editing == null) {
                    return container.getAcademicService().createSemester(yearId, semNum, name, start, end);
                }
                return container.getAcademicService().updateSemester(editing.getId(), yearId, semNum, name, start, end);
            }, null);
        }
    }
}

