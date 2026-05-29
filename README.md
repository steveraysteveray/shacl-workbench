# SHACL Workbench

A desktop application for running SHACL inferencing and validation against RDF/Turtle files, built on [Apache Jena](https://jena.apache.org/) 5.x.

## Features

- **SHACL-AF inference** — applies `sh:TripleRule` and `sh:SPARQLRule` rules to a fixed point before validating
- **SHACL validation** — produces a standard `sh:ValidationReport` using the Jena SHACL engine
- **Workspace root folder** — loads all `.ttl` files recursively; individual sub-paths can be excluded
- **Drag-and-drop** — drop folders or files onto any shapes list or input field
- **Severity filter** — show Violations, Warnings, Info, or any combination in the report table
- **Prefixed URIs** — focus nodes and constraints are displayed using declared prefix mappings, with a toggle to full URI
- **Inferred triples viewer** — inspect exactly which triples were added during the inference pass
- **Session persistence** — configuration is saved automatically on exit and restored on next launch

## Prerequisites

| Requirement | Version |
|-------------|---------|
| Java (JDK)  | 21 or later |
| Maven       | 3.8 or later |

No other installation is needed. All Jena dependencies are bundled into the fat JAR at build time.

To check your versions:
```
java -version
mvn -version
```

## Build

```
git clone https://github.com/steveraysteveray/shacl-workbench.git
cd shacl-workbench
mvn clean package -DskipTests
```

The self-contained JAR is written to `target/shacl-workbench-1.0-SNAPSHOT.jar` (~15 MB).

## Run

```
java -jar target/shacl-workbench-1.0-SNAPSHOT.jar &
```

macOS may show a security prompt the first time; if so, open **System Settings → Privacy & Security** and allow the app to run.

## Usage

### 1. Set your inputs

| Field | Purpose |
|-------|---------|
| **Root folder** | Folder whose `.ttl` files are loaded recursively as background context (ontologies, schemas). Optional if a data file is provided. |
| **Workspace exclusions** | Sub-folders or files within the root folder to skip (e.g. a directory of rules files you don't want loaded as data). |
| **Data file** | A single `.ttl` file to validate. Optional if a root folder is provided. |

All fields accept drag-and-drop. Use the **Browse…** and **Clear** buttons as alternatives.

### 2. Add shapes files

| Zone | Purpose |
|------|---------|
| **Inference shapes** | SHACL-AF shapes containing `sh:TripleRule` or `sh:SPARQLRule` rules. Only used when *Infer + Validate* is selected. |
| **Validation shapes** | Standard SHACL shapes used for constraint checking. At least one file is required. |

Drag files or folders onto each zone, or use the **Add file…** / **Add folder…** buttons. Dropping a folder adds its immediate `.ttl` children.

### 3. Choose a mode and run

- **Infer + Validate** — inference rules are applied to a fixed point, then validation runs against the expanded graph
- **Validate only** — inference is skipped

Click **Run**. Results appear in the tabs below.

### 4. Review results

**Validation Report tab**
- The table shows focus node, path, constraint component, severity, and message for each result entry
- Use the **Violation / Warning / Info** checkboxes to filter by severity; the status line shows how many results are visible
- Toggle **Full URI** to switch between prefixed and expanded URIs
- **Copy Turtle** copies the raw `sh:ValidationReport` graph; **Save report…** writes it to a file

**Inferred Triples tab**
- Shows the Turtle serialisation of only the triples added during the inference pass

### 5. Session

Configuration (root folder, shapes files, exclusions, mode) is saved automatically when the window is closed and restored on the next launch. The session file is stored at:

```
~/.shacl-workbench/session.properties
```

## Running the tests

```
mvn test
```

The test suite validates that the Jena pipeline correctly detects constraint violations and that the SHACL-AF inference engine adds the expected triples.
