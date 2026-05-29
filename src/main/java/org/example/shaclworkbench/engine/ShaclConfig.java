package org.example.shaclworkbench.engine;

import java.nio.file.Path;
import java.util.List;

/**
 * @param rootDir              root folder whose .ttl files are loaded recursively as background
 *                             context (nullable if dataFile is provided)
 * @param excludedPaths        sub-paths of rootDir to skip when loading (may be empty)
 * @param dataFile             the single data file to validate (nullable if rootDir is provided)
 * @param inferenceShapeFiles  SHACL-AF shapes containing sh:TripleRule / sh:SPARQLRule
 * @param validationShapeFiles SHACL shapes to validate against
 * @param runInference         when false, the inference pass is skipped
 */
public record ShaclConfig(
        Path rootDir,
        List<Path> excludedPaths,
        Path dataFile,
        List<Path> inferenceShapeFiles,
        List<Path> validationShapeFiles,
        boolean runInference
) {
    public ShaclConfig {
        if (rootDir == null && dataFile == null)
            throw new IllegalArgumentException("at least one of rootDir or dataFile is required");
        if (validationShapeFiles == null || validationShapeFiles.isEmpty())
            throw new IllegalArgumentException("at least one validation shapes file is required");
        excludedPaths        = excludedPaths == null        ? List.of() : List.copyOf(excludedPaths);
        inferenceShapeFiles  = inferenceShapeFiles == null  ? List.of() : List.copyOf(inferenceShapeFiles);
        validationShapeFiles = List.copyOf(validationShapeFiles);
    }
}
