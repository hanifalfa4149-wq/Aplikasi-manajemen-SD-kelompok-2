package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AssessmentReport {

    private final Long idPenilaian;
    private final Long idPenugasanMengajar;
    private final String namaKelas;
    private final String namaMataPelajaran;
    private final String judul;
    private final String jenisPenilaian;
    private final LocalDate tanggalPenilaian;
    private final BigDecimal nilaiMaksimal;
    private final int jumlahSiswaDinilai;
    private final BigDecimal rataRataNilai;
    private final BigDecimal nilaiTertinggi;
    private final BigDecimal nilaiTerendah;
    private final BigDecimal rataRataPersentase;

    public AssessmentReport(
            Long idPenilaian,
            Long idPenugasanMengajar,
            String namaKelas,
            String namaMataPelajaran,
            String judul,
            String jenisPenilaian,
            LocalDate tanggalPenilaian,
            BigDecimal nilaiMaksimal,
            int jumlahSiswaDinilai,
            BigDecimal rataRataNilai,
            BigDecimal nilaiTertinggi,
            BigDecimal nilaiTerendah,
            BigDecimal rataRataPersentase) {
        this.idPenilaian = idPenilaian;
        this.idPenugasanMengajar = idPenugasanMengajar;
        this.namaKelas = namaKelas;
        this.namaMataPelajaran = namaMataPelajaran;
        this.judul = judul;
        this.jenisPenilaian = jenisPenilaian;
        this.tanggalPenilaian = tanggalPenilaian;
        this.nilaiMaksimal = nilaiMaksimal;
        this.jumlahSiswaDinilai = jumlahSiswaDinilai;
        this.rataRataNilai = rataRataNilai;
        this.nilaiTertinggi = nilaiTertinggi;
        this.nilaiTerendah = nilaiTerendah;
        this.rataRataPersentase = rataRataPersentase;
    }

    public Long getAssessmentId() {
        return idPenilaian;
    }

    public Long getTeachingAssignmentId() {
        return idPenugasanMengajar;
    }

    public String getClassName() {
        return namaKelas;
    }

    public String getSubjectName() {
        return namaMataPelajaran;
    }

    public String getTitle() {
        return judul;
    }

    public String getAssessmentType() {
        return jenisPenilaian;
    }

    public LocalDate getAssessmentDate() {
        return tanggalPenilaian;
    }

    public BigDecimal getMaxScore() {
        return nilaiMaksimal;
    }

    public int getScoredStudentsCount() {
        return jumlahSiswaDinilai;
    }

    public BigDecimal getAverageScore() {
        return rataRataNilai;
    }

    public BigDecimal getHighestScore() {
        return nilaiTertinggi;
    }

    public BigDecimal getLowestScore() {
        return nilaiTerendah;
    }

    public BigDecimal getAveragePercentage() {
        return rataRataPersentase;
    }

    public boolean hasScores() {
        return jumlahSiswaDinilai > 0 && rataRataNilai != null;
    }
}
