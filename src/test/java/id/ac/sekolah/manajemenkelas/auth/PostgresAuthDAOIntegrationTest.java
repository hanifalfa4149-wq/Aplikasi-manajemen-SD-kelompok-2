package id.ac.sekolah.manajemenkelas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.auth.dao.PostgresAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.teacher.dao.PostgresTeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

class PostgresAuthDAOIntegrationTest {

    private static final String TEST_TEACHER_USER = "test_phase1_guru";
    private static final String TEST_INACTIVE_USER = "test_phase1_nonaktif";

    @BeforeEach
    void cleanTestUsersBefore() throws SQLException {
        SessionContext.getInstance().endSession();
        deleteTestUsers();
    }

    @AfterEach
    void cleanTestUsersAfter() throws SQLException {
        SessionContext.getInstance().endSession();
        deleteTestUsers();
    }

    private void deleteTestUsers() throws SQLException {
        try (Connection conn = DatabaseConfig.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM pengguna WHERE nama_pengguna LIKE 'test_phase1_%'")) {
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM guru WHERE nip LIKE 'TEST_AUTH_GURU_%'")) {
                ps.executeUpdate();
            }
        }
    }

    @Test
    void bootstrapSeederIsIdempotentAndSeedsAdminAndTeacherInPostgres() {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        AuthBootstrapSeeder seeder = new AuthBootstrapSeeder(authDAO, hasher);

        seeder.seedDefaultUsers();

        User adminFirst = authDAO.findByUsername(AppConfig.DEFAULT_ADMIN_USERNAME).orElseThrow();
        User teacherFirst = authDAO.findByUsername(AppConfig.DEFAULT_TEACHER_USERNAME).orElseThrow();

        assertNotNull(adminFirst.getId());
        assertNotNull(teacherFirst.getId());
        assertEquals(RoleMapper.ROLE_ADMIN, adminFirst.getRole());
        assertEquals(RoleMapper.ROLE_TEACHER, teacherFirst.getRole());
        assertNotEquals(AppConfig.DEFAULT_ADMIN_PASSWORD, adminFirst.getPasswordHash());
        assertNotEquals(AppConfig.DEFAULT_TEACHER_PASSWORD, teacherFirst.getPasswordHash());

        // Second execution must be idempotent and preserve existing IDs and hashes
        seeder.seedDefaultUsers();

        User adminSecond = authDAO.findByUsername(AppConfig.DEFAULT_ADMIN_USERNAME).orElseThrow();
        User teacherSecond = authDAO.findByUsername(AppConfig.DEFAULT_TEACHER_USERNAME).orElseThrow();

        assertEquals(adminFirst.getId(), adminSecond.getId());
        assertEquals(adminFirst.getPasswordHash(), adminSecond.getPasswordHash());
        assertEquals(teacherFirst.getId(), teacherSecond.getId());
        assertEquals(teacherFirst.getPasswordHash(), teacherSecond.getPasswordHash());
    }

    @Test
    void hashSaltDatabaseRoundTripAndRoleMappingWorkAndSurviveRestart() throws SQLException {
        PasswordHasher hasher = new PasswordHasher();
        PostgresAuthDAO writerDao = new PostgresAuthDAO();

        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("RahasiaGuru#2026", salt);

        User newTeacherUser = new User(
                null,
                TEST_TEACHER_USER,
                hash,
                salt,
                "Budi Santoso, S.Pd.",
                RoleMapper.ROLE_TEACHER,
                true);

        writerDao.save(newTeacherUser);
        assertNotNull(newTeacherUser.getId());

        // Verify physical column value in PostgreSQL is 'GURU' and not plaintext password
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT peran, hash_kata_sandi, salt_kata_sandi, terakhir_login "
                             + "FROM pengguna WHERE id = ?")) {
            ps.setLong(1, newTeacherUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                assertEquals("GURU", rs.getString("peran"));
                assertEquals(hash, rs.getString("hash_kata_sandi"));
                assertEquals(salt, rs.getString("salt_kata_sandi"));
                assertNull(rs.getTimestamp("terakhir_login"));
            }
        }

        // Simulate application restart with fresh DAO and AuthService instances
        PostgresAuthDAO restartedDao = new PostgresAuthDAO();
        AuthService restartedAuthService = new AuthService(restartedDao, new PasswordHasher());

        assertFalse(restartedAuthService.login(TEST_TEACHER_USER, "PasswordSalah"));
        assertFalse(restartedAuthService.isAuthenticated());

        assertTrue(restartedAuthService.login(TEST_TEACHER_USER, "RahasiaGuru#2026"));
        assertTrue(restartedAuthService.isAuthenticated());
        assertTrue(restartedAuthService.hasRole(RoleMapper.ROLE_TEACHER));
        assertEquals("Budi Santoso, S.Pd.", SessionContext.getInstance().getFullName());

        // Verify terakhir_login was persisted in PostgreSQL
        User afterLogin = restartedDao.findByUsername(TEST_TEACHER_USER).orElseThrow();
        assertNotNull(afterLogin.getLastLogin());
        assertEquals(RoleMapper.ROLE_TEACHER, afterLogin.getRole());
    }

    @Test
    void inactiveDatabaseUserCannotLogIn() {
        PasswordHasher hasher = new PasswordHasher();
        PostgresAuthDAO authDAO = new PostgresAuthDAO();

        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("Nonaktif#123", salt);

        User inactiveUser = new User(
                null,
                TEST_INACTIVE_USER,
                hash,
                salt,
                "Akun Nonaktif",
                RoleMapper.ROLE_TEACHER,
                false);

        authDAO.save(inactiveUser);
        assertNotNull(inactiveUser.getId());

        AuthService authService = new AuthService(authDAO, hasher);
        assertFalse(authService.login(TEST_INACTIVE_USER, "Nonaktif#123"));
        assertFalse(authService.isAuthenticated());
    }

    @Test
    void adminCanSaveUpdateAndLogInWithLinkedTeacherAccount() {
        PostgresAuthDAO authDAO = new PostgresAuthDAO();
        PostgresTeacherDAO teacherDAO = new PostgresTeacherDAO();
        PasswordHasher hasher = new PasswordHasher();
        new AuthBootstrapSeeder(authDAO, hasher).seedDefaultUsers();
        AuthService authService = new AuthService(authDAO, hasher);

        Teacher guruBaru = new Teacher(
                null,
                "TEST_AUTH_GURU_01",
                "Siti Nurhaliza, S.Pd.",
                "P",
                "siti.test@sekolah.sch.id",
                "08123456789",
                "Jl. Pendidikan No. 1",
                "S1 PGSD",
                java.time.LocalDate.of(2025, 7, 1),
                true);
        teacherDAO.save(guruBaru);
        assertNotNull(guruBaru.getId());

        assertTrue(authService.login(AppConfig.DEFAULT_ADMIN_USERNAME, AppConfig.DEFAULT_ADMIN_PASSWORD));
        User akun = authService.saveOrUpdateTeacherAccount(
                guruBaru.getId(),
                "test_phase1_siti",
                "SandiGuru#1",
                guruBaru.getFullName(),
                true);
        assertNotNull(akun.getId());
        assertEquals(guruBaru.getId(), akun.getTeacherId());
        assertTrue(authService.getTeacherAccountsMap().containsKey(guruBaru.getId()));

        authService.saveOrUpdateTeacherAccount(
                guruBaru.getId(),
                "test_phase1_siti_v2",
                "SandiGuru#2",
                guruBaru.getFullName(),
                true);
        authService.logout();

        assertFalse(authService.login("test_phase1_siti", "SandiGuru#1"));
        assertTrue(authService.login("test_phase1_siti_v2", "SandiGuru#2"));
        assertEquals(RoleMapper.ROLE_TEACHER, SessionContext.getInstance().getRole());
        assertEquals(guruBaru.getId(), SessionContext.getInstance().getTeacherId());
    }
}

