package id.ac.sekolah.manajemenkelas.attendance.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.attendance.dao.AttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.attendance.model.AttendanceSummary;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;

public class AttendanceService {

    public record StudentAttendanceEntry(Long studentId, String status, String notes) {
    }

    private static final Set<String> VALID_STATUSES = Set.of(
            Attendance.STATUS_HADIR,
            Attendance.STATUS_SAKIT,
            Attendance.STATUS_IZIN,
            Attendance.STATUS_ALPA);

    private final AttendanceDAO attendanceDAO;
    private final AcademicDAO academicDAO;
    private final StudentDAO studentDAO;

    public AttendanceService(
            AttendanceDAO attendanceDAO,
            AcademicDAO academicDAO,
            StudentDAO studentDAO) {
        if (attendanceDAO == null || academicDAO == null || studentDAO == null) {
            throw new IllegalArgumentException("DAO dependensi AttendanceService tidak boleh null.");
        }
        this.attendanceDAO = attendanceDAO;
        this.academicDAO = academicDAO;
        this.studentDAO = studentDAO;
    }

    public Attendance recordAttendance(
            Long studentId,
            Long classId,
            LocalDate attendanceDate,
            String status,
            String notes) {

        requireClassAuthorization(classId);
        validateDate(attendanceDate);
        String normalizedStatus = normalizeAndValidateStatus(status);
        validateStudentInClassOnDate(studentId, classId, attendanceDate);

        if (attendanceDAO.findByStudentAndDate(studentId, attendanceDate).isPresent()) {
            throw new IllegalArgumentException(
                    "Absensi siswa pada tanggal " + attendanceDate + " sudah tercatat.");
        }

        Long recordedBy = SessionContext.getInstance().getUserId();
        Attendance attendance = new Attendance(
                null,
                studentId,
                classId,
                attendanceDate,
                normalizedStatus,
                notes == null ? "" : notes.trim(),
                recordedBy);

        attendanceDAO.save(attendance);
        return attendance;
    }

    public List<Attendance> recordClassAttendance(
            Long classId,
            LocalDate attendanceDate,
            List<StudentAttendanceEntry> entries) {

        requireClassAuthorization(classId);
        validateDate(attendanceDate);

        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Daftar absensi siswa tidak boleh kosong.");
        }

        Long recordedBy = SessionContext.getInstance().getUserId();
        Set<Long> seenStudents = new HashSet<>();
        List<Attendance> toSave = new ArrayList<>();

        for (StudentAttendanceEntry entry : entries) {
            if (entry == null || entry.studentId() == null) {
                throw new IllegalArgumentException("ID siswa wajib diisi.");
            }
            if (!seenStudents.add(entry.studentId())) {
                throw new IllegalArgumentException(
                        "Siswa dengan ID " + entry.studentId() + " muncul lebih dari sekali.");
            }

            String normalizedStatus = normalizeAndValidateStatus(entry.status());
            validateStudentInClassOnDate(entry.studentId(), classId, attendanceDate);

            if (attendanceDAO.findByStudentAndDate(entry.studentId(), attendanceDate).isPresent()) {
                throw new IllegalArgumentException(
                        "Absensi siswa ID " + entry.studentId() + " pada tanggal "
                                + attendanceDate + " sudah tercatat.");
            }

            toSave.add(new Attendance(
                    null,
                    entry.studentId(),
                    classId,
                    attendanceDate,
                    normalizedStatus,
                    entry.notes() == null ? "" : entry.notes().trim(),
                    recordedBy));
        }

