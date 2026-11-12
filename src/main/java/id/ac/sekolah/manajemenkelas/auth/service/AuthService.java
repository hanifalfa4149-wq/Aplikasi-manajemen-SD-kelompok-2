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

        daoOtentikasi.updateLastLogin(pengguna.getId());

        konteksSesi.startSession(
                pengguna.getId(),
                pengguna.getTeacherId(),
                pengguna.getUsername(),
                pengguna.getFullName(),
                pengguna.getRole());

        return true;
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
