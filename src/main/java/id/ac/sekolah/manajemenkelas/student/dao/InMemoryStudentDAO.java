package id.ac.sekolah.manajemenkelas.student.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.student.model.Student;

public class InMemoryStudentDAO implements StudentDAO {

    private final List<Student> students = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(students);
    }

    @Override
    public Optional<Student> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return students.stream()
                .filter(student -> id.equals(student.getId()))
                .findFirst();
    }

    @Override
    public Optional<Student> findByNis(String nis) {
        if (nis == null) {
            return Optional.empty();
        }
        return students.stream()
                .filter(student -> student.getNis() != null && student.getNis().equalsIgnoreCase(nis))
                .findFirst();
    }

    @Override
    public Optional<Student> findByNisn(String nisn) {
        if (nisn == null) {
            return Optional.empty();
        }
        return students.stream()
                .filter(student -> student.getNisn() != null && student.getNisn().equalsIgnoreCase(nisn))
                .findFirst();
    }

    @Override
    public void save(Student student) {
        if (student.getId() == null) {
            student.setId(idGenerator.getAndIncrement());
        }
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
        if (id == null) {
            return;
        }
        students.removeIf(student -> id.equals(student.getId()));
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
