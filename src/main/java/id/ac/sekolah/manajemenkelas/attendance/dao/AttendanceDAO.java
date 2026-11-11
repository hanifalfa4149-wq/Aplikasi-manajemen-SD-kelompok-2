package id.ac.sekolah.manajemenkelas.attendance.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;

public interface AttendanceDAO {

    Optional<Attendance> findById(Long id);

    Optional<Attendance> findByStudentAndDate(Long studentId, LocalDate date);

    List<Attendance> findByDate(LocalDate date);

    List<Attendance> findByClassId(Long classId);

    List<Attendance> findByClassAndDate(Long classId, LocalDate date);

    List<Attendance> findByClassAndDateRange(Long classId, LocalDate startDate, LocalDate endDate);

    List<Attendance> findByStudentId(Long studentId);

    void save(Attendance attendance);

    void saveAll(List<Attendance> attendances);

    void update(Attendance attendance);
}

