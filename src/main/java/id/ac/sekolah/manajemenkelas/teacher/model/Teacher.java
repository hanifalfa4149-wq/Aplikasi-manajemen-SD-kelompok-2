package id.ac.sekolah.manajemenkelas.teacher.model;

import java.time.LocalDate;

public class Teacher {

    private Long id;
    private String nip;
    private String fullName;
    private String gender;
    private String email;
    private String phone;
    private String address;
    private String education;
    private LocalDate joinDate;
    private boolean active;

    public Teacher(
            Long id,
            String nip,
            String fullName,
            String gender,
            String email,
            String phone,
            String address,
            String education,
            LocalDate joinDate,
            boolean active
    ) {
        this.id = id;
        this.nip = nip;
        this.fullName = fullName;
        this.gender = gender;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.education = education;
        this.joinDate = joinDate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getNip() {
        return nip;
    }

    public String getFullName() {
        return fullName;
    }

    public String getGender() {
        return gender;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public String getEducation() {
        return education;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}