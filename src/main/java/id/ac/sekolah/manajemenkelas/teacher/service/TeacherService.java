package id.ac.sekolah.manajemenkelas.teacher.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.teacher.dao.TeacherDAO;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;

public class TeacherService {

    private final TeacherDAO daoGuru;

    public TeacherService(TeacherDAO daoGuru) {
        this.daoGuru = daoGuru;
    }

    public Teacher createTeacher(
            String nip,
            String namaLengkap,
            String jenisKelamin,
            String email,
            String nomorTelepon,
            String alamat,
            String pendidikan,
            LocalDate tanggalBergabung) {

        SessionContext.getInstance().requireAdminIfAuthenticated("guru");

        validasiDataGuru(
                nip, namaLengkap, jenisKelamin,
                email, alamat, pendidikan, tanggalBergabung);

        if (daoGuru.existsByNip(nip.trim())) {
            throw new IllegalArgumentException("NIP sudah terdaftar.");
        }

        String emailNormal = email == null ? "" : email.trim();
        if (!emailNormal.isEmpty()) {
            List<Teacher> semuaGuru = daoGuru.findAll();
            for (Teacher guru : semuaGuru) {
                if (guru.getEmail() != null && guru.getEmail().equalsIgnoreCase(emailNormal)) {
                    throw new IllegalArgumentException("Email sudah terdaftar.");
                }
            }
        }

        Teacher guru = new Teacher(
                null,
                nip.trim(),
                namaLengkap.trim(),
                jenisKelamin.trim().toUpperCase(),
                emailNormal,
                nomorTelepon == null ? "" : nomorTelepon.trim(),
                alamat.trim(),
                pendidikan.trim(),
                tanggalBergabung,
                true);

        daoGuru.save(guru);

        return guru;
    }

    public List<Teacher> getAllTeachers() {
        return daoGuru.findAll();
    }

    public Optional<Teacher> getTeacherById(Long id) {
        return daoGuru.findById(id);
    }

    public Teacher updateTeacher(
            Long id,
            String nip,
            String namaLengkap,
            String jenisKelamin,
            String email,
            String nomorTelepon,
            String alamat,
            String pendidikan,
            LocalDate tanggalBergabung) {

        SessionContext.getInstance().requireAdminIfAuthenticated("guru");

        Optional<Teacher> hasilCari = daoGuru.findById(id);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data guru tidak ditemukan.");
        }
        Teacher guruLama = hasilCari.get();

        validasiDataGuru(
                nip, namaLengkap, jenisKelamin,
                email, alamat, pendidikan, tanggalBergabung);

        Optional<Teacher> pemilikNip = daoGuru.findByNip(nip.trim());

        if (pemilikNip.isPresent()
                && !pemilikNip.get().getId().equals(id)) {
            throw new IllegalArgumentException("NIP sudah digunakan guru lain.");
        }

        String emailNormal = email == null ? "" : email.trim();
        if (!emailNormal.isEmpty()) {
            List<Teacher> semuaGuru = daoGuru.findAll();
            for (Teacher guru : semuaGuru) {
                if (!guru.getId().equals(id)
                        && guru.getEmail() != null
                        && guru.getEmail().equalsIgnoreCase(emailNormal)) {
                    throw new IllegalArgumentException("Email sudah digunakan guru lain.");
                }
            }
        }

        guruLama.setNip(nip.trim());
        guruLama.setFullName(namaLengkap.trim());
        guruLama.setGender(jenisKelamin.trim().toUpperCase());
        guruLama.setEmail(emailNormal);
        guruLama.setPhone(nomorTelepon == null ? "" : nomorTelepon.trim());
        guruLama.setAddress(alamat.trim());
        guruLama.setEducation(pendidikan.trim());
        guruLama.setJoinDate(tanggalBergabung);

        daoGuru.update(guruLama);

        return guruLama;
    }

    public void deleteTeacher(Long id) {

        SessionContext.getInstance().requireAdminIfAuthenticated("guru");

        Optional<Teacher> hasilCari = daoGuru.findById(id);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data guru tidak ditemukan.");
        }
        Teacher guru = hasilCari.get();

        guru.setActive(false);
        daoGuru.update(guru);
    }

    public List<Teacher> searchTeachers(String kataKunci) {

        if (kataKunci == null || kataKunci.isBlank()) {
            return getAllTeachers();
        }

        String kataKunciKecil = kataKunci.trim().toLowerCase();
        List<Teacher> semuaGuru = daoGuru.findAll();
        List<Teacher> hasilPencarian = new ArrayList<>();

        for (Teacher guru : semuaGuru) {
            boolean cocokNama = guru.getFullName() != null
                    && guru.getFullName().toLowerCase().contains(kataKunciKecil);
            boolean cocokNip = guru.getNip() != null
                    && guru.getNip().toLowerCase().contains(kataKunciKecil);
            boolean cocokEmail = guru.getEmail() != null
                    && guru.getEmail().toLowerCase().contains(kataKunciKecil);

            if (cocokNama || cocokNip || cocokEmail) {
                hasilPencarian.add(guru);
            }
        }

        return Collections.unmodifiableList(hasilPencarian);
    }

    private void validasiDataGuru(
            String nip,
            String namaLengkap,
            String jenisKelamin,
            String email,
            String alamat,
            String pendidikan,
            LocalDate tanggalBergabung) {

        if (nip == null || nip.isBlank()) {
            throw new IllegalArgumentException("NIP wajib diisi.");
        }

        if (namaLengkap == null || namaLengkap.isBlank()) {
            throw new IllegalArgumentException("Nama guru wajib diisi.");
        }

        if (jenisKelamin == null
                || (!jenisKelamin.equalsIgnoreCase("L")
                        && !jenisKelamin.equalsIgnoreCase("P"))) {
            throw new IllegalArgumentException("Jenis kelamin harus L atau P.");
        }

        if (email != null && !email.isBlank()
                && !email.matches("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("Format email tidak valid.");
        }

        if (alamat == null || alamat.isBlank()) {
            throw new IllegalArgumentException("Alamat wajib diisi.");
        }

        if (pendidikan == null || pendidikan.isBlank()) {
            throw new IllegalArgumentException("Pendidikan terakhir wajib diisi.");
        }

        if (tanggalBergabung == null || tanggalBergabung.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Tanggal bergabung tidak valid.");
        }
    }
}
