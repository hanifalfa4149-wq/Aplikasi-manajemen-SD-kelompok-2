# System Design
# Sistem Manajemen Kelas SD

**Document Status:** Synchronized with Final CLI Delivery  
**Version:** 2.0

---

## 1. Architecture Overview

The application uses a modular monolith architecture with layered separation.

The application is deployed as one desktop application. Internally, its features are separated into cohesive business modules.

The architecture prioritizes:

- Clear separation of responsibilities.
- Reusable business logic.
- Simple integration between modules.
- Maintainability within a four-person development team.
- Compatibility with Java 25 and PostgreSQL.
- No application frameworks.

## 2. Technology Stack

| Concern | Technology |
|---|---|
| Programming language | Java 17 target bytecode (developed & tested on JDK 25) |
| Active UI | Interactive Text CLI (`presentation.cli.*`); `presentation.swing.MainFrame` is an unused template stub |
| Database | PostgreSQL 18 (`manajemen-sd`) |
| Database connectivity | JDBC (`DatabaseConfig` + `JdbcUtils`) |
| Database driver | `org.postgresql:postgresql:42.7.7` |
| Build tool | Apache Maven (`maven-compiler-plugin:3.13.0`, `exec-maven-plugin:3.5.0`) |
| Testing | JUnit Jupiter `5.11.4` (`mvn clean test`, 41 tests across 13 suites) |

Do not introduce frameworks without explicit project approval.

## 3. High-Level Architecture

**Diagram:**

    +--------------------------------------------------+
    |                 Presentation                     |
    |                                                  |
    |          CLI                 Java Swing           |
    +----------------------------+---------------------+
                                 |
                                 v
    +--------------------------------------------------+
    |                   Service Layer                  |
    |                                                  |
    | Auth | Student | Teacher | Academic | Attendance  |
    | Assessment | Dashboard | Report                  |
    +--------------------------------------------------+
                                 |
                                 v
    +--------------------------------------------------+
    |                     DAO Layer                    |
    |                                                  |
    | JDBC repositories / data access objects          |
    +--------------------------------------------------+
                                 |
                                 v
    +--------------------------------------------------+
    |                   PostgreSQL                     |
    +--------------------------------------------------+

Both presentation interfaces must reuse the same Service layer.

## 4. Layer Responsibilities

### 4.1 Presentation Layer

**Packages:**

- `presentation.cli`
- `presentation.swing`

**Responsibilities:**

- Collect user input.
- Display information.
- Handle navigation.
- Call Service methods.
- Present validation and error messages.

Presentation code must not contain business rules or direct SQL operations.

### 4.2 Service Layer

**Packages:**

- `auth.service`
- `student.service`
- `teacher.service`
- `academic.service`
- `attendance.service`
- `assessment.service`
- `dashboard.service`
- `report.service`

**Responsibilities:**

- Implement business rules.
- Validate business operations.
- Enforce authorization.
- Coordinate operations across DAOs.
- Manage transactions for multi-step operations.
- Return application-level results or throw meaningful exceptions.

Service methods should represent meaningful business operations rather than merely mirror every DAO method.

### 4.3 DAO Layer

**Responsibilities:**

- Execute SQL statements.
- Map database records into Java objects.
- Persist and retrieve data.
- Handle JDBC resource lifecycle.
- Expose persistence operations to Services.

DAO classes must not decide whether a user is authorized to perform a business operation.

### 4.4 Model Layer

**Responsibilities:**

- Represent domain entities and value objects.
- Hold domain-related state.
- Provide meaningful constructors and accessors.
- Avoid embedding UI or database connection logic.

Do not turn every model into a general-purpose data container when stronger domain modeling is appropriate.

## 5. Module Boundaries

### Authentication (`auth`)

**Owns:**

- User authentication.
- User accounts.
- Password verification.
- Role information.
- Session identity.

### Student (`student`)

**Owns:**

- Student records.
- Student lifecycle and status.
- Student profile information.
- Student-related queries.

