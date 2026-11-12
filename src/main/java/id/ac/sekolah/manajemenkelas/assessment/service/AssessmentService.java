package id.ac.sekolah.manajemenkelas.assessment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.dao.AcademicDAO;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.assessment.dao.AssessmentDAO;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.AssessmentSummary;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScoreSummary;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.student.dao.StudentDAO;
import id.ac.sekolah.manajemenkelas.student.model.Student;

public class AssessmentService {

    public static final class StudentScoreEntry {

        private final Long idSiswa;
        private final BigDecimal nilai;
        private final String keterangan;

        public StudentScoreEntry(Long idSiswa, BigDecimal nilai, String keterangan) {
            this.idSiswa = idSiswa;
            this.nilai = nilai;
            this.keterangan = keterangan;
        }

        public Long getStudentId() {
            return idSiswa;
        }

        public BigDecimal getScore() {
            return nilai;
        }

        public String getNotes() {
            return keterangan;
        }

        public Long studentId() {
            return idSiswa;
        }

        public BigDecimal score() {
            return nilai;
        }

        public String notes() {
            return keterangan;
        }

        @Override
        public boolean equals(Object objekLain) {
            if (this == objekLain) {
                return true;
            }
            if (!(objekLain instanceof StudentScoreEntry)) {
                return false;
            }
            StudentScoreEntry entriLain = (StudentScoreEntry) objekLain;
            return Objects.equals(idSiswa, entriLain.idSiswa)
                    && Objects.equals(nilai, entriLain.nilai)
                    && Objects.equals(keterangan, entriLain.keterangan);
        }

        @Override
        public int hashCode() {
            return Objects.hash(idSiswa, nilai, keterangan);
        }
    }

    private static final BigDecimal BATAS_MAKSIMAL_DESIMAL = new BigDecimal("99999.99");
    private static final BigDecimal SERATUS = new BigDecimal("100");
    private static final Set<String> DAFTAR_JENIS_VALID = Set.of(
            Assessment.TYPE_KUIS,
            Assessment.TYPE_TUGAS,
            Assessment.TYPE_UTS,
            Assessment.TYPE_UAS,
            Assessment.TYPE_PRAKTIK,
            Assessment.TYPE_PROYEK,
            Assessment.TYPE_LAINNYA);

    private final AssessmentDAO daoPenilaian;
    private final AcademicDAO daoAkademik;
    private final StudentDAO daoSiswa;

    public AssessmentService(
            AssessmentDAO daoPenilaian,
            AcademicDAO daoAkademik,
            StudentDAO daoSiswa) {
        if (daoPenilaian == null || daoAkademik == null || daoSiswa == null) {
            throw new IllegalArgumentException("DAO dependensi AssessmentService tidak boleh null.");
        }
        this.daoPenilaian = daoPenilaian;
        this.daoAkademik = daoAkademik;
        this.daoSiswa = daoSiswa;
    }

    // ==========================================
    // OPERASI PENILAIAN (ASSESSMENT)
    // ==========================================

    public Assessment createAssessment(
            Long idPenugasanMengajar,
            String judul,
            String jenisPenilaian,
            LocalDate tanggalPenilaian,
            BigDecimal nilaiMaksimal,
            String deskripsi) {

        TeachingAssignment penugasanMengajar = wajibkanOtorisasiTulisPenugasanMengajar(idPenugasanMengajar);
        String judulValid = validasiJudul(judul);
        String jenisValid = normalisasiDanValidasiJenis(jenisPenilaian);
        validasiTanggalPenilaian(tanggalPenilaian, penugasanMengajar);
        BigDecimal nilaiMaksimalValid = validasiDanNormalisasiNilaiMaksimal(nilaiMaksimal);

        Assessment penilaian = new Assessment(
                null,
                idPenugasanMengajar,
                judulValid,
                jenisValid,
                tanggalPenilaian,
                nilaiMaksimalValid,
                deskripsi == null ? "" : deskripsi.trim());

        daoPenilaian.saveAssessment(penilaian);
        return penilaian;
    }

