package id.ac.sekolah.manajemenkelas.presentation.swing.module.master;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
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
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Layar CRUD Master Data Siswa: pencarian, filter status, paginasi, tambah, edit, dan nonaktifkan.
 */
public class StudentPanel extends JPanel implements ScreenPage {

    private static final int PAGE_SIZE = 12;

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari nama, NIS, atau NISN...");
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Semua Status", "Aktif", "Nonaktif"});
    private final PrimaryButton addButton = new PrimaryButton("Tambah Siswa", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));

    private final IconButton editButton = new IconButton("Edit", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah data siswa terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton deactivateButton = new IconButton("Nonaktifkan", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Nonaktifkan siswa terpilih", IconButton.Tone.DANGER);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "NIS", "NISN", "Nama Lengkap", "L/P", "Tgl Lahir", "Kelas Aktif", "Orang Tua", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StyledTable.PaginationBar paginationBar = new StyledTable.PaginationBar();
    private final StateDeckPanel stateDeck;

    private List<Student> allStudents = List.of();
    private List<Student> filteredStudents = List.of();
    private Map<Long, String> activeClassByStudent = Map.of();

    public StudentPanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel topArea = new JPanel(new BorderLayout(0, 10));
        topArea.setOpaque(false);
        topArea.add(feedbackBanner, BorderLayout.NORTH);
        topArea.add(buildToolbar(), BorderLayout.CENTER);
        add(topArea, BorderLayout.NORTH);

        JPanel tableContainer = new ClayCard(new BorderLayout(0, 10));
        table.setStatusColumns(8);
        table.setCenterAlignedColumns(0, 4, 5);
        table.setColumnWidths(54, 96, 104, 190, 54, 100, 100, 160, 96);
        table.getSelectionModel().addListSelectionListener(e -> updateSelectionButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    onEditSelected();
                }
            }
        });
        tableContainer.add(table.wrapInScrollPane(), BorderLayout.CENTER);
        tableContainer.add(paginationBar, BorderLayout.SOUTH);

        stateDeck = new StateDeckPanel(
                tableContainer,
                "Belum ada data siswa",
                "Belum ada siswa yang sesuai pencarian atau filter. Klik Tambah Siswa untuk menambahkan data baru.");
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
        addButton.addActionListener(e -> onAddStudent());
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
                () -> {
                    List<Student> students = container.getStudentService().getAllStudents();
                    Map<Long, String> classNames = new HashMap<>();
                    for (SchoolClass c : container.getAcademicService().getAllClasses()) {
                        classNames.put(c.getId(), c.getName());
                    }
                    Map<Long, String> studentClasses = new HashMap<>();
                    for (StudentPlacement p : container.getAcademicService().getAllStudentPlacements()) {
                        if (p.isActive()) {
                            studentClasses.put(p.getStudentId(), classNames.getOrDefault(p.getClassId(), "Kelas #" + p.getClassId()));
                        }
                    }
                    return new Object[]{students, studentClasses};
                },
                () -> stateDeck.showLoading("Memuat data siswa..."),
                result -> {
                    Object[] arr = (Object[]) result;
                    @SuppressWarnings("unchecked")
                    List<Student> students = (List<Student>) arr[0];
                    @SuppressWarnings("unchecked")
                    Map<Long, String> studentClasses = (Map<Long, String>) arr[1];
                    this.allStudents = students;
                    this.activeClassByStudent = studentClasses;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String keyword = searchField.getText().toLowerCase();
        String statusMode = (String) statusFilter.getSelectedItem();

        List<Student> matched = new ArrayList<>();
        for (Student s : allStudents) {
            boolean matchesKeyword = keyword.isEmpty()
                    || contains(s.getFullName(), keyword)
                    || contains(s.getNis(), keyword)
                    || contains(s.getNisn(), keyword);
            boolean matchesStatus = "Semua Status".equals(statusMode)
                    || ("Aktif".equals(statusMode) && s.isActive())
                    || ("Nonaktif".equals(statusMode) && !s.isActive());
            if (matchesKeyword && matchesStatus) {
                matched.add(s);
            }
        }
        this.filteredStudents = matched;

        if (matched.isEmpty()) {
            stateDeck.showEmpty(
                    allStudents.isEmpty() ? "Belum ada data siswa" : "Data tidak ditemukan",
                    allStudents.isEmpty()
                            ? "Silakan tambahkan siswa pertama ke dalam sistem."
                            : "Tidak ada siswa yang cocok dengan kata kunci \"" + searchField.getText() + "\".");
            updateSelectionButtons();
            return;
        }

        renderCurrentPage();
        stateDeck.showContent();
    }

    private void renderCurrentPage() {
        paginationBar.update(filteredStudents.size(), PAGE_SIZE);
        int page = paginationBar.getCurrentPage();
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(filteredStudents.size(), from + PAGE_SIZE);

        tableModel.setRowCount(0);
        for (int i = from; i < to; i++) {
            Student s = filteredStudents.get(i);
            tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getNis(),
                    s.getNisn(),
                    s.getFullName(),
                    s.getGender(),
                    String.valueOf(s.getBirthDate()),
                    activeClassByStudent.getOrDefault(s.getId(), "Belum ditempatkan"),
                    s.getParentName() == null || s.getParentName().isBlank() ? "-" : s.getParentName(),
                    s.isActive() ? "AKTIF" : "NONAKTIF"
            });
        }
        updateSelectionButtons();
    }

    private Student getSelectedStudent() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int absoluteIdx = (paginationBar.getCurrentPage() - 1) * PAGE_SIZE + row;
        if (absoluteIdx < 0 || absoluteIdx >= filteredStudents.size()) {
            return null;
        }
        return filteredStudents.get(absoluteIdx);
    }

    private void updateSelectionButtons() {
        Student selected = getSelectedStudent();
        editButton.setEnabled(selected != null);
        deactivateButton.setEnabled(selected != null && selected.isActive());
    }

    private void onAddStudent() {
        StudentFormDialog dialog = new StudentFormDialog(this, container, null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Data siswa berhasil ditambahkan.");
            refreshData();
        }
    }

    private void onEditSelected() {
        Student selected = getSelectedStudent();
        if (selected == null) {
            return;
        }
        StudentFormDialog dialog = new StudentFormDialog(this, container, selected);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Data siswa \"" + selected.getFullName() + "\" berhasil diperbarui.");
            refreshData();
        }
    }

    private void onDeactivateSelected() {
        Student selected = getSelectedStudent();
        if (selected == null || !selected.isActive()) {
            return;
        }
        boolean confirmed = ConfirmationDialog.showConfirm(
                this,
                "Nonaktifkan Siswa",
                "Apakah Anda yakin ingin menonaktifkan siswa \"" + selected.getFullName() + "\" (NIS: " + selected.getNis() + ")?\nData historis absensi dan nilai tetap tersimpan.",
                "Ya, Nonaktifkan",
                true);
        if (!confirmed) {
            return;
        }

        AsyncTaskRunner.run(
                () -> {
                    container.getStudentService().deleteStudent(selected.getId());
                    return null;
                },
                () -> deactivateButton.setEnabled(false),
                unused -> {
                    feedbackBanner.showSuccess("Siswa \"" + selected.getFullName() + "\" berhasil dinonaktifkan.");
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

    /**
     * Dialog tambah/ubah Siswa dengan pengelompokan field logis.
     */
    private static final class StudentFormDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Student editing;

        private final JTextField nisInput = new JTextField();
        private final JTextField nisnInput = new JTextField();
        private final JTextField nameInput = new JTextField();
        private final JComboBox<String> genderInput = new JComboBox<>(new String[]{"L", "P"});
        private final JTextField birthPlaceInput = new JTextField();
        private final DateField birthDateInput = new DateField(LocalDate.of(2018, 1, 1));
        private final JTextArea addressInput = new JTextArea(3, 20);
        private final JTextField parentNameInput = new JTextField();
        private final JTextField parentPhoneInput = new JTextField();

        private final FormField nisField = new FormField("NIS", nisInput, true, "Nomor Induk Siswa");
        private final FormField nisnField = new FormField("NISN", nisnInput, true, "10 digit NISN nasional");
        private final FormField nameField = new FormField("Nama Lengkap", nameInput, true);
        private final FormField genderField = new FormField("Jenis Kelamin (L/P)", genderInput, true);
        private final FormField birthPlaceField = new FormField("Tempat Lahir", birthPlaceInput, true);
        private final FormField birthDateField = new FormField("Tanggal Lahir", birthDateInput, true);
        private final FormField addressField = fullWidth(new FormField("Alamat Lengkap", FormField.wrapTextArea(addressInput, 68), true));
        private final FormField parentNameField = new FormField("Nama Orang Tua / Wali", parentNameInput, false);
        private final FormField parentPhoneField = new FormField("No. Telepon Orang Tua", parentPhoneInput, false);

        private StudentFormDialog(Component owner, ApplicationContainer container, Student editing) {
            super(owner,
                    editing == null ? "Tambah Data Siswa" : "Ubah Data Siswa",
                    editing == null ? "Simpan Siswa" : "Perbarui Siswa",
                    640,
                    610);
            this.container = container;
            this.editing = editing;

            addSectionCard("Identitas Siswa", nisField, nisnField, nameField, genderField);
            addSectionCard("Kelahiran & Alamat", birthPlaceField, birthDateField, addressField);
            addSectionCard("Informasi Orang Tua / Wali", parentNameField, parentPhoneField);

            if (editing != null) {
                nisInput.setText(editing.getNis());
                nisnInput.setText(editing.getNisn());
                nameInput.setText(editing.getFullName());
                genderInput.setSelectedItem(editing.getGender());
                birthPlaceInput.setText(editing.getBirthPlace());
                if (editing.getBirthDate() != null) {
                    birthDateInput.setLocalDate(editing.getBirthDate());
                }
                addressInput.setText(editing.getAddress());
                parentNameInput.setText(editing.getParentName());
                parentPhoneInput.setText(editing.getParentPhone());
            }
        }

        @Override
        protected void onSubmit() {
            nisField.clearError();
            nisnField.clearError();
            nameField.clearError();
            birthPlaceField.clearError();
            birthDateField.clearError();
            addressField.clearError();

            String nis = nisInput.getText().trim();
            String nisn = nisnInput.getText().trim();
            String fullName = nameInput.getText().trim();
            String gender = (String) genderInput.getSelectedItem();
            String birthPlace = birthPlaceInput.getText().trim();
            LocalDate birthDate = birthDateInput.getLocalDate();
            String address = addressInput.getText().trim();
            String parentName = parentNameInput.getText().trim();
            String parentPhone = parentPhoneInput.getText().trim();

            boolean valid = true;
            if (nis.isEmpty()) {
                nisField.setError("NIS wajib diisi.");
                valid = false;
            }
            if (nisn.isEmpty()) {
                nisnField.setError("NISN wajib diisi.");
                valid = false;
            }
            if (fullName.isEmpty()) {
                nameField.setError("Nama lengkap wajib diisi.");
                valid = false;
            }
            if (birthPlace.isEmpty()) {
                birthPlaceField.setError("Tempat lahir wajib diisi.");
                valid = false;
            }
            if (birthDate == null) {
                birthDateField.setError("Tanggal lahir wajib diisi.");
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
                    return container.getStudentService().createStudent(
                            nis, nisn, fullName, gender, birthDate, birthPlace, address, parentName, parentPhone);
                }
                return container.getStudentService().updateStudent(
                        editing.getId(), nis, nisn, fullName, gender, birthDate, birthPlace, address, parentName, parentPhone);
            }, null);
        }
    }
}

