# Team Workflow
# Sistem Manajemen Kelas SD

**Document Status:** Synchronized with Final CLI + PostgreSQL 18 Delivery  
**Version:** 2.0

---

## 1. Team Structure

The project is developed by a four-person team.

Feature ownership is organized by module, not by frontend/backend specialization.

Each module owner is responsible for its model, DAO, Service, presentation, validation, and testing.

## 2. Ownership

### Project Lead and Integrator

**Primary responsibilities:**

- Application entry point and configuration.
- Core utilities and shared infrastructure.
- Authentication module.
- CLI application shell and navigation.
- Dashboard Service.
- Report Service.
- Swing application shell.
- MainFrame and navigation structure.
- Login GUI.
- Cross-module integration.
- Architecture consistency.
- Shared contracts and integration review.

### Member 2: Student and Teacher

**Primary responsibilities:**

- Student model, DAO, and Service.
- Teacher model, DAO, and Service.
- Student and teacher CRUD operations.
- Search and filtering.
- Student lifecycle and deactivation.
- Student-related CLI menus.
- Student and teacher Swing panels and forms.
- Student placement integration with the academic module.

### Member 3: Academic Structure

**Primary responsibilities:**

- Academic year.
- Semester.
- Class.
- Subject.
- Teaching assignment.
- Homeroom assignment.
- Academic model, DAO, and Service.
- Academic CLI menus.
- Academic Swing panels and forms.
- Student placement coordination with the student module.

### Member 4: Attendance and Assessment

**Primary responsibilities:**

- Daily attendance.
- Assessment definitions.
- Student scores.
- Attendance and assessment model, DAO, and Service.
- Assignment-based access restrictions.
- Attendance and assessment CLI menus.
- Attendance and assessment Swing panels and forms.
- Summary query support for reports.

## 3. Ownership Boundaries

Ownership does not mean exclusive access to every related file.

Some changes naturally affect multiple modules.

Before changing another module's public contracts, model, or schema:

1. Identify the owning member.
2. Explain the reason for the change.
3. Agree on the expected contract.
4. Coordinate the integration.
5. Update affected documentation and tests.

The project lead coordinates shared architectural and integration decisions.

## 4. Git Workflow

**Recommended branch model:**

- `main`: stable integrated branch.
- `feature/<module>-<feature>`: feature implementation.
- `fix/<module>-<issue>`: bug fixes.

**Examples:**

- `feature/student-crud`
- `feature/academic-assignment`
- `feature/daily-attendance`
- `fix/auth-login-validation`

**Workflow:**

1. Pull the latest `main`.
2. Create a feature branch.
3. Implement a focused change.
4. Run relevant tests and verification.
5. Review the diff.
6. Commit with a descriptive message.
7. Merge through a reviewed pull request or agreed team process.

Avoid committing unrelated work together.

## 5. Shared Contracts

Before integrating modules, agree on:

- Model names and fields.
- Service method names.
- Parameter types.
- Return types.
- Exception behavior.
- Authorization expectations.
- Transaction ownership.

**Illustrative Service contracts:**

### StudentService

- `getAllStudents()`
- `getStudentById(...)`
- `saveStudent(...)`
- `updateStudent(...)`
- `deactivateStudent(...)`

### TeacherService

- `getAllTeachers()`
- `getTeacherById(...)`
- `saveTeacher(...)`
- `updateTeacher(...)`
- `deactivateTeacher(...)`

### AcademicService

- `getActiveAcademicYear()`
- `getClassesByTeacher(...)`
- `getSubjectsByTeacher(...)`
- `getTeachingAssignments(...)`

### AttendanceService

- `saveAttendance(...)`
- `getAttendanceByClassAndDate(...)`

### AssessmentService

- `createAssessment(...)`
- `saveScores(...)`
- `getScoresByAssessment(...)`

These names are examples, not immutable API definitions.

The team must agree on actual signatures and types before implementation.

## 6. Database Collaboration

Each module owner is responsible for proposing the tables, columns, relationships, and constraints required by their module.

The project lead coordinates:

- Shared identifiers.
- Cross-module foreign keys.
- Naming conventions.
- Schema integration.
- Constraint conflicts.
- Seed data consistency.

Database changes must not be silently introduced through unrelated code changes.

A schema change must be communicated before dependent DAO and Service implementations are integrated.

## 7. Definition of Done

A feature is considered complete when:

- The requirement is understood.
- The implementation follows the architecture.
- Business logic is located in the Service layer.
- DAO operations use parameterized SQL.
- Input and business validation are implemented.
- Authorization is enforced where applicable.
- Database changes are documented and integrated.
- Success and failure cases have been tested.
- CLI or Swing presentation works as required.
- Relevant documentation is updated.
- The feature is integrated with the current application.

A feature is not complete merely because its UI appears on screen.

## 8. Integration Milestones

### Milestone 1: Application Foundation

- Java project structure.
- PostgreSQL connectivity.
- Shared configuration.
- Core exception handling.
- Initial database schema.

### Milestone 2: Authentication and Master Data

- Admin login.
- Teacher login.
- Student records.
- Teacher records.
- Academic year and semester.
- Classes and subjects.

### Milestone 3: Academic Operations

- Teaching assignments.
- Homeroom assignments.
- Student placements.
- Teacher access restrictions.

### Milestone 4: Classroom Operations

- Daily attendance.
- Assessment creation.
- Score entry.
- Basic report generation.

### Milestone 5: CLI Demonstration *(Completed)*

Complete CLI workflows verified end-to-end against real PostgreSQL 18 persistence and automated QA/integration tests.

### Milestone 6: Swing Presentation *(Deferred Beyond Current Delivery Scope)*

Future enhancement: implement Swing panels and navigation in `presentation/swing` reusing the existing Service layer (`AuthService`, `StudentService`, `TeacherService`, `AcademicService`, `AttendanceService`, `AssessmentService`, `DashboardService`, `ReportService`).

## 9. Communication Rules

Communicate changes that affect:

- Shared models.
- Service contracts.
- Database schema.
- Cross-module authorization.
- Academic business rules.
- Package structure.
- Application startup.

Do not assume another member knows about a breaking change merely because the code has been pushed.

## 10. AI-Assisted Development Rules

When using GitHub Copilot or another coding agent:

- Work within the assigned module.
- Read the relevant documentation first.
- Inspect existing code before generating new files.
- Do not let the agent independently redefine shared architecture.
- Review generated code and SQL.
- Verify database operations.
- Do not accept untested claims.
- Keep generated changes focused.
- Review diffs before committing.
- Update documentation when the source of truth changes.

AI-generated code remains the responsibility of the team member who integrates it.

## 11. Change Documentation

Update relevant documentation when a change affects:

| Change | Required document |
|---|---|
| Product scope or requirement | `docs/product/PRD.md` |
| Architecture or dependency rules | `docs/architecture/SYSTEM_DESIGN.md` |
| Domain entities or business rules | `docs/domain/DOMAIN_MODEL.md` |
| Database structure or constraints | `docs/database/DATABASE.md` |
| Ownership or collaboration rules | `docs/TEAM_WORKFLOW.md` |

Do not update documentation for trivial internal refactoring unless it changes documented behavior or structure.