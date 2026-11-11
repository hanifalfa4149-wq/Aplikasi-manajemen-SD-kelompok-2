-- ============================================================
-- SISTEM MANAJEMEN KELAS SD
-- Database : manajemen-sd
-- DBMS     : PostgreSQL 18
-- Schema   : public
-- Naming   : Bahasa Indonesia (snake_case)
-- ============================================================

BEGIN;

-- ============================================================
-- 1. GURU
-- ============================================================

CREATE TABLE guru (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nip VARCHAR(30) NOT NULL UNIQUE,
    nama_lengkap VARCHAR(150) NOT NULL,
    jenis_kelamin VARCHAR(10) NOT NULL
        CHECK (jenis_kelamin IN ('LAKI_LAKI', 'PEREMPUAN')),
    email VARCHAR(150),
    nomor_telepon VARCHAR(25),
    alamat TEXT,
    pendidikan_terakhir VARCHAR(100),
    tanggal_bergabung DATE,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_guru_email_unik
    ON guru (LOWER(email))
    WHERE email IS NOT NULL;


-- ============================================================
-- 2. PENGGUNA / AUTENTIKASI
-- ============================================================

CREATE TABLE pengguna (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_guru BIGINT UNIQUE,
    nama_pengguna VARCHAR(50) NOT NULL UNIQUE,
    hash_kata_sandi TEXT NOT NULL,
    salt_kata_sandi VARCHAR(255),
    nama_lengkap VARCHAR(150) NOT NULL,
    peran VARCHAR(20) NOT NULL
        CHECK (peran IN ('ADMIN', 'GURU')),
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    terakhir_login TIMESTAMP,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pengguna_guru
        FOREIGN KEY (id_guru)
        REFERENCES guru(id)
        ON DELETE RESTRICT
);


-- ============================================================
-- 3. SISWA
-- ============================================================

CREATE TABLE siswa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nis VARCHAR(30) NOT NULL UNIQUE,
    nisn VARCHAR(30) UNIQUE,
    nama_lengkap VARCHAR(150) NOT NULL,
    jenis_kelamin VARCHAR(10) NOT NULL
        CHECK (jenis_kelamin IN ('LAKI_LAKI', 'PEREMPUAN')),
    tanggal_lahir DATE,
    tempat_lahir VARCHAR(100),
    alamat TEXT,
    nama_orang_tua VARCHAR(150),
    nomor_telepon_orang_tua VARCHAR(25),
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 4. TAHUN AJARAN
-- ============================================================

CREATE TABLE tahun_ajaran (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nama VARCHAR(30) NOT NULL UNIQUE,
    tanggal_mulai DATE NOT NULL,
    tanggal_selesai DATE NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tahun_ajaran_tanggal
        CHECK (tanggal_selesai > tanggal_mulai)
);

CREATE UNIQUE INDEX idx_tahun_ajaran_aktif
    ON tahun_ajaran (aktif)
    WHERE aktif = TRUE;


-- ============================================================
-- 5. SEMESTER
-- ============================================================

CREATE TABLE semester (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tahun_ajaran BIGINT NOT NULL,
    nomor_semester SMALLINT NOT NULL
        CHECK (nomor_semester IN (1, 2)),
    nama VARCHAR(30) NOT NULL,
    tanggal_mulai DATE NOT NULL,
    tanggal_selesai DATE NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_semester_tahun_ajaran
        FOREIGN KEY (id_tahun_ajaran)
        REFERENCES tahun_ajaran(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_semester_tahun_ajaran
        UNIQUE (id_tahun_ajaran, nomor_semester),

    CONSTRAINT chk_semester_tanggal
        CHECK (tanggal_selesai > tanggal_mulai)
);


-- ============================================================
-- 6. KELAS
-- ============================================================

CREATE TABLE kelas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tahun_ajaran BIGINT NOT NULL,
    nama VARCHAR(50) NOT NULL,
    tingkat_kelas SMALLINT NOT NULL
        CHECK (tingkat_kelas BETWEEN 1 AND 6),
    kapasitas INTEGER NOT NULL
        CHECK (kapasitas > 0),
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_kelas_tahun_ajaran
        FOREIGN KEY (id_tahun_ajaran)
        REFERENCES tahun_ajaran(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_kelas_nama_tahun
        UNIQUE (id_tahun_ajaran, nama),

    CONSTRAINT uq_kelas_id_tahun
        UNIQUE (id, id_tahun_ajaran)
);


-- ============================================================
-- 7. MATA PELAJARAN
-- ============================================================

CREATE TABLE mata_pelajaran (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    kode VARCHAR(30) NOT NULL UNIQUE,
    nama VARCHAR(100) NOT NULL,
    deskripsi TEXT,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


-- ============================================================
-- 8. PENUGASAN WALI KELAS
-- ============================================================

CREATE TABLE penugasan_wali_kelas (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_kelas BIGINT NOT NULL,
    id_guru BIGINT NOT NULL,
    tanggal_mulai DATE NOT NULL,
    tanggal_selesai DATE,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wali_kelas_kelas
        FOREIGN KEY (id_kelas)
        REFERENCES kelas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_wali_kelas_guru
        FOREIGN KEY (id_guru)
        REFERENCES guru(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_wali_kelas_tanggal
        CHECK (
            tanggal_selesai IS NULL
            OR tanggal_selesai >= tanggal_mulai
        )
);

CREATE UNIQUE INDEX idx_wali_kelas_aktif
    ON penugasan_wali_kelas (id_kelas)
    WHERE aktif = TRUE;

CREATE INDEX idx_wali_kelas_guru
    ON penugasan_wali_kelas (id_guru);


-- ============================================================
-- 9. PENUGASAN MENGAJAR
-- ============================================================

CREATE TABLE penugasan_mengajar (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_guru BIGINT NOT NULL,
    id_kelas BIGINT NOT NULL,
    id_mata_pelajaran BIGINT NOT NULL,
    id_semester BIGINT NOT NULL,
    aktif BOOLEAN NOT NULL DEFAULT TRUE,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_mengajar_guru
        FOREIGN KEY (id_guru)
        REFERENCES guru(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_mengajar_kelas
        FOREIGN KEY (id_kelas)
        REFERENCES kelas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_mengajar_mata_pelajaran
        FOREIGN KEY (id_mata_pelajaran)
        REFERENCES mata_pelajaran(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_mengajar_semester
        FOREIGN KEY (id_semester)
        REFERENCES semester(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_penugasan_mengajar
        UNIQUE (
            id_guru,
            id_kelas,
            id_mata_pelajaran,
            id_semester
        )
);

CREATE INDEX idx_mengajar_kelas
    ON penugasan_mengajar (id_kelas);

CREATE INDEX idx_mengajar_guru
    ON penugasan_mengajar (id_guru);


-- ============================================================
-- 10. RIWAYAT KELAS SISWA
-- ============================================================

CREATE TABLE riwayat_kelas_siswa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_siswa BIGINT NOT NULL,
    id_kelas BIGINT NOT NULL,
    id_tahun_ajaran BIGINT NOT NULL,
    tanggal_masuk DATE NOT NULL,
    tanggal_keluar DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'AKTIF'
        CHECK (status IN (
            'AKTIF',
            'NAIK_KELAS',
            'PINDAH',
            'LULUS',
            'KELUAR'
        )),
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_riwayat_siswa
        FOREIGN KEY (id_siswa)
        REFERENCES siswa(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_riwayat_kelas_tahun
        FOREIGN KEY (id_kelas, id_tahun_ajaran)
        REFERENCES kelas(id, id_tahun_ajaran)
        ON DELETE RESTRICT,

    CONSTRAINT chk_riwayat_tanggal
        CHECK (
            tanggal_keluar IS NULL
            OR tanggal_keluar >= tanggal_masuk
        )
);

CREATE UNIQUE INDEX idx_riwayat_siswa_aktif
    ON riwayat_kelas_siswa (id_siswa)
    WHERE status = 'AKTIF' AND tanggal_keluar IS NULL;

CREATE INDEX idx_riwayat_siswa
    ON riwayat_kelas_siswa (id_siswa);

CREATE INDEX idx_riwayat_kelas
    ON riwayat_kelas_siswa (id_kelas);

CREATE INDEX idx_riwayat_tahun_ajaran
    ON riwayat_kelas_siswa (id_tahun_ajaran);


-- ============================================================
-- 11. ABSENSI
-- ============================================================

CREATE TABLE absensi (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_siswa BIGINT NOT NULL,
    id_kelas BIGINT NOT NULL,
    tanggal_absensi DATE NOT NULL,
    status VARCHAR(10) NOT NULL
        CHECK (status IN ('HADIR', 'SAKIT', 'IZIN', 'ALPA')),
    keterangan TEXT,
    dicatat_oleh BIGINT,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_absensi_siswa
        FOREIGN KEY (id_siswa)
        REFERENCES siswa(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_absensi_kelas
        FOREIGN KEY (id_kelas)
        REFERENCES kelas(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_absensi_pengguna
        FOREIGN KEY (dicatat_oleh)
        REFERENCES pengguna(id)
        ON DELETE SET NULL,

    CONSTRAINT uq_absensi_siswa_tanggal
        UNIQUE (id_siswa, tanggal_absensi)
);

CREATE INDEX idx_absensi_tanggal
    ON absensi (tanggal_absensi);

CREATE INDEX idx_absensi_kelas_tanggal
    ON absensi (id_kelas, tanggal_absensi);

CREATE INDEX idx_absensi_siswa
    ON absensi (id_siswa);


-- ============================================================
-- 12. PENILAIAN
-- ============================================================

CREATE TABLE penilaian (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_penugasan_mengajar BIGINT NOT NULL,
    judul VARCHAR(150) NOT NULL,
    jenis_penilaian VARCHAR(30) NOT NULL
        CHECK (jenis_penilaian IN (
            'KUIS',
            'TUGAS',
            'UTS',
            'UAS',
            'PRAKTIK',
            'PROYEK',
            'LAINNYA'
        )),
    tanggal_penilaian DATE NOT NULL,
    nilai_maksimal NUMERIC(7,2) NOT NULL DEFAULT 100
        CHECK (nilai_maksimal > 0),
    deskripsi TEXT,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_penilaian_penugasan
        FOREIGN KEY (id_penugasan_mengajar)
        REFERENCES penugasan_mengajar(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_penilaian_penugasan
    ON penilaian (id_penugasan_mengajar);

CREATE INDEX idx_penilaian_tanggal
    ON penilaian (tanggal_penilaian);


-- ============================================================
-- 13. NILAI SISWA
-- ============================================================

CREATE TABLE nilai_siswa (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_penilaian BIGINT NOT NULL,
    id_siswa BIGINT NOT NULL,
    nilai NUMERIC(7,2) NOT NULL
        CHECK (nilai >= 0),
    keterangan TEXT,
    dicatat_oleh BIGINT,
    dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_nilai_penilaian
        FOREIGN KEY (id_penilaian)
        REFERENCES penilaian(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_nilai_siswa
        FOREIGN KEY (id_siswa)
        REFERENCES siswa(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_nilai_pengguna
        FOREIGN KEY (dicatat_oleh)
        REFERENCES pengguna(id)
        ON DELETE SET NULL,

    CONSTRAINT uq_nilai_siswa_penilaian
        UNIQUE (id_penilaian, id_siswa)
);

CREATE INDEX idx_nilai_siswa
    ON nilai_siswa (id_siswa);


-- ============================================================
-- 14. INDEX TAMBAHAN
-- ============================================================

CREATE INDEX idx_siswa_nama
    ON siswa (LOWER(nama_lengkap));

CREATE INDEX idx_guru_nama
    ON guru (LOWER(nama_lengkap));

CREATE INDEX idx_pengguna_peran
    ON pengguna (peran);


COMMIT;

-- ============================================================
-- SELESAI
-- ============================================================