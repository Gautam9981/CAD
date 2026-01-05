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

# Architecture argument (default to system arch if not provided)
INPUT_ARCH="$1"
if [ -z "$INPUT_ARCH" ]; then
    INPUT_ARCH=$(uname -m)
    if [ "$INPUT_ARCH" == "x86_64" ]; then
        INPUT_ARCH="x64"
    elif [ "$INPUT_ARCH" == "aarch64" ]; then
        INPUT_ARCH="arm64"
    fi
fi

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
    echo "Info: Neither .deb nor .rpm tools are available. Will only build generic .tar.gz."
fi

echo ""

# Staging directory for jpackage input
STAGING_DIR="staging"
rm -rf "$STAGING_DIR"
mkdir -p "$STAGING_DIR"
cp "$JAR_FILE" "$STAGING_DIR/"
cp "../src/main/resources/sketchapp-icon.png" "$STAGING_DIR/" || true
cp "../materials.json" "$STAGING_DIR/" || echo "Warning: materials.json not found, will use defaults"

echo "✓ Prepared staging directory with JAR and dependencies"

# Build Generic App Image & .tar.gz
echo "=== Building Generic Linux Package (.tar.gz) ==="
# Create App Image first
jpackage \
  --type app-image \
  --input "$STAGING_DIR" \
  --name "SketchApp" \
  --main-jar SketchApp.jar \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --vendor "SketchApp Team" \
  --description "Professional CAD Sketching Application" \
  --icon "$STAGING_DIR/sketchapp-icon.png" \
  --dest "$OUTPUT_DIR/generic" \
  --java-options "-Xmx2048m" \
  --java-options "-Dsun.java2d.opengl=true" \
  --arguments "--gui"

# Compress to .tar.gz
# Compress to .tar.gz
echo "Compressing to .tar.gz..."
cd "$OUTPUT_DIR/generic"
TAR_NAME="${APP_NAME}-Linux-${INPUT_ARCH}-${APP_VERSION}.tar.gz"
tar -czf "../${TAR_NAME}" "SketchApp"
cd - > /dev/null
echo "✓ Generic .tar.gz package created: $OUTPUT_DIR/$TAR_NAME"
echo ""

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
      --icon "$STAGING_DIR/sketchapp-icon.png" \
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
      --icon "$STAGING_DIR/sketchapp-icon.png" \
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
