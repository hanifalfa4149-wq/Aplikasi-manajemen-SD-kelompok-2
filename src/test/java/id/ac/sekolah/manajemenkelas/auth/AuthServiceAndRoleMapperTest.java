package id.ac.sekolah.manajemenkelas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.auth.service.AuthBootstrapSeeder;
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
    void seededUsersStoreHashedPasswordsAndAuthenticateCorrectly() {
        StubAuthDAO authDAO = new StubAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        new AuthBootstrapSeeder(authDAO, hasher).seedDefaultUsers();
        AuthService authService = new AuthService(authDAO, hasher);

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
        StubAuthDAO authDAO = new StubAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        new AuthBootstrapSeeder(authDAO, hasher).seedDefaultUsers();
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
    void registerCreatesAccountAndLoginEnforcesSelectedRole() {
        StubAuthDAO authDAO = new StubAuthDAO();
        PasswordHasher hasher = new PasswordHasher();
        AuthService authService = new AuthService(authDAO, hasher);

        User guruBaru = authService.register("guru_baru", "sandi123", RoleMapper.ROLE_TEACHER);
        User adminBaru = authService.register("admin_baru", "sandi456", RoleMapper.ROLE_ADMIN);

        assertNotNull(guruBaru.getId());
        assertEquals(RoleMapper.ROLE_TEACHER, guruBaru.getRole());
        assertEquals(RoleMapper.ROLE_ADMIN, adminBaru.getRole());

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("guru_baru", "lain123", RoleMapper.ROLE_TEACHER));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("ab", "1234", RoleMapper.ROLE_TEACHER));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("guru baru", "1234", RoleMapper.ROLE_TEACHER));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user_ok", "123", RoleMapper.ROLE_ADMIN));

        assertThrows(IllegalArgumentException.class,
                () -> authService.login("guru_baru", "sandi123", RoleMapper.ROLE_ADMIN));
        assertFalse(authService.isAuthenticated());

        assertTrue(authService.login("guru_baru", "sandi123", RoleMapper.ROLE_TEACHER));
        assertTrue(authService.isAuthenticated());
        assertTrue(authService.hasRole(RoleMapper.ROLE_TEACHER));
        authService.logout();

        assertTrue(authService.login("admin_baru", "sandi456", RoleMapper.ROLE_ADMIN));
        assertTrue(authService.hasRole(RoleMapper.ROLE_ADMIN));
    }

    private static final class StubAuthDAO implements AuthDAO {

        private final Map<String, User> usersByUsername = new HashMap<>();
        private long nextId = 1L;

        @Override
        public Optional<User> findByUsername(String username) {
            if (username == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(usersByUsername.get(username));
        }

        @Override
        public Optional<User> findByTeacherId(Long teacherId) {
            if (teacherId == null) {
                return Optional.empty();
            }
            for (User user : usersByUsername.values()) {
                if (teacherId.equals(user.getTeacherId())) {
                    return Optional.of(user);
                }
            }
            return Optional.empty();
        }

        @Override
        public List<User> findAll() {
            return new ArrayList<>(usersByUsername.values());
        }

        @Override
        public boolean existsByUsername(String username) {
            return username != null && usersByUsername.containsKey(username);
        }

        @Override
        public void save(User user) {
            if (user.getId() == null) {
                user.setId(nextId++);
            }
            usersByUsername.put(user.getUsername(), user);
        }

        @Override
        public void update(User user) {
            if (user == null || user.getId() == null) {
                return;
            }
            String keyLama = null;
            for (Map.Entry<String, User> entry : usersByUsername.entrySet()) {
                if (user.getId().equals(entry.getValue().getId())) {
                    keyLama = entry.getKey();
                    break;
                }
            }
            if (keyLama != null) {
                usersByUsername.remove(keyLama);
            }
            usersByUsername.put(user.getUsername(), user);
        }

        @Override
        public void updateLastLogin(Long userId) {
            for (User user : usersByUsername.values()) {
                if (userId != null && userId.equals(user.getId())) {
                    user.setLastLogin(LocalDateTime.now());
                    break;
                }
            }
        }
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

