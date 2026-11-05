package id.ac.sekolah.manajemenkelas.presentation.cli.academic;

import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;

public class AcademicMenu {

    private final Scanner scanner;
    private final AcademicService academicService;

    public AcademicMenu(Scanner scanner, AcademicService academicService) {
        this.scanner = scanner;
        this.academicService = academicService;
    }

    public void show() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== STRUKTUR AKADEMIK ==========");
            System.out.println("1. Manajemen Tahun Ajaran");
            System.out.println("2. Manajemen Kelas");
            System.out.println("3. Manajemen Mata Pelajaran");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            switch (scanner.nextLine().trim()) {
                case "1" -> academicYearMenu();
                case "2" -> classMenu();
                case "3" -> subjectMenu();
                case "0" -> active = false;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    // ACADEMIC YEAR

    private void academicYearMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== TAHUN AJARAN ==========");
            System.out.println("1. Tambah Tahun Ajaran");
            System.out.println("2. Lihat Tahun Ajaran");
            System.out.println("3. Edit Tahun Ajaran");
            System.out.println("4. Nonaktifkan Tahun Ajaran");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> createAcademicYear();
                    case "2" -> displayAcademicYears();
                    case "3" -> updateAcademicYear();
                    case "4" -> deactivateAcademicYear();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createAcademicYear() {

        System.out.print("Nama tahun ajaran (contoh 2026/2027): ");
        String name = scanner.nextLine();

        System.out.print("Tanggal mulai (YYYY-MM-DD): ");
        LocalDate start = readDate();

        System.out.print("Tanggal selesai (YYYY-MM-DD): ");
        LocalDate end = readDate();

        AcademicYear year = academicService.createAcademicYear(name, start, end);

        System.out.println("Tahun ajaran berhasil dibuat. ID: " + year.getId());
    }

    private void displayAcademicYears() {

        List<AcademicYear> years = academicService.getAllAcademicYears();

        if (years.isEmpty()) {
            System.out.println("Belum ada tahun ajaran.");
            return;
        }

        for (AcademicYear year : years) {
            System.out.println("----------------------------------");
            System.out.println("ID      : " + year.getId());
            System.out.println("Nama    : " + year.getName());
            System.out.println("Mulai   : " + year.getStartDate());
            System.out.println("Selesai : " + year.getEndDate());
            System.out.println("Status  : " + (year.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateAcademicYear() {

        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long id = readLong();

        AcademicYear year = academicService.getAcademicYearById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tahun ajaran tidak ditemukan."));

        System.out.print("Nama [" + year.getName() + "]: ");
        String name = readOptional(year.getName());

        System.out.print("Tanggal mulai [" + year.getStartDate() + "]: ");
        String startInput = scanner.nextLine().trim();

        System.out.print("Tanggal selesai [" + year.getEndDate() + "]: ");
        String endInput = scanner.nextLine().trim();

        LocalDate start = startInput.isEmpty()
                ? year.getStartDate()
                : LocalDate.parse(startInput);

        LocalDate end = endInput.isEmpty()
                ? year.getEndDate()
                : LocalDate.parse(endInput);

        academicService.updateAcademicYear(id, name, start, end);

        System.out.println("Tahun ajaran berhasil diperbarui.");
    }

    private void deactivateAcademicYear() {

        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long id = readLong();

        academicService.deactivateAcademicYear(id);

        System.out.println("Tahun ajaran berhasil dinonaktifkan.");
    }

    // SCHOOL CLASS

    private void classMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MANAJEMEN KELAS ==========");
            System.out.println("1. Tambah Kelas");
            System.out.println("2. Lihat Semua Kelas");
            System.out.println("3. Edit Kelas");
            System.out.println("4. Nonaktifkan Kelas");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> createClass();
                    case "2" -> displayClasses();
                    case "3" -> updateClass();
                    case "4" -> deactivateClass();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createClass() {

        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long yearId = readLong();

        System.out.print("Nama kelas (contoh 1A): ");
        String name = scanner.nextLine();

        System.out.print("Tingkat kelas (1-6): ");
        int grade = readInt();

        System.out.print("Kapasitas siswa: ");
        int capacity = readInt();

        SchoolClass schoolClass = academicService.createClass(
                yearId, name, grade, capacity);

        System.out.println("Kelas berhasil dibuat. ID: " + schoolClass.getId());
    }

    private void displayClasses() {

        List<SchoolClass> classes = academicService.getAllClasses();

        if (classes.isEmpty()) {
            System.out.println("Belum ada kelas.");
            return;
        }

        for (SchoolClass schoolClass : classes) {
            System.out.println("----------------------------------");
            System.out.println("ID           : " + schoolClass.getId());
            System.out.println("Tahun Ajaran : " + schoolClass.getAcademicYearId());
            System.out.println("Nama Kelas   : " + schoolClass.getName());
            System.out.println("Tingkat      : " + schoolClass.getGradeLevel());
            System.out.println("Kapasitas    : " + schoolClass.getCapacity());
            System.out.println("Status       : " + (schoolClass.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateClass() {

        displayClasses();

        System.out.print("ID kelas: ");
        Long id = readLong();

        SchoolClass schoolClass = academicService.getAllClasses().stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Kelas tidak ditemukan."));

        displayAcademicYears();

        System.out.print("ID tahun ajaran [" + schoolClass.getAcademicYearId() + "]: ");
        String yearInput = scanner.nextLine().trim();

        Long yearId = yearInput.isEmpty()
                ? schoolClass.getAcademicYearId()
                : Long.parseLong(yearInput);

        System.out.print("Nama kelas [" + schoolClass.getName() + "]: ");
        String name = readOptional(schoolClass.getName());

        System.out.print("Tingkat [" + schoolClass.getGradeLevel() + "]: ");
        String gradeInput = scanner.nextLine().trim();

        System.out.print("Kapasitas [" + schoolClass.getCapacity() + "]: ");
        String capacityInput = scanner.nextLine().trim();

        int grade = gradeInput.isEmpty()
                ? schoolClass.getGradeLevel()
                : Integer.parseInt(gradeInput);

        int capacity = capacityInput.isEmpty()
                ? schoolClass.getCapacity()
                : Integer.parseInt(capacityInput);

        academicService.updateClass(id, yearId, name, grade, capacity);

        System.out.println("Kelas berhasil diperbarui.");
    }

    private void deactivateClass() {

        displayClasses();

        System.out.print("ID kelas: ");
        Long id = readLong();

        academicService.deactivateClass(id);

        System.out.println("Kelas berhasil dinonaktifkan.");
    }

    // SUBJECT

    private void subjectMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MATA PELAJARAN ==========");
            System.out.println("1. Tambah Mata Pelajaran");
            System.out.println("2. Lihat Semua Mata Pelajaran");
            System.out.println("3. Edit Mata Pelajaran");
            System.out.println("4. Nonaktifkan Mata Pelajaran");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (scanner.nextLine().trim()) {
                    case "1" -> createSubject();
                    case "2" -> displaySubjects();
                    case "3" -> updateSubject();
                    case "4" -> deactivateSubject();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createSubject() {

        System.out.print("Kode mata pelajaran: ");
        String code = scanner.nextLine();

        System.out.print("Nama mata pelajaran: ");
        String name = scanner.nextLine();

        System.out.print("Deskripsi: ");
        String description = scanner.nextLine();

        Subject subject = academicService.createSubject(code, name, description);

        System.out.println("Mata pelajaran berhasil dibuat. ID: " + subject.getId());
    }

    private void displaySubjects() {

        List<Subject> subjects = academicService.getAllSubjects();

        if (subjects.isEmpty()) {
            System.out.println("Belum ada mata pelajaran.");
            return;
        }

        for (Subject subject : subjects) {
            System.out.println("----------------------------------");
            System.out.println("ID          : " + subject.getId());
            System.out.println("Kode        : " + subject.getCode());
            System.out.println("Nama        : " + subject.getName());
            System.out.println("Deskripsi   : " + subject.getDescription());
            System.out.println("Status      : " + (subject.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateSubject() {

        displaySubjects();

        System.out.print("ID mata pelajaran: ");
        Long id = readLong();

        Subject subject = academicService.getSubjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mata pelajaran tidak ditemukan."));

        System.out.print("Kode [" + subject.getCode() + "]: ");
        String code = readOptional(subject.getCode());

        System.out.print("Nama [" + subject.getName() + "]: ");
        String name = readOptional(subject.getName());

        System.out.print("Deskripsi [" + subject.getDescription() + "]: ");
        String description = readOptional(subject.getDescription());

        academicService.updateSubject(id, code, name, description);

        System.out.println("Mata pelajaran berhasil diperbarui.");
    }

    private void deactivateSubject() {

        displaySubjects();

        System.out.print("ID mata pelajaran: ");
        Long id = readLong();

        academicService.deactivateSubject(id);

        System.out.println("Mata pelajaran berhasil dinonaktifkan.");
    }

    // INPUT HELPERS

    private LocalDate readDate() {
        try {
            return LocalDate.parse(scanner.nextLine().trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Format tanggal harus YYYY-MM-DD.");
        }
    }

    private Long readLong() {
        try {
            return Long.parseLong(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input harus berupa angka.");
        }
    }

    private int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input harus berupa angka.");
        }
    }

    private String readOptional(String currentValue) {
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? currentValue : input;
    }
}