# Implementation Roadmap — Sistem Manajemen Kelas SD

**Project:** Sistem Manajemen Kelas SD  
**Package Root:** `id.ac.sekolah.manajemenkelas`  
**Database:** PostgreSQL 18 (`jdbc:postgresql://localhost:5432/manajemen-sd`)  
**Build & Test:** Maven (`Java 17` release target on `JDK 25`, JUnit Jupiter `5.11.4`, PostgreSQL JDBC `42.7.7`)

---

## 1. Executive Summary & Strategy

The application follows a **modular monolith** and **layered architecture** (`Presentation -> Service -> DAO Interface -> InMemory / PostgreSQL DAO -> DatabaseConfig -> PostgreSQL`).

Implementation follows a **vertical-slice** approach across 7 structured phases:
1. Preserve all working `InMemory*DAO` implementations as fast unit-testable fallbacks.
2. Complete each feature slice end-to-end (`Model -> DAO Interface -> InMemory/Postgres DAO -> Service -> CLI -> Tests`) before moving to the next phase.
3. Enforce PostgreSQL 18 schema constraints (`database/schema.sql`, 13 Indonesian-named tables) at both the Service validation boundary and the JDBC persistence boundary.

---

## 2. Phase Dependency Graph

```
[Phase 1: Auth & Security Foundation]
        │
        ▼
[Phase 2: Core Master Data PostgreSQL Migration (Student, Teacher, Academic)]
        │
        ▼
[Phase 3: Extended Academic Operations (Semester, Homeroom, Teaching Assignment, Student Placement)]
        │
        ▼
[Phase 4: Classroom Operations (Attendance, Assessment, Student Score)]
        │
        ▼
[Phase 5: Dashboard, Reports & Full Role-Based CLI Integration]
        │
        ▼
[Phase 6: Comprehensive Unit, Integration & Regression Testing]  [COMPLETED]
        │
        ▼
[Phase 7: QA Stabilization, Documentation Synchronization, Seed Data & Delivery Preparation]  [COMPLETED]
```

---

## 3. Detailed Phase Plan

### PHASE 1: Authentication & Security Foundation

**Objective:** Fix the current compilation error in `PasswordHasher.java`, verify existing authentication unit tests, and complete PostgreSQL persistence and bootstrap seeding for `pengguna`.

#### Tasks
1. **Fix `PasswordHasher.java` Compilation Error:** Remove the duplicated class definition (lines 143–285) in `src/main/java/id/ac/sekolah/manajemenkelas/core/security/PasswordHasher.java`.
2. **Verify Existing Unit Tests:** Run `PasswordHasherTest` and `AuthServiceAndRoleMapperTest` via `mvn test` (note: with `600,000` PBKDF2 iterations, ensure test execution time is reasonable or uses constructor parameters where appropriate while testing default config loading).
3. **Create `PostgresAuthDAO`:** Implement `AuthDAO` against table `pengguna` using `DatabaseConfig`, `PreparedStatement`, `Statement.RETURN_GENERATED_KEYS` / `RETURNING id`, and `RoleMapper` (`ADMIN` <-> `ADMIN`, `TEACHER` <-> `GURU`).
4. **Create `AuthBootstrapSeeder`:** Ensure default accounts (`admin` / `admin123` and `guru` / `guru123`, plus linked demo `guru` profile if needed) exist in PostgreSQL with PBKDF2 hashed passwords and salts idempotently on startup.
5. **Create Authentication Integration Tests:** Test `PostgresAuthDAO` and `AuthBootstrapSeeder` against PostgreSQL (`manajemen-sd`).

#### Dependencies
- `DatabaseConfig`, `application.properties`, `database/schema.sql` (`pengguna`, `guru`).

#### Files Affected
- **Modify:** `src/main/java/id/ac/sekolah/manajemenkelas/core/security/PasswordHasher.java`
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/auth/dao/PostgresAuthDAO.java`
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/auth/service/AuthBootstrapSeeder.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/auth/PostgresAuthDAOTest.java`

