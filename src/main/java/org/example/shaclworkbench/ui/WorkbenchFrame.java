package org.example.shaclworkbench.ui;

import org.example.shaclworkbench.engine.ShaclConfig;
import org.example.shaclworkbench.engine.ShaclResult;
import org.example.shaclworkbench.engine.ShaclRunner;
import org.example.shaclworkbench.session.SessionManager;
import org.example.shaclworkbench.session.SessionState;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class WorkbenchFrame extends JFrame {

    private final JTextField rootFolderField = new JTextField(40);
    private final JTextField dataFileField   = new JTextField(40);
    private final DropZone exclusionZone  = new DropZone("Workspace exclusions", false);
    private final DropZone inferenceZone  = new DropZone("Inference shapes (sh:TripleRule / sh:SPARQLRule)");
    private final DropZone validationZone = new DropZone("Validation shapes");
    private final JRadioButton inferAndValidate = new JRadioButton("Infer + Validate", true);
    private final JRadioButton validateOnly     = new JRadioButton("Validate only");
    private final JButton runButton = new JButton("Run");
    private final ReportPanel reportPanel       = new ReportPanel();
    private final InferredTriplesPanel inferredPanel = new InferredTriplesPanel();

    public WorkbenchFrame() {
        super("SHACL Workbench");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildCenterPanel(), buildBottomPanel());
        split.setResizeWeight(0.0);   // extra height on resize goes to the bottom (report) pane
        split.setBorder(null);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(split,           BorderLayout.CENTER);

        ButtonGroup group = new ButtonGroup();
        group.add(inferAndValidate);
        group.add(validateOnly);
        inferAndValidate.addActionListener(e -> inferenceZone.setEnabled(true));
        validateOnly.addActionListener(e -> inferenceZone.setEnabled(false));

        runButton.addActionListener(e -> runPipeline());

        restoreSession();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── panels ───────────────────────────────────────────────────────────────

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Input"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: root folder — accepts a dropped folder
        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Root folder:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rootFolderField.setEditable(false);
        rootFolderField.setToolTipText("Drop a folder here, or use Browse. All .ttl files are loaded recursively.");
        installFieldDrop(rootFolderField, true);
        panel.add(rootFolderField, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.gridx = 2; panel.add(browseButton("Browse…", true, rootFolderField), gbc);
        gbc.gridx = 3; panel.add(clearButton(rootFolderField), gbc);

        // Row 1: data file — accepts a dropped file
        gbc.gridx = 0; gbc.gridy = 1; panel.add(new JLabel("Data file:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        dataFileField.setEditable(false);
        dataFileField.setToolTipText("Drop a .ttl file here, or use Browse. Optional if a root folder is set.");
        installFieldDrop(dataFileField, false);
        panel.add(dataFileField, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.gridx = 2; panel.add(browseButton("Browse…", false, dataFileField), gbc);
        gbc.gridx = 3; panel.add(clearButton(dataFileField), gbc);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
        panel.add(exclusionZone);
        panel.add(inferenceZone);
        panel.add(validationZone);
        return panel;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout(4, 4));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(inferAndValidate);
        controls.add(validateOnly);
        controls.add(Box.createHorizontalStrut(16));
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD));
        controls.add(runButton);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Validation Report", reportPanel);
        tabs.addTab("Inferred Triples", inferredPanel);
        tabs.setPreferredSize(new Dimension(800, 260));

        panel.add(controls, BorderLayout.NORTH);
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    // ── drag-drop for root-folder / data-file fields ─────────────────────────

    /**
     * Installs a drop target on a read-only text field.
     * dirOnly=true: only the first dropped directory is accepted.
     * dirOnly=false: only the first dropped file is accepted.
     */
    private void installFieldDrop(JTextField field, boolean dirOnly) {
        new DropTarget(field, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent e) {
                if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                    e.acceptDrag(DnDConstants.ACTION_COPY);
                else
                    e.rejectDrag();
            }

            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) e.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) {
                        if (dirOnly && f.isDirectory()) {
                            field.setText(f.getAbsolutePath());
                            break;
                        } else if (!dirOnly && f.isFile()) {
                            field.setText(f.getAbsolutePath());
                            break;
                        }
                    }
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.dropComplete(false);
                }
            }
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private JButton browseButton(String label, boolean dirOnly, JTextField target) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(dirOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                target.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private JButton clearButton(JTextField target) {
        JButton btn = new JButton("Clear");
        btn.addActionListener(e -> target.setText(""));
        return btn;
    }

    // ── session ───────────────────────────────────────────────────────────────

    private void restoreSession() {
        SessionManager.load().ifPresent(s -> {
            rootFolderField.setText(s.rootFolder());
            dataFileField.setText(s.dataFile());
            s.exclusions().stream().map(Path::of).forEach(exclusionZone::addPathDirect);
            s.inferenceShapes().stream().map(Path::of).forEach(inferenceZone::addPathDirect);
            s.validationShapes().stream().map(Path::of).forEach(validationZone::addPathDirect);
            if (s.inferAndValidate()) {
                inferAndValidate.setSelected(true);
                inferenceZone.setEnabled(true);
            } else {
                validateOnly.setSelected(true);
                inferenceZone.setEnabled(false);
            }
        });
    }

    private void saveSession() {
        SessionManager.save(new SessionState(
                rootFolderField.getText().trim(),
                exclusionZone.getPaths().stream().map(Path::toString).toList(),
                dataFileField.getText().trim(),
                inferenceZone.getPaths().stream().map(Path::toString).toList(),
                validationZone.getPaths().stream().map(Path::toString).toList(),
                inferAndValidate.isSelected()
        ));
    }

    // ── pipeline ─────────────────────────────────────────────────────────────

    private void runPipeline() {
        String rootText     = rootFolderField.getText().trim();
        String dataFileText = dataFileField.getText().trim();
        if (rootText.isEmpty() && dataFileText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please specify a root folder, a data file, or both.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<Path> validationPaths = validationZone.getPaths();
        if (validationPaths.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please add at least one validation shapes file.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Path rootDir  = rootText.isEmpty()     ? null : Path.of(rootText);
        Path dataFile = dataFileText.isEmpty() ? null : Path.of(dataFileText);
        List<Path> excludedPaths = exclusionZone.getPaths();
        boolean doInfer = inferAndValidate.isSelected();
        List<Path> inferPaths = doInfer ? inferenceZone.getPaths() : List.of();

        ShaclConfig config = new ShaclConfig(
                rootDir, excludedPaths, dataFile, inferPaths, validationPaths, doInfer);

        runButton.setEnabled(false);
        reportPanel.clear();
        inferredPanel.clear();

        SwingWorker<ShaclResult, Void> worker = new SwingWorker<>() {
            @Override
            protected ShaclResult doInBackground() throws Exception {
                return new ShaclRunner().run(config);
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                try {
                    ShaclResult result = get();
                    reportPanel.showResult(result.report(), result.reportTurtle(),
                            result.inferredTripleCount(), result.prefixMap());
                    inferredPanel.showInferred(result.inferredTripleCount(), result.inferredTurtle());
                    saveSession();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(WorkbenchFrame.this,
                            "Error: " + cause.getMessage(), "Pipeline failed", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