    public Assessment updateAssessment(
            Long idPenilaian,
            String judul,
            String jenisPenilaian,
            LocalDate tanggalPenilaian,
            BigDecimal nilaiMaksimal,
            String deskripsi) {

        if (idPenilaian == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi.");
        }

        Optional<Assessment> hasilCari = daoPenilaian.findAssessmentById(idPenilaian);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data penilaian tidak ditemukan.");
        }
        Assessment penilaianLama = hasilCari.get();

        TeachingAssignment penugasanMengajar = wajibkanOtorisasiTulisPenugasanMengajar(
                penilaianLama.getTeachingAssignmentId());
        String judulValid = validasiJudul(judul);
        String jenisValid = normalisasiDanValidasiJenis(jenisPenilaian);
        validasiTanggalPenilaian(tanggalPenilaian, penugasanMengajar);
        BigDecimal nilaiMaksimalValid = validasiDanNormalisasiNilaiMaksimal(nilaiMaksimal);

        List<StudentScore> daftarNilaiSaatIni = daoPenilaian.findScoresByAssessmentId(idPenilaian);
        for (StudentScore nilaiSiswa : daftarNilaiSaatIni) {
            if (nilaiSiswa.getScore().compareTo(nilaiMaksimalValid) > 0) {
                throw new IllegalArgumentException(
                        "Nilai maksimal baru (" + nilaiMaksimalValid
                                + ") lebih kecil dari nilai siswa yang sudah ada ("
                                + nilaiSiswa.getScore() + ").");
            }
        }

        penilaianLama.setTitle(judulValid);
        penilaianLama.setAssessmentType(jenisValid);
        penilaianLama.setAssessmentDate(tanggalPenilaian);
        penilaianLama.setMaxScore(nilaiMaksimalValid);
        penilaianLama.setDescription(deskripsi == null ? "" : deskripsi.trim());

