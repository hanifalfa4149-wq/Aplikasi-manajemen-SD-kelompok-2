package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;

public class AssessmentSummary {

    private final Long idPenilaian;
    private final String judul;
    private final String jenisPenilaian;
    private final BigDecimal nilaiMaksimal;
    private final int totalSiswaDinilai;
    private final BigDecimal rataRataNilai;
    private final BigDecimal nilaiTertinggi;
    private final BigDecimal nilaiTerendah;
    private final BigDecimal rataRataPersentase;

    public AssessmentSummary(
            Long idPenilaian,
            String judul,
            String jenisPenilaian,
            BigDecimal nilaiMaksimal,
            int totalSiswaDinilai,
            BigDecimal rataRataNilai,
            BigDecimal nilaiTertinggi,
            BigDecimal nilaiTerendah,
            BigDecimal rataRataPersentase) {
        this.idPenilaian = idPenilaian;
        this.judul = judul;
        this.jenisPenilaian = jenisPenilaian;
        this.nilaiMaksimal = nilaiMaksimal;
        this.totalSiswaDinilai = totalSiswaDinilai;
        this.rataRataNilai = rataRataNilai;
        this.nilaiTertinggi = nilaiTertinggi;
        this.nilaiTerendah = nilaiTerendah;
        this.rataRataPersentase = rataRataPersentase;
    }

    public Long getAssessmentId() {
        return idPenilaian;
    }

    public String getTitle() {
        return judul;
    }

    public String getAssessmentType() {
        return jenisPenilaian;
    }

    public BigDecimal getMaxScore() {
        return nilaiMaksimal;
    }

    public int getTotalStudentsScored() {
        return totalSiswaDinilai;
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
}
