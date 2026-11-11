# Implementation Status Tracker — Sistem Manajemen Kelas SD

**Last Updated:** 2026-11-11 (Swing Desktop UI + Documentation Synchronization Completed)  
**Current Phase:** **Final Delivery Ready** — **47/47 Tests Passing (`mvn clean test` & `mvn package`)**

---

## 1. Phase Progress Overview

| Phase | Name | Status | Progress | Notes |
|---|---|---|---|---|
| **Phase 0** | Repository Audit & Onboarding | **COMPLETED** | 100% | Full codebase, schema, and git working tree inspected. |
| **Phase 1** | Authentication & Security Foundation | **COMPLETED** | 100% | `PasswordHasher`, `RoleMapper`, `User`, `InMemoryAuthDAO`, `PostgresAuthDAO`, `AuthBootstrapSeeder`, `AuthService`, and auth tests complete. |
| **Phase 2** | Core Master Data PostgreSQL Migration | **COMPLETED** | 100% | `DatabaseConfig`, `GenderMapper`, `JdbcUtils`, `PostgresStudentDAO`, `PostgresTeacherDAO`, `PostgresAcademicDAO`, DB-generated identity IDs, `CliApplication` wired to PostgreSQL with InMemory fallback. |
| **Phase 3** | Extended Academic Operations | **COMPLETED** | 100% | `Semester`, `HomeroomAssignment`, `TeachingAssignment`, `StudentPlacement` models, `AcademicDAO`/`InMemoryAcademicDAO`/`PostgresAcademicDAO`, `AcademicService` (including transactional student promotion/transfer and ADMIN authorization checks), `AcademicMenu`/`StudentMenu`/`TeacherMenu` CLI integration, and PostgreSQL integration tests passing. |
| **Phase 4** | Classroom Operations (Attendance & Assessment) | **COMPLETED** | 100% | **Attendance COMPLETED** (`Attendance`, `AttendanceSummary`, `AttendanceDAO`, `InMemoryAttendanceDAO`, `PostgresAttendanceDAO`, `AttendanceService`, `AttendanceMenu`, 4 tests). **Assessment & Student Scores COMPLETED** (`Assessment`, `StudentScore`, `AssessmentSummary`, `StudentScoreSummary`, `AssessmentDAO`, `InMemoryAssessmentDAO`, `PostgresAssessmentDAO`, `AssessmentService`, `AssessmentMenu`, 4 tests). |
| **Phase 5** | Dashboard, Reports & Role-Based CLI | **COMPLETED** | 100% | `DashboardSummary`, `DashboardService`, report models, `ReportDAO`/`InMemoryReportDAO`/`PostgresReportDAO` (SQL aggregation), `ReportService`, `ReportMenu`, role-scoped visibility (`ADMIN` vs `TEACHER`), `MainMenu`, `MenuNavigator`, `CliApplication`, and full role-scoped CLI/Service access control complete. |
| **Phase 6** | Unit, Integration & Regression Testing | **COMPLETED** | 100% | **41/41 tests passing across 13 test suites** (`mvn clean test` & `mvn package` against PostgreSQL 18 + InMemory unit tests), including [`FullQaStabilizationIntegrationTest`](../src/test/java/id/ac/sekolah/manajemenkelas/qa/FullQaStabilizationIntegrationTest.java) and [`CliWorkflowAndAccessControlIntegrationTest`](../src/test/java/id/ac/sekolah/manajemenkelas/presentation/cli/CliWorkflowAndAccessControlIntegrationTest.java). |
| **Phase 7** | QA Stabilization, Docs & Delivery Prep | **COMPLETED** | 100% | Full QA stabilization, synchronized Indonesian PostgreSQL documentation, reproducible `database/seed.sql`, `application.properties.example`, `.gitignore` hygiene, and `mvn clean test` / `mvn package` verification completed. |

---

## 2. Module & File Inventory Matrix

### A. Core & Configuration (`config`, `core`)
| Component / File | Status | Notes |
|---|---|---|
| `pom.xml`, `application.properties`, `AppConfig.java` | **COMPLETED** | Build, DB, and PBKDF2 security configuration. |
| `DatabaseConfig.java`, `JdbcUtils.java`, `GenderMapper.java`, `PasswordHasher.java` | **COMPLETED** | Connection management, transactions, nullable normalization, gender mapping, PBKDF2 hashing. |
| `SessionContext.java` | **COMPLETED** | Stores `userId`, `teacherId`, `username`, `fullName`, `role`. |

### B. Authentication (`auth`)
| Component / File | Status | Notes |
|---|---|---|
| `User`, `RoleMapper`, `AuthDAO`, `InMemoryAuthDAO`, `PostgresAuthDAO`, `AuthService`, `AuthBootstrapSeeder` | **COMPLETED** | Full PBKDF2 authentication & PostgreSQL persistence (`pengguna`). |

