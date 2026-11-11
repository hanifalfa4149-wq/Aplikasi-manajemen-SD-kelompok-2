package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Assessment {

    public static final String TYPE_KUIS = "KUIS";
    public static final String TYPE_TUGAS = "TUGAS";
    public static final String TYPE_UTS = "UTS";
    public static final String TYPE_UAS = "UAS";
    public static final String TYPE_PRAKTIK = "PRAKTIK";
    public static final String TYPE_PROYEK = "PROYEK";
    public static final String TYPE_LAINNYA = "LAINNYA";

    private Long id;
    private Long teachingAssignmentId;
    private String title;
    private String assessmentType;
    private LocalDate assessmentDate;
    private BigDecimal maxScore;
    private String description;

    public Assessment(
            Long id,
            Long teachingAssignmentId,
            String title,
            String assessmentType,
            LocalDate assessmentDate,
            BigDecimal maxScore,
            String description) {
        this.id = id;
        this.teachingAssignmentId = teachingAssignmentId;
        this.title = title;
        this.assessmentType = assessmentType;
        this.assessmentDate = assessmentDate;
        this.maxScore = maxScore;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeachingAssignmentId() {
        return teachingAssignmentId;
    }

    public void setTeachingAssignmentId(Long teachingAssignmentId) {
        this.teachingAssignmentId = teachingAssignmentId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAssessmentType() {
        return assessmentType;
    }

    public void setAssessmentType(String assessmentType) {
        this.assessmentType = assessmentType;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public void setAssessmentDate(LocalDate assessmentDate) {
        this.assessmentDate = assessmentDate;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(BigDecimal maxScore) {
        this.maxScore = maxScore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

