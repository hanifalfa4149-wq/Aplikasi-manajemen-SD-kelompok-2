package id.ac.sekolah.manajemenkelas.auth.service;

import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.config.AppConfig;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

/**
 * Seeder idempoten untuk memastikan akun demo bawaan (admin dan guru)
 * tersedia dengan kata sandi yang sudah di-hash menggunakan PBKDF2.
 */
public class AuthBootstrapSeeder {

    private final AuthDAO authDAO;
    private final PasswordHasher passwordHasher;

    public AuthBootstrapSeeder(AuthDAO authDAO) {
        this(authDAO, new PasswordHasher());
    }

    public AuthBootstrapSeeder(AuthDAO authDAO, PasswordHasher passwordHasher) {
        if (authDAO == null) {
            throw new IllegalArgumentException("AuthDAO tidak boleh null.");
        }
        if (passwordHasher == null) {
            throw new IllegalArgumentException("PasswordHasher tidak boleh null.");
        }
        this.authDAO = authDAO;
        this.passwordHasher = passwordHasher;
    }

    public void seedDefaultUsers() {
        seedDefaultUsers(null);
    }

    public void seedDefaultUsers(TeacherDAO teacherDAO) {
        seedUserIfMissing(
                AppConfig.DEFAULT_ADMIN_USERNAME,
                AppConfig.DEFAULT_ADMIN_PASSWORD,
                "Administrator",
                RoleMapper.ROLE_ADMIN);

        seedUserIfMissing(
                AppConfig.DEFAULT_TEACHER_USERNAME,
                AppConfig.DEFAULT_TEACHER_PASSWORD,
                "Guru Demo",
                RoleMapper.ROLE_TEACHER);

        if (teacherDAO != null) {
            tautkanAkunGuruDemoJikaKosong(teacherDAO);
        }
    }

    private void tautkanAkunGuruDemoJikaKosong(TeacherDAO teacherDAO) {
        Optional<User> akunDemoOpt = authDAO.findByUsername(AppConfig.DEFAULT_TEACHER_USERNAME);
        if (akunDemoOpt.isEmpty()) {
            return;
        }
        User akunDemo = akunDemoOpt.get();
        if (!RoleMapper.ROLE_TEACHER.equals(akunDemo.getRole()) || akunDemo.getTeacherId() != null) {
            return;
        }

        Teacher kandidat = null;
        for (Teacher guru : teacherDAO.findAll()) {
            if (!guru.isActive()) {
                continue;
            }
            if (guru.getNip() != null && guru.getNip().startsWith("TEST_")) {
                continue;
            }
            if (authDAO.findByTeacherId(guru.getId()).isEmpty()) {
                kandidat = guru;
                break;
            }
        }

        if (kandidat != null) {
            akunDemo.setTeacherId(kandidat.getId());
            akunDemo.setFullName(kandidat.getFullName());
            authDAO.update(akunDemo);
        }
    }

    private void seedUserIfMissing(
            String username,
            String rawPassword,
            String fullName,
            String role) {
        if (authDAO.existsByUsername(username)) {
            return;
        }

        String salt = passwordHasher.generateSalt();
        String hash = passwordHasher.hashPassword(rawPassword, salt);

        User user = new User(
                null,
                username,
                hash,
                salt,
                fullName,
                role,
                true);

        authDAO.save(user);
    }
}