Student class placement history is coordinated with the academic module.

### Teacher (`teacher`)

**Owns:**

- Teacher records.
- Teacher profile information.
- Teacher lifecycle and status.

### Academic (`academic`)

**Owns:**

- Academic years.
- Semesters.
- Classes.
- Subjects.
- Teaching assignments.
- Homeroom assignments.
- Student placement history or its agreed integration boundary.

### Attendance (`attendance`)

**Owns:**

- Daily attendance records.
- Attendance status validation.
- Attendance queries and summaries.

### Assessment (`assessment`)

**Owns:**

- Assessment definitions.
- Assessment techniques or categories.
- Student score records.
- Assessment score validation.

### Dashboard (`dashboard`)

**Owns:**

- Role-specific dashboard summaries.
- Aggregated information for presentation.

### Report (`report`)

**Owns:**

- Learning outcome report composition.
- Attendance summaries for reports.
- Assessment summaries for reports.

Reports should reuse existing module Services or dedicated query operations. Do not duplicate business rules.

## 6. Dependency Rules

Use this dependency direction:

    Presentation -> Service -> DAO -> Database

Additional rules:

1. Presentation may depend on models and Services.
2. Service may depend on DAOs, models, and core utilities.
3. DAO may depend on models and database configuration.
4. DAO must not depend on presentation.
5. Model must not depend on Swing, CLI, or JDBC connections.
6. Cross-module business operations should use the owning module's Service contract.
7. Avoid cyclic dependencies between modules.
8. Shared utilities belong in `core` or `common` only when genuinely shared.

## 7. Application Startup

The application entry point is:

`id.ac.sekolah.manajemenkelas.Main`

**Startup responsibilities:**

1. Load application configuration.
2. Initialize database configuration.
3. Verify required infrastructure where appropriate.
4. Initialize shared dependencies.
5. Start the selected presentation interface.
6. Handle fatal startup errors gracefully.

The application should not create database connections independently throughout UI components.

Use explicit dependency construction and injection through constructors or a small composition root. Do not introduce a dependency injection framework.

## 8. Database Connection Management

- Centralize JDBC connection configuration.
- Read connection properties from configuration or environment variables.
- Never hardcode database passwords in source code.
- Use try-with-resources for `Connection`, `PreparedStatement`, and `ResultSet` where applicable.
- Use transactions when multiple database operations must be atomic.
- Do not keep a database connection open for the entire lifetime of the Swing application without a deliberate lifecycle strategy.
- Avoid sharing a single JDBC connection concurrently across unrelated operations.

A connection factory or small database utility may be introduced to centralize connection creation.

## 9. Authentication and Authorization Design

Authentication identifies the user.

Authorization determines whether the user can perform an operation.

**Use conceptual model:**

    User
     |
     +-- role: ADMIN
     |
     +-- role: TEACHER
           |
           +-- Teaching Assignments
           |
           +-- Homeroom Assignments

The authenticated session should expose the current user's identity and role.

Authorization must be enforced in Services, not only by hiding buttons or menu items.

Teacher authorization must consider the relevant teaching or homeroom assignment and academic period.

## 10. CLI-to-Swing Migration Strategy

### Phase 1: CLI

- Implement application workflows through CLI menus.
- Complete PostgreSQL integration.
- Validate business operations and access restrictions.
- Keep menus thin and dependent on Services.

### Phase 2: Swing *(Deferred Beyond Current CLI Delivery)*

The current deliverable completes **Phase 1 (CLI)** end-to-end with PostgreSQL 18; `presentation.swing.MainFrame` remains an empty scaffold and Swing UI implementation is out of scope for the current submission. Should a Swing UI be added later:

- Build the main JFrame using `CardLayout`, `JPanel`, and `JTable`.
- Reuse the exact same Service layer without reimplementing business rules.

## 11. Swing Application Structure

