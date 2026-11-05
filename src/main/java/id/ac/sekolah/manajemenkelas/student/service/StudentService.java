package id.ac.sekolah.manajemenkelas.student.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;

public class StudentService {

    private final StudentDAO studentDAO;
    private final AtomicLong idGenerator;

    public StudentService(StudentDAO studentDAO) {
        this.studentDAO = studentDAO;
        this.idGenerator = new AtomicLong(1);
    }

    public Student createStudent(
            String nis,
            String nisn,
            String fullName,
            String gender,
            LocalDate birthDate,
            String birthPlace,
            String address,
            String parentName,
            String parentPhone) {

        validateStudentData(
                nis, nisn, fullName, gender,
                birthDate, birthPlace, address);

        if (studentDAO.existsByNis(nis.trim())) {
            throw new IllegalArgumentException("NIS sudah terdaftar.");
        }

        if (studentDAO.existsByNisn(nisn.trim())) {
            throw new IllegalArgumentException("NISN sudah terdaftar.");
        }

        Student student = new Student(
                idGenerator.getAndIncrement(),
                nis.trim(),
                nisn.trim(),
                fullName.trim(),
                gender,
                birthDate,
                birthPlace.trim(),
                address.trim(),
                parentName == null ? "" : parentName.trim(),
                parentPhone == null ? "" : parentPhone.trim(),
                true);

        studentDAO.save(student);

        return student;
    }

    public List<Student> getAllStudents() {
        return studentDAO.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return studentDAO.findById(id);
    }

    public Student updateStudent(
            Long id,
            String nis,
            String nisn,
            String fullName,
            String gender,
            LocalDate birthDate,
            String birthPlace,
            String address,
            String parentName,
            String parentPhone) {

        Student existing = studentDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));

        validateStudentData(
                nis, nisn, fullName, gender,
                birthDate, birthPlace, address);

        Optional<Student> nisOwner = studentDAO.findByNis(nis.trim());

        if (nisOwner.isPresent()
                && !nisOwner.get().getId().equals(id)) {
            throw new IllegalArgumentException("NIS sudah digunakan siswa lain.");
        }

        Optional<Student> nisnOwner = studentDAO.findByNisn(nisn.trim());

        if (nisnOwner.isPresent()
                && !nisnOwner.get().getId().equals(id)) {
            throw new IllegalArgumentException("NISN sudah digunakan siswa lain.");
        }

        existing.setNis(nis.trim());
        existing.setNisn(nisn.trim());
        existing.setFullName(fullName.trim());
        existing.setGender(gender);
        existing.setBirthDate(birthDate);
        existing.setBirthPlace(birthPlace.trim());
        existing.setAddress(address.trim());
        existing.setParentName(parentName == null ? "" : parentName.trim());
        existing.setParentPhone(parentPhone == null ? "" : parentPhone.trim());

        studentDAO.update(existing);

        return existing;
    }

    public void deleteStudent(Long id) {

        Student student = studentDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));

        student.setActive(false);
        studentDAO.update(student);
    }

    public List<Student> searchStudents(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllStudents();
        }

        String search = keyword.trim().toLowerCase();

        return studentDAO.findAll().stream()
                .filter(student -> student.getFullName().toLowerCase().contains(search)
                        || student.getNis().toLowerCase().contains(search)
                        || student.getNisn().toLowerCase().contains(search))
                .toList();
    }

    private void validateStudentData(
            String nis,
            String nisn,
            String fullName,
            String gender,
            LocalDate birthDate,
            String birthPlace,
            String address) {

        if (nis == null || nis.isBlank()) {
            throw new IllegalArgumentException("NIS wajib diisi.");
        }

        if (nisn == null || nisn.isBlank()) {
            throw new IllegalArgumentException("NISN wajib diisi.");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Nama siswa wajib diisi.");
        }

        if (gender == null
                || (!gender.equalsIgnoreCase("L")
                        && !gender.equalsIgnoreCase("P"))) {
            throw new IllegalArgumentException("Jenis kelamin harus L atau P.");
        }

        if (birthDate == null || birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Tanggal lahir tidak valid.");
        }

        if (birthPlace == null || birthPlace.isBlank()) {
            throw new IllegalArgumentException("Tempat lahir wajib diisi.");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Alamat wajib diisi.");
        }
    }
}