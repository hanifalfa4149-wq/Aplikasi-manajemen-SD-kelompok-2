package id.ac.sekolah.manajemenkelas.presentation.cli.auth;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

public class AuthMenu {

    private final SessionContext sessionContext;

    public AuthMenu() {
        sessionContext = SessionContext.getInstance();
    }

    public void displaySessionInfo() {

        System.out.println("\n========== INFORMASI SESSION ==========");
        System.out.println("User ID  : " + sessionContext.getUserId());
        System.out.println("Username : " + sessionContext.getUsername());
        System.out.println("Nama     : " + sessionContext.getFullName());
        System.out.println("Role     : " + sessionContext.getRole());
    }
}