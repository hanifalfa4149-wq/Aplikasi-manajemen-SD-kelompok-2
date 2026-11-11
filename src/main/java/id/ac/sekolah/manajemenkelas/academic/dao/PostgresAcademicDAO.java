package id.ac.sekolah.manajemenkelas.academic.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.academic.model.AcademicYear;
import id.ac.sekolah.manajemenkelas.academic.model.HomeroomAssignment;
import id.ac.sekolah.manajemenkelas.academic.model.SchoolClass;
import id.ac.sekolah.manajemenkelas.academic.model.Semester;
import id.ac.sekolah.manajemenkelas.academic.model.StudentPlacement;
import id.ac.sekolah.manajemenkelas.academic.model.Subject;
import id.ac.sekolah.manajemenkelas.academic.model.TeachingAssignment;
import id.ac.sekolah.manajemenkelas.config.DatabaseConfig;
import id.ac.sekolah.manajemenkelas.core.util.JdbcUtils;

/**
 * Implementasi JDBC PostgreSQL untuk AcademicDAO yang mengelola
 * tahun_ajaran, semester, kelas, mata_pelajaran, penugasan_wali_kelas,
 * penugasan_mengajar, dan riwayat_kelas_siswa.
 */
public class PostgresAcademicDAO implements AcademicDAO {

    // Academic Year SQL
    private static final String SQL_FIND_ALL_YEARS = """
            SELECT id, nama, tanggal_mulai, tanggal_selesai, aktif
            FROM tahun_ajaran
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_YEAR_BY_ID = """
            SELECT id, nama, tanggal_mulai, tanggal_selesai, aktif
            FROM tahun_ajaran
            WHERE id = ?
            """;

    private static final String SQL_FIND_YEAR_BY_NAME = """
            SELECT id, nama, tanggal_mulai, tanggal_selesai, aktif
            FROM tahun_ajaran
            WHERE LOWER(nama) = LOWER(?)
            """;

    private static final String SQL_DEACTIVATE_OTHER_YEARS = """
            UPDATE tahun_ajaran
            SET aktif = FALSE,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE aktif = TRUE
            """;

    private static final String SQL_DEACTIVATE_OTHER_YEARS_EXCEPT = """
            UPDATE tahun_ajaran
            SET aktif = FALSE,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE aktif = TRUE AND id <> ?
            """;

    private static final String SQL_INSERT_YEAR = """
            INSERT INTO tahun_ajaran (nama, tanggal_mulai, tanggal_selesai, aktif)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_YEAR = """
            UPDATE tahun_ajaran
            SET nama = ?,
                tanggal_mulai = ?,
                tanggal_selesai = ?,
                aktif = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    // Semester SQL
    private static final String SQL_FIND_ALL_SEMESTERS = """
            SELECT id, id_tahun_ajaran, nomor_semester, nama, tanggal_mulai, tanggal_selesai, aktif
            FROM semester
            ORDER BY id_tahun_ajaran ASC, nomor_semester ASC
            """;

    private static final String SQL_FIND_SEMESTERS_BY_YEAR = """
            SELECT id, id_tahun_ajaran, nomor_semester, nama, tanggal_mulai, tanggal_selesai, aktif
            FROM semester
            WHERE id_tahun_ajaran = ?
            ORDER BY nomor_semester ASC
            """;

    private static final String SQL_FIND_SEMESTER_BY_ID = """
            SELECT id, id_tahun_ajaran, nomor_semester, nama, tanggal_mulai, tanggal_selesai, aktif
            FROM semester
            WHERE id = ?
            """;

    private static final String SQL_INSERT_SEMESTER = """
            INSERT INTO semester (id_tahun_ajaran, nomor_semester, nama, tanggal_mulai, tanggal_selesai, aktif)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_SEMESTER = """
            UPDATE semester
            SET id_tahun_ajaran = ?,
                nomor_semester = ?,
                nama = ?,
                tanggal_mulai = ?,
                tanggal_selesai = ?,
                aktif = ?
            WHERE id = ?
            """;

