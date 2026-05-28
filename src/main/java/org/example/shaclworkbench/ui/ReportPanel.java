package org.example.shaclworkbench.ui;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
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
    private final JCheckBox showFullUri = new JCheckBox("Full URI");
    private final ReportTableModel tableModel = new ReportTableModel();
    private String currentReportTurtle = "";

    public ReportPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Validation report"));

        JTable table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);

        // Show full cell content in tooltip so long URIs are always readable
        table.setDefaultRenderer(Object.class, new TooltipRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, 200));

        // Toggle: refresh table when user switches between local name and full URI
        showFullUri.addActionListener(e -> tableModel.setFullUri(showFullUri.isSelected()));

        JPanel topBar = new JPanel(new BorderLayout());

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.add(statusLabel);
        statusRow.add(inferredLabel);
        statusRow.add(showFullUri);
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

    public void showResult(ValidationReport report, String turtle, int inferredCount,
                           PrefixMapping prefixMap) {
        currentReportTurtle = turtle;
        tableModel.setPrefixMap(prefixMap);
        tableModel.setEntries(new ArrayList<>(report.getEntries()));

        boolean conforms = report.conforms();
        statusLabel.setText(conforms
                ? "✓  Conforms"
                : "✗  Does not conform  (" + tableModel.getRowCount() + " result(s))");
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
        private boolean fullUri = false;
        private PrefixMapping prefixMap = null;

        void setEntries(List<ReportEntry> entries) {
            this.entries = entries;
            fireTableDataChanged();
        }

        void setFullUri(boolean full) {
            this.fullUri = full;
            fireTableDataChanged();
        }

        void setPrefixMap(PrefixMapping pm) {
            this.prefixMap = pm;
            fireTableDataChanged();
        }

        @Override public int getRowCount()    { return entries.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            ReportEntry e = entries.get(row);
            return switch (col) {
                case 0 -> format(e.focusNode() != null ? e.focusNode().toString() : "");
                case 1 -> e.resultPath() != null ? e.resultPath().toString() : "";
                case 2 -> format(e.sourceConstraintComponent() != null
                        ? e.sourceConstraintComponent().toString() : "");
                case 3 -> e.severity() != null ? e.severity().level().getLocalName() : "";
                case 4 -> e.message() != null ? e.message() : "";
                default -> "";
            };
        }

        /**
         * Strips angle brackets from Jena's Node.toString() output, then:
         * - fullUri mode: returns the bare URI
         * - default: tries to shorten using the loaded prefix map; falls back to full URI
         *   if no matching prefix is declared (avoids silent loss of namespace info).
         */
        private String format(String raw) {
            String uri = raw.startsWith("<") && raw.endsWith(">")
                    ? raw.substring(1, raw.length() - 1) : raw;
            if (fullUri || uri.isEmpty()) return uri;
            if (prefixMap != null) {
                String shortened = prefixMap.shortForm(uri);
                if (!shortened.equals(uri)) return shortened;
            }
            return uri;
        }
    }

    // ── tooltip renderer ─────────────────────────────────────────────────────

    /** Shows the full cell value as a tooltip so truncated URIs are always readable. */
    private static class TooltipRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            setToolTipText(value != null ? value.toString() : null);
            return this;
        }
    }
}
