package id.ac.sekolah.manajemenkelas.academic.model;

import java.time.LocalDate;

public class Semester {

    private Long id;
    private Long academicYearId;
    private int semesterNumber;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public Semester(
            Long id,
            Long academicYearId,
            int semesterNumber,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            boolean active) {
        this.id = id;
        this.academicYearId = academicYearId;
        this.semesterNumber = semesterNumber;
        this.name = name;
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

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public int getSemesterNumber() {
        return semesterNumber;
    }

    public void setSemesterNumber(int semesterNumber) {
        this.semesterNumber = semesterNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