        attendanceDAO.saveAll(toSave);
        return toSave;
    }

    public Attendance updateAttendance(Long attendanceId, String status, String notes) {
        if (attendanceId == null) {
            throw new IllegalArgumentException("ID absensi wajib diisi.");
        }

        Attendance existing = attendanceDAO.findById(attendanceId)
                .orElseThrow(() -> new IllegalArgumentException("Data absensi tidak ditemukan."));

        requireClassAuthorization(existing.getClassId());
        String normalizedStatus = normalizeAndValidateStatus(status);

        existing.setStatus(normalizedStatus);
        existing.setNotes(notes == null ? "" : notes.trim());
        existing.setRecordedBy(SessionContext.getInstance().getUserId());

        attendanceDAO.update(existing);
        return existing;
    }

    public Optional<Attendance> getAttendanceById(Long id) {
        Optional<Attendance> found = attendanceDAO.findById(id);
        found.ifPresent(a -> requireClassAuthorization(a.getClassId()));
        return found;
    }

    public List<Attendance> getAttendanceByDate(LocalDate date) {
        requireLoggedIn();
        validateDate(date);

        List<Attendance> allOnDate = attendanceDAO.findByDate(date);
        SessionContext session = SessionContext.getInstance();
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            return allOnDate;
        }
        if (!session.hasRole(RoleMapper.ROLE_TEACHER)) {
            throw new IllegalArgumentException("Akses ditolak: peran tidak diizinkan.");
        }
        Long teacherId = session.getTeacherId();
        if (teacherId == null) {
            throw new IllegalArgumentException(
                    "Akses ditolak: akun guru belum terhubung dengan profil guru.");
        }

        Set<Long> assignedClasses = getAuthorizedClassIdsForTeacher(teacherId);
        return allOnDate.stream()
                .filter(a -> assignedClasses.contains(a.getClassId()))
                .toList();
    }

    public List<Attendance> getAttendanceByClass(Long classId) {
        requireClassAuthorization(classId);
        return attendanceDAO.findByClassId(classId);
    }

    public List<Attendance> getAttendanceByClassAndDate(Long classId, LocalDate date) {
        requireClassAuthorization(classId);
        validateDate(date);
        return attendanceDAO.findByClassAndDate(classId, date);
    }

    public List<Attendance> getAttendanceByStudent(Long studentId) {
        requireLoggedIn();
        if (studentId == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }

        studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));

        List<Attendance> history = attendanceDAO.findByStudentId(studentId);
        SessionContext session = SessionContext.getInstance();
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            return history;
        }

        Set<Long> assignedClasses = getAuthorizedClassIdsForTeacher(session.getTeacherId());
        boolean hasAccessToCurrentClass = academicDAO.findActivePlacementByStudentId(studentId)
                .map(p -> assignedClasses.contains(p.getClassId()))
                .orElse(false);

        List<Attendance> filtered = history.stream()
                .filter(a -> assignedClasses.contains(a.getClassId()))
                .toList();

        if (!hasAccessToCurrentClass && filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    "Akses ditolak: guru tidak ditugaskan pada kelas siswa ini.");
        }
        return filtered;
    }

    public AttendanceSummary getClassAttendanceSummary(
            Long classId,
            LocalDate startDate,
            LocalDate endDate) {

        requireClassAuthorization(classId);

        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Rentang tanggal rekapitulasi tidak valid.");
        }

        List<Attendance> records = attendanceDAO.findByClassAndDateRange(classId, startDate, endDate);
        int hadir = 0;
        int sakit = 0;
        int izin = 0;
        int alpa = 0;

        for (Attendance record : records) {
            switch (record.getStatus()) {
                case Attendance.STATUS_HADIR -> hadir++;
                case Attendance.STATUS_SAKIT -> sakit++;
                case Attendance.STATUS_IZIN -> izin++;
                case Attendance.STATUS_ALPA -> alpa++;
                default -> {
                }
            }
        }

        return new AttendanceSummary(
                classId,
                startDate,
                endDate,
                records.size(),
                hadir,
                sakit,
                izin,
                alpa);
    }

    // ==========================================
    // AUTHORIZATION & VALIDATION HELPERS
    // ==========================================

    private void requireLoggedIn() {
        if (!SessionContext.getInstance().isLoggedIn()) {
            throw new IllegalArgumentException("Silakan login terlebih dahulu untuk mengakses absensi.");
        }
    }

    private void requireClassAuthorization(Long classId) {
        requireLoggedIn();
        if (classId == null) {
            throw new IllegalArgumentException("ID kelas wajib diisi.");
        }

        SchoolClass schoolClass = academicDAO.findClassById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Data kelas tidak ditemukan."));

        SessionContext session = SessionContext.getInstance();
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            return;
        }

        if (!session.hasRole(RoleMapper.ROLE_TEACHER)) {
            throw new IllegalArgumentException("Akses ditolak: peran tidak diizinkan.");
        }

        Long teacherId = session.getTeacherId();
        if (teacherId == null) {
            throw new IllegalArgumentException(
                    "Akses ditolak: akun guru belum terhubung dengan profil guru.");
        }

        Set<Long> authorizedClasses = getAuthorizedClassIdsForTeacher(teacherId);
        if (!authorizedClasses.contains(schoolClass.getId())) {
            throw new IllegalArgumentException(
                    "Akses ditolak: guru tidak memiliki penugasan pada kelas ini.");
        }
    }

    private Set<Long> getAuthorizedClassIdsForTeacher(Long teacherId) {
        if (teacherId == null) {
            return Set.of();
        }
        Set<Long> classIds = new HashSet<>();
        academicDAO.findAllHomeroomAssignments().stream()
                .filter(hr -> hr.isActive() && teacherId.equals(hr.getTeacherId()))
                .forEach(hr -> classIds.add(hr.getClassId()));
        academicDAO.findTeachingAssignmentsByTeacherId(teacherId).stream()
                .filter(ta -> ta.isActive())
                .forEach(ta -> classIds.add(ta.getClassId()));
        return classIds;
    }

    private void validateStudentInClassOnDate(Long studentId, Long classId, LocalDate date) {
        if (studentId == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }

        Student student = studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));
        if (!student.isActive()) {
            throw new IllegalArgumentException("Siswa tidak aktif.");
        }

        List<StudentPlacement> history = academicDAO.findPlacementHistoryByStudentId(studentId);
        boolean movedToAnotherClassOnDate = history.stream()
                .anyMatch(p -> !p.getClassId().equals(classId)
                        && p.getEntryDate().equals(date));

        boolean belongsToClassOnDate = history.stream()
                .filter(p -> p.getClassId().equals(classId))
                .anyMatch(p -> !date.isBefore(p.getEntryDate())
                        && (p.getExitDate() == null || !date.isAfter(p.getExitDate())));

        if (!belongsToClassOnDate || movedToAnotherClassOnDate) {
            throw new IllegalArgumentException(
                    "Siswa ID " + studentId + " tidak terdaftar pada kelas ID " + classId
                            + " pada tanggal " + date + ".");
        }
    }

    private static void validateDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Tanggal absensi wajib diisi.");
        }
    }

    private static String normalizeAndValidateStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status absensi wajib diisi.");
        }
        String normalized = status.trim().toUpperCase();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Status absensi tidak valid. Pilihan: HADIR, SAKIT, IZIN, ALPA.");
        }
        return normalized;
    }
}

