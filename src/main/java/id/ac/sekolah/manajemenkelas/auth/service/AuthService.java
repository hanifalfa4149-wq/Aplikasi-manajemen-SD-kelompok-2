package id.ac.sekolah.manajemenkelas.auth.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import id.ac.sekolah.manajemenkelas.auth.dao.AuthDAO;
import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.auth.model.User;
import id.ac.sekolah.manajemenkelas.core.security.PasswordHasher;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

public class AuthService {

    private final AuthDAO daoOtentikasi;
    private final PasswordHasher pengacakSandi;
    private final SessionContext konteksSesi;

    public AuthService(AuthDAO daoOtentikasi) {
        this(daoOtentikasi, new PasswordHasher());
    }

    public AuthService(AuthDAO daoOtentikasi, PasswordHasher pengacakSandi) {
        this.daoOtentikasi = daoOtentikasi;
        this.pengacakSandi = pengacakSandi;
        this.konteksSesi = SessionContext.getInstance();
    }

    public boolean login(String namaPengguna, String kataSandi) {
        return login(namaPengguna, kataSandi, null);
    }

    public boolean login(String namaPengguna, String kataSandi, String peranDiharapkan) {
        if (namaPengguna == null || namaPengguna.isBlank()
                || kataSandi == null || kataSandi.isBlank()) {
            return false;
        }

        Optional<User> penggunaOpt = daoOtentikasi.findByUsername(namaPengguna.trim());

        if (penggunaOpt.isEmpty()) {
            return false;
        }

        User pengguna = penggunaOpt.get();

        if (!pengguna.isActive()) {
            return false;
        }

        if (!pengacakSandi.verifyPassword(
                kataSandi, pengguna.getPasswordHash(), pengguna.getPasswordSalt())) {
            return false;
        }

        if (peranDiharapkan != null && !peranDiharapkan.isBlank()) {
            String peranNormalisasi = RoleMapper.fromDatabaseRole(RoleMapper.toDatabaseRole(peranDiharapkan));
            if (!peranNormalisasi.equalsIgnoreCase(pengguna.getRole())) {
                throw new IllegalArgumentException(
                        "Akun ini terdaftar sebagai " + formatLabelPeran(pengguna.getRole())
                                + ", bukan " + formatLabelPeran(peranNormalisasi) + ".");
            }
        }

        daoOtentikasi.updateLastLogin(pengguna.getId());

        konteksSesi.startSession(
                pengguna.getId(),
                pengguna.getTeacherId(),
                pengguna.getUsername(),
                pengguna.getFullName(),
                pengguna.getRole());

        return true;
    }

    public User register(String namaPengguna, String kataSandi, String peran) {
        if (namaPengguna == null || namaPengguna.isBlank()) {
            throw new IllegalArgumentException("Username wajib diisi.");
        }

        String usernameBersih = namaPengguna.trim();
        if (usernameBersih.contains(" ")) {
            throw new IllegalArgumentException("Username tidak boleh mengandung spasi.");
        }
        if (usernameBersih.length() < 3) {
            throw new IllegalArgumentException("Username minimal 3 karakter.");
        }

        if (kataSandi == null || kataSandi.isBlank()) {
            throw new IllegalArgumentException("Password wajib diisi.");
        }
        if (kataSandi.length() < 4) {
            throw new IllegalArgumentException("Password minimal 4 karakter.");
        }

        String peranNormalisasi = RoleMapper.fromDatabaseRole(RoleMapper.toDatabaseRole(peran));

        if (daoOtentikasi.existsByUsername(usernameBersih)) {
            throw new IllegalArgumentException(
                    "Username \"" + usernameBersih + "\" sudah terdaftar. Silakan gunakan username lain.");
        }

        String salt = pengacakSandi.generateSalt();
        String hash = pengacakSandi.hashPassword(kataSandi, salt);

        User akunBaru = new User(
                null,
                null,
                usernameBersih,
                hash,
                salt,
                usernameBersih,
                peranNormalisasi,
                true,
                null);
        daoOtentikasi.save(akunBaru);
        return akunBaru;
    }

    private static String formatLabelPeran(String peran) {
        if (RoleMapper.ROLE_ADMIN.equalsIgnoreCase(peran)) {
            return "Admin";
        }
        if (RoleMapper.ROLE_TEACHER.equalsIgnoreCase(peran)) {
            return "Guru";
        }
        return peran;
    }

