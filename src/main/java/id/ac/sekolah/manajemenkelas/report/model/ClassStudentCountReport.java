package id.ac.sekolah.manajemenkelas.report.model;

public class ClassStudentCountReport {

    private final Long classId;
    private final String className;
    private final int gradeLevel;
    private final String academicYearName;
    private final int capacity;
    private final int activeStudentCount;

    public ClassStudentCountReport(
            Long classId,
            String className,
            int gradeLevel,
            String academicYearName,
            int capacity,
            int activeStudentCount) {
        this.classId = classId;
        this.className = className;
        this.gradeLevel = gradeLevel;
        this.academicYearName = academicYearName;
        this.capacity = capacity;
        this.activeStudentCount = activeStudentCount;
    }

    public Long getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public int getGradeLevel() {
        return gradeLevel;
    }

    public String getAcademicYearName() {
        return academicYearName;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getActiveStudentCount() {
        return activeStudentCount;
    }
}

