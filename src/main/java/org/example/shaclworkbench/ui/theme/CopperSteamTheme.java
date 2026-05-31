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
 * Palette: warm copper primary tones over a cool industrial-grey background
 * with deep-steel shadows — the aesthetic of a Victorian engine room.
 * Contrasts with the Parchment & Brass variant: cooler, heavier, more mechanical.
 *
 * To activate: call {@link #install()} before any Swing components are created.
 */
public final class CopperSteamTheme extends DefaultMetalTheme {

    // ── palette ───────────────────────────────────────────────────────────────

    // Primary: weathered copper / verdigris edge
    private static final ColorUIResource PRIMARY_1 = new ColorUIResource(0x6B, 0x38, 0x18); // dark copper-rust (focus rings, pressed)
    private static final ColorUIResource PRIMARY_2 = new ColorUIResource(0xA0, 0x60, 0x28); // copper (scroll bars, button outlines)
    private static final ColorUIResource PRIMARY_3 = new ColorUIResource(0xC8, 0x86, 0x50); // light copper (selection backgrounds)

    // Secondary: cool steel grey
    private static final ColorUIResource SECONDARY_1 = new ColorUIResource(0x28, 0x26, 0x24); // near-black steel (deep shadows)
    private static final ColorUIResource SECONDARY_2 = new ColorUIResource(0x58, 0x54, 0x50); // medium steel (borders, separators)
    private static final ColorUIResource SECONDARY_3 = new ColorUIResource(0xED, 0xE9, 0xE4); // warm white (panel backgrounds)

    // Alternating table row — a faint cool-grey tint over the warm white base
    private static final Color ALT_ROW = new Color(0xDE, 0xD8, 0xD2);

    // ── DefaultMetalTheme overrides ───────────────────────────────────────────

    @Override public String getName() { return "Copper & Steam"; }

    @Override protected ColorUIResource getPrimary1()   { return PRIMARY_1; }
    @Override protected ColorUIResource getPrimary2()   { return PRIMARY_2; }
    @Override protected ColorUIResource getPrimary3()   { return PRIMARY_3; }
    @Override protected ColorUIResource getSecondary1() { return SECONDARY_1; }
    @Override protected ColorUIResource getSecondary2() { return SECONDARY_2; }
    @Override protected ColorUIResource getSecondary3() { return SECONDARY_3; }

    // All controls use SansSerif — the clean, technical style of engineering blueprints.
    // (No Serif override; DefaultMetalTheme SansSerif is appropriate for this aesthetic.)

    // Slightly heavier control font to reinforce the industrial character
    @Override
    public FontUIResource getControlTextFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.PLAIN, 13);
    }

    @Override
    public FontUIResource getWindowTitleFont() {
        return new FontUIResource(Font.SANS_SERIF, Font.BOLD, 13);
    }

    // ── installer ─────────────────────────────────────────────────────────────

    /**
     * Install the theme: set Metal LAF + apply extra UIManager overrides.
     * Must be called on the EDT before any Swing components are created.
     */
    public static void install() {
        MetalLookAndFeel.setCurrentTheme(new CopperSteamTheme());
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            // fall through; theme won't apply but app still starts
        }

        // Zebra striping (picked up by TooltipRenderer in both table panels)
        UIManager.put("Table.alternateRowColor", ALT_ROW);

        // Titled borders: recessed etched look with copper highlight / steel shadow
        UIManager.put("TitledBorder.border",
                BorderFactory.createEtchedBorder(EtchedBorder.LOWERED,
                        PRIMARY_3.brighter(), SECONDARY_1));
        UIManager.put("TitledBorder.titleColor", PRIMARY_1);

        // Tooltip colours — faint copper tint background, near-black text
        UIManager.put("ToolTip.background", new Color(0xF0, 0xE8, 0xE0));
        UIManager.put("ToolTip.foreground", new Color(0x18, 0x14, 0x10));
        UIManager.put("ToolTip.border",
                BorderFactory.createLineBorder(new Color(0x80, 0x50, 0x28), 1));
    }
}
