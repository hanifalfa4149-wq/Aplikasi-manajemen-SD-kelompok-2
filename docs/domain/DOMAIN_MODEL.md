# Domain Model

# Sistem Manajemen Kelas SD

**Document Status:** Synchronized with Implemented Domain Models  
**Version:** 2.0

---

## 1. Domain Overview

The system manages elementary-school academic administration.

Its core domain concepts include:

- Users and roles.
- Students and teachers.
- Academic years and semesters.
- Classes and subjects.
- Teaching assignments.
- Homeroom assignments.
- Student class placements.
- Daily attendance.
- Assessments and scores.
- Learning outcome reports.

The domain model must preserve academic context and historical relationships.

## 2. User and Role

### User

Represents an account that can authenticate into the application.

Conceptual attributes:

- User ID.
- Username.
- Password hash.
- Role.
- Account status.
- Related teacher profile, when applicable.

### Roles

Only two authentication roles exist:

- "ADMIN"
- "TEACHER"

Homeroom teacher and subject teacher are not separate roles.

They are responsibilities derived from assignment records.

## 3. Student

Represents a student enrolled in the school.

Conceptual attributes:

- Student ID.
- Student number or school identifier, if required.
- Full name.
- Gender, if required.
- Date of birth, if required.
- Address, if required.
- Enrollment status.
- Other school-approved profile attributes.

A student's current class must not be modeled as a permanent property that overwrites previous academic history.

Class membership is represented through student placement records.

## 4. Teacher

Represents a teacher employed or registered by the school.

Conceptual attributes:

- Teacher ID.
- Teacher number or school identifier, if required.
- Full name.
- Contact information, if required.
- Employment or active status.
- Associated user account, when applicable.

A teacher may have multiple teaching assignments and homeroom responsibilities.

## 5. Academic Year

Represents an academic year.

Examples:

- 2025/2026
- 2026/2027

Conceptual attributes:

- Academic year ID.
- Display name or year range.
- Start date.
- End date.
- Status.

An academic year provides the context for academic activities and assignments.

## 6. Semester

Represents an academic semester within an academic year.

Conceptual attributes:

- Semester ID.
- Academic year reference.
- Semester name or number.
- Start date.
- End date.
- Status.

A semester belongs to one academic year.

The exact semester naming and calendar rules should follow the target school's policy.

## 7. Class

Represents a school class or rombongan belajar.

Conceptual attributes:

- Class ID.
- Class name.
- Grade level.
- Optional class label.
- Status.

A class is associated with academic periods through placement and assignment records.

Do not assume that a class is a permanent group independent of academic context.

## 8. Subject

Represents a school subject.

Conceptual attributes:

- Subject ID.
- Subject code, if applicable.
- Subject name.
- Description, if needed.
- Status.

Subjects must be data-driven.

Do not hardcode the list of subjects in Java source code.

## 9. Teaching Assignment

Represents a teacher's responsibility for teaching a subject to a class during a particular academic period.

Conceptual relationship:

    Teacher
       |
       | 1..*
       v
    TeachingAssignment
       ^
       | 1
       |
    Class

TeachingAssignment also references:

- Subject
- Academic period

A teaching assignment should contain references to:

- Teacher.
- Class.
- Subject.
- Academic year.
- Semester or agreed academic period.

Business rules:

- One teacher can have multiple teaching assignments.
- One class can have multiple teachers.
- One subject may be taught in multiple classes.
- Assignment records must be scoped to an academic period.
- Teacher access to class and subject data must respect these assignments.

## 10. Homeroom Assignment

Represents a teacher's homeroom responsibility for a class during a particular academic period.

Conceptual references:

- Teacher.
- Class.
- Academic year.
- Relevant academic period, if required by school policy.

Business rules:

- A teacher may be a homeroom teacher for different classes across different academic years.
- A class may have a different homeroom teacher in a later academic year.
- A homeroom assignment is not an authentication role.
- The system should prevent conflicting active homeroom assignments for the same class and period unless school policy explicitly permits them.

## 11. Student Placement

Represents a student's placement in a class during an academic period.

Conceptual references:

- Student.
- Class.
- Academic year.
- Placement date or effective period.
- Placement status, if needed.

Business rules:

1. A student may have different class placements across academic years.
2. Previous placements must remain available as historical records.
3. A new academic year must not overwrite the previous placement.
4. Conflicting placements within the same academic period must be prevented.
5. Student promotion or placement is a manual administrative operation in the initial MVP.

The exact cardinality and whether a student may transfer classes within one academic year must follow the school's policy.

If intra-year transfers are supported, the model must represent effective dates or placement history without losing the previous record.

## 12. Daily Attendance

Represents a student's attendance status for a particular school date.

Conceptual references:

- Student.
- Class or relevant placement.
- Attendance date.
- Attendance status.
- Recording user.
- Optional notes.

