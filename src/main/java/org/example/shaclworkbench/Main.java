package org.example.shaclworkbench;

import org.example.shaclworkbench.ui.WorkbenchFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        // Must be set before any AWT/Swing class is loaded
        System.setProperty("apple.awt.application.name", "SHACL Workbench");
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(WorkbenchFrame::new);
    }
}
