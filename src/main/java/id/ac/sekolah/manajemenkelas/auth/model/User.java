package id.ac.sekolah.manajemenkelas.auth.model;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private Long teacherId;
    private String username;
    private String passwordHash;
    private String passwordSalt;
    private String fullName;
    private String role;
    private boolean active;
    private LocalDateTime lastLogin;

    public User(
            Long id,
            Long teacherId,
            String username,
            String passwordHash,
            String passwordSalt,
            String fullName,
            String role,
            boolean active,
            LocalDateTime lastLogin) {
        this.id = id;
        this.teacherId = teacherId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
        this.lastLogin = lastLogin;
    }

    public User(
            Long id,
            String username,
            String passwordHash,
            String passwordSalt,
            String fullName,
            String role,
            boolean active) {
        this(id, null, username, passwordHash, passwordSalt, fullName, role, active, null);
    }

    public Long getId() {
        return id;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
}