package id.ac.sekolah.manajemenkelas.report.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

    private final ReportDAO daoLaporan;
    private final StudentDAO daoSiswa;

    public ReportService(ReportDAO daoLaporan, StudentDAO daoSiswa) {
        if (daoLaporan == null || daoSiswa == null) {
            throw new IllegalArgumentException("Dependensi ReportService tidak boleh null.");
        }
        this.daoLaporan = daoLaporan;
        this.daoSiswa = daoSiswa;
    }

    public List<ClassStudentCountReport> getStudentCountsByClass() {
        return daoLaporan.findActiveStudentCountsByClass(tentukanCakupanGuru());
    }

    public List<GenderStudentCountReport> getStudentCountsByGender() {
        return daoLaporan.findActiveStudentCountsByGender(tentukanCakupanGuru());
    }

    public AttendanceRangeReport getAttendanceSummaryByDateRange(LocalDate tanggalMulai, LocalDate tanggalSelesai) {
        validasiRentangTanggal(tanggalMulai, tanggalSelesai);
        return daoLaporan.findAttendanceSummaryByDateRange(tanggalMulai, tanggalSelesai, tentukanCakupanGuru());
    }

    public List<ClassAttendanceReport> getAttendanceSummaryByClass() {
        return daoLaporan.findAttendanceSummariesByClass(null, null, tentukanCakupanGuru());
    }

    public List<ClassAttendanceReport> getAttendanceSummaryByClass(LocalDate tanggalMulai, LocalDate tanggalSelesai) {
        validasiRentangTanggal(tanggalMulai, tanggalSelesai);
        return daoLaporan.findAttendanceSummariesByClass(tanggalMulai, tanggalSelesai, tentukanCakupanGuru());
    }

    public List<AssessmentReport> getAssessmentSummaries() {
        return daoLaporan.findAssessmentSummaries(tentukanCakupanGuru());
    }

    public List<StudentScoreReport> getStudentScoreSummaries() {
        return daoLaporan.findStudentScoreSummaries(tentukanCakupanGuru());
    }

    public StudentScoreReport getStudentScoreSummary(Long idSiswa) {
        Long idCakupanGuru = tentukanCakupanGuru();
        if (idSiswa == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }
        if (daoSiswa.findById(idSiswa).isEmpty()) {
            throw new IllegalArgumentException("Data siswa tidak ditemukan.");
        }

        Optional<StudentScoreReport> hasilCari = daoLaporan.findStudentScoreSummaryByStudentId(idSiswa, idCakupanGuru);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException(
                    "Akses ditolak atau siswa tidak aktif dalam cakupan akses Anda.");
        }
        return hasilCari.get();
    }

    public List<SubjectScoreReport> getAverageScoresBySubject() {
        return daoLaporan.findAverageScoresBySubject(tentukanCakupanGuru());
    }

    private Long tentukanCakupanGuru() {
        return SessionContext.getInstance().resolveTeacherScopeOrNullForAdmin("laporan");
    }

    private void validasiRentangTanggal(LocalDate tanggalMulai, LocalDate tanggalSelesai) {
        if (tanggalMulai == null || tanggalSelesai == null || tanggalSelesai.isBefore(tanggalMulai)) {
            throw new IllegalArgumentException("Rentang tanggal laporan tidak valid.");
        }
    }
}
