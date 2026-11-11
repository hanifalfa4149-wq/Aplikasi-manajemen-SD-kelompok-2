package id.ac.sekolah.manajemenkelas.report.dao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import id.ac.sekolah.manajemenkelas.assessment.dao.AssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.attendance.dao.AttendanceDAO;
import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;
import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class InMemoryReportDAO implements ReportDAO {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private final StudentDAO studentDAO;
    private final TeacherDAO teacherDAO;
    private final AcademicDAO academicDAO;
    private final AttendanceDAO attendanceDAO;
    private final AssessmentDAO assessmentDAO;

    public InMemoryReportDAO(
            StudentDAO studentDAO,
            TeacherDAO teacherDAO,
            AcademicDAO academicDAO,
            AttendanceDAO attendanceDAO,
            AssessmentDAO assessmentDAO) {
        this.studentDAO = studentDAO;
        this.teacherDAO = teacherDAO;
        this.academicDAO = academicDAO;
        this.attendanceDAO = attendanceDAO;
        this.assessmentDAO = assessmentDAO;
    }

    @Override
    public DashboardSummary getDashboardSummary(Long teacherIdScope) {
        Set<Long> teacherClasses = getTeacherAssignedClassIds(teacherIdScope);

        List<Student> visibleStudents = getScopedStudents(teacherIdScope, teacherClasses);
        int activeStudents = (int) visibleStudents.stream().filter(Student::isActive).count();
        int inactiveStudents = (int) visibleStudents.stream().filter(s -> !s.isActive()).count();

        int activeTeachers;
        int inactiveTeachers;
        if (teacherIdScope == null) {
            List<Teacher> allTeachers = teacherDAO.findAll();
            activeTeachers = (int) allTeachers.stream().filter(Teacher::isActive).count();
            inactiveTeachers = (int) allTeachers.stream().filter(t -> !t.isActive()).count();
        } else {
            Optional<Teacher> self = teacherDAO.findById(teacherIdScope);
            activeTeachers = self.filter(Teacher::isActive).isPresent() ? 1 : 0;
            inactiveTeachers = self.filter(t -> !t.isActive()).isPresent() ? 1 : 0;
        }

        int activeClasses = (int) academicDAO.findAllClasses().stream()
                .filter(SchoolClass::isActive)
                .filter(c -> teacherIdScope == null || teacherClasses.contains(c.getId()))
                .count();

        int activeSubjects;
        if (teacherIdScope == null) {
            activeSubjects = (int) academicDAO.findAllSubjects().stream().filter(Subject::isActive).count();
        } else {
            Set<Long> teacherSubjectIds = new HashSet<>();
            academicDAO.findTeachingAssignmentsByTeacherId(teacherIdScope).stream()
                    .filter(TeachingAssignment::isActive)
                    .forEach(ta -> teacherSubjectIds.add(ta.getSubjectId()));
            activeSubjects = (int) academicDAO.findAllSubjects().stream()
                    .filter(s -> s.isActive() && teacherSubjectIds.contains(s.getId()))
                    .count();
        }

        String activeYearName = academicDAO.findAllAcademicYears().stream()
                .filter(AcademicYear::isActive)
                .map(AcademicYear::getName)
                .findFirst()
                .orElse("-");

        int activeSemesters = (int) academicDAO.findAllSemesters().stream()
                .filter(Semester::isActive)
                .count();

        int homeroomCount = (int) academicDAO.findAllHomeroomAssignments().stream()
                .filter(HomeroomAssignment::isActive)
                .filter(hr -> teacherIdScope == null || teacherIdScope.equals(hr.getTeacherId()))
                .count();

        int teachingCount = (int) academicDAO.findAllTeachingAssignments().stream()
                .filter(TeachingAssignment::isActive)
                .filter(ta -> teacherIdScope == null || teacherIdScope.equals(ta.getTeacherId()))
                .count();

        Set<Long> teacherTaIds = getTeacherAssignmentIds(teacherIdScope);
        int assessmentsCount = (int) assessmentDAO.findAllAssessments().stream()
                .filter(a -> teacherIdScope == null || teacherTaIds.contains(a.getTeachingAssignmentId()))
                .count();

        List<Attendance> scopedAttendances = getAllScopedAttendances(teacherIdScope, teacherClasses);
        int totalAtt = scopedAttendances.size();
        int hadirAtt = (int) scopedAttendances.stream()
                .filter(a -> Attendance.STATUS_HADIR.equals(a.getStatus()))
                .count();
        BigDecimal attPct = calculatePercentage(hadirAtt, totalAtt);

        String scopeLabel = teacherIdScope == null ? "Sekolah (Admin)" : "Guru (Kelas Diampu)";
        return new DashboardSummary(
                scopeLabel,
                activeStudents,
                inactiveStudents,
                activeTeachers,
                inactiveTeachers,
                activeClasses,
                activeSubjects,
                activeYearName,
                activeSemesters,
                homeroomCount,
                teachingCount,
                assessmentsCount,
                totalAtt,
                attPct);
    }

    @Override
    public List<ClassStudentCountReport> findActiveStudentCountsByClass(Long teacherIdScope) {
        Set<Long> teacherClasses = getTeacherAssignedClassIds(teacherIdScope);
        Map<Long, AcademicYear> yearMap = new HashMap<>();
        academicDAO.findAllAcademicYears().forEach(y -> yearMap.put(y.getId(), y));

        Map<Long, Student> studentMap = new HashMap<>();
        studentDAO.findAll().forEach(s -> studentMap.put(s.getId(), s));

        return academicDAO.findAllClasses().stream()
                .filter(SchoolClass::isActive)
                .filter(c -> teacherIdScope == null || teacherClasses.contains(c.getId()))
                .sorted(Comparator.comparingInt(SchoolClass::getGradeLevel)
                        .thenComparing(SchoolClass::getName)
                        .thenComparing(SchoolClass::getId))
                .map(c -> {
                    int activeCount = (int) academicDAO.findActivePlacementsByClassId(c.getId()).stream()
                            .filter(p -> {
                                Student st = studentMap.get(p.getStudentId());
                                return st != null && st.isActive();
                            })
                            .count();
                    String yearName = Optional.ofNullable(yearMap.get(c.getAcademicYearId()))
                            .map(AcademicYear::getName)
                            .orElse("-");
                    return new ClassStudentCountReport(
                            c.getId(),
                            c.getName(),
                            c.getGradeLevel(),
                            yearName,
                            c.getCapacity(),
                            activeCount);
                })
                .toList();
    }

    @Override
    public List<GenderStudentCountReport> findActiveStudentCountsByGender(Long teacherIdScope) {
        Set<Long> teacherClasses = getTeacherAssignedClassIds(teacherIdScope);
        List<Student> activeStudents = getScopedStudents(teacherIdScope, teacherClasses).stream()
                .filter(Student::isActive)
                .toList();

        int maleCount = (int) activeStudents.stream()
                .filter(s -> "L".equalsIgnoreCase(s.getGender()))
                .count();
        int femaleCount = (int) activeStudents.stream()
                .filter(s -> "P".equalsIgnoreCase(s.getGender()))
                .count();

        return List.of(
                new GenderStudentCountReport("L", "Laki-laki", maleCount),
                new GenderStudentCountReport("P", "Perempuan", femaleCount));
    }

    @Override
    public AttendanceRangeReport findAttendanceSummaryByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Long teacherIdScope) {

        Set<Long> teacherClasses = getTeacherAssignedClassIds(teacherIdScope);
        List<Attendance> filtered = getAllScopedAttendances(teacherIdScope, teacherClasses).stream()
                .filter(a -> !a.getAttendanceDate().isBefore(startDate)
                        && !a.getAttendanceDate().isAfter(endDate))
                .toList();

        int hadir = countStatus(filtered, Attendance.STATUS_HADIR);
        int sakit = countStatus(filtered, Attendance.STATUS_SAKIT);
        int izin = countStatus(filtered, Attendance.STATUS_IZIN);
        int alpa = countStatus(filtered, Attendance.STATUS_ALPA);

        return new AttendanceRangeReport(
                startDate,
                endDate,
                filtered.size(),
                hadir,
                sakit,
                izin,
                alpa,
                calculatePercentage(hadir, filtered.size()));
    }

    @Override
    public List<ClassAttendanceReport> findAttendanceSummariesByClass(
            LocalDate startDate,
            LocalDate endDate,
            Long teacherIdScope) {

        Set<Long> teacherClasses = getTeacherAssignedClassIds(teacherIdScope);
        return academicDAO.findAllClasses().stream()
                .filter(SchoolClass::isActive)
                .filter(c -> teacherIdScope == null || teacherClasses.contains(c.getId()))
                .sorted(Comparator.comparingInt(SchoolClass::getGradeLevel)
                        .thenComparing(SchoolClass::getName)
                        .thenComparing(SchoolClass::getId))
                .map(c -> {
                    List<Attendance> classAtt = attendanceDAO.findByClassId(c.getId()).stream()
                            .filter(a -> startDate == null || !a.getAttendanceDate().isBefore(startDate))
                            .filter(a -> endDate == null || !a.getAttendanceDate().isAfter(endDate))
                            .toList();
                    int hadir = countStatus(classAtt, Attendance.STATUS_HADIR);
                    int sakit = countStatus(classAtt, Attendance.STATUS_SAKIT);
                    int izin = countStatus(classAtt, Attendance.STATUS_IZIN);
                    int alpa = countStatus(classAtt, Attendance.STATUS_ALPA);
                    return new ClassAttendanceReport(
                            c.getId(),
                            c.getName(),
                            c.getGradeLevel(),
                            classAtt.size(),
                            hadir,
                            sakit,
                            izin,
                            alpa,
                            calculatePercentage(hadir, classAtt.size()));
                })
                .toList();
    }

    @Override
    public List<AssessmentReport> findAssessmentSummaries(Long teacherIdScope) {
        Set<Long> teacherTaIds = getTeacherAssignmentIds(teacherIdScope);
        Map<Long, TeachingAssignment> taMap = new HashMap<>();
        academicDAO.findAllTeachingAssignments().forEach(ta -> taMap.put(ta.getId(), ta));

        Map<Long, SchoolClass> classMap = new HashMap<>();
        academicDAO.findAllClasses().forEach(c -> classMap.put(c.getId(), c));

        Map<Long, Subject> subjectMap = new HashMap<>();
        academicDAO.findAllSubjects().forEach(s -> subjectMap.put(s.getId(), s));

        return assessmentDAO.findAllAssessments().stream()
                .filter(a -> teacherIdScope == null || teacherTaIds.contains(a.getTeachingAssignmentId()))
                .sorted(Comparator.comparing(Assessment::getAssessmentDate).reversed()
                        .thenComparing(Assessment::getId, Comparator.reverseOrder()))
                .map(a -> {
                    TeachingAssignment ta = taMap.get(a.getTeachingAssignmentId());
                    String className = ta != null && classMap.containsKey(ta.getClassId())
                            ? classMap.get(ta.getClassId()).getName()
                            : "-";
                    String subjectName = ta != null && subjectMap.containsKey(ta.getSubjectId())
                            ? subjectMap.get(ta.getSubjectId()).getName()
                            : "-";

                    List<StudentScore> scores = assessmentDAO.findScoresByAssessmentId(a.getId());
                    if (scores.isEmpty()) {
                        return new AssessmentReport(
                                a.getId(), a.getTeachingAssignmentId(), className, subjectName,
                                a.getTitle(), a.getAssessmentType(), a.getAssessmentDate(), a.getMaxScore(),
                                0, null, null, null, null);
                    }

                    BigDecimal sum = BigDecimal.ZERO;
                    BigDecimal highest = scores.get(0).getScore();
                    BigDecimal lowest = scores.get(0).getScore();
                    for (StudentScore sc : scores) {
                        BigDecimal val = sc.getScore();
                        sum = sum.add(val);
                        if (val.compareTo(highest) > 0) {
                            highest = val;
                        }
                        if (val.compareTo(lowest) < 0) {
                            lowest = val;
                        }
                    }

                    BigDecimal avg = sum.divide(BigDecimal.valueOf(scores.size()), 2, RoundingMode.HALF_UP);
                    BigDecimal avgPct = a.getMaxScore().compareTo(BigDecimal.ZERO) > 0
                            ? avg.multiply(ONE_HUNDRED).divide(a.getMaxScore(), 2, RoundingMode.HALF_UP)
                            : null;

                    return new AssessmentReport(
                            a.getId(), a.getTeachingAssignmentId(), className, subjectName,
                            a.getTitle(), a.getAssessmentType(), a.getAssessmentDate(), a.getMaxScore(),
                            scores.size(), avg,
                            highest.setScale(2, RoundingMode.HALF_UP),
                            lowest.setScale(2, RoundingMode.HALF_UP),
                            avgPct);
                })
                .toList();
    }

    @Override
    public List<StudentScoreReport> findStudentScoreSummaries(Long teacherIdScope) {
        Set<Long> teacherTaIds = getTeacherAssignmentIds(teacherIdScope);
        Set<Long> teacherClasses = getTeacherAssignedClassIds(teacherIdScope);

        Map<Long, SchoolClass> classMap = new HashMap<>();
        academicDAO.findAllClasses().forEach(c -> classMap.put(c.getId(), c));

        Map<Long, Assessment> assessmentMap = new HashMap<>();
        assessmentDAO.findAllAssessments().forEach(a -> assessmentMap.put(a.getId(), a));

        return studentDAO.findAll().stream()
                .filter(Student::isActive)
                .filter(s -> {
                    if (teacherIdScope == null) {
                        return true;
                    }
                    boolean placedInTeacherClass = academicDAO.findActivePlacementByStudentId(s.getId())
                            .map(p -> teacherClasses.contains(p.getClassId()))
                            .orElse(false);
                    boolean hasTeacherScore = assessmentDAO.findScoresByStudentId(s.getId()).stream()
                            .anyMatch(sc -> {
                                Assessment a = assessmentMap.get(sc.getAssessmentId());
                                return a != null && teacherTaIds.contains(a.getTeachingAssignmentId());
                            });
                    return placedInTeacherClass || hasTeacherScore;
                })
                .sorted(Comparator.comparing(Student::getFullName)
                        .thenComparing(Student::getId))
                .map(s -> buildStudentScoreReport(s, teacherIdScope, teacherTaIds, classMap, assessmentMap))
                .toList();
    }

    @Override
    public Optional<StudentScoreReport> findStudentScoreSummaryByStudentId(Long studentId, Long teacherIdScope) {
        if (studentId == null) {
            return Optional.empty();
        }
        return findStudentScoreSummaries(teacherIdScope).stream()
                .filter(r -> r.getStudentId().equals(studentId))
                .findFirst();
    }

    @Override
    public List<SubjectScoreReport> findAverageScoresBySubject(Long teacherIdScope) {
        List<TeachingAssignment> allTas = academicDAO.findAllTeachingAssignments();
        Set<Long> teacherTaIds = getTeacherAssignmentIds(teacherIdScope);

        Map<Long, Long> taToSubject = new HashMap<>();
        Set<Long> teacherSubjectIds = new HashSet<>();
        for (TeachingAssignment ta : allTas) {
            taToSubject.put(ta.getId(), ta.getSubjectId());
            if (teacherIdScope != null && teacherIdScope.equals(ta.getTeacherId())) {
                teacherSubjectIds.add(ta.getSubjectId());
            }
        }

        List<Assessment> visibleAssessments = assessmentDAO.findAllAssessments().stream()
                .filter(a -> teacherIdScope == null || teacherTaIds.contains(a.getTeachingAssignmentId()))
                .toList();

        return academicDAO.findAllSubjects().stream()
                .filter(Subject::isActive)
                .filter(sub -> teacherIdScope == null || teacherSubjectIds.contains(sub.getId()))
                .sorted(Comparator.comparing(Subject::getName)
                        .thenComparing(Subject::getId))
                .map(sub -> {
                    List<Assessment> subAssessments = visibleAssessments.stream()
                            .filter(a -> sub.getId().equals(taToSubject.get(a.getTeachingAssignmentId())))
                            .toList();

                    int entryCount = 0;
                    BigDecimal sumScore = BigDecimal.ZERO;
                    BigDecimal sumMax = BigDecimal.ZERO;

                    for (Assessment a : subAssessments) {
                        List<StudentScore> scores = assessmentDAO.findScoresByAssessmentId(a.getId());
                        for (StudentScore sc : scores) {
                            entryCount++;
                            sumScore = sumScore.add(sc.getScore());
                            sumMax = sumMax.add(a.getMaxScore());
                        }
                    }

                    if (entryCount == 0) {
                        return new SubjectScoreReport(
                                sub.getId(), sub.getCode(), sub.getName(),
                                subAssessments.size(), 0, null, null);
                    }

                    BigDecimal avg = sumScore.divide(BigDecimal.valueOf(entryCount), 2, RoundingMode.HALF_UP);
                    BigDecimal avgPct = sumMax.compareTo(BigDecimal.ZERO) > 0
                            ? sumScore.multiply(ONE_HUNDRED).divide(sumMax, 2, RoundingMode.HALF_UP)
                            : null;

                    return new SubjectScoreReport(
                            sub.getId(), sub.getCode(), sub.getName(),
                            subAssessments.size(), entryCount, avg, avgPct);
                })
                .toList();
    }

    private StudentScoreReport buildStudentScoreReport(
            Student student,
            Long teacherIdScope,
            Set<Long> teacherTaIds,
            Map<Long, SchoolClass> classMap,
            Map<Long, Assessment> assessmentMap) {

        String currentClassName = academicDAO.findActivePlacementByStudentId(student.getId())
                .map(p -> classMap.get(p.getClassId()))
                .map(SchoolClass::getName)
                .orElse("-");

        List<StudentScore> scopedScores = assessmentDAO.findScoresByStudentId(student.getId()).stream()
                .filter(sc -> {
                    if (teacherIdScope == null) {
                        return true;
                    }
                    Assessment a = assessmentMap.get(sc.getAssessmentId());
                    return a != null && teacherTaIds.contains(a.getTeachingAssignmentId());
                })
                .toList();

        if (scopedScores.isEmpty()) {
            BigDecimal zero = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new StudentScoreReport(
                    student.getId(), student.getNis(), student.getFullName(),
                    currentClassName, 0, zero, zero, null, null);
        }

        BigDecimal totalEarned = BigDecimal.ZERO;
        BigDecimal totalMax = BigDecimal.ZERO;
        for (StudentScore sc : scopedScores) {
            totalEarned = totalEarned.add(sc.getScore());
            Assessment a = assessmentMap.get(sc.getAssessmentId());
            if (a != null) {
                totalMax = totalMax.add(a.getMaxScore());
            }
        }

        BigDecimal avg = totalEarned.divide(BigDecimal.valueOf(scopedScores.size()), 2, RoundingMode.HALF_UP);
        BigDecimal pct = totalMax.compareTo(BigDecimal.ZERO) > 0
                ? totalEarned.multiply(ONE_HUNDRED).divide(totalMax, 2, RoundingMode.HALF_UP)
                : null;

        return new StudentScoreReport(
                student.getId(), student.getNis(), student.getFullName(),
                currentClassName, scopedScores.size(),
                totalEarned.setScale(2, RoundingMode.HALF_UP),
                totalMax.setScale(2, RoundingMode.HALF_UP),
                avg, pct);
    }

    private Set<Long> getTeacherAssignedClassIds(Long teacherIdScope) {
        if (teacherIdScope == null) {
            return Set.of();
        }
        Set<Long> classIds = new HashSet<>();
        academicDAO.findAllHomeroomAssignments().stream()
                .filter(hr -> hr.isActive() && teacherIdScope.equals(hr.getTeacherId()))
                .forEach(hr -> classIds.add(hr.getClassId()));
        academicDAO.findTeachingAssignmentsByTeacherId(teacherIdScope).stream()
                .filter(TeachingAssignment::isActive)
                .forEach(ta -> classIds.add(ta.getClassId()));
        return classIds;
    }

    private Set<Long> getTeacherAssignmentIds(Long teacherIdScope) {
        if (teacherIdScope == null) {
            return Set.of();
        }
        Set<Long> taIds = new HashSet<>();
        academicDAO.findTeachingAssignmentsByTeacherId(teacherIdScope)
                .forEach(ta -> taIds.add(ta.getId()));
        return taIds;
    }

    private List<Student> getScopedStudents(Long teacherIdScope, Set<Long> teacherClasses) {
        if (teacherIdScope == null) {
            return studentDAO.findAll();
        }
        Set<Long> placedStudentIds = new HashSet<>();
        for (Long classId : teacherClasses) {
            academicDAO.findAllStudentPlacements().stream()
                    .filter(p -> classId.equals(p.getClassId())
                            && StudentPlacement.STATUS_AKTIF.equals(p.getStatus())
                            && p.getExitDate() == null)
                    .forEach(p -> placedStudentIds.add(p.getStudentId()));
        }
        return studentDAO.findAll().stream()
                .filter(s -> placedStudentIds.contains(s.getId()))
                .toList();
    }

    private List<Attendance> getAllScopedAttendances(Long teacherIdScope, Set<Long> teacherClasses) {
        List<Attendance> all = new ArrayList<>();
        for (SchoolClass sc : academicDAO.findAllClasses()) {
            if (teacherIdScope == null || teacherClasses.contains(sc.getId())) {
                all.addAll(attendanceDAO.findByClassId(sc.getId()));
            }
        }
        return all;
    }

    private int countStatus(List<Attendance> rows, String status) {
        return (int) rows.stream().filter(a -> status.equals(a.getStatus())).count();
    }

    private BigDecimal calculatePercentage(int numerator, int denominator) {
        if (denominator <= 0) {
            return null;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}

