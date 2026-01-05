#!/bin/bash
# Build script for Windows .exe installer using jpackage
# Run this on a Windows machine (Git Bash or WSL)

set -e

echo "=== Building Windows Installer for SketchApp ==="

# Configuration
APP_NAME="SketchApp"
APP_VERSION="4.5.0"
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
echo "Ensuring icon is ready..."
if command -v python &> /dev/null; then
    python convert_icon.py
elif command -v python3 &> /dev/null; then
    python3 convert_icon.py
else
    echo "Warning: Python not found. Skipping icon conversion check."
fi

# Staging directory for jpackage input
STAGING_DIR="staging"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
cp "$JAR_FILE" "$STAGING_DIR/"
cp "../src/main/resources/sketchapp-icon.ico" "$STAGING_DIR/" || true
cp "../materials.json" "$STAGING_DIR/" || echo "Warning: materials.json not found, will use defaults"

echo "Creating Windows installer..."

# Create the Windows installer
# PER-USER INSTALLATION
jpackage \
  --type exe \
  --input "$STAGING_DIR" \
  --name "$APP_NAME" \
  --main-jar SketchApp.jar \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --vendor "SketchApp Team" \
  --description "Professional CAD Sketching Application" \
  --icon "$STAGING_DIR/sketchapp-icon.ico" \
  --dest "$OUTPUT_DIR" \
  --win-dir-chooser \
  --win-menu \
  --win-shortcut \
  --win-menu-group "SketchApp" \
  --win-per-user-install \
  --java-options "-Xmx2048m" \
  --java-options "-Dsun.java2d.opengl=true" \
  --arguments "--gui"

# Cleanup
rm -rf "$STAGING_DIR"

echo ""
echo "=== Build Complete ==="
echo "Windows installer created: $OUTPUT_DIR/$APP_NAME-$APP_VERSION.exe"
echo ""
echo "Installation Instructions:"
echo "  1. Double-click the .exe file"
echo "  2. Follow the installation wizard"
echo "  3. SketchApp will be installed to %LocalAppDir%\\SketchApp"
