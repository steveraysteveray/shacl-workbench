# SHACL Workbench — Claude guidance

## What this project is

A Java/Swing desktop workbench for running SHACL inference (sh:TripleRule, sh:SPARQLRule)
and validation over RDF datasets. Standalone fat JAR; macOS DMG via `package-mac.sh`.

- Repo: https://github.com/steveraysteveray/shacl-workbench
- Java 21, Apache Jena 5.6.0, Swing (Metal LAF), Maven

## Build

```
mvn package          # produces target/shacl-workbench-1.0-SNAPSHOT.jar
java -jar target/shacl-workbench-1.0-SNAPSHOT.jar
```

See `shacl-workbench-build.md` in memory for fat-JAR gotchas.

## Release process

1. Bump `VERSION=` in `package-mac.sh`
2. `./package-mac.sh` → `dist/SHACL Workbench-<VERSION>.dmg`
3. `gh release create v<VERSION> "dist/SHACL Workbench-<VERSION>.dmg" --title "..." --notes "..."`
- DMG bundles its own JRE. App is unsigned; first launch needs right-click → Open.
- `dist/` is gitignored; never commit the DMG.

## Key source locations

| What | Where |
|---|---|
| Entry point | `src/main/java/org/example/shaclworkbench/Main.java` |
| Main window | `src/main/java/org/example/shaclworkbench/ui/WorkbenchFrame.java` |
| SHACL engine | `src/main/java/org/example/shaclworkbench/engine/ShaclRunner.java` |
| Theme | `src/main/java/org/example/shaclworkbench/ui/theme/CopperSteamTheme.java` |
| Session persistence | `src/main/java/org/example/shaclworkbench/session/` |
| Resources | `src/main/resources/` — `icon.icns`, `icon.png`, `watermark.png` |

## Session persistence

Saved to `~/.shacl-workbench/session.properties`; named configs in `~/.shacl-workbench/configs/`.
Fields: rootFolder, exclusions, dataFile, inferenceShapes, validationShapes, inferAndValidate, fontSize.

## UI notes

- Theme: "Copper & Steam" (`CopperSteamTheme extends DefaultMetalTheme`)
- Font scaling: CMD-= / CMD-+ (larger), CMD-- (smaller), CMD-0 (reset to 13pt); persisted to session
- Watermark: magnifier PNG drawn at 15% opacity in both result tables when empty
- `apple.laf.useScreenMenuBar=true` → Help menu appears in macOS system menu bar
- Dock icon set via `Taskbar.getTaskbar().setIconImage()` in `Main.java`
- Use `ImageIO.read(url)` not `new ImageIcon(url)` — ImageIcon loads asynchronously and
  returns -1 dimensions before the image is ready

## Working with Claude

- Commit completed work but do not push — user reviews before pushing
- Do not add code comments unless the WHY is non-obvious
- Keep responses concise; no trailing summaries of what was just done
