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

    DashboardSummary getDashboardSummary(Long teacherIdScope);

    List<ClassStudentCountReport> findActiveStudentCountsByClass(Long teacherIdScope);

    List<GenderStudentCountReport> findActiveStudentCountsByGender(Long teacherIdScope);

    AttendanceRangeReport findAttendanceSummaryByDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Long teacherIdScope);

    List<ClassAttendanceReport> findAttendanceSummariesByClass(
            LocalDate startDate,
            LocalDate endDate,
            Long teacherIdScope);

    List<AssessmentReport> findAssessmentSummaries(Long teacherIdScope);

    List<StudentScoreReport> findStudentScoreSummaries(Long teacherIdScope);

    Optional<StudentScoreReport> findStudentScoreSummaryByStudentId(Long studentId, Long teacherIdScope);

    List<SubjectScoreReport> findAverageScoresBySubject(Long teacherIdScope);
}

