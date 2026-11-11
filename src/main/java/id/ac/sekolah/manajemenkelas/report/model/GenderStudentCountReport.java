package id.ac.sekolah.manajemenkelas.report.model;

public class GenderStudentCountReport {

    private final String genderCode;
    private final String genderLabel;
    private final int activeStudentCount;

    public GenderStudentCountReport(String genderCode, String genderLabel, int activeStudentCount) {
        this.genderCode = genderCode;
        this.genderLabel = genderLabel;
        this.activeStudentCount = activeStudentCount;
    }

    public String getGenderCode() {
        return genderCode;
    }

    public String getGenderLabel() {
        return genderLabel;
    }

    public int getActiveStudentCount() {
        return activeStudentCount;
    }
}

