package id.ac.sekolah.manajemenkelas.academic.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;

public class AcademicService {

    private final AcademicDAO academicDAO;

    private final AtomicLong yearIdGenerator = new AtomicLong(1);
    private final AtomicLong classIdGenerator = new AtomicLong(1);
    private final AtomicLong subjectIdGenerator = new AtomicLong(1);

    public AcademicService(AcademicDAO academicDAO) {
        this.academicDAO = academicDAO;
    }

    // ==========================================
    // ACADEMIC YEAR
    // ==========================================

    public AcademicYear createAcademicYear(
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        validateAcademicYear(name, startDate, endDate);

        if (academicDAO.findAcademicYearByName(name.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Tahun ajaran sudah terdaftar.");
        }

        AcademicYear academicYear = new AcademicYear(
                yearIdGenerator.getAndIncrement(),
                name.trim(),
                startDate,
                endDate,
                true);

        academicDAO.saveAcademicYear(academicYear);

        return academicYear;
    }

    public List<AcademicYear> getAllAcademicYears() {
        return academicDAO.findAllAcademicYears();
    }

    public Optional<AcademicYear> getAcademicYearById(Long id) {
        return academicDAO.findAcademicYearById(id);
    }

    public AcademicYear updateAcademicYear(
            Long id,
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        AcademicYear existing = academicDAO.findAcademicYearById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        validateAcademicYear(name, startDate, endDate);

        Optional<AcademicYear> duplicate = academicDAO.findAcademicYearByName(name.trim());

        if (duplicate.isPresent()
                && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException(
                    "Nama tahun ajaran sudah digunakan.");
        }

        existing.setName(name.trim());
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);

        academicDAO.updateAcademicYear(existing);

        return existing;
    }

    public void deactivateAcademicYear(Long id) {

        AcademicYear academicYear = academicDAO.findAcademicYearById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        academicYear.setActive(false);
        academicDAO.updateAcademicYear(academicYear);
    }

    private void validateAcademicYear(
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Nama tahun ajaran wajib diisi.");
        }

        if (startDate == null || endDate == null
                || !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException(
                    "Rentang tanggal tahun ajaran tidak valid.");
        }
    }

    // ==========================================
    // SCHOOL CLASS
    // ==========================================

    public SchoolClass createClass(
            Long academicYearId,
            String name,
            int gradeLevel,
            int capacity) {

        AcademicYear academicYear = academicDAO.findAcademicYearById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        if (!academicYear.isActive()) {
            throw new IllegalArgumentException(
                    "Tahun ajaran tidak aktif.");
        }

        validateClass(name, gradeLevel, capacity);

        boolean duplicate = academicDAO.findAllClasses().stream()
                .anyMatch(schoolClass -> schoolClass.getAcademicYearId().equals(academicYearId)
                        && schoolClass.getName().equalsIgnoreCase(name.trim()));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Nama kelas sudah digunakan pada tahun ajaran tersebut.");
        }

        SchoolClass schoolClass = new SchoolClass(
                classIdGenerator.getAndIncrement(),
                academicYearId,
                name.trim(),
                gradeLevel,
                capacity,
                true);

        academicDAO.saveClass(schoolClass);

        return schoolClass;
    }

    public List<SchoolClass> getAllClasses() {
        return academicDAO.findAllClasses();
    }

    public List<SchoolClass> getClassesByAcademicYear(Long academicYearId) {

        return academicDAO.findAllClasses().stream()
                .filter(schoolClass -> schoolClass.getAcademicYearId().equals(academicYearId))
                .toList();
    }

    public SchoolClass updateClass(
            Long id,
            Long academicYearId,
            String name,
            int gradeLevel,
            int capacity) {

        SchoolClass existing = academicDAO.findClassById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Data kelas tidak ditemukan."));

        AcademicYear academicYear = academicDAO.findAcademicYearById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        if (!academicYear.isActive()) {
            throw new IllegalArgumentException(
                    "Tahun ajaran tidak aktif.");
        }

        validateClass(name, gradeLevel, capacity);

        boolean duplicate = academicDAO.findAllClasses().stream()
                .anyMatch(schoolClass -> !schoolClass.getId().equals(id)
                        && schoolClass.getAcademicYearId().equals(academicYearId)
                        && schoolClass.getName().equalsIgnoreCase(name.trim()));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Nama kelas sudah digunakan pada tahun ajaran tersebut.");
        }

        existing.setAcademicYearId(academicYearId);
        existing.setName(name.trim());
        existing.setGradeLevel(gradeLevel);
        existing.setCapacity(capacity);

        academicDAO.updateClass(existing);

        return existing;
    }

    public void deactivateClass(Long id) {

        SchoolClass schoolClass = academicDAO.findClassById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Data kelas tidak ditemukan."));

        schoolClass.setActive(false);
        academicDAO.updateClass(schoolClass);
    }

    private void validateClass(
            String name,
            int gradeLevel,
            int capacity) {

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Nama kelas wajib diisi.");
        }

        if (gradeLevel < 1 || gradeLevel > 6) {
            throw new IllegalArgumentException(
                    "Tingkat kelas harus antara 1 sampai 6.");
        }

        if (capacity < 1) {
            throw new IllegalArgumentException(
                    "Kapasitas kelas minimal 1 siswa.");
        }
    }

    // ==========================================
    // SUBJECT
    // ==========================================

    public Subject createSubject(
            String code,
            String name,
            String description) {

        validateSubject(code, name);

        if (academicDAO.findSubjectByCode(code.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Kode mata pelajaran sudah terdaftar.");
        }

        Subject subject = new Subject(
                subjectIdGenerator.getAndIncrement(),
                code.trim().toUpperCase(),
                name.trim(),
                description == null ? "" : description.trim(),
                true);

        academicDAO.saveSubject(subject);

        return subject;
    }

    public List<Subject> getAllSubjects() {
        return academicDAO.findAllSubjects();
    }

    public Optional<Subject> getSubjectById(Long id) {
        return academicDAO.findSubjectById(id);
    }

    public Subject updateSubject(
            Long id,
            String code,
            String name,
            String description) {

        Subject existing = academicDAO.findSubjectById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mata pelajaran tidak ditemukan."));

        validateSubject(code, name);

        Optional<Subject> duplicate = academicDAO.findSubjectByCode(code.trim());

        if (duplicate.isPresent()
                && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException(
                    "Kode mata pelajaran sudah digunakan.");
        }

        existing.setCode(code.trim().toUpperCase());
        existing.setName(name.trim());
        existing.setDescription(
                description == null ? "" : description.trim());

        academicDAO.updateSubject(existing);

        return existing;
    }

    public void deactivateSubject(Long id) {

        Subject subject = academicDAO.findSubjectById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mata pelajaran tidak ditemukan."));

        subject.setActive(false);
        academicDAO.updateSubject(subject);
    }

    private void validateSubject(String code, String name) {

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Kode mata pelajaran wajib diisi.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Nama mata pelajaran wajib diisi.");
        }
    }
}