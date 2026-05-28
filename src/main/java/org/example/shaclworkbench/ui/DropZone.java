package org.example.shaclworkbench.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A list panel that accepts drag-and-drop of files and folders.
 * Folders are expanded to their immediate *.ttl children (non-recursive).
 * Individual files are added as-is regardless of extension.
 */
public class DropZone extends JPanel {

    private final DefaultListModel<Path> model = new DefaultListModel<>();
    private final JList<Path> list;

    public DropZone(String title) {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder(title));

        list = new JList<>(model);
        list.setCellRenderer(new PathCellRenderer());
        list.setToolTipText("Drag files or folders here, or use Add button");

        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(320, 160));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton addFile = new JButton("Add file…");
        JButton addFolder = new JButton("Add folder…");
        JButton remove = new JButton("Remove");

        addFile.addActionListener(e -> chooseFile());
        addFolder.addActionListener(e -> chooseFolder());
        remove.addActionListener(e -> removeSelected());

        buttons.add(addFile);
        buttons.add(addFolder);
        buttons.add(remove);

        add(scroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        installDropTarget();
    }

    public List<Path> getPaths() {
        List<Path> result = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) result.add(model.get(i));
        return result;
    }

    public void clear() {
        model.clear();
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : fc.getSelectedFiles()) addPath(f.toPath());
        }
    }

    private void chooseFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            addPath(fc.getSelectedFile().toPath());
        }
    }

    private void removeSelected() {
        int[] indices = list.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) model.remove(indices[i]);
    }

    private void addPath(Path p) {
        if (Files.isDirectory(p)) {
            try (var stream = Files.list(p)) {
                stream.filter(f -> f.toString().endsWith(".ttl"))
                      .sorted()
                      .forEach(this::addIfAbsent);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Could not read folder: " + ex.getMessage());
            }
        } else {
            addIfAbsent(p);
        }
    }

    private void addIfAbsent(Path p) {
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).equals(p)) return;
        }
        model.addElement(p);
    }

    private void installDropTarget() {
        new DropTarget(list, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent e) {
                if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    e.acceptDrag(DnDConstants.ACTION_COPY);
                } else {
                    e.rejectDrag();
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent e) {
                try {
                    e.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) e.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    files.forEach(f -> addPath(f.toPath()));
                    e.dropComplete(true);
                } catch (Exception ex) {
                    e.dropComplete(false);
                }
            }
        });
    }

    private static class PathCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focused) {
            super.getListCellRendererComponent(list, value, index, selected, focused);
            if (value instanceof Path p) {
                setText(p.getFileName().toString());
                setToolTipText(p.toString());
            }
            return this;
        }
    }
}
