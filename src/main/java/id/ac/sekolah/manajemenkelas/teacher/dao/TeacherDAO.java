package id.ac.sekolah.manajemenkelas.teacher.dao;

import java.util.List;
import java.util.Optional;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public interface TeacherDAO {

    List<Teacher> findAll();

    Optional<Teacher> findById(Long id);

    Optional<Teacher> findByNip(String nip);

    void save(Teacher teacher);

    void update(Teacher teacher);

    void deleteById(Long id);

    boolean existsByNip(String nip);
}