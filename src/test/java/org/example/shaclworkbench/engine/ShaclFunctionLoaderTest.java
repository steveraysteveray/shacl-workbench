package org.example.shaclworkbench.engine;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that ShaclFunctionLoader correctly registers sh:SPARQLFunction
 * declarations so they can be called from SPARQL queries.
 */
class ShaclFunctionLoaderTest {

    private static final String PREFIXES = """
            PREFIX sh:   <http://www.w3.org/ns/shacl#>
            PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>
            PREFIX ex:   <http://example.org/fn#>
            """;

    /** A simple sh:SPARQLFunction that concatenates a string with a suffix. */
    private static final String SIMPLE_FUNCTION_TTL = PREFIXES + """
            ex:addSuffix
              a sh:SPARQLFunction ;
              sh:parameter [ sh:path ex:input ; sh:order 0 ] ;
              sh:parameter [ sh:path ex:suffix ; sh:order 1 ] ;
              sh:select "SELECT (CONCAT(STR(?input), STR(?suffix)) AS ?result) WHERE {}" .
            """;

    /** A function that calls another function — mirrors the qfn: call-chain pattern. */
    private static final String CHAINED_FUNCTION_TTL = PREFIXES + """
            ex:double
              a sh:SPARQLFunction ;
              sh:parameter [ sh:path ex:value ; sh:order 0 ] ;
              sh:select "SELECT (?value * 2 AS ?result) WHERE {}" .

            # sh:prefixes supplies the ex: prefix so ex:double resolves inside the body
            ex:quadruple
              a sh:SPARQLFunction ;
              sh:parameter [ sh:path ex:value ; sh:order 0 ] ;
              sh:prefixes ex:prefixGraph ;
              sh:select "SELECT (ex:double(ex:double(?value)) AS ?result) WHERE {}" .

            ex:prefixGraph sh:declare [
              sh:prefix "ex" ;
              sh:namespace "http://example.org/fn#"^^xsd:anyURI
            ] .
            """;

    @Test
    void simpleFunction_registersAndExecutes() {
        Model model = parse(SIMPLE_FUNCTION_TTL);
        int count = ShaclFunctionLoader.registerFrom(model);
        assertEquals(1, count);

        String result = execScalarSparql(
                "PREFIX ex: <http://example.org/fn#> " +
                "SELECT (ex:addSuffix('hello', '_world') AS ?r) WHERE {}",
                DatasetFactory.empty());
        assertEquals("hello_world", result);
    }

    @Test
    void chainedFunctions_bothCallableAfterRegistration() {
        Model model = parse(CHAINED_FUNCTION_TTL);
        int count = ShaclFunctionLoader.registerFrom(model);
        assertEquals(2, count);

        String result = execScalarSparql(
                "PREFIX ex: <http://example.org/fn#> " +
                "SELECT (ex:quadruple(3) AS ?r) WHERE {}",
                DatasetFactory.empty());
        assertEquals("12", result);
    }

    @Test
    void nfDecimalDiv_registersAndDivides() {
        // registerFrom triggers nf: registration as a side-effect
        ShaclFunctionLoader.registerFrom(org.apache.jena.rdf.model.ModelFactory.createDefaultModel());

        String result = execScalarSparql(
                "PREFIX nf: <https://github.com/qudtlib/numericFunctions/> " +
                "SELECT (nf:decimal.div(1.0, 4.0, 10) AS ?r) WHERE {}",
                DatasetFactory.empty());
        assertNotNull(result);
        assertTrue(result.startsWith("0.25"), "Expected 0.25…, got: " + result);
    }

    @Test
    void functionWithPrefixDeclarations_resolvesNamespaces() {
        // Mimics the qfn: pattern where sh:prefixes carries all namespace declarations
        String ttl = """
                @prefix sh:   <http://www.w3.org/ns/shacl#> .
                @prefix xsd:  <http://www.w3.org/2001/XMLSchema#> .
                @prefix ex:   <http://example.org/fn#> .

                ex:fnPrefixDecls
                  a sh:SPARQLFunction ;
                  sh:parameter [ sh:path ex:v ; sh:order 0 ] ;
                  sh:prefixes ex:myPrefixGraph ;
                  sh:select "SELECT (xsd:integer(?v) + 1 AS ?result) WHERE {}" .

                ex:myPrefixGraph
                  sh:declare [ sh:prefix "xsd" ;
                                sh:namespace "http://www.w3.org/2001/XMLSchema#"^^xsd:anyURI ] .
                """;
        Model model = parse(ttl);
        ShaclFunctionLoader.registerFrom(model);

        String result = execScalarSparql(
                "PREFIX ex: <http://example.org/fn#> " +
                "SELECT (ex:fnPrefixDecls(41) AS ?r) WHERE {}",
                DatasetFactory.empty());
        assertEquals("42", result);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Model parse(String ttl) {
        Model m = org.apache.jena.rdf.model.ModelFactory.createDefaultModel(); //NOSONAR
        RDFParser.fromString(ttl).lang(Lang.TURTLE).parse(m);
        return m;
    }

    private static String execScalarSparql(String sparql, Dataset dataset) {
        try (QueryExecution qe = QueryExecutionFactory.create(sparql, dataset)) {
            ResultSet rs = qe.execSelect();
            if (!rs.hasNext()) return null;
            QuerySolution sol = rs.nextSolution();
            String var = rs.getResultVars().get(0);
            org.apache.jena.rdf.model.RDFNode node = sol.get(var);
            return node != null ? node.asNode().getLiteralLexicalForm() : null;
        }
    }

}
