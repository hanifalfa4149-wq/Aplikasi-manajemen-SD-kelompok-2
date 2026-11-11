package id.ac.sekolah.manajemenkelas.auth.dao;

import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.model.User;

public interface AuthDAO {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    void save(User user);

    void updateLastLogin(Long userId);
}