        daoPenilaian.updateAssessment(penilaianLama);
        return penilaianLama;
    }

    public Optional<Assessment> getAssessmentById(Long id) {
        Optional<Assessment> hasilCari = daoPenilaian.findAssessmentById(id);
        if (hasilCari.isPresent()) {
            wajibkanOtorisasiBacaPenugasanMengajar(hasilCari.get().getTeachingAssignmentId());
        }
        return hasilCari;
    }

    public List<Assessment> getAllAssessments() {
        wajibkanSudahLogin();
        SessionContext konteksSesi = SessionContext.getInstance();
        List<Assessment> semuaPenilaian = daoPenilaian.findAllAssessments();
        if (konteksSesi.hasRole(RoleMapper.ROLE_ADMIN)) {
            return semuaPenilaian;
        }

        Set<Long> kumpulanIdPenugasanGuru = ambilIdPenugasanMengajarTerotorisasi(
                wajibkanIdGuruUntukPeranGuru(konteksSesi));
        List<Assessment> hasilFilter = new ArrayList<>();
        for (Assessment penilaian : semuaPenilaian) {
            if (kumpulanIdPenugasanGuru.contains(penilaian.getTeachingAssignmentId())) {
                hasilFilter.add(penilaian);
            }
        }
        return Collections.unmodifiableList(hasilFilter);
    }

    public List<Assessment> getAssessmentsByTeachingAssignment(Long idPenugasanMengajar) {
        wajibkanOtorisasiBacaPenugasanMengajar(idPenugasanMengajar);
        return daoPenilaian.findAssessmentsByTeachingAssignmentId(idPenugasanMengajar);
    }

    // ==========================================
    // OPERASI NILAI SISWA (STUDENT SCORE)
    // ==========================================

    public StudentScore recordScore(
            Long idPenilaian,
            Long idSiswa,
            BigDecimal nilai,
            String keterangan) {

        Assessment penilaian = ambilPenilaianTerotorisasiUntukTulis(idPenilaian);
        TeachingAssignment penugasanMengajar = wajibkanOtorisasiTulisPenugasanMengajar(
                penilaian.getTeachingAssignmentId());
        BigDecimal nilaiValid = validasiDanNormalisasiNilaiSiswa(nilai, penilaian.getMaxScore());
        validasiSiswaDiKelasPadaTanggal(idSiswa, penugasanMengajar.getClassId(), penilaian.getAssessmentDate());

        if (daoPenilaian.findScoreByAssessmentAndStudent(idPenilaian, idSiswa).isPresent()) {
            throw new IllegalArgumentException(
                    "Nilai siswa ID " + idSiswa + " untuk penilaian ini sudah tercatat.");
        }

        StudentScore nilaiSiswa = new StudentScore(
                null,
                idPenilaian,
                idSiswa,
                nilaiValid,
                keterangan == null ? "" : keterangan.trim(),
                SessionContext.getInstance().getUserId());

        daoPenilaian.saveScore(nilaiSiswa);
        return nilaiSiswa;
    }

    public List<StudentScore> recordAssessmentScores(
            Long idPenilaian,
            List<StudentScoreEntry> daftarEntri) {

        Assessment penilaian = ambilPenilaianTerotorisasiUntukTulis(idPenilaian);
        TeachingAssignment penugasanMengajar = wajibkanOtorisasiTulisPenugasanMengajar(
                penilaian.getTeachingAssignmentId());

        if (daftarEntri == null || daftarEntri.isEmpty()) {
            throw new IllegalArgumentException("Daftar nilai siswa tidak boleh kosong.");
        }

        Long dicatatOleh = SessionContext.getInstance().getUserId();
        Set<Long> siswaYangSudahTerlihat = new HashSet<>();
        List<StudentScore> daftarYangAkanDisimpan = new ArrayList<>();

        for (StudentScoreEntry entri : daftarEntri) {
            if (entri == null || entri.getStudentId() == null) {
                throw new IllegalArgumentException("ID siswa wajib diisi.");
            }
            if (!siswaYangSudahTerlihat.add(entri.getStudentId())) {
                throw new IllegalArgumentException(
                        "Siswa dengan ID " + entri.getStudentId() + " muncul lebih dari sekali.");
            }

            BigDecimal nilaiValid = validasiDanNormalisasiNilaiSiswa(entri.getScore(), penilaian.getMaxScore());
            validasiSiswaDiKelasPadaTanggal(
                    entri.getStudentId(),
                    penugasanMengajar.getClassId(),
                    penilaian.getAssessmentDate());

            if (daoPenilaian.findScoreByAssessmentAndStudent(idPenilaian, entri.getStudentId()).isPresent()) {
                throw new IllegalArgumentException(
                        "Nilai siswa ID " + entri.getStudentId() + " untuk penilaian ini sudah tercatat.");
            }

            daftarYangAkanDisimpan.add(new StudentScore(
                    null,
                    idPenilaian,
                    entri.getStudentId(),
                    nilaiValid,
                    entri.getNotes() == null ? "" : entri.getNotes().trim(),
                    dicatatOleh));
        }

        daoPenilaian.saveAllScores(daftarYangAkanDisimpan);
        return daftarYangAkanDisimpan;
    }

    public StudentScore updateScore(Long idNilai, BigDecimal nilaiBaru, String keterangan) {
        if (idNilai == null) {
            throw new IllegalArgumentException("ID nilai siswa wajib diisi.");
        }

        Optional<StudentScore> hasilCari = daoPenilaian.findScoreById(idNilai);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data nilai siswa tidak ditemukan.");
        }
        StudentScore nilaiLama = hasilCari.get();

        Assessment penilaian = ambilPenilaianTerotorisasiUntukTulis(nilaiLama.getAssessmentId());
        BigDecimal nilaiValid = validasiDanNormalisasiNilaiSiswa(nilaiBaru, penilaian.getMaxScore());

        nilaiLama.setScore(nilaiValid);
        nilaiLama.setNotes(keterangan == null ? "" : keterangan.trim());
        nilaiLama.setRecordedBy(SessionContext.getInstance().getUserId());

        daoPenilaian.updateScore(nilaiLama);
        return nilaiLama;
    }

    public Optional<StudentScore> getScoreById(Long idNilai) {
        Optional<StudentScore> hasilCari = daoPenilaian.findScoreById(idNilai);
        if (hasilCari.isPresent()) {
            ambilPenilaianTerotorisasiUntukBaca(hasilCari.get().getAssessmentId());
        }
        return hasilCari;
    }

    public List<StudentScore> getScoresByAssessment(Long idPenilaian) {
        ambilPenilaianTerotorisasiUntukBaca(idPenilaian);
        return daoPenilaian.findScoresByAssessmentId(idPenilaian);
    }

    public List<StudentScore> getScoresByStudent(Long idSiswa) {
        wajibkanSudahLogin();
        if (idSiswa == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }

        if (daoSiswa.findById(idSiswa).isEmpty()) {
            throw new IllegalArgumentException("Data siswa tidak ditemukan.");
        }

        List<StudentScore> semuaNilaiSiswa = daoPenilaian.findScoresByStudentId(idSiswa);
        SessionContext konteksSesi = SessionContext.getInstance();
        if (konteksSesi.hasRole(RoleMapper.ROLE_ADMIN)) {
            return semuaNilaiSiswa;
        }

        Long idGuru = wajibkanIdGuruUntukPeranGuru(konteksSesi);
        Set<Long> kumpulanIdPenugasanGuru = ambilIdPenugasanMengajarTerotorisasi(idGuru);
        Set<Long> kumpulanIdKelasAktifGuru = ambilIdKelasMengajarAktif(idGuru);

        Map<Long, Long> petaPenilaianKePenugasan = new HashMap<>();
        for (StudentScore nilai : semuaNilaiSiswa) {
            Long idPenilaian = nilai.getAssessmentId();
            if (!petaPenilaianKePenugasan.containsKey(idPenilaian)) {
                Optional<Assessment> penilaianOpt = daoPenilaian.findAssessmentById(idPenilaian);
                if (penilaianOpt.isPresent()) {
                    petaPenilaianKePenugasan.put(idPenilaian, penilaianOpt.get().getTeachingAssignmentId());
                } else {
                    petaPenilaianKePenugasan.put(idPenilaian, -1L);
                }
            }
        }

        List<StudentScore> hasilFilter = new ArrayList<>();
        for (StudentScore nilai : semuaNilaiSiswa) {
            Long idPenugasan = petaPenilaianKePenugasan.getOrDefault(nilai.getAssessmentId(), -1L);
            if (kumpulanIdPenugasanGuru.contains(idPenugasan)) {
                hasilFilter.add(nilai);
            }
        }

        boolean mengajarKelasSaatIni = false;
        Optional<StudentPlacement> penempatanAktifOpt = daoAkademik.findActivePlacementByStudentId(idSiswa);
        if (penempatanAktifOpt.isPresent()) {
            mengajarKelasSaatIni = kumpulanIdKelasAktifGuru.contains(penempatanAktifOpt.get().getClassId());
        }

        if (!mengajarKelasSaatIni && hasilFilter.isEmpty()) {
            throw new IllegalArgumentException(
                    "Akses ditolak: guru tidak memiliki penugasan mengajar untuk siswa ini.");
        }

        return Collections.unmodifiableList(hasilFilter);
    }

    // ==========================================
    // RINGKASAN (SUMMARIES)
    // ==========================================

    public AssessmentSummary getAssessmentScoreSummary(Long idPenilaian) {
        Assessment penilaian = ambilPenilaianTerotorisasiUntukBaca(idPenilaian);
        List<StudentScore> daftarNilai = daoPenilaian.findScoresByAssessmentId(idPenilaian);

        if (daftarNilai.isEmpty()) {
            BigDecimal nol = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new AssessmentSummary(
                    penilaian.getId(),
                    penilaian.getTitle(),
                    penilaian.getAssessmentType(),
                    penilaian.getMaxScore(),
                    0,
                    nol,
                    nol,
                    nol,
                    nol);
        }

        BigDecimal totalNilai = BigDecimal.ZERO;
        BigDecimal nilaiTertinggi = daftarNilai.get(0).getScore();
        BigDecimal nilaiTerendah = daftarNilai.get(0).getScore();

        for (StudentScore item : daftarNilai) {
            BigDecimal nilaiItem = item.getScore();
            totalNilai = totalNilai.add(nilaiItem);
            if (nilaiItem.compareTo(nilaiTertinggi) > 0) {
                nilaiTertinggi = nilaiItem;
            }
            if (nilaiItem.compareTo(nilaiTerendah) < 0) {
                nilaiTerendah = nilaiItem;
            }
        }

        BigDecimal jumlahSiswa = BigDecimal.valueOf(daftarNilai.size());
        BigDecimal rataRata = totalNilai.divide(jumlahSiswa, 2, RoundingMode.HALF_UP);
        BigDecimal rataRataPersentase = rataRata.multiply(SERATUS)
                .divide(penilaian.getMaxScore(), 2, RoundingMode.HALF_UP);

        return new AssessmentSummary(
                penilaian.getId(),
                penilaian.getTitle(),
                penilaian.getAssessmentType(),
                penilaian.getMaxScore(),
                daftarNilai.size(),
                rataRata,
                nilaiTertinggi.setScale(2, RoundingMode.HALF_UP),
                nilaiTerendah.setScale(2, RoundingMode.HALF_UP),
                rataRataPersentase);
    }

    public StudentScoreSummary getStudentScoreSummary(Long idSiswa) {
        List<StudentScore> daftarNilaiTerlihat = getScoresByStudent(idSiswa);
        if (daftarNilaiTerlihat.isEmpty()) {
            BigDecimal nol = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return new StudentScoreSummary(idSiswa, 0, nol, nol, nol, nol);
        }

        BigDecimal totalDiperoleh = BigDecimal.ZERO;
        BigDecimal totalMaksimal = BigDecimal.ZERO;

        for (StudentScore nilai : daftarNilaiTerlihat) {
            totalDiperoleh = totalDiperoleh.add(nilai.getScore());
            Optional<Assessment> penilaianOpt = daoPenilaian.findAssessmentById(nilai.getAssessmentId());
            BigDecimal nilaiMaksimal = BigDecimal.ZERO;
            if (penilaianOpt.isPresent()) {
                nilaiMaksimal = penilaianOpt.get().getMaxScore();
            }
            totalMaksimal = totalMaksimal.add(nilaiMaksimal);
        }

        BigDecimal jumlahPenilaian = BigDecimal.valueOf(daftarNilaiTerlihat.size());
        BigDecimal rataRata = totalDiperoleh.divide(jumlahPenilaian, 2, RoundingMode.HALF_UP);
        BigDecimal persentaseKeseluruhan = totalMaksimal.compareTo(BigDecimal.ZERO) > 0
                ? totalDiperoleh.multiply(SERATUS).divide(totalMaksimal, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new StudentScoreSummary(
                idSiswa,
                daftarNilaiTerlihat.size(),
                totalDiperoleh.setScale(2, RoundingMode.HALF_UP),
                totalMaksimal.setScale(2, RoundingMode.HALF_UP),
                rataRata,
                persentaseKeseluruhan);
    }

    // ==========================================
    // METODE BANTU OTORISASI & VALIDASI
    // ==========================================

    private void wajibkanSudahLogin() {
        if (!SessionContext.getInstance().isLoggedIn()) {
            throw new IllegalArgumentException("Silakan login terlebih dahulu untuk mengakses penilaian.");
        }
    }

    private Long wajibkanIdGuruUntukPeranGuru(SessionContext konteksSesi) {
        if (!konteksSesi.hasRole(RoleMapper.ROLE_TEACHER)) {
            throw new IllegalArgumentException("Akses ditolak: peran tidak diizinkan.");
        }
        Long idGuru = konteksSesi.getTeacherId();
        if (idGuru == null) {
            throw new IllegalArgumentException(
                    "Akses ditolak: akun guru belum terhubung dengan profil guru.");
        }
        return idGuru;
    }

    private TeachingAssignment wajibkanOtorisasiTulisPenugasanMengajar(Long idPenugasanMengajar) {
        TeachingAssignment penugasan = wajibkanOtorisasiBacaPenugasanMengajar(idPenugasanMengajar);
        if (!penugasan.isActive()) {
            throw new IllegalArgumentException("Penugasan mengajar sudah tidak aktif.");
        }
        return penugasan;
    }

    private TeachingAssignment wajibkanOtorisasiBacaPenugasanMengajar(Long idPenugasanMengajar) {
        wajibkanSudahLogin();
        if (idPenugasanMengajar == null) {
            throw new IllegalArgumentException("ID penugasan mengajar wajib diisi.");
        }

        Optional<TeachingAssignment> hasilCari = daoAkademik.findTeachingAssignmentById(idPenugasanMengajar);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data penugasan mengajar tidak ditemukan.");
        }
        TeachingAssignment penugasan = hasilCari.get();

        SessionContext konteksSesi = SessionContext.getInstance();
        if (konteksSesi.hasRole(RoleMapper.ROLE_ADMIN)) {
            return penugasan;
        }

        Long idGuru = wajibkanIdGuruUntukPeranGuru(konteksSesi);
        if (!idGuru.equals(penugasan.getTeacherId())) {
            throw new IllegalArgumentException(
                    "Akses ditolak: guru hanya dapat mengakses penilaian untuk penugasan mengajar miliknya.");
        }
        return penugasan;
    }

    private Assessment ambilPenilaianTerotorisasiUntukTulis(Long idPenilaian) {
        if (idPenilaian == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi.");
        }
        Optional<Assessment> hasilCari = daoPenilaian.findAssessmentById(idPenilaian);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data penilaian tidak ditemukan.");
        }
        Assessment penilaian = hasilCari.get();
        wajibkanOtorisasiTulisPenugasanMengajar(penilaian.getTeachingAssignmentId());
        return penilaian;
    }

    private Assessment ambilPenilaianTerotorisasiUntukBaca(Long idPenilaian) {
        if (idPenilaian == null) {
            throw new IllegalArgumentException("ID penilaian wajib diisi.");
        }
        Optional<Assessment> hasilCari = daoPenilaian.findAssessmentById(idPenilaian);
        if (hasilCari.isEmpty()) {
            throw new IllegalArgumentException("Data penilaian tidak ditemukan.");
        }
        Assessment penilaian = hasilCari.get();
        wajibkanOtorisasiBacaPenugasanMengajar(penilaian.getTeachingAssignmentId());
        return penilaian;
    }

    private Set<Long> ambilIdPenugasanMengajarTerotorisasi(Long idGuru) {
        Set<Long> kumpulanId = new HashSet<>();
        List<TeachingAssignment> daftarPenugasan = daoAkademik.findTeachingAssignmentsByTeacherId(idGuru);
        for (TeachingAssignment penugasan : daftarPenugasan) {
            kumpulanId.add(penugasan.getId());
        }
        return kumpulanId;
    }

    private Set<Long> ambilIdKelasMengajarAktif(Long idGuru) {
        Set<Long> kumpulanIdKelas = new HashSet<>();
        List<TeachingAssignment> daftarPenugasan = daoAkademik.findTeachingAssignmentsByTeacherId(idGuru);
        for (TeachingAssignment penugasan : daftarPenugasan) {
            if (penugasan.isActive()) {
                kumpulanIdKelas.add(penugasan.getClassId());
            }
        }
        return kumpulanIdKelas;
    }

    private void validasiSiswaDiKelasPadaTanggal(Long idSiswa, Long idKelas, LocalDate tanggalPenilaian) {
        if (idSiswa == null) {
            throw new IllegalArgumentException("ID siswa wajib diisi.");
        }

        Optional<Student> hasilCariSiswa = daoSiswa.findById(idSiswa);
        if (hasilCariSiswa.isEmpty()) {
            throw new IllegalArgumentException("Data siswa tidak ditemukan.");
        }
        Student siswa = hasilCariSiswa.get();
        if (!siswa.isActive()) {
            throw new IllegalArgumentException("Siswa tidak aktif.");
        }

        List<StudentPlacement> riwayatPenempatan = daoAkademik.findPlacementHistoryByStudentId(idSiswa);
        boolean pindahKeKelasLainPadaTanggal = false;
        boolean terdaftarDiKelasPadaTanggal = false;

        for (StudentPlacement penempatan : riwayatPenempatan) {
            if (!penempatan.getClassId().equals(idKelas)
                    && penempatan.getEntryDate().equals(tanggalPenilaian)) {
                pindahKeKelasLainPadaTanggal = true;
            }
            if (penempatan.getClassId().equals(idKelas)) {
                boolean mulaiSebelumAtauPadaTanggal = !tanggalPenilaian.isBefore(penempatan.getEntryDate());
                boolean belumKeluarAtauKeluarSetelahnya = penempatan.getExitDate() == null
                        || !tanggalPenilaian.isAfter(penempatan.getExitDate());
                if (mulaiSebelumAtauPadaTanggal && belumKeluarAtauKeluarSetelahnya) {
                    terdaftarDiKelasPadaTanggal = true;
                }
            }
        }

        if (!terdaftarDiKelasPadaTanggal || pindahKeKelasLainPadaTanggal) {
            throw new IllegalArgumentException(
                    "Siswa ID " + idSiswa + " tidak terdaftar pada kelas ID " + idKelas
                            + " pada tanggal penilaian " + tanggalPenilaian + ".");
        }
    }

    private static String validasiJudul(String judul) {
        if (judul == null || judul.isBlank()) {
            throw new IllegalArgumentException("Judul penilaian wajib diisi.");
        }
        String judulRapih = judul.trim();
        if (judulRapih.length() > 150) {
            throw new IllegalArgumentException("Judul penilaian maksimal 150 karakter.");
        }
        return judulRapih;
    }

    private static String normalisasiDanValidasiJenis(String jenisPenilaian) {
        if (jenisPenilaian == null || jenisPenilaian.isBlank()) {
            throw new IllegalArgumentException("Jenis penilaian wajib diisi.");
        }
        String jenisNormal = jenisPenilaian.trim().toUpperCase();
        if (!DAFTAR_JENIS_VALID.contains(jenisNormal)) {
            throw new IllegalArgumentException(
                    "Jenis penilaian tidak valid. Pilihan: KUIS, TUGAS, UTS, UAS, PRAKTIK, PROYEK, LAINNYA.");
        }
        return jenisNormal;
    }

    private void validasiTanggalPenilaian(LocalDate tanggalPenilaian, TeachingAssignment penugasanMengajar) {
        if (tanggalPenilaian == null) {
            throw new IllegalArgumentException("Tanggal penilaian wajib diisi.");
        }
        Optional<Semester> semesterOpt = daoAkademik.findSemesterById(penugasanMengajar.getSemesterId());
        if (semesterOpt.isPresent()) {
            Semester semester = semesterOpt.get();
            if (tanggalPenilaian.isBefore(semester.getStartDate())
                    || tanggalPenilaian.isAfter(semester.getEndDate())) {
                throw new IllegalArgumentException(
                        "Tanggal penilaian harus berada dalam rentang semester ("
                                + semester.getStartDate() + " s.d. " + semester.getEndDate() + ").");
            }
        }
    }

    private static BigDecimal validasiDanNormalisasiNilaiMaksimal(BigDecimal nilaiMaksimal) {
        if (nilaiMaksimal == null || nilaiMaksimal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Nilai maksimal harus lebih besar dari 0.");
        }
        if (nilaiMaksimal.compareTo(BATAS_MAKSIMAL_DESIMAL) > 0) {
            throw new IllegalArgumentException("Nilai maksimal melebihi batas (99999.99).");
        }
        return nilaiMaksimal.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal validasiDanNormalisasiNilaiSiswa(BigDecimal nilai, BigDecimal nilaiMaksimal) {
        if (nilai == null) {
            throw new IllegalArgumentException("Nilai siswa wajib diisi.");
        }
        if (nilai.compareTo(BigDecimal.ZERO) < 0 || nilai.compareTo(nilaiMaksimal) > 0) {
            throw new IllegalArgumentException(
                    "Nilai siswa harus berada di antara 0 dan nilai maksimal (" + nilaiMaksimal + ").");
        }
        return nilai.setScale(2, RoundingMode.HALF_UP);
    }
}
