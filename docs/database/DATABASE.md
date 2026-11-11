# Database Design

# Sistem Manajemen Kelas SD

**Document Status:** Synchronized with Implemented PostgreSQL 18 Schema  
**Version:** 2.0  
**Database Engine:** PostgreSQL 18 (`manajemen-sd`)

---

## 1. Purpose

This document defines database design principles, naming conventions, integrity expectations, and the conceptual data structure of the application.

The executable database definition belongs in:

- `database/schema.sql`
- `database/seed.sql`
- `database/migrations/`

This document explains the database design. It must remain consistent with the actual SQL implementation.

## 2. Database Design Principles

1. Use relational modeling.
2. Preserve academic history.
3. Enforce referential integrity.
4. Avoid unnecessary data duplication.
5. Use transactions for multi-step operations.
6. Use parameterized SQL statements.
7. Prefer explicit constraints over relying exclusively on application validation.
8. Avoid destructive deletion of referenced academic records.
9. Model academic periods explicitly.
10. Keep the schema understandable for a small development team.

## 3. Naming Conventions

Physical identifiers in [`database/schema.sql`](../../database/schema.sql) use **Bahasa Indonesia `snake_case`** (`guru`, `pengguna`, `siswa`, `tahun_ajaran`, `semester`, `kelas`, `mata_pelajaran`, `penugasan_wali_kelas`, `penugasan_mengajar`, `riwayat_kelas_siswa`, `absensi`, `penilaian`, `nilai_siswa`), while Java models and DAOs map cleanly between English domain classes and Indonesian SQL columns.

### Tables & Columns

- Use `snake_case` table and column names consistently.
- Primary key convention on all 13 tables: `id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`.
- Foreign key convention: `id_<entitas>` (e.g., `id_siswa`, `id_kelas`, `id_tahun_ajaran`, `id_guru`, `id_penugasan_mengajar`), plus `dicatat_oleh` referencing `pengguna(id)`.
- Audit timestamps: `dibuat_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP`, and `diperbarui_pada TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` on mutable entity tables (`guru`, `pengguna`, `siswa`, `tahun_ajaran`, `kelas`, `mata_pelajaran`, `absensi`, `penilaian`, `nilai_siswa`).

### Constraints & Indexes

- Foreign keys: `fk_<tabel>_<referensi>` (mostly `ON DELETE RESTRICT`; `ON DELETE SET NULL` for optional audit actor `dicatat_oleh`)
- Unique constraints: `uq_<tabel>_<kolom>`
- Check constraints: `chk_<tabel>_<aturan>`
- Partial/lookup indexes: `idx_<tabel>_<kolom>` (e.g., `idx_guru_email_unik`, `idx_tahun_ajaran_aktif`, `idx_wali_kelas_aktif`, `idx_riwayat_siswa_aktif`)

## 4. Data Types

| Data | Actual PostgreSQL 18 Type (`schema.sql`) |
|---|---|
| Primary identifiers | `BIGINT GENERATED ALWAYS AS IDENTITY` |
| Codes / short identifiers | `VARCHAR(30)` (`nip`, `nis`, `nisn`, `kode`, `tahun_ajaran.nama`, `semester.nama`) |
| Names | `VARCHAR(50)` (`kelas.nama`), `VARCHAR(100)` (`mata_pelajaran.nama`), `VARCHAR(150)` (full names, assessment titles) |
| Phone numbers & usernames | `VARCHAR(25)` (`nomor_telepon*`), `VARCHAR(50)` (`nama_pengguna`) |
| Descriptions, addresses, notes | `TEXT` (`alamat`, `deskripsi`, `keterangan`, `hash_kata_sandi`) |
| Dates & timestamps | `DATE`, `TIMESTAMP` |
| Boolean flags | `BOOLEAN NOT NULL DEFAULT TRUE` (`aktif`) |
| Numeric capacities & scores | `INTEGER` / `SMALLINT` (`kapasitas`, `tingkat_kelas`, `nomor_semester`), `NUMERIC(7,2)` (`nilai`, `nilai_maksimal`) |
| Enumerated values | `VARCHAR` with explicit `CHECK` constraints (`jenis_kelamin`, `peran`, `riwayat_kelas_siswa.status`, `absensi.status`, `jenis_penilaian`) |

## 5. Implemented Tables & Java Mapping (`database/schema.sql`)

