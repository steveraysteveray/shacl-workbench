# SHACL Workbench — Compatibility Notes

The Workbench uses the **Apache Jena** SHACL engine. Jena implements the
[W3C SHACL](https://www.w3.org/TR/shacl/) and
[SHACL-AF](https://www.w3.org/TR/shacl-af/) specifications faithfully, but there
are categories of shapes that rely on capabilities outside those standards. This
page describes each category, its effect, and how to recognise it.

---

## 1. Custom SPARQL function libraries

SHACL shapes can call SPARQL extension functions beyond the standard SPARQL 1.1
built-ins. These are often defined as `sh:SPARQLFunction` declarations (standard
SHACL-AF) and registered explicitly by the toolchain before running validation.

**Effect:** Jena does not automatically discover `sh:SPARQLFunction` declarations
from the loaded graph and register them as callable functions. Constraints that
call unregistered functions will **silently produce no results** — the function
call fails quietly, the constraint fires on no nodes, and no warning is raised.
Loading the function definition file into the Workbench does not help.

**How to recognise it:** Look for prefixed function calls inside `sh:sparql`,
`sh:SPARQLRule`, or `sh:SPARQLTarget` blocks, e.g.:

```turtle
sh:select """
  SELECT $this WHERE {
    FILTER(qfn:dimVec.normalize($this) != "")
  }
"""
```

**Example:** The QUDT ontology defines its own function library under
`http://qudt.org/shacl/functions#` (prefix `qfn:`), implemented as
`sh:SPARQLFunction` declarations in `src/build/validation/qudt-shacl-functions.ttl`.
The QUDT Maven build registers these explicitly via a `<shaclFunctions>` parameter
before running any SHACL operations. Jena does not perform this registration
automatically, so the functions remain unresolved even when the definitions file
is loaded into the Workbench. For more information on QUDT, see the
[QUDT repository](https://github.com/qudt/qudt-public-repo). See the table below.

### QUDT shapes files — compatibility summary

| Shapes file | `qfn:` calls | Workbench result |
|---|---|---|
| `QUDT_SRC_QA_TESTS.ttl` | 0 | ✅ Reliable |
| `SHACL-SHACL.ttl` | 0 | ✅ Reliable |
| `COLLECTION_QUDT_USER_TESTS.ttl` | 0 | ✅ Reliable |
| `COLLECTION_QUDT_QA_TESTS_ALL.ttl` | 28 (`dimVec`, `conversionMultiplier`, `bound`…) | ❌ Affected constraints silently produce no results |
| `sparql2shacl/*/infer.ttl` and `validate.ttl` | Many (`qfn:unit.dimVec.calculate`, `qfn:decimalToDouble`, …) | ❌ Not suitable |

**Practical rule:** Use `COLLECTION_QUDT_USER_TESTS.ttl` with the Workbench.
Use the QUDT Maven build for full QA validation.

---

## 2. SHACL-JS (JavaScript constraints)

The [SHACL-JS](https://www.w3.org/TR/shacl-js/) extension allows constraints and
functions to be written in JavaScript (`sh:JSConstraint`, `sh:JSFunction`,
`sh:JSRule`). Jena does not implement SHACL-JS.

**Effect:** JavaScript-based shapes are **silently ignored** — no error is raised
and no result is produced for those constraints.

**How to recognise it:** Look for `sh:js`, `sh:jsLibrary`, or
`sh:JSConstraint` / `sh:JSFunction` in the shapes file.

---

## 3. OWL entailment / reasoning

The Workbench validates against the **asserted** graph only. It does not perform
OWL reasoning before validation. Shapes that assume OWL entailments — inferred
class memberships from `owl:equivalentClass`, property chains from
`owl:propertyChainAxiom`, values inferred via `owl:inverseOf`, etc. — may produce
missed violations or false positives.

**How to recognise it:** The ontology uses OWL axioms heavily and its shapes are
designed to run under an OWL-RL or OWL-DL reasoner (e.g. TopBraid, RDFox). Look
for `sh:entailment` declarations in the shapes graph:

```turtle
[] sh:entailment <http://www.w3.org/ns/entailment/OWL-RDF-Based> .
```

---

## 4. `owl:imports` not followed

If an ontology file declares `owl:imports <some-iri>`, the Workbench does **not**
fetch or load the imported ontology automatically. The imported triples will be
absent from the graph being validated.

**Workaround:** Download the imported ontology and add it to the root folder (or
drop it into the appropriate shapes zone) before running.

---

## 5. Non-Turtle serializations ignored

The Workbench loads only **`.ttl` files**. RDF/XML (`.rdf`, `.owl`), JSON-LD
(`.jsonld`), N-Triples (`.nt`), TriG (`.trig`), and N-Quads (`.nq`) files are
silently skipped when scanning a folder recursively.

**Workaround:** Convert to Turtle first (e.g. using Apache Jena's `riot` command-line
tool: `riot --output=turtle input.rdf > input.ttl`).

---

## 6. Named graphs

All loaded files are merged into a **single default graph**. Named graph structure
from TriG or N-Quads files is lost (and those formats are not loaded anyway — see
above). Shapes that target specific named graphs using `sh:target` or graph-scoped
`sh:SPARQLTarget` queries will not behave as intended.

---

## 7. Federated SPARQL in `sh:sparql` constraints

`SERVICE` clauses inside `sh:sparql` or `sh:SPARQLRule` blocks will not execute —
Jena will not reach out to remote SPARQL endpoints at validation time.

**Effect:** The constraint either produces no results or fails with a SPARQL
evaluation error. No remote data is fetched.
