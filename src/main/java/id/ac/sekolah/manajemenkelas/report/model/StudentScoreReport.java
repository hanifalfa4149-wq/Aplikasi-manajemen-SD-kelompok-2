package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;

public class StudentScoreReport {

    private final Long idSiswa;
    private final String nis;
    private final String namaSiswa;
    private final String namaKelasSaatIni;
    private final int jumlahPenilaian;
    private final BigDecimal totalNilaiDiperoleh;
    private final BigDecimal totalNilaiMaksimal;
    private final BigDecimal rataRataNilai;
    private final BigDecimal persentaseKeseluruhan;

    public StudentScoreReport(
            Long idSiswa,
            String nis,
            String namaSiswa,
            String namaKelasSaatIni,
            int jumlahPenilaian,
            BigDecimal totalNilaiDiperoleh,
            BigDecimal totalNilaiMaksimal,
            BigDecimal rataRataNilai,
            BigDecimal persentaseKeseluruhan) {
        this.idSiswa = idSiswa;
        this.nis = nis;
        this.namaSiswa = namaSiswa;
        this.namaKelasSaatIni = namaKelasSaatIni;
        this.jumlahPenilaian = jumlahPenilaian;
        this.totalNilaiDiperoleh = totalNilaiDiperoleh;
        this.totalNilaiMaksimal = totalNilaiMaksimal;
        this.rataRataNilai = rataRataNilai;
        this.persentaseKeseluruhan = persentaseKeseluruhan;
    }

    public Long getStudentId() {
        return idSiswa;
    }

    public String getNis() {
        return nis;
    }

    public String getStudentName() {
        return namaSiswa;
    }

    public String getCurrentClassName() {
        return namaKelasSaatIni;
    }

    public int getAssessmentCount() {
        return jumlahPenilaian;
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

    public boolean hasScores() {
        return jumlahPenilaian > 0 && persentaseKeseluruhan != null;
    }
}