#### Acceptance Criteria & Test Requirements
- `mvn clean test` compiles cleanly and passes all unit + integration tests.
- Default admin and teacher logins work against PostgreSQL with PBKDF2 password verification.
- `terakhir_login` updates on successful authentication.
- Role mapping (`TEACHER` <-> `GURU`) is transparent to callers of `AuthService` and `SessionContext`.

---

### PHASE 2: Core Master Data PostgreSQL Migration (Student, Teacher, Academic)

**Objective:** Introduce shared JDBC/mapper helpers, migrate Student, Teacher, and core Academic (`AcademicYear`, `SchoolClass`, `Subject`) persistence to PostgreSQL, support database-generated IDs, and wire `CliApplication` to PostgreSQL with fallback support.

#### Tasks
1. **Shared Mappers & JDBC Helpers:**
   - Create `GenderMapper` (`L` <-> `LAKI_LAKI`, `P` <-> `PEREMPUAN`) in `common` or `core.util`.
   - Create string/nullable normalization utility (converting blank optional strings like `""` to SQL `NULL`, critical for `idx_guru_email_unik` and optional `siswa.nisn`).
   - Add transaction execution helper in `DatabaseConfig` or `core.util.TransactionManager` for multi-step operations.
2. **Model & ID Generation Alignment:**
   - Add `setId(Long id)` to `Student`, `Teacher`, `AcademicYear`, `SchoolClass`, and `Subject` so PostgreSQL `RETURNING id` can populate generated primary keys without breaking `InMemory*DAO`.
   - Update `StudentService`, `TeacherService`, and `AcademicService` so ID generation works seamlessly with both `Postgres*DAO` (DB-generated identity) and `InMemory*DAO`.
   - Add activation/reactivation support where needed (`activateStudent`, `activateTeacher`, `activateAcademicYear`, etc.) and handle single-active-academic-year constraint (`idx_tahun_ajaran_aktif`).
3. **Implement `PostgresStudentDAO`:** Map `Student` <-> `siswa` (including `GenderMapper` and nullable `nisn` if blank, though service currently validates `nisn`).
4. **Implement `PostgresTeacherDAO`:** Map `Teacher` <-> `guru` (including `GenderMapper` and normalizing blank `email` to `NULL`).
5. **Implement `PostgresAcademicDAO`:** Map `AcademicYear` <-> `tahun_ajaran`, `SchoolClass` <-> `kelas`, `Subject` <-> `mata_pelajaran`. When activating a new `tahun_ajaran`, ensure previous active year is deactivated within a transaction to satisfy `idx_tahun_ajaran_aktif`.
6. **CLI Dependency Wiring:** Update `CliApplication` to use PostgreSQL DAOs and run `AuthBootstrapSeeder` on startup (with clean error handling).

#### Dependencies
- Phase 1 complete.

#### Files Affected
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/core/util/GenderMapper.java`
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/core/util/JdbcUtils.java`
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/student/dao/PostgresStudentDAO.java`
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/teacher/dao/PostgresTeacherDAO.java`
- **Create:** `src/main/java/id/ac/sekolah/manajemenkelas/academic/dao/PostgresAcademicDAO.java`
- **Modify:** `Student.java`, `Teacher.java`, `AcademicYear.java`, `SchoolClass.java`, `Subject.java`
- **Modify:** `StudentService.java`, `TeacherService.java`, `AcademicService.java`, `CliApplication.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/student/StudentServiceIntegrationTest.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/teacher/TeacherServiceIntegrationTest.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/academic/AcademicServiceIntegrationTest.java`

#### Acceptance Criteria & Test Requirements
- All CRUD operations for `siswa`, `guru`, `tahun_ajaran`, `kelas`, and `mata_pelajaran` persist in PostgreSQL and assign database-generated `BIGINT` IDs.
- Multiple teachers with empty emails can be saved without violating `idx_guru_email_unik`.
- Gender `L`/`P` maps cleanly to/from `LAKI_LAKI`/`PEREMPUAN`.
- `InMemory*DAO` implementations remain functional.

---

### PHASE 3: Extended Academic Operations (Semester, Homeroom, Teaching Assignment, Student Placement)

**Objective:** Implement the 4 remaining academic/placement domain tables (`semester`, `penugasan_wali_kelas`, `penugasan_mengajar`, `riwayat_kelas_siswa`) across Model, DAO, Service, CLI, and Tests.

