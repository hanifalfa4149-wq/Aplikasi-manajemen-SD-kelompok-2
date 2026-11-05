package id.ac.sekolah.manajemenkelas.academic.dao;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryAcademicDAO implements AcademicDAO {

    private final List<AcademicYear> academicYears = new ArrayList<>();
    private final List<SchoolClass> classes = new ArrayList<>();
    private final List<Subject> subjects = new ArrayList<>();

    // Academic Year

    @Override
    public List<AcademicYear> findAllAcademicYears() {
        return new ArrayList<>(academicYears);
    }

    @Override
    public Optional<AcademicYear> findAcademicYearById(Long id) {
        return academicYears.stream()
                .filter(year -> year.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<AcademicYear> findAcademicYearByName(String name) {
        return academicYears.stream()
                .filter(year -> year.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public void saveAcademicYear(AcademicYear academicYear) {
        academicYears.add(academicYear);
    }

    @Override
    public void updateAcademicYear(AcademicYear academicYear) {
        findAcademicYearById(academicYear.getId())
                .ifPresent(existing -> {
                    int index = academicYears.indexOf(existing);
                    academicYears.set(index, academicYear);
                });
    }

    // School Class

    @Override
    public List<SchoolClass> findAllClasses() {
        return new ArrayList<>(classes);
    }

    @Override
    public Optional<SchoolClass> findClassById(Long id) {
        return classes.stream()
                .filter(schoolClass -> schoolClass.getId().equals(id))
                .findFirst();
    }

    @Override
    public void saveClass(SchoolClass schoolClass) {
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
        return subjects.stream()
                .filter(subject -> subject.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<Subject> findSubjectByCode(String code) {
        return subjects.stream()
                .filter(subject -> subject.getCode().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public void saveSubject(Subject subject) {
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
}
