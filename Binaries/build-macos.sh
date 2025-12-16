#!/bin/bash
# Build script for macOS .dmg installer using jpackage
# Run this on a macOS machine

set -e

echo "=== Building macOS Installer for SketchApp ==="

# Configuration
APP_NAME="SketchApp"
APP_VERSION="4.0.0"
MAIN_CLASS="cad.Main"
JAR_FILE="../target/SketchApp.jar"
OUTPUT_DIR="installer"

# Ensure output directory exists
mkdir -p "$OUTPUT_DIR"

# Check if JAR exists
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Please run: mvn clean package"
    exit 1
fi

# Check if jpackage is available
if ! command -v jpackage &> /dev/null; then
    echo "Error: jpackage not found. Please ensure you're using JDK 14 or later."
    exit 1
fi

# Convert Icon
echo "Ensuring icon is ready (Generating ICNS)..."
if command -v python3 &> /dev/null; then
    python3 convert_icon.py
else
    echo "Warning: Python3 not found. Skipping icon conversion. Ensure .icns exists."
fi

# Staging directory for jpackage input
STAGING_DIR="staging"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
cp "$JAR_FILE" "$STAGING_DIR/"
cp "../src/main/resources/sketchapp-icon.icns" "$STAGING_DIR/" || true

echo "Creating macOS DMG installer..."

# Create the macOS installer
jpackage \
  --type dmg \
  --input "$STAGING_DIR" \
  --name "$APP_NAME" \
  --main-jar SketchApp.jar \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --vendor "SketchApp Team" \
  --description "Professional CAD Sketching Application" \
  --icon "$STAGING_DIR/sketchapp-icon.icns" \
  --dest "$OUTPUT_DIR" \
  --mac-package-name "SketchApp" \
  --java-options "-Xmx2048m" \
  --java-options "-Dsun.java2d.opengl=true" \
  --java-options "-XstartOnFirstThread" \
  --arguments "--gui"

# Cleanup
rm -rf "$STAGING_DIR"

echo ""
echo "=== Build Complete ==="
echo "macOS installer created: $OUTPUT_DIR/$APP_NAME-$APP_VERSION.dmg"
echo ""
echo "Installation Instructions:"
echo "  1. Double-click the .dmg file"
echo "  2. Drag SketchApp.app to the Applications folder"
echo "  3. Launch from Launchpad or Applications folder"
