package id.ac.sekolah.manajemenkelas.academic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

class AcademicAssignmentAndPlacementIntegrationTest {

    private Long previouslyActiveYearId;

    @BeforeEach
    void setupAndClean() throws SQLException {
        SessionContext.getInstance().endSession();
        cleanupTestRows();
        previouslyActiveYearId = findActiveNonTestYearId();
    }

    @AfterEach
    void teardownAndRestore() throws SQLException {
        SessionContext.getInstance().endSession();
        cleanupTestRows();
        if (previouslyActiveYearId != null) {
            try (Connection conn = DatabaseConfig.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "UPDATE tahun_ajaran SET aktif = TRUE WHERE id = ?")) {
                ps.setLong(1, previouslyActiveYearId);
                ps.executeUpdate();
            }
        }
    }

    private Long findActiveNonTestYearId() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_P3_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM riwayat_kelas_siswa WHERE id_siswa IN "
                            + "(SELECT id FROM siswa WHERE nis LIKE 'TEST_P3_%') "
                            + "OR id_kelas IN (SELECT id FROM kelas WHERE nama LIKE 'TEST_P3_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_mengajar WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P3_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P3_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM penugasan_wali_kelas WHERE id_kelas IN "
                            + "(SELECT id FROM kelas WHERE nama LIKE 'TEST_P3_%') "
                            + "OR id_guru IN (SELECT id FROM guru WHERE nip LIKE 'TEST_P3_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM semester WHERE id_tahun_ajaran IN "
                            + "(SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P3_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_P3_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P3_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_P3_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_P3_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_P3_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM siswa WHERE nis LIKE 'TEST_P3_%'")) {
                ps.executeUpdate();
            }
        }
    }

    private AcademicService createService() {
        return new AcademicService(
                new PostgresAcademicDAO(),
                new PostgresStudentDAO(),
                new PostgresTeacherDAO());
    }

    @Test
    void semesterHomeroomAndTeachingAssignmentWorkflowsAndValidationsWork() {
        AcademicService academicService = createService();
        TeacherService teacherService = new TeacherService(new PostgresTeacherDAO());

        Teacher teacher1 = teacherService.createTeacher(
                "TEST_P3_NIP_01", "Guru Wali 1", "L", "", "", "Jl. A", "S1", LocalDate.of(2021, 1, 1));
        Teacher teacher2 = teacherService.createTeacher(
                "TEST_P3_NIP_02", "Guru Wali 2", "P", "", "", "Jl. B", "S1", LocalDate.of(2022, 1, 1));

        AcademicYear year1 = academicService.createAcademicYear(
                "TEST_P3_2025/2026", LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 30));
        SchoolClass class1A = academicService.createClass(year1.getId(), "TEST_P3_1A", 1, 30);

        AcademicYear year2 = academicService.createAcademicYear(
                "TEST_P3_2026/2027", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        SchoolClass class2A = academicService.createClass(year2.getId(), "TEST_P3_2A", 2, 30);

        // Semester validations & creation
        assertThrows(IllegalArgumentException.class, () -> academicService.createSemester(
                year2.getId(), 3, "Invalid", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20)));
        assertThrows(IllegalArgumentException.class, () -> academicService.createSemester(
                year2.getId(), 1, "Out of Range", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 20)));

        Semester sem1 = academicService.createSemester(
                year2.getId(), 1, "Ganjil 2026/2027", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20));
        assertNotNull(sem1.getId());
        assertThrows(IllegalArgumentException.class, () -> academicService.createSemester(
                year2.getId(), 1, "Duplikat", LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 20)));

        // Homeroom assignment: prevent overlapping active assignment, allow after ending
        HomeroomAssignment hr1 = academicService.assignHomeroomTeacher(
                class2A.getId(), teacher1.getId(), LocalDate.of(2026, 7, 1));
        assertNotNull(hr1.getId());
        assertTrue(academicService.getActiveHomeroomByClass(class2A.getId()).isPresent());

        assertThrows(IllegalArgumentException.class, () -> academicService.assignHomeroomTeacher(
                class2A.getId(), teacher2.getId(), LocalDate.of(2026, 8, 1)));

        academicService.endHomeroomAssignment(hr1.getId(), LocalDate.of(2026, 9, 30));
        assertTrue(academicService.getActiveHomeroomByClass(class2A.getId()).isEmpty());

        HomeroomAssignment hr2 = academicService.assignHomeroomTeacher(
                class2A.getId(), teacher2.getId(), LocalDate.of(2026, 10, 1));
        assertNotNull(hr2.getId());

        // Teaching assignment: must match academic year of semester and class
        Subject math = academicService.createSubject("TEST_P3_MTK", "Matematika", "Wajib");
        assertThrows(IllegalArgumentException.class, () -> academicService.createTeachingAssignment(
                teacher1.getId(), class1A.getId(), math.getId(), sem1.getId()));

        TeachingAssignment ta = academicService.createTeachingAssignment(
                teacher1.getId(), class2A.getId(), math.getId(), sem1.getId());
        assertNotNull(ta.getId());
        assertEquals(1, academicService.getTeachingAssignmentsByTeacher(teacher1.getId()).size());
        assertEquals(1, academicService.getTeachingAssignmentsByClass(class2A.getId()).size());
        assertEquals(1, academicService.getTeachingAssignmentsBySemester(sem1.getId()).size());

        // Duplicate teaching assignment prevented
        assertThrows(IllegalArgumentException.class, () -> academicService.createTeachingAssignment(
                teacher1.getId(), class2A.getId(), math.getId(), sem1.getId()));

        academicService.deactivateTeachingAssignment(ta.getId());
        assertFalse(academicService.getTeachingAssignmentById(ta.getId()).orElseThrow().isActive());
    }

    @Test
    void studentPlacementPromotionTransferAndHistoryWorkTransactionally() {
        AcademicService academicService = createService();
        StudentService studentService = new StudentService(new PostgresStudentDAO());

        Student student = studentService.createStudent(
                "TEST_P3_NIS_01", "TEST_P3_NISN_01", "Dewi Kartika", "P",
                LocalDate.of(2016, 4, 12), "Pekanbaru", "Jl. Anggrek", "Ibu Sari", "0812345");

        AcademicYear year = academicService.createAcademicYear(
                "TEST_P3_2026/2027_B", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        SchoolClass class1A = academicService.createClass(year.getId(), "TEST_P3_1A_B", 1, 2);
        SchoolClass class1B = academicService.createClass(year.getId(), "TEST_P3_1B_B", 1, 2);

        StudentPlacement p1 = academicService.placeStudentInClass(
                student.getId(), class1A.getId(), LocalDate.of(2026, 7, 10));
        assertNotNull(p1.getId());
        assertEquals(StudentPlacement.STATUS_AKTIF, p1.getStatus());

        // Prevent duplicate active placement
        assertThrows(IllegalArgumentException.class, () -> academicService.placeStudentInClass(
                student.getId(), class1B.getId(), LocalDate.of(2026, 7, 11)));

        // Transfer to class 1B atomically
        StudentPlacement p2 = academicService.promoteOrTransferStudent(
                student.getId(), class1B.getId(), LocalDate.of(2026, 8, 1), StudentPlacement.STATUS_PINDAH);
        assertNotNull(p2.getId());
        assertEquals(class1B.getId(), p2.getClassId());

        // History preserves both records
        List<StudentPlacement> history = academicService.getPlacementHistoryByStudent(student.getId());
        assertEquals(2, history.size());
        assertEquals(StudentPlacement.STATUS_PINDAH, history.get(0).getStatus());
        assertEquals(LocalDate.of(2026, 8, 1), history.get(0).getExitDate());
        assertEquals(StudentPlacement.STATUS_AKTIF, history.get(1).getStatus());

        // Current placement points to 1B
        StudentPlacement current = academicService.getCurrentPlacementByStudent(student.getId()).orElseThrow();
        assertEquals(class1B.getId(), current.getClassId());
        assertEquals(0, academicService.getActivePlacementsByClass(class1A.getId()).size());
        assertEquals(1, academicService.getActivePlacementsByClass(class1B.getId()).size());

        // Complete placement with LULUS
        academicService.completeStudentPlacement(
                student.getId(), LocalDate.of(2027, 6, 15), StudentPlacement.STATUS_LULUS);
        assertTrue(academicService.getCurrentPlacementByStudent(student.getId()).isEmpty());
    }

    @Test
    void teacherRoleCannotMutateAcademicConfiguration() {
        AcademicService academicService = createService();

        SessionContext.getInstance().startSession(99L, "guru", "Guru Demo", RoleMapper.ROLE_TEACHER);
        assertThrows(IllegalArgumentException.class, () -> academicService.createAcademicYear(
                "TEST_P3_FORBIDDEN", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30)));

        SessionContext.getInstance().startSession(1L, "admin", "Administrator", RoleMapper.ROLE_ADMIN);
        AcademicYear created = academicService.createAcademicYear(
                "TEST_P3_ADMIN_OK", LocalDate.of(2026, 7, 1), LocalDate.of(2027, 6, 30));
        assertNotNull(created.getId());
    }
}

