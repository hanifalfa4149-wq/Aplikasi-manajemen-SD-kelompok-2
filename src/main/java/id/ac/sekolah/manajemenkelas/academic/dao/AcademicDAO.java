package id.ac.sekolah.manajemenkelas.academic.dao;

import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;

public interface AcademicDAO {

    // Academic Year
    List<AcademicYear> findAllAcademicYears();

    Optional<AcademicYear> findAcademicYearById(Long id);

    Optional<AcademicYear> findAcademicYearByName(String name);

    void saveAcademicYear(AcademicYear academicYear);

    void updateAcademicYear(AcademicYear academicYear);

    // Semester
    List<Semester> findAllSemesters();

    List<Semester> findSemestersByAcademicYearId(Long academicYearId);

    Optional<Semester> findSemesterById(Long id);

    void saveSemester(Semester semester);

    void updateSemester(Semester semester);

    // School Class
    List<SchoolClass> findAllClasses();

    Optional<SchoolClass> findClassById(Long id);

    void saveClass(SchoolClass schoolClass);

    void updateClass(SchoolClass schoolClass);

    // Subject
    List<Subject> findAllSubjects();

    Optional<Subject> findSubjectById(Long id);

    Optional<Subject> findSubjectByCode(String code);

    void saveSubject(Subject subject);

    void updateSubject(Subject subject);

    // Homeroom Assignment
    List<HomeroomAssignment> findAllHomeroomAssignments();

    Optional<HomeroomAssignment> findHomeroomAssignmentById(Long id);

    Optional<HomeroomAssignment> findActiveHomeroomByClassId(Long classId);

    void saveHomeroomAssignment(HomeroomAssignment assignment);

    void updateHomeroomAssignment(HomeroomAssignment assignment);

    // Teaching Assignment
    List<TeachingAssignment> findAllTeachingAssignments();

    Optional<TeachingAssignment> findTeachingAssignmentById(Long id);

    List<TeachingAssignment> findTeachingAssignmentsByTeacherId(Long teacherId);

    List<TeachingAssignment> findTeachingAssignmentsByClassId(Long classId);

    List<TeachingAssignment> findTeachingAssignmentsBySemesterId(Long semesterId);

    void saveTeachingAssignment(TeachingAssignment assignment);

    void updateTeachingAssignment(TeachingAssignment assignment);

    // Student Placement
    List<StudentPlacement> findAllStudentPlacements();

    Optional<StudentPlacement> findStudentPlacementById(Long id);

    Optional<StudentPlacement> findActivePlacementByStudentId(Long studentId);

    List<StudentPlacement> findPlacementHistoryByStudentId(Long studentId);

    List<StudentPlacement> findActivePlacementsByClassId(Long classId);

    void saveStudentPlacement(StudentPlacement placement);

    void updateStudentPlacement(StudentPlacement placement);

    void transitionStudentPlacement(StudentPlacement previousPlacement, StudentPlacement newPlacement);
}
