package id.ac.sekolah.manajemenkelas.presentation.swing.module.academic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
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
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SearchField;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Layar manajemen Tahun Ajaran: tambah, ubah, aktifkan, dan nonaktifkan tahun ajaran.
 */
public class AcademicYearPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari nama tahun ajaran...");
    private final PrimaryButton addButton = new PrimaryButton("Tambah Tahun Ajaran", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah tahun ajaran terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton activateButton = new IconButton("Aktifkan", BotanicalIcons.get(BotanicalIcons.Type.CHECK, 15, BotanicalTheme.SUCCESS), "Aktifkan tahun ajaran terpilih", IconButton.Tone.SUCCESS);
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan tahun ajaran terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Nama Tahun Ajaran", "Tanggal Mulai", "Tanggal Selesai", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<AcademicYear> allYears = List.of();
    private List<AcademicYear> filteredYears = List.of();

    public AcademicYearPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel(new BorderLayout(0, 10));
        top.setOpaque(false);
        top.add(feedbackBanner, BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        ClayCard tableCard = new ClayCard(new BorderLayout());
        table.setStatusColumns(4);
        table.setCenterAlignedColumns(0, 2, 3);
        table.setColumnWidths(60, 220, 140, 140, 110);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada tahun ajaran",
                "Tambahkan tahun ajaran baru untuk mulai menyusun semester dan rombongan belajar.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());
        activateButton.addActionListener(e -> onToggleActive(true));
        deactivateButton.addActionListener(e -> onToggleActive(false));
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        toolbar.add(searchField, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(activateButton);
        actions.add(deactivateButton);
        actions.add(addButton);
        toolbar.add(actions, BorderLayout.EAST);
        return toolbar;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> container.getAcademicService().getAllAcademicYears(),
                () -> stateDeck.showLoading("Memuat data tahun ajaran..."),
                years -> {
                    this.allYears = years;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        List<AcademicYear> matched = new ArrayList<>();
        for (AcademicYear y : allYears) {
            if (kw.isEmpty() || (y.getName() != null && y.getName().toLowerCase().contains(kw))) {
                matched.add(y);
            }
        }
        this.filteredYears = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (AcademicYear y : matched) {
            tableModel.addRow(new Object[]{
                    y.getId(),
                    y.getName(),
                    String.valueOf(y.getStartDate()),
                    String.valueOf(y.getEndDate()),
                    y.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private AcademicYear getSelectedYear() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredYears.size()) ? filteredYears.get(row) : null;
    }

    private void updateButtons() {
        AcademicYear selected = getSelectedYear();
        editButton.setEnabled(selected != null);
        activateButton.setEnabled(selected != null && !selected.isActive());
        deactivateButton.setEnabled(selected != null && selected.isActive());
    }

    private void onAdd() {
        YearDialog dialog = new YearDialog(this, container, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Tahun ajaran baru berhasil disimpan.");
            refreshData();
        }
    }

    private void onEdit() {
        AcademicYear selected = getSelectedYear();
        if (selected == null) {
            return;
        }
        YearDialog dialog = new YearDialog(this, container, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Tahun ajaran \"" + selected.getName() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private void onToggleActive(boolean activate) {
        AcademicYear selected = getSelectedYear();
        if (selected == null) {
            return;
        }
        String actionText = activate ? "Aktifkan" : "Nonaktifkan";
        boolean confirmed = ConfirmationDialog.showConfirm(
                this,
                actionText + " Tahun Ajaran",
                actionText + " tahun ajaran \"" + selected.getName() + "\"?"
                        + (activate ? "\nCatatan: sistem hanya mengizinkan satu tahun ajaran aktif pada saat bersamaan." : ""),
                "Ya, " + actionText,
                !activate);
        if (!confirmed) {
            return;
        }
        AsyncTaskRunner.run(
                () -> {
                    if (activate) {
                        container.getAcademicService().activateAcademicYear(selected.getId());
                    } else {
                        container.getAcademicService().deactivateAcademicYear(selected.getId());
                    }
                    return null;
                },
                () -> {
                    activateButton.setEnabled(false);
                    deactivateButton.setEnabled(false);
                },
                unused -> {
                    feedbackBanner.showSuccess("Status tahun ajaran \"" + selected.getName() + "\" berhasil diperbarui.");
                    refreshData();
                },
                error -> {
                    updateButtons();
                    feedbackBanner.showError(error);
                });
    }

    private static final class YearDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final AcademicYear editing;
        private final JTextField nameInput = new JTextField();
        private final DateField startInput = new DateField(LocalDate.of(LocalDate.now().getYear(), 7, 1));
        private final DateField endInput = new DateField(LocalDate.of(LocalDate.now().getYear() + 1, 6, 30));
        private final FormField nameField = fullWidth(new FormField("Nama Tahun Ajaran", nameInput, true, "Contoh: 2026/2027"));
        private final FormField startField = new FormField("Tanggal Mulai", startInput, true);
        private final FormField endField = new FormField("Tanggal Selesai", endInput, true);

        private YearDialog(Component owner, ApplicationContainer container, AcademicYear editing) {
            super(owner,
                    editing == null ? "Tambah Tahun Ajaran" : "Edit Tahun Ajaran",
                    editing == null ? "Simpan" : "Perbarui",
                    520,
                    380);
            this.container = container;
            this.editing = editing;
            addSectionCard("Periode Tahun Ajaran", nameField, startField, endField);

            if (editing != null) {
                nameInput.setText(editing.getName());
                startInput.setLocalDate(editing.getStartDate());
                endInput.setLocalDate(editing.getEndDate());
            }
        }

        @Override
        protected void onSubmit() {
            nameField.clearError();
            String name = nameInput.getText().trim();
            LocalDate start = startInput.getLocalDate();
            LocalDate end = endInput.getLocalDate();
            if (name.isEmpty()) {
                nameField.setError("Nama tahun ajaran wajib diisi.");
                return;
            }
            executeSave(() -> {
                if (editing == null) {
                    return container.getAcademicService().createAcademicYear(name, start, end);
                }
                return container.getAcademicService().updateAcademicYear(editing.getId(), name, start, end);
            }, null);
        }
    }
}

