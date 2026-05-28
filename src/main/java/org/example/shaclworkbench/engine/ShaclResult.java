package org.example.shaclworkbench.engine;

import org.apache.jena.shacl.ValidationReport;

/**
 * @param conforms            true iff sh:conforms true
 * @param report              Jena ValidationReport for programmatic access
 * @param reportTurtle        serialized Turtle of the sh:ValidationReport graph
 * @param inferredTripleCount triples added during the inference pass (0 if inference skipped)
 */
public record ShaclResult(
        boolean conforms,
        ValidationReport report,
        String reportTurtle,
        int inferredTripleCount
) {}
