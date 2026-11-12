package id.ac.sekolah.manajemenkelas.core.session;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;

/**
 * Menyimpan informasi sesi pengguna yang sedang login (Singleton).
 */
public class SessionContext {

    private static SessionContext instans;

    private Long idPengguna;
    private Long idGuru;
    private String namaPengguna;
    private String namaLengkap;
    private String peran;

    private SessionContext() {
    }

    public static SessionContext getInstance() {
        if (instans == null) {
            instans = new SessionContext();
        }

        return instans;
    }

    public void startSession(
            Long idPengguna,
            String namaPengguna,
            String namaLengkap,
            String peran) {
        startSession(idPengguna, null, namaPengguna, namaLengkap, peran);
    }

    public void startSession(
            Long idPengguna,
            Long idGuru,
            String namaPengguna,
            String namaLengkap,
            String peran) {
        this.idPengguna = idPengguna;
        this.idGuru = idGuru;
        this.namaPengguna = namaPengguna;
        this.namaLengkap = namaLengkap;
        this.peran = peran;
    }

    public void endSession() {
        this.idPengguna = null;
        this.idGuru = null;
        this.namaPengguna = null;
        this.namaLengkap = null;
        this.peran = null;
    }

    public boolean isLoggedIn() {
        return idPengguna != null;
    }

    public Long getUserId() {
        return idPengguna;
    }

    public Long getTeacherId() {
        return idGuru;
    }

    public String getUsername() {
        return namaPengguna;
    }

    public String getFullName() {
        return namaLengkap;
    }

    public String getRole() {
        return peran;
    }

    public boolean hasRole(String peranDiharapkan) {
        return peran != null && peran.equalsIgnoreCase(peranDiharapkan);
    }

    /**
     * Mengembalikan null untuk ADMIN (akses menyeluruh) atau ID guru untuk TEACHER (akses terbatas).
     */
    public Long resolveTeacherScopeOrNullForAdmin(String labelFitur) {
        if (!isLoggedIn()) {
            throw new IllegalArgumentException(
                    "Silakan login terlebih dahulu untuk mengakses " + labelFitur + ".");
        }
        if (hasRole(RoleMapper.ROLE_ADMIN)) {
            return null;
        }
        if (hasRole(RoleMapper.ROLE_TEACHER)) {
            if (idGuru == null) {
                throw new IllegalArgumentException(
                        "Akses ditolak: akun guru belum terhubung dengan profil guru.");
            }
            return idGuru;
        }
        throw new IllegalArgumentException("Akses ditolak: peran tidak diizinkan.");
    }

    /**
     * Menolak perubahan data oleh pengguna non-ADMIN ketika sesi login aktif.
     */
    public void requireAdminIfAuthenticated(String labelSumberDaya) {
        if (isLoggedIn() && !hasRole(RoleMapper.ROLE_ADMIN)) {
            throw new IllegalArgumentException(
                    "Akses ditolak: hanya ADMIN yang dapat mengubah data " + labelSumberDaya + ".");
        }
    }
}
