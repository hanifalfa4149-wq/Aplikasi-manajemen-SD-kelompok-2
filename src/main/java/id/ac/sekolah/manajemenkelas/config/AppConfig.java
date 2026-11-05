package id.ac.sekolah.manajemenkelas.config;

public final class AppConfig {

    public static final String APPLICATION_NAME = "SISTEM MANAJEMEN KELAS SD";

    public static final String APPLICATION_VERSION = "1.0.0";

    public static final String APPLICATION_DESCRIPTION = "Aplikasi Manajemen Kelas Sekolah Dasar";

    public static final String DEFAULT_ADMIN_USERNAME = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    public static final String DEFAULT_TEACHER_USERNAME = "guru";

    public static final String DEFAULT_TEACHER_PASSWORD = "guru123";

    private AppConfig() {
        // Prevent object instantiation.
    }

    public static void displayApplicationInfo() {
        System.out.println("==========================================");
        System.out.println("       " + APPLICATION_NAME);
        System.out.println("==========================================");
        System.out.println("Version : " + APPLICATION_VERSION);
        System.out.println("Description : " + APPLICATION_DESCRIPTION);
        System.out.println("==========================================");
    }
}