#### Tasks
1. **Models:**
   - `Semester` (`id`, `academicYearId`, `semesterNumber` [1|2], `name`, `startDate`, `endDate`, `active`)
   - `HomeroomAssignment` (`id`, `classId`, `teacherId`, `startDate`, `endDate`, `active`)
   - `TeachingAssignment` (`id`, `teacherId`, `classId`, `subjectId`, `semesterId`, `active`)
   - `StudentPlacement` (`id`, `studentId`, `classId`, `academicYearId`, `entryDate`, `exitDate`, `status` [`AKTIF`, `NAIK_KELAS`, `PINDAH`, `LULUS`, `KELUAR`])
2. **DAO Contracts & Implementations:**
   - Extend `AcademicDAO` (or add focused methods) in both `InMemoryAcademicDAO` and `PostgresAcademicDAO` for `Semester`, `HomeroomAssignment`, `TeachingAssignment`, and `StudentPlacement`.
   - Enforce database integrity rules:
     - `uq_semester_tahun_ajaran (id_tahun_ajaran, nomor_semester)`
     - `idx_wali_kelas_aktif (id_kelas) WHERE aktif = TRUE` (deactivate prior active homeroom teacher for a class when assigning a new one, or reject duplicates clearly)
     - `uq_penugasan_mengajar (id_guru, id_kelas, id_mata_pelajaran, id_semester)`
     - `fk_riwayat_kelas_tahun (id_kelas, id_tahun_ajaran) REFERENCES kelas(id, id_tahun_ajaran)`
     - `idx_riwayat_siswa_aktif (id_siswa) WHERE status = 'AKTIF' AND tanggal_keluar IS NULL`
3. **Service Business Logic:**
   - Extend `AcademicService` (and/or coordinate with `StudentService`) to manage semesters, assign homeroom teachers, assign subject teachers, place students into classes (validating class capacity and active status), update placement status/history, and query placements by class or student.
4. **CLI Integration:**
   - Update `AcademicMenu` to include Semester Management, Homeroom Teacher Assignment, Teaching Assignment, and Student Class Placement.
   - Update `StudentMenu` to allow viewing a student's class placement history and placing a student into a class.

#### Dependencies
- Phase 2 complete.

#### Files Affected
- **Create:** `Semester.java`, `HomeroomAssignment.java`, `TeachingAssignment.java`, `StudentPlacement.java` in `academic.model`
- **Modify:** `AcademicDAO.java`, `InMemoryAcademicDAO.java`, `PostgresAcademicDAO.java`, `AcademicService.java`
- **Modify:** `AcademicMenu.java`, `StudentMenu.java`, `CliApplication.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/academic/AcademicAssignmentAndPlacementTest.java`

#### Acceptance Criteria & Test Requirements
- Admin can create semesters (1 and 2) for an academic year.
- Admin can assign a homeroom teacher (`penugasan_wali_kelas`) and subject teaching assignments (`penugasan_mengajar`).
- Admin can place students into classes (`riwayat_kelas_siswa`), view class rosters, and view a student's historical placements without overwriting past records.

---

### PHASE 4: Classroom Operations (Attendance, Assessment, Student Score)

**Objective:** Implement full vertical slices for `absensi`, `penilaian`, and `nilai_siswa`, including validation, transactions, and CLI menus.

#### Tasks
1. **Attendance Module (`attendance`):**
   - **Model:** `AttendanceRecord` (`id`, `studentId`, `classId`, `attendanceDate`, `status` [`HADIR`, `SAKIT`, `IZIN`, `ALPA`], `notes`, `recordedBy`).
   - **DAO:** `AttendanceDAO`, `InMemoryAttendanceDAO`, `PostgresAttendanceDAO` (supporting upsert/update and queries by class+date and by student).
   - **Service:** `AttendanceService` (validating status in `HADIR`/`SAKIT`/`IZIN`/`ALPA`, checking student placement in class, enforcing `uq_absensi_siswa_tanggal`, supporting batch/transactional class attendance recording, and calculating attendance recapitulation).
   - **CLI:** Implement `AttendanceMenu` (record attendance per class/student, view attendance by class & date, view attendance by student, update attendance, view attendance summary).
