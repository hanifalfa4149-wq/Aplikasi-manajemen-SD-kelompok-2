package id.ac.sekolah.manajemenkelas.academic.dao;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;

import java.util.List;
import java.util.Optional;

public interface AcademicDAO {

    // Academic Year
    List<AcademicYear> findAllAcademicYears();

    Optional<AcademicYear> findAcademicYearById(Long id);

    Optional<AcademicYear> findAcademicYearByName(String name);

    void saveAcademicYear(AcademicYear academicYear);

    void updateAcademicYear(AcademicYear academicYear);

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
}
