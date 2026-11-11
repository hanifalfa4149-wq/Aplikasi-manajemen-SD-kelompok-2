package id.ac.sekolah.manajemenkelas.presentation.cli.academic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.academic.service.AcademicService;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;
import id.ac.sekolah.manajemenkelas.presentation.cli.menu.MenuNavigator;
import id.ac.sekolah.manajemenkelas.student.model.Student;
import id.ac.sekolah.manajemenkelas.student.service.StudentService;
import id.ac.sekolah.manajemenkelas.teacher.model.Teacher;
import id.ac.sekolah.manajemenkelas.teacher.service.TeacherService;

public class AcademicMenu {

    private final Scanner scanner;
    private final AcademicService academicService;
    private final StudentService studentService;
    private final TeacherService teacherService;

    public AcademicMenu(Scanner scanner, AcademicService academicService) {
        this(scanner, academicService, null, null);
    }

    public AcademicMenu(
            Scanner scanner,
            AcademicService academicService,
            StudentService studentService,
            TeacherService teacherService) {
        this.scanner = scanner;
        this.academicService = academicService;
        this.studentService = studentService;
        this.teacherService = teacherService;
    }

    public void show() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== STRUKTUR AKADEMIK ==========");
            System.out.println("1. Manajemen Tahun Ajaran");
            System.out.println("2. Manajemen Semester");
            System.out.println("3. Manajemen Kelas");
            System.out.println("4. Manajemen Mata Pelajaran");
            System.out.println("5. Penugasan Wali Kelas");
            System.out.println("6. Penugasan Mengajar");
            System.out.println("7. Penempatan & Riwayat Kelas Siswa");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            switch (MenuNavigator.readMenuChoice(scanner)) {
                case "1" -> academicYearMenu();
                case "2" -> semesterMenu();
                case "3" -> classMenu();
                case "4" -> subjectMenu();
                case "5" -> homeroomMenu();
                case "6" -> teachingAssignmentMenu();
                case "7" -> placementMenu();
                case "0" -> active = false;
                default -> System.out.println("Pilihan tidak valid.");
            }
        }
    }

    private boolean isAdminSession() {
        SessionContext session = SessionContext.getInstance();
        return !session.isLoggedIn() || session.hasRole(RoleMapper.ROLE_ADMIN);
    }

    private void requireAdminAction() {
        if (!isAdminSession()) {
            throw new IllegalArgumentException(
                    "Akses ditolak: fitur ini hanya dapat diakses oleh ADMIN.");
        }
    }

    // ==========================================
    // ACADEMIC YEAR
    // ==========================================

    private void academicYearMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== TAHUN AJARAN ==========");
            System.out.println("1. Tambah Tahun Ajaran");
            System.out.println("2. Lihat Tahun Ajaran");
            System.out.println("3. Edit Tahun Ajaran");
            System.out.println("4. Nonaktifkan Tahun Ajaran");
            System.out.println("5. Aktifkan Tahun Ajaran");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> createAcademicYear();
                    case "2" -> displayAcademicYears();
                    case "3" -> updateAcademicYear();
                    case "4" -> deactivateAcademicYear();
                    case "5" -> activateAcademicYear();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createAcademicYear() {
        requireAdminAction();

        System.out.print("Nama tahun ajaran (contoh 2026/2027): ");
        String name = scanner.nextLine();

        System.out.print("Tanggal mulai (YYYY-MM-DD): ");
        LocalDate start = readDate();

        System.out.print("Tanggal selesai (YYYY-MM-DD): ");
        LocalDate end = readDate();

        AcademicYear year = academicService.createAcademicYear(name, start, end);

        System.out.println("Tahun ajaran berhasil dibuat. ID: " + year.getId());
    }

    private void displayAcademicYears() {

        List<AcademicYear> years = academicService.getAllAcademicYears();

        if (years.isEmpty()) {
            System.out.println("Belum ada tahun ajaran.");
            return;
        }

        for (AcademicYear year : years) {
            System.out.println("----------------------------------");
            System.out.println("ID      : " + year.getId());
            System.out.println("Nama    : " + year.getName());
            System.out.println("Mulai   : " + year.getStartDate());
            System.out.println("Selesai : " + year.getEndDate());
            System.out.println("Status  : " + (year.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateAcademicYear() {
        requireAdminAction();
        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long id = readLong();

        AcademicYear year = academicService.getAcademicYearById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tahun ajaran tidak ditemukan."));

        System.out.print("Nama [" + year.getName() + "]: ");
        String name = readOptional(year.getName());

        System.out.print("Tanggal mulai [" + year.getStartDate() + "]: ");
        String startInput = scanner.nextLine().trim();

        System.out.print("Tanggal selesai [" + year.getEndDate() + "]: ");
        String endInput = scanner.nextLine().trim();

        LocalDate start = startInput.isEmpty()
                ? year.getStartDate()
                : parseDate(startInput);

        LocalDate end = endInput.isEmpty()
                ? year.getEndDate()
                : parseDate(endInput);

        academicService.updateAcademicYear(id, name, start, end);

        System.out.println("Tahun ajaran berhasil diperbarui.");
    }

    private void deactivateAcademicYear() {
        requireAdminAction();
        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long id = readLong();

        academicService.deactivateAcademicYear(id);

        System.out.println("Tahun ajaran berhasil dinonaktifkan.");
    }

    private void activateAcademicYear() {
        requireAdminAction();
        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long id = readLong();

        academicService.activateAcademicYear(id);

        System.out.println("Tahun ajaran berhasil diaktifkan.");
    }

    // ==========================================
    // SEMESTER
    // ==========================================

    private void semesterMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MANAJEMEN SEMESTER ==========");
            System.out.println("1. Tambah Semester");
            System.out.println("2. Lihat Semua Semester");
            System.out.println("3. Lihat Semester per Tahun Ajaran");
            System.out.println("4. Edit Semester");
            System.out.println("5. Nonaktifkan Semester");
            System.out.println("6. Aktifkan Semester");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> createSemester();
                    case "2" -> displaySemesters(academicService.getAllSemesters());
                    case "3" -> listSemestersByYear();
                    case "4" -> updateSemester();
                    case "5" -> toggleSemesterStatus(false);
                    case "6" -> toggleSemesterStatus(true);
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createSemester() {
        requireAdminAction();
        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long yearId = readLong();

        System.out.print("Nomor semester (1=Ganjil, 2=Genap): ");
        int number = readInt();

        System.out.print("Nama semester (contoh Ganjil 2026/2027): ");
        String name = scanner.nextLine();

        System.out.print("Tanggal mulai (YYYY-MM-DD): ");
        LocalDate start = readDate();

        System.out.print("Tanggal selesai (YYYY-MM-DD): ");
        LocalDate end = readDate();

        Semester semester = academicService.createSemester(yearId, number, name, start, end);
        System.out.println("Semester berhasil dibuat. ID: " + semester.getId());
    }

    private void listSemestersByYear() {
        displayAcademicYears();
        System.out.print("ID tahun ajaran: ");
        Long yearId = readLong();
        displaySemesters(academicService.getSemestersByAcademicYear(yearId));
    }

    private void displaySemesters(List<Semester> semesters) {
        if (semesters.isEmpty()) {
            System.out.println("Belum ada data semester.");
            return;
        }

        for (Semester s : semesters) {
            System.out.println("----------------------------------");
            System.out.println("ID           : " + s.getId());
            System.out.println("Tahun Ajaran : " + s.getAcademicYearId());
            System.out.println("Semester Ke  : " + s.getSemesterNumber());
            System.out.println("Nama         : " + s.getName());
            System.out.println("Periode      : " + s.getStartDate() + " s.d. " + s.getEndDate());
            System.out.println("Status       : " + (s.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateSemester() {
        requireAdminAction();
        displaySemesters(academicService.getAllSemesters());

        System.out.print("ID semester: ");
        Long id = readLong();

        Semester existing = academicService.getSemesterById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semester tidak ditemukan."));

        displayAcademicYears();
        System.out.print("ID tahun ajaran [" + existing.getAcademicYearId() + "]: ");
        String yearInput = scanner.nextLine().trim();
        Long yearId = yearInput.isEmpty() ? existing.getAcademicYearId() : parseLong(yearInput);

        System.out.print("Nomor semester (1/2) [" + existing.getSemesterNumber() + "]: ");
        String numInput = scanner.nextLine().trim();
        int num = numInput.isEmpty() ? existing.getSemesterNumber() : parseInt(numInput);

        System.out.print("Nama [" + existing.getName() + "]: ");
        String name = readOptional(existing.getName());

        System.out.print("Tanggal mulai [" + existing.getStartDate() + "]: ");
        String startInput = scanner.nextLine().trim();
        LocalDate start = startInput.isEmpty() ? existing.getStartDate() : parseDate(startInput);

        System.out.print("Tanggal selesai [" + existing.getEndDate() + "]: ");
        String endInput = scanner.nextLine().trim();
        LocalDate end = endInput.isEmpty() ? existing.getEndDate() : parseDate(endInput);

        academicService.updateSemester(id, yearId, num, name, start, end);
        System.out.println("Semester berhasil diperbarui.");
    }

    private void toggleSemesterStatus(boolean activate) {
        requireAdminAction();
        displaySemesters(academicService.getAllSemesters());

        System.out.print("ID semester: ");
        Long id = readLong();

        if (activate) {
            academicService.activateSemester(id);
            System.out.println("Semester berhasil diaktifkan.");
        } else {
            academicService.deactivateSemester(id);
            System.out.println("Semester berhasil dinonaktifkan.");
        }
    }

    // ==========================================
    // SCHOOL CLASS
    // ==========================================

    private void classMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MANAJEMEN KELAS ==========");
            System.out.println("1. Tambah Kelas");
            System.out.println("2. Lihat Semua Kelas");
            System.out.println("3. Edit Kelas");
            System.out.println("4. Nonaktifkan Kelas");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> createClass();
                    case "2" -> displayClasses();
                    case "3" -> updateClass();
                    case "4" -> deactivateClass();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createClass() {
        requireAdminAction();
        displayAcademicYears();

        System.out.print("ID tahun ajaran: ");
        Long yearId = readLong();

        System.out.print("Nama kelas (contoh 1A): ");
        String name = scanner.nextLine();

        System.out.print("Tingkat kelas (1-6): ");
        int grade = readInt();

        System.out.print("Kapasitas siswa: ");
        int capacity = readInt();

        SchoolClass schoolClass = academicService.createClass(
                yearId, name, grade, capacity);

        System.out.println("Kelas berhasil dibuat. ID: " + schoolClass.getId());
    }

    private void displayClasses() {

        List<SchoolClass> classes = academicService.getAllClasses();

        if (classes.isEmpty()) {
            System.out.println("Belum ada kelas.");
            return;
        }

        for (SchoolClass schoolClass : classes) {
            System.out.println("----------------------------------");
            System.out.println("ID           : " + schoolClass.getId());
            System.out.println("Tahun Ajaran : " + schoolClass.getAcademicYearId());
            System.out.println("Nama Kelas   : " + schoolClass.getName());
            System.out.println("Tingkat      : " + schoolClass.getGradeLevel());
            System.out.println("Kapasitas    : " + schoolClass.getCapacity());
            System.out.println("Status       : " + (schoolClass.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateClass() {
        requireAdminAction();
        displayClasses();

        System.out.print("ID kelas: ");
        Long id = readLong();

        SchoolClass schoolClass = academicService.getClassById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kelas tidak ditemukan."));

        displayAcademicYears();

        System.out.print("ID tahun ajaran [" + schoolClass.getAcademicYearId() + "]: ");
        String yearInput = scanner.nextLine().trim();

        Long yearId = yearInput.isEmpty()
                ? schoolClass.getAcademicYearId()
                : parseLong(yearInput);

        System.out.print("Nama kelas [" + schoolClass.getName() + "]: ");
        String name = readOptional(schoolClass.getName());

        System.out.print("Tingkat [" + schoolClass.getGradeLevel() + "]: ");
        String gradeInput = scanner.nextLine().trim();

        System.out.print("Kapasitas [" + schoolClass.getCapacity() + "]: ");
        String capacityInput = scanner.nextLine().trim();

        int grade = gradeInput.isEmpty()
                ? schoolClass.getGradeLevel()
                : parseInt(gradeInput);

        int capacity = capacityInput.isEmpty()
                ? schoolClass.getCapacity()
                : parseInt(capacityInput);

        academicService.updateClass(id, yearId, name, grade, capacity);

        System.out.println("Kelas berhasil diperbarui.");
    }

    private void deactivateClass() {
        requireAdminAction();
        displayClasses();

        System.out.print("ID kelas: ");
        Long id = readLong();

        academicService.deactivateClass(id);

        System.out.println("Kelas berhasil dinonaktifkan.");
    }

    // ==========================================
    // SUBJECT
    // ==========================================

    private void subjectMenu() {

        boolean active = true;

        while (active) {

            System.out.println("\n========== MATA PELAJARAN ==========");
            System.out.println("1. Tambah Mata Pelajaran");
            System.out.println("2. Lihat Semua Mata Pelajaran");
            System.out.println("3. Edit Mata Pelajaran");
            System.out.println("4. Nonaktifkan Mata Pelajaran");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> createSubject();
                    case "2" -> displaySubjects();
                    case "3" -> updateSubject();
                    case "4" -> deactivateSubject();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createSubject() {
        requireAdminAction();

        System.out.print("Kode mata pelajaran: ");
        String code = scanner.nextLine();

        System.out.print("Nama mata pelajaran: ");
        String name = scanner.nextLine();

        System.out.print("Deskripsi: ");
        String description = scanner.nextLine();

        Subject subject = academicService.createSubject(code, name, description);

        System.out.println("Mata pelajaran berhasil dibuat. ID: " + subject.getId());
    }

    private void displaySubjects() {

        List<Subject> subjects = academicService.getAllSubjects();

        if (subjects.isEmpty()) {
            System.out.println("Belum ada mata pelajaran.");
            return;
        }

        for (Subject subject : subjects) {
            System.out.println("----------------------------------");
            System.out.println("ID          : " + subject.getId());
            System.out.println("Kode        : " + subject.getCode());
            System.out.println("Nama        : " + subject.getName());
            System.out.println("Deskripsi   : " + subject.getDescription());
            System.out.println("Status      : " + (subject.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    private void updateSubject() {
        requireAdminAction();
        displaySubjects();

        System.out.print("ID mata pelajaran: ");
        Long id = readLong();

        Subject subject = academicService.getSubjectById(id)
                .orElseThrow(() -> new IllegalArgumentException("Mata pelajaran tidak ditemukan."));

        System.out.print("Kode [" + subject.getCode() + "]: ");
        String code = readOptional(subject.getCode());

        System.out.print("Nama [" + subject.getName() + "]: ");
        String name = readOptional(subject.getName());

        System.out.print("Deskripsi [" + subject.getDescription() + "]: ");
        String description = readOptional(subject.getDescription());

        academicService.updateSubject(id, code, name, description);

        System.out.println("Mata pelajaran berhasil diperbarui.");
    }

    private void deactivateSubject() {
        requireAdminAction();
        displaySubjects();

        System.out.print("ID mata pelajaran: ");
        Long id = readLong();

        academicService.deactivateSubject(id);

        System.out.println("Mata pelajaran berhasil dinonaktifkan.");
    }

    // ==========================================
    // HOMEROOM ASSIGNMENT
    // ==========================================

    private void homeroomMenu() {
        boolean active = true;

        while (active) {
            System.out.println("\n========== PENUGASAN WALI KELAS ==========");
            System.out.println("1. Tugaskan Wali Kelas");
            System.out.println("2. Lihat Semua Penugasan Wali Kelas");
            System.out.println("3. Cek Wali Kelas Aktif per Kelas");
            System.out.println("4. Akhiri Penugasan Wali Kelas");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> assignHomeroom();
                    case "2" -> displayHomerooms(academicService.getAllHomeroomAssignments());
                    case "3" -> checkActiveHomeroomByClass();
                    case "4" -> endHomeroom();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void assignHomeroom() {
        requireAdminAction();
        displayClasses();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        displayTeacherOptions();
        System.out.print("ID guru: ");
        Long teacherId = readLong();

        System.out.print("Tanggal mulai (YYYY-MM-DD): ");
        LocalDate startDate = readDate();

        HomeroomAssignment assignment = academicService.assignHomeroomTeacher(
                classId, teacherId, startDate);
        System.out.println("Wali kelas berhasil ditugaskan. ID Penugasan: " + assignment.getId());
    }

    private void checkActiveHomeroomByClass() {
        displayClasses();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        Optional<HomeroomAssignment> active = academicService.getActiveHomeroomByClass(classId);
        if (active.isEmpty()) {
            System.out.println("Kelas ini belum memiliki wali kelas aktif.");
        } else {
            displayHomerooms(List.of(active.get()));
        }
    }

    private void endHomeroom() {
        requireAdminAction();
        displayHomerooms(academicService.getAllHomeroomAssignments());

        System.out.print("ID penugasan wali kelas: ");
        Long assignmentId = readLong();

        System.out.print("Tanggal selesai (YYYY-MM-DD): ");
        LocalDate endDate = readDate();

        academicService.endHomeroomAssignment(assignmentId, endDate);
        System.out.println("Penugasan wali kelas berhasil diakhiri.");
    }

    private void displayHomerooms(List<HomeroomAssignment> assignments) {
        if (assignments.isEmpty()) {
            System.out.println("Belum ada penugasan wali kelas.");
            return;
        }

        for (HomeroomAssignment a : assignments) {
            System.out.println("----------------------------------");
            System.out.println("ID Penugasan : " + a.getId());
            System.out.println("ID Kelas     : " + a.getClassId());
            System.out.println("ID Guru      : " + a.getTeacherId());
            System.out.println("Mulai        : " + a.getStartDate());
            System.out.println("Selesai      : " + (a.getEndDate() != null ? a.getEndDate() : "-"));
            System.out.println("Status       : " + (a.isActive() ? "Aktif" : "Selesai"));
        }
    }

    // ==========================================
    // TEACHING ASSIGNMENT
    // ==========================================

    private void teachingAssignmentMenu() {
        boolean active = true;

        while (active) {
            System.out.println("\n========== PENUGASAN MENGAJAR ==========");
            System.out.println("1. Tambah Penugasan Mengajar");
            System.out.println("2. Lihat Semua Penugasan Mengajar");
            System.out.println("3. Lihat Penugasan per Guru");
            System.out.println("4. Lihat Penugasan per Kelas");
            System.out.println("5. Nonaktifkan Penugasan Mengajar");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> createTeachingAssignment();
                    case "2" -> displayTeachingAssignments(academicService.getAllTeachingAssignments());
                    case "3" -> listTeachingByTeacher();
                    case "4" -> listTeachingByClass();
                    case "5" -> deactivateTeachingAssignment();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void createTeachingAssignment() {
        requireAdminAction();

        displayTeacherOptions();
        System.out.print("ID guru: ");
        Long teacherId = readLong();

        displayClasses();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        displaySubjects();
        System.out.print("ID mata pelajaran: ");
        Long subjectId = readLong();

        displaySemesters(academicService.getAllSemesters());
        System.out.print("ID semester: ");
        Long semesterId = readLong();

        TeachingAssignment assignment = academicService.createTeachingAssignment(
                teacherId, classId, subjectId, semesterId);
        System.out.println("Penugasan mengajar berhasil dibuat. ID: " + assignment.getId());
    }

    private void listTeachingByTeacher() {
        displayTeacherOptions();
        System.out.print("ID guru: ");
        Long teacherId = readLong();
        displayTeachingAssignments(academicService.getTeachingAssignmentsByTeacher(teacherId));
    }

    private void listTeachingByClass() {
        displayClasses();
        System.out.print("ID kelas: ");
        Long classId = readLong();
        displayTeachingAssignments(academicService.getTeachingAssignmentsByClass(classId));
    }

    private void deactivateTeachingAssignment() {
        requireAdminAction();
        displayTeachingAssignments(academicService.getAllTeachingAssignments());

        System.out.print("ID penugasan mengajar: ");
        Long assignmentId = readLong();

        academicService.deactivateTeachingAssignment(assignmentId);
        System.out.println("Penugasan mengajar berhasil dinonaktifkan.");
    }

    private void displayTeachingAssignments(List<TeachingAssignment> list) {
        if (list.isEmpty()) {
            System.out.println("Belum ada data penugasan mengajar.");
            return;
        }

        for (TeachingAssignment a : list) {
            System.out.println("----------------------------------");
            System.out.println("ID         : " + a.getId());
            System.out.println("ID Guru    : " + a.getTeacherId());
            System.out.println("ID Kelas   : " + a.getClassId());
            System.out.println("ID Mapel   : " + a.getSubjectId());
            System.out.println("ID Semester: " + a.getSemesterId());
            System.out.println("Status     : " + (a.isActive() ? "Aktif" : "Nonaktif"));
        }
    }

    // ==========================================
    // STUDENT PLACEMENT
    // ==========================================

    private void placementMenu() {
        boolean active = true;

        while (active) {
            System.out.println("\n========== PENEMPATAN & RIWAYAT KELAS SISWA ==========");
            System.out.println("1. Tempatkan Siswa ke Kelas");
            System.out.println("2. Lihat Penempatan Aktif Siswa");
            System.out.println("3. Lihat Riwayat Kelas Siswa");
            System.out.println("4. Lihat Daftar Siswa Aktif per Kelas");
            System.out.println("5. Naik Kelas / Pindah Kelas Siswa");
            System.out.println("6. Selesaikan Penempatan (Lulus / Keluar)");
            System.out.println("0. Kembali");
            System.out.print("Pilih menu: ");

            try {
                switch (MenuNavigator.readMenuChoice(scanner)) {
                    case "1" -> placeStudent();
                    case "2" -> viewCurrentPlacement();
                    case "3" -> viewPlacementHistory();
                    case "4" -> viewActiveRosterByClass();
                    case "5" -> promoteOrTransferStudent();
                    case "6" -> completeStudentPlacement();
                    case "0" -> active = false;
                    default -> System.out.println("Pilihan tidak valid.");
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println("Gagal: " + e.getMessage());
            }
        }
    }

    private void placeStudent() {
        requireAdminAction();

        displayStudentOptions();
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        displayClasses();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        System.out.print("Tanggal masuk (YYYY-MM-DD): ");
        LocalDate entryDate = readDate();

        StudentPlacement placement = academicService.placeStudentInClass(
                studentId, classId, entryDate);
        System.out.println("Siswa berhasil ditempatkan. ID Riwayat: " + placement.getId());
    }

    private void viewCurrentPlacement() {
        displayStudentOptions();
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        Optional<StudentPlacement> placement = academicService.getCurrentPlacementByStudent(studentId);
        if (placement.isEmpty()) {
            System.out.println("Siswa tidak memiliki penempatan kelas aktif.");
        } else {
            displayPlacements(List.of(placement.get()));
        }
    }

    private void viewPlacementHistory() {
        displayStudentOptions();
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        displayPlacements(academicService.getPlacementHistoryByStudent(studentId));
    }

    private void viewActiveRosterByClass() {
        displayClasses();
        System.out.print("ID kelas: ");
        Long classId = readLong();

        displayPlacements(academicService.getActivePlacementsByClass(classId));
    }

    private void promoteOrTransferStudent() {
        requireAdminAction();

        displayStudentOptions();
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        displayClasses();
        System.out.print("ID kelas tujuan: ");
        Long targetClassId = readLong();

        System.out.print("Jenis perpindahan (NAIK_KELAS / PINDAH): ");
        String status = scanner.nextLine();

        System.out.print("Tanggal efektif (YYYY-MM-DD): ");
        LocalDate date = readDate();

        StudentPlacement placement = academicService.promoteOrTransferStudent(
                studentId, targetClassId, date, status);
        System.out.println("Siswa berhasil dipindahkan/dinaikkan kelas. ID Riwayat Baru: "
                + placement.getId());
    }

    private void completeStudentPlacement() {
        requireAdminAction();

        displayStudentOptions();
        System.out.print("ID siswa: ");
        Long studentId = readLong();

        System.out.print("Status akhir (LULUS / KELUAR): ");
        String status = scanner.nextLine();

        System.out.print("Tanggal keluar (YYYY-MM-DD): ");
        LocalDate exitDate = readDate();

        academicService.completeStudentPlacement(studentId, exitDate, status);
        System.out.println("Status akhir penempatan siswa berhasil disimpan.");
    }

    private void displayPlacements(List<StudentPlacement> placements) {
        if (placements.isEmpty()) {
            System.out.println("Belum ada data penempatan siswa.");
            return;
        }

        for (StudentPlacement p : placements) {
            System.out.println("----------------------------------");
            System.out.println("ID Riwayat   : " + p.getId());
            System.out.println("ID Siswa     : " + p.getStudentId());
            System.out.println("ID Kelas     : " + p.getClassId());
            System.out.println("Tahun Ajaran : " + p.getAcademicYearId());
            System.out.println("Tgl Masuk    : " + p.getEntryDate());
            System.out.println("Tgl Keluar   : " + (p.getExitDate() != null ? p.getExitDate() : "-"));
            System.out.println("Status       : " + p.getStatus());
        }
    }

    private void displayTeacherOptions() {
        if (teacherService == null) {
            return;
        }
        List<Teacher> teachers = teacherService.getAllTeachers();
        if (teachers.isEmpty()) {
            System.out.println("(Belum ada data guru terdaftar)");
            return;
        }
        System.out.println("-- Daftar Guru --");
        for (Teacher t : teachers) {
            System.out.println("  ID " + t.getId() + " | " + t.getNip() + " | " + t.getFullName()
                    + (t.isActive() ? "" : " [Nonaktif]"));
        }
    }

    private void displayStudentOptions() {
        if (studentService == null) {
            return;
        }
        List<Student> students = studentService.getAllStudents();
        if (students.isEmpty()) {
            System.out.println("(Belum ada data siswa terdaftar)");
            return;
        }
        System.out.println("-- Daftar Siswa --");
        for (Student s : students) {
            System.out.println("  ID " + s.getId() + " | " + s.getNis() + " | " + s.getFullName()
                    + (s.isActive() ? "" : " [Nonaktif]"));
        }
    }

    // ==========================================
    // INPUT HELPERS
    // ==========================================

    private LocalDate readDate() {
        return parseDate(scanner.nextLine().trim());
    }

    private LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format tanggal harus YYYY-MM-DD.");
        }
    }

    private Long readLong() {
        return parseLong(scanner.nextLine().trim());
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input harus berupa angka.");
        }
    }

    private int readInt() {
        return parseInt(scanner.nextLine().trim());
    }

    private int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input harus berupa angka.");
        }
    }

    private String readOptional(String currentValue) {
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? currentValue : input;
    }
}
