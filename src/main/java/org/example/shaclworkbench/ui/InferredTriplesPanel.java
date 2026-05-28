package org.example.shaclworkbench.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class InferredTriplesPanel extends JPanel {

    private final JTextArea textArea = new JTextArea();
    private final JLabel countLabel = new JLabel("No inference run yet.");

    public InferredTriplesPanel() {
        super(new BorderLayout(4, 4));

        textArea.setEditable(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(countLabel, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton copy = new JButton("Copy Turtle");
        JButton save = new JButton("Save inferred…");
        copy.addActionListener(e -> copyText());
        save.addActionListener(e -> saveText());
        buttonRow.add(copy);
        buttonRow.add(save);
        topBar.add(buttonRow, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
    }

    public void showInferred(int count, String turtle) {
        if (count == 0) {
            countLabel.setText("No triples inferred.");
            textArea.setText("");
        } else {
            countLabel.setText(count + " triple(s) inferred:");
            textArea.setText(turtle);
            textArea.setCaretPosition(0);
        }
    }

    public void clear() {
        countLabel.setText("No inference run yet.");
        textArea.setText("");
    }

    private void copyText() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(textArea.getText()), null);
    }

    private void saveText() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("inferred.ttl"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                fw.write(textArea.getText());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
