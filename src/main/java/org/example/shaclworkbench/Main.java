package org.example.shaclworkbench;

import org.example.shaclworkbench.ui.WorkbenchFrame;
import org.example.shaclworkbench.ui.theme.CopperSteamTheme;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Must be set before any AWT/Swing class is loaded
        System.setProperty("apple.awt.application.name", "SHACL Workbench");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // "Copper & Steam" steampunk theme (Metal LAF base)
        CopperSteamTheme.install();

        SwingUtilities.invokeLater(WorkbenchFrame::new);
    }
}
