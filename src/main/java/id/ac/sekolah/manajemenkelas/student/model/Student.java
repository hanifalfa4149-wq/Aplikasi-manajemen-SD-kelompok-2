package id.ac.sekolah.manajemenkelas.student.model;

import java.time.LocalDate;

public class Student {

    private Long id;
    private String nis;
    private String nisn;
    private String fullName;
    private String gender;
    private LocalDate birthDate;
    private String birthPlace;
    private String address;
    private String parentName;
    private String parentPhone;
    private boolean active;

    public Student(
            Long id,
            String nis,
            String nisn,
            String fullName,
            String gender,
            LocalDate birthDate,
            String birthPlace,
            String address,
            String parentName,
            String parentPhone,
            boolean active) {
        this.id = id;
        this.nis = nis;
        this.nisn = nisn;
        this.fullName = fullName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.birthPlace = birthPlace;
        this.address = address;
        this.parentName = parentName;
        this.parentPhone = parentPhone;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getNis() {
        return nis;
    }

    public String getNisn() {
        return nisn;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getBirthPlace() {
        return birthPlace;
    }

    public String getAddress() {
        return address;
    }

    public String getParentName() {
        return parentName;
    }

    public String getParentPhone() {
        return parentPhone;
    }

    public boolean isActive() {
        return active;
    }

    public void setNis(String nis) {
        this.nis = nis;
    }

    public void setNisn(String nisn) {
        this.nisn = nisn;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void setParentPhone(String parentPhone) {
        this.parentPhone = parentPhone;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
