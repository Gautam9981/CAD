#!/bin/bash
# Build script for Linux installers using jpackage
# Creates both .deb (Debian/Ubuntu) and .rpm (Fedora/RHEL/CentOS) packages

set -e

echo "=== Building Linux Installers for SketchApp ==="

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
echo "Ensuring icon is ready..."
if command -v python3 &> /dev/null; then
    python3 convert_icon.py
else
    echo "Warning: Python3 not found. Skipping icon conversion."
fi

# Check for .deb requirements
if command -v dpkg-deb &> /dev/null; then
    DEB_AVAILABLE=true
else
    echo "Warning: dpkg-deb not found. .deb package cannot be built."
    DEB_AVAILABLE=false
fi

# Check for .rpm requirements
if command -v rpmbuild &> /dev/null; then
    RPM_AVAILABLE=true
else
    echo "Warning: rpmbuild not found. .rpm package cannot be built."
    RPM_AVAILABLE=false
fi

if [ "$DEB_AVAILABLE" = false ] && [ "$RPM_AVAILABLE" = false ]; then
    echo "Error: Neither .deb nor .rpm tools are available. Cannot build Linux packages."
    exit 1
fi

echo ""

# Staging directory for jpackage input
STAGING_DIR="staging"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
cp "$JAR_FILE" "$STAGING_DIR/"
cp "../resources/sketchapp-icon.png" "$STAGING_DIR/" || true

echo "✓ Prepared staging directory with JAR and dependencies"

# Build .deb package
if [ "$DEB_AVAILABLE" = true ]; then
    echo "=== Building .deb package (Debian/Ubuntu) ==="
    
    jpackage \
      --type deb \
      --input "$STAGING_DIR" \
      --name "sketchapp" \
      --main-jar SketchApp.jar \
      --main-class "$MAIN_CLASS" \
      --app-version "$APP_VERSION" \
      --vendor "SketchApp Team" \
      --description "Professional CAD Sketching Application" \
      --icon "../resources/sketchapp-icon.png" \
      --dest "$OUTPUT_DIR" \
      --linux-shortcut \
      --linux-menu-group "Graphics;Engineering;" \
      --linux-app-category "Graphics" \
      --java-options "-Xmx2048m" \
      --java-options "-Dsun.java2d.opengl=true" \
      --arguments "--gui"
    
    echo "✓ .deb package created in $OUTPUT_DIR"
    echo ""
fi

# Build .rpm package
if [ "$RPM_AVAILABLE" = true ]; then
    echo "=== Building .rpm package (Fedora/RHEL/CentOS) ==="
    
    jpackage \
      --type rpm \
      --input "$STAGING_DIR" \
      --name "sketchapp" \
      --main-jar SketchApp.jar \
      --main-class "$MAIN_CLASS" \
      --app-version "$APP_VERSION" \
      --vendor "SketchApp Team" \
      --description "Professional CAD Sketching Application" \
      --icon "../resources/sketchapp-icon.png" \
      --dest "$OUTPUT_DIR" \
      --linux-shortcut \
      --linux-menu-group "Graphics;Engineering;" \
      --linux-app-category "Graphics" \
      --java-options "-Xmx2048m" \
      --java-options "-Dsun.java2d.opengl=true" \
      --arguments "--gui"
    
    echo "✓ .rpm package created in $OUTPUT_DIR"
    echo ""
fi

# Cleanup
rm -rf "$STAGING_DIR"

echo "=== Build Complete ==="
echo "Installers are located in: $OUTPUT_DIR"
