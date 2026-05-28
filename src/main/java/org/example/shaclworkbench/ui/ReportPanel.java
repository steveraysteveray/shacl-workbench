package org.example.shaclworkbench.ui;

import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportPanel extends JPanel {

    private final JLabel statusLabel = new JLabel("No results yet.");
    private final JLabel inferredLabel = new JLabel();
    private final ReportTableModel tableModel = new ReportTableModel();
    private String currentReportTurtle = "";

    public ReportPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Validation report"));

        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, 200));

        JPanel topBar = new JPanel(new BorderLayout());
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.add(statusLabel);
        statusRow.add(inferredLabel);
        topBar.add(statusRow, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton copyTurtle = new JButton("Copy Turtle");
        JButton saveAs = new JButton("Save report…");
        copyTurtle.addActionListener(e -> copyTurtle());
        saveAs.addActionListener(e -> saveReport());
        buttonRow.add(copyTurtle);
        buttonRow.add(saveAs);
        topBar.add(buttonRow, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void showResult(ValidationReport report, String turtle, int inferredCount) {
        currentReportTurtle = turtle;
        tableModel.setEntries(new ArrayList<>(report.getEntries()));

        boolean conforms = report.conforms();
        statusLabel.setText(conforms ? "✓  Conforms" : "✗  Does not conform  (" + tableModel.getRowCount() + " result(s))");
        statusLabel.setForeground(conforms ? new Color(0, 128, 0) : Color.RED);

        inferredLabel.setText(inferredCount > 0 ? "[" + inferredCount + " triple(s) inferred]" : "");
    }

    public void clear() {
        tableModel.setEntries(List.of());
        statusLabel.setText("No results yet.");
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));
        inferredLabel.setText("");
        currentReportTurtle = "";
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void copyTurtle() {
        Toolkit.getDefaultToolkit().getSystemClipboard()
               .setContents(new StringSelection(currentReportTurtle), null);
    }

    private void saveReport() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("validationReport.ttl"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                fw.write(currentReportTurtle);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── table model ──────────────────────────────────────────────────────────

    private static class ReportTableModel extends AbstractTableModel {

        private static final String[] COLUMNS = {"Focus node", "Path", "Constraint", "Severity", "Message"};
        private List<ReportEntry> entries = List.of();

        void setEntries(List<ReportEntry> entries) {
            this.entries = entries;
            fireTableDataChanged();
        }

        @Override public int getRowCount() { return entries.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            ReportEntry e = entries.get(row);
            return switch (col) {
                case 0 -> e.focusNode() != null ? localName(e.focusNode().toString()) : "";
                case 1 -> e.resultPath() != null ? e.resultPath().toString() : "";
                case 2 -> e.sourceConstraintComponent() != null
                        ? localName(e.sourceConstraintComponent().toString()) : "";
                case 3 -> e.severity() != null ? e.severity().level().getLocalName() : "";
                case 4 -> e.message() != null ? e.message() : "";
                default -> "";
            };
        }

        private static String localName(String uri) {
            int h = uri.lastIndexOf('#');
            int s = uri.lastIndexOf('/');
            int cut = Math.max(h, s);
            return cut >= 0 && cut < uri.length() - 1 ? uri.substring(cut + 1) : uri;
        }
    }
}
