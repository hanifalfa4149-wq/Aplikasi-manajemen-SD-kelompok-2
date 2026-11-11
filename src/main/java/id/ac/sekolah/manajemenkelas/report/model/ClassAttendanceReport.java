package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;

public class ClassAttendanceReport {

    private final Long classId;
    private final String className;
    private final int gradeLevel;
    private final int totalRecords;
    private final int hadirCount;
    private final int sakitCount;
    private final int izinCount;
    private final int alpaCount;
    private final BigDecimal attendancePercentage;

    public ClassAttendanceReport(
            Long classId,
            String className,
            int gradeLevel,
            int totalRecords,
            int hadirCount,
            int sakitCount,
            int izinCount,
            int alpaCount,
            BigDecimal attendancePercentage) {
        this.classId = classId;
        this.className = className;
        this.gradeLevel = gradeLevel;
        this.totalRecords = totalRecords;
        this.hadirCount = hadirCount;
        this.sakitCount = sakitCount;
        this.izinCount = izinCount;
        this.alpaCount = alpaCount;
        this.attendancePercentage = attendancePercentage;
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

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getHadirCount() {
        return hadirCount;
    }

    public int getSakitCount() {
        return sakitCount;
    }

    public int getIzinCount() {
        return izinCount;
    }

    public int getAlpaCount() {
        return alpaCount;
    }

    public BigDecimal getAttendancePercentage() {
        return attendancePercentage;
    }

    public boolean hasSufficientData() {
        return totalRecords > 0 && attendancePercentage != null;
    }
}

