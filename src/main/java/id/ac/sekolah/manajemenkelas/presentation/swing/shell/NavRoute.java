package id.ac.sekolah.manajemenkelas.presentation.swing.shell;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.presentation.swing.theme.BotanicalIcons;

/**
 * Definisi rute navigasi aplikasi, pengelompokan kategori sidebar,
 * breadcrumb, serta hak akses berbasis peran (ADMIN / TEACHER).
 */
public enum NavRoute {

    DASHBOARD("Overview", "Dashboard", "Ringkasan statistik, grafik kelas, dan aktivitas terkini", BotanicalIcons.Type.DASHBOARD, true, true),

    STUDENTS("Master Data", "Data Siswa", "Kelola biodata siswa, status aktif, dan informasi orang tua", BotanicalIcons.Type.STUDENTS, true, false),
    TEACHERS("Master Data", "Data Guru", "Kelola profil pendidik, NIP, kontak, dan kepegawaian", BotanicalIcons.Type.TEACHERS, true, false),

    ACADEMIC_YEARS("Akademik", "Tahun Ajaran", "Atur periode tahun ajaran dan status tahun ajaran aktif", BotanicalIcons.Type.CALENDAR, true, false),
    SEMESTERS("Akademik", "Semester", "Kelola semester ganjil/genap dalam rentang tahun ajaran", BotanicalIcons.Type.SEMESTER, true, false),
    CLASSES("Akademik", "Data Kelas", "Kelola rombongan belajar tingkat 1 sampai 6 beserta kapasitas", BotanicalIcons.Type.CLASSROOM, true, false),
    SUBJECTS("Akademik", "Mata Pelajaran", "Kelola daftar mata pelajaran kurikulum sekolah dasar", BotanicalIcons.Type.SUBJECT, true, false),
    HOMEROOM("Akademik", "Wali Kelas", "Tetapkan dan akhiri penugasan wali kelas per rombel", BotanicalIcons.Type.HOMEROOM, true, false),
    TEACHING_ASSIGNMENTS("Akademik", "Penugasan Mengajar", "Hubungkan guru, mata pelajaran, kelas, dan semester", BotanicalIcons.Type.TEACHING, true, false),
    STUDENT_PLACEMENTS("Akademik", "Penempatan Siswa", "Penempatan awal, naik kelas, pindah kelas, dan kelulusan", BotanicalIcons.Type.PLACEMENT, true, false),
    TEACHER_SCOPE("Akademik", "Kelas & Siswa Diampu", "Daftar wali kelas, jadwal mengajar, dan siswa dalam cakupan Anda", BotanicalIcons.Type.CLASSROOM, false, true),

    ATTENDANCE("Operasional", "Absensi", "Pencatatan kehadiran harian (Hadir, Sakit, Izin, Alpa)", BotanicalIcons.Type.ATTENDANCE, true, true),
    ASSESSMENTS("Operasional", "Penilaian", "Kelola agenda kuis, tugas, UTS, UAS, praktik, dan proyek", BotanicalIcons.Type.ASSESSMENT, true, true),
    STUDENT_SCORES("Operasional", "Nilai Siswa", "Input, pembaruan, dan pantauan nilai per penilaian atau siswa", BotanicalIcons.Type.SCORES, true, true),

    REPORT_ACADEMIC("Laporan", "Laporan Akademik", "Sebaran siswa aktif per kelas, kapasitas, dan jenis kelamin", BotanicalIcons.Type.REPORTS, true, true),
    REPORT_ATTENDANCE("Laporan", "Laporan Absensi", "Rekapitulasi kehadiran per kelas dan rentang tanggal", BotanicalIcons.Type.REPORTS, true, true),
    REPORT_SCORES("Laporan", "Laporan Nilai", "Analisis rata-rata penilaian, mata pelajaran, dan capaian siswa", BotanicalIcons.Type.REPORTS, true, true);

    private final String category;
    private final String title;
    private final String subtitle;
    private final BotanicalIcons.Type iconType;
    private final boolean adminVisible;
    private final boolean teacherVisible;

    NavRoute(
            String category,
            String title,
            String subtitle,
            BotanicalIcons.Type iconType,
            boolean adminVisible,
            boolean teacherVisible) {
        this.category = category;
        this.title = title;
        this.subtitle = subtitle;
        this.iconType = iconType;
        this.adminVisible = adminVisible;
        this.teacherVisible = teacherVisible;
    }

    public String getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public BotanicalIcons.Type getIconType() {
        return iconType;
    }

    public boolean isAuthorizedForRole(String role) {
        if (role == null) {
            return false;
        }
        if (RoleMapper.ROLE_ADMIN.equalsIgnoreCase(role)) {
            return adminVisible;
        }
        if (RoleMapper.ROLE_TEACHER.equalsIgnoreCase(role) || RoleMapper.DB_ROLE_GURU.equalsIgnoreCase(role)) {
            return teacherVisible;
        }
        return false;
    }
}

