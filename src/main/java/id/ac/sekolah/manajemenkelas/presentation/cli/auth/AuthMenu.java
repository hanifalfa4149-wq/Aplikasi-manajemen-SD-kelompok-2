package id.ac.sekolah.manajemenkelas.presentation.cli.auth;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

public class AuthMenu {

    private final SessionContext sessionContext;

    public AuthMenu() {
        sessionContext = SessionContext.getInstance();
    }

    public void displaySessionInfo() {

        System.out.println("\n========== INFORMASI SESSION ==========");
        if (!sessionContext.isLoggedIn()) {
            System.out.println("Belum ada sesi login aktif.");
            return;
        }
        System.out.println("User ID    : " + sessionContext.getUserId());
        if (sessionContext.getTeacherId() != null) {
            System.out.println("Teacher ID : " + sessionContext.getTeacherId());
        }
        System.out.println("Username   : " + sessionContext.getUsername());
        System.out.println("Nama       : " + sessionContext.getFullName());
        System.out.println("Role       : " + sessionContext.getRole());
    }
}
