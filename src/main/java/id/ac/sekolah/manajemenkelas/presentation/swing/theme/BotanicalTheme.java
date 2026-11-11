package id.ac.sekolah.manajemenkelas.presentation.swing.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Set;
import javax.swing.UIManager;

/**
 * Sistem desain terpusat Claymorphism Botanical Green untuk aplikasi desktop Java Swing.
 * Seluruh warna, tipografi, radius, dan spacing merujuk ke kelas ini.
 */
public final class BotanicalTheme {

    // ==========================================
    // COLOR TOKENS
    // ==========================================
    public static final Color EVERGREEN = new Color(0x17, 0x4C, 0x3C);
    public static final Color DEEP_FOREST = new Color(0x12, 0x3B, 0x30);
    public static final Color SAGE = new Color(0x8F, 0xAF, 0x9B);
    public static final Color SOFT_SAGE = new Color(0xDC, 0xE9, 0xDE);
    public static final Color MINT = new Color(0xC8, 0xE8, 0xD5);
    public static final Color PALE_MINT = new Color(0xEA, 0xF5, 0xED);
    public static final Color SOFT_IVORY = new Color(0xF1, 0xF6, 0xF0);
    public static final Color WHITE = new Color(0xFF, 0xFF, 0xFF);
    public static final Color TEXT_PRIMARY = new Color(0x24, 0x3B, 0x30);
    public static final Color TEXT_SECONDARY = new Color(0x71, 0x84, 0x77);
    public static final Color BORDER = new Color(0xDC, 0xE6, 0xDD);
    public static final Color SUCCESS = new Color(0x3E, 0x76, 0x5B);
    public static final Color WARNING = new Color(0xD5, 0xA4, 0x51);
    public static final Color DANGER = new Color(0xC6, 0x6B, 0x68);

    // Subtle translucent derivations strictly anchored to the botanical palette
    public static final Color CLAY_SHADOW = new Color(DEEP_FOREST.getRed(), DEEP_FOREST.getGreen(), DEEP_FOREST.getBlue(), 22);
    public static final Color CLAY_SHADOW_SOFT = new Color(DEEP_FOREST.getRed(), DEEP_FOREST.getGreen(), DEEP_FOREST.getBlue(), 12);
    public static final Color CLAY_HIGHLIGHT = new Color(255, 255, 255, 215);
    public static final Color ROW_ALT = new Color(0xF8, 0xFB, 0xF7);
    public static final Color WARNING_BG = new Color(0xFC, 0xF4, 0xE4);
    public static final Color DANGER_BG = new Color(0xFA, 0xEB, 0xEA);
    public static final Color SIDEBAR_DIVIDER = new Color(255, 255, 255, 28);
    public static final Color SIDEBAR_PROFILE_BG = new Color(0x12, 0x3B, 0x30);

    // ==========================================
    // GEOMETRY & SPACING TOKENS
    // ==========================================
    public static final int RADIUS_CARD = 18;
    public static final int RADIUS_CONTROL = 10;
    public static final int RADIUS_PILL = 16;
    public static final int SIDEBAR_WIDTH = 256;
    public static final int HEADER_HEIGHT = 76;
    public static final int INPUT_HEIGHT = 38;
    public static final int PAGE_PADDING = 24;
    public static final int SECTION_GAP = 18;

    // ==========================================
    // TYPOGRAPHY TOKENS
    // ==========================================
    private static final String FONT_FAMILY = resolveFontFamily();

    public static final Font FONT_DISPLAY = new Font(FONT_FAMILY, Font.BOLD, 24);
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 20);
    public static final Font FONT_SUBTITLE = new Font(FONT_FAMILY, Font.BOLD, 15);
    public static final Font FONT_BODY = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font(FONT_FAMILY, Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_BOLD = new Font(FONT_FAMILY, Font.BOLD, 12);
    public static final Font FONT_CAPTION = new Font(FONT_FAMILY, Font.BOLD, 11);
    public static final Font FONT_METRIC = new Font(FONT_FAMILY, Font.BOLD, 28);

    private BotanicalTheme() {
    }

    private static String resolveFontFamily() {
        try {
            String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            Set<String> availableSet = Set.copyOf(Arrays.asList(available));
            for (String candidate : new String[]{"Segoe UI", "Inter", "Plus Jakarta Sans", "Noto Sans", "SansSerif"}) {
                if (availableSet.contains(candidate)) {
                    return candidate;
                }
            }
        } catch (RuntimeException ignored) {
            // Fallback ke SansSerif standar bila environment font tidak tersedia
        }
        return Font.SANS_SERIF;
    }

    /**
     * Menerapkan default tampilan global UIManager agar komponen standar Swing
     * mengikuti tipografi dan palet Botanical Green.
     */
    public static void installDefaults() {
        UIManager.put("Panel.background", SOFT_IVORY);
        UIManager.put("OptionPane.background", WHITE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.font", FONT_BODY);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.background", WHITE);
        UIManager.put("PasswordField.font", FONT_BODY);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.background", WHITE);
        UIManager.put("TextArea.font", FONT_BODY);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", WHITE);
        UIManager.put("ComboBox.font", FONT_BODY);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.selectionBackground", MINT);
        UIManager.put("ComboBox.selectionForeground", TEXT_PRIMARY);
        UIManager.put("Table.font", FONT_BODY);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", WHITE);
        UIManager.put("Table.selectionBackground", PALE_MINT);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.font", FONT_SMALL_BOLD);
        UIManager.put("TableHeader.foreground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", PALE_MINT);
        UIManager.put("TabbedPane.font", FONT_BODY_BOLD);
        UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.selected", WHITE);
        UIManager.put("ToolTip.background", DEEP_FOREST);
        UIManager.put("ToolTip.foreground", WHITE);
        UIManager.put("ToolTip.font", FONT_SMALL);
        UIManager.put("ScrollPane.background", WHITE);
    }
}

