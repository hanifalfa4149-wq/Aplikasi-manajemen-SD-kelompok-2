package id.ac.sekolah.manajemenkelas.auth.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;

public class InMemoryAuthDAO implements AuthDAO {

    private final List<User> users = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    private final PasswordHasher passwordHasher;

    public InMemoryAuthDAO() {
        this(new PasswordHasher());
    }

    public InMemoryAuthDAO(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {
        saveWithHashedPassword(
                AppConfig.DEFAULT_ADMIN_USERNAME,
                AppConfig.DEFAULT_ADMIN_PASSWORD,
                "Administrator",
                RoleMapper.ROLE_ADMIN);

        saveWithHashedPassword(
                AppConfig.DEFAULT_TEACHER_USERNAME,
                AppConfig.DEFAULT_TEACHER_PASSWORD,
                "Guru Demo",
                RoleMapper.ROLE_TEACHER);
    }

    private void saveWithHashedPassword(
            String username,
            String rawPassword,
            String fullName,
            String role) {
        String salt = passwordHasher.generateSalt();
        String hash = passwordHasher.hashPassword(rawPassword, salt);

        users.add(new User(
                idGenerator.getAndIncrement(),
                username,
                hash,
                salt,
                fullName,
                role,
                true));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        if (username == null) {
            return Optional.empty();
        }
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username).isPresent();
    }

    @Override
    public void save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }
        users.add(user);
    }

    @Override
    public void updateLastLogin(Long userId) {
        if (userId == null) {
            return;
        }
        users.stream()
                .filter(user -> userId.equals(user.getId()))
                .findFirst()
                .ifPresent(user -> user.setLastLogin(LocalDateTime.now()));
    }
}