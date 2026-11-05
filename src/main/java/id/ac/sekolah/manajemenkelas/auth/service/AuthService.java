package id.ac.sekolah.manajemenkelas.auth.service;

import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

public class AuthService {

    private final AuthDAO authDAO;
    private final SessionContext sessionContext;

    public AuthService(AuthDAO authDAO) {
        this.authDAO = authDAO;
        this.sessionContext = SessionContext.getInstance();
    }

    public boolean login(String username, String password) {

        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            return false;
        }

        Optional<User> userOptional = authDAO.findByUsername(username.trim());

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        if (!user.isActive()) {
            return false;
        }

        if (!user.getPassword().equals(password)) {
            return false;
        }

        sessionContext.startSession(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole());

        return true;
    }

    public void logout() {
        sessionContext.endSession();
    }

    public boolean isAuthenticated() {
        return sessionContext.isLoggedIn();
    }

    public boolean hasRole(String role) {
        return sessionContext.hasRole(role);
    }
}