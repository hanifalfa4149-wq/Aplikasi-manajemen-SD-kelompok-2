package id.ac.sekolah.manajemenkelas.presentation.swing.module.academic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.Subject;
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
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Layar CRUD Mata Pelajaran SD.
 */
public class SubjectPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari kode atau nama mata pelajaran...");
    private final PrimaryButton addButton = new PrimaryButton("Tambah Mapel", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah mata pelajaran terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan mata pelajaran terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Kode", "Nama Mata Pelajaran", "Deskripsi", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<Subject> allSubjects = List.of();
    private List<Subject> filteredSubjects = List.of();

    public SubjectPanel(ApplicationContainer container) {
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
        table.setCenterAlignedColumns(0, 1);
        table.setColumnWidths(56, 110, 210, 320, 100);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada mata pelajaran",
                "Tambahkan daftar mata pelajaran yang diajarkan.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        addButton.addActionListener(e -> onAdd());
        editButton.addActionListener(e -> onEdit());
        deactivateButton.addActionListener(e -> onDeactivate());
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        toolbar.add(searchField, BorderLayout.WEST);
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
                () -> container.getAcademicService().getAllSubjects(),
                () -> stateDeck.showLoading("Memuat data mata pelajaran..."),
                subjects -> {
                    this.allSubjects = subjects;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        List<Subject> matched = new ArrayList<>();
        for (Subject s : allSubjects) {
            if (kw.isEmpty()
                    || (s.getCode() != null && s.getCode().toLowerCase().contains(kw))
                    || (s.getName() != null && s.getName().toLowerCase().contains(kw))) {
                matched.add(s);
            }
        }
        this.filteredSubjects = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (Subject s : matched) {
            tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getCode(),
                    s.getName(),
                    s.getDescription() == null || s.getDescription().isBlank() ? "-" : s.getDescription(),
                    s.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private Subject getSelectedSubject() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredSubjects.size()) ? filteredSubjects.get(row) : null;
    }

    private void updateButtons() {
        Subject s = getSelectedSubject();
        editButton.setEnabled(s != null);
        deactivateButton.setEnabled(s != null && s.isActive());
    }

    private void onAdd() {
        SubjectDialog dialog = new SubjectDialog(this, container, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Mata pelajaran baru berhasil disimpan.");
            refreshData();
        }
    }

    private void onEdit() {
        Subject selected = getSelectedSubject();
        if (selected == null) {
            return;
        }
        SubjectDialog dialog = new SubjectDialog(this, container, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Mata pelajaran \"" + selected.getName() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private void onDeactivate() {
        Subject selected = getSelectedSubject();
        if (selected == null || !selected.isActive()) {
            return;
        }
        if (!ConfirmationDialog.showConfirm(this, "Nonaktifkan Mata Pelajaran",
                "Nonaktifkan mata pelajaran \"" + selected.getName() + "\" (" + selected.getCode() + ")?",
                "Ya, Nonaktifkan", true)) {
            return;
        }
        AsyncTaskRunner.run(
                () -> {
                    container.getAcademicService().deactivateSubject(selected.getId());
                    return null;
                },
                null,
                unused -> {
                    feedbackBanner.showSuccess("Mata pelajaran berhasil dinonaktifkan.");
                    refreshData();
                },
                error -> feedbackBanner.showError(error));
    }

    private static final class SubjectDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Subject editing;
        private final JTextField codeInput = new JTextField();
        private final JTextField nameInput = new JTextField();
        private final JTextArea descInput = new JTextArea(3, 20);
        private final FormField codeField = new FormField("Kode Mata Pelajaran", codeInput, true, "Contoh: MTK-SD");
        private final FormField nameField = new FormField("Nama Mata Pelajaran", nameInput, true);
        private final FormField descField = fullWidth(new FormField("Deskripsi", FormField.wrapTextArea(descInput, 76), false));

        private SubjectDialog(Component owner, ApplicationContainer container, Subject editing) {
            super(owner,
                    editing == null ? "Tambah Mata Pelajaran" : "Edit Mata Pelajaran",
                    editing == null ? "Simpan" : "Perbarui",
                    540,
                    400);
            this.container = container;
            this.editing = editing;
            addSectionCard("Detail Mata Pelajaran", codeField, nameField, descField);
            if (editing != null) {
                codeInput.setText(editing.getCode());
                nameInput.setText(editing.getName());
                descInput.setText(editing.getDescription());
            }
        }

        @Override
        protected void onSubmit() {
            codeField.clearError();
            nameField.clearError();
            String code = codeInput.getText().trim();
            String name = nameInput.getText().trim();
            String desc = descInput.getText().trim();
            boolean valid = true;
            if (code.isEmpty()) {
                codeField.setError("Kode wajib diisi.");
                valid = false;
            }
            if (name.isEmpty()) {
                nameField.setError("Nama mata pelajaran wajib diisi.");
                valid = false;
            }
            if (!valid) {
                return;
            }
            executeSave(() -> {
                if (editing == null) {
                    return container.getAcademicService().createSubject(code, name, desc);
                }
                return container.getAcademicService().updateSubject(editing.getId(), code, name, desc);
            }, null);
        }
    }
}