2. **Assessment & Score Module (`assessment`):**
   - **Models:**
     - `Assessment` (`id`, `teachingAssignmentId`, `title`, `assessmentType` [`KUIS`, `TUGAS`, `UTS`, `UAS`, `PRAKTIK`, `PROYEK`, `LAINNYA`], `assessmentDate`, `maxScore` [`BigDecimal`], `description`).
     - `StudentScore` (`id`, `assessmentId`, `studentId`, `score` [`BigDecimal`], `notes`, `recordedBy`).
   - **DAO:** `AssessmentDAO`, `InMemoryAssessmentDAO`, `PostgresAssessmentDAO`.
   - **Service:** `AssessmentService` (validating assessment types against DB CHECK constraint, ensuring `maxScore > 0`, validating `0 <= score <= maxScore`, enforcing `uq_nilai_siswa_penilaian`, and computing score summaries).
   - **CLI:** Implement `AssessmentMenu` (create assessment, list assessments by teaching assignment/class, input/update student scores, view assessment scores and averages).

#### Dependencies
- Phase 3 complete (`TeachingAssignment` and `StudentPlacement` required for assessments and attendance).

#### Files Affected
- **Create:** `AttendanceRecord.java`, `AttendanceDAO.java`, `InMemoryAttendanceDAO.java`, `PostgresAttendanceDAO.java`, `AttendanceService.java`
- **Create:** `Assessment.java`, `StudentScore.java`, `AssessmentDAO.java`, `InMemoryAssessmentDAO.java`, `PostgresAssessmentDAO.java`, `AssessmentService.java`
- **Modify:** `AttendanceMenu.java`, `AssessmentMenu.java`, `CliApplication.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/attendance/AttendanceServiceTest.java`
- **Create:** `src/test/java/id/ac/sekolah/manajemenkelas/assessment/AssessmentServiceTest.java`

#### Acceptance Criteria & Test Requirements
- Attendance records persist in `absensi` and enforce uniqueness per `(id_siswa, tanggal_absensi)`.
- Assessments persist in `penilaian` with valid `jenis_penilaian` and `nilai_maksimal > 0`.
- Student scores persist in `nilai_siswa` with `0 <= nilai <= nilai_maksimal` and uniqueness per `(id_penilaian, id_siswa)`.
- `AttendanceMenu` and `AssessmentMenu` are fully interactive and connected in `CliApplication`.

---

### PHASE 5: Dashboard, Reports & Role-Based CLI Integration

**Objective:** Complete the Dashboard and Report modules and enforce clear role-based authorization (`ADMIN` vs `TEACHER`) across Services and CLI navigation.

#### Tasks
1. **Dashboard Module (`dashboard`):**
   - Create `DashboardSummary` model and `DashboardService` (total active/inactive students, total active/inactive teachers, active academic year & semester, total classes, total subjects, teacher-specific assignment counts when logged in as `TEACHER`).
2. **Report Module (`report`):**
   - Create report models (`StudentReportSummary`, `ClassSummary`, `AttendanceRecap`, `AssessmentRecap`) and `ReportService` aggregating student, teacher, class, attendance, and assessment/score data.
   - Implement `ReportMenu` for viewing Dashboard Summary, Student Summary, Teacher Summary, Class Summary, Attendance Recapitulation, and Assessment/Score Recapitulation.
3. **Role-Based CLI Navigation & Authorization:**
   - Update `SessionContext` and `AuthService` to also carry `teacherId` (`id_guru`) when a `TEACHER` logs in.
   - Update `CliApplication` main menu to expose all modules (`1. Dashboard`, `2. Manajemen Siswa`, `3. Manajemen Guru`, `4. Struktur Akademik & Penugasan`, `5. Absensi Harian`, `6. Penilaian & Nilai Siswa`, `7. Laporan & Rekapitulasi`, `8. Informasi Sesi / Profil`, `9. Logout`, `0. Keluar`).
   - Enforce role permissions (`ADMIN` manages master data & assignments; `TEACHER` views academic data and manages attendance, assessments, scores, and reports).

