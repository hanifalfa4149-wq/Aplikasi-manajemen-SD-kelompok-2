package id.ac.sekolah.manajemenkelas.student.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;

public class StudentService {

    private final StudentDAO daoSiswa;

    public StudentService(StudentDAO daoSiswa) {
        this.daoSiswa = daoSiswa;
    }

    public Student createStudent(
            String nis,
            String nisn,
            String namaLengkap,
            String jenisKelamin,
            LocalDate tanggalLahir,
            String tempatLahir,
            String alamat,
            String namaOrangTua,
            String teleponOrangTua) {

        SessionContext.getInstance().requireAdminIfAuthenticated("siswa");

        validasiDataSiswa(
                nis, nisn, namaLengkap, jenisKelamin,
                tanggalLahir, tempatLahir, alamat);

        if (daoSiswa.existsByNis(nis.trim())) {
            throw new IllegalArgumentException("NIS sudah terdaftar.");
        }

        if (daoSiswa.existsByNisn(nisn.trim())) {
            throw new IllegalArgumentException("NISN sudah terdaftar.");
        }

        Student siswa = new Student(
                null,
                nis.trim(),
                nisn.trim(),
                namaLengkap.trim(),
                jenisKelamin.trim().toUpperCase(),
                tanggalLahir,
                tempatLahir.trim(),
                alamat.trim(),
                namaOrangTua == null ? "" : namaOrangTua.trim(),
                teleponOrangTua == null ? "" : teleponOrangTua.trim(),
                true);

        daoSiswa.save(siswa);

        return siswa;
    }

    public List<Student> getAllStudents() {
        return daoSiswa.findAll();
    }

    public Optional<Student> getStudentById(Long id) {
        return daoSiswa.findById(id);
    }

    public Student updateStudent(
            Long id,
            String nis,
            String nisn,
            String namaLengkap,
            String jenisKelamin,
            LocalDate tanggalLahir,
            String tempatLahir,
            String alamat,
            String namaOrangTua,
            String teleponOrangTua) {

        SessionContext.getInstance().requireAdminIfAuthenticated("siswa");

        Optional<Student> hasilCari = daoSiswa.findById(id);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data siswa tidak ditemukan.");
        }
        Student siswaLama = hasilCari.get();

        validasiDataSiswa(
                nis, nisn, namaLengkap, jenisKelamin,
                tanggalLahir, tempatLahir, alamat);

        Optional<Student> pemilikNis = daoSiswa.findByNis(nis.trim());

        if (pemilikNis.isPresent()
                && !pemilikNis.get().getId().equals(id)) {
            throw new IllegalArgumentException("NIS sudah digunakan siswa lain.");
        }

        Optional<Student> pemilikNisn = daoSiswa.findByNisn(nisn.trim());

        if (pemilikNisn.isPresent()
                && !pemilikNisn.get().getId().equals(id)) {
            throw new IllegalArgumentException("NISN sudah digunakan siswa lain.");
        }

        siswaLama.setNis(nis.trim());
        siswaLama.setNisn(nisn.trim());
        siswaLama.setFullName(namaLengkap.trim());
        siswaLama.setGender(jenisKelamin.trim().toUpperCase());
        siswaLama.setBirthDate(tanggalLahir);
        siswaLama.setBirthPlace(tempatLahir.trim());
        siswaLama.setAddress(alamat.trim());
        siswaLama.setParentName(namaOrangTua == null ? "" : namaOrangTua.trim());
        siswaLama.setParentPhone(teleponOrangTua == null ? "" : teleponOrangTua.trim());

        daoSiswa.update(siswaLama);

        return siswaLama;
    }

    public void deleteStudent(Long id) {

        SessionContext.getInstance().requireAdminIfAuthenticated("siswa");

        Optional<Student> hasilCari = daoSiswa.findById(id);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data siswa tidak ditemukan.");
        }
        Student siswa = hasilCari.get();

        siswa.setActive(false);
        daoSiswa.update(siswa);
    }

    public List<Student> searchStudents(String kataKunci) {

        if (kataKunci == null || kataKunci.isBlank()) {
            return getAllStudents();
        }

        String kataKunciKecil = kataKunci.trim().toLowerCase();
        List<Student> semuaSiswa = daoSiswa.findAll();
        List<Student> hasilPencarian = new ArrayList<>();

        for (Student siswa : semuaSiswa) {
            boolean cocokNama = siswa.getFullName() != null
                    && siswa.getFullName().toLowerCase().contains(kataKunciKecil);
            boolean cocokNis = siswa.getNis() != null
                    && siswa.getNis().toLowerCase().contains(kataKunciKecil);
            boolean cocokNisn = siswa.getNisn() != null
                    && siswa.getNisn().toLowerCase().contains(kataKunciKecil);

            if (cocokNama || cocokNis || cocokNisn) {
                hasilPencarian.add(siswa);
            }
        }

        return Collections.unmodifiableList(hasilPencarian);
    }

    private void validasiDataSiswa(
            String nis,
            String nisn,
            String namaLengkap,
            String jenisKelamin,
            LocalDate tanggalLahir,
            String tempatLahir,
            String alamat) {

        if (nis == null || nis.isBlank()) {
            throw new IllegalArgumentException("NIS wajib diisi.");
        }

        if (nisn == null || nisn.isBlank()) {
            throw new IllegalArgumentException("NISN wajib diisi.");
        }

        if (namaLengkap == null || namaLengkap.isBlank()) {
            throw new IllegalArgumentException("Nama siswa wajib diisi.");
        }

        if (jenisKelamin == null
                || (!jenisKelamin.equalsIgnoreCase("L")
                        && !jenisKelamin.equalsIgnoreCase("P"))) {
            throw new IllegalArgumentException("Jenis kelamin harus L atau P.");
        }

        if (tanggalLahir == null || tanggalLahir.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Tanggal lahir tidak valid.");
        }

        if (tempatLahir == null || tempatLahir.isBlank()) {
            throw new IllegalArgumentException("Tempat lahir wajib diisi.");
        }

        if (alamat == null || alamat.isBlank()) {
            throw new IllegalArgumentException("Alamat wajib diisi.");
        }
    }
}