    public void logout() {
        konteksSesi.endSession();
    }

    public boolean isAuthenticated() {
        return konteksSesi.isLoggedIn();
    }

    public boolean hasRole(String peran) {
        return konteksSesi.hasRole(peran);
    }

    public Optional<User> getAccountByTeacherId(Long idGuru) {
        konteksSesi.requireAdminIfAuthenticated("akun guru");
        if (idGuru == null) {
            return Optional.empty();
        }
        return daoOtentikasi.findByTeacherId(idGuru);
    }

    public Map<Long, User> getTeacherAccountsMap() {
        konteksSesi.requireAdminIfAuthenticated("akun guru");
        Map<Long, User> petaAkun = new HashMap<>();
        for (User pengguna : daoOtentikasi.findAll()) {
            if (pengguna.getTeacherId() != null) {
                petaAkun.put(pengguna.getTeacherId(), pengguna);
            }
        }
        return petaAkun;
    }

    public User saveOrUpdateTeacherAccount(
            Long idGuru,
            String namaPengguna,
            String kataSandiBaru,
            String namaLengkap,
            boolean aktif) {
        konteksSesi.requireAdminIfAuthenticated("akun guru");
        if (idGuru == null) {
            throw new IllegalArgumentException("Profil guru wajib dipilih terlebih dahulu.");
        }
        if (namaPengguna == null || namaPengguna.isBlank()) {
            throw new IllegalArgumentException("Username akun guru wajib diisi.");
        }

        String usernameBersih = namaPengguna.trim();
        if (usernameBersih.contains(" ")) {
            throw new IllegalArgumentException("Username tidak boleh mengandung spasi.");
        }

        String namaTampil = (namaLengkap != null && !namaLengkap.isBlank())
                ? namaLengkap.trim()
                : usernameBersih;

        Optional<User> akunGuruOpt = daoOtentikasi.findByTeacherId(idGuru);
        Optional<User> akunUsernameOpt = daoOtentikasi.findByUsername(usernameBersih);

        if (akunGuruOpt.isPresent()) {
            User akun = akunGuruOpt.get();
            if (akunUsernameOpt.isPresent() && !akunUsernameOpt.get().getId().equals(akun.getId())) {
                throw new IllegalArgumentException(
                        "Username \"" + usernameBersih + "\" sudah digunakan oleh akun lain.");
            }
            akun.setUsername(usernameBersih);
            akun.setFullName(namaTampil);
            akun.setActive(aktif);
            if (kataSandiBaru != null && !kataSandiBaru.isBlank()) {
                String salt = pengacakSandi.generateSalt();
                akun.setPasswordSalt(salt);
                akun.setPasswordHash(pengacakSandi.hashPassword(kataSandiBaru, salt));
            }
            daoOtentikasi.update(akun);
            return akun;
        }

        if (akunUsernameOpt.isPresent()) {
            User akunAda = akunUsernameOpt.get();
            if (RoleMapper.ROLE_TEACHER.equals(akunAda.getRole()) && akunAda.getTeacherId() == null) {
                akunAda.setTeacherId(idGuru);
                akunAda.setUsername(usernameBersih);
                akunAda.setFullName(namaTampil);
                akunAda.setActive(aktif);
                if (kataSandiBaru != null && !kataSandiBaru.isBlank()) {
                    String salt = pengacakSandi.generateSalt();
                    akunAda.setPasswordSalt(salt);
                    akunAda.setPasswordHash(pengacakSandi.hashPassword(kataSandiBaru, salt));
                }
                daoOtentikasi.update(akunAda);
                return akunAda;
            }
            throw new IllegalArgumentException(
                    "Username \"" + usernameBersih + "\" sudah digunakan oleh akun lain.");
        }

        if (kataSandiBaru == null || kataSandiBaru.isBlank()) {
            throw new IllegalArgumentException("Kata sandi wajib diisi untuk membuat akun guru baru.");
        }

        String salt = pengacakSandi.generateSalt();
        String hash = pengacakSandi.hashPassword(kataSandiBaru, salt);
        User akunBaru = new User(
                null,
                idGuru,
                usernameBersih,
                hash,
                salt,
                namaTampil,
                RoleMapper.ROLE_TEACHER,
                aktif,
                null);
        daoOtentikasi.save(akunBaru);
        return akunBaru;
    }
}
