package id.ac.sekolah.manajemenkelas.core.session;

public class SessionContext {

    private static SessionContext instance;

    private Long userId;
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
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public void endSession() {
        this.userId = null;
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
}