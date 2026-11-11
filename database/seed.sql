-- ============================================================
-- SISTEM MANAJEMEN KELAS SD
-- Demo Seed Data (Deterministic & Idempotent)
-- Database : manajemen-sd (PostgreSQL 18)
--
-- Catatan:
-- - Jalankan setelah database/schema.sql.
-- - Aman dijalankan berulang kali (idempotent via ON CONFLICT / NOT EXISTS).
-- - Akun login (admin & guru) di-seed otomatis saat aplikasi
--   pertama kali dijalankan oleh AuthBootstrapSeeder menggunakan
--   PBKDF2WithHmacSHA256 + random salt (tanpa plaintext password di SQL).
-- ============================================================

BEGIN;

-- 1. GURU DEMO
INSERT INTO guru (
    nip, nama_lengkap, jenis_kelamin, email, nomor_telepon, alamat, pendidikan_terakhir, tanggal_bergabung, aktif
) VALUES
    ('198501012010011001', 'Budi Santoso, S.Pd.', 'LAKI_LAKI', 'budi.santoso@sdn01.sch.id', '081234567801', 'Jl. Pendidikan No. 10, Pekanbaru', 'S1 PGSD', DATE '2010-07-01', TRUE),
    ('198805122014022002', 'Siti Aminah, S.Pd.', 'PEREMPUAN', 'siti.aminah@sdn01.sch.id', '081234567802', 'Jl. Melati No. 5, Pekanbaru', 'S1 Pendidikan Matematika', DATE '2014-07-01', TRUE)
ON CONFLICT (nip) DO NOTHING;

-- 2. SISWA DEMO
INSERT INTO siswa (
    nis, nisn, nama_lengkap, jenis_kelamin, tanggal_lahir, tempat_lahir, alamat, nama_orang_tua, nomor_telepon_orang_tua, aktif
) VALUES
    ('202601001', '0160001001', 'Ahmad Pratama', 'LAKI_LAKI', DATE '2019-03-14', 'Pekanbaru', 'Jl. Dahlia No. 12', 'Hendra Pratama', '081311110001', TRUE),
    ('202601002', '0160001002', 'Aulia Putri', 'PEREMPUAN', DATE '2019-06-21', 'Pekanbaru', 'Jl. Anggrek No. 8', 'Dewi Lestari', '081311110002', TRUE),
    ('202602001', '0150002001', 'Bagas Saputra', 'LAKI_LAKI', DATE '2018-09-05', 'Siak', 'Jl. Kenanga No. 3', 'Rudi Saputra', '081311110003', TRUE)
ON CONFLICT (nis) DO NOTHING;

-- 3. TAHUN AJARAN DEMO
-- Hanya mengaktifkan '2026/2027' jika belum ada tahun ajaran lain yang aktif (menghormati idx_tahun_ajaran_aktif).
INSERT INTO tahun_ajaran (nama, tanggal_mulai, tanggal_selesai, aktif)
SELECT '2026/2027', DATE '2026-07-13', DATE '2027-06-19',
       NOT EXISTS (SELECT 1 FROM tahun_ajaran WHERE aktif = TRUE AND nama <> '2026/2027')
WHERE NOT EXISTS (SELECT 1 FROM tahun_ajaran WHERE nama = '2026/2027');

-- 4. SEMESTER DEMO
INSERT INTO semester (id_tahun_ajaran, nomor_semester, nama, tanggal_mulai, tanggal_selesai, aktif)
SELECT ta.id, s.nomor_semester, s.nama, s.tanggal_mulai, s.tanggal_selesai, TRUE
FROM tahun_ajaran ta
CROSS JOIN (
    VALUES
        (1, 'Ganjil 2026/2027', DATE '2026-07-13', DATE '2026-12-19'),
        (2, 'Genap 2026/2027',  DATE '2027-01-04', DATE '2027-06-19')
) AS s(nomor_semester, nama, tanggal_mulai, tanggal_selesai)
WHERE ta.nama = '2026/2027'
ON CONFLICT (id_tahun_ajaran, nomor_semester) DO NOTHING;

