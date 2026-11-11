package id.ac.sekolah.manajemenkelas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.auth.dao.InMemoryAuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthService;
import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

class AuthServiceAndRoleMapperTest {

    @AfterEach
    void clearSession() {
        SessionContext.getInstance().endSession();
    }

    @Test
    void inMemoryDaoStoresHashedPasswordsAndAuthenticatesDemoUsers() {
        InMemoryAuthDAO authDAO = new InMemoryAuthDAO();
        AuthService authService = new AuthService(authDAO);

        User admin = authDAO.findByUsername(AppConfig.DEFAULT_ADMIN_USERNAME).orElseThrow();
        assertNotEquals(AppConfig.DEFAULT_ADMIN_PASSWORD, admin.getPasswordHash());
        assertNotNull(admin.getPasswordSalt());
        assertFalse(admin.getPasswordSalt().isBlank());

        assertTrue(authService.login(
                AppConfig.DEFAULT_ADMIN_USERNAME,
                AppConfig.DEFAULT_ADMIN_PASSWORD));
        assertTrue(authService.isAuthenticated());
        assertTrue(authService.hasRole("ADMIN"));
        assertNotNull(admin.getLastLogin());

        authService.logout();
        assertFalse(authService.isAuthenticated());

        assertTrue(authService.login(
                AppConfig.DEFAULT_TEACHER_USERNAME,
                AppConfig.DEFAULT_TEACHER_PASSWORD));
        assertTrue(authService.hasRole("TEACHER"));
    }

    @Test
    void loginFailsForWrongPasswordEmptyPasswordOrInactiveUser() {
        InMemoryAuthDAO authDAO = new InMemoryAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        AuthService authService = new AuthService(authDAO, hasher);

        assertFalse(authService.login(AppConfig.DEFAULT_ADMIN_USERNAME, "salah"));
        assertFalse(authService.login(AppConfig.DEFAULT_ADMIN_USERNAME, ""));
        assertFalse(authService.login("", AppConfig.DEFAULT_ADMIN_PASSWORD));

        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("pass123", salt);
        authDAO.save(new User(null, "nonaktif", hash, salt, "Nonaktif", "TEACHER", false));

        assertFalse(authService.login("nonaktif", "pass123"));
        assertFalse(authService.isAuthenticated());
    }

    @Test
    void roleMapperConvertsBetweenJavaAndPostgresRoles() {
        assertEquals("ADMIN", RoleMapper.toDatabaseRole("ADMIN"));
        assertEquals("GURU", RoleMapper.toDatabaseRole("TEACHER"));
        assertEquals("ADMIN", RoleMapper.fromDatabaseRole("ADMIN"));
        assertEquals("TEACHER", RoleMapper.fromDatabaseRole("GURU"));

        assertThrows(IllegalArgumentException.class, () -> RoleMapper.toDatabaseRole("SISWA"));
        assertThrows(IllegalArgumentException.class, () -> RoleMapper.fromDatabaseRole("STAFF"));
    }
}

