package org.example.shaclworkbench;

import org.example.shaclworkbench.ui.WorkbenchFrame;
import org.example.shaclworkbench.ui.theme.CopperSteamTheme;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import java.awt.Image;
import java.awt.Taskbar;

public class Main {
    public static void main(String[] args) {
        // Must be set before any AWT/Swing class is loaded
        System.setProperty("apple.awt.application.name", "SHACL Workbench");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // Set Dock / taskbar icon for bare java -jar runs
        setTaskbarIcon();

        // "Copper & Steam" steampunk theme (Metal LAF base)
        CopperSteamTheme.install();

        SwingUtilities.invokeLater(WorkbenchFrame::new);
    }

    private static void setTaskbarIcon() {
        try {
            var url = Main.class.getResource("/icon.png");
            if (url == null) return;
            Image img = ImageIO.read(url);
            Taskbar.getTaskbar().setIconImage(img);
        } catch (Exception ignored) {}
    }
}