-- 5. KELAS DEMO
INSERT INTO kelas (id_tahun_ajaran, nama, tingkat_kelas, kapasitas, aktif)
SELECT ta.id, k.nama, k.tingkat_kelas, k.kapasitas, TRUE
FROM tahun_ajaran ta
CROSS JOIN (
    VALUES
        ('1A', 1, 28),
        ('2A', 2, 28)
) AS k(nama, tingkat_kelas, kapasitas)
WHERE ta.nama = '2026/2027'
ON CONFLICT (id_tahun_ajaran, nama) DO NOTHING;

-- 6. MATA PELAJARAN DEMO
INSERT INTO mata_pelajaran (kode, nama, deskripsi, aktif) VALUES
    ('MTK-SD', 'Matematika', 'Berhitung, geometri dasar, dan pemecahan masalah.', TRUE),
    ('BIN-SD', 'Bahasa Indonesia', 'Membaca, menulis, dan memahami teks.', TRUE),
    ('IPA-SD', 'Ilmu Pengetahuan Alam', 'Pengenalan alam sekitar dan eksperimen sederhana.', TRUE)
ON CONFLICT (kode) DO NOTHING;

-- 7. PENUGASAN WALI KELAS DEMO
INSERT INTO penugasan_wali_kelas (id_kelas, id_guru, tanggal_mulai, tanggal_selesai, aktif)
SELECT k.id, g.id, DATE '2026-07-13', NULL, TRUE
FROM kelas k
JOIN tahun_ajaran ta ON ta.id = k.id_tahun_ajaran
JOIN guru g ON g.nip = '198501012010011001'
WHERE ta.nama = '2026/2027' AND k.nama = '1A'
  AND NOT EXISTS (
      SELECT 1 FROM penugasan_wali_kelas wk WHERE wk.id_kelas = k.id AND wk.aktif = TRUE
  );

INSERT INTO penugasan_wali_kelas (id_kelas, id_guru, tanggal_mulai, tanggal_selesai, aktif)
SELECT k.id, g.id, DATE '2026-07-13', NULL, TRUE
FROM kelas k
JOIN tahun_ajaran ta ON ta.id = k.id_tahun_ajaran
JOIN guru g ON g.nip = '198805122014022002'
WHERE ta.nama = '2026/2027' AND k.nama = '2A'
  AND NOT EXISTS (
      SELECT 1 FROM penugasan_wali_kelas wk WHERE wk.id_kelas = k.id AND wk.aktif = TRUE
  );

-- 8. PENUGASAN MENGAJAR DEMO (Semester 1)
INSERT INTO penugasan_mengajar (id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif)
SELECT g.id, k.id, mp.id, sem.id, TRUE
FROM tahun_ajaran ta
JOIN semester sem ON sem.id_tahun_ajaran = ta.id AND sem.nomor_semester = 1
JOIN kelas k ON k.id_tahun_ajaran = ta.id AND k.nama = '1A'
JOIN guru g ON g.nip = '198501012010011001'
JOIN mata_pelajaran mp ON mp.kode = 'BIN-SD'
WHERE ta.nama = '2026/2027'
ON CONFLICT (id_guru, id_kelas, id_mata_pelajaran, id_semester) DO NOTHING;

INSERT INTO penugasan_mengajar (id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif)
SELECT g.id, k.id, mp.id, sem.id, TRUE
FROM tahun_ajaran ta
JOIN semester sem ON sem.id_tahun_ajaran = ta.id AND sem.nomor_semester = 1
JOIN kelas k ON k.id_tahun_ajaran = ta.id AND k.nama = '1A'
JOIN guru g ON g.nip = '198805122014022002'
JOIN mata_pelajaran mp ON mp.kode = 'MTK-SD'
WHERE ta.nama = '2026/2027'
ON CONFLICT (id_guru, id_kelas, id_mata_pelajaran, id_semester) DO NOTHING;

