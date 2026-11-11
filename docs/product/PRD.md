# Product Requirements Document (PRD)

# Sistem Manajemen Kelas SD

**Document Status:** Synchronized with Final CLI Delivery  
**Version:** 2.0  
**Product Type:** Desktop CLI Application  
**Primary Language:** Bahasa Indonesia

---

## 1. Product Overview

Sistem Manajemen Kelas SD is a desktop application designed to support elementary-school classroom and academic administration.

The application centralizes student data, teacher data, academic structures, classroom assignments, daily attendance, assessments, and learning outcome reports.

The system is developed as an academic software engineering project and will be demonstrated through a working application integrated with PostgreSQL.

## 2. Problem Statement

Classroom administration involves multiple related datasets and recurring activities.

Without a centralized system, school personnel may need to manage student information, teacher assignments, attendance, assessment results, and academic reports separately.

This application aims to provide a structured environment for managing those activities with consistent data relationships and controlled access.

## 3. Product Objectives

The application should:

1. Centralize student and teacher records.
2. Support academic year, semester, class, and subject management.
3. Represent teacher responsibilities through explicit assignments.
4. Maintain historical student class placements.
5. Record daily student attendance.
6. Support configurable assessment activities and score records.
7. Produce basic learning outcome reports.
8. Restrict access according to user roles and teaching assignments.
9. Preserve records in a PostgreSQL database.

## 4. Target Users

### 4.1 Admin Sistem

The administrator manages system-wide and academic master data.

Responsibilities:

- Manage teacher accounts.
- Manage student records.
- Manage academic years and semesters.
- Manage classes.
- Manage subjects.
- Manage teacher assignments.
- Manage student class placements.
- Maintain system configuration.

The "ADMIN" role does not automatically represent the legal or institutional authority of a school principal or official school operator.

### 4.2 Guru

Teachers access classroom and academic activities according to their assignments.

Responsibilities may include:

- View assigned classes and subjects.
- View relevant student information.
- Record daily student attendance for authorized classes.
- Create or manage assessments within authorized teaching assignments.
- Enter and review assessment scores.
- View relevant learning outcome summaries.

A teacher must not automatically gain access to every class or subject in the school.

## 5. Authentication and Authorization

The application has exactly two authentication roles:

| Role | Description |
|---|---|
| "ADMIN" | System and academic administration |
| "TEACHER" | Classroom and academic activities |

Homeroom teacher and subject teacher are responsibilities represented through assignment records, not separate login roles.

Authorization must consider both the user's role and relevant academic assignments.

## 6. Functional Requirements

### FR-01 Authentication

- Users can log in using their credentials.
- The application identifies the user's role.
- The application restricts features according to authorization.
- Users can log out.
- Passwords must not be stored in plaintext.

### FR-02 Dashboard

- Display relevant information according to the authenticated role.
- Provide navigation to authorized modules.
- Display useful academic summaries when available.

### FR-03 Student Management

- Create student records.
- View student details.
- Update student information.
- Search and filter student records.
- Deactivate student records instead of deleting historical data where appropriate.
- Maintain historical class placements.

### FR-04 Teacher Management

- Create and update teacher records.
- View teacher details.
- Search teacher records.
- Manage teacher account status.
- Associate teachers with academic responsibilities.

### FR-05 Academic Structure

- Manage academic years.
- Manage semesters.
- Manage classes or rombongan belajar.
- Manage subjects.
- Identify the active academic period.
- Prevent inconsistent academic configurations.

### FR-06 Teacher Assignments

- Assign teachers to classes and subjects.
- Assign homeroom responsibilities.
- Associate assignments with an academic period.
- Support one teacher having multiple assignments.
- Support multiple teachers being assigned to one class.

### FR-07 Student Class Placement

- Place students into classes for a particular academic period.
- Preserve previous placements.
- Support manual placement when a new academic year begins.
- Prevent conflicting active placements within the same academic period.

Automatic student promotion decisions are outside the initial scope.

### FR-08 Daily Attendance

