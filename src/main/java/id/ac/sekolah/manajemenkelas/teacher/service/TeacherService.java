package id.ac.sekolah.manajemenkelas.teacher.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class TeacherService {

    private final TeacherDAO teacherDAO;

    public TeacherService(TeacherDAO teacherDAO) {
        this.teacherDAO = teacherDAO;
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

        SessionContext.getInstance().requireAdminIfAuthenticated("guru");

        validateTeacherData(
                nip, fullName, gender,
                email, address, education, joinDate);

        if (teacherDAO.existsByNip(nip.trim())) {
            throw new IllegalArgumentException("NIP sudah terdaftar.");
        }

        String normalizedEmail = email == null ? "" : email.trim();
        if (!normalizedEmail.isEmpty()) {
            boolean emailUsed = teacherDAO.findAll().stream()
                    .anyMatch(teacher -> teacher.getEmail() != null
                            && teacher.getEmail().equalsIgnoreCase(normalizedEmail));
            if (emailUsed) {
                throw new IllegalArgumentException("Email sudah terdaftar.");
            }
        }

        Teacher teacher = new Teacher(
                null,
                nip.trim(),
                fullName.trim(),
                gender.trim().toUpperCase(),
                normalizedEmail,
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

        SessionContext.getInstance().requireAdminIfAuthenticated("guru");

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

        String normalizedEmail = email == null ? "" : email.trim();
        if (!normalizedEmail.isEmpty()) {
            boolean emailUsedByOther = teacherDAO.findAll().stream()
                    .anyMatch(teacher -> !teacher.getId().equals(id)
                            && teacher.getEmail() != null
                            && teacher.getEmail().equalsIgnoreCase(normalizedEmail));
            if (emailUsedByOther) {
                throw new IllegalArgumentException("Email sudah digunakan guru lain.");
            }
        }

        existing.setNip(nip.trim());
        existing.setFullName(fullName.trim());
        existing.setGender(gender.trim().toUpperCase());
        existing.setEmail(normalizedEmail);
        existing.setPhone(phone == null ? "" : phone.trim());
        existing.setAddress(address.trim());
        existing.setEducation(education.trim());
        existing.setJoinDate(joinDate);

        teacherDAO.update(existing);

        return existing;
    }

    public void deleteTeacher(Long id) {

        SessionContext.getInstance().requireAdminIfAuthenticated("guru");

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
                .filter(teacher -> (teacher.getFullName() != null
                                && teacher.getFullName().toLowerCase().contains(search))
                        || (teacher.getNip() != null
                                && teacher.getNip().toLowerCase().contains(search))
                        || (teacher.getEmail() != null
                                && teacher.getEmail().toLowerCase().contains(search)))
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