package id.ac.sekolah.manajemenkelas.dashboard.model;

import java.math.BigDecimal;

public class DashboardSummary {

    private final String labelCakupan;
    private final int totalSiswaAktif;
    private final int totalSiswaTidakAktif;
    private final int totalGuruAktif;
    private final int totalGuruTidakAktif;
    private final int totalKelasAktif;
    private final int totalMataPelajaranAktif;
    private final String namaTahunAjaranAktif;
    private final int jumlahSemesterAktif;
    private final int jumlahWaliKelasAktif;
    private final int jumlahPenugasanMengajarAktif;
    private final int totalJumlahPenilaian;
    private final int totalCatatanAbsensi;
    private final BigDecimal persentaseKehadiranKeseluruhan;

    public DashboardSummary(
            String labelCakupan,
            int totalSiswaAktif,
            int totalSiswaTidakAktif,
            int totalGuruAktif,
            int totalGuruTidakAktif,
            int totalKelasAktif,
            int totalMataPelajaranAktif,
            String namaTahunAjaranAktif,
            int jumlahSemesterAktif,
            int jumlahWaliKelasAktif,
            int jumlahPenugasanMengajarAktif,
            int totalJumlahPenilaian,
            int totalCatatanAbsensi,
            BigDecimal persentaseKehadiranKeseluruhan) {
        this.labelCakupan = labelCakupan;
        this.totalSiswaAktif = totalSiswaAktif;
        this.totalSiswaTidakAktif = totalSiswaTidakAktif;
        this.totalGuruAktif = totalGuruAktif;
        this.totalGuruTidakAktif = totalGuruTidakAktif;
        this.totalKelasAktif = totalKelasAktif;
        this.totalMataPelajaranAktif = totalMataPelajaranAktif;
        this.namaTahunAjaranAktif = namaTahunAjaranAktif;
        this.jumlahSemesterAktif = jumlahSemesterAktif;
        this.jumlahWaliKelasAktif = jumlahWaliKelasAktif;
        this.jumlahPenugasanMengajarAktif = jumlahPenugasanMengajarAktif;
        this.totalJumlahPenilaian = totalJumlahPenilaian;
        this.totalCatatanAbsensi = totalCatatanAbsensi;
        this.persentaseKehadiranKeseluruhan = persentaseKehadiranKeseluruhan;
    }

    public String getScopeLabel() {
        return labelCakupan;
    }

    public int getTotalActiveStudents() {
        return totalSiswaAktif;
    }

    public int getTotalInactiveStudents() {
        return totalSiswaTidakAktif;
    }

    public int getTotalActiveTeachers() {
        return totalGuruAktif;
    }

    public int getTotalInactiveTeachers() {
        return totalGuruTidakAktif;
    }

    public int getTotalActiveClasses() {
        return totalKelasAktif;
    }

    public int getTotalActiveSubjects() {
        return totalMataPelajaranAktif;
    }

    public String getActiveAcademicYearName() {
        return namaTahunAjaranAktif;
    }

    public int getActiveSemestersCount() {
        return jumlahSemesterAktif;
    }

    public int getActiveHomeroomCount() {
        return jumlahWaliKelasAktif;
    }

    public int getActiveTeachingAssignmentCount() {
        return jumlahPenugasanMengajarAktif;
    }

    public int getTotalAssessmentsCount() {
        return totalJumlahPenilaian;
    }

    public int getTotalAttendanceRecords() {
        return totalCatatanAbsensi;
    }

    public BigDecimal getOverallAttendancePercentage() {
        return persentaseKehadiranKeseluruhan;
    }

    public boolean hasAttendanceData() {
        return totalCatatanAbsensi > 0 && persentaseKehadiranKeseluruhan != null;
    }
}
