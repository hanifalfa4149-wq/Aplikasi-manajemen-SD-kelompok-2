package id.ac.sekolah.manajemenkelas.assessment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.assessment.dao.AssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.AssessmentSummary;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScoreSummary;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;

public class AssessmentService {

    public record StudentScoreEntry(Long studentId, BigDecimal score, String notes) {
    }

    private static final BigDecimal MAX_DECIMAL_LIMIT = new BigDecimal("99999.99");
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Set<String> VALID_TYPES = Set.of(
            Assessment.TYPE_KUIS,
            Assessment.TYPE_TUGAS,
            Assessment.TYPE_UTS,
            Assessment.TYPE_UAS,
            Assessment.TYPE_PRAKTIK,
            Assessment.TYPE_PROYEK,
            Assessment.TYPE_LAINNYA);

    private final AssessmentDAO assessmentDAO;
    private final AcademicDAO academicDAO;
    private final StudentDAO studentDAO;

    public AssessmentService(
            AssessmentDAO assessmentDAO,
            AcademicDAO academicDAO,
            StudentDAO studentDAO) {
        if (assessmentDAO == null || academicDAO == null || studentDAO == null) {
            throw new IllegalArgumentException("DAO dependensi AssessmentService tidak boleh null.");
        }
        this.assessmentDAO = assessmentDAO;
        this.academicDAO = academicDAO;
        this.studentDAO = studentDAO;
    }

    // ==========================================
    // ASSESSMENT OPERATIONS
    // ==========================================

    public Assessment createAssessment(
            Long teachingAssignmentId,
            String title,
            String assessmentType,
            LocalDate assessmentDate,
            BigDecimal maxScore,
            String description) {

        TeachingAssignment ta = requireTeachingAssignmentWriteAuthorization(teachingAssignmentId);
        String validTitle = validateTitle(title);
        String validType = normalizeAndValidateType(assessmentType);
        validateAssessmentDate(assessmentDate, ta);
        BigDecimal validMaxScore = validateAndNormalizeMaxScore(maxScore);

        Assessment assessment = new Assessment(
                null,
                teachingAssignmentId,
                validTitle,
                validType,
                assessmentDate,
                validMaxScore,
                description == null ? "" : description.trim());

        assessmentDAO.saveAssessment(assessment);
        return assessment;
    }

    public Assessment updateAssessment(
            Long assessmentId,
            String title,
            String assessmentType,
            LocalDate assessmentDate,
            BigDecimal maxScore,
            String description) {

        if (assessmentId == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi.");
        }

        Assessment existing = assessmentDAO.findAssessmentById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data penilaian tidak ditemukan."));

        TeachingAssignment ta = requireTeachingAssignmentWriteAuthorization(existing.getTeachingAssignmentId());
        String validTitle = validateTitle(title);
        String validType = normalizeAndValidateType(assessmentType);
        validateAssessmentDate(assessmentDate, ta);
        BigDecimal validMaxScore = validateAndNormalizeMaxScore(maxScore);

        List<StudentScore> existingScores = assessmentDAO.findScoresByAssessmentId(assessmentId);
        for (StudentScore score : existingScores) {
            if (score.getScore().compareTo(validMaxScore) > 0) {
                throw new IllegalArgumentException(
                        "Nilai maksimal baru (" + validMaxScore + ") lebih kecil dari nilai siswa yang sudah ada ("
                                + score.getScore() + ").");
            }
        }

        existing.setTitle(validTitle);
        existing.setAssessmentType(validType);
        existing.setAssessmentDate(assessmentDate);
        existing.setMaxScore(validMaxScore);
        existing.setDescription(description == null ? "" : description.trim());

        assessmentDAO.updateAssessment(existing);
        return existing;
    }

    public Optional<Assessment> getAssessmentById(Long id) {
        Optional<Assessment> found = assessmentDAO.findAssessmentById(id);
        found.ifPresent(a -> requireTeachingAssignmentReadAuthorization(a.getTeachingAssignmentId()));
        return found;
    }

