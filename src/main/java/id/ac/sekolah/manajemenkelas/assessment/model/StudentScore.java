package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;

public class StudentScore {

    private Long id;
    private Long assessmentId;
    private Long studentId;
    private BigDecimal score;
    private String notes;
    private Long recordedBy;

    public StudentScore(
            Long id,
            Long assessmentId,
            Long studentId,
            BigDecimal score,
            String notes,
            Long recordedBy) {
        this.id = id;
        this.assessmentId = assessmentId;
        this.studentId = studentId;
        this.score = score;
        this.notes = notes;
        this.recordedBy = recordedBy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Long assessmentId) {
        this.assessmentId = assessmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(Long recordedBy) {
        this.recordedBy = recordedBy;
    }
}

