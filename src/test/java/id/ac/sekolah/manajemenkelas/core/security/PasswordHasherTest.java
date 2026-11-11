package id.ac.sekolah.manajemenkelas.core.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    @Test
    void loadsSecurityConfigurationFromApplicationProperties() {
        PasswordHasher hasher = new PasswordHasher();

        assertEquals("PBKDF2WithHmacSHA256", hasher.getAlgorithm());
        assertEquals(600_000, hasher.getIterations());
        assertEquals(256, hasher.getKeyLength());
        assertEquals(16, hasher.getSaltLength());
    }

    @Test
    void verifyPasswordReturnsTrueForMatchingPassword() {
        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("admin123", salt);

        assertTrue(hasher.verifyPassword("admin123", hash, salt));
    }

    @Test
    void verifyPasswordReturnsFalseForWrongPassword() {
        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("admin123", salt);

        assertFalse(hasher.verifyPassword("salah123", hash, salt));
    }

    @Test
    void differentSaltsProduceDifferentHashesForSamePassword() {
        PasswordHasher hasher = new PasswordHasher();
        String salt1 = hasher.generateSalt();
        String salt2 = hasher.generateSalt();

        String hash1 = hasher.hashPassword("rahasia123", salt1);
        String hash2 = hasher.hashPassword("rahasia123", salt2);

        assertNotEquals(salt1, salt2);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void hashAndSaltCanBeStoredAndReadBack() {
        PasswordHasher writer = new PasswordHasher();
        String salt = writer.generateSalt();
        String hash = writer.hashPassword("guru123", salt);

        String persistedSalt = String.valueOf(salt);
        String persistedHash = String.valueOf(hash);

        PasswordHasher reader = new PasswordHasher();
        assertTrue(reader.verifyPassword("guru123", persistedHash, persistedSalt));
    }

    @Test
    void emptyAndBlankPasswordsAreHandledProperly() {
        PasswordHasher hasher = new PasswordHasher();
        String salt = hasher.generateSalt();
        String hash = hasher.hashPassword("guru123", salt);

        assertFalse(hasher.verifyPassword("", hash, salt));
        assertFalse(hasher.verifyPassword("   ", hash, salt));
        assertFalse(hasher.verifyPassword(null, hash, salt));

        assertThrows(IllegalArgumentException.class, () -> hasher.hashPassword("", salt));
        assertThrows(IllegalArgumentException.class, () -> hasher.hashPassword("   ", salt));
        assertThrows(IllegalArgumentException.class, () -> hasher.hashPassword(null, salt));
    }
}