- Record student attendance by class and date.
- Support the following statuses:
  - "HADIR"
  - "SAKIT"
  - "IZIN"
  - "ALPA"
- View attendance records.
- Update attendance records according to authorization.
- Prevent duplicate attendance records for the same student and date within the applicable school context.

Attendance is daily student attendance, not per-subject attendance.

### FR-09 Assessment and Scores

- Create assessment activities.
- Associate assessments with a teaching assignment or relevant academic context.
- Support different assessment techniques.
- Record student scores.
- Review and update scores according to authorization.
- Avoid hardcoding a single subject list or universal grading formula.

### FR-10 Learning Outcome Reports

Generate a basic learning outcome report containing relevant information such as:

- Student identity.
- Class.
- Academic year and semester.
- Subject.
- Assessment summary.
- Final score, if the applicable school policy defines one.
- Learning outcome descriptions.
- Attendance summary.

The initial report is not intended to replace a complete official school report card.

## 7. Non-Functional Requirements

### NFR-01 Data Integrity

- Use relational constraints where appropriate.
- Preserve historical academic records.
- Prevent invalid relationships between entities.
- Use database transactions for operations requiring atomicity.

### NFR-02 Security

- Store passwords using an appropriate password hashing algorithm.
- Use parameterized SQL statements.
- Enforce authorization in the Service layer.
- Avoid exposing credentials or sensitive information in logs.

### NFR-03 Maintainability

- Separate presentation, business logic, and persistence.
- Keep modules cohesive.
- Use consistent naming conventions.
- Avoid duplicated business rules.

### NFR-04 Usability

- Use a consistent, role-scoped CLI interface (`ADMIN` and `TEACHER` menus).
- Provide clear validation and error messages without crashing on malformed input or DB failures.
- Preserve clear empty-state messages and clean navigation back to parent menus.
- Make common administrative and classroom workflows easy to navigate.

### NFR-05 Reliability

- Persist application data in PostgreSQL.
- Handle database connectivity failures gracefully.
- Avoid partial writes in multi-step business operations.

## 8. Out of Scope

The following are not part of the initial MVP:

- Complete official report card generation.
- Automated student promotion decisions.
- Parent or guardian portal.
- Student mobile application.
- Online payment processing.
- School-wide learning management system.
- External government education-system integration.
- Complex curriculum planning.
- Attendance per individual subject session.

These may be considered in future versions.

## 9. MVP Acceptance Criteria

The MVP is considered demonstrable when:

1. Admin can log in.
2. Admin can configure an academic year and semester.
3. Admin can create classes and subjects.
4. Admin can create teacher and student records.
5. Admin can assign teachers to classes and subjects.
6. Admin can place students into classes.
7. Teacher can log in.
8. Teacher can only access authorized academic resources.
9. Teacher can record daily attendance.
10. Teacher can create assessments and enter scores.
11. The system can display a basic learning outcome report.
12. Data persists in PostgreSQL after application restart.
13. All CLI workflows reuse the same Service and DAO layers (with Swing GUI deferred beyond this CLI delivery).

## 10. Product Scope & Delivered Implementation Notes

- **Target interface delivered:** Interactive CLI (`CliApplication`, `MainMenu`, `MenuNavigator`, and 7 module menus); Swing GUI (`MainFrame`) is deferred and not implemented in this delivery.
- **Confirmed implementation defaults:** Java 17 bytecode (`JDK 25`), PostgreSQL 18 (`manajemen-sd`), PBKDF2WithHmacSHA256 authentication, weighted/percentage summaries (`AssessmentSummary`, `StudentScoreSummary`, `SubjectScoreReport`) computed from configurable assessment `maxScore` without hardcoding school-specific report card grading thresholds.

## 11. Product Constraints

- Java 17 target bytecode (JDK 25 development environment).
- Desktop CLI interface (Swing GUI deferred out of current submission scope).
- PostgreSQL 18 with JDBC-based persistence and `InMemory*DAO` unit-test fallbacks.
- No application frameworks.
- Four-person development team and academic project timeline.