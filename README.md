# Sistem Manajemen Kelas SD

Aplikasi desktop berbasis Java Swing (*Claymorphism Botanical Green*) serta antarmuka CLI (*Command-Line Interface*) dengan persistensi PostgreSQL untuk mengelola administrasi akademik dan operasional kelas sekolah dasar (SD). Proyek ini dikembangkan untuk mata kuliah **Pemrograman Berorientasi Objek (PBO)** Kelompok 2 dengan arsitektur modular berlapis (`Presentation -> Service -> DAO -> PostgreSQL`).

---

## Fitur Utama

### Akses `ADMIN`
- **Autentikasi & Sesi:** Login berbasis username/password (`PBKDF2WithHmacSHA256`), informasi sesi aktif, dan logout bersih.
- **Dashboard Ringkasan:** Statistik siswa aktif/nonaktif, guru aktif/nonaktif, tahun ajaran & semester aktif, kelas, mapel, wali kelas, penugasan mengajar, total penilaian, total absensi, dan persentase kehadiran.
- **Manajemen Siswa & Guru:** CRUD lengkap, pencarian, soft-deactivation (`aktif = false`), validasi duplikasi (`NIS`/`NISN`/`NIP`/`email`), serta riwayat penempatan kelas siswa.
- **Struktur Akademik & Penugasan:** Pengelolaan Tahun Ajaran (hanya 1 aktif pada satu waktu), Semester (1 Ganjil / 2 Genap), Kelas (tingkat 1–6 & kapasitas), Mata Pelajaran, Penugasan Wali Kelas, Penugasan Mengajar, serta Penempatan/Naik/Pindah/Lulus Siswa (transaksional).
- **Absensi & Penilaian:** Pencatatan absensi harian (`HADIR`, `SAKIT`, `IZIN`, `ALPA`) satuan maupun massal per kelas, pembuatan penilaian (`KUIS`, `TUGAS`, `UTS`, `UAS`, `PRAKTIK`, `PROYEK`, `LAINNYA`), serta pencatatan & pembaruan nilai (`0 <= nilai <= nilai_maksimal`).
- **Laporan & Rekapitulasi:** Siswa per kelas, siswa per jenis kelamin, rekapitulasi absensi (rentang tanggal & per kelas beserta persentase), ringkasan penilaian, ringkasan nilai siswa, dan rata-rata nilai per mata pelajaran.

### Akses `TEACHER` (`GURU` di database)
- Menu utama khusus guru yang menyembunyikan fitur administrasi master data (`Manajemen Siswa`, `Manajemen Guru`, `Struktur Akademik`).
- Pembatasan cakupan data (*scope*) berdasarkan penugasan wali kelas dan penugasan mengajar aktif pada sisi UI maupun Service layer:
  - Melihat kelas, mata pelajaran, dan daftar siswa yang diampu.
  - Mengelola absensi harian hanya untuk kelas yang diampu.
  - Mengelola penilaian dan nilai hanya untuk penugasan mengajar milik guru tersebut.
  - Melihat dashboard dan laporan yang tersaring ke cakupan kelas/siswa yang diampu.

---

## Technology Stack

- **Bahasa & Target Kompilasi:** Java 17 (`maven.compiler.release=17`), diuji pada **JDK 25**
- **Build Tool:** Apache Maven 3 (`exec-maven-plugin:3.5.0`, `maven-compiler-plugin:3.13.0`)
- **Database:** **PostgreSQL 18**
- **Driver JDBC:** `org.postgresql:postgresql:42.7.7`
- **Keamanan Kata Sandi:** `PBKDF2WithHmacSHA256` (600.000 iterasi, key 256-bit, random salt 16-byte Base64, perbandingan constant-time)
- **Pengujian:** JUnit Jupiter `5.11.4` (unit test `InMemory*DAO` + integration test PostgreSQL 18)
- **Antarmuka:** Desktop Java Swing ([`SwingApplication`](src/main/java/id/ac/sekolah/manajemenkelas/presentation/swing/SwingApplication.java), [`MainFrame`](src/main/java/id/ac/sekolah/manajemenkelas/presentation/swing/MainFrame.java)) sebagai mode default serta fallback CLI berbasis teks ([`CliApplication`](src/main/java/id/ac/sekolah/manajemenkelas/presentation/cli/CliApplication.java)) via argumen `--cli`

---

## Prasyarat & Asumsi Instalasi

1. **Java Development Kit (JDK):** JDK 17 atau lebih baru (lingkungan pengembangan proyek menggunakan JDK 25 dengan target bytecode Java 17).
2. **Apache Maven:** Terpasang dan tersedia pada `PATH` (`mvn -v`), atau gunakan runner Maven bawaan NetBeans.
3. **PostgreSQL 18:** Server PostgreSQL terpasang secara lokal pada `localhost:5432`, aktif (*running*), dan dapat diakses oleh user database (default lokal: `postgres`).

---

## Panduan Setup Database PostgreSQL

### 1. Membuat Database
Pastikan service PostgreSQL berjalan, lalu buat database bernama `manajemen-sd`:

```sql
CREATE DATABASE "manajemen-sd";
```

Atau melalui terminal:

```bash
createdb -U postgres "manajemen-sd"
```

### 2. Inisialisasi Skema (`database/schema.sql`)
Jalankan skrip DDL untuk membuat seluruh 13 tabel, constraint `CHECK`, foreign key, unique constraint, dan partial unique index:

