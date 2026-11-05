package id.ac.sekolah.manajemenkelas.presentation.cli.student;

import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class StudentMenu {

    private final Scanner scanner;
    private final StudentService studentService;

    public StudentMenu(Scanner scanner, StudentService studentService) {
        this.scanner = scanner;
        this.studentService = studentService;
    }

    public void show() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MANAJEMEN SISWA ==========");
            System.out.println("1. Tambah Siswa");
            System.out.println("2. Lihat Semua Siswa");
            System.out.println("3. Cari Siswa");
            System.out.println("4. Edit Siswa");
            System.out.println("5. Nonaktifkan Siswa");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createStudent();
                    case "2" -> displayStudents(studentService.getAllStudents());
                    case "3" -> searchStudent();
                    case "4" -> updateStudent();
                    case "5" -> deactivateStudent();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createStudent() {

        System.out.println("\n========== TAMBAH SISWA ==========");

        System.out.print("NIS: ");
        String nis = scanner.nextLine();

        System.out.print("NISN: ");
        String nisn = scanner.nextLine();

        System.out.print("Nama lengkap: ");
        String name = scanner.nextLine();

        System.out.print("Jenis kelamin (L/P): ");
        String gender = scanner.nextLine();

        System.out.print("Tanggal lahir (YYYY-MM-DD): ");
        LocalDate birthDate = readDate();

        System.out.print("Tempat lahir: ");
        String birthPlace = scanner.nextLine();

        System.out.print("Alamat: ");
        String address = scanner.nextLine();

        System.out.print("Nama orang tua: ");
        String parentName = scanner.nextLine();

        System.out.print("Nomor telepon orang tua: ");
        String parentPhone = scanner.nextLine();

        Student student = studentService.createStudent(
                nis, nisn, name, gender, birthDate,
                birthPlace, address, parentName, parentPhone
        );

        System.out.println("Siswa berhasil ditambahkan. ID: " + student.getId());
    }

    private void searchStudent() {

        System.out.print("Masukkan nama/NIS/NISN: ");
        String keyword = scanner.nextLine();

        displayStudents(studentService.searchStudents(keyword));
    }

    private void updateStudent() {

        System.out.print("Masukkan ID siswa: ");
        Long id = readLong();

        Student student = studentService.getStudentById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Siswa tidak ditemukan."));

        System.out.println("Kosongkan input jika ingin mempertahankan nilai lama.");

        String nis = readOptional("NIS", student.getNis());
        String nisn = readOptional("NISN", student.getNisn());
        String name = readOptional("Nama", student.getFullName());
        String gender = readOptional("Jenis kelamin", student.getGender());

        System.out.print("Tanggal lahir [" + student.getBirthDate() + "]: ");
        String dateInput = scanner.nextLine().trim();
        LocalDate birthDate = dateInput.isEmpty()
                ? student.getBirthDate()
                : LocalDate.parse(dateInput);

        String birthPlace = readOptional("Tempat lahir", student.getBirthPlace());
        String address = readOptional("Alamat", student.getAddress());
        String parentName = readOptional("Nama orang tua", student.getParentName());
        String parentPhone = readOptional("Telepon orang tua", student.getParentPhone());

        studentService.updateStudent(
                id, nis, nisn, name, gender, birthDate,
                birthPlace, address, parentName, parentPhone
        );

        System.out.println("Data siswa berhasil diperbarui.");
    }

    private void deactivateStudent() {

        System.out.print("Masukkan ID siswa: ");
        Long id = readLong();

        studentService.deleteStudent(id);

        System.out.println("Siswa berhasil dinonaktifkan.");
    }

    private void displayStudents(List<Student> students) {

        System.out.println("\n========== DAFTAR SISWA ==========");

        if (students.isEmpty()) {
            System.out.println("Belum ada data siswa.");
            return;
        }

        for (Student student : students) {

            System.out.println("----------------------------------");
            System.out.println("ID       : " + student.getId());
            System.out.println("NIS      : " + student.getNis());
            System.out.println("NISN     : " + student.getNisn());
            System.out.println("Nama     : " + student.getFullName());
            System.out.println("Gender   : " + student.getGender());
            System.out.println("Lahir    : " + student.getBirthPlace()
                    + ", " + student.getBirthDate());
            System.out.println("Alamat   : " + student.getAddress());
            System.out.println("Orang tua: " + student.getParentName());
            System.out.println("Telepon  : " + student.getParentPhone());
            System.out.println("Status   : " + (student.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private String readOptional(String label, String currentValue) {

        System.out.print(label + " [" + currentValue + "]: ");
        String input = scanner.nextLine().trim();

        return input.isEmpty() ? currentValue : input;
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