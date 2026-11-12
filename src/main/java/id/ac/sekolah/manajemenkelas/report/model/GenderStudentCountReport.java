package id.ac.sekolah.manajemenkelas.report.model;

public class GenderStudentCountReport {

    private final String kodeJenisKelamin;
    private final String labelJenisKelamin;
    private final int jumlahSiswaAktif;

    public GenderStudentCountReport(String kodeJenisKelamin, String labelJenisKelamin, int jumlahSiswaAktif) {
        this.kodeJenisKelamin = kodeJenisKelamin;
        this.labelJenisKelamin = labelJenisKelamin;
        this.jumlahSiswaAktif = jumlahSiswaAktif;
    }

    public String getGenderCode() {
        return kodeJenisKelamin;
    }

    public String getGenderLabel() {
        return labelJenisKelamin;
    }

    public int getActiveStudentCount() {
        return jumlahSiswaAktif;
    }
}
