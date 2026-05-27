#!/bin/bash

# Ensure we are in the Binaries directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

ARCH=${1:-$(uname -m)}
VERSION="5.1.0"
APP_NAME="SketchApp"
MAIN_CLASS="cad.Main"
MAIN_JAR="SketchApp.jar"
INPUT_DIR="../target"
DEST_DIR="installer"

mkdir -p "$DEST_DIR"

echo "Building macOS Installer (.dmg) for architecture: $ARCH..."
jpackage \
  --type dmg \
  --dest "$DEST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --main-class "$MAIN_CLASS" \
  --main-jar "$MAIN_JAR" \
  --app-version "$VERSION" \
  --icon "assets/logo.icns" \
  --mac-package-name "$APP_NAME"

echo "macOS Build Complete! Image is located in $DEST_DIR/"
