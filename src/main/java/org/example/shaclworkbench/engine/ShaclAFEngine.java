package org.example.shaclworkbench.engine;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.parser.Shape;
import org.apache.jena.shacl.vocabulary.SHACL;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Minimal SHACL-AF rules engine for sh:TripleRule and sh:SPARQLRule.
 *
 * Iterates rule application to a fixed point (no new triples are added).
 * A safety limit of 100 passes guards against infinite loops in cyclic rules.
 * Each added triple is returned with attribution to the shape and rule that produced it.
 */
public class ShaclAFEngine {

    private static final Node SH_THIS = NodeFactory.createURI(SHACL.NS + "this");
    private static final int MAX_PASSES = 100;

    private final Graph shapesGraph;
    private final Graph dataGraph;
    private final Model shapesModel;
    private final Model dataModel;
    private final Shapes shapes;

    public ShaclAFEngine(Graph shapesGraph, Graph dataGraph) {
        this.shapesGraph = shapesGraph;
        this.dataGraph   = dataGraph;
        this.shapesModel = ModelFactory.createModelForGraph(shapesGraph);
        this.dataModel   = ModelFactory.createModelForGraph(dataGraph);
        this.shapes      = Shapes.parse(shapesGraph);
    }

    /**
     * Applies all rules until no new triples are produced.
     *
     * @return every triple added to the data graph, attributed to the rule that added it
     */
    public List<InferredTriple> execute() {
        List<InferredTriple> all = new ArrayList<>();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            List<InferredTriple> added = onePass();
            all.addAll(added);
            if (added.isEmpty()) break;
        }
        return all;
    }

    // ── private ──────────────────────────────────────────────────────────────

    private List<InferredTriple> onePass() {
        List<InferredTriple> added = new ArrayList<>();

        for (Shape shape : shapes.getTargetShapes()) {
            if (shape.deactivated()) continue;

            List<Node> rules = shapesGraph
                    .find(shape.getShapeNode(), SHACL.rule, Node.ANY)
                    .mapWith(Triple::getObject)
                    .toList();
            if (rules.isEmpty()) continue;

            Set<Node> focusNodes = new HashSet<>();
            for (var target : shape.getTargets()) {
                focusNodes.addAll(target.getFocusNodes(dataGraph));
            }
            if (focusNodes.isEmpty()) continue;

            for (Node rule : rules) {
                if (isType(rule, SHACL.TripleRule)) {
                    applyTripleRule(focusNodes, rule, shape.getShapeNode(), added);
                } else if (isType(rule, SHACL.SPARQLRule)) {
                    applySPARQLRule(focusNodes, rule, shape.getShapeNode(), added);
                }
            }
        }

        return added;
    }

    private void applyTripleRule(Set<Node> focusNodes, Node rule, Node shapeNode,
                                  List<InferredTriple> out) {
        Node subjExpr = singleObject(rule, SHACL.subject);
        Node predExpr = singleObject(rule, SHACL.predicate);
        Node objExpr  = singleObject(rule, SHACL.object);
        if (subjExpr == null || predExpr == null || objExpr == null) return;

        for (Node focus : focusNodes) {
            Node s = resolve(subjExpr, focus);
            Node p = resolve(predExpr, focus);
            Node o = resolve(objExpr, focus);
            if (s != null && p != null && o != null) {
                Triple t = Triple.create(s, p, o);
                if (!dataGraph.contains(t)) {
                    dataGraph.add(t);
                    out.add(new InferredTriple(t, shapeNode, rule, "TripleRule"));
                }
            }
        }
    }

    private void applySPARQLRule(Set<Node> focusNodes, Node rule, Node shapeNode,
                                  List<InferredTriple> out) {
        Node constructNode = singleObject(rule, SHACL.construct);
        if (constructNode == null || !constructNode.isLiteral()) return;

        String queryString = buildPrefixHeader(rule) + constructNode.getLiteralLexicalForm();
        Query query;
        try {
            query = QueryFactory.create(queryString);
        } catch (QueryParseException e) {
            throw new IllegalArgumentException(
                    "sh:construct query in rule <" + rule + "> is invalid: " + e.getMessage(), e);
        }

        for (Node focus : focusNodes) {
            QuerySolutionMap binding = new QuerySolutionMap();
            binding.add("this", dataModel.asRDFNode(focus));
            try (QueryExecution qe = QueryExecution.model(dataModel)
                    .query(query)
                    .substitution(binding)
                    .build()) {
                qe.execConstruct().listStatements().forEachRemaining(stmt -> {
                    if (!dataModel.contains(stmt)) {
                        dataModel.add(stmt);
                        out.add(new InferredTriple(stmt.asTriple(), shapeNode, rule, "SPARQLRule"));
                    }
                });
            }
        }
    }

    /** sh:this → focus node; anything else → value as-is. */
    private static Node resolve(Node expr, Node focus) {
        return SH_THIS.equals(expr) ? focus : expr;
    }

    private boolean isType(Node node, Node type) {
        return shapesGraph.contains(node, RDF.type.asNode(), type);
    }

    private Node singleObject(Node subject, Node predicate) {
        var it = shapesGraph.find(subject, predicate, Node.ANY);
        return it.hasNext() ? it.next().getObject() : null;
    }

    /**
     * Assembles PREFIX declarations from sh:prefixes → sh:declare triples so that
     * the sh:construct query body can reference them without embedding PREFIX lines.
     */
    private String buildPrefixHeader(Node rule) {
        StringBuilder sb = new StringBuilder();
        shapesGraph.find(rule, SHACL.prefixes, Node.ANY).forEachRemaining(t -> {
            Node bucket = t.getObject();
            shapesGraph.find(bucket, SHACL.declare, Node.ANY).forEachRemaining(d -> {
                Node pfx = singleObject(d.getObject(), SHACL.prefix);
                Node ns  = singleObject(d.getObject(), SHACL.namespace);
                if (pfx != null && ns != null) {
                    sb.append("PREFIX ").append(pfx.getLiteralLexicalForm())
                      .append(": <").append(ns.getLiteralLexicalForm()).append(">\n");
                }
            });
        });
        return sb.toString();
    }
}