    public List<Assessment> getAllAssessments() {
        requireLoggedIn();
        SessionContext session = SessionContext.getInstance();
        List<Assessment> all = assessmentDAO.findAllAssessments();
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            return all;
        }

        Set<Long> teacherAssignmentIds = getAuthorizedTeachingAssignmentIds(requireTeacherIdForTeacherRole(session));
        return all.stream()
                .filter(a -> teacherAssignmentIds.contains(a.getTeachingAssignmentId()))
                .toList();
    }

    public List<Assessment> getAssessmentsByTeachingAssignment(Long teachingAssignmentId) {
        requireTeachingAssignmentReadAuthorization(teachingAssignmentId);
        return assessmentDAO.findAssessmentsByTeachingAssignmentId(teachingAssignmentId);
    }

    // ==========================================
    // STUDENT SCORE OPERATIONS
    // ==========================================

    public StudentScore recordScore(
            Long assessmentId,
            Long studentId,
            BigDecimal score,
            String notes) {

        Assessment assessment = getAuthorizedAssessmentForWrite(assessmentId);
        TeachingAssignment ta = requireTeachingAssignmentWriteAuthorization(assessment.getTeachingAssignmentId());
        BigDecimal validScore = validateAndNormalizeStudentScore(score, assessment.getMaxScore());
        validateStudentInClassOnDate(studentId, ta.getClassId(), assessment.getAssessmentDate());

        if (assessmentDAO.findScoreByAssessmentAndStudent(assessmentId, studentId).isPresent()) {
            throw new IllegalArgumentException(
                    "Nilai siswa ID " + studentId + " untuk penilaian ini sudah tercatat.");
        }

        StudentScore studentScore = new StudentScore(
                null,
                assessmentId,
                studentId,
                validScore,
                notes == null ? "" : notes.trim(),
                SessionContext.getInstance().getUserId());

        assessmentDAO.saveScore(studentScore);
        return studentScore;
    }

    public List<StudentScore> recordAssessmentScores(
            Long assessmentId,
            List<StudentScoreEntry> entries) {

        Assessment assessment = getAuthorizedAssessmentForWrite(assessmentId);
        TeachingAssignment ta = requireTeachingAssignmentWriteAuthorization(assessment.getTeachingAssignmentId());

        if (entries == null || entries.isEmpty()) {
            throw new IllegalArgumentException("Daftar nilai siswa tidak boleh kosong.");
        }

        Long recordedBy = SessionContext.getInstance().getUserId();
        Set<Long> seenStudents = new HashSet<>();
        List<StudentScore> toSave = new ArrayList<>();

        for (StudentScoreEntry entry : entries) {
            if (entry == null || entry.studentId() == null) {
                throw new IllegalArgumentException("ID siswa wajib diisi.");
            }
            if (!seenStudents.add(entry.studentId())) {
                throw new IllegalArgumentException(
                        "Siswa dengan ID " + entry.studentId() + " muncul lebih dari sekali.");
            }

            BigDecimal validScore = validateAndNormalizeStudentScore(entry.score(), assessment.getMaxScore());
            validateStudentInClassOnDate(entry.studentId(), ta.getClassId(), assessment.getAssessmentDate());

            if (assessmentDAO.findScoreByAssessmentAndStudent(assessmentId, entry.studentId()).isPresent()) {
                throw new IllegalArgumentException(
                        "Nilai siswa ID " + entry.studentId() + " untuk penilaian ini sudah tercatat.");
            }

            toSave.add(new StudentScore(
                    null,
                    assessmentId,
                    entry.studentId(),
                    validScore,
                    entry.notes() == null ? "" : entry.notes().trim(),
                    recordedBy));
        }

        assessmentDAO.saveAllScores(toSave);
        return toSave;
    }

    public StudentScore updateScore(Long scoreId, BigDecimal newScore, String notes) {
        if (scoreId == null) {
            throw new IllegalArgumentException("ID nilai siswa wajib diisi.");
        }

        StudentScore existing = assessmentDAO.findScoreById(scoreId)
                .orElseThrow(() -> new IllegalArgumentException("Data nilai siswa tidak ditemukan."));

        Assessment assessment = getAuthorizedAssessmentForWrite(existing.getAssessmentId());
        BigDecimal validScore = validateAndNormalizeStudentScore(newScore, assessment.getMaxScore());

        existing.setScore(validScore);
        existing.setNotes(notes == null ? "" : notes.trim());
        existing.setRecordedBy(SessionContext.getInstance().getUserId());

        assessmentDAO.updateScore(existing);
        return existing;
    }

    public Optional<StudentScore> getScoreById(Long scoreId) {
        Optional<StudentScore> found = assessmentDAO.findScoreById(scoreId);
        found.ifPresent(s -> getAuthorizedAssessmentForRead(s.getAssessmentId()));
        return found;
    }

    public List<StudentScore> getScoresByAssessment(Long assessmentId) {
        getAuthorizedAssessmentForRead(assessmentId);
        return assessmentDAO.findScoresByAssessmentId(assessmentId);
    }

    public List<StudentScore> getScoresByStudent(Long studentId) {
        requireLoggedIn();
        if (studentId == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }

        studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));

        List<StudentScore> allStudentScores = assessmentDAO.findScoresByStudentId(studentId);
        SessionContext session = SessionContext.getInstance();
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            return allStudentScores;
        }

        Long teacherId = requireTeacherIdForTeacherRole(session);
        Set<Long> teacherAssignmentIds = getAuthorizedTeachingAssignmentIds(teacherId);
        Set<Long> teacherActiveClassIds = getActiveTeachingClassIds(teacherId);

        Map<Long, Long> assessmentToAssignment = new HashMap<>();
        for (StudentScore score : allStudentScores) {
            assessmentToAssignment.computeIfAbsent(score.getAssessmentId(),
                    aid -> assessmentDAO.findAssessmentById(aid)
                            .map(Assessment::getTeachingAssignmentId)
                            .orElse(-1L));
        }

        List<StudentScore> filtered = allStudentScores.stream()
                .filter(score -> teacherAssignmentIds.contains(
                        assessmentToAssignment.getOrDefault(score.getAssessmentId(), -1L)))
                .toList();

        boolean teachesCurrentClass = academicDAO.findActivePlacementByStudentId(studentId)
                .map(p -> teacherActiveClassIds.contains(p.getClassId()))
                .orElse(false);

        if (!teachesCurrentClass && filtered.isEmpty()) {
            throw new IllegalArgumentException(
                    "Akses ditolak: guru tidak memiliki penugasan mengajar untuk siswa ini.");
        }

        return filtered;
    }

    // ==========================================
    // SUMMARIES
    // ==========================================

    public AssessmentSummary getAssessmentScoreSummary(Long assessmentId) {
        Assessment assessment = getAuthorizedAssessmentForRead(assessmentId);
        List<StudentScore> scores = assessmentDAO.findScoresByAssessmentId(assessmentId);

        if (scores.isEmpty()) {
            return new AssessmentSummary(
                    assessment.getId(),
                    assessment.getTitle(),
                    assessment.getAssessmentType(),
                    assessment.getMaxScore(),
                    0,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        }

        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal highest = scores.get(0).getScore();
        BigDecimal lowest = scores.get(0).getScore();

        for (StudentScore item : scores) {
            BigDecimal val = item.getScore();
            sum = sum.add(val);
            if (val.compareTo(highest) > 0) {
                highest = val;
            }
            if (val.compareTo(lowest) < 0) {
                lowest = val;
            }
        }

        BigDecimal count = BigDecimal.valueOf(scores.size());
        BigDecimal average = sum.divide(count, 2, RoundingMode.HALF_UP);
        BigDecimal averagePct = average.multiply(ONE_HUNDRED)
                .divide(assessment.getMaxScore(), 2, RoundingMode.HALF_UP);

        return new AssessmentSummary(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getAssessmentType(),
                assessment.getMaxScore(),
                scores.size(),
                average,
                highest.setScale(2, RoundingMode.HALF_UP),
                lowest.setScale(2, RoundingMode.HALF_UP),
                averagePct);
    }

    public StudentScoreSummary getStudentScoreSummary(Long studentId) {
        List<StudentScore> visibleScores = getScoresByStudent(studentId);
        if (visibleScores.isEmpty()) {
            BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new StudentScoreSummary(studentId, 0, zero, zero, zero, zero);
        }

        BigDecimal totalEarned = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;

        for (StudentScore score : visibleScores) {
            totalEarned = totalEarned.add(score.getScore());
            BigDecimal maxScore = assessmentDAO.findAssessmentById(score.getAssessmentId())
                    .map(Assessment::getMaxScore)
                    .orElse(BigDecimal.ZERO);
            totalMax = totalMax.add(maxScore);
        }

        BigDecimal count = BigDecimal.valueOf(visibleScores.size());
        BigDecimal average = totalEarned.divide(count, 2, RoundingMode.HALF_UP);
        BigDecimal overallPct = totalMax.compareTo(BigDecimal.ZERO) > 0
                ? totalEarned.multiply(ONE_HUNDRED).divide(totalMax, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new StudentScoreSummary(
                studentId,
                visibleScores.size(),
                totalEarned.setScale(2, RoundingMode.HALF_UP),
                totalMax.setScale(2, RoundingMode.HALF_UP),
                average,
                overallPct);
    }

    // ==========================================
    // AUTHORIZATION & VALIDATION HELPERS
    // ==========================================

    private void requireLoggedIn() {
        if (!SessionContext.getInstance().isLoggedIn()) {
            throw new IllegalArgumentException("Silakan login terlebih dahulu untuk mengakses penilaian.");
        }
    }

    private Long requireTeacherIdForTeacherRole(SessionContext session) {
        if (!session.hasRole(RoleMapper.ROLE_TEACHER)) {
            throw new IllegalArgumentException("Akses ditolak: peran tidak diizinkan.");
        }
        Long teacherId = session.getTeacherId();
        if (teacherId == null) {
            throw new IllegalArgumentException(
                    "Akses ditolak: akun guru belum terhubung dengan profil guru.");
        }
        return teacherId;
    }

    private TeachingAssignment requireTeachingAssignmentWriteAuthorization(Long teachingAssignmentId) {
        TeachingAssignment ta = requireTeachingAssignmentReadAuthorization(teachingAssignmentId);
        if (!ta.isActive()) {
            throw new IllegalArgumentException("Penugasan mengajar sudah tidak aktif.");
        }
        return ta;
    }

    private TeachingAssignment requireTeachingAssignmentReadAuthorization(Long teachingAssignmentId) {
        requireLoggedIn();
        if (teachingAssignmentId == null) {
            throw new IllegalArgumentException("ID penugasan mengajar wajib diisi.");
        }

        TeachingAssignment ta = academicDAO.findTeachingAssignmentById(teachingAssignmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data penugasan mengajar tidak ditemukan."));

        SessionContext session = SessionContext.getInstance();
        if (session.hasRole(RoleMapper.ROLE_ADMIN)) {
            return ta;
        }

        Long teacherId = requireTeacherIdForTeacherRole(session);
        if (!teacherId.equals(ta.getTeacherId())) {
            throw new IllegalArgumentException(
                    "Akses ditolak: guru hanya dapat mengakses penilaian untuk penugasan mengajar miliknya.");
        }
        return ta;
    }

    private Assessment getAuthorizedAssessmentForWrite(Long assessmentId) {
        if (assessmentId == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi.");
        }
        Assessment assessment = assessmentDAO.findAssessmentById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data penilaian tidak ditemukan."));
        requireTeachingAssignmentWriteAuthorization(assessment.getTeachingAssignmentId());
        return assessment;
    }

    private Assessment getAuthorizedAssessmentForRead(Long assessmentId) {
        if (assessmentId == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi.");
        }
        Assessment assessment = assessmentDAO.findAssessmentById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data penilaian tidak ditemukan."));
        requireTeachingAssignmentReadAuthorization(assessment.getTeachingAssignmentId());
        return assessment;
    }

    private Set<Long> getAuthorizedTeachingAssignmentIds(Long teacherId) {
        Set<Long> ids = new HashSet<>();
        academicDAO.findTeachingAssignmentsByTeacherId(teacherId)
                .forEach(ta -> ids.add(ta.getId()));
        return ids;
    }

    private Set<Long> getActiveTeachingClassIds(Long teacherId) {
        Set<Long> classIds = new HashSet<>();
        academicDAO.findTeachingAssignmentsByTeacherId(teacherId).stream()
                .filter(TeachingAssignment::isActive)
                .forEach(ta -> classIds.add(ta.getClassId()));
        return classIds;
    }

    private void validateStudentInClassOnDate(Long studentId, Long classId, LocalDate assessmentDate) {
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
                        && p.getEntryDate().equals(assessmentDate));

        boolean belongsToClassOnDate = history.stream()
                .filter(p -> p.getClassId().equals(classId))
                .anyMatch(p -> !assessmentDate.isBefore(p.getEntryDate())
                        && (p.getExitDate() == null || !assessmentDate.isAfter(p.getExitDate())));

        if (!belongsToClassOnDate || movedToAnotherClassOnDate) {
            throw new IllegalArgumentException(
                    "Siswa ID " + studentId + " tidak terdaftar pada kelas ID " + classId
                            + " pada tanggal penilaian " + assessmentDate + ".");
        }
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Judul penilaian wajib diisi.");
        }
        String trimmed = title.trim();
        if (trimmed.length() > 150) {
            throw new IllegalArgumentException("Judul penilaian maksimal 150 karakter.");
        }
        return trimmed;
    }

    private static String normalizeAndValidateType(String assessmentType) {
        if (assessmentType == null || assessmentType.isBlank()) {
            throw new IllegalArgumentException("Jenis penilaian wajib diisi.");
        }
        String normalized = assessmentType.trim().toUpperCase();
        if (!VALID_TYPES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Jenis penilaian tidak valid. Pilihan: KUIS, TUGAS, UTS, UAS, PRAKTIK, PROYEK, LAINNYA.");
        }
        return normalized;
    }

    private void validateAssessmentDate(LocalDate assessmentDate, TeachingAssignment ta) {
        if (assessmentDate == null) {
            throw new IllegalArgumentException("Tanggal penilaian wajib diisi.");
        }
        Optional<Semester> semesterOpt = academicDAO.findSemesterById(ta.getSemesterId());
        if (semesterOpt.isPresent()) {
            Semester semester = semesterOpt.get();
            if (assessmentDate.isBefore(semester.getStartDate())
                    || assessmentDate.isAfter(semester.getEndDate())) {
                throw new IllegalArgumentException(
                        "Tanggal penilaian harus berada dalam rentang semester ("
                                + semester.getStartDate() + " s.d. " + semester.getEndDate() + ").");
            }
        }
    }

    private static BigDecimal validateAndNormalizeMaxScore(BigDecimal maxScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nilai maksimal harus lebih besar dari 0.");
        }
        if (maxScore.compareTo(MAX_DECIMAL_LIMIT) > 0) {
            throw new IllegalArgumentException("Nilai maksimal melebihi batas (99999.99).");
        }
        return maxScore.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validateAndNormalizeStudentScore(BigDecimal score, BigDecimal maxScore) {
        if (score == null) {
            throw new IllegalArgumentException("Nilai siswa wajib diisi.");
        }
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(maxScore) > 0) {
            throw new IllegalArgumentException(
                    "Nilai siswa harus berada di antara 0 dan nilai maksimal (" + maxScore + ").");
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }
}

