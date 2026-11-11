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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.academic.dao.PostgresAcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;

class PostgresAcademicDAOIntegrationTest {

    private Long previouslyActiveYearId;

    @BeforeEach
    void setupSnapshotAndCleanup() throws SQLException {
        cleanupTestAcademicRows();
        previouslyActiveYearId = findActiveNonTestYearId();
    }

    @AfterEach
    void cleanupAndRestoreActiveYear() throws SQLException {
        cleanupTestAcademicRows();
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
                     "SELECT id FROM tahun_ajaran WHERE aktif = TRUE AND nama NOT LIKE 'TEST_P2_%'");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong("id") : null;
        }
    }

    private void cleanupTestAcademicRows() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM kelas WHERE nama LIKE 'TEST_P2_%' "
                            + "OR id_tahun_ajaran IN (SELECT id FROM tahun_ajaran WHERE nama LIKE 'TEST_P2_%')")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM tahun_ajaran WHERE nama LIKE 'TEST_P2_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM mata_pelajaran WHERE kode LIKE 'TEST_P2_%'")) {
                ps.executeUpdate();
            }
        }
    }

    @Test
    void academicYearClassAndSubjectCrudPersistAcrossRestart() {
        AcademicService writerService = new AcademicService(new PostgresAcademicDAO());

        AcademicYear year1 = writerService.createAcademicYear(
                "TEST_P2_2025/2026",
                LocalDate.of(2025, 7, 1),
                LocalDate.of(2026, 6, 30));
        assertNotNull(year1.getId());
        assertTrue(year1.isActive());

        // Creating second active year deactivates the first atomically (respects idx_tahun_ajaran_aktif)
        AcademicYear year2 = writerService.createAcademicYear(
                "TEST_P2_2026/2027",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2027, 6, 30));
        assertNotNull(year2.getId());
        assertTrue(year2.isActive());

        SchoolClass schoolClass = writerService.createClass(
                year2.getId(),
                "TEST_P2_1A",
                1,
                28);
        assertNotNull(schoolClass.getId());

        Subject subject = writerService.createSubject(
                "TEST_P2_MTK",
                "Matematika",
                "Matematika dasar kelas 1-6");
        assertNotNull(subject.getId());

        // Restart simulation
        AcademicService restartedService = new AcademicService(new PostgresAcademicDAO());
        assertFalse(restartedService.getAcademicYearById(year1.getId()).orElseThrow().isActive());
        assertTrue(restartedService.getAcademicYearById(year2.getId()).orElseThrow().isActive());
        assertEquals("TEST_P2_1A", restartedService.getClassesByAcademicYear(year2.getId()).get(0).getName());
        assertEquals("Matematika", restartedService.getSubjectById(subject.getId()).orElseThrow().getName());

        // Update & deactivate
        restartedService.updateClass(schoolClass.getId(), year2.getId(), "TEST_P2_1A_UPD", 2, 30);
        restartedService.deactivateClass(schoolClass.getId());
        SchoolClass updatedClass = restartedService.getClassesByAcademicYear(year2.getId()).get(0);
        assertEquals("TEST_P2_1A_UPD", updatedClass.getName());
        assertFalse(updatedClass.isActive());

        restartedService.updateSubject(subject.getId(), "TEST_P2_MTK", "Matematika SD", "Updated");
        restartedService.deactivateSubject(subject.getId());
        Subject updatedSubject = restartedService.getSubjectById(subject.getId()).orElseThrow();
        assertEquals("Matematika SD", updatedSubject.getName());
        assertFalse(updatedSubject.isActive());
    }

    @Test
    void foreignKeyConstraintOnClassIsEnforcedAtDaoAndServiceLevel() {
        PostgresAcademicDAO dao = new PostgresAcademicDAO();
        SchoolClass invalidClass = new SchoolClass(null, 99999999L, "TEST_P2_INVALID", 1, 20, true);

        assertThrows(IllegalArgumentException.class, () -> dao.saveClass(invalidClass));
    }
}

