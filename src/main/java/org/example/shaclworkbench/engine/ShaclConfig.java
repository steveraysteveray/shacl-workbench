package org.example.shaclworkbench.engine;

import java.nio.file.Path;
import java.util.List;

/**
 * @param workspaceDir       folder whose .ttl files are loaded as background context (nullable)
 * @param dataFile           the single data file to validate
 * @param inferenceShapeFiles SHACL-AF shapes containing sh:TripleRule / sh:SPARQLRule
 * @param validationShapeFiles SHACL shapes to validate against
 * @param runInference       when false, the inference pass is skipped
 */
public record ShaclConfig(
        Path workspaceDir,
        Path dataFile,
        List<Path> inferenceShapeFiles,
        List<Path> validationShapeFiles,
        boolean runInference
) {
    public ShaclConfig {
        if (dataFile == null) throw new IllegalArgumentException("dataFile is required");
        if (validationShapeFiles == null || validationShapeFiles.isEmpty())
            throw new IllegalArgumentException("at least one validation shapes file is required");
        inferenceShapeFiles = inferenceShapeFiles == null ? List.of() : List.copyOf(inferenceShapeFiles);
        validationShapeFiles = List.copyOf(validationShapeFiles);
    }
}
