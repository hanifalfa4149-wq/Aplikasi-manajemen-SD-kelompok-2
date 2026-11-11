package id.ac.sekolah.manajemenkelas.academic.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;

public class InMemoryAcademicDAO implements AcademicDAO {

    private final List<AcademicYear> academicYears = new ArrayList<>();
    private final List<Semester> semesters = new ArrayList<>();
    private final List<SchoolClass> classes = new ArrayList<>();
    private final List<Subject> subjects = new ArrayList<>();
    private final List<HomeroomAssignment> homeroomAssignments = new ArrayList<>();
    private final List<TeachingAssignment> teachingAssignments = new ArrayList<>();
    private final List<StudentPlacement> studentPlacements = new ArrayList<>();

    private final AtomicLong yearIdGenerator = new AtomicLong(1);
    private final AtomicLong semesterIdGenerator = new AtomicLong(1);
    private final AtomicLong classIdGenerator = new AtomicLong(1);
    private final AtomicLong subjectIdGenerator = new AtomicLong(1);
    private final AtomicLong homeroomIdGenerator = new AtomicLong(1);
    private final AtomicLong teachingIdGenerator = new AtomicLong(1);
    private final AtomicLong placementIdGenerator = new AtomicLong(1);

    // Academic Year

    @Override
    public List<AcademicYear> findAllAcademicYears() {
        return new ArrayList<>(academicYears);
    }

