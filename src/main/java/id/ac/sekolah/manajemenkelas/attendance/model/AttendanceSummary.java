package id.ac.sekolah.manajemenkelas.attendance.model;

import java.time.LocalDate;

public class AttendanceSummary {

    private final Long classId;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final int totalRecords;
    private final int hadirCount;
    private final int sakitCount;
    private final int izinCount;
    private final int alpaCount;

    public AttendanceSummary(
            Long classId,
            LocalDate startDate,
            LocalDate endDate,
            int totalRecords,
            int hadirCount,
            int sakitCount,
            int izinCount,
            int alpaCount) {
        this.classId = classId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRecords = totalRecords;
        this.hadirCount = hadirCount;
        this.sakitCount = sakitCount;
        this.izinCount = izinCount;
        this.alpaCount = alpaCount;
    }

    public Long getClassId() {
        return classId;
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
}

