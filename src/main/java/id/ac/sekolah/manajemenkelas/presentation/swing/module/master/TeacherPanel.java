package id.ac.sekolah.manajemenkelas.presentation.swing.module.master;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

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
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

/**
 * Layar CRUD Master Data Guru: pencarian, filter status, paginasi, tambah, edit, dan nonaktifkan.
 */
public class TeacherPanel extends JPanel implements ScreenPage {

    private static final int PAGE_SIZE = 12;

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari nama, NIP, atau email...");
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Semua Status", "Aktif", "Nonaktif"});
    private final PrimaryButton addButton = new PrimaryButton("Tambah Guru", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah data guru terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan guru terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "NIP", "Nama Lengkap", "L/P", "Email", "No. Telepon", "Pendidikan", "Tgl Bergabung", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StyledTable.PaginationBar paginationBar = new StyledTable.PaginationBar();
    private final StateDeckPanel stateDeck;

    private List<Teacher> allTeachers = List.of();
    private List<Teacher> filteredTeachers = List.of();

    public TeacherPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(feedbackBanner, BorderLayout.NORTH);
        topArea.add(buildToolbar(), BorderLayout.CENTER);
        add(topArea, BorderLayout.NORTH);

        JPanel tableCard = new ClayCard(new BorderLayout(0, 10));
        table.setStatusColumns(8);
        table.setCenterAlignedColumns(0, 3, 7);
        table.setColumnWidths(52, 130, 185, 50, 175, 110, 115, 102, 92);
        table.getSelectionModel().addListSelectionListener(e -> updateSelectionButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    onEditSelected();
                }
            }
        });
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);
        tableCard.add(paginationBar, BorderLayout.SOUTH);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada data guru",
                "Belum ada guru yang cocok dengan pencarian atau filter.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> {
            paginationBar.resetToFirstPage();
            applyFilterAndRender();
        });
        statusFilter.addActionListener(e -> {
            paginationBar.resetToFirstPage();
            applyFilterAndRender();
        });
        paginationBar.setOnPageChange(this::renderCurrentPage);
        addButton.addActionListener(e -> onAddTeacher());
        editButton.addActionListener(e -> onEditSelected());
        deactivateButton.addActionListener(e -> onDeactivateSelected());
        updateSelectionButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(searchField);
        FormField.styleInputComponent(statusFilter);
        statusFilter.setPreferredSize(new Dimension(148, BotanicalTheme.INPUT_HEIGHT));
        left.add(statusFilter);
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
                () -> container.getTeacherService().getAllTeachers(),
                () -> stateDeck.showLoading("Memuat data guru..."),
                teachers -> {
                    this.allTeachers = teachers;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String keyword = searchField.getText().toLowerCase();
        String statusMode = (String) statusFilter.getSelectedItem();

        List<Teacher> matched = new ArrayList<>();
        for (Teacher t : allTeachers) {
            boolean matchesKeyword = keyword.isEmpty()
                    || contains(t.getFullName(), keyword)
                    || contains(t.getNip(), keyword)
                    || contains(t.getEmail(), keyword);
            boolean matchesStatus = "Semua Status".equals(statusMode)
                    || ("Aktif".equals(statusMode) && t.isActive())
                    || ("Nonaktif".equals(statusMode) && !t.isActive());
            if (matchesKeyword && matchesStatus) {
                matched.add(t);
            }
        }
        this.filteredTeachers = matched;

        if (matched.isEmpty()) {
            stateDeck.showEmpty(
                    allTeachers.isEmpty() ? "Belum ada data guru" : "Data guru tidak ditemukan",
                    allTeachers.isEmpty()
                            ? "Silakan tambahkan data guru melalui tombol Tambah Guru."
                            : "Tidak ada guru yang cocok dengan kriteria pencarian saat ini.");
            updateSelectionButtons();
            return;
        }
        renderCurrentPage();
        stateDeck.showContent();
    }

    private void renderCurrentPage() {
        paginationBar.update(filteredTeachers.size(), PAGE_SIZE);
        int page = paginationBar.getCurrentPage();
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(filteredTeachers.size(), from + PAGE_SIZE);

        tableModel.setRowCount(0);
        for (int i = from; i < to; i++) {
            Teacher t = filteredTeachers.get(i);
            tableModel.addRow(new Object[]{
                    t.getId(),
                    t.getNip(),
                    t.getFullName(),
                    t.getGender(),
                    t.getEmail() == null || t.getEmail().isBlank() ? "-" : t.getEmail(),
                    t.getPhone() == null || t.getPhone().isBlank() ? "-" : t.getPhone(),
                    t.getEducation(),
                    String.valueOf(t.getJoinDate()),
                    t.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateSelectionButtons();
    }

    private Teacher getSelectedTeacher() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int idx = (paginationBar.getCurrentPage() - 1) * PAGE_SIZE + row;
        return (idx >= 0 && idx < filteredTeachers.size()) ? filteredTeachers.get(idx) : null;
    }

    private void updateSelectionButtons() {
        Teacher selected = getSelectedTeacher();
        editButton.setEnabled(selected != null);
        deactivateButton.setEnabled(selected != null && selected.isActive());
    }

    private void onAddTeacher() {
        TeacherFormDialog dialog = new TeacherFormDialog(this, container, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Data guru berhasil ditambahkan.");
            refreshData();
        }
    }

    private void onEditSelected() {
        Teacher selected = getSelectedTeacher();
        if (selected == null) {
            return;
        }
        TeacherFormDialog dialog = new TeacherFormDialog(this, container, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Data guru \"" + selected.getFullName() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private void onDeactivateSelected() {
        Teacher selected = getSelectedTeacher();
        if (selected == null || !selected.isActive()) {
            return;
        }
        boolean confirmed = ConfirmationDialog.showConfirm(
                this,
                "Nonaktifkan Guru",
                "Apakah Anda yakin ingin menonaktifkan guru \"" + selected.getFullName() + "\" (NIP: " + selected.getNip() + ")?",
                "Ya, Nonaktifkan",
                true);
        if (!confirmed) {
            return;
        }

        AsyncTaskRunner.run(
                () -> {
                    container.getTeacherService().deleteTeacher(selected.getId());
                    return null;
                },
                () -> deactivateButton.setEnabled(false),
                unused -> {
                    feedbackBanner.showSuccess("Guru \"" + selected.getFullName() + "\" berhasil dinonaktifkan.");
                    refreshData();
                },
                error -> {
                    updateSelectionButtons();
                    feedbackBanner.showError(error);
                });
    }

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword);
    }

    private static final class TeacherFormDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Teacher editing;

        private final JTextField nipInput = new JTextField();
        private final JTextField nameInput = new JTextField();
        private final JComboBox<String> genderInput = new JComboBox<>(new String[]{"L", "P"});
        private final JTextField emailInput = new JTextField();
        private final JTextField phoneInput = new JTextField();
        private final JTextField educationInput = new JTextField();
        private final DateField joinDateInput = new DateField(LocalDate.now());
        private final JTextArea addressInput = new JTextArea(3, 20);

        private final FormField nipField = new FormField("NIP", nipInput, true);
        private final FormField nameField = new FormField("Nama Lengkap", nameInput, true);
        private final FormField genderField = new FormField("Jenis Kelamin (L/P)", genderInput, true);
        private final FormField emailField = new FormField("Email", emailInput, false, "Opsional, contoh: guru@sekolah.sch.id");
        private final FormField phoneField = new FormField("No. Telepon", phoneInput, false);
        private final FormField educationField = new FormField("Pendidikan Terakhir", educationInput, true, "Contoh: S1 PGSD");
        private final FormField joinDateField = new FormField("Tanggal Bergabung", joinDateInput, true);
        private final FormField addressField = fullWidth(new FormField("Alamat Lengkap", FormField.wrapTextArea(addressInput, 68), true));

        private TeacherFormDialog(Component owner, ApplicationContainer container, Teacher editing) {
            super(owner,
                    editing == null ? "Tambah Data Guru" : "Ubah Data Guru",
                    editing == null ? "Simpan Guru" : "Perbarui Guru",
                    640,
                    590);
            this.container = container;
            this.editing = editing;

            addSectionCard("Identitas & Kontak Guru", nipField, nameField, genderField, emailField, phoneField);
            addSectionCard("Pendidikan & Kepegawaian", educationField, joinDateField, addressField);

            if (editing != null) {
                nipInput.setText(editing.getNip());
                nameInput.setText(editing.getFullName());
                genderInput.setSelectedItem(editing.getGender());
                emailInput.setText(editing.getEmail());
                phoneInput.setText(editing.getPhone());
                educationInput.setText(editing.getEducation());
                if (editing.getJoinDate() != null) {
                    joinDateInput.setLocalDate(editing.getJoinDate());
                }
                addressInput.setText(editing.getAddress());
            }
        }

        @Override
        protected void onSubmit() {
            nipField.clearError();
            nameField.clearError();
            educationField.clearError();
            joinDateField.clearError();
            addressField.clearError();

            String nip = nipInput.getText().trim();
            String fullName = nameInput.getText().trim();
            String gender = (String) genderInput.getSelectedItem();
            String email = emailInput.getText().trim();
            String phone = phoneInput.getText().trim();
            String education = educationInput.getText().trim();
            LocalDate joinDate = joinDateInput.getLocalDate();
            String address = addressInput.getText().trim();

            boolean valid = true;
            if (nip.isEmpty()) {
                nipField.setError("NIP wajib diisi.");
                valid = false;
            }
            if (fullName.isEmpty()) {
                nameField.setError("Nama lengkap wajib diisi.");
                valid = false;
            }
            if (education.isEmpty()) {
                educationField.setError("Pendidikan terakhir wajib diisi.");
                valid = false;
            }
            if (joinDate == null) {
                joinDateField.setError("Tanggal bergabung wajib diisi.");
                valid = false;
            }
            if (address.isEmpty()) {
                addressField.setError("Alamat wajib diisi.");
                valid = false;
            }
            if (!valid) {
                return;
            }

            executeSave(() -> {
                if (editing == null) {
                    return container.getTeacherService().createTeacher(
                            nip, fullName, gender, email, phone, address, education, joinDate);
                }
                return container.getTeacherService().updateTeacher(
                        editing.getId(), nip, fullName, gender, email, phone, address, education, joinDate);
            }, null);
        }
    }
}

