package id.ac.sekolah.manajemenkelas.attendance.model;

import java.time.LocalDate;

public class Attendance {

    public static final String STATUS_HADIR = "HADIR";
    public static final String STATUS_SAKIT = "SAKIT";
    public static final String STATUS_IZIN = "IZIN";
    public static final String STATUS_ALPA = "ALPA";

    private Long id;
    private Long studentId;
    private Long classId;
    private LocalDate attendanceDate;
    private String status;
    private String notes;
    private Long recordedBy;

    public Attendance(
            Long id,
            Long studentId,
            Long classId,
            LocalDate attendanceDate,
            String status,
            String notes,
            Long recordedBy) {
        this.id = id;
        this.studentId = studentId;
        this.classId = classId;
        this.attendanceDate = attendanceDate;
        this.status = status;
        this.notes = notes;
        this.recordedBy = recordedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }
}

