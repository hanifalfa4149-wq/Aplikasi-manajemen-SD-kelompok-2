package id.ac.sekolah.manajemenkelas.core.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Properties;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Utilitas untuk membuat salt acak serta melakukan hashing dan verifikasi kata sandi
 * menggunakan PBKDF2WithHmacSHA256 sesuai konfigurasi di application.properties.
 */
public class PasswordHasher {

    private static final String ALGORITMA_BAWAAN = "PBKDF2WithHmacSHA256";
    private static final int ITERASI_BAWAAN = 600_000;
    private static final int PANJANG_KUNCI_BAWAAN = 256;
    private static final int PANJANG_SALT_BAWAAN = 16;

    private final String algoritma;
    private final int jumlahIterasi;
    private final int panjangKunci;
    private final int panjangSalt;
    private final SecureRandom pengacakAman = new SecureRandom();

    public PasswordHasher() {
        Properties properti = muatProperti();
        this.algoritma = properti.getProperty("security.password.algorithm", ALGORITMA_BAWAAN).trim();
        this.jumlahIterasi = konversiIntegerPositif(
                properti.getProperty("security.password.iterations"), ITERASI_BAWAAN);
        this.panjangKunci = konversiIntegerPositif(
                properti.getProperty("security.password.key-length"), PANJANG_KUNCI_BAWAAN);
        this.panjangSalt = konversiIntegerPositif(
                properti.getProperty("security.password.salt-length"), PANJANG_SALT_BAWAAN);
    }

    public PasswordHasher(String algoritma, int jumlahIterasi, int panjangKunci, int panjangSalt) {
        if (algoritma == null || algoritma.isBlank()) {
            throw new IllegalArgumentException("Algoritma hashing wajib diisi.");
        }
        if (jumlahIterasi <= 0 || panjangKunci <= 0 || panjangSalt <= 0) {
            throw new IllegalArgumentException("Parameter hashing harus bernilai positif.");
        }
        this.algoritma = algoritma;
        this.jumlahIterasi = jumlahIterasi;
        this.panjangKunci = panjangKunci;
        this.panjangSalt = panjangSalt;
    }

    public String getAlgorithm() {
        return algoritma;
    }

    public int getIterations() {
        return jumlahIterasi;
    }

    public int getKeyLength() {
        return panjangKunci;
    }

    public int getSaltLength() {
        return panjangSalt;
    }

    public String generateSalt() {
        byte[] byteSalt = new byte[panjangSalt];
        pengacakAman.nextBytes(byteSalt);
        return Base64.getEncoder().encodeToString(byteSalt);
    }

    public String hashPassword(String kataSandiMentah, String saltBase64) {
        if (kataSandiMentah == null || kataSandiMentah.isBlank()) {
            throw new IllegalArgumentException("Kata sandi tidak boleh kosong.");
        }
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new IllegalArgumentException("Salt kata sandi tidak boleh kosong.");
        }

        byte[] byteSalt = Base64.getDecoder().decode(saltBase64);
        byte[] byteHash = turunkanKunci(kataSandiMentah.toCharArray(), byteSalt);
        return Base64.getEncoder().encodeToString(byteHash);
    }

    public boolean verifyPassword(String kataSandiMentah, String hashDiharapkanBase64, String saltBase64) {
        if (kataSandiMentah == null || kataSandiMentah.isBlank()
                || hashDiharapkanBase64 == null || hashDiharapkanBase64.isBlank()
                || saltBase64 == null || saltBase64.isBlank()) {
            return false;
        }

        try {
            byte[] byteSalt = Base64.getDecoder().decode(saltBase64);
            byte[] byteHashDiharapkan = Base64.getDecoder().decode(hashDiharapkanBase64);
            byte[] byteHashAktual = turunkanKunci(kataSandiMentah.toCharArray(), byteSalt);
            return MessageDigest.isEqual(byteHashDiharapkan, byteHashAktual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] turunkanKunci(char[] karakterSandi, byte[] byteSalt) {
        PBEKeySpec spesifikasiKunci = new PBEKeySpec(karakterSandi, byteSalt, jumlahIterasi, panjangKunci);
        try {
            SecretKeyFactory pabrikKunci = SecretKeyFactory.getInstance(algoritma);
            return pabrikKunci.generateSecret(spesifikasiKunci).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Gagal memproses hashing kata sandi.", e);
        } finally {
            spesifikasiKunci.clearPassword();
        }
    }

    private static Properties muatProperti() {
        Properties properti = new Properties();
        try (InputStream aliranInput = PasswordHasher.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (aliranInput != null) {
                properti.load(aliranInput);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca konfigurasi keamanan.", e);
        }
        return properti;
    }

    private static int konversiIntegerPositif(String nilai, int nilaiCadangan) {
        if (nilai == null || nilai.isBlank()) {
            return nilaiCadangan;
        }
        try {
            int angka = Integer.parseInt(nilai.trim());
            return angka > 0 ? angka : nilaiCadangan;
        } catch (NumberFormatException e) {
            return nilaiCadangan;
        }
    }
}
