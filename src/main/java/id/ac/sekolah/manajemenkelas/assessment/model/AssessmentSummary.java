package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;

public class AssessmentSummary {

    private final Long assessmentId;
    private final String title;
    private final String assessmentType;
    private final BigDecimal maxScore;
    private final int totalStudentsScored;
    private final BigDecimal averageScore;
    private final BigDecimal highestScore;
    private final BigDecimal lowestScore;
    private final BigDecimal averagePercentage;

    public AssessmentSummary(
            Long assessmentId,
            String title,
            String assessmentType,
            BigDecimal maxScore,
            int totalStudentsScored,
            BigDecimal averageScore,
            BigDecimal highestScore,
            BigDecimal lowestScore,
            BigDecimal averagePercentage) {
        this.assessmentId = assessmentId;
        this.title = title;
        this.assessmentType = assessmentType;
        this.maxScore = maxScore;
        this.totalStudentsScored = totalStudentsScored;
        this.averageScore = averageScore;
        this.highestScore = highestScore;
        this.lowestScore = lowestScore;
        this.averagePercentage = averagePercentage;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public String getTitle() {
        return title;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public int getTotalStudentsScored() {
        return totalStudentsScored;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public BigDecimal getHighestScore() {
        return highestScore;
    }

    public BigDecimal getLowestScore() {
        return lowestScore;
    }

    public BigDecimal getAveragePercentage() {
        return averagePercentage;
    }
}

