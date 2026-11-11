package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;

public class SubjectScoreReport {

    private final Long subjectId;
    private final String subjectCode;
    private final String subjectName;
    private final int assessmentCount;
    private final int scoredEntriesCount;
    private final BigDecimal averageScore;
    private final BigDecimal averagePercentage;

    public SubjectScoreReport(
            Long subjectId,
            String subjectCode,
            String subjectName,
            int assessmentCount,
            int scoredEntriesCount,
            BigDecimal averageScore,
            BigDecimal averagePercentage) {
        this.subjectId = subjectId;
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.assessmentCount = assessmentCount;
        this.scoredEntriesCount = scoredEntriesCount;
        this.averageScore = averageScore;
        this.averagePercentage = averagePercentage;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public int getAssessmentCount() {
        return assessmentCount;
    }

    public int getScoredEntriesCount() {
        return scoredEntriesCount;
    }

    public BigDecimal getAverageScore() {
        return averageScore;
    }

    public BigDecimal getAveragePercentage() {
        return averagePercentage;
    }

    public boolean hasScores() {
        return scoredEntriesCount > 0 && averageScore != null;
    }
}

