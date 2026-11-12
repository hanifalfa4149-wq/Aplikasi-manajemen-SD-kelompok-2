package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;

public class StudentScoreSummary {

    private final Long idSiswa;
    private final int totalPenilaian;
    private final BigDecimal totalNilaiDiperoleh;
    private final BigDecimal totalNilaiMaksimal;
    private final BigDecimal rataRataNilai;
    private final BigDecimal persentaseKeseluruhan;

    public StudentScoreSummary(
            Long idSiswa,
            int totalPenilaian,
            BigDecimal totalNilaiDiperoleh,
            BigDecimal totalNilaiMaksimal,
            BigDecimal rataRataNilai,
            BigDecimal persentaseKeseluruhan) {
        this.idSiswa = idSiswa;
        this.totalPenilaian = totalPenilaian;
        this.totalNilaiDiperoleh = totalNilaiDiperoleh;
        this.totalNilaiMaksimal = totalNilaiMaksimal;
        this.rataRataNilai = rataRataNilai;
        this.persentaseKeseluruhan = persentaseKeseluruhan;
    }

    public Long getStudentId() {
        return idSiswa;
    }

    public int getTotalAssessments() {
        return totalPenilaian;
    }

    public BigDecimal getTotalEarnedScore() {
        return totalNilaiDiperoleh;
    }

    public BigDecimal getTotalMaxScore() {
        return totalNilaiMaksimal;
    }

    public BigDecimal getAverageScore() {
        return rataRataNilai;
    }

    public BigDecimal getOverallPercentage() {
        return persentaseKeseluruhan;
    }
}
