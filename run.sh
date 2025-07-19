#!/bin/bash

# ----------------------
# SketchApp Launcher for macOS (JOGL + JavaFX native support)
# macOS-only optimized version
# ----------------------

set -e

echo "Starting SketchApp on macOS..."

# Check if running on macOS
if [[ "$(uname -s)" != "Darwin" ]]; then
    echo "ERROR: This script is designed for macOS only!"
    exit 1
fi

# Detect architecture
ARCH="$(uname -m)"
echo "Detected Architecture: $ARCH"

# Normalize architecture for macOS
case "$ARCH" in
  x86_64|amd64)
    FX_ARCH="x64"
    ;;
  arm64|aarch64)
    FX_ARCH="aarch64"
    ;;
  *)
    echo "Unsupported macOS architecture: $ARCH"
    exit 1
    ;;
esac

# Determine paths
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

# Set macOS native paths
FX_NATIVE="$BASE_DIR/javafx/natives/osx-$FX_ARCH"
JOGL_NATIVE="$BASE_DIR/lib/macosx-universal"

echo "Using JavaFX natives: $FX_NATIVE"
echo "Using JOGL natives: $JOGL_NATIVE"

# Verify native libraries exist
if [[ ! -d "$FX_NATIVE" ]]; then
    echo "ERROR: JavaFX native directory not found: $FX_NATIVE"
    exit 1
fi

if [[ ! -d "$JOGL_NATIVE" ]]; then
    echo "ERROR: JOGL native directory not found: $JOGL_NATIVE"
    exit 1
fi

# Combine native library paths
LIB_PATH="$FX_NATIVE:$JOGL_NATIVE"

# macOS specific JVM arguments
MACOS_ARGS="-XstartOnFirstThread"
MACOS_ARGS="$MACOS_ARGS -Djava.awt.headless=false"
MACOS_ARGS="$MACOS_ARGS -Dprism.order=sw"
MACOS_ARGS="$MACOS_ARGS -Dprism.verbose=true"
MACOS_ARGS="$MACOS_ARGS -Djavafx.animation.fullspeed=true"
MACOS_ARGS="$MACOS_ARGS -Dcom.sun.javafx.isEmbedded=false"

echo "Launching SketchApp with macOS optimizations..."

# Simple JAR execution - no complex classpath manipulation
exec java $MACOS_ARGS -Djava.library.path="$LIB_PATH" -jar "$BASE_DIR/SketchApp.jar"
