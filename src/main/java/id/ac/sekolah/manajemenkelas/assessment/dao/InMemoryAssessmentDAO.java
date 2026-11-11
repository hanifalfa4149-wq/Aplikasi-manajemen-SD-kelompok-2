package id.ac.sekolah.manajemenkelas.assessment.dao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;

public class InMemoryAssessmentDAO implements AssessmentDAO {

    private final List<Assessment> assessments = new ArrayList<>();
    private final List<StudentScore> scores = new ArrayList<>();
    private final AtomicLong assessmentIdGenerator = new AtomicLong(1);
    private final AtomicLong scoreIdGenerator = new AtomicLong(1);

    @Override
    public List<Assessment> findAllAssessments() {
        return assessments.stream()
                .sorted(Comparator.comparing(Assessment::getAssessmentDate).reversed()
                        .thenComparing(Assessment::getId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public Optional<Assessment> findAssessmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return assessments.stream()
                .filter(a -> a.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Assessment> findAssessmentsByTeachingAssignmentId(Long teachingAssignmentId) {
        if (teachingAssignmentId == null) {
            return List.of();
        }
        return assessments.stream()
                .filter(a -> a.getTeachingAssignmentId().equals(teachingAssignmentId))
                .sorted(Comparator.comparing(Assessment::getAssessmentDate).reversed()
                        .thenComparing(Assessment::getId, Comparator.reverseOrder()))
                .toList();
    }

    @Override
    public void saveAssessment(Assessment assessment) {
        if (assessment == null) {
            throw new IllegalArgumentException("Data penilaian tidak boleh null.");
        }
        if (assessment.getId() == null) {
            assessment.setId(assessmentIdGenerator.getAndIncrement());
        }
        assessments.add(assessment);
    }

    @Override
    public void updateAssessment(Assessment assessment) {
        if (assessment == null || assessment.getId() == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi untuk pembaruan.");
        }
        for (int i = 0; i < assessments.size(); i++) {
            if (assessments.get(i).getId().equals(assessment.getId())) {
                assessments.set(i, assessment);
                return;
            }
        }
        throw new IllegalArgumentException("Data penilaian tidak ditemukan.");
    }

    @Override
    public Optional<StudentScore> findScoreById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return scores.stream()
                .filter(s -> s.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<StudentScore> findScoreByAssessmentAndStudent(Long assessmentId, Long studentId) {
        if (assessmentId == null || studentId == null) {
            return Optional.empty();
        }
        return scores.stream()
                .filter(s -> s.getAssessmentId().equals(assessmentId) && s.getStudentId().equals(studentId))
                .findFirst();
    }

    @Override
    public List<StudentScore> findScoresByAssessmentId(Long assessmentId) {
        if (assessmentId == null) {
            return List.of();
        }
        return scores.stream()
                .filter(s -> s.getAssessmentId().equals(assessmentId))
                .sorted(Comparator.comparing(StudentScore::getStudentId))
                .toList();
    }

    @Override
    public List<StudentScore> findScoresByStudentId(Long studentId) {
        if (studentId == null) {
            return List.of();
        }
        return scores.stream()
                .filter(s -> s.getStudentId().equals(studentId))
                .sorted(Comparator.comparing(StudentScore::getId).reversed())
                .toList();
    }

    @Override
    public void saveScore(StudentScore score) {
        if (score == null) {
            throw new IllegalArgumentException("Data nilai siswa tidak boleh null.");
        }
        if (findScoreByAssessmentAndStudent(score.getAssessmentId(), score.getStudentId()).isPresent()) {
            throw new IllegalArgumentException("Nilai siswa untuk penilaian ini sudah tercatat.");
        }
        if (score.getId() == null) {
            score.setId(scoreIdGenerator.getAndIncrement());
        }
        scores.add(score);
    }

    @Override
    public void saveAllScores(List<StudentScore> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        java.util.Set<String> seenInBatch = new java.util.HashSet<>();
        for (StudentScore item : batch) {
            String key = item.getAssessmentId() + ":" + item.getStudentId();
            if (!seenInBatch.add(key)) {
                throw new IllegalArgumentException(
                        "Terdapat duplikasi siswa ID " + item.getStudentId() + " dalam batch nilai.");
            }
            if (findScoreByAssessmentAndStudent(item.getAssessmentId(), item.getStudentId()).isPresent()) {
                throw new IllegalArgumentException(
                        "Nilai siswa ID " + item.getStudentId() + " untuk penilaian ini sudah tercatat.");
            }
        }
        for (StudentScore item : batch) {
            saveScore(item);
        }
    }

    @Override
    public void updateScore(StudentScore score) {
        if (score == null || score.getId() == null) {
            throw new IllegalArgumentException("ID nilai siswa wajib diisi untuk pembaruan.");
        }
        for (int i = 0; i < scores.size(); i++) {
            if (scores.get(i).getId().equals(score.getId())) {
                scores.set(i, score);
                return;
            }
        }
        throw new IllegalArgumentException("Data nilai siswa tidak ditemukan.");
    }
}

