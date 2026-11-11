package id.ac.sekolah.manajemenkelas.dashboard.model;

import java.math.BigDecimal;

public class DashboardSummary {

    private final String scopeLabel;
    private final int totalActiveStudents;
    private final int totalInactiveStudents;
    private final int totalActiveTeachers;
    private final int totalInactiveTeachers;
    private final int totalActiveClasses;
    private final int totalActiveSubjects;
    private final String activeAcademicYearName;
    private final int activeSemestersCount;
    private final int activeHomeroomCount;
    private final int activeTeachingAssignmentCount;
    private final int totalAssessmentsCount;
    private final int totalAttendanceRecords;
    private final BigDecimal overallAttendancePercentage;

    public DashboardSummary(
            String scopeLabel,
            int totalActiveStudents,
            int totalInactiveStudents,
            int totalActiveTeachers,
            int totalInactiveTeachers,
            int totalActiveClasses,
            int totalActiveSubjects,
            String activeAcademicYearName,
            int activeSemestersCount,
            int activeHomeroomCount,
            int activeTeachingAssignmentCount,
            int totalAssessmentsCount,
            int totalAttendanceRecords,
            BigDecimal overallAttendancePercentage) {
        this.scopeLabel = scopeLabel;
        this.totalActiveStudents = totalActiveStudents;
        this.totalInactiveStudents = totalInactiveStudents;
        this.totalActiveTeachers = totalActiveTeachers;
        this.totalInactiveTeachers = totalInactiveTeachers;
        this.totalActiveClasses = totalActiveClasses;
        this.totalActiveSubjects = totalActiveSubjects;
        this.activeAcademicYearName = activeAcademicYearName;
        this.activeSemestersCount = activeSemestersCount;
        this.activeHomeroomCount = activeHomeroomCount;
        this.activeTeachingAssignmentCount = activeTeachingAssignmentCount;
        this.totalAssessmentsCount = totalAssessmentsCount;
        this.totalAttendanceRecords = totalAttendanceRecords;
        this.overallAttendancePercentage = overallAttendancePercentage;
    }

    public String getScopeLabel() {
        return scopeLabel;
    }

    public int getTotalActiveStudents() {
        return totalActiveStudents;
    }

    public int getTotalInactiveStudents() {
        return totalInactiveStudents;
    }

    public int getTotalActiveTeachers() {
        return totalActiveTeachers;
    }

    public int getTotalInactiveTeachers() {
        return totalInactiveTeachers;
    }

    public int getTotalActiveClasses() {
        return totalActiveClasses;
    }

    public int getTotalActiveSubjects() {
        return totalActiveSubjects;
    }

    public String getActiveAcademicYearName() {
        return activeAcademicYearName;
    }

    public int getActiveSemestersCount() {
        return activeSemestersCount;
    }

    public int getActiveHomeroomCount() {
        return activeHomeroomCount;
    }

    public int getActiveTeachingAssignmentCount() {
        return activeTeachingAssignmentCount;
    }

    public int getTotalAssessmentsCount() {
        return totalAssessmentsCount;
    }

    public int getTotalAttendanceRecords() {
        return totalAttendanceRecords;
    }

    public BigDecimal getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public boolean hasAttendanceData() {
        return totalAttendanceRecords > 0 && overallAttendancePercentage != null;
    }
}

