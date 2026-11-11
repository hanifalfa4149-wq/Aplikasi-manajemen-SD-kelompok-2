package id.ac.sekolah.manajemenkelas.presentation.cli.assessment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.assessment.model.Assessment;
import id.ac.sekolah.manajemenkelas.assessment.model.AssessmentSummary;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScore;
import id.ac.sekolah.manajemenkelas.assessment.model.StudentScoreSummary;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService;
import id.ac.sekolah.manajemenkelas.assessment.service.AssessmentService.StudentScoreEntry;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MenuNavigator;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

public class AssessmentMenu {

    private final Scanner scanner;
    private final AssessmentService assessmentService;
    private final AcademicService academicService;
    private final StudentService studentService;
    private final TeacherService teacherService;

    public AssessmentMenu(
            Scanner scanner,
            AssessmentService assessmentService,
            AcademicService academicService,
            StudentService studentService,
            TeacherService teacherService) {
        this.scanner = scanner;
        this.assessmentService = assessmentService;
        this.academicService = academicService;
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    public void show() {
        boolean active = true;

        while (active) {
            System.out.println("\n========== PENILAIAN & NILAI SISWA ==========");
            System.out.println("1. Buat Penilaian Baru");
            System.out.println("2. Daftar Semua Penilaian");
            System.out.println("3. Daftar Penilaian per Penugasan Mengajar");
            System.out.println("4. Input Nilai Satu Siswa");
            System.out.println("5. Input Nilai Massal Satu Kelas");
            System.out.println("6. Perbarui Nilai Siswa");
            System.out.println("7. Lihat Nilai per Penilaian");
            System.out.println("8. Lihat Nilai per Siswa");
            System.out.println("9. Ringkasan Nilai Penilaian / Siswa");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            String choice = MenuNavigator.readMenuChoice(scanner);

            try {
                switch (choice) {
                    case "1" -> createAssessment();
                    case "2" -> listAllAssessments();
                    case "3" -> listByTeachingAssignment();
                    case "4" -> recordSingleScore();
                    case "5" -> recordBatchScores();
                    case "6" -> updateScore();
                    case "7" -> viewScoresByAssessment();
                    case "8" -> viewScoresByStudent();
                    case "9" -> viewSummaries();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Gagal: " + e.getMessage());
            } catch (IllegalStateException e) {
                System.out.println("Kesalahan sistem/database: " + e.getMessage());
            }
        }
    }

    private void createAssessment() {
        displayAvailableTeachingAssignments();
        System.out.print("ID penugasan mengajar: ");
        Long taId = readLong();

        System.out.print("Judul penilaian: ");
        String title = scanner.nextLine();

        System.out.print("Jenis (KUIS/TUGAS/UTS/UAS/PRAKTIK/PROYEK/LAINNYA): ");
        String type = scanner.nextLine();

        System.out.print("Tanggal penilaian (YYYY-MM-DD): ");
        LocalDate date = readDate();

        System.out.print("Nilai maksimal (contoh 100): ");
        BigDecimal maxScore = readBigDecimal();

        System.out.print("Deskripsi (opsional): ");
        String description = scanner.nextLine();

        Assessment created = assessmentService.createAssessment(
                taId, title, type, date, maxScore, description);
        System.out.println("Penilaian berhasil dibuat. ID: " + created.getId());
    }

    private void listAllAssessments() {
        displayAssessments(assessmentService.getAllAssessments());
    }

    private void listByTeachingAssignment() {
        displayAvailableTeachingAssignments();
        System.out.print("ID penugasan mengajar: ");
        Long taId = readLong();
        displayAssessments(assessmentService.getAssessmentsByTeachingAssignment(taId));
    }

    private void recordSingleScore() {
        displayAssessments(assessmentService.getAllAssessments());
        System.out.print("ID penilaian: ");
        Long assessmentId = readLong();

        Assessment assessment = assessmentService.getAssessmentById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data penilaian tidak ditemukan."));
        TeachingAssignment ta = academicService.getTeachingAssignmentById(assessment.getTeachingAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Penugasan mengajar tidak ditemukan."));

        displayStudentsInClass(ta.getClassId());
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        System.out.print("Nilai (0 s.d. " + assessment.getMaxScore() + "): ");
        BigDecimal score = readBigDecimal();

        System.out.print("Keterangan (opsional): ");
        String notes = scanner.nextLine();

        StudentScore saved = assessmentService.recordScore(assessmentId, studentId, score, notes);
        System.out.println("Nilai berhasil dicatat. ID: " + saved.getId());
    }

    private void recordBatchScores() {
        displayAssessments(assessmentService.getAllAssessments());
        System.out.print("ID penilaian: ");
        Long assessmentId = readLong();

        Assessment assessment = assessmentService.getAssessmentById(assessmentId)
                .orElseThrow(() -> new IllegalArgumentException("Data penilaian tidak ditemukan."));
        TeachingAssignment ta = academicService.getTeachingAssignmentById(assessment.getTeachingAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException("Penugasan mengajar tidak ditemukan."));

        List<StudentPlacement> placements = academicService.getActivePlacementsByClass(ta.getClassId());
        if (placements.isEmpty()) {
            System.out.println("Tidak ada siswa aktif di kelas ini.");
            return;
        }

        List<StudentScoreEntry> entries = new ArrayList<>();
        System.out.println("Masukkan nilai (0 s.d. " + assessment.getMaxScore()
                + ") untuk siswa (kosongkan untuk lewati):");

        for (StudentPlacement p : placements) {
            String studentName = studentService.getStudentById(p.getStudentId())
                    .map(Student::getFullName)
                    .orElse("Siswa #" + p.getStudentId());

            System.out.print(" - [" + p.getStudentId() + "] " + studentName + " Nilai: ");
            String scoreInput = scanner.nextLine().trim();
            if (scoreInput.isEmpty()) {
                continue;
            }
            BigDecimal scoreVal = parseBigDecimal(scoreInput);

            System.out.print("   Keterangan (opsional): ");
            String notes = scanner.nextLine();
            entries.add(new StudentScoreEntry(p.getStudentId(), scoreVal, notes));
        }

        List<StudentScore> saved = assessmentService.recordAssessmentScores(assessmentId, entries);
        System.out.println("Berhasil menyimpan " + saved.size() + " nilai siswa.");
    }

    private void updateScore() {
        displayAssessments(assessmentService.getAllAssessments());
        System.out.print("ID penilaian: ");
        Long assessmentId = readLong();

        displayScores(assessmentService.getScoresByAssessment(assessmentId));
        System.out.print("ID nilai siswa yang ingin diperbarui: ");
        Long scoreId = readLong();

        System.out.print("Nilai baru: ");
        BigDecimal newScore = readBigDecimal();

        System.out.print("Keterangan baru (opsional): ");
        String notes = scanner.nextLine();

        StudentScore updated = assessmentService.updateScore(scoreId, newScore, notes);
        System.out.println("Nilai berhasil diperbarui -> " + updated.getScore());
    }

    private void viewScoresByAssessment() {
        displayAssessments(assessmentService.getAllAssessments());
        System.out.print("ID penilaian: ");
        Long assessmentId = readLong();
        displayScores(assessmentService.getScoresByAssessment(assessmentId));
    }

    private void viewScoresByStudent() {
        displayActiveStudents();
        System.out.print("ID siswa: ");
        Long studentId = readLong();
        displayScores(assessmentService.getScoresByStudent(studentId));
    }

    private void viewSummaries() {
        System.out.println("\nPilih jenis ringkasan:");
        System.out.println("1. Ringkasan per Penilaian");
        System.out.println("2. Ringkasan per Siswa");
        System.out.print("Pilihan: ");
        String opt = scanner.nextLine().trim();

        if ("1".equals(opt)) {
            displayAssessments(assessmentService.getAllAssessments());
            System.out.print("ID penilaian: ");
            Long assessmentId = readLong();
            AssessmentSummary summary = assessmentService.getAssessmentScoreSummary(assessmentId);

            System.out.println("\n--- Ringkasan Nilai Penilaian ---");
            System.out.println("Penilaian     : [" + summary.getAssessmentId() + "] "
                    + summary.getTitle() + " (" + summary.getAssessmentType() + ")");
            System.out.println("Nilai Maks    : " + summary.getMaxScore());
            System.out.println("Jumlah Dinilai: " + summary.getTotalStudentsScored());
            System.out.println("Rata-rata     : " + summary.getAverageScore()
                    + " (" + summary.getAveragePercentage() + "%)");
            System.out.println("Tertinggi     : " + summary.getHighestScore());
            System.out.println("Terendah      : " + summary.getLowestScore());
        } else if ("2".equals(opt)) {
            displayActiveStudents();
            System.out.print("ID siswa: ");
            Long studentId = readLong();
            StudentScoreSummary summary = assessmentService.getStudentScoreSummary(studentId);
            String studentName = studentService.getStudentById(studentId)
                    .map(Student::getFullName)
                    .orElse("Siswa #" + studentId);

            System.out.println("\n--- Ringkasan Nilai Siswa ---");
            System.out.println("Siswa            : [" + studentId + "] " + studentName);
            System.out.println("Jumlah Penilaian : " + summary.getTotalAssessments());
            System.out.println("Total Nilai      : " + summary.getTotalEarnedScore()
                    + " / " + summary.getTotalMaxScore());
            System.out.println("Rata-rata        : " + summary.getAverageScore());
            System.out.println("Persentase Total : " + summary.getOverallPercentage() + "%");
        } else {
            System.out.println("Pilihan ringkasan tidak valid.");
        }
    }

    private void displayAvailableTeachingAssignments() {
        SessionContext session = SessionContext.getInstance();
        List<TeachingAssignment> assignments;
        if (session.hasRole(RoleMapper.ROLE_TEACHER) && session.getTeacherId() != null) {
            assignments = academicService.getTeachingAssignmentsByTeacher(session.getTeacherId());
        } else {
            assignments = academicService.getAllTeachingAssignments();
        }

        System.out.println("\nDaftar Penugasan Mengajar:");
        for (TeachingAssignment ta : assignments) {
            String className = academicService.getClassById(ta.getClassId())
                    .map(SchoolClass::getName)
                    .orElse("Kelas #" + ta.getClassId());
            String subjectName = academicService.getSubjectById(ta.getSubjectId())
                    .map(Subject::getName)
                    .orElse("Mapel #" + ta.getSubjectId());
            String teacherName = teacherService.getTeacherById(ta.getTeacherId())
                    .map(Teacher::getFullName)
                    .orElse("Guru #" + ta.getTeacherId());
            String semesterName = academicService.getSemesterById(ta.getSemesterId())
                    .map(Semester::getName)
                    .orElse("Semester #" + ta.getSemesterId());

            System.out.println(" - ID: " + ta.getId()
                    + " | " + className
                    + " | " + subjectName
                    + " | " + teacherName
                    + " | " + semesterName
                    + " | Aktif: " + (ta.isActive() ? "Ya" : "Tidak"));
        }
    }

    private void displayStudentsInClass(Long classId) {
        List<StudentPlacement> placements = academicService.getActivePlacementsByClass(classId);
        System.out.println("\nSiswa Aktif di Kelas ID " + classId + ":");
        for (StudentPlacement p : placements) {
            String name = studentService.getStudentById(p.getStudentId())
                    .map(Student::getFullName)
                    .orElse("Siswa #" + p.getStudentId());
            System.out.println(" - ID: " + p.getStudentId() + " | " + name);
        }
    }

    private void displayActiveStudents() {
        SessionContext session = SessionContext.getInstance();
        Set<Long> allowedStudentIds = null;
        if (session.hasRole(RoleMapper.ROLE_TEACHER) && session.getTeacherId() != null) {
            allowedStudentIds = new HashSet<>();
            for (TeachingAssignment ta : academicService.getTeachingAssignmentsByTeacher(session.getTeacherId())) {
                if (ta.isActive()) {
                    for (StudentPlacement p : academicService.getActivePlacementsByClass(ta.getClassId())) {
                        allowedStudentIds.add(p.getStudentId());
                    }
                }
            }
        }

        System.out.println("\nDaftar Siswa Aktif:");
        boolean found = false;
        for (Student s : studentService.getAllStudents()) {
            if (s.isActive() && (allowedStudentIds == null || allowedStudentIds.contains(s.getId()))) {
                System.out.println(" - ID: " + s.getId() + " | " + s.getNis() + " | " + s.getFullName());
                found = true;
            }
        }
        if (!found) {
            System.out.println("Belum ada siswa aktif dalam cakupan Anda.");
        }
    }

    private void displayAssessments(List<Assessment> assessments) {
        if (assessments.isEmpty()) {
            System.out.println("Belum ada data penilaian.");
            return;
        }
        System.out.println("\nDaftar Penilaian:");
        for (Assessment a : assessments) {
            System.out.println(" - ID: " + a.getId()
                    + " | Penugasan ID: " + a.getTeachingAssignmentId()
                    + " | [" + a.getAssessmentType() + "] " + a.getTitle()
                    + " | Tgl: " + a.getAssessmentDate()
                    + " | Maks: " + a.getMaxScore());
        }
    }

    private void displayScores(List<StudentScore> scores) {
        if (scores.isEmpty()) {
            System.out.println("Belum ada data nilai siswa.");
            return;
        }
        System.out.println("\nDaftar Nilai Siswa:");
        for (StudentScore s : scores) {
            String studentName = studentService.getStudentById(s.getStudentId())
                    .map(Student::getFullName)
                    .orElse("Siswa #" + s.getStudentId());
            System.out.println(" - ID: " + s.getId()
                    + " | Penilaian ID: " + s.getAssessmentId()
                    + " | Siswa: [" + s.getStudentId() + "] " + studentName
                    + " | Nilai: " + s.getScore()
                    + (s.getNotes() == null || s.getNotes().isBlank() ? "" : " | Ket: " + s.getNotes()));
        }
    }

    private Long readLong() {
        String raw = scanner.nextLine().trim();
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input ID harus berupa angka.");
        }
    }

    private LocalDate readDate() {
        String raw = scanner.nextLine().trim();
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan YYYY-MM-DD.");
        }
    }

    private BigDecimal readBigDecimal() {
        return parseBigDecimal(scanner.nextLine().trim());
    }

    private BigDecimal parseBigDecimal(String raw) {
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input nilai harus berupa angka yang valid.");
        }
    }
}
