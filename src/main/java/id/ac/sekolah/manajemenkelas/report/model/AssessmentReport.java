package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AssessmentReport {

    private final Long assessmentId;
    private final Long teachingAssignmentId;
    private final String className;
    private final String subjectName;
    private final String title;
    private final String assessmentType;
    private final LocalDate assessmentDate;
    private final BigDecimal maxScore;
    private final int scoredStudentsCount;
    private final BigDecimal averageScore;
    private final BigDecimal highestScore;
    private final BigDecimal lowestScore;
    private final BigDecimal averagePercentage;

    public AssessmentReport(
            Long assessmentId,
            Long teachingAssignmentId,
            String className,
            String subjectName,
            String title,
            String assessmentType,
            LocalDate assessmentDate,
            BigDecimal maxScore,
            int scoredStudentsCount,
            BigDecimal averageScore,
            BigDecimal highestScore,
            BigDecimal lowestScore,
            BigDecimal averagePercentage) {
        this.assessmentId = assessmentId;
        this.teachingAssignmentId = teachingAssignmentId;
        this.className = className;
        this.subjectName = subjectName;
        this.title = title;
        this.assessmentType = assessmentType;
        this.assessmentDate = assessmentDate;
        this.maxScore = maxScore;
        this.scoredStudentsCount = scoredStudentsCount;
        this.averageScore = averageScore;
        this.highestScore = highestScore;
        this.lowestScore = lowestScore;
        this.averagePercentage = averagePercentage;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public Long getTeachingAssignmentId() {
        return teachingAssignmentId;
    }

    public String getClassName() {
        return className;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getTitle() {
        return title;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public int getScoredStudentsCount() {
        return scoredStudentsCount;
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

    public boolean hasScores() {
        return scoredStudentsCount > 0 && averageScore != null;
    }
}

