package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;

public class SubjectScoreReport {

    private final Long idMataPelajaran;
    private final String kodeMataPelajaran;
    private final String namaMataPelajaran;
    private final int jumlahPenilaian;
    private final int jumlahEntriDinilai;
    private final BigDecimal rataRataNilai;
    private final BigDecimal rataRataPersentase;

    public SubjectScoreReport(
            Long idMataPelajaran,
            String kodeMataPelajaran,
            String namaMataPelajaran,
            int jumlahPenilaian,
            int jumlahEntriDinilai,
            BigDecimal rataRataNilai,
            BigDecimal rataRataPersentase) {
        this.idMataPelajaran = idMataPelajaran;
        this.kodeMataPelajaran = kodeMataPelajaran;
        this.namaMataPelajaran = namaMataPelajaran;
        this.jumlahPenilaian = jumlahPenilaian;
        this.jumlahEntriDinilai = jumlahEntriDinilai;
        this.rataRataNilai = rataRataNilai;
        this.rataRataPersentase = rataRataPersentase;
    }

    public Long getSubjectId() {
        return idMataPelajaran;
    }

    public String getSubjectCode() {
        return kodeMataPelajaran;
    }

    public String getSubjectName() {
        return namaMataPelajaran;
    }

    public int getAssessmentCount() {
        return jumlahPenilaian;
    }

    public int getScoredEntriesCount() {
        return jumlahEntriDinilai;
    }

    public BigDecimal getAverageScore() {
        return rataRataNilai;
    }

    public BigDecimal getAveragePercentage() {
        return rataRataPersentase;
    }

    public boolean hasScores() {
        return jumlahEntriDinilai > 0 && rataRataNilai != null;
    }
}
