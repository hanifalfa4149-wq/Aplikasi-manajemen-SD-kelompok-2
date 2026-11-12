package id.ac.sekolah.manajemenkelas.auth.dao;

import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.model.User;

public interface AuthDAO {

    Optional<User> findByUsername(String username);

    Optional<User> findByTeacherId(Long teacherId);

    List<User> findAll();

    boolean existsByUsername(String username);

    void save(User user);

    void update(User user);

    void updateLastLogin(Long userId);
}