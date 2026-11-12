package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;

public class StudentScore {

    private Long id;
    private Long idPenilaian;
    private Long idSiswa;
    private BigDecimal nilai;
    private String keterangan;
    private Long dicatatOleh;

    public StudentScore(
            Long id,
            Long idPenilaian,
            Long idSiswa,
            BigDecimal nilai,
            String keterangan,
            Long dicatatOleh) {
        this.id = id;
        this.idPenilaian = idPenilaian;
        this.idSiswa = idSiswa;
        this.nilai = nilai;
        this.keterangan = keterangan;
        this.dicatatOleh = dicatatOleh;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAssessmentId() {
        return idPenilaian;
    }

    public void setAssessmentId(Long idPenilaian) {
        this.idPenilaian = idPenilaian;
    }

    public Long getStudentId() {
        return idSiswa;
    }

    public void setStudentId(Long idSiswa) {
        this.idSiswa = idSiswa;
    }

    public BigDecimal getScore() {
        return nilai;
    }

    public void setScore(BigDecimal nilai) {
        this.nilai = nilai;
    }

    public String getNotes() {
        return keterangan;
    }

    public void setNotes(String keterangan) {
        this.keterangan = keterangan;
    }

    public Long getRecordedBy() {
        return dicatatOleh;
    }

    public void setRecordedBy(Long dicatatOleh) {
        this.dicatatOleh = dicatatOleh;
    }
}
