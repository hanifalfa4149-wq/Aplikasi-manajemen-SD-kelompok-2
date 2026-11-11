package id.ac.sekolah.manajemenkelas.presentation.swing.module.operational;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.model.AttendanceSummary;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
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
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SecondaryButton;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.SelectItem;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Layar Operasional Absensi: lihat per kelas/tanggal/siswa, input massal sekelas,
 * catat tunggal, ubah status, serta rekap rentang tanggal.
 */
public class AttendancePanel extends JPanel implements ScreenPage {

    private enum ViewMode {
        CLASS_AND_DATE("Kelas & Tanggal"),
        CLASS_ALL("Semua di Kelas"),
        STUDENT_HISTORY("Riwayat Siswa"),
        CLASS_RANGE("Rekap Rentang Tanggal");

        private final String label;

        ViewMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final ApplicationContainer container;
    private final FeedbackBanner feedbackBanner = new FeedbackBanner();

    private final JComboBox<ViewMode> modeCombo = new JComboBox<>(ViewMode.values());
    private final JComboBox<SelectItem<SchoolClass>> classCombo = new JComboBox<>();
    private final JComboBox<SelectItem<Student>> studentCombo = new JComboBox<>();
    private final DateField startDateField = new DateField(LocalDate.now());
    private final DateField endDateField = new DateField(LocalDate.now());
    private final SecondaryButton applyButton = new SecondaryButton("Tampilkan");

    private final PrimaryButton batchButton = new PrimaryButton("Input Absensi Kelas", BotanicalIcons.get(BotanicalIcons.Type.PLUS, 16, BotanicalTheme.WHITE));
    private final IconButton singleButton = new IconButton("Catat Tunggal", BotanicalIcons.get(BotanicalIcons.Type.ATTENDANCE, 15, BotanicalTheme.EVERGREEN), "Catat absensi satu siswa", IconButton.Tone.NEUTRAL);
    private final IconButton editButton = new IconButton("Ubah Status", BotanicalIcons.get(BotanicalIcons.Type.EDIT, 15, BotanicalTheme.EVERGREEN), "Ubah status/keterangan absensi terpilih", IconButton.Tone.NEUTRAL);

    private final JLabel totalStat = new JLabel("Total: 0");
    private final JLabel hadirStat = new JLabel("Hadir: 0");
    private final JLabel sakitStat = new JLabel("Sakit: 0");
    private final JLabel izinStat = new JLabel("Izin: 0");
    private final JLabel alpaStat = new JLabel("Alpa: 0");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Tanggal", "NIS", "Nama Siswa", "Kelas", "Status", "Keterangan"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable table = new StyledTable(tableModel);
    private final StateDeckPanel stateDeck;

    private List<SchoolClass> authorizedClasses = List.of();
    private List<Student> allStudents = List.of();
    private Map<Long, Student> studentMap = Map.of();
    private Map<Long, String> classNames = Map.of();
    private List<Attendance> currentRows = List.of();
    private boolean initializedMeta = false;

    public AttendancePanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        feedbackBanner.setAlignmentX(LEFT_ALIGNMENT);
        top.add(feedbackBanner);
        top.add(Box.createVerticalStrut(6));

        ClayCard toolbar = buildToolbar();
        toolbar.setAlignmentX(LEFT_ALIGNMENT);
        top.add(toolbar);
        top.add(Box.createVerticalStrut(10));

        ClayCard strip = buildSummaryStrip();
        strip.setAlignmentX(LEFT_ALIGNMENT);
        top.add(strip);

        add(top, BorderLayout.NORTH);

        ClayCard tableCard = new ClayCard(new BorderLayout());
        table.setStatusColumns(5);
        table.setCenterAlignedColumns(0, 1, 2);
        table.setColumnWidths(52, 102, 96, 190, 90, 96, 210);
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());
        tableCard.add(table.wrapInScrollPane(), BorderLayout.CENTER);

        stateDeck = new StateDeckPanel(
                tableCard,
                "Belum ada catatan absensi",
                "Belum ada data absensi pada filter ini. Gunakan Input Absensi Kelas untuk mencatat kehadiran.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);

        modeCombo.addActionListener(e -> syncFilterVisibility());
        applyButton.addActionListener(e -> loadAttendanceRecords());
        batchButton.addActionListener(e -> onBatchRecord());
        singleButton.addActionListener(e -> onSingleRecord());
        editButton.addActionListener(e -> onEditSelected());
        syncFilterVisibility();
        updateButtons();
    }

