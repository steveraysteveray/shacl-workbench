package org.example.shaclworkbench.engine;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shacl.ValidationReport;

/**
 * @param conforms            true iff sh:conforms true
 * @param report              Jena ValidationReport for programmatic access
 * @param reportTurtle        serialized Turtle of the sh:ValidationReport graph
 * @param inferredTripleCount triples added during the inference pass (0 if inference skipped)
 * @param inferredTurtle      serialized Turtle of only the inferred triples (empty if none)
 * @param prefixMap           merged prefix declarations from all loaded files, for URI display
 */
public record ShaclResult(
        boolean conforms,
        ValidationReport report,
        String reportTurtle,
        int inferredTripleCount,
        String inferredTurtle,
        PrefixMapping prefixMap
) {}
