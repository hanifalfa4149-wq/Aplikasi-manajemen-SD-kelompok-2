package id.ac.sekolah.manajemenkelas.teacher.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class InMemoryTeacherDAO implements TeacherDAO {

    private final List<Teacher> teachers = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public List<Teacher> findAll() {
        return new ArrayList<>(teachers);
    }

    @Override
    public Optional<Teacher> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return teachers.stream()
                .filter(teacher -> id.equals(teacher.getId()))
                .findFirst();
    }

    @Override
    public Optional<Teacher> findByNip(String nip) {
        if (nip == null) {
            return Optional.empty();
        }
        return teachers.stream()
                .filter(teacher -> teacher.getNip() != null && teacher.getNip().equalsIgnoreCase(nip))
                .findFirst();
    }

    @Override
    public void save(Teacher teacher) {
        if (teacher.getId() == null) {
            teacher.setId(idGenerator.getAndIncrement());
        }
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
        if (id == null) {
            return;
        }
        teachers.removeIf(teacher -> id.equals(teacher.getId()));
    }

    @Override
    public boolean existsByNip(String nip) {
        return findByNip(nip).isPresent();
    }
}
