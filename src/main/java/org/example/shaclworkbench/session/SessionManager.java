package org.example.shaclworkbench.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves and restores workbench state to {@code ~/.shacl-workbench/session.properties}.
 * All I/O errors are logged and silently swallowed so they never interrupt the UI.
 */
public class SessionManager {

    private static final Logger LOG = Logger.getLogger(SessionManager.class.getName());
    private static final Path STATE_FILE = Path.of(
            System.getProperty("user.home"), ".shacl-workbench", "session.properties");
    /** Delimiter between list items — pipe cannot appear in macOS file paths. */
    private static final String SEP = "|";

    public static void save(SessionState state) {
        try {
            Files.createDirectories(STATE_FILE.getParent());
            Properties p = new Properties();
            p.setProperty("rootFolder",       state.rootFolder());
            p.setProperty("exclusions",       join(state.exclusions()));
            p.setProperty("dataFile",         state.dataFile());
            p.setProperty("inferenceShapes",  join(state.inferenceShapes()));
            p.setProperty("validationShapes", join(state.validationShapes()));
            p.setProperty("inferAndValidate", String.valueOf(state.inferAndValidate()));
            try (OutputStream out = Files.newOutputStream(STATE_FILE)) {
                p.store(out, "SHACL Workbench session");
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not save session", e);
        }
    }

    public static Optional<SessionState> load() {
        if (!Files.exists(STATE_FILE)) return Optional.empty();
        try (InputStream in = Files.newInputStream(STATE_FILE)) {
            Properties p = new Properties();
            p.load(in);
            return Optional.of(new SessionState(
                    p.getProperty("rootFolder",       ""),
                    split(p.getProperty("exclusions",       "")),
                    p.getProperty("dataFile",         ""),
                    split(p.getProperty("inferenceShapes",  "")),
                    split(p.getProperty("validationShapes", "")),
                    Boolean.parseBoolean(p.getProperty("inferAndValidate", "true"))
            ));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Could not load session", e);
            return Optional.empty();
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String join(List<String> items) {
        return String.join(SEP, items);
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("\\|", -1))
                     .filter(s -> !s.isBlank())
                     .toList();
    }
}
