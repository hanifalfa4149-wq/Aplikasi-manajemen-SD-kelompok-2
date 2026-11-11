package id.ac.sekolah.manajemenkelas.presentation.swing.module.academic;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;

import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.presentation.swing.app.ApplicationContainer;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.AsyncTaskRunner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.ClayCard;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.FeedbackBanner;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StateDeckPanel;
import id.ac.sekolah.manajemenkelas.presentation.swing.component.StyledTable;
import id.ac.sekolah.manajemenkelas.presentation.swing.shell.ScreenPage;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;
import id.ac.sekolah.manajemenkelas.student.model.Student;

/**
 * Tampilan khusus peran TEACHER: menampilkan penugasan mengajar, wali kelas,
 * dan daftar siswa aktif pada kelas yang diampu guru tersebut.
 */
public class TeacherScopePanel extends JPanel implements ScreenPage {

    private final ApplicationContainer container;
    private final DefaultTableModel assignModel = new DefaultTableModel(
            new String[]{"ID Tugas", "Peran", "Kelas", "Mata Pelajaran / Semester", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable assignTable = new StyledTable(assignModel);

    private final DefaultTableModel studentModel = new DefaultTableModel(
            new String[]{"NIS", "Nama Lengkap", "L/P", "Kelas", "Orang Tua"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final StyledTable studentTable = new StyledTable(studentModel);
    private final StateDeckPanel stateDeck;

    public TeacherScopePanel(ApplicationContainer container) {
        this.container = container;
        setOpaque(false);
        setLayout(new BorderLayout());

        JPanel grid = new JPanel(new GridLayout(2, 1, 0, 16));
        grid.setOpaque(false);

        ClayCard assignCard = new ClayCard(new BorderLayout(0, 8));
        JLabel assignTitle = new JLabel("Penugasan Wali Kelas & Mengajar Aktif");
        assignTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        assignCard.add(assignTitle, BorderLayout.NORTH);
        assignTable.setStatusColumns(4);
        assignTable.setCenterAlignedColumns(0);
        assignCard.add(assignTable.wrapInScrollPane(), BorderLayout.CENTER);

        ClayCard studentCard = new ClayCard(new BorderLayout(0, 8));
        JLabel studentTitle = new JLabel("Daftar Siswa pada Kelas yang Diampu");
        studentTitle.setFont(BotanicalTheme.FONT_SUBTITLE);
        studentCard.add(studentTitle, BorderLayout.NORTH);
        studentTable.setCenterAlignedColumns(2, 3);
        studentCard.add(studentTable.wrapInScrollPane(), BorderLayout.CENTER);

        grid.add(assignCard);
        grid.add(studentCard);

        stateDeck = new StateDeckPanel(
                grid,
                "Belum ada kelas yang diampu",
                "Anda belum memiliki penugasan wali kelas atau mengajar aktif.");
        stateDeck.setRetryAction(this::refreshData);
        add(stateDeck, BorderLayout.CENTER);
    }

    @Override
    public void refreshData() {
        Long teacherId = container.getSessionContext().getTeacherId();
        if (teacherId == null) {
            stateDeck.showError("Akun guru ini belum terhubung dengan profil guru di database (kolom id_guru masih kosong).");
            return;
        }

        AsyncTaskRunner.run(
                () -> {
                    List<HomeroomAssignment> homerooms = container.getAcademicService().getAllHomeroomAssignments().stream()
                            .filter(h -> h.isActive() && teacherId.equals(h.getTeacherId()))
                            .toList();
                    List<TeachingAssignment> teachings = container.getAcademicService().getTeachingAssignmentsByTeacher(teacherId).stream()
                            .filter(TeachingAssignment::isActive)
                            .toList();
                    Map<Long, SchoolClass> classMap = new HashMap<>();
                    for (SchoolClass c : container.getAcademicService().getAllClasses()) {
                        classMap.put(c.getId(), c);
                    }
                    Map<Long, Subject> subjectMap = new HashMap<>();
                    for (Subject s : container.getAcademicService().getAllSubjects()) {
                        subjectMap.put(s.getId(), s);
                    }
                    Map<Long, Semester> semMap = new HashMap<>();
                    for (Semester s : container.getAcademicService().getAllSemesters()) {
                        semMap.put(s.getId(), s);
                    }
                    Map<Long, Student> studentMap = new HashMap<>();
                    for (Student s : container.getStudentService().getAllStudents()) {
                        studentMap.put(s.getId(), s);
                    }
                    Set<Long> classIds = new LinkedHashSet<>();
                    homerooms.forEach(h -> classIds.add(h.getClassId()));
                    teachings.forEach(t -> classIds.add(t.getClassId()));

                    List<Object[]> studentRows = new java.util.ArrayList<>();
                    for (Long classId : classIds) {
                        SchoolClass sc = classMap.get(classId);
                        String cName = sc != null ? sc.getName() : "Kelas #" + classId;
                        for (StudentPlacement p : container.getAcademicService().getActivePlacementsByClass(classId)) {
                            Student st = studentMap.get(p.getStudentId());
                            if (st != null) {
                                studentRows.add(new Object[]{
                                        st.getNis(),
                                        st.getFullName(),
                                        st.getGender(),
                                        cName,
                                        st.getParentName() == null || st.getParentName().isBlank() ? "-" : st.getParentName()
                                });
                            }
                        }
                    }
                    return new Object[]{homerooms, teachings, classMap, subjectMap, semMap, studentRows};
                },
                () -> stateDeck.showLoading("Memuat kelas dan siswa yang diampu..."),
                res -> {
                    Object[] arr = (Object[]) res;
                    @SuppressWarnings("unchecked")
                    List<HomeroomAssignment> homerooms = (List<HomeroomAssignment>) arr[0];
                    @SuppressWarnings("unchecked")
                    List<TeachingAssignment> teachings = (List<TeachingAssignment>) arr[1];
                    @SuppressWarnings("unchecked")
                    Map<Long, SchoolClass> classMap = (Map<Long, SchoolClass>) arr[2];
                    @SuppressWarnings("unchecked")
                    Map<Long, Subject> subjectMap = (Map<Long, Subject>) arr[3];
                    @SuppressWarnings("unchecked")
                    Map<Long, Semester> semMap = (Map<Long, Semester>) arr[4];
                    @SuppressWarnings("unchecked")
                    List<Object[]> studentRows = (List<Object[]>) arr[5];

                    if (homerooms.isEmpty() && teachings.isEmpty()) {
                        stateDeck.showEmpty();
                        return;
                    }

                    assignModel.setRowCount(0);
                    for (HomeroomAssignment h : homerooms) {
                        SchoolClass sc = classMap.get(h.getClassId());
                        assignModel.addRow(new Object[]{
                                h.getId(),
                                "Wali Kelas",
                                sc != null ? sc.getName() : "Kelas #" + h.getClassId(),
                                "Sejak " + h.getStartDate(),
                                "AKTIF"
                        });
                    }
                    for (TeachingAssignment t : teachings) {
                        SchoolClass sc = classMap.get(t.getClassId());
                        Subject sub = subjectMap.get(t.getSubjectId());
                        Semester sem = semMap.get(t.getSemesterId());
                        String detail = (sub != null ? sub.getName() : "Mapel #" + t.getSubjectId())
                                + " (" + (sem != null ? sem.getName() : "Sem #" + t.getSemesterId()) + ")";
                        assignModel.addRow(new Object[]{
                                t.getId(),
                                "Guru Mapel",
                                sc != null ? sc.getName() : "Kelas #" + t.getClassId(),
                                detail,
                                "AKTIF"
                        });
                    }

                    studentModel.setRowCount(0);
                    for (Object[] row : studentRows) {
                        studentModel.addRow(row);
                    }
                    stateDeck.showContent();
                },
                error -> stateDeck.showError(FeedbackBanner.extractMessage(error)));
    }
}

