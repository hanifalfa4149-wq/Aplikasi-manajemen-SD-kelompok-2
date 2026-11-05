package id.ac.sekolah.manajemenkelas.student.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.student.model.Student;

public class InMemoryStudentDAO implements StudentDAO {

    private final List<Student> students = new ArrayList<>();

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(students);
    }

    @Override
    public Optional<Student> findById(Long id) {
        return students.stream()
                .filter(student -> student.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<Student> findByNis(String nis) {
        return students.stream()
                .filter(student -> student.getNis().equalsIgnoreCase(nis))
                .findFirst();
    }

    @Override
    public Optional<Student> findByNisn(String nisn) {
        return students.stream()
                .filter(student -> student.getNisn().equalsIgnoreCase(nisn))
                .findFirst();
    }

    @Override
    public void save(Student student) {
        students.add(student);
    }

    @Override
    public void update(Student student) {
        findById(student.getId()).ifPresent(existing -> {
            int index = students.indexOf(existing);
            students.set(index, student);
        });
    }

    @Override
    public void deleteById(Long id) {
        students.removeIf(student -> student.getId().equals(id));
    }

    @Override
    public boolean existsByNis(String nis) {
        return findByNis(nis).isPresent();
    }

    @Override
    public boolean existsByNisn(String nisn) {
        return findByNisn(nisn).isPresent();
    }
}
