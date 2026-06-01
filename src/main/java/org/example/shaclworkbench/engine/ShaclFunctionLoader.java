package org.example.shaclworkbench.engine;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.function.Function;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.function.FunctionRegistry;
import org.apache.jena.sparql.util.Context;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Discovers {@code sh:SPARQLFunction} declarations in loaded RDF models and
 * registers them with Jena's global {@link FunctionRegistry} so they can be
 * called from within SHACL SPARQL constraints and rules.
 *
 * <p>Also registers {@code nf:decimal.div}, a high-precision decimal division
 * function that some QUDT {@code qfn:} functions depend on.
 */
public class ShaclFunctionLoader {

    private static final Logger LOG = Logger.getLogger(ShaclFunctionLoader.class.getName());

    private static final String SH        = "http://www.w3.org/ns/shacl#";
    private static final String NF_NS     = "https://github.com/qudtlib/numericFunctions/";
    private static final String NF_DIV    = NF_NS + "decimal.div";

    // ── public API ────────────────────────────────────────────────────────────

    /**
     * Scans all supplied models for {@code sh:SPARQLFunction} declarations and
     * registers each one with Jena's {@link FunctionRegistry}.
     * Also registers the {@code nf:decimal.div} helper.
     *
     * @return number of {@code sh:SPARQLFunction} entries registered
     */
    public static int registerFrom(Model... models) {
        registerNumericHelpers();
        int count = 0;
        Set<String> seen = new HashSet<>();
        for (Model model : models) {
            count += registerFromModel(model, seen);
        }
        if (count > 0) LOG.info("Registered " + count + " sh:SPARQLFunction(s)");
        return count;
    }

    // ── discovery ─────────────────────────────────────────────────────────────

    private static int registerFromModel(Model model, Set<String> seen) {
        Property rdfType   = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
        Resource fnClass   = model.createResource(SH + "SPARQLFunction");
        int count = 0;
        ResIterator subjects = model.listSubjectsWithProperty(rdfType, fnClass);
        while (subjects.hasNext()) {
            Resource fn = subjects.next();
            if (!fn.isURIResource()) continue;
            String uri = fn.getURI();
            if (seen.contains(uri)) continue;
            try {
                SPARQLFunctionImpl impl = build(fn, model);
                FunctionRegistry.get().put(uri, u -> impl);
                seen.add(uri);
                count++;
                LOG.fine("Registered sh:SPARQLFunction: " + uri);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Could not register sh:SPARQLFunction <" + uri + ">: " + e.getMessage());
            }
        }
        return count;
    }

    // ── function builder ──────────────────────────────────────────────────────

    private static SPARQLFunctionImpl build(Resource fn, Model model) {
        Property shSelect    = model.createProperty(SH + "select");
        Property shParameter = model.createProperty(SH + "parameter");
        Property shOrder     = model.createProperty(SH + "order");
        Property shPath      = model.createProperty(SH + "path");
        Property shPrefixes  = model.createProperty(SH + "prefixes");
        Property shDeclare   = model.createProperty(SH + "declare");
        Property shPrefix    = model.createProperty(SH + "prefix");
        Property shNamespace = model.createProperty(SH + "namespace");

        // sh:select
        Statement selectStmt = fn.getProperty(shSelect);
        if (selectStmt == null)
            throw new IllegalArgumentException("No sh:select on <" + fn.getURI() + ">");
        String selectText = selectStmt.getString();

        // sh:parameter — ordered by sh:order
        List<Resource> paramNodes = new ArrayList<>();
        fn.listProperties(shParameter).forEachRemaining(s -> paramNodes.add(s.getObject().asResource()));
        paramNodes.sort(Comparator.comparingInt(p -> {
            Statement os = p.getProperty(shOrder);
            return os != null ? os.getObject().asLiteral().getInt() : 0;
        }));
        List<String> paramVarNames = new ArrayList<>();
        for (Resource param : paramNodes) {
            Statement pathStmt = param.getProperty(shPath);
            if (pathStmt == null) continue;
            String pathUri = pathStmt.getObject().asResource().getURI();
            int sep = Math.max(pathUri.lastIndexOf('#'), pathUri.lastIndexOf('/'));
            paramVarNames.add(sep >= 0 ? pathUri.substring(sep + 1) : pathUri);
        }

        // sh:prefixes — collect all sh:declare prefix/namespace pairs
        StringBuilder prefixBlock = new StringBuilder();
        Statement prefixesStmt = fn.getProperty(shPrefixes);
        if (prefixesStmt != null) {
            Resource prefixesNode = prefixesStmt.getObject().asResource();
            prefixesNode.listProperties(shDeclare).forEachRemaining(declStmt -> {
                Resource decl = declStmt.getObject().asResource();
                Statement ps = decl.getProperty(shPrefix);
                Statement ns = decl.getProperty(shNamespace);
                if (ps != null && ns != null) {
                    prefixBlock.append("PREFIX ").append(ps.getString())
                               .append(": <").append(ns.getString()).append(">\n");
                }
            });
        }

        String fullQuery = prefixBlock + "\n" + selectText;
        Query query = QueryFactory.create(fullQuery);
        List<String> resultVars = query.getResultVars();
        String resultVar = resultVars.isEmpty() ? "result" : resultVars.get(0);

        return new SPARQLFunctionImpl(fn.getURI(), query, paramVarNames, resultVar);
    }

