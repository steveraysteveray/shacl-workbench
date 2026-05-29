#!/usr/bin/env bash
# Builds a macOS .dmg installer using jpackage (requires JDK 21+).
# Output: dist/SHACL Workbench-<VERSION>.dmg
set -euo pipefail

VERSION="1.0"
JAR="shacl-workbench-1.0-SNAPSHOT.jar"
APP_NAME="SHACL Workbench"

echo "▶ Building fat JAR..."
mvn clean package -q -DskipTests

mkdir -p dist

# Use a custom icon if one has been placed at src/main/resources/icon.icns
ICON_ARG=()
if [ -f "src/main/resources/icon.icns" ]; then
  ICON_ARG=(--icon src/main/resources/icon.icns)
  echo "  icon: src/main/resources/icon.icns"
fi

echo "▶ Running jpackage..."
jpackage \
  --name         "$APP_NAME" \
  --app-version  "$VERSION" \
  --description  "SHACL inference and validation desktop workbench" \
  --vendor       "Steve Ray" \
  --input        target \
  --main-jar     "$JAR" \
  --main-class   org.example.shaclworkbench.Main \
  --type         dmg \
  --dest         dist \
  --java-options "-Xmx512m" \
  ${ICON_ARG[@]+"${ICON_ARG[@]}"}

DMG="dist/${APP_NAME}-${VERSION}.dmg"
echo "✓  Created: $DMG"
echo ""
echo "To publish a GitHub release run:"
echo "  gh release create v${VERSION} \"$DMG\" \\"
echo "    --title \"SHACL Workbench ${VERSION}\" \\"
echo "    --notes \"See README for usage instructions.\""
