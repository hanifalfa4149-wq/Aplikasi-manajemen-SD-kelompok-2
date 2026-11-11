package id.ac.sekolah.manajemenkelas.report.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AttendanceRangeReport {

    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int totalRecords;
    private final int hadirCount;
    private final int sakitCount;
    private final int izinCount;
    private final int alpaCount;
    private final BigDecimal attendancePercentage;

    public AttendanceRangeReport(
            LocalDate startDate,
            LocalDate endDate,
            int totalRecords,
            int hadirCount,
            int sakitCount,
            int izinCount,
            int alpaCount,
            BigDecimal attendancePercentage) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRecords = totalRecords;
        this.hadirCount = hadirCount;
        this.sakitCount = sakitCount;
        this.izinCount = izinCount;
        this.alpaCount = alpaCount;
        this.attendancePercentage = attendancePercentage;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public int getHadirCount() {
        return hadirCount;
    }

    public int getSakitCount() {
        return sakitCount;
    }

    public int getIzinCount() {
        return izinCount;
    }

    public int getAlpaCount() {
        return alpaCount;
    }

    public BigDecimal getAttendancePercentage() {
        return attendancePercentage;
    }

    public boolean hasSufficientData() {
        return totalRecords > 0 && attendancePercentage != null;
    }
}

