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

# Optional Architecture Argument
ARCH=${1:-""}
INSTALLER_NAME="$APP_NAME"
if [ -n "$ARCH" ]; then
    INSTALLER_NAME="$APP_NAME-$ARCH"
    echo "Building for Architecture: $ARCH (Installer: $INSTALLER_NAME)"
fi

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
# Step 1: Create App Image
echo "Generating App Image..."
jpackage \
  --type app-image \
  --input "$STAGING_DIR" \
  --name "$APP_NAME" \
  --main-jar SketchApp.jar \
  --main-class "$MAIN_CLASS" \
  --app-version "$APP_VERSION" \
  --vendor "SketchApp Team" \
  --description "Professional CAD Sketching Application" \
  --icon "$STAGING_DIR/sketchapp-icon.icns" \
  --dest "$OUTPUT_DIR/app-image" \
  --mac-package-name "SketchApp" \
  --java-options "-Xmx2048m" \
  --java-options "-Dsun.java2d.opengl=true" \
  --java-options "-XstartOnFirstThread" \
  --arguments "--gui"

# Step 2: Modify Info.plist to support older macOS versions (11.0+)
INFO_PLIST="$OUTPUT_DIR/app-image/$APP_NAME.app/Contents/Info.plist"
if [ -f "$INFO_PLIST" ]; then
    echo "Updating LSMinimumSystemVersion in Info.plist to 11.0..."
    # Use plytool or sed. sed is safer to assume presence.
    # We replace the value if it exists, or insert it.
    # jpackage usually adds LSMinimumSystemVersion, so we verify and replace.
    echo "Updating LSMinimumSystemVersion in Info.plist to 11.0..."
    # Robustly set LSMinimumSystemVersion using PlistBuddy
    # 1. Try to delete the key first to avoid type conflicts or errors if it exists
    /usr/libexec/PlistBuddy -c "Delete :LSMinimumSystemVersion" "$INFO_PLIST" || true
    # 2. Add the key as a string with value "11.0"
    /usr/libexec/PlistBuddy -c "Add :LSMinimumSystemVersion string 11.0" "$INFO_PLIST"
    
    echo "Info.plist updated:"
    grep -A 1 "LSMinimumSystemVersion" "$INFO_PLIST" || echo "Reference verified via PlistBuddy"
else
    echo "Warning: Info.plist not found at $INFO_PLIST"
fi

# Step 3: Create DMG from App Image
echo "Creating macOS DMG installer from App Image..."
jpackage \
  --type dmg \
  --app-image "$OUTPUT_DIR/app-image/$APP_NAME.app" \
  --name "$INSTALLER_NAME" \
  --app-version "$APP_VERSION" \
  --vendor "SketchApp Team" \
  --dest "$OUTPUT_DIR"

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
