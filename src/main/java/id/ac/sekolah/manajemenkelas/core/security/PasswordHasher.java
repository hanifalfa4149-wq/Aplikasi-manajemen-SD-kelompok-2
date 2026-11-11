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
 * Utility untuk membuat salt acak serta melakukan hashing dan verifikasi kata sandi
 * menggunakan PBKDF2WithHmacSHA256 sesuai konfigurasi di application.properties.
 */
public class PasswordHasher {

    private static final String DEFAULT_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int DEFAULT_ITERATIONS = 600_000;
    private static final int DEFAULT_KEY_LENGTH = 256;
    private static final int DEFAULT_SALT_LENGTH = 16;

    private final String algorithm;
    private final int iterations;
    private final int keyLength;
    private final int saltLength;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordHasher() {
        Properties props = loadProperties();
        this.algorithm = props.getProperty("security.password.algorithm", DEFAULT_ALGORITHM).trim();
        this.iterations = parsePositiveInt(
                props.getProperty("security.password.iterations"), DEFAULT_ITERATIONS);
        this.keyLength = parsePositiveInt(
                props.getProperty("security.password.key-length"), DEFAULT_KEY_LENGTH);
        this.saltLength = parsePositiveInt(
                props.getProperty("security.password.salt-length"), DEFAULT_SALT_LENGTH);
    }

    public PasswordHasher(String algorithm, int iterations, int keyLength, int saltLength) {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("Algoritma hashing wajib diisi.");
        }
        if (iterations <= 0 || keyLength <= 0 || saltLength <= 0) {
            throw new IllegalArgumentException("Parameter hashing harus bernilai positif.");
        }
        this.algorithm = algorithm;
        this.iterations = iterations;
        this.keyLength = keyLength;
        this.saltLength = saltLength;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public int getIterations() {
        return iterations;
    }

    public int getKeyLength() {
        return keyLength;
    }

    public int getSaltLength() {
        return saltLength;
    }

    public String generateSalt() {
        byte[] salt = new byte[saltLength];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hashPassword(String rawPassword, String saltBase64) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Kata sandi tidak boleh kosong.");
        }
        if (saltBase64 == null || saltBase64.isBlank()) {
            throw new IllegalArgumentException("Salt kata sandi tidak boleh kosong.");
        }

        byte[] saltBytes = Base64.getDecoder().decode(saltBase64);
        byte[] hashBytes = deriveKey(rawPassword.toCharArray(), saltBytes);
        return Base64.getEncoder().encodeToString(hashBytes);
    }

    public boolean verifyPassword(String rawPassword, String expectedHashBase64, String saltBase64) {
        if (rawPassword == null || rawPassword.isBlank()
                || expectedHashBase64 == null || expectedHashBase64.isBlank()
                || saltBase64 == null || saltBase64.isBlank()) {
            return false;
        }

        try {
            byte[] saltBytes = Base64.getDecoder().decode(saltBase64);
            byte[] expectedHashBytes = Base64.getDecoder().decode(expectedHashBase64);
            byte[] actualHashBytes = deriveKey(rawPassword.toCharArray(), saltBytes);
            return MessageDigest.isEqual(expectedHashBytes, actualHashBytes);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] deriveKey(char[] passwordChars, byte[] saltBytes) {
        PBEKeySpec spec = new PBEKeySpec(passwordChars, saltBytes, iterations, keyLength);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Gagal memproses hashing kata sandi.", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = PasswordHasher.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                props.load(input);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca konfigurasi keamanan.", e);
        }
        return props;
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