Allowed initial statuses:

| Status | Meaning |
|---|---|
| "HADIR" | Present |
| "SAKIT" | Sick |
| "IZIN" | Excused |
| "ALPA" | Absent without permission |

Business rules:

- Attendance is recorded daily, not per subject.
- A student should not have conflicting attendance records for the same school date and applicable attendance context.
- Teachers may record attendance only for authorized classes.
- Attendance records should preserve who recorded or updated them where appropriate.
- Corrections must follow the agreed authorization policy.

## 13. Assessment

Represents an assessment activity conducted for a subject or teaching context.

Conceptual attributes:

- Assessment ID.
- Assessment name.
- Description.
- Assessment technique or category.
- Teaching assignment reference.
- Assessment date.
- Maximum score, if applicable.
- Status.

Assessment categories and techniques should be configurable or represented flexibly.

Do not assume that all assessments use the same scoring formula.

## 14. Student Score

Represents a student's result for an assessment.

Conceptual references:

- Student.
- Assessment.
- Score value.
- Optional feedback or notes.
- Recording or updating user.

Business rules:

- Scores must belong to a valid assessment.
- Scores must be associated with students who are valid participants in the relevant class and academic context.
- Score validation must respect the assessment's defined scoring scale.
- A universal grading formula must not be hardcoded without school approval.

## 15. Learning Outcome Report

Represents a report composed from relevant academic records.

The initial report may include:

- Student identity.
- Class.
- Academic year.
- Semester.
- Subject.
- Assessment summary.
- Final score, if policy defines one.
- Learning outcome description.
- Attendance summary.

The report is a derived view of academic data.

Avoid duplicating authoritative score or attendance records in a separate table unless a confirmed business requirement requires historical report snapshots.

## 16. Core Relationships

Conceptual relationship summary:

    AcademicYear
        |
        +---- Semester
        |
        +---- TeachingAssignment
        |
        +---- HomeroomAssignment
        |
        +---- StudentPlacement

    Teacher
        |
        +---- TeachingAssignment
        |
        +---- HomeroomAssignment
        |
        +---- User Account (optional relationship)

    Student
        |
        +---- StudentPlacement
        |
        +---- DailyAttendance
        |
        +---- StudentScore

    Class
        |
        +---- StudentPlacement
        |
        +---- TeachingAssignment
        |
        +---- HomeroomAssignment

    Subject
        |
        +---- TeachingAssignment

    TeachingAssignment
        |
        +---- Assessment
                 |
                 +---- StudentScore

This is a conceptual model, not a finalized physical ERD.

## 17. Domain Invariants

The following invariants should be enforced through Service validation and database constraints where appropriate:

1. A user must have one valid authentication role.
2. A teacher account must be associated with a valid teacher profile when required.
3. A semester must belong to a valid academic year.
4. Teaching assignments must reference valid teachers, classes, subjects, and academic periods.
5. Homeroom assignments must reference valid teachers, classes, and academic periods.
6. Student placements must reference valid students, classes, and academic periods.
7. Conflicting active student placements must be prevented.
8. Attendance records must reference valid students and dates.
9. Attendance status must belong to the approved set.
10. Assessment records must reference a valid academic teaching context.
11. Score records must reference valid students and assessments.
12. Teacher access must be restricted by assignment.

## 18. Historical Data Principles

Historical records must remain meaningful even after master data changes.

Examples:

- Changing a teacher's current status must not erase past teaching assignments.
- Moving a student to another class must not erase previous placements.
- Closing an academic year must not remove its attendance or assessment records.
- Deactivating a subject must not invalidate historical assessment records.

Prefer status changes and historical records over destructive deletion when data is referenced by academic transactions.

## 19. Resolved Domain Policies (`Implemented MVP`)

- **Class transfers:** Supported atomically via `AcademicService.promoteOrTransferStudent` (`NAIK_KELAS` / `PINDAH`) preserving historical `riwayat_kelas_siswa` rows.
- **Homeroom teacher:** At most one active homeroom teacher per class (`idx_wali_kelas_aktif`), ending prior assignments via `endHomeroomAssignment`.
- **Academic periods:** Semesters are numbered `1` (Ganjil) or `2` (Genap) within a `tahun_ajaran`, and only one academic year can be active at a time (`idx_tahun_ajaran_aktif`).
- **Assessments & scores:** Supports 7 categories (`KUIS`, `TUGAS`, `UTS`, `UAS`, `PRAKTIK`, `PROYEK`, `LAINNYA`) with configurable `BigDecimal` `maxScore > 0` and student scores validated as `0 <= nilai <= nilai_maksimal`.
- **Attendance:** Daily (`HADIR`, `SAKIT`, `IZIN`, `ALPA`), unique per `(id_siswa, tanggal_absensi)`, optional `keterangan`, and audit actor `dicatat_oleh`.