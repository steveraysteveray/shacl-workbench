package org.example.shaclworkbench;

import org.example.shaclworkbench.ui.WorkbenchFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(WorkbenchFrame::new);
    }
}
