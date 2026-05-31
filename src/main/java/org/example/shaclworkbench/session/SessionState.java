package org.example.shaclworkbench.session;

import java.util.List;

/**
 * Snapshot of workbench state that is persisted between sessions.
 * All path values are stored as absolute path strings; empty string means "not set".
 */
public record SessionState(
        String rootFolder,
        List<String> exclusions,
        String dataFile,
        List<String> inferenceShapes,
        List<String> validationShapes,
        boolean inferAndValidate,
        int fontSize
) {}
