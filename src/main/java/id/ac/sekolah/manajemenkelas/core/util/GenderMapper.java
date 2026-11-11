package id.ac.sekolah.manajemenkelas.core.util;

/**
 * Pemetaan nilai jenis kelamin antara model Java ('L', 'P')
 * dan constraint PostgreSQL ('LAKI_LAKI', 'PEREMPUAN').
 */
public final class GenderMapper {

    public static final String JAVA_MALE = "L";
    public static final String JAVA_FEMALE = "P";

    public static final String DB_MALE = "LAKI_LAKI";
    public static final String DB_FEMALE = "PEREMPUAN";

    private GenderMapper() {
    }

    public static String toDatabaseGender(String javaGender) {
        if (javaGender == null || javaGender.isBlank()) {
            throw new IllegalArgumentException("Jenis kelamin wajib diisi.");
        }

        String normalized = javaGender.trim().toUpperCase();
        if (JAVA_MALE.equals(normalized) || DB_MALE.equals(normalized)) {
            return DB_MALE;
        }
        if (JAVA_FEMALE.equals(normalized) || DB_FEMALE.equals(normalized)) {
            return DB_FEMALE;
        }

        throw new IllegalArgumentException("Jenis kelamin harus L atau P.");
    }

    public static String fromDatabaseGender(String databaseGender) {
        if (databaseGender == null || databaseGender.isBlank()) {
            throw new IllegalArgumentException("Jenis kelamin database tidak boleh kosong.");
        }

        String normalized = databaseGender.trim().toUpperCase();
        if (DB_MALE.equals(normalized) || JAVA_MALE.equals(normalized)) {
            return JAVA_MALE;
        }
        if (DB_FEMALE.equals(normalized) || JAVA_FEMALE.equals(normalized)) {
            return JAVA_FEMALE;
        }

        throw new IllegalArgumentException("Jenis kelamin database tidak dikenali: " + databaseGender);
    }
}

