package id.ac.sekolah.manajemenkelas.auth.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.config.AppConfig;

public class InMemoryAuthDAO implements AuthDAO {

    private final List<User> users = new ArrayList<>();

    public InMemoryAuthDAO() {
        initializeDefaultUsers();
    }

    private void initializeDefaultUsers() {

        users.add(new User(
                1L,
                AppConfig.DEFAULT_ADMIN_USERNAME,
                AppConfig.DEFAULT_ADMIN_PASSWORD,
                "Administrator",
                "ADMIN",
                true
        ));

        users.add(new User(
                2L,
                AppConfig.DEFAULT_TEACHER_USERNAME,
                AppConfig.DEFAULT_TEACHER_PASSWORD,
                "Guru Demo",
                "TEACHER",
                true
        ));
    }

    @Override
    public Optional<User> findByUsername(String username) {

        return users.stream()
                .filter(user -> user.getUsername()
                        .equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public boolean existsByUsername(String username) {

        return findByUsername(username).isPresent();
    }

    @Override
    public void save(User user) {

        users.add(user);
    }
}