package org.example.shaclworkbench.engine;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ShaclRunner {

    public ShaclResult run(ShaclConfig config) throws IOException {
        // ── 1. Build the data graph ───────────────────────────────────────────
        // Workspace files provide background context (schemas, ontologies, etc.);
        // the data file is the focus of validation.
        Model dataModel = ModelFactory.createDefaultModel();

        if (config.rootDir() != null) {
            try (var stream = Files.walk(config.rootDir())) {
                stream.filter(p -> p.toString().endsWith(".ttl"))
                      .filter(p -> config.excludedPaths().stream().noneMatch(p::startsWith))
                      .forEach(p -> RDFDataMgr.read(dataModel, p.toUri().toString()));
            }
        }
        if (config.dataFile() != null) {
            RDFDataMgr.read(dataModel, config.dataFile().toUri().toString());
        }

        // ── 2. Inference pass (SHACL-AF: sh:TripleRule and sh:SPARQLRule) ─────
        List<InferredTriple> inferredTriples = List.of();
        String inferredTurtle = "";
        if (config.runInference() && !config.inferenceShapeFiles().isEmpty()) {
            Model inferShapesModel = loadMerged(config.inferenceShapeFiles());
            ShaclAFEngine engine = new ShaclAFEngine(
                    inferShapesModel.getGraph(), dataModel.getGraph());
            inferredTriples = engine.execute();

            if (!inferredTriples.isEmpty()) {
                Model inferredModel = ModelFactory.createDefaultModel();
                inferredModel.setNsPrefixes(dataModel.getNsPrefixMap());
                for (InferredTriple it : inferredTriples) {
                    inferredModel.getGraph().add(it.triple());
                }
                ByteArrayOutputStream ibuf = new ByteArrayOutputStream();
                RDFDataMgr.write(ibuf, inferredModel, Lang.TURTLE);
                inferredTurtle = ibuf.toString();
            }
        }

        // ── 3. Validation pass ────────────────────────────────────────────────
        Model validationShapesModel = loadMerged(config.validationShapeFiles());
        Shapes validationShapes = Shapes.parse(validationShapesModel.getGraph());
        ValidationReport report = ShaclValidator.get().validate(
                validationShapes, dataModel.getGraph());

        // ── 4. Merge prefix declarations from every loaded file ───────────────
        // Used by the UI to display prefixed URIs (e.g. ex:Alice) in the report.
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefixes(dataModel);
        prefixes.setNsPrefixes(validationShapesModel);

        // ── 5. Serialize the report ───────────────────────────────────────────
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        RDFDataMgr.write(baos, report.getModel(), Lang.TURTLE);

        return new ShaclResult(report.conforms(), report, baos.toString(),
                inferredTriples, inferredTurtle, prefixes);
    }

    private static Model loadMerged(List<Path> paths) {
        Model model = ModelFactory.createDefaultModel();
        for (Path p : paths) {
            RDFDataMgr.read(model, p.toUri().toString());
        }
        return model;
    }
}
