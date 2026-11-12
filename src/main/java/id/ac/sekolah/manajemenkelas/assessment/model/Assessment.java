package id.ac.sekolah.manajemenkelas.assessment.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class Assessment {

    public static final String TYPE_KUIS = "KUIS";
    public static final String TYPE_TUGAS = "TUGAS";
    public static final String TYPE_UTS = "UTS";
    public static final String TYPE_UAS = "UAS";
    public static final String TYPE_PRAKTIK = "PRAKTIK";
    public static final String TYPE_PROYEK = "PROYEK";
    public static final String TYPE_LAINNYA = "LAINNYA";

    private Long id;
    private Long idPenugasanMengajar;
    private String judul;
    private String jenisPenilaian;
    private LocalDate tanggalPenilaian;
    private BigDecimal nilaiMaksimal;
    private String deskripsi;

    public Assessment(
            Long id,
            Long idPenugasanMengajar,
            String judul,
            String jenisPenilaian,
            LocalDate tanggalPenilaian,
            BigDecimal nilaiMaksimal,
            String deskripsi) {
        this.id = id;
        this.idPenugasanMengajar = idPenugasanMengajar;
        this.judul = judul;
        this.jenisPenilaian = jenisPenilaian;
        this.tanggalPenilaian = tanggalPenilaian;
        this.nilaiMaksimal = nilaiMaksimal;
        this.deskripsi = deskripsi;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeachingAssignmentId() {
        return idPenugasanMengajar;
    }

    public void setTeachingAssignmentId(Long idPenugasanMengajar) {
        this.idPenugasanMengajar = idPenugasanMengajar;
    }

    public String getTitle() {
        return judul;
    }

    public void setTitle(String judul) {
        this.judul = judul;
    }

    public String getAssessmentType() {
        return jenisPenilaian;
    }

    public void setAssessmentType(String jenisPenilaian) {
        this.jenisPenilaian = jenisPenilaian;
    }

    public LocalDate getAssessmentDate() {
        return tanggalPenilaian;
    }

    public void setAssessmentDate(LocalDate tanggalPenilaian) {
        this.tanggalPenilaian = tanggalPenilaian;
    }

    public BigDecimal getMaxScore() {
        return nilaiMaksimal;
    }

    public void setMaxScore(BigDecimal nilaiMaksimal) {
        this.nilaiMaksimal = nilaiMaksimal;
    }

    public String getDescription() {
        return deskripsi;
    }

    public void setDescription(String deskripsi) {
        this.deskripsi = deskripsi;
    }
}
