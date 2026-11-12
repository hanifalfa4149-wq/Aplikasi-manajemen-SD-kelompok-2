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
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
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
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SecondaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SelectItem;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Layar Penempatan Siswa: tempatkan ke kelas, naik/pindah kelas, lulus/keluar, dan lihat riwayat.
 */
public class StudentPlacementPanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();
    private final SearchField searchField = new SearchField("Cari nama siswa atau kelas...");
    private final JComboBox<String> statusFilter = new JComboBox<>(new String[]{"Semua Status", "AKTIF", "NAIK_KELAS", "PINDAH", "LULUS", "KELUAR"});
    private final PrimaryButton placeButton = new PrimaryButton("Tempatkan Siswa", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton moveButton = new IconButton("Naik / Pindah", BotanicalIcons.get(BotanicalIcons.Type.PLACEMENT, 15, BotanicalTheme.EVERGREEN), "Naik kelas atau pindah kelas untuk siswa aktif terpilih", IconButton.Tone.NEUTRAL);
    private final IconButton completeButton = new IconButton("Lulus / Keluar", BotanicalIcons.get(BotanicalIcons.Type.DEACTIVATE, 15, BotanicalTheme.DANGER), "Selesaikan penempatan aktif siswa terpilih", IconButton.Tone.DANGER);
    private final IconButton historyButton = new IconButton("Riwayat", BotanicalIcons.get(BotanicalIcons.Type.DETAIL, 15, BotanicalTheme.EVERGREEN), "Lihat riwayat kelas siswa terpilih", IconButton.Tone.NEUTRAL);

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Siswa", "Kelas", "Tahun Ajaran", "Tgl Masuk", "Tgl Keluar", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<Student> activeStudents = List.of();
    private List<SchoolClass> activeClasses = List.of();
    private Map<Long, Student> studentMap = Map.of();
    private Map<Long, String> classNames = Map.of();
    private Map<Long, String> yearNames = Map.of();
    private List<StudentPlacement> allPlacements = List.of();
    private List<StudentPlacement> filteredPlacements = List.of();

    public StudentPlacementPanel(ApplicationContainer container) {
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
        table.setCenterAlignedColumns(0, 4, 5);
        table.setColumnWidths(52, 220, 105, 130, 104, 104, 110);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada penempatan siswa",
                "Tempatkan siswa aktif ke dalam kelas melalui tombol Tempatkan Siswa.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        searchField.addSearchListener(q -> applyFilterAndRender());
        statusFilter.addActionListener(e -> applyFilterAndRender());
        placeButton.addActionListener(e -> onPlaceStudent());
        moveButton.addActionListener(e -> onMoveStudent());
        completeButton.addActionListener(e -> onCompletePlacement());
        historyButton.addActionListener(e -> onViewHistory());
        updateButtons();
    }

    private JPanel buildToolbar() {
        ClayCard toolbar = new ClayCard(new BorderLayout(12, 0));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(searchField);
        FormField.styleInputComponent(statusFilter);
        statusFilter.setPreferredSize(new Dimension(150, BotanicalTheme.INPUT_HEIGHT));
        left.add(statusFilter);
        toolbar.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(historyButton);
        right.add(moveButton);
        right.add(completeButton);
        right.add(placeButton);
        toolbar.add(right, BorderLayout.EAST);
        return toolbar;
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                () -> new Object[]{
                        container.getStudentService().getAllStudents(),
                        container.getAcademicService().getAllClasses(),
                        container.getAcademicService().getAllAcademicYears(),
                        container.getAcademicService().getAllStudentPlacements()
                },
                () -> stateDeck.showLoading("Memuat data penempatan siswa..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<Student> students = (List<Student>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<SchoolClass> classes = (List<SchoolClass>) arr[1];
                    @SuppressWarnings("unchecked")
                    List<AcademicYear> years = (List<AcademicYear>) arr[2];
                    @SuppressWarnings("unchecked")
                    List<StudentPlacement> placements = (List<StudentPlacement>) arr[3];

                    List<Student> daftarSiswaAktif = new ArrayList<>();
                    Map<Long, Student> sMap = new HashMap<>();
                    for (Student s : students) {
                        if (s.isActive()) {
                            daftarSiswaAktif.add(s);
                        }
                        sMap.put(s.getId(), s);
                    }
                    this.activeStudents = daftarSiswaAktif;
                    this.studentMap = sMap;

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

                    Map<Long, String> yMap = new HashMap<>();
                    for (AcademicYear y : years) {
                        yMap.put(y.getId(), y.getName());
                    }
                    this.yearNames = yMap;

                    this.allPlacements = placements;
                    applyFilterAndRender();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void applyFilterAndRender() {
        String kw = searchField.getText().toLowerCase();
        String statusMode = (String) statusFilter.getSelectedItem();

        List<StudentPlacement> matched = new ArrayList<>();
        for (StudentPlacement p : allPlacements) {
            if (!"Semua Status".equals(statusMode) && !statusMode.equalsIgnoreCase(p.getStatus())) {
                continue;
            }
            Student s = studentMap.get(p.getStudentId());
            String sText = s != null ? s.getNis() + " " + s.getFullName() : "Siswa #" + p.getStudentId();
            String cText = classNames.getOrDefault(p.getClassId(), "Kelas #" + p.getClassId());
            if (kw.isEmpty() || sText.toLowerCase().contains(kw) || cText.toLowerCase().contains(kw)) {
                matched.add(p);
            }
        }
        this.filteredPlacements = matched;
        if (matched.isEmpty()) {
            stateDeck.showEmpty();
            updateButtons();
            return;
        }
        tableModel.setRowCount(0);
        for (StudentPlacement p : matched) {
            Student s = studentMap.get(p.getStudentId());
            String sLabel = s != null ? s.getNis() + " - " + s.getFullName() : "Siswa #" + p.getStudentId();
            tableModel.addRow(new Object[]{
                    p.getId(),
                    sLabel,
                    classNames.getOrDefault(p.getClassId(), "Kelas #" + p.getClassId()),
                    yearNames.getOrDefault(p.getAcademicYearId(), "TA #" + p.getAcademicYearId()),
                    String.valueOf(p.getEntryDate()),
                    p.getExitDate() == null ? "-" : String.valueOf(p.getExitDate()),
                    p.getStatus()
            });
        }
        updateButtons();
        stateDeck.showContent();
    }

    private StudentPlacement getSelected() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < filteredPlacements.size()) ? filteredPlacements.get(row) : null;
    }

    private void updateButtons() {
        StudentPlacement sel = getSelected();
        placeButton.setEnabled(!activeStudents.isEmpty() && !activeClasses.isEmpty());
        moveButton.setEnabled(sel != null && sel.isActive());
        completeButton.setEnabled(sel != null && sel.isActive());
        historyButton.setEnabled(sel != null);
    }

    private void onPlaceStudent() {
        PlaceStudentDialog dialog = new PlaceStudentDialog(this, container, activeStudents, activeClasses);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Siswa berhasil ditempatkan ke kelas.");
            refreshData();
        }
    }

    private void onMoveStudent() {
        StudentPlacement sel = getSelected();
        if (sel == null || !sel.isActive()) {
            return;
        }
        Student student = studentMap.get(sel.getStudentId());
        String studentName = student != null ? student.getFullName() : "Siswa #" + sel.getStudentId();
        List<SchoolClass> targetClasses = new ArrayList<>();
        for (SchoolClass c : activeClasses) {
            if (!c.getId().equals(sel.getClassId())) {
                targetClasses.add(c);
            }
        }
        if (targetClasses.isEmpty()) {
            feedbackBanner.showWarning("Tidak ada kelas tujuan lain yang aktif.");
            return;
        }
        MoveStudentDialog dialog = new MoveStudentDialog(this, container, sel, studentName, targetClasses);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Perpindahan kelas siswa \"" + studentName + "\" berhasil dicatat.");
            refreshData();
        }
    }

    private void onCompletePlacement() {
        StudentPlacement sel = getSelected();
        if (sel == null || !sel.isActive()) {
            return;
        }
        Student student = studentMap.get(sel.getStudentId());
        String studentName = student != null ? student.getFullName() : "Siswa #" + sel.getStudentId();
        CompletePlacementDialog dialog = new CompletePlacementDialog(this, container, sel, studentName);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Penempatan siswa \"" + studentName + "\" berhasil diselesaikan.");
            refreshData();
        }
    }

    private void onViewHistory() {
        StudentPlacement sel = getSelected();
        if (sel == null) {
            return;
        }
        Student student = studentMap.get(sel.getStudentId());
        String title = student != null ? student.getNis() + " - " + student.getFullName() : "Siswa #" + sel.getStudentId();
        List<StudentPlacement> history = new ArrayList<>();
        for (StudentPlacement p : allPlacements) {
            if (p.getStudentId().equals(sel.getStudentId())) {
                history.add(p);
            }
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Riwayat Penempatan: " + title, JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(640, 360);
        dialog.setLocationRelativeTo(this);
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BotanicalTheme.SOFT_IVORY);
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        DefaultTableModel histModel = new DefaultTableModel(new String[]{"Kelas", "Tahun Ajaran", "Tgl Masuk", "Tgl Keluar", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        for (StudentPlacement p : history) {
            histModel.addRow(new Object[]{
                    classNames.getOrDefault(p.getClassId(), "Kelas #" + p.getClassId()),
                    yearNames.getOrDefault(p.getAcademicYearId(), "TA #" + p.getAcademicYearId()),
                    String.valueOf(p.getEntryDate()),
                    p.getExitDate() == null ? "-" : String.valueOf(p.getExitDate()),
                    p.getStatus()
            });
        }
        StyledTable histTable = new StyledTable(histModel);
        histTable.setStatusColumns(4);
        root.add(histTable.wrapInScrollPane(), BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        footer.setOpaque(false);
        SecondaryButton closeBtn = new SecondaryButton("Tutup");
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private static final class PlaceStudentDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final JComboBox<SelectItem<Student>> studentCombo = new JComboBox<>();
        private final JComboBox<SelectItem<SchoolClass>> classCombo = new JComboBox<>();
        private final DateField entryDateInput = new DateField(LocalDate.now());

        private PlaceStudentDialog(Component owner, ApplicationContainer container, List<Student> students, List<SchoolClass> classes) {
            super(owner, "Tempatkan Siswa ke Kelas", "Tempatkan Siswa", 540, 380);
            this.container = container;
            for (Student s : students) {
                studentCombo.addItem(new SelectItem<>(s, s.getNis() + " - " + s.getFullName()));
            }
            for (SchoolClass c : classes) {
                classCombo.addItem(new SelectItem<>(c, c.getName() + " (Kap: " + c.getCapacity() + ")"));
            }
            addSectionCard(
                    "Penempatan Awal",
                    fullWidth(new FormField("Siswa Aktif", studentCombo, true)),
                    fullWidth(new FormField("Kelas Tujuan", classCombo, true)),
                    fullWidth(new FormField("Tanggal Masuk Kelas", entryDateInput, true)));
        }

        @Override
        protected void onSubmit() {
            @SuppressWarnings("unchecked")
            SelectItem<Student> s = (SelectItem<Student>) studentCombo.getSelectedItem();
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> c = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            if (s == null || c == null) {
                feedbackBanner.showError("Siswa dan kelas wajib dipilih.");
                return;
            }
            executeSave(() -> container.getAcademicService().placeStudentInClass(
                    s.getValue().getId(), c.getValue().getId(), entryDateInput.getLocalDate()), null);
        }
    }

    private static final class MoveStudentDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final StudentPlacement current;
        private final JComboBox<SelectItem<SchoolClass>> targetCombo = new JComboBox<>();
        private final JComboBox<String> reasonCombo = new JComboBox<>(new String[]{StudentPlacement.STATUS_NAIK_KELAS, StudentPlacement.STATUS_PINDAH});
        private final DateField dateInput = new DateField(LocalDate.now());

        private MoveStudentDialog(
                Component owner,
                ApplicationContainer container,
                StudentPlacement current,
                String studentName,
                List<SchoolClass> targets) {
            super(owner, "Naik Kelas / Pindah Kelas", "Proses Perpindahan", 540, 390);
            this.container = container;
            this.current = current;
            for (SchoolClass c : targets) {
                targetCombo.addItem(new SelectItem<>(c, c.getName() + " (Tingkat " + c.getGradeLevel() + ")"));
            }
            addSectionCard(
                    "Perpindahan Siswa: " + studentName,
                    fullWidth(new FormField("Jenis Perpindahan", reasonCombo, true)),
                    fullWidth(new FormField("Kelas Tujuan", targetCombo, true)),
                    fullWidth(new FormField("Tanggal Efektif (Min: " + current.getEntryDate() + ")", dateInput, true)));
        }

        @Override
        protected void onSubmit() {
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> target = (SelectItem<SchoolClass>) targetCombo.getSelectedItem();
            if (target == null) {
                feedbackBanner.showError("Kelas tujuan wajib dipilih.");
                return;
            }
            String closingStatus = (String) reasonCombo.getSelectedItem();
            executeSave(() -> container.getAcademicService().promoteOrTransferStudent(
                    current.getStudentId(), target.getValue().getId(), dateInput.getLocalDate(), closingStatus), null);
        }
    }

    private static final class CompletePlacementDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final StudentPlacement current;
        private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{StudentPlacement.STATUS_LULUS, StudentPlacement.STATUS_KELUAR});
        private final DateField exitInput = new DateField(LocalDate.now());

        private CompletePlacementDialog(Component owner, ApplicationContainer container, StudentPlacement current, String studentName) {
            super(owner, "Selesaikan Penempatan Siswa", "Simpan Status Akhir", 490, 340);
            this.container = container;
            this.current = current;
            addSectionCard(
                    "Penutupan Penempatan: " + studentName,
                    fullWidth(new FormField("Status Akhir", statusCombo, true)),
                    fullWidth(new FormField("Tanggal Keluar (Min: " + current.getEntryDate() + ")", exitInput, true)));
        }

        @Override
        protected void onSubmit() {
            String status = (String) statusCombo.getSelectedItem();
            executeSave(() -> container.getAcademicService().completeStudentPlacement(
                    current.getStudentId(), exitInput.getLocalDate(), status), null);
        }
    }
}

