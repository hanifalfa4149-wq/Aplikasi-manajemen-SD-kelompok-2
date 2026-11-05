package id.ac.sekolah.manajemenkelas.teacher.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class InMemoryTeacherDAO implements TeacherDAO {

    private final List<Teacher> teachers = new ArrayList<>();

    @Override
    public List<Teacher> findAll() {
        return new ArrayList<>(teachers);
    }

    @Override
    public Optional<Teacher> findById(Long id) {
        return teachers.stream()
                .filter(teacher -> teacher.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<Teacher> findByNip(String nip) {
        return teachers.stream()
                .filter(teacher -> teacher.getNip().equalsIgnoreCase(nip))
                .findFirst();
    }

    @Override
    public void save(Teacher teacher) {
        teachers.add(teacher);
    }

    @Override
    public void update(Teacher teacher) {
        findById(teacher.getId()).ifPresent(existing -> {
            int index = teachers.indexOf(existing);
            teachers.set(index, teacher);
        });
    }

    @Override
    public void deleteById(Long id) {
        teachers.removeIf(teacher -> teacher.getId().equals(id));
    }

    @Override
    public boolean existsByNip(String nip) {
        return findByNip(nip).isPresent();
    }
}