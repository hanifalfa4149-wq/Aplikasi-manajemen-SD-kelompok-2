package id.ac.sekolah.manajemenkelas.presentation.cli.menu;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import id.ac.sekolah.manajemenkelas.auth.model.RoleMapper;
import id.ac.sekolah.manajemenkelas.core.session.SessionContext;

public final class MenuNavigator {

    private MenuNavigator() {
    }

    public static String readMenuChoice(Scanner scanner) {
        if (scanner == null || !scanner.hasNextLine()) {
            return "0";
        }
        return scanner.nextLine().trim();
    }

    public static Long readLong(Scanner scanner) {
        String raw = readRequiredLine(scanner, "ID harus berupa angka.");
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input harus berupa angka yang valid.");
        }
    }

    public static LocalDate readDate(Scanner scanner) {
        String raw = readRequiredLine(scanner, "Format tanggal harus YYYY-MM-DD.");
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Format tanggal tidak valid. Gunakan YYYY-MM-DD.");
        }
    }

    public static BigDecimal readBigDecimal(Scanner scanner) {
        String raw = readRequiredLine(scanner, "Input angka wajib diisi.");
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Input nilai harus berupa angka yang valid.");
        }
    }

    public static void executeSafely(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException e) {
            System.out.println("Gagal: " + e.getMessage());
        } catch (IllegalStateException e) {
            System.out.println("Kesalahan sistem/database: " + e.getMessage());
        }
    }

    public static boolean isAdmin() {
        return SessionContext.getInstance().hasRole(RoleMapper.ROLE_ADMIN);
    }

    public static boolean isTeacher() {
        return SessionContext.getInstance().hasRole(RoleMapper.ROLE_TEACHER);
    }

    private static String readRequiredLine(Scanner scanner, String errorMessage) {
        if (scanner == null || !scanner.hasNextLine()) {
            throw new IllegalArgumentException(errorMessage);
        }
        return scanner.nextLine().trim();
    }
}