    @Override
    public Optional<AcademicYear> findAcademicYearById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return academicYears.stream()
                .filter(year -> id.equals(year.getId()))
                .findFirst();
    }

    @Override
    public Optional<AcademicYear> findAcademicYearByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return academicYears.stream()
                .filter(year -> year.getName() != null && year.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void saveAcademicYear(AcademicYear academicYear) {
        if (academicYear.getId() == null) {
            academicYear.setId(yearIdGenerator.getAndIncrement());
        }
        if (academicYear.isActive()) {
            academicYears.forEach(year -> year.setActive(false));
        }
        academicYears.add(academicYear);
    }

    @Override
    public void updateAcademicYear(AcademicYear academicYear) {
        findAcademicYearById(academicYear.getId())
                .ifPresent(existing -> {
                    if (academicYear.isActive()) {
                        academicYears.stream()
                                .filter(year -> !academicYear.getId().equals(year.getId()))
                                .forEach(year -> year.setActive(false));
                    }
                    int index = academicYears.indexOf(existing);
                    academicYears.set(index, academicYear);
                });
    }

    // Semester

    @Override
    public List<Semester> findAllSemesters() {
        return new ArrayList<>(semesters);
    }

    @Override
    public List<Semester> findSemestersByAcademicYearId(Long academicYearId) {
        if (academicYearId == null) {
            return List.of();
        }
        return semesters.stream()
                .filter(s -> academicYearId.equals(s.getAcademicYearId()))
                .toList();
    }

    @Override
    public Optional<Semester> findSemesterById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return semesters.stream()
                .filter(s -> id.equals(s.getId()))
                .findFirst();
    }

    @Override
    public void saveSemester(Semester semester) {
        if (semester.getId() == null) {
            semester.setId(semesterIdGenerator.getAndIncrement());
        }
        semesters.add(semester);
    }

    @Override
    public void updateSemester(Semester semester) {
        findSemesterById(semester.getId())
                .ifPresent(existing -> {
                    int index = semesters.indexOf(existing);
                    semesters.set(index, semester);
                });
    }

    // School Class

    @Override
    public List<SchoolClass> findAllClasses() {
        return new ArrayList<>(classes);
    }

    @Override
    public Optional<SchoolClass> findClassById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return classes.stream()
                .filter(schoolClass -> id.equals(schoolClass.getId()))
                .findFirst();
    }

    @Override
    public void saveClass(SchoolClass schoolClass) {
        if (schoolClass.getId() == null) {
            schoolClass.setId(classIdGenerator.getAndIncrement());
        }
        classes.add(schoolClass);
    }

    @Override
    public void updateClass(SchoolClass schoolClass) {
        findClassById(schoolClass.getId())
                .ifPresent(existing -> {
                    int index = classes.indexOf(existing);
                    classes.set(index, schoolClass);
                });
    }

    // Subject

    @Override
    public List<Subject> findAllSubjects() {
        return new ArrayList<>(subjects);
    }

    @Override
    public Optional<Subject> findSubjectById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return subjects.stream()
                .filter(subject -> id.equals(subject.getId()))
                .findFirst();
    }

    @Override
    public Optional<Subject> findSubjectByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return subjects.stream()
                .filter(subject -> subject.getCode() != null && subject.getCode().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public void saveSubject(Subject subject) {
        if (subject.getId() == null) {
            subject.setId(subjectIdGenerator.getAndIncrement());
        }
        subjects.add(subject);
    }

    @Override
    public void updateSubject(Subject subject) {
        findSubjectById(subject.getId())
                .ifPresent(existing -> {
                    int index = subjects.indexOf(existing);
                    subjects.set(index, subject);
                });
    }

    // Homeroom Assignment

    @Override
    public List<HomeroomAssignment> findAllHomeroomAssignments() {
        return new ArrayList<>(homeroomAssignments);
    }

    @Override
    public Optional<HomeroomAssignment> findHomeroomAssignmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return homeroomAssignments.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst();
    }

    @Override
    public Optional<HomeroomAssignment> findActiveHomeroomByClassId(Long classId) {
        if (classId == null) {
            return Optional.empty();
        }
        return homeroomAssignments.stream()
                .filter(a -> classId.equals(a.getClassId()) && a.isActive())
                .findFirst();
    }

    @Override
    public void saveHomeroomAssignment(HomeroomAssignment assignment) {
        if (assignment.getId() == null) {
            assignment.setId(homeroomIdGenerator.getAndIncrement());
        }
        homeroomAssignments.add(assignment);
    }

    @Override
    public void updateHomeroomAssignment(HomeroomAssignment assignment) {
        findHomeroomAssignmentById(assignment.getId())
                .ifPresent(existing -> {
                    int index = homeroomAssignments.indexOf(existing);
                    homeroomAssignments.set(index, assignment);
                });
    }

    // Teaching Assignment

    @Override
    public List<TeachingAssignment> findAllTeachingAssignments() {
        return new ArrayList<>(teachingAssignments);
    }

    @Override
    public Optional<TeachingAssignment> findTeachingAssignmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return teachingAssignments.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst();
    }

    @Override
    public List<TeachingAssignment> findTeachingAssignmentsByTeacherId(Long teacherId) {
        if (teacherId == null) {
            return List.of();
        }
        return teachingAssignments.stream()
                .filter(a -> teacherId.equals(a.getTeacherId()))
                .toList();
    }

    @Override
    public List<TeachingAssignment> findTeachingAssignmentsByClassId(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return teachingAssignments.stream()
                .filter(a -> classId.equals(a.getClassId()))
                .toList();
    }

    @Override
    public List<TeachingAssignment> findTeachingAssignmentsBySemesterId(Long semesterId) {
        if (semesterId == null) {
            return List.of();
        }
        return teachingAssignments.stream()
                .filter(a -> semesterId.equals(a.getSemesterId()))
                .toList();
    }

    @Override
    public void saveTeachingAssignment(TeachingAssignment assignment) {
        if (assignment.getId() == null) {
            assignment.setId(teachingIdGenerator.getAndIncrement());
        }
        teachingAssignments.add(assignment);
    }

    @Override
    public void updateTeachingAssignment(TeachingAssignment assignment) {
        findTeachingAssignmentById(assignment.getId())
                .ifPresent(existing -> {
                    int index = teachingAssignments.indexOf(existing);
                    teachingAssignments.set(index, assignment);
                });
    }

    // Student Placement

    @Override
    public List<StudentPlacement> findAllStudentPlacements() {
        return new ArrayList<>(studentPlacements);
    }

    @Override
    public Optional<StudentPlacement> findStudentPlacementById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return studentPlacements.stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst();
    }

    @Override
    public Optional<StudentPlacement> findActivePlacementByStudentId(Long studentId) {
        if (studentId == null) {
            return Optional.empty();
        }
        return studentPlacements.stream()
                .filter(p -> studentId.equals(p.getStudentId()) && p.isActive())
                .findFirst();
    }

    @Override
    public List<StudentPlacement> findPlacementHistoryByStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return studentPlacements.stream()
                .filter(p -> studentId.equals(p.getStudentId()))
                .toList();
    }

    @Override
    public List<StudentPlacement> findActivePlacementsByClassId(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return studentPlacements.stream()
                .filter(p -> classId.equals(p.getClassId()) && p.isActive())
                .toList();
    }

    @Override
    public void saveStudentPlacement(StudentPlacement placement) {
        if (placement.getId() == null) {
            placement.setId(placementIdGenerator.getAndIncrement());
        }
        studentPlacements.add(placement);
    }

    @Override
    public void updateStudentPlacement(StudentPlacement placement) {
        findStudentPlacementById(placement.getId())
                .ifPresent(existing -> {
                    int index = studentPlacements.indexOf(existing);
                    studentPlacements.set(index, placement);
                });
    }

    @Override
    public void transitionStudentPlacement(StudentPlacement previousPlacement, StudentPlacement newPlacement) {
        updateStudentPlacement(previousPlacement);
        saveStudentPlacement(newPlacement);
    }
}