    // School Class SQL
    private static final String SQL_FIND_ALL_CLASSES = """
            SELECT id, id_tahun_ajaran, nama, tingkat_kelas, kapasitas, aktif
            FROM kelas
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_CLASS_BY_ID = """
            SELECT id, id_tahun_ajaran, nama, tingkat_kelas, kapasitas, aktif
            FROM kelas
            WHERE id = ?
            """;

    private static final String SQL_INSERT_CLASS = """
            INSERT INTO kelas (id_tahun_ajaran, nama, tingkat_kelas, kapasitas, aktif)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_CLASS = """
            UPDATE kelas
            SET id_tahun_ajaran = ?,
                nama = ?,
                tingkat_kelas = ?,
                kapasitas = ?,
                aktif = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    // Subject SQL
    private static final String SQL_FIND_ALL_SUBJECTS = """
            SELECT id, kode, nama, deskripsi, aktif
            FROM mata_pelajaran
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_SUBJECT_BY_ID = """
            SELECT id, kode, nama, deskripsi, aktif
            FROM mata_pelajaran
            WHERE id = ?
            """;

    private static final String SQL_FIND_SUBJECT_BY_CODE = """
            SELECT id, kode, nama, deskripsi, aktif
            FROM mata_pelajaran
            WHERE LOWER(kode) = LOWER(?)
            """;

    private static final String SQL_INSERT_SUBJECT = """
            INSERT INTO mata_pelajaran (kode, nama, deskripsi, aktif)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_SUBJECT = """
            UPDATE mata_pelajaran
            SET kode = ?,
                nama = ?,
                deskripsi = ?,
                aktif = ?,
                diperbarui_pada = CURRENT_TIMESTAMP
            WHERE id = ?
            """;

    // Homeroom Assignment SQL
    private static final String SQL_FIND_ALL_HOMEROOMS = """
            SELECT id, id_kelas, id_guru, tanggal_mulai, tanggal_selesai, aktif
            FROM penugasan_wali_kelas
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_HOMEROOM_BY_ID = """
            SELECT id, id_kelas, id_guru, tanggal_mulai, tanggal_selesai, aktif
            FROM penugasan_wali_kelas
            WHERE id = ?
            """;

    private static final String SQL_FIND_ACTIVE_HOMEROOM_BY_CLASS = """
            SELECT id, id_kelas, id_guru, tanggal_mulai, tanggal_selesai, aktif
            FROM penugasan_wali_kelas
            WHERE id_kelas = ? AND aktif = TRUE
            """;

    private static final String SQL_INSERT_HOMEROOM = """
            INSERT INTO penugasan_wali_kelas (id_kelas, id_guru, tanggal_mulai, tanggal_selesai, aktif)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_HOMEROOM = """
            UPDATE penugasan_wali_kelas
            SET id_kelas = ?,
                id_guru = ?,
                tanggal_mulai = ?,
                tanggal_selesai = ?,
                aktif = ?
            WHERE id = ?
            """;

    // Teaching Assignment SQL
    private static final String SQL_FIND_ALL_TEACHINGS = """
            SELECT id, id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif
            FROM penugasan_mengajar
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_TEACHING_BY_ID = """
            SELECT id, id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif
            FROM penugasan_mengajar
            WHERE id = ?
            """;

    private static final String SQL_FIND_TEACHINGS_BY_TEACHER = """
            SELECT id, id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif
            FROM penugasan_mengajar
            WHERE id_guru = ?
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_TEACHINGS_BY_CLASS = """
            SELECT id, id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif
            FROM penugasan_mengajar
            WHERE id_kelas = ?
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_TEACHINGS_BY_SEMESTER = """
            SELECT id, id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif
            FROM penugasan_mengajar
            WHERE id_semester = ?
            ORDER BY id ASC
            """;

