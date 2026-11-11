package id.ac.sekolah.manajemenkelas.assessment.dao;

import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;

public interface AssessmentDAO {

    // Assessment
    List<Assessment> findAllAssessments();

    Optional<Assessment> findAssessmentById(Long id);

    List<Assessment> findAssessmentsByTeachingAssignmentId(Long teachingAssignmentId);

    void saveAssessment(Assessment assessment);

    void updateAssessment(Assessment assessment);

    // Student Score
    Optional<StudentScore> findScoreById(Long id);

    Optional<StudentScore> findScoreByAssessmentAndStudent(Long assessmentId, Long studentId);

    List<StudentScore> findScoresByAssessmentId(Long assessmentId);

    List<StudentScore> findScoresByStudentId(Long studentId);

    void saveScore(StudentScore score);

    void saveAllScores(List<StudentScore> scores);

    void updateScore(StudentScore score);
}