**Recommended composition:**

    MainFrame
     |
     +-- HeaderPanel
     |
     +-- SidebarPanel
     |
     +-- ContentPanel
           |
           +-- CardLayout
                 |
                 +-- DashboardPanel
                 +-- StudentPanel
                 +-- TeacherPanel
                 +-- AcademicPanel
                 +-- AttendancePanel
                 +-- AssessmentPanel
                 +-- ReportPanel

UI components should be cohesive and reusable where useful.

Avoid placing the entire application in one large JFrame class.

Follow Swing's Event Dispatch Thread rules:

- Create and update Swing components on the EDT.
- Do not perform long-running database operations directly on the EDT.
- Use `SwingWorker` or another appropriate mechanism for background operations.
- Return UI updates to the EDT.

## 12. Error Handling

Use meaningful exception types for important failure categories, such as:

- Validation errors.
- Authentication failures.
- Authorization failures.
- Entity not found.
- Duplicate or conflicting records.
- Database failures.

Do not expose raw SQL errors or stack traces to ordinary application users.

Log technical details appropriately while keeping credentials and sensitive information out of logs.

## 13. Transaction Boundaries

Transactions should be owned by the Service operation that coordinates multiple persistence actions.

**Examples:**

- Creating an academic configuration involving related records.
- Saving an attendance operation involving multiple student records.
- Performing a multi-step operation that must not leave partially updated data.

DAO methods should not independently commit operations that belong to one larger Service transaction.

## 14. Configuration

Keep configuration separate from business logic.

**Possible configuration values:**

- Database URL.
- Database username.
- Database password.
- Application environment.
- Application display settings.

Use `application.properties` for non-secret defaults. Use environment variables or an appropriate local configuration mechanism for secrets.

Do not commit real credentials.

## 15. Design System

The application uses a clean academic administration dashboard style.

### Color Palette

| Token | Value | Usage |
|---|---|---|
| Primary | `#2563EB` | Primary actions and navigation |
| Background | `#F5F7FB` | Main application background |
| Surface | `#FFFFFF` | Cards, tables, forms |
| Text Primary | `#172033` | Main text |
| Text Secondary | `#64748B` | Supporting text |
| Border | `#E2E8F0` | Separators and outlines |
| Success | `#16A34A` | Successful states |
| Warning | `#D97706` | Warning states |
| Danger | `#DC2626` | Errors and destructive actions |

### Typography

- Preferred font: Segoe UI.
- Use a sans-serif fallback when unavailable.
- Maintain consistent font hierarchy and spacing.

### Interaction Principles

- Use consistent button styles.
- Provide clear form validation.
- Confirm destructive operations.
- Use readable tables and clear empty states.
- Display useful feedback after successful or failed operations.

## 16. Project Structure

The package root is:

`id.ac.sekolah.manajemenkelas`

Feature modules should follow:

    module/
    ├── model/
    ├── dao/
    └── service/

Presentation code is separated into CLI and Swing packages.

Do not create packages solely for architectural decoration. Add them when there is a clear responsibility.

## 17. Resolved Architecture Decisions

- **Transaction & connection ownership:** Managed by `JdbcUtils.inTransaction` / `inTransactionVoid` using short-lived `DatabaseConfig.getConnection()` calls inside try-with-resources.
- **Build & testing:** Maven (`pom.xml`), `exec-maven-plugin`, PostgreSQL JDBC `42.7.7`, and JUnit Jupiter `5.11.4`.
- **Error handling:** `Postgres*DAO` translates constraint/truncation SQLStates (`23505`, `23503`, `23514`, `22001`) into `IllegalArgumentException`, caught cleanly in CLI menus via `MenuNavigator`.
- **Packaging:** Standard Maven JAR (`mvn package` -> `target/sistem-manajemen-kelas-sd-1.0.0-SNAPSHOT.jar`), runnable via NetBeans or `mvn exec:java`.
- **Student placement ownership:** Managed by `AcademicService` / `AcademicDAO` (`riwayat_kelas_siswa`) and inspected from `StudentMenu` via `AcademicService`.