package org.example.shaclworkbench.ui;

import org.example.shaclworkbench.engine.ShaclConfig;
import org.example.shaclworkbench.engine.ShaclResult;
import org.example.shaclworkbench.engine.ShaclRunner;
import org.example.shaclworkbench.session.SessionManager;
import org.example.shaclworkbench.session.SessionState;
import org.example.shaclworkbench.ui.theme.CopperSteamTheme;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
    private final JButton runButton = new JButton("⚙  Run");
    private final JLabel sessionLabel = new JLabel("");

    private final JProgressBar progressBar   = new JProgressBar();
    private final JLabel       progressLabel = new JLabel();

    private final ReportPanel          reportPanel   = new ReportPanel();
    private final InferredTriplesPanel inferredPanel = new InferredTriplesPanel();

    public WorkbenchFrame() {
        super("SHACL Workbench");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                saveSession();
                dispose();
                System.exit(0);
            }
        });
        setLayout(new BorderLayout(8, 8));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildCenterPanel(), buildBottomPanel());
        split.setResizeWeight(0.0);
        split.setBorder(null);

        add(buildTopPanel(), BorderLayout.NORTH);
        add(split,           BorderLayout.CENTER);

        ButtonGroup group = new ButtonGroup();
        group.add(inferAndValidate);
        group.add(validateOnly);
        inferAndValidate.addActionListener(e -> inferenceZone.setEnabled(true));
        validateOnly.addActionListener(e -> inferenceZone.setEnabled(false));

        runButton.addActionListener(e -> runPipeline());

        installFontSizeBindings();
        restoreSession();

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ── font scaling ──────────────────────────────────────────────────────────

    private void installFontSizeBindings() {
        int cmd = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        InputMap  im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // CMD-=  and  CMD-Shift-= (i.e. CMD-+)  →  larger
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, cmd),                              "font+");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, cmd | KeyEvent.SHIFT_DOWN_MASK),   "font+");
        // CMD--  →  smaller
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,  cmd),                              "font-");
        // CMD-0  →  reset
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_0,      cmd),                              "font0");

        am.put("font+", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { adjustFontSize(+1); }
        });
        am.put("font-", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { adjustFontSize(-1); }
        });
        am.put("font0", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                CopperSteamTheme.setBaseFontSize(CopperSteamTheme.DEFAULT_FONT_SIZE);
                CopperSteamTheme.reinstall(WorkbenchFrame.this);
                reportPanel.packColumns();
                inferredPanel.packColumns();
                saveSession();
            }
        });
    }

    private void adjustFontSize(int delta) {
        CopperSteamTheme.setBaseFontSize(CopperSteamTheme.getBaseFontSize() + delta);
        CopperSteamTheme.reinstall(this);
        reportPanel.packColumns();
        inferredPanel.packColumns();
        saveSession();
    }

    // ── panels ────────────────────────────────────────────────────────────────

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Input"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 4, 2, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Root folder:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        rootFolderField.setEditable(false);
        rootFolderField.setToolTipText("Drop a folder here, or use Browse. All .ttl files are loaded recursively.");
        installFieldDrop(rootFolderField, true);
        panel.add(rootFolderField, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        gbc.gridx = 2; panel.add(browseButton("Browse…", true,  rootFolderField), gbc);
        gbc.gridx = 3; panel.add(clearButton(rootFolderField), gbc);

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

        progressBar.setIndeterminate(false);
        progressBar.setPreferredSize(new Dimension(110, runButton.getPreferredSize().height - 4));
        progressBar.setVisible(false);
        controls.add(progressBar);

        progressLabel.setFont(progressLabel.getFont().deriveFont(Font.ITALIC, 11f));
        progressLabel.setForeground(Color.GRAY);
        controls.add(progressLabel);

        controls.add(Box.createHorizontalStrut(8));
        JButton saveConfig = new JButton("Save config…");
        JButton loadConfig = new JButton("Load config…");
        saveConfig.addActionListener(e -> saveNamedConfig());
        loadConfig.addActionListener(e -> loadNamedConfig());
        controls.add(saveConfig);
        controls.add(loadConfig);
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(Font.ITALIC));
        sessionLabel.setForeground(Color.GRAY);
        controls.add(sessionLabel);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Validation Report", reportPanel);
        tabs.addTab("Inferred Triples",  inferredPanel);
        tabs.setPreferredSize(new Dimension(800, 260));

        panel.add(controls, BorderLayout.NORTH);
        panel.add(tabs,     BorderLayout.CENTER);
        return panel;
    }

    // ── drag-drop for text fields ─────────────────────────────────────────────

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
                        if (dirOnly && f.isDirectory())  { field.setText(f.getAbsolutePath()); break; }
                        if (!dirOnly && f.isFile())       { field.setText(f.getAbsolutePath()); break; }
                    }
                    e.dropComplete(true);
                } catch (Exception ex) { e.dropComplete(false); }
            }
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private JButton browseButton(String label, boolean dirOnly, JTextField target) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(dirOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                target.setText(fc.getSelectedFile().getAbsolutePath());
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
        var loaded = SessionManager.load();
        loaded.ifPresent(s -> {
            applySession(s);
            CopperSteamTheme.setBaseFontSize(s.fontSize());
            CopperSteamTheme.reinstall(this);
        });
        sessionLabel.setText(loaded.isPresent() ? "Session restored" : "No saved session");
    }

    private void saveSession() {
        SessionManager.save(currentSessionState());
        sessionLabel.setText("Session saved");
    }

    private SessionState currentSessionState() {
        return new SessionState(
                rootFolderField.getText().trim(),
                exclusionZone.getPaths().stream().map(Path::toString).toList(),
                dataFileField.getText().trim(),
                inferenceZone.getPaths().stream().map(Path::toString).toList(),
                validationZone.getPaths().stream().map(Path::toString).toList(),
                inferAndValidate.isSelected(),
                CopperSteamTheme.getBaseFontSize()
        );
    }

    private void applySession(SessionState s) {
        rootFolderField.setText(s.rootFolder());
        dataFileField.setText(s.dataFile());
        exclusionZone.clear(); inferenceZone.clear(); validationZone.clear();
        s.exclusions().stream().map(Path::of).forEach(exclusionZone::addPathDirect);
        s.inferenceShapes().stream().map(Path::of).forEach(inferenceZone::addPathDirect);
        s.validationShapes().stream().map(Path::of).forEach(validationZone::addPathDirect);
        if (s.inferAndValidate()) { inferAndValidate.setSelected(true); inferenceZone.setEnabled(true); }
        else                      { validateOnly.setSelected(true);     inferenceZone.setEnabled(false); }
    }

    // ── named configurations ──────────────────────────────────────────────────

    private void saveNamedConfig() {
        String name = JOptionPane.showInputDialog(this,
                "Configuration name:", "Save configuration", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        name = name.trim();
        SessionManager.saveNamed(name, currentSessionState());
        sessionLabel.setText("Config '" + name + "' saved");
    }

    private void loadNamedConfig() {
        List<String> configs = SessionManager.listConfigs();
        if (configs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No saved configurations found.",
                    "Load configuration", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String[] options = configs.toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(this,
                "Select a configuration:", "Load configuration",
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (selected == null) return;
        SessionManager.loadNamed(selected).ifPresent(s -> {
            applySession(s);
            sessionLabel.setText("Config '" + selected + "' loaded");
        });
    }

    // ── pipeline ──────────────────────────────────────────────────────────────

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
        boolean doInfer = inferAndValidate.isSelected();
        ShaclConfig config = new ShaclConfig(
                rootDir, exclusionZone.getPaths(), dataFile,
                doInfer ? inferenceZone.getPaths() : List.of(),
                validationPaths, doInfer);

        runButton.setEnabled(false);
        reportPanel.clear();
        inferredPanel.clear();
        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        progressLabel.setText("Starting…");

        SwingWorker<ShaclResult, String> worker = new SwingWorker<>() {
            @Override protected ShaclResult doInBackground() throws Exception {
                return new ShaclRunner().run(config, msg -> publish(msg));
            }
            @Override protected void process(List<String> chunks) {
                progressLabel.setText(chunks.get(chunks.size() - 1));
            }
            @Override protected void done() {
                runButton.setEnabled(true);
                progressBar.setVisible(false);
                progressBar.setIndeterminate(false);
                progressLabel.setText("");
                try {
                    ShaclResult result = get();
                    reportPanel.showResult(result.report(), result.reportTurtle(),
                            result.inferredTriples().size(), result.prefixMap());
                    inferredPanel.showInferred(result.inferredTriples(),
                            result.inferredTurtle(), result.prefixMap());
                    saveSession();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    JOptionPane.showMessageDialog(WorkbenchFrame.this,
                            "Error: " + cause.getMessage(), "Pipeline failed",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
