package id.ac.sekolah.manajemenkelas.report.model;

public class ClassStudentCountReport {

    private final Long idKelas;
    private final String namaKelas;
    private final int tingkatKelas;
    private final String namaTahunAjaran;
    private final int kapasitas;
    private final int jumlahSiswaAktif;

    public ClassStudentCountReport(
            Long idKelas,
            String namaKelas,
            int tingkatKelas,
            String namaTahunAjaran,
            int kapasitas,
            int jumlahSiswaAktif) {
        this.idKelas = idKelas;
        this.namaKelas = namaKelas;
        this.tingkatKelas = tingkatKelas;
        this.namaTahunAjaran = namaTahunAjaran;
        this.kapasitas = kapasitas;
        this.jumlahSiswaAktif = jumlahSiswaAktif;
    }

    public Long getClassId() {
        return idKelas;
    }

    public String getClassName() {
        return namaKelas;
    }

    public int getGradeLevel() {
        return tingkatKelas;
    }

    public String getAcademicYearName() {
        return namaTahunAjaran;
    }

    public int getCapacity() {
        return kapasitas;
    }

    public int getActiveStudentCount() {
        return jumlahSiswaAktif;
    }
}