### C. Student & Teacher (`student`, `teacher`)
| Component / File | Status | Notes |
|---|---|---|
| `Student`, `StudentDAO`, `InMemoryStudentDAO`, `PostgresStudentDAO`, `StudentService`, `StudentMenu` | **COMPLETED** | CRUD, deactivation, search, ADMIN write guard, and class placement history viewer. |
| `Teacher`, `TeacherDAO`, `InMemoryTeacherDAO`, `PostgresTeacherDAO`, `TeacherService`, `TeacherMenu` | **COMPLETED** | CRUD, deactivation, search, ADMIN write guard, nullable email normalization. |

### D. Academic (`academic`)
| Component / File | Status | Notes |
|---|---|---|
| `AcademicYear`, `Semester`, `SchoolClass`, `Subject`, `HomeroomAssignment`, `TeachingAssignment`, `StudentPlacement` | **COMPLETED** | All 7 academic domain models implemented with DB-compatible fields & IDs. |
| `AcademicDAO`, `InMemoryAcademicDAO`, `PostgresAcademicDAO` | **COMPLETED** | Covers all 7 academic tables (`tahun_ajaran`, `semester`, `kelas`, `mata_pelajaran`, `penugasan_wali_kelas`, `penugasan_mengajar`, `riwayat_kelas_siswa`) including transactional `transitionStudentPlacement`. |
| `AcademicService` | **COMPLETED** | Full validation (semester range & number, overlapping active homeroom prevention, compatible academic year for teaching assignments, capacity & single active placement enforcement, ADMIN authorization guard). |
| `AcademicMenu` | **COMPLETED** | Interactive CLI menus for all 7 academic sub-modules. |

### E. Attendance (`attendance`), Assessment (`assessment`), Dashboard & Reports (`dashboard`, `report`)
| Component / File | Status | Notes |
|---|---|---|
| `Attendance`, `AttendanceSummary`, `AttendanceDAO`, `InMemoryAttendanceDAO`, `PostgresAttendanceDAO`, `AttendanceService`, `AttendanceMenu` | **COMPLETED** | Daily & batch transactional recording, updates, placement-date validation, role/teacher-assignment authorization, date-range class summaries, and CLI menu integration. |
| `Assessment`, `StudentScore`, `AssessmentSummary`, `StudentScoreSummary`, `AssessmentDAO`, `InMemoryAssessmentDAO`, `PostgresAssessmentDAO`, `AssessmentService`, `AssessmentMenu` | **COMPLETED** | Assessment CRUD (`KUIS`/`TUGAS`/`UTS`/`UAS`/`PRAKTIK`/`PROYEK`/`LAINNYA`), semester date & max-score validation, single & transactional batch scores (`0 <= nilai <= nilai_maksimal`), student-class verification, assessment & student summaries, teacher-assignment authorization, and CLI integration. |
| `DashboardSummary`, `DashboardService`, `ClassStudentCountReport`, `GenderStudentCountReport`, `AttendanceRangeReport`, `ClassAttendanceReport`, `AssessmentReport`, `StudentScoreReport`, `SubjectScoreReport`, `ReportDAO`, `InMemoryReportDAO`, `PostgresReportDAO`, `ReportService`, `ReportMenu` | **COMPLETED** | SQL aggregations in `PostgresReportDAO` + `InMemoryReportDAO` fallback, division-by-zero & empty-dataset protection, deterministic ordering, and role-based data visibility (`ADMIN` vs `TEACHER`). |

### F. Presentation CLI, Swing Desktop UI & Access Control (`presentation.cli`, `presentation.swing`)
| Component / File | Status | Notes |
|---|---|---|
| `Main.java`, `CliApplication.java`, `MainMenu.java`, `MenuNavigator.java`, `AuthMenu.java` | **COMPLETED** | Default Swing desktop launch with `--cli`/headless fallback, role-based main menus (`ADMIN` vs `TEACHER`), safe input parsing, clean session logout (`SessionContext`). |
| `StudentMenu.java`, `TeacherMenu.java`, `AcademicMenu.java`, `AttendanceMenu.java`, `AssessmentMenu.java`, `ReportMenu.java` | **COMPLETED** | Every CLI menu option wired to real Services; role-hidden/guarded admin options, teacher-scoped views, and readable empty/error output. |
| `SwingApplication.java`, `MainFrame.java`, `AppShellPanel.java`, `SidebarPanel.java`, `BotanicalTheme.java`, `BotanicalIcons.java`, `ApplicationContainer.java`, `presentation/swing/auth/*`, `component/*`, `module/*` | **COMPLETED** | Complete Java Swing desktop UI with Claymorphism Botanical Green design system, custom reusable components & Java2D charts, asynchronous `SwingWorker` loading (`AsyncTaskRunner`), role-scoped navigation (`ADMIN` & `TEACHER`), and end-to-end PostgreSQL + `InMemory` fallback integration. |

---

## 3. Database Table Mapping Matrix (`database/schema.sql`)

