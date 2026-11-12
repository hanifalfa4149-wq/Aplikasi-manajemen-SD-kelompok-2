package id.ac.sekolah.manajemenkelas.assessment.dao;

import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;

public interface AssessmentDAO {

    // Penilaian (Assessment)
    List<Assessment> findAllAssessments();

    Optional<Assessment> findAssessmentById(Long id);

    List<Assessment> findAssessmentsByTeachingAssignmentId(Long idPenugasanMengajar);

    void saveAssessment(Assessment penilaian);

    void updateAssessment(Assessment penilaian);

    // Nilai Siswa (Student Score)
    Optional<StudentScore> findScoreById(Long id);

    Optional<StudentScore> findScoreByAssessmentAndStudent(Long idPenilaian, Long idSiswa);

    List<StudentScore> findScoresByAssessmentId(Long idPenilaian);

    List<StudentScore> findScoresByStudentId(Long idSiswa);

    void saveScore(StudentScore nilaiSiswa);

    void saveAllScores(List<StudentScore> daftarNilaiSiswa);

    void updateScore(StudentScore nilaiSiswa);
}
