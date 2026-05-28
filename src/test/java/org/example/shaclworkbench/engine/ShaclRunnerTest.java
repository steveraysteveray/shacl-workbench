package org.example.shaclworkbench.engine;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShaclRunnerTest {

    @Test
    void validationDetectsViolation() throws Exception {
        ShaclConfig config = new ShaclConfig(
                null,
                fixture("data.ttl"),
                List.of(),
                List.of(fixture("shapes.ttl")),
                false
        );
        ShaclResult result = new ShaclRunner().run(config);

        assertFalse(result.conforms(), "Alice has no ex:name — should not conform");
        assertEquals(1, result.report().getEntries().size(), "expected exactly one violation");
        assertEquals(0, result.inferredTripleCount());
        assertTrue(result.reportTurtle().contains("sh:ValidationReport"));
    }

    @Test
    void inferenceAddsTriples() throws Exception {
        ShaclConfig config = new ShaclConfig(
                null,
                fixture("data.ttl"),
                List.of(fixture("inference-shapes.ttl")),
                List.of(fixture("shapes.ttl")),
                true
        );
        ShaclResult result = new ShaclRunner().run(config);

        // Alice and Bob should each gain an ex:LivingThing triple
        assertTrue(result.inferredTripleCount() >= 2,
                "expected at least 2 inferred triples, got " + result.inferredTripleCount());
    }

    private static Path fixture(String name) throws Exception {
        URL url = ShaclRunnerTest.class.getClassLoader().getResource("fixtures/" + name);
        assertNotNull(url, "fixture not found: " + name);
        return Path.of(url.toURI());
    }
}
