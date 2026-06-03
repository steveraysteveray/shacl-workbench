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
import java.util.function.Consumer;

public class ShaclRunner {

    /** Runs the pipeline with no progress reporting. */
    public ShaclResult run(ShaclConfig config) throws IOException {
        return run(config, ignored -> {});
    }

    /**
     * Runs the pipeline, calling {@code progress} with a short status string at
     * each phase boundary (and per-file while loading) so the UI can display a
     * live indicator rather than appearing hung.
     */
    public ShaclResult run(ShaclConfig config, Consumer<String> progress) throws IOException {
        // ── 1. Build the data graph ───────────────────────────────────────────
        Model dataModel = ModelFactory.createDefaultModel();

        if (config.rootDir() != null) {
            progress.accept("Loading workspace files…");
            try (var stream = Files.walk(config.rootDir())) {
                stream.filter(p -> p.toString().endsWith(".ttl"))
                      .filter(p -> config.excludedPaths().stream().noneMatch(p::startsWith))
                      .forEach(p -> {
                          progress.accept("Loading " + p.getFileName() + "…");
                          RDFDataMgr.read(dataModel, p.toUri().toString());
                      });
            }
        }
        if (config.dataFile() != null) {
            progress.accept("Loading " + config.dataFile().getFileName() + "…");
            RDFDataMgr.read(dataModel, config.dataFile().toUri().toString());
        }

        // ── 2. Load shapes models and register any sh:SPARQLFunction found ────
        Model inferShapesModel = config.runInference() && !config.inferenceShapeFiles().isEmpty()
                ? loadMerged(config.inferenceShapeFiles()) : ModelFactory.createDefaultModel();
        Model validationShapesModel = loadMerged(config.validationShapeFiles());

        int fnCount = ShaclFunctionLoader.registerFrom(dataModel, inferShapesModel, validationShapesModel);
        if (fnCount > 0) progress.accept("Registered " + fnCount + " SPARQL function(s)…");

        // ── 3. Inference pass (SHACL-AF: sh:TripleRule and sh:SPARQLRule) ─────
        List<InferredTriple> inferredTriples = List.of();
        String inferredTurtle = "";
        if (config.runInference() && !config.inferenceShapeFiles().isEmpty()) {
            progress.accept("Running inference…");
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

        // ── 4. Validation pass ────────────────────────────────────────────────
        progress.accept("Validating…");
        Shapes validationShapes = Shapes.parse(validationShapesModel.getGraph());
        ValidationReport report = ShaclValidator.get().validate(
                validationShapes, dataModel.getGraph());

        // ── 5. Merge prefix declarations from every loaded file ───────────────
        progress.accept("Serializing report…");
        PrefixMapping prefixes = PrefixMapping.Factory.create();
        prefixes.setNsPrefixes(dataModel);
        prefixes.setNsPrefixes(validationShapesModel);

        // ── 6. Serialize the report ───────────────────────────────────────────
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