-- 9. PENEMPATAN SISWA DEMO
INSERT INTO riwayat_kelas_siswa (id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status)
SELECT s.id, k.id, ta.id, DATE '2026-07-13', NULL, 'AKTIF'
FROM siswa s
JOIN tahun_ajaran ta ON ta.nama = '2026/2027'
JOIN kelas k ON k.id_tahun_ajaran = ta.id AND k.nama = '1A'
WHERE s.nis IN ('202601001', '202601002')
  AND NOT EXISTS (
      SELECT 1 FROM riwayat_kelas_siswa r WHERE r.id_siswa = s.id AND r.status = 'AKTIF' AND r.tanggal_keluar IS NULL
  );

INSERT INTO riwayat_kelas_siswa (id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status)
SELECT s.id, k.id, ta.id, DATE '2026-07-13', NULL, 'AKTIF'
FROM siswa s
JOIN tahun_ajaran ta ON ta.nama = '2026/2027'
JOIN kelas k ON k.id_tahun_ajaran = ta.id AND k.nama = '2A'
WHERE s.nis = '202602001'
  AND NOT EXISTS (
      SELECT 1 FROM riwayat_kelas_siswa r WHERE r.id_siswa = s.id AND r.status = 'AKTIF' AND r.tanggal_keluar IS NULL
  );

-- 10. ABSENSI DEMO
INSERT INTO absensi (id_siswa, id_kelas, tanggal_absensi, status, keterangan, dicatat_oleh)
SELECT s.id, k.id, a.tanggal_absensi, a.status, a.keterangan, NULL
FROM (
    VALUES
        ('202601001', '1A', DATE '2026-08-03', 'HADIR', 'Hadir tepat waktu'),
        ('202601002', '1A', DATE '2026-08-03', 'IZIN',  'Izin acara keluarga'),
        ('202602001', '2A', DATE '2026-08-03', 'HADIR', 'Hadir tepat waktu')
) AS a(nis, kelas_nama, tanggal_absensi, status, keterangan)
JOIN siswa s ON s.nis = a.nis
JOIN tahun_ajaran ta ON ta.nama = '2026/2027'
JOIN kelas k ON k.id_tahun_ajaran = ta.id AND k.nama = a.kelas_nama
ON CONFLICT (id_siswa, tanggal_absensi) DO NOTHING;

-- 11. PENILAIAN DEMO
INSERT INTO penilaian (id_penugasan_mengajar, judul, jenis_penilaian, tanggal_penilaian, nilai_maksimal, deskripsi)
SELECT pm.id, 'Kuis Penjumlahan 1-20', 'KUIS', DATE '2026-08-10', 100.00, 'Latihan penjumlahan dasar kelas 1A'
FROM penugasan_mengajar pm
JOIN guru g ON g.id = pm.id_guru AND g.nip = '198805122014022002'
JOIN kelas k ON k.id = pm.id_kelas
JOIN tahun_ajaran ta ON ta.id = k.id_tahun_ajaran AND ta.nama = '2026/2027'
JOIN mata_pelajaran mp ON mp.id = pm.id_mata_pelajaran AND mp.kode = 'MTK-SD'
WHERE k.nama = '1A'
  AND NOT EXISTS (
      SELECT 1 FROM penilaian p
      WHERE p.id_penugasan_mengajar = pm.id AND p.judul = 'Kuis Penjumlahan 1-20'
  );

-- 12. NILAI SISWA DEMO
INSERT INTO nilai_siswa (id_penilaian, id_siswa, nilai, keterangan, dicatat_oleh)
SELECT p.id, s.id, n.nilai, n.keterangan, NULL
FROM (
    VALUES
        ('202601001', 90.00, 'Pemahaman baik'),
        ('202601002', 85.00, 'Teliti mengerjakan soal')
) AS n(nis, nilai, keterangan)
JOIN siswa s ON s.nis = n.nis
JOIN penilaian p ON p.judul = 'Kuis Penjumlahan 1-20'
ON CONFLICT (id_penilaian, id_siswa) DO NOTHING;

COMMIT;

