package id.ac.sekolah.manajemenkelas.academic.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class AcademicService {

    private static final Set<String> TRANSITION_STATUSES = Set.of(
            StudentPlacement.STATUS_NAIK_KELAS,
            StudentPlacement.STATUS_PINDAH);

    private static final Set<String> COMPLETION_STATUSES = Set.of(
            StudentPlacement.STATUS_LULUS,
            StudentPlacement.STATUS_KELUAR);

    private final AcademicDAO academicDAO;
    private final StudentDAO studentDAO;
    private final TeacherDAO teacherDAO;

    public AcademicService(AcademicDAO academicDAO) {
        this(academicDAO, null, null);
    }

    public AcademicService(
            AcademicDAO academicDAO,
            StudentDAO studentDAO,
            TeacherDAO teacherDAO) {
        if (academicDAO == null) {
            throw new IllegalArgumentException("AcademicDAO tidak boleh null.");
        }
        this.academicDAO = academicDAO;
        this.studentDAO = studentDAO;
        this.teacherDAO = teacherDAO;
    }

    // ==========================================
    // AUTHORIZATION GUARD
    // ==========================================

    private void requireAdminIfAuthenticated() {
        SessionContext.getInstance().requireAdminIfAuthenticated("akademik");
    }

    // ==========================================
    // ACADEMIC YEAR
    // ==========================================

    public AcademicYear createAcademicYear(
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        requireAdminIfAuthenticated();
        validateAcademicYear(name, startDate, endDate);

        if (academicDAO.findAcademicYearByName(name.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Tahun ajaran sudah terdaftar.");
        }

        AcademicYear academicYear = new AcademicYear(
                null,
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

        requireAdminIfAuthenticated();

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
        requireAdminIfAuthenticated();

        AcademicYear academicYear = academicDAO.findAcademicYearById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        academicYear.setActive(false);
        academicDAO.updateAcademicYear(academicYear);
    }

    public void activateAcademicYear(Long id) {
        requireAdminIfAuthenticated();

        AcademicYear academicYear = academicDAO.findAcademicYearById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        academicYear.setActive(true);
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
    // SEMESTER
    // ==========================================

    public Semester createSemester(
            Long academicYearId,
            int semesterNumber,
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        requireAdminIfAuthenticated();

        AcademicYear academicYear = academicDAO.findAcademicYearById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tahun ajaran tidak ditemukan."));

        validateSemester(academicYear, semesterNumber, name, startDate, endDate);

        boolean duplicate = academicDAO.findSemestersByAcademicYearId(academicYearId).stream()
                .anyMatch(s -> s.getSemesterNumber() == semesterNumber);

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Semester " + semesterNumber + " sudah terdaftar pada tahun ajaran ini.");
        }

        Semester semester = new Semester(
                null,
                academicYearId,
                semesterNumber,
                name.trim(),
                startDate,
                endDate,
                true);

        academicDAO.saveSemester(semester);
        return semester;
    }

    public List<Semester> getAllSemesters() {
        return academicDAO.findAllSemesters();
    }

    public List<Semester> getSemestersByAcademicYear(Long academicYearId) {
        return academicDAO.findSemestersByAcademicYearId(academicYearId);
    }

    public Optional<Semester> getSemesterById(Long id) {
        return academicDAO.findSemesterById(id);
    }

    public Semester updateSemester(
            Long id,
            Long academicYearId,
            int semesterNumber,
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        requireAdminIfAuthenticated();

        Semester existing = academicDAO.findSemesterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semester tidak ditemukan."));

        AcademicYear academicYear = academicDAO.findAcademicYearById(academicYearId)
                .orElseThrow(() -> new IllegalArgumentException("Tahun ajaran tidak ditemukan."));

        validateSemester(academicYear, semesterNumber, name, startDate, endDate);

        boolean duplicate = academicDAO.findSemestersByAcademicYearId(academicYearId).stream()
                .anyMatch(s -> !s.getId().equals(id) && s.getSemesterNumber() == semesterNumber);

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Semester " + semesterNumber + " sudah terdaftar pada tahun ajaran ini.");
        }

        existing.setAcademicYearId(academicYearId);
        existing.setSemesterNumber(semesterNumber);
        existing.setName(name.trim());
        existing.setStartDate(startDate);
        existing.setEndDate(endDate);

        academicDAO.updateSemester(existing);
        return existing;
    }

    public void deactivateSemester(Long id) {
        requireAdminIfAuthenticated();

        Semester semester = academicDAO.findSemesterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semester tidak ditemukan."));

        semester.setActive(false);
        academicDAO.updateSemester(semester);
    }

    public void activateSemester(Long id) {
        requireAdminIfAuthenticated();

        Semester semester = academicDAO.findSemesterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semester tidak ditemukan."));

        semester.setActive(true);
        academicDAO.updateSemester(semester);
    }

    private void validateSemester(
            AcademicYear academicYear,
            int semesterNumber,
            String name,
            LocalDate startDate,
            LocalDate endDate) {

        if (semesterNumber != 1 && semesterNumber != 2) {
            throw new IllegalArgumentException("Nomor semester harus 1 atau 2.");
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nama semester wajib diisi.");
        }

        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new IllegalArgumentException("Rentang tanggal semester tidak valid.");
        }

        if (startDate.isBefore(academicYear.getStartDate())
                || endDate.isAfter(academicYear.getEndDate())) {
            throw new IllegalArgumentException(
                    "Rentang tanggal semester harus berada dalam rentang tahun ajaran ("
                            + academicYear.getStartDate() + " s.d. "
                            + academicYear.getEndDate() + ").");
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

        requireAdminIfAuthenticated();

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
                null,
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

    public Optional<SchoolClass> getClassById(Long id) {
        return academicDAO.findClassById(id);
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

        requireAdminIfAuthenticated();

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

        int activeStudents = academicDAO.findActivePlacementsByClassId(id).size();
        if (capacity < activeStudents) {
            throw new IllegalArgumentException(
                    "Kapasitas kelas (" + capacity + ") tidak boleh kurang dari jumlah siswa aktif (" + activeStudents + ").");
        }

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
        requireAdminIfAuthenticated();

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

        requireAdminIfAuthenticated();
        validateSubject(code, name);

        if (academicDAO.findSubjectByCode(code.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Kode mata pelajaran sudah terdaftar.");
        }

        Subject subject = new Subject(
                null,
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

        requireAdminIfAuthenticated();

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
        requireAdminIfAuthenticated();

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

    // ==========================================
    // HOMEROOM ASSIGNMENT
    // ==========================================

    public HomeroomAssignment assignHomeroomTeacher(
            Long classId,
            Long teacherId,
            LocalDate startDate) {

        requireAdminIfAuthenticated();

        SchoolClass schoolClass = academicDAO.findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Kelas tidak ditemukan."));
        if (!schoolClass.isActive()) {
            throw new IllegalArgumentException("Kelas tidak aktif.");
        }

        validateTeacherExistsAndActive(teacherId);

        if (startDate == null) {
            throw new IllegalArgumentException("Tanggal mulai penugasan wajib diisi.");
        }

        if (academicDAO.findActiveHomeroomByClassId(classId).isPresent()) {
            throw new IllegalArgumentException(
                    "Kelas sudah memiliki wali kelas aktif. Akhiri penugasan sebelumnya terlebih dahulu.");
        }

        HomeroomAssignment assignment = new HomeroomAssignment(
                null,
                classId,
                teacherId,
                startDate,
                null,
                true);

        academicDAO.saveHomeroomAssignment(assignment);
        return assignment;
    }

    public List<HomeroomAssignment> getAllHomeroomAssignments() {
        return academicDAO.findAllHomeroomAssignments();
    }

    public Optional<HomeroomAssignment> getActiveHomeroomByClass(Long classId) {
        return academicDAO.findActiveHomeroomByClassId(classId);
    }

    public HomeroomAssignment endHomeroomAssignment(Long assignmentId, LocalDate endDate) {
        requireAdminIfAuthenticated();

        HomeroomAssignment assignment = academicDAO.findHomeroomAssignmentById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Penugasan wali kelas tidak ditemukan."));

        if (!assignment.isActive()) {
            throw new IllegalArgumentException("Penugasan wali kelas sudah tidak aktif.");
        }

        if (endDate == null || endDate.isBefore(assignment.getStartDate())) {
            throw new IllegalArgumentException(
                    "Tanggal selesai tidak valid atau mendahului tanggal mulai.");
        }

        assignment.setEndDate(endDate);
        assignment.setActive(false);
        academicDAO.updateHomeroomAssignment(assignment);
        return assignment;
    }

    // ==========================================
    // TEACHING ASSIGNMENT
    // ==========================================

    public TeachingAssignment createTeachingAssignment(
            Long teacherId,
            Long classId,
            Long subjectId,
            Long semesterId) {

        requireAdminIfAuthenticated();

        validateTeacherExistsAndActive(teacherId);

        SchoolClass schoolClass = academicDAO.findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Kelas tidak ditemukan."));
        if (!schoolClass.isActive()) {
            throw new IllegalArgumentException("Kelas tidak aktif.");
        }

        Subject subject = academicDAO.findSubjectById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Mata pelajaran tidak ditemukan."));
        if (!subject.isActive()) {
            throw new IllegalArgumentException("Mata pelajaran tidak aktif.");
        }

        Semester semester = academicDAO.findSemesterById(semesterId)
                .orElseThrow(() -> new IllegalArgumentException("Semester tidak ditemukan."));
        if (!semester.isActive()) {
            throw new IllegalArgumentException("Semester tidak aktif.");
        }

        if (!semester.getAcademicYearId().equals(schoolClass.getAcademicYearId())) {
            throw new IllegalArgumentException(
                    "Semester dan kelas harus berada pada tahun ajaran yang sama.");
        }

        boolean duplicate = academicDAO.findAllTeachingAssignments().stream()
                .anyMatch(a -> a.getTeacherId().equals(teacherId)
                        && a.getClassId().equals(classId)
                        && a.getSubjectId().equals(subjectId)
                        && a.getSemesterId().equals(semesterId));

        if (duplicate) {
            throw new IllegalArgumentException(
                    "Penugasan mengajar untuk guru, kelas, mata pelajaran, dan semester ini sudah ada.");
        }

        TeachingAssignment assignment = new TeachingAssignment(
                null,
                teacherId,
                classId,
                subjectId,
                semesterId,
                true);

        academicDAO.saveTeachingAssignment(assignment);
        return assignment;
    }

    public List<TeachingAssignment> getAllTeachingAssignments() {
        return academicDAO.findAllTeachingAssignments();
    }

    public List<TeachingAssignment> getTeachingAssignmentsByTeacher(Long teacherId) {
        return academicDAO.findTeachingAssignmentsByTeacherId(teacherId);
    }

    public List<TeachingAssignment> getTeachingAssignmentsByClass(Long classId) {
        return academicDAO.findTeachingAssignmentsByClassId(classId);
    }

    public List<TeachingAssignment> getTeachingAssignmentsBySemester(Long semesterId) {
        return academicDAO.findTeachingAssignmentsBySemesterId(semesterId);
    }

    public Optional<TeachingAssignment> getTeachingAssignmentById(Long id) {
        return academicDAO.findTeachingAssignmentById(id);
    }

    public void deactivateTeachingAssignment(Long assignmentId) {
        requireAdminIfAuthenticated();

        TeachingAssignment assignment = academicDAO.findTeachingAssignmentById(assignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Penugasan mengajar tidak ditemukan."));

        assignment.setActive(false);
        academicDAO.updateTeachingAssignment(assignment);
    }

    // ==========================================
    // STUDENT PLACEMENT
    // ==========================================

    public StudentPlacement placeStudentInClass(
            Long studentId,
            Long classId,
            LocalDate entryDate) {

        requireAdminIfAuthenticated();

        validateStudentExistsAndActive(studentId);

        SchoolClass schoolClass = academicDAO.findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Kelas tidak ditemukan."));
        if (!schoolClass.isActive()) {
            throw new IllegalArgumentException("Kelas tidak aktif.");
        }

        if (entryDate == null) {
            throw new IllegalArgumentException("Tanggal masuk kelas wajib diisi.");
        }

        if (academicDAO.findActivePlacementByStudentId(studentId).isPresent()) {
            throw new IllegalArgumentException(
                    "Siswa masih memiliki penempatan kelas aktif. Gunakan fitur pindah atau naik kelas.");
        }

        int currentCount = academicDAO.findActivePlacementsByClassId(classId).size();
        if (currentCount >= schoolClass.getCapacity()) {
            throw new IllegalArgumentException("Kapasitas kelas sudah penuh.");
        }

        StudentPlacement placement = new StudentPlacement(
                null,
                studentId,
                classId,
                schoolClass.getAcademicYearId(),
                entryDate,
                null,
                StudentPlacement.STATUS_AKTIF);

        academicDAO.saveStudentPlacement(placement);
        return placement;
    }

    public StudentPlacement promoteOrTransferStudent(
            Long studentId,
            Long targetClassId,
            LocalDate transitionDate,
            String closingStatus) {

        requireAdminIfAuthenticated();

        validateStudentExistsAndActive(studentId);

        if (closingStatus == null || !TRANSITION_STATUSES.contains(closingStatus.trim().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Status perpindahan harus NAIK_KELAS atau PINDAH.");
        }

        SchoolClass targetClass = academicDAO.findClassById(targetClassId)
                .orElseThrow(() -> new IllegalArgumentException("Kelas tujuan tidak ditemukan."));
        if (!targetClass.isActive()) {
            throw new IllegalArgumentException("Kelas tujuan tidak aktif.");
        }

        StudentPlacement currentPlacement = academicDAO.findActivePlacementByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Siswa tidak memiliki penempatan kelas aktif."));

        if (currentPlacement.getClassId().equals(targetClassId)) {
            throw new IllegalArgumentException("Kelas tujuan tidak boleh sama dengan kelas saat ini.");
        }

        if (transitionDate == null || transitionDate.isBefore(currentPlacement.getEntryDate())) {
            throw new IllegalArgumentException(
                    "Tanggal pindah/naik kelas tidak valid atau mendahului tanggal masuk sebelumnya.");
        }

        int targetCount = academicDAO.findActivePlacementsByClassId(targetClassId).size();
        if (targetCount >= targetClass.getCapacity()) {
            throw new IllegalArgumentException("Kapasitas kelas tujuan sudah penuh.");
        }

        currentPlacement.setExitDate(transitionDate);
        currentPlacement.setStatus(closingStatus.trim().toUpperCase());

        StudentPlacement newPlacement = new StudentPlacement(
                null,
                studentId,
                targetClassId,
                targetClass.getAcademicYearId(),
                transitionDate,
                null,
                StudentPlacement.STATUS_AKTIF);

        academicDAO.transitionStudentPlacement(currentPlacement, newPlacement);
        return newPlacement;
    }

    public StudentPlacement completeStudentPlacement(
            Long studentId,
            LocalDate exitDate,
            String finalStatus) {

        requireAdminIfAuthenticated();

        if (finalStatus == null || !COMPLETION_STATUSES.contains(finalStatus.trim().toUpperCase())) {
            throw new IllegalArgumentException(
                    "Status akhir penempatan harus LULUS atau KELUAR.");
        }

        StudentPlacement currentPlacement = academicDAO.findActivePlacementByStudentId(studentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Siswa tidak memiliki penempatan kelas aktif."));

        if (exitDate == null || exitDate.isBefore(currentPlacement.getEntryDate())) {
            throw new IllegalArgumentException(
                    "Tanggal keluar tidak valid atau mendahului tanggal masuk.");
        }

        currentPlacement.setExitDate(exitDate);
        currentPlacement.setStatus(finalStatus.trim().toUpperCase());
        academicDAO.updateStudentPlacement(currentPlacement);
        return currentPlacement;
    }

    public Optional<StudentPlacement> getCurrentPlacementByStudent(Long studentId) {
        return academicDAO.findActivePlacementByStudentId(studentId);
    }

    public List<StudentPlacement> getPlacementHistoryByStudent(Long studentId) {
        return academicDAO.findPlacementHistoryByStudentId(studentId);
    }

    public List<StudentPlacement> getActivePlacementsByClass(Long classId) {
        return academicDAO.findActivePlacementsByClassId(classId);
    }

    public List<StudentPlacement> getAllStudentPlacements() {
        return academicDAO.findAllStudentPlacements();
    }

    private void validateTeacherExistsAndActive(Long teacherId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("ID guru wajib diisi.");
        }
        if (teacherDAO != null) {
            Teacher teacher = teacherDAO.findById(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Data guru tidak ditemukan."));
            if (!teacher.isActive()) {
                throw new IllegalArgumentException("Guru tidak aktif.");
            }
        }
    }

    private void validateStudentExistsAndActive(Long studentId) {
        if (studentId == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }
        if (studentDAO != null) {
            Student student = studentDAO.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));
            if (!student.isActive()) {
                throw new IllegalArgumentException("Siswa tidak aktif.");
            }
        }
    }
}
