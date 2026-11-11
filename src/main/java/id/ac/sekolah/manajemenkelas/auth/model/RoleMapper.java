package id.ac.sekolah.manajemenkelas.auth.model;

/**
 * Pemetaan nilai peran internal aplikasi Java ('ADMIN', 'TEACHER')
 * ke nilai constraint kolom pengguna.peran di PostgreSQL ('ADMIN', 'GURU') dan sebaliknya.
 */
public final class RoleMapper {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TEACHER = "TEACHER";

    public static final String DB_ROLE_ADMIN = "ADMIN";
    public static final String DB_ROLE_GURU = "GURU";

    private RoleMapper() {
    }

    public static String toDatabaseRole(String applicationRole) {
        if (applicationRole == null || applicationRole.isBlank()) {
            throw new IllegalArgumentException("Peran pengguna tidak boleh kosong.");
        }

        String normalized = applicationRole.trim().toUpperCase();
        if (ROLE_ADMIN.equals(normalized)) {
            return DB_ROLE_ADMIN;
        }
        if (ROLE_TEACHER.equals(normalized) || DB_ROLE_GURU.equals(normalized)) {
            return DB_ROLE_GURU;
        }

        throw new IllegalArgumentException("Peran aplikasi tidak dikenali: " + applicationRole);
    }

    public static String fromDatabaseRole(String databaseRole) {
        if (databaseRole == null || databaseRole.isBlank()) {
            throw new IllegalArgumentException("Peran database tidak boleh kosong.");
        }

        String normalized = databaseRole.trim().toUpperCase();
        if (DB_ROLE_ADMIN.equals(normalized)) {
            return ROLE_ADMIN;
        }
        if (DB_ROLE_GURU.equals(normalized)) {
            return ROLE_TEACHER;
        }

        throw new IllegalArgumentException("Peran database tidak dikenali: " + databaseRole);
    }
}

