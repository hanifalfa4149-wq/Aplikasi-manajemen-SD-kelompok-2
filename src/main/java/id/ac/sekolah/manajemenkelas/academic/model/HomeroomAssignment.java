package id.ac.sekolah.manajemenkelas.academic.model;

import java.time.LocalDate;

public class HomeroomAssignment {

    private Long id;
    private Long classId;
    private Long teacherId;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public HomeroomAssignment(
            Long id,
            Long classId,
            Long teacherId,
            LocalDate startDate,
            LocalDate endDate,
            boolean active) {
        this.id = id;
        this.classId = classId;
        this.teacherId = teacherId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

