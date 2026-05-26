#!/bin/bash

# Ensure we are in the Binaries directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

VERSION="5.0.0"
APP_NAME="SketchApp"
MAIN_CLASS="cad.Main"
MAIN_JAR="SketchApp.jar"
INPUT_DIR="../target"
DEST_DIR="installer"

mkdir -p "$DEST_DIR"

echo "Building Windows Installer (.exe)..."
jpackage \
  --type exe \
  --dest "$DEST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --main-class "$MAIN_CLASS" \
  --main-jar "$MAIN_JAR" \
  --app-version "$VERSION" \
  --win-shortcut \
  --win-menu \
  --win-dir-chooser

echo "Windows Build Complete! Executable is located in $DEST_DIR/"