    private static final String SQL_INSERT_TEACHING = """
            INSERT INTO penugasan_mengajar (id_guru, id_kelas, id_mata_pelajaran, id_semester, aktif)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_TEACHING = """
            UPDATE penugasan_mengajar
            SET id_guru = ?,
                id_kelas = ?,
                id_mata_pelajaran = ?,
                id_semester = ?,
                aktif = ?
            WHERE id = ?
            """;

    // Student Placement SQL
    private static final String SQL_FIND_ALL_PLACEMENTS = """
            SELECT id, id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status
            FROM riwayat_kelas_siswa
            ORDER BY id ASC
            """;

    private static final String SQL_FIND_PLACEMENT_BY_ID = """
            SELECT id, id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status
            FROM riwayat_kelas_siswa
            WHERE id = ?
            """;

    private static final String SQL_FIND_ACTIVE_PLACEMENT_BY_STUDENT = """
            SELECT id, id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status
            FROM riwayat_kelas_siswa
            WHERE id_siswa = ? AND status = 'AKTIF' AND tanggal_keluar IS NULL
            """;

    private static final String SQL_FIND_PLACEMENTS_BY_STUDENT = """
            SELECT id, id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status
            FROM riwayat_kelas_siswa
            WHERE id_siswa = ?
            ORDER BY tanggal_masuk ASC, id ASC
            """;

    private static final String SQL_FIND_ACTIVE_PLACEMENTS_BY_CLASS = """
            SELECT id, id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status
            FROM riwayat_kelas_siswa
            WHERE id_kelas = ? AND status = 'AKTIF' AND tanggal_keluar IS NULL
            ORDER BY id ASC
            """;

    private static final String SQL_INSERT_PLACEMENT = """
            INSERT INTO riwayat_kelas_siswa (
                id_siswa, id_kelas, id_tahun_ajaran, tanggal_masuk, tanggal_keluar, status
            ) VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;

    private static final String SQL_UPDATE_PLACEMENT = """
            UPDATE riwayat_kelas_siswa
            SET id_siswa = ?,
                id_kelas = ?,
                id_tahun_ajaran = ?,
                tanggal_masuk = ?,
                tanggal_keluar = ?,
                status = ?
            WHERE id = ?
            """;

    // ==========================================
    // ACADEMIC YEAR
    // ==========================================

