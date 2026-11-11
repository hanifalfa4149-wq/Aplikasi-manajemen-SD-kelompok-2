package id.ac.sekolah.manajemenkelas.presentation.cli.attendance;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.model.AttendanceSummary;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService;
import id.ac.sekolah.manajemenkelas.attendance.service.AttendanceService.StudentAttendanceEntry;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MenuNavigator;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;

public class AttendanceMenu {

    private final Scanner scanner;
    private final AttendanceService attendanceService;
    private final AcademicService academicService;
    private final StudentService studentService;

    public AttendanceMenu(
            Scanner scanner,
            AttendanceService attendanceService,
            AcademicService academicService,
            StudentService studentService) {
        this.scanner = scanner;
        this.attendanceService = attendanceService;
        this.academicService = academicService;
        this.studentService = studentService;
    }

    public void show() {
        boolean active = true;

        while (active) {
            System.out.println("\n========== MANAJEMEN ABSENSI HARIAN ==========");
            System.out.println("1. Catat Absensi Siswa");
            System.out.println("2. Catat Absensi Massal Satu Kelas");
            System.out.println("3. Lihat Absensi per Tanggal");
            System.out.println("4. Lihat Absensi per Kelas");
            System.out.println("5. Lihat Absensi per Kelas & Tanggal");
            System.out.println("6. Lihat Riwayat Absensi Siswa");
            System.out.println("7. Perbarui Absensi");
            System.out.println("8. Rekapitulasi Absensi Kelas (Rentang Tanggal)");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            String choice = MenuNavigator.readMenuChoice(scanner);

            try {
                switch (choice) {
                    case "1" -> recordSingleAttendance();
                    case "2" -> recordClassAttendance();
                    case "3" -> viewByDate();
                    case "4" -> viewByClass();
                    case "5" -> viewByClassAndDate();
                    case "6" -> viewByStudent();
                    case "7" -> updateAttendance();
                    case "8" -> viewClassSummary();
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

    private void recordSingleAttendance() {
        displayClassList();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        displayStudentsInClass(classId);
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        System.out.print("Tanggal absensi (YYYY-MM-DD): ");
        LocalDate date = readDate();

        System.out.print("Status (HADIR/SAKIT/IZIN/ALPA): ");
        String status = scanner.nextLine();

        System.out.print("Keterangan (opsional): ");
        String notes = scanner.nextLine();

        Attendance saved = attendanceService.recordAttendance(studentId, classId, date, status, notes);
        System.out.println("Absensi berhasil dicatat. ID: " + saved.getId());
    }

    private void recordClassAttendance() {
        displayClassList();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        System.out.print("Tanggal absensi (YYYY-MM-DD): ");
        LocalDate date = readDate();

        List<StudentPlacement> placements = academicService.getActivePlacementsByClass(classId);
        if (placements.isEmpty()) {
            System.out.println("Tidak ada siswa aktif di kelas ini.");
            return;
        }

        List<StudentAttendanceEntry> entries = new ArrayList<>();
        System.out.println("Masukkan status (HADIR/SAKIT/IZIN/ALPA) untuk setiap siswa (Enter = HADIR):");

        for (StudentPlacement p : placements) {
            String studentName = studentService.getStudentById(p.getStudentId())
                    .map(Student::getFullName)
                    .orElse("Siswa #" + p.getStudentId());

            System.out.print(" - [" + p.getStudentId() + "] " + studentName + " Status [HADIR]: ");
            String statusInput = scanner.nextLine().trim();
            String status = statusInput.isEmpty() ? Attendance.STATUS_HADIR : statusInput;

            System.out.print("   Keterangan (opsional): ");
            String notes = scanner.nextLine();

            entries.add(new StudentAttendanceEntry(p.getStudentId(), status, notes));
        }

        List<Attendance> saved = attendanceService.recordClassAttendance(classId, date, entries);
        System.out.println("Berhasil menyimpan " + saved.size() + " data absensi kelas.");
    }

    private void viewByDate() {
        System.out.print("Tanggal (YYYY-MM-DD): ");
        LocalDate date = readDate();
        displayAttendances(attendanceService.getAttendanceByDate(date));
    }

    private void viewByClass() {
        displayClassList();
        System.out.print("ID kelas: ");
        Long classId = readLong();
        displayAttendances(attendanceService.getAttendanceByClass(classId));
    }

    private void viewByClassAndDate() {
        displayClassList();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        System.out.print("Tanggal (YYYY-MM-DD): ");
        LocalDate date = readDate();

        displayAttendances(attendanceService.getAttendanceByClassAndDate(classId, date));
    }

    private void viewByStudent() {
        System.out.print("ID siswa: ");
        Long studentId = readLong();
        displayAttendances(attendanceService.getAttendanceByStudent(studentId));
    }

    private void updateAttendance() {
        System.out.print("ID absensi yang akan diperbarui: ");
        Long attendanceId = readLong();

        Attendance existing = attendanceService.getAttendanceById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Data absensi tidak ditemukan."));

        System.out.println("Status saat ini: " + existing.getStatus()
                + " | Keterangan: " + existing.getNotes());

        System.out.print("Status baru (HADIR/SAKIT/IZIN/ALPA): ");
        String status = scanner.nextLine();

        System.out.print("Keterangan baru (kosongkan untuk hapus/kosong): ");
        String notes = scanner.nextLine();

        attendanceService.updateAttendance(attendanceId, status, notes);
        System.out.println("Data absensi berhasil diperbarui.");
    }

    private void viewClassSummary() {
        displayClassList();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        System.out.print("Tanggal mulai (YYYY-MM-DD): ");
        LocalDate startDate = readDate();

        System.out.print("Tanggal selesai (YYYY-MM-DD): ");
        LocalDate endDate = readDate();

        AttendanceSummary summary = attendanceService.getClassAttendanceSummary(classId, startDate, endDate);

        System.out.println("\n========== REKAPITULASI ABSENSI KELAS ==========");
        System.out.println("ID Kelas      : " + summary.getClassId());
        System.out.println("Periode       : " + summary.getStartDate() + " s.d. " + summary.getEndDate());
        System.out.println("Total Catatan : " + summary.getTotalRecords());
        System.out.println("HADIR         : " + summary.getHadirCount());
        System.out.println("SAKIT         : " + summary.getSakitCount());
        System.out.println("IZIN          : " + summary.getIzinCount());
        System.out.println("ALPA          : " + summary.getAlpaCount());
    }

    private void displayAttendances(List<Attendance> list) {
        System.out.println("\n========== DATA ABSENSI ==========");
        if (list.isEmpty()) {
            System.out.println("Belum ada data absensi.");
            return;
        }
        for (Attendance a : list) {
            System.out.println("----------------------------------");
            System.out.println("ID         : " + a.getId());
            System.out.println("Tanggal    : " + a.getAttendanceDate());
            System.out.println("ID Kelas   : " + a.getClassId());
            System.out.println("ID Siswa   : " + a.getStudentId());
            System.out.println("Status     : " + a.getStatus());
            System.out.println("Keterangan : " + (a.getNotes().isEmpty() ? "-" : a.getNotes()));
            System.out.println("DicatatOleh: " + (a.getRecordedBy() != null ? a.getRecordedBy() : "-"));
        }
    }

    private void displayClassList() {
        List<SchoolClass> classes = academicService.getAllClasses();
        SessionContext session = SessionContext.getInstance();
        if (session.hasRole(RoleMapper.ROLE_TEACHER) && session.getTeacherId() != null) {
            Long teacherId = session.getTeacherId();
            Set<Long> assignedIds = new HashSet<>();
            academicService.getAllHomeroomAssignments().stream()
                    .filter(hr -> hr.isActive() && teacherId.equals(hr.getTeacherId()))
                    .forEach(hr -> assignedIds.add(hr.getClassId()));
            academicService.getTeachingAssignmentsByTeacher(teacherId).stream()
                    .filter(ta -> ta.isActive())
                    .forEach(ta -> assignedIds.add(ta.getClassId()));
            classes = classes.stream()
                    .filter(c -> assignedIds.contains(c.getId()))
                    .toList();
        }

        if (classes.isEmpty()) {
            System.out.println("Belum ada data kelas yang tersedia.");
            return;
        }
        System.out.println("-- Daftar Kelas --");
        for (SchoolClass c : classes) {
            System.out.println("  ID " + c.getId() + " | " + c.getName() + " (Tingkat " + c.getGradeLevel() + ")");
        }
    }

    private void displayStudentsInClass(Long classId) {
        List<StudentPlacement> active = academicService.getActivePlacementsByClass(classId);
        if (active.isEmpty()) {
            return;
        }
        System.out.println("-- Siswa Aktif Kelas ID " + classId + " --");
        for (StudentPlacement p : active) {
            String name = studentService.getStudentById(p.getStudentId())
                    .map(Student::getFullName)
                    .orElse("Siswa #" + p.getStudentId());
            System.out.println("  ID " + p.getStudentId() + " | " + name);
        }
    }

    private LocalDate readDate() {
        try {
            return LocalDate.parse(scanner.nextLine().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format tanggal harus YYYY-MM-DD.");
        }
    }

    private Long readLong() {
        try {
            return Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID harus berupa angka.");
        }
    }
}
