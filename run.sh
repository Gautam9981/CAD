#!/bin/bash

# ----------------------
# SketchApp Launcher (JOGL + JavaFX native support)
# Uses: java -jar SketchApp.jar with proper native paths
# ----------------------

set -e

# Detect platform and architecture
OS="$(uname -s)"
ARCH="$(uname -m)"

# Normalize architecture
case "$ARCH" in
  x86_64|amd64)
    FX_ARCH="x64"
    ;;
  arm64|aarch64)
    FX_ARCH="aarch64"
    ;;
  *)
    echo "Unsupported architecture: $ARCH"
    exit 1
    ;;
esac

# Determine paths
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# Set JavaFX and JOGL native paths
case "$OS" in
  Darwin)
    FX_NATIVE="$BASE_DIR/javafx/natives/osx-$FX_ARCH"
    JOGL_NATIVE="$BASE_DIR/lib/macosx-universal"
    ;;
  Linux)
    if [[ "$FX_ARCH" != "x64" ]]; then
      echo "Only Linux x86_64 is supported"
      exit 1
    fi
    FX_NATIVE="$BASE_DIR/javafx/natives/linux-x64"
    JOGL_NATIVE="$BASE_DIR/lib/linux-amd64"
    ;;
  *)
    echo "Unsupported OS: $OS"
    exit 1
    ;;
esac

# Combine native library paths
LIB_PATH="$FX_NATIVE:$JOGL_NATIVE"

# JavaFX module path
MODULE_PATH="$BASE_DIR/javafx/shared-lib/javafx.base.jar:$BASE_DIR/javafx/shared-lib/javafx.controls.jar:$BASE_DIR/javafx/shared-lib/javafx.fxml.jar:$BASE_DIR/javafx/shared-lib/javafx.graphics.jar:$BASE_DIR/javafx/shared-lib/javafx.media.jar:$BASE_DIR/javafx/shared-lib/javafx.swing.jar:$BASE_DIR/javafx/shared-lib/javafx.web.jar"

exec java -Djava.library.path="$LIB_PATH" --module-path "$MODULE_PATH" --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.media -jar "$BASE_DIR/SketchApp.jar"