    @Override
    public List<AcademicYear> findAllAcademicYears() {
        List<AcademicYear> years = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_YEARS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                years.add(mapYearRow(rs));
            }
            return years;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar tahun ajaran.", e);
        }
    }

    @Override
    public Optional<AcademicYear> findAcademicYearById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_YEAR_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapYearRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari tahun ajaran berdasarkan ID.", e);
        }
    }

    @Override
    public Optional<AcademicYear> findAcademicYearByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_YEAR_BY_NAME)) {

            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapYearRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari tahun ajaran berdasarkan nama.", e);
        }
    }

    @Override
    public void saveAcademicYear(AcademicYear academicYear) {
        if (academicYear == null) {
            throw new IllegalArgumentException("Data tahun ajaran tidak boleh null.");
        }

        JdbcUtils.inTransactionVoid(conn -> {
            try {
                if (academicYear.isActive()) {
                    try (PreparedStatement deactivatePs = conn.prepareStatement(SQL_DEACTIVATE_OTHER_YEARS)) {
                        deactivatePs.executeUpdate();
                    }
                }

                try (PreparedStatement insertPs = conn.prepareStatement(SQL_INSERT_YEAR)) {
                    insertPs.setString(1, academicYear.getName().trim());
                    insertPs.setDate(2, Date.valueOf(academicYear.getStartDate()));
                    insertPs.setDate(3, Date.valueOf(academicYear.getEndDate()));
                    insertPs.setBoolean(4, academicYear.isActive());

                    try (ResultSet rs = insertPs.executeQuery()) {
                        if (rs.next()) {
                            academicYear.setId(rs.getLong(1));
                        }
                    }
                }
            } catch (SQLException e) {
                throw translateAcademicSqlException("Gagal menyimpan tahun ajaran.", e);
            }
        });
    }

    @Override
    public void updateAcademicYear(AcademicYear academicYear) {
        if (academicYear == null || academicYear.getId() == null) {
            throw new IllegalArgumentException("ID tahun ajaran wajib diisi untuk pembaruan.");
        }

        JdbcUtils.inTransactionVoid(conn -> {
            try {
                if (academicYear.isActive()) {
                    try (PreparedStatement deactivatePs = conn.prepareStatement(SQL_DEACTIVATE_OTHER_YEARS_EXCEPT)) {
                        deactivatePs.setLong(1, academicYear.getId());
                        deactivatePs.executeUpdate();
                    }
                }

                try (PreparedStatement updatePs = conn.prepareStatement(SQL_UPDATE_YEAR)) {
                    updatePs.setString(1, academicYear.getName().trim());
                    updatePs.setDate(2, Date.valueOf(academicYear.getStartDate()));
                    updatePs.setDate(3, Date.valueOf(academicYear.getEndDate()));
                    updatePs.setBoolean(4, academicYear.isActive());
                    updatePs.setLong(5, academicYear.getId());

                    int affected = updatePs.executeUpdate();
                    if (affected == 0) {
                        throw new IllegalArgumentException("Tahun ajaran tidak ditemukan.");
                    }
                }
            } catch (SQLException e) {
                throw translateAcademicSqlException("Gagal memperbarui tahun ajaran.", e);
            }
        });
    }

    // ==========================================
    // SEMESTER
    // ==========================================

    @Override
    public List<Semester> findAllSemesters() {
        List<Semester> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_SEMESTERS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapSemesterRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar semester.", e);
        }
    }

    @Override
    public List<Semester> findSemestersByAcademicYearId(Long academicYearId) {
        if (academicYearId == null) {
            return List.of();
        }
        List<Semester> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_SEMESTERS_BY_YEAR)) {

            ps.setLong(1, academicYearId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSemesterRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil semester berdasarkan tahun ajaran.", e);
        }
    }

    @Override
    public Optional<Semester> findSemesterById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_SEMESTER_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapSemesterRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari semester berdasarkan ID.", e);
        }
    }

    @Override
    public void saveSemester(Semester semester) {
        if (semester == null) {
            throw new IllegalArgumentException("Data semester tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_SEMESTER)) {

            ps.setLong(1, semester.getAcademicYearId());
            ps.setInt(2, semester.getSemesterNumber());
            ps.setString(3, semester.getName().trim());
            ps.setDate(4, Date.valueOf(semester.getStartDate()));
            ps.setDate(5, Date.valueOf(semester.getEndDate()));
            ps.setBoolean(6, semester.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    semester.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal menyimpan data semester.", e);
        }
    }

    @Override
    public void updateSemester(Semester semester) {
        if (semester == null || semester.getId() == null) {
            throw new IllegalArgumentException("ID semester wajib diisi untuk pembaruan.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_SEMESTER)) {

            ps.setLong(1, semester.getAcademicYearId());
            ps.setInt(2, semester.getSemesterNumber());
            ps.setString(3, semester.getName().trim());
            ps.setDate(4, Date.valueOf(semester.getStartDate()));
            ps.setDate(5, Date.valueOf(semester.getEndDate()));
            ps.setBoolean(6, semester.isActive());
            ps.setLong(7, semester.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data semester tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal memperbarui data semester.", e);
        }
    }

    // ==========================================
    // SCHOOL CLASS
    // ==========================================

    @Override
    public List<SchoolClass> findAllClasses() {
        List<SchoolClass> classes = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_CLASSES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                classes.add(mapClassRow(rs));
            }
            return classes;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar kelas.", e);
        }
    }

    @Override
    public Optional<SchoolClass> findClassById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_CLASS_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapClassRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari kelas berdasarkan ID.", e);
        }
    }

    @Override
    public void saveClass(SchoolClass schoolClass) {
        if (schoolClass == null) {
            throw new IllegalArgumentException("Data kelas tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_CLASS)) {

            ps.setLong(1, schoolClass.getAcademicYearId());
            ps.setString(2, schoolClass.getName().trim());
            ps.setInt(3, schoolClass.getGradeLevel());
            ps.setInt(4, schoolClass.getCapacity());
            ps.setBoolean(5, schoolClass.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    schoolClass.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal menyimpan data kelas.", e);
        }
    }

    @Override
    public void updateClass(SchoolClass schoolClass) {
        if (schoolClass == null || schoolClass.getId() == null) {
            throw new IllegalArgumentException("ID kelas wajib diisi untuk pembaruan.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CLASS)) {

            ps.setLong(1, schoolClass.getAcademicYearId());
            ps.setString(2, schoolClass.getName().trim());
            ps.setInt(3, schoolClass.getGradeLevel());
            ps.setInt(4, schoolClass.getCapacity());
            ps.setBoolean(5, schoolClass.isActive());
            ps.setLong(6, schoolClass.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data kelas tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal memperbarui data kelas.", e);
        }
    }

    // ==========================================
    // SUBJECT
    // ==========================================

    @Override
    public List<Subject> findAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_SUBJECTS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                subjects.add(mapSubjectRow(rs));
            }
            return subjects;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar mata pelajaran.", e);
        }
    }

    @Override
    public Optional<Subject> findSubjectById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_SUBJECT_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapSubjectRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari mata pelajaran berdasarkan ID.", e);
        }
    }

    @Override
    public Optional<Subject> findSubjectByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_SUBJECT_BY_CODE)) {

            ps.setString(1, code.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapSubjectRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari mata pelajaran berdasarkan kode.", e);
        }
    }

    @Override
    public void saveSubject(Subject subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Data mata pelajaran tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_SUBJECT)) {

            ps.setString(1, subject.getCode().trim().toUpperCase());
            ps.setString(2, subject.getName().trim());
            JdbcUtils.setNullableString(ps, 3, subject.getDescription());
            ps.setBoolean(4, subject.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    subject.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal menyimpan mata pelajaran.", e);
        }
    }

    @Override
    public void updateSubject(Subject subject) {
        if (subject == null || subject.getId() == null) {
            throw new IllegalArgumentException("ID mata pelajaran wajib diisi untuk pembaruan.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_SUBJECT)) {

            ps.setString(1, subject.getCode().trim().toUpperCase());
            ps.setString(2, subject.getName().trim());
            JdbcUtils.setNullableString(ps, 3, subject.getDescription());
            ps.setBoolean(4, subject.isActive());
            ps.setLong(5, subject.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Mata pelajaran tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal memperbarui mata pelajaran.", e);
        }
    }

    // ==========================================
    // HOMEROOM ASSIGNMENT
    // ==========================================

    @Override
    public List<HomeroomAssignment> findAllHomeroomAssignments() {
        List<HomeroomAssignment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_HOMEROOMS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapHomeroomRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar penugasan wali kelas.", e);
        }
    }

    @Override
    public Optional<HomeroomAssignment> findHomeroomAssignmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_HOMEROOM_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapHomeroomRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari penugasan wali kelas.", e);
        }
    }

    @Override
    public Optional<HomeroomAssignment> findActiveHomeroomByClassId(Long classId) {
        if (classId == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE_HOMEROOM_BY_CLASS)) {

            ps.setLong(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapHomeroomRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari wali kelas aktif.", e);
        }
    }

    @Override
    public void saveHomeroomAssignment(HomeroomAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Data penugasan wali kelas tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_HOMEROOM)) {

            ps.setLong(1, assignment.getClassId());
            ps.setLong(2, assignment.getTeacherId());
            ps.setDate(3, Date.valueOf(assignment.getStartDate()));
            if (assignment.getEndDate() != null) {
                ps.setDate(4, Date.valueOf(assignment.getEndDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setBoolean(5, assignment.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    assignment.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal menyimpan penugasan wali kelas.", e);
        }
    }

    @Override
    public void updateHomeroomAssignment(HomeroomAssignment assignment) {
        if (assignment == null || assignment.getId() == null) {
            throw new IllegalArgumentException("ID penugasan wali kelas wajib diisi.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_HOMEROOM)) {

            ps.setLong(1, assignment.getClassId());
            ps.setLong(2, assignment.getTeacherId());
            ps.setDate(3, Date.valueOf(assignment.getStartDate()));
            if (assignment.getEndDate() != null) {
                ps.setDate(4, Date.valueOf(assignment.getEndDate()));
            } else {
                ps.setNull(4, Types.DATE);
            }
            ps.setBoolean(5, assignment.isActive());
            ps.setLong(6, assignment.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Penugasan wali kelas tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal memperbarui penugasan wali kelas.", e);
        }
    }

    // ==========================================
    // TEACHING ASSIGNMENT
    // ==========================================

    @Override
    public List<TeachingAssignment> findAllTeachingAssignments() {
        List<TeachingAssignment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_TEACHINGS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapTeachingRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar penugasan mengajar.", e);
        }
    }

    @Override
    public Optional<TeachingAssignment> findTeachingAssignmentById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_TEACHING_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapTeachingRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari penugasan mengajar.", e);
        }
    }

    @Override
    public List<TeachingAssignment> findTeachingAssignmentsByTeacherId(Long teacherId) {
        return findTeachingsByLong(SQL_FIND_TEACHINGS_BY_TEACHER, teacherId,
                "Gagal mengambil penugasan mengajar guru.");
    }

    @Override
    public List<TeachingAssignment> findTeachingAssignmentsByClassId(Long classId) {
        return findTeachingsByLong(SQL_FIND_TEACHINGS_BY_CLASS, classId,
                "Gagal mengambil penugasan mengajar kelas.");
    }

    @Override
    public List<TeachingAssignment> findTeachingAssignmentsBySemesterId(Long semesterId) {
        return findTeachingsByLong(SQL_FIND_TEACHINGS_BY_SEMESTER, semesterId,
                "Gagal mengambil penugasan mengajar semester.");
    }

    private List<TeachingAssignment> findTeachingsByLong(String sql, Long id, String errMsg) {
        if (id == null) {
            return List.of();
        }
        List<TeachingAssignment> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapTeachingRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(errMsg, e);
        }
    }

    @Override
    public void saveTeachingAssignment(TeachingAssignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Data penugasan mengajar tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT_TEACHING)) {

            ps.setLong(1, assignment.getTeacherId());
            ps.setLong(2, assignment.getClassId());
            ps.setLong(3, assignment.getSubjectId());
            ps.setLong(4, assignment.getSemesterId());
            ps.setBoolean(5, assignment.isActive());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    assignment.setId(rs.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal menyimpan penugasan mengajar.", e);
        }
    }

    @Override
    public void updateTeachingAssignment(TeachingAssignment assignment) {
        if (assignment == null || assignment.getId() == null) {
            throw new IllegalArgumentException("ID penugasan mengajar wajib diisi.");
        }

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_TEACHING)) {

            ps.setLong(1, assignment.getTeacherId());
            ps.setLong(2, assignment.getClassId());
            ps.setLong(3, assignment.getSubjectId());
            ps.setLong(4, assignment.getSemesterId());
            ps.setBoolean(5, assignment.isActive());
            ps.setLong(6, assignment.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Penugasan mengajar tidak ditemukan.");
            }
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal memperbarui penugasan mengajar.", e);
        }
    }

    // ==========================================
    // STUDENT PLACEMENT
    // ==========================================

    @Override
    public List<StudentPlacement> findAllStudentPlacements() {
        List<StudentPlacement> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ALL_PLACEMENTS);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapPlacementRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mengambil daftar penempatan siswa.", e);
        }
    }

    @Override
    public Optional<StudentPlacement> findStudentPlacementById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_PLACEMENT_BY_ID)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapPlacementRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari penempatan siswa.", e);
        }
    }

    @Override
    public Optional<StudentPlacement> findActivePlacementByStudentId(Long studentId) {
        if (studentId == null) {
            return Optional.empty();
        }
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_FIND_ACTIVE_PLACEMENT_BY_STUDENT)) {

            ps.setLong(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapPlacementRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Gagal mencari penempatan aktif siswa.", e);
        }
    }

    @Override
    public List<StudentPlacement> findPlacementHistoryByStudentId(Long studentId) {
        return findPlacementsByLong(SQL_FIND_PLACEMENTS_BY_STUDENT, studentId,
                "Gagal mengambil riwayat kelas siswa.");
    }

    @Override
    public List<StudentPlacement> findActivePlacementsByClassId(Long classId) {
        return findPlacementsByLong(SQL_FIND_ACTIVE_PLACEMENTS_BY_CLASS, classId,
                "Gagal mengambil daftar siswa aktif pada kelas.");
    }

    private List<StudentPlacement> findPlacementsByLong(String sql, Long id, String errMsg) {
        if (id == null) {
            return List.of();
        }
        List<StudentPlacement> list = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapPlacementRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException(errMsg, e);
        }
    }

    @Override
    public void saveStudentPlacement(StudentPlacement placement) {
        if (placement == null) {
            throw new IllegalArgumentException("Data penempatan siswa tidak boleh null.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            insertPlacement(conn, placement);
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal menyimpan penempatan siswa.", e);
        }
    }

    @Override
    public void updateStudentPlacement(StudentPlacement placement) {
        if (placement == null || placement.getId() == null) {
            throw new IllegalArgumentException("ID penempatan siswa wajib diisi.");
        }

        try (Connection conn = DatabaseConfig.getConnection()) {
            updatePlacement(conn, placement);
        } catch (SQLException e) {
            throw translateAcademicSqlException("Gagal memperbarui penempatan siswa.", e);
        }
    }

    @Override
    public void transitionStudentPlacement(StudentPlacement previousPlacement, StudentPlacement newPlacement) {
        if (previousPlacement == null || previousPlacement.getId() == null || newPlacement == null) {
            throw new IllegalArgumentException("Data transisi penempatan siswa tidak valid.");
        }

        JdbcUtils.inTransactionVoid(conn -> {
            try {
                updatePlacement(conn, previousPlacement);
                insertPlacement(conn, newPlacement);
            } catch (SQLException e) {
                throw translateAcademicSqlException("Gagal memindahkan atau menaikkan kelas siswa.", e);
            }
        });
    }

    private void insertPlacement(Connection conn, StudentPlacement placement) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT_PLACEMENT)) {
            ps.setLong(1, placement.getStudentId());
            ps.setLong(2, placement.getClassId());
            ps.setLong(3, placement.getAcademicYearId());
            ps.setDate(4, Date.valueOf(placement.getEntryDate()));
            if (placement.getExitDate() != null) {
                ps.setDate(5, Date.valueOf(placement.getExitDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, placement.getStatus());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    placement.setId(rs.getLong(1));
                }
            }
        }
    }

    private void updatePlacement(Connection conn, StudentPlacement placement) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PLACEMENT)) {
            ps.setLong(1, placement.getStudentId());
            ps.setLong(2, placement.getClassId());
            ps.setLong(3, placement.getAcademicYearId());
            ps.setDate(4, Date.valueOf(placement.getEntryDate()));
            if (placement.getExitDate() != null) {
                ps.setDate(5, Date.valueOf(placement.getExitDate()));
            } else {
                ps.setNull(5, Types.DATE);
            }
            ps.setString(6, placement.getStatus());
            ps.setLong(7, placement.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) {
                throw new IllegalArgumentException("Data penempatan siswa tidak ditemukan.");
            }
        }
    }

    // ==========================================
    // ROW MAPPERS & EXCEPTION TRANSLATION
    // ==========================================

    private AcademicYear mapYearRow(ResultSet rs) throws SQLException {
        return new AcademicYear(
                rs.getLong("id"),
                rs.getString("nama"),
                rs.getDate("tanggal_mulai").toLocalDate(),
                rs.getDate("tanggal_selesai").toLocalDate(),
                rs.getBoolean("aktif"));
    }

    private Semester mapSemesterRow(ResultSet rs) throws SQLException {
        return new Semester(
                rs.getLong("id"),
                rs.getLong("id_tahun_ajaran"),
                rs.getInt("nomor_semester"),
                rs.getString("nama"),
                rs.getDate("tanggal_mulai").toLocalDate(),
                rs.getDate("tanggal_selesai").toLocalDate(),
                rs.getBoolean("aktif"));
    }

    private SchoolClass mapClassRow(ResultSet rs) throws SQLException {
        return new SchoolClass(
                rs.getLong("id"),
                rs.getLong("id_tahun_ajaran"),
                rs.getString("nama"),
                rs.getInt("tingkat_kelas"),
                rs.getInt("kapasitas"),
                rs.getBoolean("aktif"));
    }

    private Subject mapSubjectRow(ResultSet rs) throws SQLException {
        return new Subject(
                rs.getLong("id"),
                rs.getString("kode"),
                rs.getString("nama"),
                JdbcUtils.emptyIfNull(rs.getString("deskripsi")),
                rs.getBoolean("aktif"));
    }

    private HomeroomAssignment mapHomeroomRow(ResultSet rs) throws SQLException {
        Date endDateSql = rs.getDate("tanggal_selesai");
        return new HomeroomAssignment(
                rs.getLong("id"),
                rs.getLong("id_kelas"),
                rs.getLong("id_guru"),
                rs.getDate("tanggal_mulai").toLocalDate(),
                endDateSql != null ? endDateSql.toLocalDate() : null,
                rs.getBoolean("aktif"));
    }

    private TeachingAssignment mapTeachingRow(ResultSet rs) throws SQLException {
        return new TeachingAssignment(
                rs.getLong("id"),
                rs.getLong("id_guru"),
                rs.getLong("id_kelas"),
                rs.getLong("id_mata_pelajaran"),
                rs.getLong("id_semester"),
                rs.getBoolean("aktif"));
    }

    private StudentPlacement mapPlacementRow(ResultSet rs) throws SQLException {
        Date exitDateSql = rs.getDate("tanggal_keluar");
        return new StudentPlacement(
                rs.getLong("id"),
                rs.getLong("id_siswa"),
                rs.getLong("id_kelas"),
                rs.getLong("id_tahun_ajaran"),
                rs.getDate("tanggal_masuk").toLocalDate(),
                exitDateSql != null ? exitDateSql.toLocalDate() : null,
                rs.getString("status"));
    }

    private static RuntimeException translateAcademicSqlException(String contextMessage, SQLException e) {
        String sqlState = e.getSQLState();
        if ("23505".equals(sqlState)) {
            return new IllegalArgumentException("Data akademik duplikat atau melanggar keunikan aktif.", e);
        }
        if ("23503".equals(sqlState)) {
            return new IllegalArgumentException("Referensi data akademik terkait tidak ditemukan atau masih digunakan.", e);
        }
        if ("23514".equals(sqlState)) {
            return new IllegalArgumentException("Data akademik tidak memenuhi batasan nilai yang diizinkan.", e);
        }
        if ("22001".equals(sqlState)) {
            return new IllegalArgumentException("Panjang data teks akademik melebihi batas kolom database.", e);
        }
        return new IllegalStateException(contextMessage, e);
    }
}
