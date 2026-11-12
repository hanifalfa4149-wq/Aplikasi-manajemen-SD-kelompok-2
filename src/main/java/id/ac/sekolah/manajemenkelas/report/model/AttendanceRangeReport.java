package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AttendanceRangeReport {

    private final LocalDate tanggalMulai;
    private final LocalDate tanggalSelesai;
    private final int totalCatatan;
    private final int jumlahHadir;
    private final int jumlahSakit;
    private final int jumlahIzin;
    private final int jumlahAlpa;
    private final BigDecimal persentaseKehadiran;

    public AttendanceRangeReport(
            LocalDate tanggalMulai,
            LocalDate tanggalSelesai,
            int totalCatatan,
            int jumlahHadir,
            int jumlahSakit,
            int jumlahIzin,
            int jumlahAlpa,
            BigDecimal persentaseKehadiran) {
        this.tanggalMulai = tanggalMulai;
        this.tanggalSelesai = tanggalSelesai;
        this.totalCatatan = totalCatatan;
        this.jumlahHadir = jumlahHadir;
        this.jumlahSakit = jumlahSakit;
        this.jumlahIzin = jumlahIzin;
        this.jumlahAlpa = jumlahAlpa;
        this.persentaseKehadiran = persentaseKehadiran;
    }

    public LocalDate getStartDate() {
        return tanggalMulai;
    }

    public LocalDate getEndDate() {
        return tanggalSelesai;
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
