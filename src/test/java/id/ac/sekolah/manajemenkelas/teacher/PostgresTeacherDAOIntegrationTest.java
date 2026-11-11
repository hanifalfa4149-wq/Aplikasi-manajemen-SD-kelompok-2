package id.ac.sekolah.manajemenkelas.teacher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

class PostgresTeacherDAOIntegrationTest {

    @BeforeEach
    @AfterEach
    void cleanupTestTeachers() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM guru WHERE nip LIKE 'TEST_P2_%'")) {
            ps.executeUpdate();
        }
    }

    @Test
    void crudBlankEmailNormalizedToNullAndGenderMappingWork() throws SQLException {
        TeacherService writerService = new TeacherService(new PostgresTeacherDAO());

        Teacher t1 = writerService.createTeacher(
                "TEST_P2_NIP_001",
                "Guru Pertama",
                "l",
                "",
                "0811111111",
                "Jl. Pendidikan No. 1",
                "S1 PGSD",
                LocalDate.of(2020, 7, 1));

        // Second teacher with blank email must succeed (blank normalized to SQL NULL)
        Teacher t2 = writerService.createTeacher(
                "TEST_P2_NIP_002",
                "Guru Kedua",
                "P",
                "   ",
                "",
                "Jl. Pendidikan No. 2",
                "S1 Pendidikan Matematika",
                LocalDate.of(2021, 7, 1));

        assertNotNull(t1.getId());
        assertNotNull(t2.getId());
        assertEquals("L", t1.getGender());
        assertEquals("P", t2.getGender());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT jenis_kelamin, email FROM guru WHERE id = ?")) {
            ps.setLong(1, t1.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("LAKI_LAKI", rs.getString("jenis_kelamin"));
                assertNull(rs.getString("email"));
            }
        }

        // Simulate restart
        TeacherService restartedService = new TeacherService(new PostgresTeacherDAO());
        Teacher loaded = restartedService.getTeacherById(t1.getId()).orElseThrow();
        assertEquals("Guru Pertama", loaded.getFullName());
        assertEquals("", loaded.getEmail());

        restartedService.updateTeacher(
                t1.getId(),
                "TEST_P2_NIP_001",
                "Guru Pertama Updated",
                "L",
                "guru.pertama.p2@sekolah.sch.id",
                "0811111111",
                "Jl. Pendidikan No. 1",
                "S2 Pendidikan",
                LocalDate.of(2020, 7, 1));

        Teacher updated = restartedService.getTeacherById(t1.getId()).orElseThrow();
        assertEquals("Guru Pertama Updated", updated.getFullName());
        assertEquals("guru.pertama.p2@sekolah.sch.id", updated.getEmail());

        restartedService.deleteTeacher(t1.getId());
        assertFalse(restartedService.getTeacherById(t1.getId()).orElseThrow().isActive());
    }

    @Test
    void duplicateNipAndDuplicateEmailAreRejected() {
        TeacherService service = new TeacherService(new PostgresTeacherDAO());

        service.createTeacher(
                "TEST_P2_NIP_003",
                "Guru Tiga",
                "L",
                "guru.tiga.p2@sekolah.sch.id",
                "",
                "Jl. Mawar",
                "S1",
                LocalDate.of(2022, 1, 10));

        assertThrows(IllegalArgumentException.class, () -> service.createTeacher(
                "TEST_P2_NIP_003",
                "Guru Duplikat NIP",
                "P",
                "",
                "",
                "Jl. Mawar",
                "S1",
                LocalDate.of(2022, 1, 10)));

        assertThrows(IllegalArgumentException.class, () -> service.createTeacher(
                "TEST_P2_NIP_004",
                "Guru Duplikat Email",
                "P",
                "GURU.TIGA.P2@sekolah.sch.id",
                "",
                "Jl. Mawar",
                "S1",
                LocalDate.of(2022, 1, 10)));
    }
}