    private ClayCard buildToolbar() {
        ClayCard card = new ClayCard(new BorderLayout(10, 10));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        filters.setOpaque(false);
        FormField.styleInputComponent(modeCombo);
        modeCombo.setPreferredSize(new Dimension(170, BotanicalTheme.INPUT_HEIGHT));
        FormField.styleInputComponent(classCombo);
        classCombo.setPreferredSize(new Dimension(175, BotanicalTheme.INPUT_HEIGHT));
        FormField.styleInputComponent(studentCombo);
        studentCombo.setPreferredSize(new Dimension(210, BotanicalTheme.INPUT_HEIGHT));
        startDateField.setPreferredSize(new Dimension(216, BotanicalTheme.INPUT_HEIGHT));
        endDateField.setPreferredSize(new Dimension(216, BotanicalTheme.INPUT_HEIGHT));

        filters.add(modeCombo);
        filters.add(classCombo);
        filters.add(studentCombo);
        filters.add(startDateField);
        filters.add(endDateField);
        filters.add(applyButton);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        actions.setOpaque(false);
        actions.add(editButton);
        actions.add(singleButton);
        actions.add(batchButton);

        card.add(filters, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private ClayCard buildSummaryStrip() {
        ClayCard card = new ClayCard(new GridLayout(1, 5, 12, 0));
        card.setSurfaceColor(BotanicalTheme.PALE_MINT);
        for (JLabel lbl : new JLabel[]{totalStat, hadirStat, sakitStat, izinStat, alpaStat}) {
            lbl.setFont(BotanicalTheme.FONT_BODY_BOLD);
            lbl.setForeground(BotanicalTheme.TEXT_PRIMARY);
            card.add(lbl);
        }
        hadirStat.setForeground(BotanicalTheme.SUCCESS);
        sakitStat.setForeground(BotanicalTheme.WARNING.darker());
        izinStat.setForeground(BotanicalTheme.EVERGREEN);
        alpaStat.setForeground(BotanicalTheme.DANGER);
        return card;
    }

    private void syncFilterVisibility() {
        ViewMode mode = (ViewMode) modeCombo.getSelectedItem();
        classCombo.setVisible(mode != ViewMode.STUDENT_HISTORY);
        studentCombo.setVisible(mode == ViewMode.STUDENT_HISTORY);
        startDateField.setVisible(mode == ViewMode.CLASS_AND_DATE || mode == ViewMode.CLASS_RANGE);
        endDateField.setVisible(mode == ViewMode.CLASS_RANGE);
        revalidate();
        repaint();
    }

    @Override
    public void refreshData() {
        feedbackBanner.clear();
        AsyncTaskRunner.run(
                this::loadReferenceMetadata,
                () -> stateDeck.showLoading("Memuat referensi kelas dan absensi..."),
                unused -> {
                    initializedMeta = true;
                    updateButtons();
                    loadAttendanceRecords();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private Void loadReferenceMetadata() {
        SessionContext session = container.getSessionContext();
        List<SchoolClass> allClasses = container.getAcademicService().getAllClasses().stream()
                .filter(SchoolClass::isActive)
                .toList();

        List<SchoolClass> scopedClasses;
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            scopedClasses = allClasses;
        } else {
            Long teacherId = session.resolveTeacherScopeOrNullForAdmin("absensi");
            Set<Long> allowedIds = new HashSet<>();
            for (HomeroomAssignment h : container.getAcademicService().getAllHomeroomAssignments()) {
                if (h.isActive() && teacherId.equals(h.getTeacherId())) {
                    allowedIds.add(h.getClassId());
                }
            }
            for (TeachingAssignment t : container.getAcademicService().getTeachingAssignmentsByTeacher(teacherId)) {
                if (t.isActive()) {
                    allowedIds.add(t.getClassId());
                }
            }
            scopedClasses = allClasses.stream().filter(c -> allowedIds.contains(c.getId())).toList();
        }

        List<Student> students = container.getStudentService().getAllStudents().stream()
                .filter(Student::isActive)
                .toList();

        Map<Long, Student> sMap = new HashMap<>();
        students.forEach(s -> sMap.put(s.getId(), s));
        Map<Long, String> cMap = new HashMap<>();
        container.getAcademicService().getAllClasses().forEach(c -> cMap.put(c.getId(), c.getName()));

        this.authorizedClasses = scopedClasses;
        this.allStudents = students;
        this.studentMap = sMap;
        this.classNames = cMap;

        Long prevClassId = getSelectedClassId();
        classCombo.removeAllItems();
        for (SchoolClass c : scopedClasses) {
            classCombo.addItem(new SelectItem<>(c, "Kelas " + c.getName() + " (Tingkat " + c.getGradeLevel() + ")"));
        }
        if (prevClassId != null) {
            for (int i = 0; i < classCombo.getItemCount(); i++) {
                if (classCombo.getItemAt(i).getValue().getId().equals(prevClassId)) {
                    classCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        studentCombo.removeAllItems();
        for (Student s : students) {
            studentCombo.addItem(new SelectItem<>(s, s.getNis() + " - " + s.getFullName()));
        }
        return null;
    }

    private void loadAttendanceRecords() {
        if (!initializedMeta) {
            refreshData();
            return;
        }
        ViewMode mode = (ViewMode) modeCombo.getSelectedItem();
        if (mode != ViewMode.STUDENT_HISTORY && authorizedClasses.isEmpty()) {
            currentRows = List.of();
            updateStatsFromRows(currentRows);
            stateDeck.showEmpty("Tidak ada kelas dalam cakupan", "Belum ada kelas aktif yang dapat diakses untuk absensi.");
            return;
        }

        Long classId = getSelectedClassId();
        Long studentId = getSelectedStudentId();
        LocalDate start = startDateField.getLocalDate();
        LocalDate end = endDateField.getLocalDate();

        AsyncTaskRunner.run(
                () -> {
                    AttendanceService service = container.getAttendanceService();
                    List<Attendance> rows;
                    AttendanceSummary summary = null;
                    switch (mode) {
                        case CLASS_AND_DATE -> rows = service.getAttendanceByClassAndDate(classId, start);
                        case CLASS_ALL -> rows = service.getAttendanceByClass(classId);
                        case STUDENT_HISTORY -> {
                            if (studentId == null) {
                                throw new IllegalArgumentException("Pilih siswa terlebih dahulu.");
                            }
                            rows = service.getAttendanceByStudent(studentId);
                        }
                        case CLASS_RANGE -> {
                            summary = service.getClassAttendanceSummary(classId, start, end);
                            rows = service.getAttendanceByClass(classId).stream()
                                    .filter(a -> !a.getAttendanceDate().isBefore(start)
                                            && !a.getAttendanceDate().isAfter(end))
                                    .toList();
                        }
                        default -> rows = List.of();
                    }
                    return new Object[]{rows, summary};
                },
                () -> stateDeck.showLoading("Mengambil catatan absensi..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<Attendance> rows = (List<Attendance>) arr[0];
                    AttendanceSummary summary = (AttendanceSummary) arr[1];
                    this.currentRows = rows;
                    if (summary != null) {
                        applySummaryStats(summary.getTotalRecords(), summary.getHadirCount(),
                                summary.getSakitCount(), summary.getIzinCount(), summary.getAlpaCount());
                    } else {
                        updateStatsFromRows(rows);
                    }

                    if (rows.isEmpty()) {
                        stateDeck.showEmpty();
                        updateButtons();
                        return;
                    }
                    tableModel.setRowCount(0);
                    for (Attendance a : rows) {
                        Student st = studentMap.get(a.getStudentId());
                        tableModel.addRow(new Object[]{
                                a.getId(),
                                String.valueOf(a.getAttendanceDate()),
                                st != null ? st.getNis() : "-",
                                st != null ? st.getFullName() : "Siswa #" + a.getStudentId(),
                                classNames.getOrDefault(a.getClassId(), "Kelas #" + a.getClassId()),
                                a.getStatus(),
                                a.getNotes() == null || a.getNotes().isBlank() ? "-" : a.getNotes()
                        });
                    }
                    updateButtons();
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }

    private void updateStatsFromRows(List<Attendance> rows) {
        int h = 0, s = 0, i = 0, a = 0;
        for (Attendance r : rows) {
            switch (r.getStatus()) {
                case Attendance.STATUS_HADIR -> h++;
                case Attendance.STATUS_SAKIT -> s++;
                case Attendance.STATUS_IZIN -> i++;
                case Attendance.STATUS_ALPA -> a++;
                default -> {
                }
            }
        }
        applySummaryStats(rows.size(), h, s, i, a);
    }

    private void applySummaryStats(int total, int hadir, int sakit, int izin, int alpa) {
        totalStat.setText("Total: " + total);
        hadirStat.setText("Hadir: " + hadir);
        sakitStat.setText("Sakit: " + sakit);
        izinStat.setText("Izin: " + izin);
        alpaStat.setText("Alpa: " + alpa);
    }

    private Long getSelectedClassId() {
        @SuppressWarnings("unchecked")
        SelectItem<SchoolClass> item = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
        return item != null ? item.getValue().getId() : null;
    }

    private Long getSelectedStudentId() {
        @SuppressWarnings("unchecked")
        SelectItem<Student> item = (SelectItem<Student>) studentCombo.getSelectedItem();
        return item != null ? item.getValue().getId() : null;
    }

    private Attendance getSelectedAttendance() {
        int row = table.getSelectedRow();
        return (row >= 0 && row < currentRows.size()) ? currentRows.get(row) : null;
    }

    private void updateButtons() {
        boolean hasClass = !authorizedClasses.isEmpty();
        batchButton.setEnabled(hasClass);
        singleButton.setEnabled(hasClass);
        editButton.setEnabled(getSelectedAttendance() != null);
    }

    private void onBatchRecord() {
        Long selectedClassId = getSelectedClassId();
        LocalDate date = startDateField.getLocalDate();
        BatchAttendanceDialog dialog = new BatchAttendanceDialog(
                this, container, authorizedClasses, selectedClassId, date, studentMap);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Absensi kelas berhasil dicatat.");
            loadAttendanceRecords();
        }
    }

    private void onSingleRecord() {
        SingleAttendanceDialog dialog = new SingleAttendanceDialog(
                this, container, authorizedClasses, getSelectedClassId(), startDateField.getLocalDate(), studentMap);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Absensi siswa berhasil dicatat.");
            loadAttendanceRecords();
        }
    }

    private void onEditSelected() {
        Attendance selected = getSelectedAttendance();
        if (selected == null) {
            return;
        }
        Student st = studentMap.get(selected.getStudentId());
        String sLabel = st != null ? st.getFullName() : "Siswa #" + selected.getStudentId();
        EditAttendanceDialog dialog = new EditAttendanceDialog(this, container, selected, sLabel);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            feedbackBanner.showSuccess("Absensi \"" + sLabel + "\" berhasil diperbarui.");
            loadAttendanceRecords();
        }
    }

    private static final class BatchAttendanceDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Map<Long, Student> studentMap;
        private final JComboBox<SelectItem<SchoolClass>> classCombo = new JComboBox<>();
        private final DateField dateField = new DateField(LocalDate.now());
        private final DefaultTableModel batchModel = new DefaultTableModel(
                new String[]{"ID Siswa", "NIS", "Nama Siswa", "Status", "Keterangan"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 4;
            }
        };
        private final StyledTable batchTable = new StyledTable(batchModel);

        private BatchAttendanceDialog(
                Component owner,
                ApplicationContainer container,
                List<SchoolClass> classes,
                Long initialClassId,
                LocalDate initialDate,
                Map<Long, Student> studentMap) {
            super(owner, "Input Absensi Kelas (Massal)", "Simpan Absensi Kelas", 700, 540);
            this.container = container;
            this.studentMap = studentMap;
            classes.forEach(c -> classCombo.addItem(new SelectItem<>(c, "Kelas " + c.getName())));
            if (initialClassId != null) {
                for (int i = 0; i < classCombo.getItemCount(); i++) {
                    if (classCombo.getItemAt(i).getValue().getId().equals(initialClassId)) {
                        classCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (initialDate != null) {
                dateField.setLocalDate(initialDate);
            }

            addSectionCard("Pilih Kelas & Tanggal",
                    new FormField("Kelas", classCombo, true),
                    new FormField("Tanggal Absensi", dateField, true));

            JComboBox<String> statusEditor = new JComboBox<>(new String[]{
                    Attendance.STATUS_HADIR, Attendance.STATUS_SAKIT, Attendance.STATUS_IZIN, Attendance.STATUS_ALPA});
            batchTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(statusEditor));
            batchTable.setColumnWidths(68, 96, 200, 96, 180);

            ClayCard tableWrap = new ClayCard(new BorderLayout(0, 8));
            tableWrap.setPreferredSize(new Dimension(620, 260));
            JLabel hint = new JLabel("Ubah kolom Status (HADIR/SAKIT/IZIN/ALPA) dan Keterangan langsung pada baris tabel:");
            hint.setFont(BotanicalTheme.FONT_SMALL);
            hint.setForeground(BotanicalTheme.TEXT_SECONDARY);
            tableWrap.add(hint, BorderLayout.NORTH);
            tableWrap.add(batchTable.wrapInScrollPane(), BorderLayout.CENTER);
            addSectionCard("Daftar Siswa Belum Terabsen", fullWidth(tableWrap));

            classCombo.addActionListener(e -> loadEligibleStudents());
            loadEligibleStudents();
        }

        private void loadEligibleStudents() {
            feedbackBanner.clear();
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> cItem = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            if (cItem == null) {
                return;
            }
            Long classId = cItem.getValue().getId();
            LocalDate date = dateField.getLocalDate();

            AsyncTaskRunner.run(
                    () -> {
                        List<StudentPlacement> placements = container.getAcademicService().getActivePlacementsByClass(classId);
                        Set<Long> alreadyRecorded = new HashSet<>();
                        for (Attendance a : container.getAttendanceService().getAttendanceByClassAndDate(classId, date)) {
                            alreadyRecorded.add(a.getStudentId());
                        }
                        List<Student> eligible = new ArrayList<>();
                        for (StudentPlacement p : placements) {
                            if (!alreadyRecorded.contains(p.getStudentId())) {
                                Student st = studentMap.get(p.getStudentId());
                                if (st != null && st.isActive()) {
                                    eligible.add(st);
                                }
                            }
                        }
                        return eligible;
                    },
                    () -> batchModel.setRowCount(0),
                    eligible -> {
                        batchModel.setRowCount(0);
                        for (Student st : eligible) {
                            batchModel.addRow(new Object[]{st.getId(), st.getNis(), st.getFullName(), Attendance.STATUS_HADIR, ""});
                        }
                        if (eligible.isEmpty()) {
                            feedbackBanner.showInfo("Semua siswa aktif di kelas ini sudah memiliki catatan absensi pada tanggal tersebut (atau belum ada siswa ditempatkan).");
                        }
                    },
                    error -> feedbackBanner.showError(error));
        }

        @Override
        protected void onSubmit() {
            if (batchTable.isEditing()) {
                batchTable.getCellEditor().stopCellEditing();
            }
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> cItem = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            if (cItem == null) {
                feedbackBanner.showError("Pilih kelas terlebih dahulu.");
                return;
            }
            if (batchModel.getRowCount() == 0) {
                feedbackBanner.showError("Tidak ada siswa untuk dicatat absensinya.");
                return;
            }
            List<AttendanceService.StudentAttendanceEntry> entries = new ArrayList<>();
            for (int r = 0; r < batchModel.getRowCount(); r++) {
                Long studentId = (Long) batchModel.getValueAt(r, 0);
                String status = String.valueOf(batchModel.getValueAt(r, 3));
                String notes = String.valueOf(batchModel.getValueAt(r, 4));
                entries.add(new AttendanceService.StudentAttendanceEntry(studentId, status, notes));
            }
            executeSave(() -> container.getAttendanceService().recordClassAttendance(
                    cItem.getValue().getId(), dateField.getLocalDate(), entries), null);
        }
    }

    private static final class SingleAttendanceDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Map<Long, Student> studentMap;
        private final JComboBox<SelectItem<SchoolClass>> classCombo = new JComboBox<>();
        private final JComboBox<SelectItem<Student>> studentCombo = new JComboBox<>();
        private final DateField dateInput = new DateField(LocalDate.now());
        private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
                Attendance.STATUS_HADIR, Attendance.STATUS_SAKIT, Attendance.STATUS_IZIN, Attendance.STATUS_ALPA});
        private final JTextField notesInput = new JTextField();

        private SingleAttendanceDialog(
                Component owner,
                ApplicationContainer container,
                List<SchoolClass> classes,
                Long initialClassId,
                LocalDate initialDate,
                Map<Long, Student> studentMap) {
            super(owner, "Catat Absensi Siswa", "Simpan Absensi", 540, 440);
            this.container = container;
            this.studentMap = studentMap;
            classes.forEach(c -> classCombo.addItem(new SelectItem<>(c, "Kelas " + c.getName())));
            if (initialClassId != null) {
                for (int i = 0; i < classCombo.getItemCount(); i++) {
                    if (classCombo.getItemAt(i).getValue().getId().equals(initialClassId)) {
                        classCombo.setSelectedIndex(i);
                        break;
                    }
                }
            }
            if (initialDate != null) {
                dateInput.setLocalDate(initialDate);
            }

            addSectionCard(
                    "Detail Absensi",
                    new FormField("Kelas", classCombo, true),
                    new FormField("Tanggal", dateInput, true),
                    fullWidth(new FormField("Siswa", studentCombo, true)),
                    new FormField("Status Kehadiran", statusCombo, true),
                    new FormField("Keterangan", notesInput, false));

            classCombo.addActionListener(e -> loadClassStudents());
            loadClassStudents();
        }

        private void loadClassStudents() {
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> cItem = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            if (cItem == null) {
                return;
            }
            studentCombo.removeAllItems();
            AsyncTaskRunner.run(
                    () -> container.getAcademicService().getActivePlacementsByClass(cItem.getValue().getId()),
                    null,
                    placements -> {
                        studentCombo.removeAllItems();
                        for (StudentPlacement p : placements) {
                            Student s = studentMap.get(p.getStudentId());
                            if (s != null) {
                                studentCombo.addItem(new SelectItem<>(s, s.getNis() + " - " + s.getFullName()));
                            }
                        }
                    },
                    error -> feedbackBanner.showError(error));
        }

        @Override
        protected void onSubmit() {
            @SuppressWarnings("unchecked")
            SelectItem<SchoolClass> cItem = (SelectItem<SchoolClass>) classCombo.getSelectedItem();
            @SuppressWarnings("unchecked")
            SelectItem<Student> sItem = (SelectItem<Student>) studentCombo.getSelectedItem();
            if (cItem == null || sItem == null) {
                feedbackBanner.showError("Kelas dan siswa wajib dipilih.");
                return;
            }
            executeSave(() -> container.getAttendanceService().recordAttendance(
                    sItem.getValue().getId(),
                    cItem.getValue().getId(),
                    dateInput.getLocalDate(),
                    (String) statusCombo.getSelectedItem(),
                    notesInput.getText()), null);
        }
    }

    private static final class EditAttendanceDialog extends ModalFormDialog {

        private final ApplicationContainer container;
        private final Attendance editing;
        private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{
                Attendance.STATUS_HADIR, Attendance.STATUS_SAKIT, Attendance.STATUS_IZIN, Attendance.STATUS_ALPA});
        private final JTextField notesInput = new JTextField();

        private EditAttendanceDialog(Component owner, ApplicationContainer container, Attendance editing, String studentName) {
            super(owner, "Ubah Status Absensi", "Perbarui", 470, 320);
            this.container = container;
            this.editing = editing;
            statusCombo.setSelectedItem(editing.getStatus());
            notesInput.setText(editing.getNotes());
            addSectionCard(
                    studentName + " (" + editing.getAttendanceDate() + ")",
                    fullWidth(new FormField("Status Kehadiran", statusCombo, true)),
                    fullWidth(new FormField("Keterangan", notesInput, false)));
        }

        @Override
        protected void onSubmit() {
            executeSave(() -> container.getAttendanceService().updateAttendance(
                    editing.getId(), (String) statusCombo.getSelectedItem(), notesInput.getText()), null);
        }
    }
}

