package id.ac.sekolah.manajemenkelas.student.model;

import java.time.LocalDate;

public class Student {

    private Long id;
    private String nis;
    private String nisn;
    private String namaLengkap;
    private String jenisKelamin;
    private LocalDate tanggalLahir;
    private String tempatLahir;
    private String alamat;
    private String namaOrangTua;
    private String teleponOrangTua;
    private boolean aktif;

    public Student(
            Long id,
            String nis,
            String nisn,
            String namaLengkap,
            String jenisKelamin,
            LocalDate tanggalLahir,
            String tempatLahir,
            String alamat,
            String namaOrangTua,
            String teleponOrangTua,
            boolean aktif) {
        this.id = id;
        this.nis = nis;
        this.nisn = nisn;
        this.namaLengkap = namaLengkap;
        this.jenisKelamin = jenisKelamin;
        this.tanggalLahir = tanggalLahir;
        this.tempatLahir = tempatLahir;
        this.alamat = alamat;
        this.namaOrangTua = namaOrangTua;
        this.teleponOrangTua = teleponOrangTua;
        this.aktif = aktif;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNis() {
        return nis;
    }

    public String getNisn() {
        return nisn;
    }

    public String getFullName() {
        return namaLengkap;
    }

    public String getGender() {
        return jenisKelamin;
    }

    public LocalDate getBirthDate() {
        return tanggalLahir;
    }

    public String getBirthPlace() {
        return tempatLahir;
    }

    public String getAddress() {
        return alamat;
    }

    public String getParentName() {
        return namaOrangTua;
    }

    public String getParentPhone() {
        return teleponOrangTua;
    }

    public boolean isActive() {
        return aktif;
    }

    public void setNis(String nis) {
        this.nis = nis;
    }

    public void setNisn(String nisn) {
        this.nisn = nisn;
    }

    public void setFullName(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public void setGender(String jenisKelamin) {
        this.jenisKelamin = jenisKelamin;
    }

    public void setBirthDate(LocalDate tanggalLahir) {
        this.tanggalLahir = tanggalLahir;
    }

    public void setBirthPlace(String tempatLahir) {
        this.tempatLahir = tempatLahir;
    }

    public void setAddress(String alamat) {
        this.alamat = alamat;
    }

    public void setParentName(String namaOrangTua) {
        this.namaOrangTua = namaOrangTua;
    }

    public void setParentPhone(String teleponOrangTua) {
        this.teleponOrangTua = teleponOrangTua;
    }

    public void setActive(boolean aktif) {
        this.aktif = aktif;
    }
}
