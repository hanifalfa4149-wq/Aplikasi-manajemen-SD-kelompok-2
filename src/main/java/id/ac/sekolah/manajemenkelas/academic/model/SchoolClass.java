package id.ac.sekolah.manajemenkelas.academic.model;

public class SchoolClass {

    private Long id;
    private Long academicYearId;
    private String name;
    private int gradeLevel;
    private int capacity;
    private boolean active;

    public SchoolClass(
            Long id,
            Long academicYearId,
            String name,
            int gradeLevel,
            int capacity,
            boolean active
    ) {
        this.id = id;
        this.academicYearId = academicYearId;
        this.name = name;
        this.gradeLevel = gradeLevel;
        this.capacity = capacity;
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

    public String getName() {
        return name;
    }

    public int getGradeLevel() {
        return gradeLevel;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isActive() {
        return active;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGradeLevel(int gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}