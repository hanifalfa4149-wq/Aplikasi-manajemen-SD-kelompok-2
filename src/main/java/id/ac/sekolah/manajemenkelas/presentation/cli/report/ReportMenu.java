package id.ac.sekolah.manajemenkelas.presentation.cli.report;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.dashboard.service.DashboardService;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MenuNavigator;
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;
import id.ac.sekolah.manajemenkelas.report.service.ReportService;

public class ReportMenu {

    private final Scanner scanner;
    private final DashboardService dashboardService;
    private final ReportService reportService;

    public ReportMenu(
            Scanner scanner,
            DashboardService dashboardService,
            ReportService reportService) {
        this.scanner = scanner;
        this.dashboardService = dashboardService;
        this.reportService = reportService;
    }

    public void show() {
        boolean active = true;

        while (active) {
            System.out.println("\n========== DASHBOARD & LAPORAN ==========");
            System.out.println("1. Dashboard Ringkasan");
            System.out.println("2. Laporan Siswa Aktif per Kelas");
            System.out.println("3. Laporan Siswa Aktif per Jenis Kelamin");
            System.out.println("4. Rekapitulasi Absensi (Rentang Tanggal)");
            System.out.println("5. Rekapitulasi Absensi per Kelas");
            System.out.println("6. Ringkasan Penilaian");
            System.out.println("7. Ringkasan Nilai Siswa");
            System.out.println("8. Rata-rata Nilai per Mata Pelajaran");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            String choice = MenuNavigator.readMenuChoice(scanner);

            try {
                switch (choice) {
                    case "1" -> displayDashboard();
                    case "2" -> displayStudentsByClass();
                    case "3" -> displayStudentsByGender();
                    case "4" -> displayAttendanceByDateRange();
                    case "5" -> displayAttendanceByClass();
                    case "6" -> displayAssessmentSummaries();
                    case "7" -> displayStudentScoreSummaries();
                    case "8" -> displaySubjectAverages();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("Kesalahan sistem/database: " + e.getMessage());
            }
        }
    }

    public void displayDashboard() {
        DashboardSummary d = dashboardService.getDashboardSummary();
        System.out.println("\n========== DASHBOARD RINGKASAN ==========");
        System.out.println("Cakupan Data         : " + d.getScopeLabel());
        System.out.println("Tahun Ajaran Aktif   : " + d.getActiveAcademicYearName()
                + " (Semester Aktif: " + d.getActiveSemestersCount() + ")");
        System.out.println("Siswa Aktif          : " + d.getTotalActiveStudents()
                + " (Nonaktif: " + d.getTotalInactiveStudents() + ")");
        System.out.println("Guru Aktif           : " + d.getTotalActiveTeachers()
                + " (Nonaktif: " + d.getTotalInactiveTeachers() + ")");
        System.out.println("Kelas Aktif          : " + d.getTotalActiveClasses());
        System.out.println("Mata Pelajaran Aktif : " + d.getTotalActiveSubjects());
        System.out.println("Wali Kelas Aktif     : " + d.getActiveHomeroomCount());
        System.out.println("Penugasan Mengajar   : " + d.getActiveTeachingAssignmentCount());
        System.out.println("Total Penilaian      : " + d.getTotalAssessmentsCount());
        System.out.println("Total Catatan Absensi: " + d.getTotalAttendanceRecords());
        System.out.println("Persentase Kehadiran : "
                + formatPercentage(d.getOverallAttendancePercentage(), d.hasAttendanceData()));
    }

    private void displayStudentsByClass() {
        List<ClassStudentCountReport> rows = reportService.getStudentCountsByClass();
        System.out.println("\n--- Laporan Siswa Aktif per Kelas ---");
        if (rows.isEmpty()) {
            System.out.println("Belum ada data kelas aktif.");
            return;
        }
        for (ClassStudentCountReport r : rows) {
            System.out.println(" - [" + r.getClassId() + "] " + r.getClassName()
                    + " (Tingkat " + r.getGradeLevel() + ", TA: " + r.getAcademicYearName() + ")"
                    + " | Siswa Aktif: " + r.getActiveStudentCount() + "/" + r.getCapacity());
        }
    }

    private void displayStudentsByGender() {
        List<GenderStudentCountReport> rows = reportService.getStudentCountsByGender();
        System.out.println("\n--- Laporan Siswa Aktif per Jenis Kelamin ---");
        for (GenderStudentCountReport r : rows) {
            System.out.println(" - " + r.getGenderLabel() + " (" + r.getGenderCode() + "): "
                    + r.getActiveStudentCount());
        }
    }

    private void displayAttendanceByDateRange() {
        System.out.print("Tanggal mulai (YYYY-MM-DD): ");
        LocalDate start = readDate();
        System.out.print("Tanggal selesai (YYYY-MM-DD): ");
        LocalDate end = readDate();

        AttendanceRangeReport r = reportService.getAttendanceSummaryByDateRange(start, end);
        System.out.println("\n--- Rekapitulasi Absensi " + r.getStartDate() + " s.d. " + r.getEndDate() + " ---");
        System.out.println("Total Catatan : " + r.getTotalRecords());
        System.out.println("HADIR         : " + r.getHadirCount());
        System.out.println("SAKIT         : " + r.getSakitCount());
        System.out.println("IZIN          : " + r.getIzinCount());
        System.out.println("ALPA          : " + r.getAlpaCount());
        System.out.println("Kehadiran (%) : "
                + formatPercentage(r.getAttendancePercentage(), r.hasSufficientData()));
    }

    private void displayAttendanceByClass() {
        System.out.print("Filter tanggal mulai (YYYY-MM-DD, Enter untuk semua tanggal): ");
        String startRaw = scanner.nextLine().trim();
        List<ClassAttendanceReport> rows;
        if (startRaw.isEmpty()) {
            rows = reportService.getAttendanceSummaryByClass();
        } else {
            LocalDate start = parseDate(startRaw);
            System.out.print("Filter tanggal selesai (YYYY-MM-DD): ");
            LocalDate end = readDate();
            rows = reportService.getAttendanceSummaryByClass(start, end);
        }

        System.out.println("\n--- Rekapitulasi Absensi per Kelas ---");
        if (rows.isEmpty()) {
            System.out.println("Belum ada data kelas aktif.");
            return;
        }
        for (ClassAttendanceReport r : rows) {
            System.out.println(" - [" + r.getClassId() + "] " + r.getClassName()
                    + " (Tingkat " + r.getGradeLevel() + ")"
                    + " | Total: " + r.getTotalRecords()
                    + " | H:" + r.getHadirCount()
                    + " S:" + r.getSakitCount()
                    + " I:" + r.getIzinCount()
                    + " A:" + r.getAlpaCount()
                    + " | Hadir: " + formatPercentage(r.getAttendancePercentage(), r.hasSufficientData()));
        }
    }

    private void displayAssessmentSummaries() {
        List<AssessmentReport> rows = reportService.getAssessmentSummaries();
        System.out.println("\n--- Ringkasan Penilaian ---");
        if (rows.isEmpty()) {
            System.out.println("Belum ada data penilaian.");
            return;
        }
        for (AssessmentReport r : rows) {
            System.out.print(" - [" + r.getAssessmentId() + "] " + r.getTitle()
                    + " (" + r.getAssessmentType() + ")"
                    + " | " + r.getClassName() + " - " + r.getSubjectName()
                    + " | Tgl: " + r.getAssessmentDate()
                    + " | Maks: " + r.getMaxScore()
                    + " | Dinilai: " + r.getScoredStudentsCount());
            if (r.hasScores()) {
                System.out.println(" | Rata-rata: " + r.getAverageScore()
                        + " (" + r.getAveragePercentage() + "%)"
                        + " | Min: " + r.getLowestScore()
                        + " | Max: " + r.getHighestScore());
            } else {
                System.out.println(" | Rata-rata: - (belum ada nilai)");
            }
        }
    }

    private void displayStudentScoreSummaries() {
        System.out.print("ID siswa tertentu (kosongkan untuk tampilkan semua): ");
        String raw = scanner.nextLine().trim();
        if (!raw.isEmpty()) {
            Long studentId = parseLong(raw);
            StudentScoreReport r = reportService.getStudentScoreSummary(studentId);
            printStudentScoreRow(r);
            return;
        }

        List<StudentScoreReport> rows = reportService.getStudentScoreSummaries();
        System.out.println("\n--- Ringkasan Nilai Siswa ---");
        if (rows.isEmpty()) {
            System.out.println("Belum ada data siswa aktif dalam cakupan Anda.");
            return;
        }
        for (StudentScoreReport r : rows) {
            printStudentScoreRow(r);
        }
    }

    private void displaySubjectAverages() {
        List<SubjectScoreReport> rows = reportService.getAverageScoresBySubject();
        System.out.println("\n--- Rata-rata Nilai per Mata Pelajaran ---");
        if (rows.isEmpty()) {
            System.out.println("Belum ada data mata pelajaran.");
            return;
        }
        for (SubjectScoreReport r : rows) {
            System.out.print(" - [" + r.getSubjectCode() + "] " + r.getSubjectName()
                    + " | Penilaian: " + r.getAssessmentCount()
                    + " | Entri Nilai: " + r.getScoredEntriesCount());
            if (r.hasScores()) {
                System.out.println(" | Rata-rata: " + r.getAverageScore()
                        + " (" + r.getAveragePercentage() + "%)");
            } else {
                System.out.println(" | Rata-rata: - (belum ada nilai)");
            }
        }
    }

    private void printStudentScoreRow(StudentScoreReport r) {
        System.out.print(" - [" + r.getStudentId() + "] " + r.getNis() + " - " + r.getStudentName()
                + " (Kelas: " + r.getCurrentClassName() + ")"
                + " | Penilaian: " + r.getAssessmentCount()
                + " | Total: " + r.getTotalEarnedScore() + "/" + r.getTotalMaxScore());
        if (r.hasScores()) {
            System.out.println(" | Rata-rata: " + r.getAverageScore()
                    + " (" + r.getOverallPercentage() + "%)");
        } else {
            System.out.println(" | Rata-rata: - (belum ada nilai)");
        }
    }

    private String formatPercentage(BigDecimal pct, boolean sufficientData) {
        if (!sufficientData || pct == null) {
            return "- (data belum mencukupi)";
        }
        return pct + "%";
    }

    private LocalDate readDate() {
        return parseDate(scanner.nextLine().trim());
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan YYYY-MM-DD.");
        }
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input ID harus berupa angka.");
        }
    }
}
