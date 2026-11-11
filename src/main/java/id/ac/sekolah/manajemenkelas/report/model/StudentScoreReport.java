package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;

public class StudentScoreReport {

    private final Long studentId;
    private final String nis;
    private final String studentName;
    private final String currentClassName;
    private final int assessmentCount;
    private final BigDecimal totalEarnedScore;
    private final BigDecimal totalMaxScore;
    private final BigDecimal averageScore;
    private final BigDecimal overallPercentage;

    public StudentScoreReport(
            Long studentId,
            String nis,
            String studentName,
            String currentClassName,
            int assessmentCount,
            BigDecimal totalEarnedScore,
            BigDecimal totalMaxScore,
            BigDecimal averageScore,
            BigDecimal overallPercentage) {
        this.studentId = studentId;
        this.nis = nis;
        this.studentName = studentName;
        this.currentClassName = currentClassName;
        this.assessmentCount = assessmentCount;
        this.totalEarnedScore = totalEarnedScore;
        this.totalMaxScore = totalMaxScore;
        this.averageScore = averageScore;
        this.overallPercentage = overallPercentage;
    }

    public Long getStudentId() {
        return studentId;
    }

    public String getNis() {
        return nis;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCurrentClassName() {
        return currentClassName;
    }

    public int getAssessmentCount() {
        return assessmentCount;
    }

    public BigDecimal getTotalEarnedScore() {
        return totalEarnedScore;
    }

    public BigDecimal getTotalMaxScore() {
        return totalMaxScore;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public BigDecimal getOverallPercentage() {
        return overallPercentage;
    }

    public boolean hasScores() {
        return assessmentCount > 0 && overallPercentage != null;
    }
}