| # | PostgreSQL Table | Java Model | DAO Interface & Postgres Impl | Key Constraints & Mappings |
|---|---|---|---|---|
| 1 | `guru` | `Teacher` | `TeacherDAO` / `PostgresTeacherDAO` | `nip UNIQUE`, `idx_guru_email_unik` (`LOWER(email) WHERE email IS NOT NULL`), `GenderMapper` (`L`/`P` <-> `LAKI_LAKI`/`PEREMPUAN`) |
| 2 | `pengguna` | `User` | `AuthDAO` / `PostgresAuthDAO` | `nama_pengguna UNIQUE`, `id_guru UNIQUE FK -> guru`, `RoleMapper` (`ADMIN`/`TEACHER` <-> `ADMIN`/`GURU`), PBKDF2 hash & salt |
| 3 | `siswa` | `Student` | `StudentDAO` / `PostgresStudentDAO` | `nis UNIQUE`, `nisn UNIQUE`, `GenderMapper` (`L`/`P` <-> `LAKI_LAKI`/`PEREMPUAN`) |
| 4 | `tahun_ajaran` | `AcademicYear` | `AcademicDAO` / `PostgresAcademicDAO` | `nama UNIQUE`, `chk_tahun_ajaran_tanggal`, `idx_tahun_ajaran_aktif` (`WHERE aktif = TRUE`) |
| 5 | `semester` | `Semester` | `AcademicDAO` / `PostgresAcademicDAO` | `uq_semester_tahun_ajaran (id_tahun_ajaran, nomor_semester)`, `nomor_semester IN (1, 2)`, `chk_semester_tanggal` |
| 6 | `kelas` | `SchoolClass` | `AcademicDAO` / `PostgresAcademicDAO` | `tingkat_kelas BETWEEN 1 AND 6`, `kapasitas > 0`, `uq_kelas_nama_tahun`, composite `uq_kelas_id_tahun (id, id_tahun_ajaran)` |
| 7 | `mata_pelajaran` | `Subject` | `AcademicDAO` / `PostgresAcademicDAO` | `kode UNIQUE`, optional nullable `deskripsi` |
| 8 | `penugasan_wali_kelas` | `HomeroomAssignment` | `AcademicDAO` / `PostgresAcademicDAO` | `idx_wali_kelas_aktif` (`id_kelas WHERE aktif = TRUE`), `chk_wali_kelas_tanggal` |
| 9 | `penugasan_mengajar` | `TeachingAssignment` | `AcademicDAO` / `PostgresAcademicDAO` | `uq_penugasan_mengajar (id_guru, id_kelas, id_mata_pelajaran, id_semester)` |
| 10 | `riwayat_kelas_siswa` | `StudentPlacement` | `AcademicDAO` / `PostgresAcademicDAO` | Composite `fk_riwayat_kelas_tahun`, `status IN ('AKTIF','NAIK_KELAS','PINDAH','LULUS','KELUAR')`, `idx_riwayat_siswa_aktif` (`WHERE status = 'AKTIF' AND tanggal_keluar IS NULL`) |
| 11 | `absensi` | `Attendance` | `AttendanceDAO` / `PostgresAttendanceDAO` | `status IN ('HADIR','SAKIT','IZIN','ALPA')`, `uq_absensi_siswa_tanggal (id_siswa, tanggal_absensi)`, nullable `dicatat_oleh` |
| 12 | `penilaian` | `Assessment` | `AssessmentDAO` / `PostgresAssessmentDAO` | `jenis_penilaian IN ('KUIS','TUGAS','UTS','UAS','PRAKTIK','PROYEK','LAINNYA')`, `nilai_maksimal NUMERIC(7,2) > 0` |
| 13 | `nilai_siswa` | `StudentScore` | `AssessmentDAO` / `PostgresAssessmentDAO` | `nilai NUMERIC(7,2) >= 0`, `uq_nilai_siswa_penilaian (id_penilaian, id_siswa)`, nullable `dicatat_oleh` |

Reporting queries in `ReportDAO` / `PostgresReportDAO` execute parameterized SQL aggregations across these 13 tables (`COUNT`, `SUM`, `AVG`, `ROUND`, `COALESCE`) without duplicating stored summary tables.

## 6. Important Relationships

The database must represent these relationships:

- One academic year has multiple semesters.
- A teaching assignment references a teacher, class, subject, and academic period.
- A homeroom assignment references a teacher, class, and academic period.
- A student placement references a student, class, and academic period.
- An attendance record references a student and applicable academic context.
- An assessment references an appropriate teaching assignment or equivalent academic context.
- A student score references a student and assessment.
- A user account may reference a teacher profile when the account belongs to a teacher.

Do not store a student's permanent current class directly as the authoritative historical relationship.

## 7. Referential Integrity

Use foreign keys for relationships between persistent entities.

Define deletion behavior deliberately.

General recommendations:

- Prefer `RESTRICT` or `NO ACTION` for referenced academic records.
- Use `CASCADE` only when dependent data is genuinely disposable and cascading deletion is explicitly intended.
- Avoid cascading deletion from students, teachers, classes, or academic years into historical records.
- Use status fields for deactivation where appropriate.

