package id.ac.sekolah.manajemenkelas.presentation.swing;

import javax.swing.SwingUtilities;

import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalTheme;

/**
 * Titik masuk aplikasi desktop Java Swing yang menginisialisasi tema dan MainFrame pada EDT.
 */
public final class SwingApplication {

    private SwingApplication() {
    }

    public static void launch() {
        BotanicalTheme.installDefaults();
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

