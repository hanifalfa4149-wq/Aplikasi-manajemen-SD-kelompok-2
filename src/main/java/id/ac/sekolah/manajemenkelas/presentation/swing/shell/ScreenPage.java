package id.ac.sekolah.manajemenkelas.presentation.swing.shell;

/**
 * Kontrak umum halaman modul dalam CardLayout agar setiap layar dapat memuat ulang data saat dibuka.
 */
public interface ScreenPage {

    void refreshData();
}

