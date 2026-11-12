package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;

public class ClassAttendanceReport {

    private final Long idKelas;
    private final String namaKelas;
    private final int tingkatKelas;
    private final int totalCatatan;
    private final int jumlahHadir;
    private final int jumlahSakit;
    private final int jumlahIzin;
    private final int jumlahAlpa;
    private final BigDecimal persentaseKehadiran;

    public ClassAttendanceReport(
            Long idKelas,
            String namaKelas,
            int tingkatKelas,
            int totalCatatan,
            int jumlahHadir,
            int jumlahSakit,
            int jumlahIzin,
            int jumlahAlpa,
            BigDecimal persentaseKehadiran) {
        this.idKelas = idKelas;
        this.namaKelas = namaKelas;
        this.tingkatKelas = tingkatKelas;
        this.totalCatatan = totalCatatan;
        this.jumlahHadir = jumlahHadir;
        this.jumlahSakit = jumlahSakit;
        this.jumlahIzin = jumlahIzin;
        this.jumlahAlpa = jumlahAlpa;
        this.persentaseKehadiran = persentaseKehadiran;
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

    public int getTotalRecords() {
        return totalCatatan;
    }

    public int getHadirCount() {
        return jumlahHadir;
    }

    public int getSakitCount() {
        return jumlahSakit;
    }

    public int getIzinCount() {
        return jumlahIzin;
    }

    public int getAlpaCount() {
        return jumlahAlpa;
    }

    public BigDecimal getAttendancePercentage() {
        return persentaseKehadiran;
    }

    public boolean hasSufficientData() {
        return totalCatatan > 0 && persentaseKehadiran != null;
    }
}
