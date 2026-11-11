package id.ac.sekolah.manajemenkelas.report.service;

import java.time.LocalDate;
import java.util.List;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.report.dao.ReportDAO;
import id.ac.sekolah.manajemenkelas.report.model.AssessmentReport;
import id.ac.sekolah.manajemenkelas.report.model.AttendanceRangeReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassAttendanceReport;
import id.ac.sekolah.manajemenkelas.report.model.ClassStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.GenderStudentCountReport;
import id.ac.sekolah.manajemenkelas.report.model.StudentScoreReport;
import id.ac.sekolah.manajemenkelas.report.model.SubjectScoreReport;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;

public class ReportService {

    private final ReportDAO reportDAO;
    private final StudentDAO studentDAO;

    public ReportService(ReportDAO reportDAO, StudentDAO studentDAO) {
        if (reportDAO == null || studentDAO == null) {
            throw new IllegalArgumentException("Dependensi ReportService tidak boleh null.");
        }
        this.reportDAO = reportDAO;
        this.studentDAO = studentDAO;
    }

    public List<ClassStudentCountReport> getStudentCountsByClass() {
        return reportDAO.findActiveStudentCountsByClass(resolveScope());
    }

    public List<GenderStudentCountReport> getStudentCountsByGender() {
        return reportDAO.findActiveStudentCountsByGender(resolveScope());
    }

    public AttendanceRangeReport getAttendanceSummaryByDateRange(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return reportDAO.findAttendanceSummaryByDateRange(startDate, endDate, resolveScope());
    }

    public List<ClassAttendanceReport> getAttendanceSummaryByClass() {
        return reportDAO.findAttendanceSummariesByClass(null, null, resolveScope());
    }

    public List<ClassAttendanceReport> getAttendanceSummaryByClass(LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return reportDAO.findAttendanceSummariesByClass(startDate, endDate, resolveScope());
    }

    public List<AssessmentReport> getAssessmentSummaries() {
        return reportDAO.findAssessmentSummaries(resolveScope());
    }

    public List<StudentScoreReport> getStudentScoreSummaries() {
        return reportDAO.findStudentScoreSummaries(resolveScope());
    }

    public StudentScoreReport getStudentScoreSummary(Long studentId) {
        Long teacherScope = resolveScope();
        if (studentId == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }
        studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Data siswa tidak ditemukan."));

        return reportDAO.findStudentScoreSummaryByStudentId(studentId, teacherScope)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Akses ditolak atau siswa tidak aktif dalam cakupan akses Anda."));
    }

    public List<SubjectScoreReport> getAverageScoresBySubject() {
        return reportDAO.findAverageScoresBySubject(resolveScope());
    }

    private Long resolveScope() {
        return SessionContext.getInstance().resolveTeacherScopeOrNullForAdmin("laporan");
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("Rentang tanggal laporan tidak valid.");
        }
    }
}