| # | PostgreSQL Table | Java Model | DAO Interface | Postgres DAO Status |
|---|---|---|---|---|
| 1 | `guru` | `Teacher` | `TeacherDAO` | **COMPLETED (Phase 2)** |
| 2 | `pengguna` | `User` | `AuthDAO` | **COMPLETED (Phase 1)** |
| 3 | `siswa` | `Student` | `StudentDAO` | **COMPLETED (Phase 2)** |
| 4 | `tahun_ajaran` | `AcademicYear` | `AcademicDAO` | **COMPLETED (Phase 2)** |
| 5 | `semester` | `Semester` | `AcademicDAO` | **COMPLETED (Phase 3)** |
| 6 | `kelas` | `SchoolClass` | `AcademicDAO` | **COMPLETED (Phase 2)** |
| 7 | `mata_pelajaran` | `Subject` | `AcademicDAO` | **COMPLETED (Phase 2)** |
| 8 | `penugasan_wali_kelas` | `HomeroomAssignment` | `AcademicDAO` | **COMPLETED (Phase 3)** |
| 9 | `penugasan_mengajar` | `TeachingAssignment` | `AcademicDAO` | **COMPLETED (Phase 3)** |
| 10 | `riwayat_kelas_siswa` | `StudentPlacement` | `AcademicDAO` | **COMPLETED (Phase 3)** |
| 11 | `absensi` | `Attendance` | `AttendanceDAO` | **COMPLETED (Phase 4)** |
| 12 | `penilaian` | `Assessment` | `AssessmentDAO` | **COMPLETED (Phase 5)** |
| 13 | `nilai_siswa` | `StudentScore` | `AssessmentDAO` | **COMPLETED (Phase 5)** |

---

## 4. QA Verification Summary & Known Limitations

### Latest Verification Results (`2026-11-11`)
- **`mvn clean test` & `mvn package`:** **47/47 tests passing, 0 failures, 0 errors, 0 skipped** across **14 test suites** (unit tests + live PostgreSQL 18 integration tests + Swing UI integration tests), packaged to `target/sistem-manajemen-kelas-sd-1.0.0-SNAPSHOT.jar`.
- **Security & Resource Audit:** All DAO JDBC `Connection`/`PreparedStatement`/`ResultSet` instances use try-with-resources and parameterized `?` bindings (zero `java.sql.Statement` concatenation/SQL injection); passwords use PBKDF2WithHmacSHA256 (600,000 iterations, 16-byte random salt, Base64, constant-time `MessageDigest.isEqual`, `PBEKeySpec.clearPassword()`).

### Known Limitations & Repository Notes
1. **Local Development Credentials (`application.properties` & `AppConfig.java`):** `src/main/resources/application.properties` is excluded from Git via `.gitignore`, with `src/main/resources/application.properties.example` committed as the template for local PostgreSQL setup. `AppConfig.java` retains demo bootstrap credentials (`admin`/`admin123`, `guru`/`guru123`) hashed on seed via `AuthBootstrapSeeder`.
2. **Demo `guru` Account Teacher Profile Scope:** The default seeded `"guru"` account has `id_guru = NULL` in `pengguna` until linked to a record in `guru`; teacher-scoped features (`DashboardService`, `ReportService`, `AttendanceService`, `AssessmentService`) intentionally reject `TEACHER` accounts without a linked `teacherId`.
3. **Seed SQL & Standalone Utility File:** `database/seed.sql` provides idempotent academic demonstration seed data verified twice cleanly against PostgreSQL 18; `AuthBootstrapSeeder` handles PBKDF2-HMAC-SHA256 user credential seeding on application startup. `Main.java` defaults to launching the Swing Desktop UI ([`SwingApplication`](../src/main/java/id/ac/sekolah/manajemenkelas/presentation/swing/SwingApplication.java)) and falls back to `CliApplication` on `--cli` or headless environments. `DatabaseConnectionTest.java` remains in `src/main/java` as a standalone manual JDBC connection check.
4. **InMemory vs PostgreSQL Report Rounding Nuance:** Under repeating decimals, `InMemoryReportDAO.findAssessmentSummaries` rounds average score before percentage calculation while `PostgresReportDAO` divides unrounded SQL `AVG`, which may differ by `0.01%`; `PostgresReportDAO.findAttendanceSummariesByClass` filters by date only when both `startDate` and `endDate` are provided.
5. **Unauthenticated Setup Calls in Core CRUD Services:** `StudentService`, `TeacherService`, and `AcademicService` enforce `ADMIN` authorization whenever a user is logged in (`requireAdminIfAuthenticated`), while allowing unauthenticated execution so standalone service/integration test setups remain compatible.
6. **Git Ignore Quirk:** `.gitignore` lists bare filenames (`SYSTEM_DESIGN.md`, `DATABASE.md`, `PRD.md`, `TEAM_WORKFLOW.md`, `DOMAIN_MODEL.md`), causing Git to ignore matching filenames inside `docs/`.
