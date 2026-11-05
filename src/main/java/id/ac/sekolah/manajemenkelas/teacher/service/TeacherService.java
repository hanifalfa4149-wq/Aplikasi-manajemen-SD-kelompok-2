package id.ac.sekolah.manajemenkelas.teacher.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class TeacherService {

    private final TeacherDAO teacherDAO;
    private final AtomicLong idGenerator;

    public TeacherService(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
        this.idGenerator = new AtomicLong(1);
    }

    public Teacher createTeacher(
            String nip,
            String fullName,
            String gender,
            String email,
            String phone,
            String address,
            String education,
            LocalDate joinDate) {

        validateTeacherData(
                nip, fullName, gender,
                email, address, education, joinDate);

        if (teacherDAO.existsByNip(nip.trim())) {
            throw new IllegalArgumentException("NIP sudah terdaftar.");
        }

        Teacher teacher = new Teacher(
                idGenerator.getAndIncrement(),
                nip.trim(),
                fullName.trim(),
                gender.toUpperCase(),
                email == null ? "" : email.trim(),
                phone == null ? "" : phone.trim(),
                address.trim(),
                education.trim(),
                joinDate,
                true);

        teacherDAO.save(teacher);

        return teacher;
    }

    public List<Teacher> getAllTeachers() {
        return teacherDAO.findAll();
    }

    public Optional<Teacher> getTeacherById(Long id) {
        return teacherDAO.findById(id);
    }

    public Teacher updateTeacher(
            Long id,
            String nip,
            String fullName,
            String gender,
            String email,
            String phone,
            String address,
            String education,
            LocalDate joinDate) {

        Teacher existing = teacherDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data guru tidak ditemukan."));

        validateTeacherData(
                nip, fullName, gender,
                email, address, education, joinDate);

        Optional<Teacher> nipOwner = teacherDAO.findByNip(nip.trim());

        if (nipOwner.isPresent()
                && !nipOwner.get().getId().equals(id)) {
            throw new IllegalArgumentException("NIP sudah digunakan guru lain.");
        }

        existing.setNip(nip.trim());
        existing.setFullName(fullName.trim());
        existing.setGender(gender.toUpperCase());
        existing.setEmail(email == null ? "" : email.trim());
        existing.setPhone(phone == null ? "" : phone.trim());
        existing.setAddress(address.trim());
        existing.setEducation(education.trim());
        existing.setJoinDate(joinDate);

        teacherDAO.update(existing);

        return existing;
    }

    public void deleteTeacher(Long id) {

        Teacher teacher = teacherDAO.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Data guru tidak ditemukan."));

        teacher.setActive(false);
        teacherDAO.update(teacher);
    }

    public List<Teacher> searchTeachers(String keyword) {

        if (keyword == null || keyword.isBlank()) {
            return getAllTeachers();
        }

        String search = keyword.trim().toLowerCase();

        return teacherDAO.findAll().stream()
                .filter(teacher -> teacher.getFullName().toLowerCase().contains(search)
                        || teacher.getNip().toLowerCase().contains(search)
                        || teacher.getEmail().toLowerCase().contains(search))
                .toList();
    }

    private void validateTeacherData(
            String nip,
            String fullName,
            String gender,
            String email,
            String address,
            String education,
            LocalDate joinDate) {

        if (nip == null || nip.isBlank()) {
            throw new IllegalArgumentException("NIP wajib diisi.");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Nama guru wajib diisi.");
        }

        if (gender == null
                || (!gender.equalsIgnoreCase("L")
                        && !gender.equalsIgnoreCase("P"))) {
            throw new IllegalArgumentException("Jenis kelamin harus L atau P.");
        }

        if (email != null && !email.isBlank()
                && !email.matches("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Format email tidak valid.");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Alamat wajib diisi.");
        }

        if (education == null || education.isBlank()) {
            throw new IllegalArgumentException("Pendidikan terakhir wajib diisi.");
        }

        if (joinDate == null || joinDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Tanggal bergabung tidak valid.");
        }
    }
}