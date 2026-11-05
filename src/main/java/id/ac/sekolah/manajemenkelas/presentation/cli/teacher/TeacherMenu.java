package id.ac.sekolah.manajemenkelas.presentation.cli.teacher;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

public class TeacherMenu {

    private final Scanner scanner;
    private final TeacherService teacherService;

    public TeacherMenu(Scanner scanner, TeacherService teacherService) {
        this.scanner = scanner;
        this.teacherService = teacherService;
    }

    public void show() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MANAJEMEN GURU ==========");
            System.out.println("1. Tambah Guru");
            System.out.println("2. Lihat Semua Guru");
            System.out.println("3. Cari Guru");
            System.out.println("4. Edit Guru");
            System.out.println("5. Nonaktifkan Guru");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> createTeacher();
                    case "2" -> displayTeachers(teacherService.getAllTeachers());
                    case "3" -> searchTeacher();
                    case "4" -> updateTeacher();
                    case "5" -> deactivateTeacher();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createTeacher() {

        System.out.println("\n========== TAMBAH GURU ==========");

        System.out.print("NIP: ");
        String nip = scanner.nextLine();

        System.out.print("Nama lengkap: ");
        String name = scanner.nextLine();

        System.out.print("Jenis kelamin (L/P): ");
        String gender = scanner.nextLine();

        System.out.print("Email: ");
        String email = scanner.nextLine();

        System.out.print("Nomor telepon: ");
        String phone = scanner.nextLine();

        System.out.print("Alamat: ");
        String address = scanner.nextLine();

        System.out.print("Pendidikan terakhir: ");
        String education = scanner.nextLine();

        System.out.print("Tanggal bergabung (YYYY-MM-DD): ");
        LocalDate joinDate = readDate();

        Teacher teacher = teacherService.createTeacher(
                nip, name, gender, email, phone,
                address, education, joinDate);

        System.out.println("Guru berhasil ditambahkan. ID: " + teacher.getId());
    }

    private void searchTeacher() {

        System.out.print("Masukkan nama/NIP/email: ");
        String keyword = scanner.nextLine();

        displayTeachers(teacherService.searchTeachers(keyword));
    }

    private void updateTeacher() {

        System.out.print("Masukkan ID guru: ");
        Long id = readLong();

        Teacher teacher = teacherService.getTeacherById(id)
                .orElseThrow(() -> new IllegalArgumentException("Guru tidak ditemukan."));

        System.out.println("Kosongkan input untuk mempertahankan nilai lama.");

        String nip = readOptional("NIP", teacher.getNip());
        String name = readOptional("Nama", teacher.getFullName());
        String gender = readOptional("Gender", teacher.getGender());
        String email = readOptional("Email", teacher.getEmail());
        String phone = readOptional("Telepon", teacher.getPhone());
        String address = readOptional("Alamat", teacher.getAddress());
        String education = readOptional("Pendidikan", teacher.getEducation());

        System.out.print("Tanggal bergabung [" + teacher.getJoinDate() + "]: ");
        String dateInput = scanner.nextLine().trim();

        LocalDate joinDate = dateInput.isEmpty()
                ? teacher.getJoinDate()
                : LocalDate.parse(dateInput);

        teacherService.updateTeacher(
                id, nip, name, gender, email, phone,
                address, education, joinDate);

        System.out.println("Data guru berhasil diperbarui.");
    }

    private void deactivateTeacher() {

        System.out.print("Masukkan ID guru: ");
        Long id = readLong();

        teacherService.deleteTeacher(id);

        System.out.println("Guru berhasil dinonaktifkan.");
    }

    private void displayTeachers(List<Teacher> teachers) {

        System.out.println("\n========== DAFTAR GURU ==========");

        if (teachers.isEmpty()) {
            System.out.println("Belum ada data guru.");
            return;
        }

        for (Teacher teacher : teachers) {

            System.out.println("----------------------------------");
            System.out.println("ID         : " + teacher.getId());
            System.out.println("NIP        : " + teacher.getNip());
            System.out.println("Nama       : " + teacher.getFullName());
            System.out.println("Gender     : " + teacher.getGender());
            System.out.println("Email      : " + teacher.getEmail());
            System.out.println("Telepon    : " + teacher.getPhone());
            System.out.println("Alamat     : " + teacher.getAddress());
            System.out.println("Pendidikan : " + teacher.getEducation());
            System.out.println("Bergabung  : " + teacher.getJoinDate());
            System.out.println("Status     : " + (teacher.isActive() ? "Aktif" : "Nonaktif"));
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