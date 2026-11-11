package id.ac.sekolah.manajemenkelas;

import java.awt.GraphicsEnvironment;
import java.util.Arrays;

import id.ac.sekolah.manajemenkelas.presentation.cli.CliApplication;
import id.ac.sekolah.manajemenkelas.presentation.swing.SwingApplication;

/**
 * Titik masuk utama Sistem Manajemen Kelas SD.
 * Secara default menjalankan antarmuka desktop Java Swing; gunakan argumen {@code --cli}
 * untuk menjalankan antarmuka konsol (CLI).
 */
public class Main {

    public static void main(String[] args) {
        boolean forceCli = args != null && Arrays.stream(args)
                .anyMatch(arg -> "--cli".equalsIgnoreCase(arg));

        if (forceCli || GraphicsEnvironment.isHeadless()) {
            new CliApplication().start();
            return;
        }

        SwingApplication.launch();
    }
}
