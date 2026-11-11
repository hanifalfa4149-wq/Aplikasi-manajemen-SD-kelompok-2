package id.ac.sekolah.manajemenkelas.core.session;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;

public class SessionContext {

    private static SessionContext instance;

    private Long userId;
    private Long teacherId;
    private String username;
    private String fullName;
    private String role;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        if (instance == null) {
            instance = new SessionContext();
        }

        return instance;
    }

    public void startSession(
            Long userId,
            String username,
            String fullName,
            String role) {
        startSession(userId, null, username, fullName, role);
    }

    public void startSession(
            Long userId,
            Long teacherId,
            String username,
            String fullName,
            String role) {
        this.userId = userId;
        this.teacherId = teacherId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public void endSession() {
        this.userId = null;
        this.teacherId = null;
        this.username = null;
        this.fullName = null;
        this.role = null;
    }

    public boolean isLoggedIn() {
        return userId != null;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String expectedRole) {
        return role != null && role.equalsIgnoreCase(expectedRole);
    }

    /**
     * Mengembalikan null untuk ADMIN (akses menyeluruh) atau teacherId untuk TEACHER (akses terbatas).
     */
    public Long resolveTeacherScopeOrNullForAdmin(String featureLabel) {
        if (!isLoggedIn()) {
            throw new IllegalArgumentException(
                    "Silakan login terlebih dahulu untuk mengakses " + featureLabel + ".");
        }
        if (hasRole(RoleMapper.ROLE_ADMIN)) {
            return null;
        }
        if (hasRole(RoleMapper.ROLE_TEACHER)) {
            if (teacherId == null) {
                throw new IllegalArgumentException(
                        "Akses ditolak: akun guru belum terhubung dengan profil guru.");
            }
            return teacherId;
        }
        throw new IllegalArgumentException("Akses ditolak: peran tidak diizinkan.");
    }

    /**
     * Menolak mutasi data oleh pengguna non-ADMIN ketika sesi login aktif.
     */
    public void requireAdminIfAuthenticated(String resourceLabel) {
        if (isLoggedIn() && !hasRole(RoleMapper.ROLE_ADMIN)) {
            throw new IllegalArgumentException(
                    "Akses ditolak: hanya ADMIN yang dapat mengubah data " + resourceLabel + ".");
        }
    }
}