#### Dependencies
- Phases 1–4 complete.

#### Files Affected
- **Create:** `DashboardSummary.java`, `DashboardService.java` in `dashboard`
- **Create:** `ReportService.java` and summary models in `report`
- **Modify:** `ReportMenu.java`, `AuthMenu.java`, `CliApplication.java`, `SessionContext.java`, `AuthService.java`

#### Acceptance Criteria & Test Requirements
- Both `ADMIN` and `TEACHER` roles see appropriate menus and authorization checks.
- Every menu in `presentation.cli` has real, working functionality with zero empty placeholder menus.

---

### PHASE 6: Comprehensive Unit, Integration & Regression Testing

**Objective:** Verify the entire application with automated JUnit 5 tests (both InMemory unit tests and PostgreSQL integration tests) and ensure `mvn clean test` and `mvn package` succeed reliably.

#### Tasks
1. Execute full test suite (`mvn clean test`) covering:
   - Authentication (valid login, invalid login, inactive account, PBKDF2 hash/salt, role mapping).
   - Student, Teacher, Academic CRUD + constraints (unique NIS/NISN/NIP, blank email normalization, gender mapping, single active academic year).
   - Semester, Homeroom, Teaching Assignment, Student Placement (historical preservation, active placement uniqueness).
   - Attendance (status validation, duplicate prevention, update, recap).
   - Assessment & Student Score (max score validation, duplicate prevention, update, recap).
   - Dashboard & Report generation.
2. Verify `mvn package` builds the application artifact cleanly.

#### Dependencies
- Phases 1–5 complete.

#### Acceptance Criteria
- `mvn clean test` passes 100% with 0 compilation errors and 0 test failures.
- `mvn package` succeeds.

---

### PHASE 7: QA Stabilization, Documentation, Seed Data & Delivery Preparation *(Completed)*

**Objective:** Perform full QA stabilization, synchronize project documentation with the Indonesian PostgreSQL schema, populate `database/seed.sql` for instant academic demonstration, fix `.gitignore` & secret hygiene, and complete final delivery verification.

#### Tasks
1. **Full QA & Stabilization (Phase 08):** Fix `DatabaseConfig` connection leak edge case, harden CLI date/EOF input handling, translate SQLState `23505`/`23503`/`22001` cleanly, align service/InMemory parity, and verify full automated tests (`mvn clean test` & `mvn package`). *(Completed)*
2. **Populate `database/seed.sql`:** Provide deterministic, idempotent demo data (`guru`, `siswa`, `tahun_ajaran`, `semester`, `kelas`, `mata_pelajaran`, `penugasan_wali_kelas`, `penugasan_mengajar`, `riwayat_kelas_siswa`, `absensi`, `penilaian`, and `nilai_siswa`; credentials seeded via `AuthBootstrapSeeder` PBKDF2-HMAC-SHA256). *(Completed & verified idempotent on PostgreSQL 18)*
3. **Update Documentation:**
   - `README.md`: Complete setup, configuration, build, test, and run instructions (NetBeans & Maven CLI), demo accounts, and troubleshooting. *(Completed)*
   - `docs/database/DATABASE.md`: Updated table and column documentation to match the authoritative 13 Indonesian tables in `database/schema.sql`. *(Completed)*
   - `docs/architecture/SYSTEM_DESIGN.md`, `docs/domain/DOMAIN_MODEL.md`, `docs/product/PRD.md`, `docs/TEAM_WORKFLOW.md`: Synchronized with implemented classes, mappings, and CLI delivery scope. *(Completed)*
4. **Fix `.gitignore` & Secret Hygiene:** Ensure documentation files inside `docs/` are tracked properly instead of being ignored by bare filename patterns, provide `src/main/resources/application.properties.example`, and exclude local `src/main/resources/application.properties`. *(Completed)*

#### Dependencies
- Phases 1–6 complete.

#### Acceptance Criteria
- All documentation matches the actual codebase and `database/schema.sql`.
- `database/seed.sql` runs cleanly and idempotently on PostgreSQL 18.
- `mvn clean test` and `mvn package` pass without failures or skipped tests.