## 8. Uniqueness and Conflict Prevention

Potential uniqueness rules to evaluate:

- Username must be unique.
- School student identifier must be unique when the school requires one.
- Teacher identifier must be unique when the school requires one.
- Academic year label must not be duplicated.
- Subject code must be unique when used.
- A student should not have conflicting active placements in the same academic period.
- Attendance should not have duplicate records for the same student and school date within the applicable context.
- Homeroom assignments should not conflict for the same class and academic period unless explicitly permitted.

Some rules require partial unique indexes or additional date-range constraints. Choose the actual implementation after the domain rules are finalized.

## 9. Transactions

Multi-step business operations must be atomic where partial completion would produce inconsistent data.

Examples:

- Saving attendance for a class.
- Creating or updating related academic records.
- Performing operations that modify multiple dependent tables.

Transaction ownership belongs to the Service operation coordinating the work.

DAO methods should participate in the transaction rather than independently committing intermediate operations.

## 10. Indexing

Add indexes based on actual query patterns.

Likely candidates include:

- Foreign key columns used in joins.
- Student identifiers used in searches.
- Teacher identifiers used in searches.
- Academic period references.
- Class references.
- Attendance date and student references.
- Assessment references in score queries.

Do not create indexes indiscriminately. Evaluate query patterns and uniqueness requirements.

## 11. Audit and Historical Data

Consider recording:

- `created_at`
- `updated_at`
- `created_by`
- `updated_by`

Use these fields where they provide meaningful auditability.

Do not treat audit columns as a replacement for historical domain entities such as student placements or teaching assignments.

## 12. SQL Security

- Use `PreparedStatement` for parameterized SQL.
- Never concatenate untrusted user input into SQL statements.
- Whitelist dynamic column names and sort directions when dynamic SQL is necessary.
- Do not store plaintext passwords.
- Keep database credentials outside committed source code.
- Grant only necessary database privileges for the application environment.

## 13. Schema and Migration Management

`database/schema.sql` defines the current database structure for initial setup.

`database/seed.sql` provides controlled initial or demonstration data.

`database/migrations/` contains incremental schema changes when the project adopts migration-based updates.

Every schema change must be coordinated with affected module owners.

When modifying the schema:

1. Update SQL definitions.
2. Update this document when the conceptual design changes.
3. Update affected DAO queries.
4. Update affected Service logic.
5. Review seed data.
6. Verify database constraints.
7. Test affected workflows.

Never assume that changing a Java model automatically changes the PostgreSQL schema.

## 14. Demo Data

Seed data should be deterministic and suitable for demonstrating:

- Admin login.
- Teacher login.
- Multiple classes.
- Multiple teachers.
- Multiple subjects.
- Teaching assignments.
- Student placements.
- Attendance records.
- Assessments and scores.

Never commit real student or teacher personal information.

Use fictional demonstration identities.

## 15. Query and Performance Principles

- Retrieve only required columns when practical.
- Avoid N+1 query patterns in report generation.
- Use joins and aggregation where appropriate.
- Paginate large result sets if the application requires it.
- Use indexes that support actual filters and joins.
- Do not perform blocking UI database work directly if a future GUI layer is added.

## 16. Resolved Schema Decisions

All initial design questions are resolved in [`database/schema.sql`](../../database/schema.sql):

- **Primary key strategy:** `BIGINT GENERATED ALWAYS AS IDENTITY` across all 13 tables, populated back into Java models via `RETURNING id`.
- **Student & teacher identifiers:** `siswa.nis` (`NOT NULL UNIQUE`), `siswa.nisn` (`UNIQUE`), `guru.nip` (`NOT NULL UNIQUE`), and `idx_guru_email_unik` (`LOWER(email) WHERE email IS NOT NULL`, blank inputs normalized to `SQL NULL` by `JdbcUtils.setNullableString`).
- **Semester & assignments:** `penugasan_mengajar` requires `id_semester`; `penugasan_wali_kelas` allows at most one active homeroom teacher per class (`idx_wali_kelas_aktif`).
- **Intra-year transfers:** Supported atomically via `AcademicDAO.transitionStudentPlacement`, updating `tanggal_keluar` and `status` (`NAIK_KELAS`/`PINDAH`) and creating the next active `riwayat_kelas_siswa` row inside `JdbcUtils.inTransactionVoid`.
- **Attendance & scores:** `absensi` references `siswa(id)`, `kelas(id)`, and optional `pengguna(id)` (`dicatat_oleh`); `nilai_siswa` enforces `0 <= nilai <= nilai_maksimal` at Service validation and `uq_nilai_siswa_penilaian` at DB level.
- **Schema management:** Manual SQL scripts (`database/schema.sql` and idempotent `database/seed.sql`).