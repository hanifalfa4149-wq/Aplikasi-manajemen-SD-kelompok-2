package id.ac.sekolah.manajemenkelas.report.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.dashboard.model.DashboardSummary;
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;

public interface ReportDAO {

    DashboardSummary getDashboardSummary(Long idCakupanGuru);

    List<ClassStudentCountReport> findActiveStudentCountsByClass(Long idCakupanGuru);

    List<GenderStudentCountReport> findActiveStudentCountsByGender(Long idCakupanGuru);

    AttendanceRangeReport findAttendanceSummaryByDateRange(
            LocalDate tanggalMulai,
            LocalDate tanggalSelesai,
            Long idCakupanGuru);

    List<ClassAttendanceReport> findAttendanceSummariesByClass(
            LocalDate tanggalMulai,
            LocalDate tanggalSelesai,
            Long idCakupanGuru);

    List<AssessmentReport> findAssessmentSummaries(Long idCakupanGuru);

    List<StudentScoreReport> findStudentScoreSummaries(Long idCakupanGuru);

    Optional<StudentScoreReport> findStudentScoreSummaryByStudentId(Long idSiswa, Long idCakupanGuru);

    List<SubjectScoreReport> findAverageScoresBySubject(Long idCakupanGuru);
}
