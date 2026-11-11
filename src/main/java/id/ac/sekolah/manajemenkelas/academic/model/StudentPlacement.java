package id.ac.sekolah.manajemenkelas.academic.model;

import java.time.LocalDate;

public class StudentPlacement {

    public static final String STATUS_AKTIF = "AKTIF";
    public static final String STATUS_NAIK_KELAS = "NAIK_KELAS";
    public static final String STATUS_PINDAH = "PINDAH";
    public static final String STATUS_LULUS = "LULUS";
    public static final String STATUS_KELUAR = "KELUAR";

    private Long id;
    private Long studentId;
    private Long classId;
    private Long academicYearId;
    private LocalDate entryDate;
    private LocalDate exitDate;
    private String status;

    public StudentPlacement(
            Long id,
            Long studentId,
            Long classId,
            Long academicYearId,
            LocalDate entryDate,
            LocalDate exitDate,
            String status) {
        this.id = id;
        this.studentId = studentId;
        this.classId = classId;
        this.academicYearId = academicYearId;
        this.entryDate = entryDate;
        this.exitDate = exitDate;
        this.status = status;
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

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public LocalDate getExitDate() {
        return exitDate;
    }

    public void setExitDate(LocalDate exitDate) {
        this.exitDate = exitDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isActive() {
        return STATUS_AKTIF.equalsIgnoreCase(status) && exitDate == null;
    }
}

