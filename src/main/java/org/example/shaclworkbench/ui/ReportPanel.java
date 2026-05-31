package org.example.shaclworkbench.ui;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReportPanel extends JPanel {

    private static final BufferedImage WATERMARK;
    static {
        BufferedImage img = null;
        try (var in = ReportPanel.class.getResourceAsStream("/watermark.png")) {
            if (in != null) img = ImageIO.read(in);
        } catch (Exception ignored) {}
        WATERMARK = img;
    }

    private final JLabel statusLabel   = new JLabel("No results yet.");
    private final JLabel inferredLabel = new JLabel();
    private final JCheckBox showFullUri     = new JCheckBox("Full URI");
    private final JCheckBox filterViolation = new JCheckBox("Violation", true);
    private final JCheckBox filterWarning   = new JCheckBox("Warning",   true);
    private final JCheckBox filterInfo      = new JCheckBox("Info",      true);

    private final ReportTableModel tableModel = new ReportTableModel();
    private final JTable table = new JTable(tableModel) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getRowCount() == 0 && WATERMARK != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int sz = (int)(Math.min(getWidth(), getHeight()) * 0.9);
                if (sz > 0) {
                    int x = (getWidth()  - sz) / 2;
                    int y = (getHeight() - sz) / 2;
                    g2.drawImage(WATERMARK, x, y, sz, sz, null);
                }
                g2.dispose();
            }
        }
    };
    private final TableRowSorter<ReportTableModel> sorter = new TableRowSorter<>(tableModel);

    private String currentReportTurtle = "";
    private boolean lastConforms = true;
    private int totalResultCount = 0;

    public ReportPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Validation report"));

        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(70);
        table.getColumnModel().getColumn(4).setPreferredWidth(260);
        table.setDefaultRenderer(Object.class, new TooltipRenderer());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(800, 200));

        showFullUri.addActionListener(e -> tableModel.setFullUri(showFullUri.isSelected()));

        ActionListener filterListener = e -> {
            applyFilter();
            packColumns();
            updateStatusLabel();
        };
        filterViolation.addActionListener(filterListener);
        filterWarning.addActionListener(filterListener);
        filterInfo.addActionListener(filterListener);

        JPanel topBar = new JPanel(new BorderLayout());

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        statusRow.add(statusLabel);
        statusRow.add(inferredLabel);
        statusRow.add(showFullUri);
        statusRow.add(new JSeparator(SwingConstants.VERTICAL));
        statusRow.add(new JLabel("Show:"));
        statusRow.add(filterViolation);
        statusRow.add(filterWarning);
        statusRow.add(filterInfo);
        topBar.add(statusRow, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        JButton copyTurtle = new JButton("Copy Turtle");
        JButton saveAs     = new JButton("Save report…");
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
        lastConforms = report.conforms();
        totalResultCount = report.getEntries().size();
        tableModel.setPrefixMap(prefixMap);
        tableModel.setEntries(new ArrayList<>(report.getEntries()));
        applyFilter();
        packColumns();
        updateStatusLabel();
        inferredLabel.setText(inferredCount > 0 ? "[" + inferredCount + " triple(s) inferred]" : "");
    }

    public void clear() {
        tableModel.setEntries(List.of());
        lastConforms = true;
        totalResultCount = 0;
        statusLabel.setText("No results yet.");
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));
        inferredLabel.setText("");
        currentReportTurtle = "";
    }

    // ── private ──────────────────────────────────────────────────────────────

    /**
     * Rebuilds the row filter from the three severity checkboxes.
     * RowFilter.regexFilter returns RowFilter&lt;Object,Object&gt; which satisfies
     * setRowFilter's bound of RowFilter&lt;? super M, ? super I&gt;, neatly avoiding
     * the generic-erasure clash that arises when overriding include() directly.
     */
    private void applyFilter() {
        List<String> enabled = new ArrayList<>(3);
        if (filterViolation.isSelected()) enabled.add("Violation");
        if (filterWarning.isSelected())   enabled.add("Warning");
        if (filterInfo.isSelected())      enabled.add("Info");

        if (enabled.size() == 3) {
            sorter.setRowFilter(null);                          // all visible — no filter needed
        } else if (enabled.isEmpty()) {
            sorter.setRowFilter(RowFilter.regexFilter("(?!x)x", 3)); // matches nothing
        } else {
            sorter.setRowFilter(RowFilter.regexFilter(
                    "^(" + String.join("|", enabled) + ")$", 3));
        }
    }

    public void packColumns() {
        FontMetrics fm = table.getFontMetrics(table.getFont());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int col = 0; col < table.getColumnCount(); col++) {
            TableColumn tc = table.getColumnModel().getColumn(col);
            // Header: extra padding for the sort indicator
            int width = fm.stringWidth(table.getColumnName(col)) + 24;
            // Visible data rows
            for (int row = 0; row < table.getRowCount(); row++) {
                Object val = table.getValueAt(row, col);
                if (val != null) width = Math.max(width, fm.stringWidth(val.toString()) + 16);
            }
            tc.setPreferredWidth(width);
        }
        table.doLayout();
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
    }

    private void updateStatusLabel() {
        if (lastConforms) {
            statusLabel.setText("✓  Conforms");
            statusLabel.setForeground(new Color(0, 128, 0));
        } else {
            int visible = table.getRowCount();   // post-filter count
            String count = (visible == totalResultCount)
                    ? totalResultCount + " result(s)"
                    : visible + " of " + totalResultCount + " result(s)";
            statusLabel.setText("✗  Does not conform  (" + count + ")");
            statusLabel.setForeground(Color.RED);
        }
    }

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

    private static class TooltipRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean selected, boolean focused, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, col);
            setToolTipText(value != null ? value.toString() : null);
            if (!selected) {
                Color alt = UIManager.getColor("Table.alternateRowColor");
                setBackground(row % 2 != 0 && alt != null ? alt : table.getBackground());
            }
            return this;
        }
    }
}
