package org.example.shaclworkbench.ui;

import org.apache.jena.graph.Node;
import org.apache.jena.shared.PrefixMapping;
import org.example.shaclworkbench.engine.InferredTriple;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class InferredTriplesPanel extends JPanel {

    private final JLabel countLabel = new JLabel("No inference run yet.");
    private final InferredTableModel tableModel = new InferredTableModel();
    private final JTable table = new JTable(tableModel);
    private final TableRowSorter<InferredTableModel> sorter = new TableRowSorter<>(tableModel);
    private String currentTurtle = "";

    public InferredTriplesPanel() {
        super(new BorderLayout(4, 4));

        table.setRowSorter(sorter);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new TooltipRenderer());

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(countLabel, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton copy = new JButton("Copy Turtle");
        JButton save = new JButton("Save inferred…");
        copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(new StringSelection(currentTurtle), null));
        save.addActionListener(e -> saveText());
        buttonRow.add(copy);
        buttonRow.add(save);
        topBar.add(buttonRow, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void showInferred(List<InferredTriple> triples, String turtle, PrefixMapping prefixMap) {
        currentTurtle = turtle;
        tableModel.setPrefixMap(prefixMap);
        tableModel.setTriples(triples);
        packColumns();
        if (triples.isEmpty()) {
            countLabel.setText("No triples inferred.");
        } else {
            countLabel.setText(triples.size() + " triple(s) inferred:");
        }
    }

    public void clear() {
        tableModel.setTriples(List.of());
        countLabel.setText("No inference run yet.");
        currentTurtle = "";
    }

    // ── column packing ────────────────────────────────────────────────────────

    private void packColumns() {
        FontMetrics fm = table.getFontMetrics(table.getFont());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn tc = table.getColumnModel().getColumn(col);
            int width = fm.stringWidth(table.getColumnName(col)) + 24;
            for (int row = 0; row < table.getRowCount(); row++) {
                Object val = table.getValueAt(row, col);
                if (val != null) width = Math.max(width, fm.stringWidth(val.toString()) + 16);
            }
            tc.setPreferredWidth(width);
        }
        table.doLayout();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    // ── save ──────────────────────────────────────────────────────────────────

    private void saveText() {
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new File("inferred.ttl"));
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (FileWriter fw = new FileWriter(fc.getSelectedFile())) {
                fw.write(currentTurtle);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── table model ───────────────────────────────────────────────────────────

    private static class InferredTableModel extends AbstractTableModel {

        private static final String[] COLUMNS =
                {"Subject", "Predicate", "Object", "Shape", "Rule"};

        private List<InferredTriple> triples = List.of();
        private PrefixMapping prefixMap = null;

        void setTriples(List<InferredTriple> triples) {
            this.triples = triples;
            fireTableDataChanged();
        }

        void setPrefixMap(PrefixMapping pm) {
            this.prefixMap = pm;
        }

        @Override public int getRowCount()    { return triples.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            InferredTriple it = triples.get(row);
            return switch (col) {
                case 0 -> formatNode(it.triple().getSubject());
                case 1 -> formatNode(it.triple().getPredicate());
                case 2 -> formatNode(it.triple().getObject());
                case 3 -> localName(it.shapeNode());
                case 4 -> it.ruleType();
                default -> "";
            };
        }

        private String formatNode(Node n) {
            if (n == null) return "";
            if (n.isLiteral()) return n.toString();
            if (n.isBlank())   return "_:" + n.getBlankNodeLabel();
            String uri = n.getURI();
            if (prefixMap != null) {
                String shortened = prefixMap.shortForm(uri);
                if (!shortened.equals(uri)) return shortened;
            }
            return uri;
        }

        private static String localName(Node n) {
            if (n == null)    return "";
            if (n.isBlank())  return "_:" + n.getBlankNodeLabel();
            try {
                String uri = n.getURI();
                int i = Math.max(uri.lastIndexOf('#'), uri.lastIndexOf('/'));
                return i >= 0 ? uri.substring(i + 1) : uri;
            } catch (Exception e) {
                return n.toString();
            }
        }
    }

    // ── tooltip renderer ──────────────────────────────────────────────────────

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
