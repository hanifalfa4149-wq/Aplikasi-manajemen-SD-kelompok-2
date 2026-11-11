package id.ac.sekolah.manajemenkelas.academic.model;

public class TeachingAssignment {

    private Long id;
    private Long teacherId;
    private Long classId;
    private Long subjectId;
    private Long semesterId;
    private boolean active;

    public TeachingAssignment(
            Long id,
            Long teacherId,
            Long classId,
            Long subjectId,
            Long semesterId,
            boolean active) {
        this.id = id;
        this.teacherId = teacherId;
        this.classId = classId;
        this.subjectId = subjectId;
        this.semesterId = semesterId;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getClassId() {
        return classId;
    }

    public void setClassId(Long classId) {
        this.classId = classId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

