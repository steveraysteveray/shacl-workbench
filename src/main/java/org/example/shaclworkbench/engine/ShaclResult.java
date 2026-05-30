package org.example.shaclworkbench.engine;

import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shacl.ValidationReport;

import java.util.List;

/**
 * @param conforms            true iff sh:conforms true
 * @param report              Jena ValidationReport for programmatic access
 * @param reportTurtle        serialized Turtle of the sh:ValidationReport graph
 * @param inferredTriples     triples added during the inference pass, with rule attribution
 * @param inferredTurtle      serialized Turtle of only the inferred triples (empty if none)
 * @param prefixMap           merged prefix declarations from all loaded files, for URI display
 */
public record ShaclResult(
        boolean conforms,
        ValidationReport report,
        String reportTurtle,
        List<InferredTriple> inferredTriples,
        String inferredTurtle,
        PrefixMapping prefixMap
) {}