```bash
psql -U postgres -d "manajemen-sd" -f database/schema.sql
```

### 3. Inisialisasi Data Demo (`database/seed.sql`) — Opsional
Skrip ini idempoten (aman dijalankan berulang kali) untuk mengisi data demo guru, siswa, tahun ajaran `2026/2027`, semester, kelas `1A`/`2A`, mata pelajaran, penugasan wali kelas/mengajar, penempatan siswa, absensi, penilaian, dan nilai tanpa menyimpan plaintext password:

```bash
psql -U postgres -d "manajemen-sd" -f database/seed.sql
```

*(Anda juga dapat menjalankan isi [`database/schema.sql`](database/schema.sql) dan [`database/seed.sql`](database/seed.sql) lewat pgAdmin Query Tool pada database `manajemen-sd`.)*

---

## Konfigurasi `application.properties`

File `src/main/resources/application.properties` sengaja dikecualikan dari Git (`.gitignore`) agar kredensial PostgreSQL lokal tidak ikut ter-commit.

1. Salin template konfigurasi:
   - Windows PowerShell:
     ```powershell
     Copy-Item src\main\resources\application.properties.example src\main\resources\application.properties
     ```
   - Linux/macOS:
     ```bash
     cp src/main/resources/application.properties.example src/main/resources/application.properties
     ```
2. Buka `src/main/resources/application.properties` dan sesuaikan `db.username` serta `db.password` dengan PostgreSQL lokal Anda:
   ```properties
   db.host=localhost
   db.port=5432
   db.name=manajemen-sd
   db.username=postgres
   db.password=YOUR_POSTGRES_PASSWORD
   ```

---

## Build, Pengujian & Menjalankan Aplikasi

### Build & Uji via Maven
- Menjalankan seluruh unit test & integration test PostgreSQL:
  ```bash
  mvn clean test
  ```
- Membangun paket JAR (`target/sistem-manajemen-kelas-sd-1.0.0-SNAPSHOT.jar`):
  ```bash
  mvn package
  ```
- Menjalankan aplikasi desktop Java Swing via Maven (mode default):
  ```bash
  mvn exec:java
  ```
- Menjalankan mode CLI via Maven (`--cli`):
  ```bash
  mvn exec:java -Dexec.args="--cli"
  ```

### Menjalankan di Apache NetBeans
1. Buka NetBeans -> **File -> Open Project...** lalu pilih folder root `Aplikasi-manajemen-SD-kelompok-2`.
2. Pastikan file `src/main/resources/application.properties` sudah disalin dan diisi kredensial PostgreSQL yang valid.
3. Klik kanan project -> **Clean and Build** (untuk mengunduh dependensi Maven dan mengompilasi proyek).
4. Klik kanan project -> **Run** (entry point utama adalah `id.ac.sekolah.manajemenkelas.Main`).
5. Untuk menguji koneksi JDBC secara terpisah, klik kanan file [`DatabaseConnectionTest.java`](src/main/java/id/ac/sekolah/manajemenkelas/DatabaseConnectionTest.java) -> **Run File**.

---

## Akun Demo Bawaan

Saat aplikasi dijalankan pertama kali, [`AuthBootstrapSeeder`](src/main/java/id/ac/sekolah/manajemenkelas/auth/service/AuthBootstrapSeeder.java) membuat dua akun bawaan secara idempoten dengan hash `PBKDF2WithHmacSHA256`:

| Username | Kata Sandi Demo | Peran Aplikasi (`peran` DB) | Catatan |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` (`ADMIN`) | Akses penuh ke seluruh master data, penugasan, absensi, penilaian, dan laporan. |
| `guru` | `guru123` | `TEACHER` (`GURU`) | Secara default `id_guru` bernilai `NULL`. Fitur khusus guru (absensi/penilaian/laporan terlingkup) membutuhkan akun `TEACHER` yang terhubung ke baris valid pada tabel `guru` (`id_guru` tidak `NULL`) dan memiliki penugasan kelas/mengajar. |

---

## Troubleshooting

1. **Aplikasi masuk ke mode InMemory fallback saat startup**
   - Gejala: muncul pesan gagal koneksi PostgreSQL pada konsol saat startup, lalu aplikasi beralih ke `InMemory*DAO`.
   - Solusi: pastikan service PostgreSQL aktif di `localhost:5432`, database `manajemen-sd` sudah dibuat, `database/schema.sql` telah dijalankan, dan `db.username`/`db.password` pada `src/main/resources/application.properties` benar.
2. **Error `application.properties tidak ditemukan`**
   - Solusi: salin `src/main/resources/application.properties.example` ke `src/main/resources/application.properties`, lalu jalankan `mvn clean compile` agar resource tersalin ke `target/classes`.
3. **Login sebagai `guru` gagal mengakses Absensi / Penilaian / Dashboard Guru**
   - Penyebab: akun `guru` bawaan tidak memiliki relasi `id_guru` pada tabel `pengguna` sampai Admin/penguji menautkannya ke profil di tabel `guru` yang memiliki penugasan wali kelas atau penugasan mengajar.
4. **`mvn test` gagal karena koneksi database**
   - Sebagian besar integration test (`*IntegrationTest`) memerlukan database PostgreSQL `manajemen-sd` dengan skema `database/schema.sql` aktif dan kredensial lokal valid di `src/main/resources/application.properties`.