    // ── nf: numeric helpers ───────────────────────────────────────────────────

    /** Registers {@code nf:decimal.div(numerator, denominator, precision)}. */
    private static void registerNumericHelpers() {
        if (!FunctionRegistry.get().isRegistered(NF_DIV)) {
            FunctionRegistry.get().put(NF_DIV, uri -> new NfDecimalDiv());
        }
    }

    // ── sh:SPARQLFunction wrapper ─────────────────────────────────────────────

    private static final class SPARQLFunctionImpl implements Function {

        private final String uri;
        private final Query query;
        private final List<String> paramVarNames;
        private final String resultVarName;

        SPARQLFunctionImpl(String uri, Query query,
                           List<String> paramVarNames, String resultVarName) {
            this.uri          = uri;
            this.query        = query;
            this.paramVarNames = paramVarNames;
            this.resultVarName = resultVarName;
        }

        @Override
        public void build(String uri, ExprList args, Context context) {
            // lenient: no arg-count validation
        }

        @Override
        public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
            // Bind positional arguments to parameter variable names
            QuerySolutionMap initialBinding = new QuerySolutionMap();
            Model helper = ModelFactory.createDefaultModel();
            for (int i = 0; i < paramVarNames.size() && i < args.size(); i++) {
                try {
                    NodeValue val = args.get(i).eval(binding, env);
                    initialBinding.add(paramVarNames.get(i), helper.asRDFNode(val.asNode()));
                } catch (ExprEvalException ignored) {
                    // unbound / error — leave the variable unbound in the sub-query
                }
            }

            // Run against the same dataset the outer query is using
            Dataset dataset = env.getDataset() != null
                    ? DatasetFactory.wrap(env.getDataset())
                    : DatasetFactory.empty();

            try (QueryExecution qe = QueryExecutionFactory.create(query, dataset, initialBinding)) {
                ResultSet rs = qe.execSelect();
                if (rs.hasNext()) {
                    QuerySolution sol = rs.nextSolution();
                    RDFNode result = sol.get(resultVarName);
                    if (result != null) return NodeValue.makeNode(result.asNode());
                }
            } catch (Exception e) {
                LOG.log(Level.FINE, "sh:SPARQLFunction <" + uri + "> evaluation error", e);
            }
            throw new ExprEvalException("sh:SPARQLFunction <" + this.uri + "> returned no result");
        }
    }

    // ── nf:decimal.div ────────────────────────────────────────────────────────

    /**
     * High-precision decimal division: {@code nf:decimal.div(numerator, denominator, precision)}.
     * Returns {@code numerator / denominator} rounded to {@code precision} significant digits.
     */
    private static final class NfDecimalDiv implements Function {

        @Override public void build(String uri, ExprList args, Context context) {}

        @Override
        public NodeValue exec(Binding binding, ExprList args, String uri, FunctionEnv env) {
            if (args.size() < 2)
                throw new ExprEvalException("nf:decimal.div requires at least 2 arguments");
            NodeValue nv0 = args.get(0).eval(binding, env);
            NodeValue nv1 = args.get(1).eval(binding, env);
            int precision = 34;
            if (args.size() >= 3) {
                try {
                    precision = args.get(2).eval(binding, env).getDecimal().intValue();
                } catch (Exception ignored) {}
            }
            BigDecimal numerator   = asBigDecimal(nv0);
            BigDecimal denominator = asBigDecimal(nv1);
            if (denominator.compareTo(BigDecimal.ZERO) == 0)
                throw new ExprEvalException("nf:decimal.div: division by zero");
            BigDecimal result = numerator.divide(denominator,
                    new MathContext(precision, RoundingMode.HALF_UP));
            return NodeValue.makeNode(result.toPlainString(),
                    XSDDatatype.XSDdecimal);
        }

        private static BigDecimal asBigDecimal(NodeValue nv) {
            if (nv.isDecimal()) return nv.getDecimal();
            if (nv.isDouble())  return BigDecimal.valueOf(nv.getDouble());
            if (nv.isFloat())   return BigDecimal.valueOf(nv.getFloat());
            if (nv.isInteger()) return new BigDecimal(nv.getInteger());
            try { return new BigDecimal(nv.getString()); } catch (Exception ignore) {}
            throw new ExprEvalException("nf:decimal.div: cannot convert to decimal: " + nv);
        }
    }
}
