package id.ac.sekolah.manajemenkelas.student.dao;

import id.ac.sekolah.manajemenkelas.student.model.Student;
import java.util.List;
import java.util.Optional;

public interface StudentDAO {

    List<Student> findAll();

    Optional<Student> findById(Long id);

    Optional<Student> findByNis(String nis);

    Optional<Student> findByNisn(String nisn);

    void save(Student student);

    void update(Student student);

    void deleteById(Long id);

    boolean existsByNis(String nis);

    boolean existsByNisn(String nisn);
}