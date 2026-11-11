package id.ac.sekolah.manajemenkelas.student;

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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.student.dao.PostgresStudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;

class PostgresStudentDAOIntegrationTest {

    @BeforeEach
    @AfterEach
    void cleanupTestStudents() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM siswa WHERE nis LIKE 'TEST_P2_%'")) {
            ps.executeUpdate();
        }
    }

    @Test
    void crudGeneratedIdGenderMappingAndRestartPersistenceWork() throws SQLException {
        PostgresStudentDAO writerDao = new PostgresStudentDAO();
        StudentService writerService = new StudentService(writerDao);

        Student created = writerService.createStudent(
                "TEST_P2_NIS_001",
                "TEST_P2_NISN_001",
                "Ani Lestari",
                "p",
                LocalDate.of(2016, 5, 14),
                "Pekanbaru",
                "Jl. Merpati No. 10",
                "Bapak Rudi",
                "081234567890");

        assertNotNull(created.getId());
        assertEquals("P", created.getGender());
        assertTrue(created.isActive());

        // Verify physical PostgreSQL enum string is PEREMPUAN
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT jenis_kelamin FROM siswa WHERE id = ?")) {
            ps.setLong(1, created.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("PEREMPUAN", rs.getString("jenis_kelamin"));
            }
        }

        // Simulate restart with new DAO and Service instances
        StudentService restartedService = new StudentService(new PostgresStudentDAO());
        Student loaded = restartedService.getStudentById(created.getId()).orElseThrow();
        assertEquals("Ani Lestari", loaded.getFullName());
        assertEquals("P", loaded.getGender());

        // Update
        restartedService.updateStudent(
                created.getId(),
                "TEST_P2_NIS_001",
                "TEST_P2_NISN_001",
                "Ani Lestari Putri",
                "L",
                LocalDate.of(2016, 5, 14),
                "Pekanbaru",
                "Jl. Sudirman No. 1",
                "Bapak Rudi",
                "081234567890");

        Student updated = restartedService.getStudentById(created.getId()).orElseThrow();
        assertEquals("Ani Lestari Putri", updated.getFullName());
        assertEquals("L", updated.getGender());

        // Soft deactivate
        restartedService.deleteStudent(created.getId());
        Student deactivated = restartedService.getStudentById(created.getId()).orElseThrow();
        assertFalse(deactivated.isActive());

        // Hard delete via DAO
        new PostgresStudentDAO().deleteById(created.getId());
        assertTrue(restartedService.getStudentById(created.getId()).isEmpty());
    }

    @Test
    void duplicateNisOrNisnIsRejected() {
        StudentService service = new StudentService(new PostgresStudentDAO());

        service.createStudent(
                "TEST_P2_NIS_002",
                "TEST_P2_NISN_002",
                "Siswa Pertama",
                "L",
                LocalDate.of(2016, 1, 10),
                "Pekanbaru",
                "Jl. Melati",
                "",
                "");

        assertThrows(IllegalArgumentException.class, () -> service.createStudent(
                "TEST_P2_NIS_002",
                "TEST_P2_NISN_999",
                "Siswa Duplikat NIS",
                "L",
                LocalDate.of(2016, 2, 10),
                "Pekanbaru",
                "Jl. Melati",
                "",
                ""));

        assertThrows(IllegalArgumentException.class, () -> service.createStudent(
                "TEST_P2_NIS_999",
                "TEST_P2_NISN_002",
                "Siswa Duplikat NISN",
                "P",
                LocalDate.of(2016, 2, 10),
                "Pekanbaru",
                "Jl. Melati",
                "",
                ""));
    }
}

