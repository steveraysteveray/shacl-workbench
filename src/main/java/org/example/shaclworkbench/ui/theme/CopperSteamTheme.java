package org.example.shaclworkbench.ui.theme;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.*;

/**
 * "Copper & Steam" steampunk theme.
 *
 * To activate initially: call {@link #install()} before any Swing components are created.
 * To resize fonts at runtime: call {@link #setBaseFontSize(int)} then {@link #reinstall(JFrame)}.
 */
public final class CopperSteamTheme extends DefaultMetalTheme {

    // ── palette ───────────────────────────────────────────────────────────────

    private static final ColorUIResource PRIMARY_1   = new ColorUIResource(0x6B, 0x38, 0x18);
    private static final ColorUIResource PRIMARY_2   = new ColorUIResource(0xA0, 0x60, 0x28);
    private static final ColorUIResource PRIMARY_3   = new ColorUIResource(0xC8, 0x86, 0x50);
    private static final ColorUIResource SECONDARY_1 = new ColorUIResource(0x28, 0x26, 0x24);
    private static final ColorUIResource SECONDARY_2 = new ColorUIResource(0x58, 0x54, 0x50);
    private static final ColorUIResource SECONDARY_3 = new ColorUIResource(0xED, 0xE9, 0xE4);
    private static final Color           ALT_ROW     = new Color(0xDE, 0xD8, 0xD2);

    // ── font size (mutable for runtime scaling) ───────────────────────────────

    public static final int DEFAULT_FONT_SIZE = 13;
    private static int baseFontSize = DEFAULT_FONT_SIZE;

    public static int  getBaseFontSize()       { return baseFontSize; }
    public static void setBaseFontSize(int sz) { baseFontSize = Math.max(10, Math.min(20, sz)); }

    // ── DefaultMetalTheme overrides ───────────────────────────────────────────

    @Override public String getName() { return "Copper & Steam"; }

    @Override protected ColorUIResource getPrimary1()   { return PRIMARY_1; }
    @Override protected ColorUIResource getPrimary2()   { return PRIMARY_2; }
    @Override protected ColorUIResource getPrimary3()   { return PRIMARY_3; }
    @Override protected ColorUIResource getSecondary1() { return SECONDARY_1; }
    @Override protected ColorUIResource getSecondary2() { return SECONDARY_2; }
    @Override protected ColorUIResource getSecondary3() { return SECONDARY_3; }

    @Override
    public FontUIResource getControlTextFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.PLAIN, baseFontSize);
    }

    @Override
    public FontUIResource getSystemTextFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.PLAIN, baseFontSize);
    }

    @Override
    public FontUIResource getUserTextFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.PLAIN, baseFontSize);
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.BOLD, baseFontSize);
    }

    @Override
    public FontUIResource getMenuTextFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.PLAIN, baseFontSize);
    }

    // ── installer ─────────────────────────────────────────────────────────────

    /** Initial install — call before any Swing components are created. */
    public static void install() {
        MetalLookAndFeel.setCurrentTheme(new CopperSteamTheme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) {}
        applyExtras();
    }

    /**
     * Re-apply after a font-size change.
     * Rebuilds the theme, re-applies extra UIManager keys, then walks the full
     * component tree of {@code frame} to propagate all changes.
     */
    public static void reinstall(JFrame frame) {
        install();
        SwingUtilities.updateComponentTreeUI(frame);
    }

    private static void applyExtras() {
        UIManager.put("Table.alternateRowColor", ALT_ROW);
        UIManager.put("TitledBorder.border",
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED,
                        PRIMARY_3.brighter(), SECONDARY_1));
        UIManager.put("TitledBorder.titleColor", PRIMARY_1);
        UIManager.put("ToolTip.background", new Color(0xF0, 0xE8, 0xE0));
        UIManager.put("ToolTip.foreground", new Color(0x18, 0x14, 0x10));
        UIManager.put("ToolTip.border",
                BorderFactory.createLineBorder(new Color(0x80, 0x50, 0x28), 1));
    }
}
