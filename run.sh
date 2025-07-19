#!/bin/bash

# ----------------------
# SketchApp Launcher (JOGL + JavaFX native support)
# Uses: java -jar SketchApp.jar with proper native paths
# ----------------------

set -e

echo "Starting SketchApp..."
echo "Detected OS: $(uname -s), Architecture: $(uname -m)"

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
    echo "macOS detected - using native libs: $FX_NATIVE"
    ;;
  Linux)
    if [[ "$FX_ARCH" != "x64" ]]; then
      echo "Only Linux x86_64 is supported"
      exit 1
    fi
    FX_NATIVE="$BASE_DIR/javafx/natives/linux-x64"
    JOGL_NATIVE="$BASE_DIR/lib/linux-amd64"
    echo "Linux detected - using native libs: $FX_NATIVE"
    ;;
  *)
    echo "Unsupported OS: $OS"
    exit 1
    ;;
esac

# Verify native libraries exist
if [[ ! -d "$FX_NATIVE" ]]; then
    echo "Warning: JavaFX native directory not found: $FX_NATIVE"
fi

if [[ ! -d "$JOGL_NATIVE" ]]; then
    echo "Warning: JOGL native directory not found: $JOGL_NATIVE"
fi

# Combine native library paths
LIB_PATH="$FX_NATIVE:$JOGL_NATIVE"

# Platform-specific execution
case "$OS" in
  Darwin)
    # macOS specific JVM arguments and execution
    echo "Launching on macOS with native libraries..."
    MACOS_ARGS="-XstartOnFirstThread -Djava.awt.headless=false -Dprism.order=sw"
    
    # Try to run with external JavaFX first, fallback to embedded
    if [[ -d "$BASE_DIR/javafx/shared-lib" ]]; then
        # Use external JavaFX libraries
        CLASSPATH="$BASE_DIR/javafx/shared-lib/*:$BASE_DIR/SketchApp.jar"
        exec java $MACOS_ARGS -Djava.library.path="$LIB_PATH" -cp "$CLASSPATH" -Djava.awt.headless=false com.example.SketchApp 2>/dev/null || \
        exec java $MACOS_ARGS -Djava.library.path="$LIB_PATH" -cp "$CLASSPATH" Main 2>/dev/null || \
        exec java $MACOS_ARGS -Djava.library.path="$LIB_PATH" -jar "$BASE_DIR/SketchApp.jar"
    else
        # Fallback to embedded JavaFX
        exec java $MACOS_ARGS -Djava.library.path="$LIB_PATH" -jar "$BASE_DIR/SketchApp.jar"
    fi
    ;;
  Linux)
    # Linux - simple execution (works perfectly)
    echo "Launching on Linux..."
    exec java -Djava.library.path="$LIB_PATH" -jar "$BASE_DIR/SketchApp.jar"
    ;;
  *)
    echo "Unsupported OS: $OS"
    exit 1
    ;;
esac
