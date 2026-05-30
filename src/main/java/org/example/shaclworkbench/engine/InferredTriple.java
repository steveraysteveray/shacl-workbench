package org.example.shaclworkbench.engine;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * An RDF triple produced during the SHACL-AF inference pass, annotated with
 * the shape and rule that generated it.
 *
 * @param triple    the newly added triple
 * @param shapeNode the sh:NodeShape whose sh:rule fired
 * @param ruleNode  the blank node or IRI that is the sh:rule object
 * @param ruleType  "TripleRule" or "SPARQLRule"
 */
public record InferredTriple(
        Triple triple,
        Node shapeNode,
        Node ruleNode,
        String ruleType
) {}
