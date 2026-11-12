package id.ac.sekolah.manajemenkelas.teacher.model;

import java.time.LocalDate;

public class Teacher {

    private Long id;
    private String nip;
    private String namaLengkap;
    private String jenisKelamin;
    private String email;
    private String nomorTelepon;
    private String alamat;
    private String pendidikan;
    private LocalDate tanggalBergabung;
    private boolean aktif;

    public Teacher(
            Long id,
            String nip,
            String namaLengkap,
            String jenisKelamin,
            String email,
            String nomorTelepon,
            String alamat,
            String pendidikan,
            LocalDate tanggalBergabung,
            boolean aktif) {
        this.id = id;
        this.nip = nip;
        this.namaLengkap = namaLengkap;
        this.jenisKelamin = jenisKelamin;
        this.email = email;
        this.nomorTelepon = nomorTelepon;
        this.alamat = alamat;
        this.pendidikan = pendidikan;
        this.tanggalBergabung = tanggalBergabung;
        this.aktif = aktif;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNip() {
        return nip;
    }

    public String getFullName() {
        return namaLengkap;
    }

    public String getGender() {
        return jenisKelamin;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return nomorTelepon;
    }

    public String getAddress() {
        return alamat;
    }

    public String getEducation() {
        return pendidikan;
    }

    public LocalDate getJoinDate() {
        return tanggalBergabung;
    }

    public boolean isActive() {
        return aktif;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public void setFullName(String namaLengkap) {
        this.namaLengkap = namaLengkap;
    }

    public void setGender(String jenisKelamin) {
        this.jenisKelamin = jenisKelamin;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String nomorTelepon) {
        this.nomorTelepon = nomorTelepon;
    }

    public void setAddress(String alamat) {
        this.alamat = alamat;
    }

    public void setEducation(String pendidikan) {
        this.pendidikan = pendidikan;
    }

    public void setJoinDate(LocalDate tanggalBergabung) {
        this.tanggalBergabung = tanggalBergabung;
    }

    public void setActive(boolean aktif) {
        this.aktif = aktif;
    }
}
