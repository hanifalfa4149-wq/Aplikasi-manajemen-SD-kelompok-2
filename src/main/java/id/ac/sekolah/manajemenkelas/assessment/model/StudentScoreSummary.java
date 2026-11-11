package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;

public class StudentScoreSummary {

    private final Long studentId;
    private final int totalAssessments;
    private final BigDecimal totalEarnedScore;
    private final BigDecimal totalMaxScore;
    private final BigDecimal averageScore;
    private final BigDecimal overallPercentage;

    public StudentScoreSummary(
            Long studentId,
            int totalAssessments,
            BigDecimal totalEarnedScore,
            BigDecimal totalMaxScore,
            BigDecimal averageScore,
            BigDecimal overallPercentage) {
        this.studentId = studentId;
        this.totalAssessments = totalAssessments;
        this.totalEarnedScore = totalEarnedScore;
        this.totalMaxScore = totalMaxScore;
        this.averageScore = averageScore;
        this.overallPercentage = overallPercentage;
    }

    public Long getStudentId() {
        return studentId;
    }

    public int getTotalAssessments() {
        return totalAssessments;
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
}

