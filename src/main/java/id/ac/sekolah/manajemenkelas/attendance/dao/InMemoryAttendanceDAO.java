package id.ac.sekolah.manajemenkelas.attendance.dao;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.attendance.model.Attendance;

public class InMemoryAttendanceDAO implements AttendanceDAO {

    private final List<Attendance> records = new ArrayList<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Optional<Attendance> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return records.stream()
                .filter(a -> id.equals(a.getId()))
                .findFirst();
    }

    @Override
    public Optional<Attendance> findByStudentAndDate(Long studentId, LocalDate date) {
        if (studentId == null || date == null) {
            return Optional.empty();
        }
        return records.stream()
                .filter(a -> studentId.equals(a.getStudentId()) && date.equals(a.getAttendanceDate()))
                .findFirst();
    }

    @Override
    public List<Attendance> findByDate(LocalDate date) {
        if (date == null) {
            return List.of();
        }
        return records.stream()
                .filter(a -> date.equals(a.getAttendanceDate()))
                .toList();
    }

    @Override
    public List<Attendance> findByClassId(Long classId) {
        if (classId == null) {
            return List.of();
        }
        return records.stream()
                .filter(a -> classId.equals(a.getClassId()))
                .toList();
    }

    @Override
    public List<Attendance> findByClassAndDate(Long classId, LocalDate date) {
        if (classId == null || date == null) {
            return List.of();
        }
        return records.stream()
                .filter(a -> classId.equals(a.getClassId()) && date.equals(a.getAttendanceDate()))
                .toList();
    }

    @Override
    public List<Attendance> findByClassAndDateRange(Long classId, LocalDate startDate, LocalDate endDate) {
        if (classId == null || startDate == null || endDate == null) {
            return List.of();
        }
        return records.stream()
                .filter(a -> classId.equals(a.getClassId())
                        && !a.getAttendanceDate().isBefore(startDate)
                        && !a.getAttendanceDate().isAfter(endDate))
                .toList();
    }

    @Override
    public List<Attendance> findByStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return records.stream()
                .filter(a -> studentId.equals(a.getStudentId()))
                .toList();
    }

    @Override
    public void save(Attendance attendance) {
        if (findByStudentAndDate(attendance.getStudentId(), attendance.getAttendanceDate()).isPresent()) {
            throw new IllegalArgumentException("Siswa sudah memiliki catatan absensi pada tanggal tersebut.");
        }
        if (attendance.getId() == null) {
            attendance.setId(idGenerator.getAndIncrement());
        }
        records.add(attendance);
    }

    @Override
    public void saveAll(List<Attendance> attendances) {
        if (attendances == null || attendances.isEmpty()) {
            return;
        }
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Attendance a : attendances) {
            String key = a.getStudentId() + ":" + a.getAttendanceDate();
            if (!seen.add(key) || findByStudentAndDate(a.getStudentId(), a.getAttendanceDate()).isPresent()) {
                throw new IllegalArgumentException("Siswa sudah memiliki catatan absensi pada tanggal tersebut.");
            }
        }
        for (Attendance a : attendances) {
            save(a);
        }
    }

    @Override
    public void update(Attendance attendance) {
        findById(attendance.getId()).ifPresent(existing -> {
            int index = records.indexOf(existing);
            records.set(index, attendance);
        });
    }
}

