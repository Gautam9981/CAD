#!/bin/bash
# Master build script - builds installers for the current platform only
# For cross-platform builds, use GitHub Actions or run on each platform individually

set -e

echo "=== SketchApp Multi-Platform Build Script ==="
echo ""

# Detect platform
OS_TYPE=$(uname -s)
echo "Detected platform: $OS_TYPE"
echo ""

# Check if JAR exists
JAR_FILE="../target/SketchApp.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "Error: JAR file not found at $JAR_FILE"
    echo "Building JAR with Maven..."
    cd ..
    mvn clean package -DskipTests
    cd Binaries
    echo ""
fi

# Build for current platform
case "$OS_TYPE" in
    Linux*)
        echo "Building Linux installers (.deb and .rpm)..."
        ./build-linux.sh
        ;;
    Darwin*)
        echo "Building macOS .dmg installer..."
        ./build-macos.sh
        ;;
    MINGW*|MSYS*|CYGWIN*)
        echo "Building Windows .exe installer..."
        ./build-windows.sh
        ;;
    *)
        echo "Error: Unsupported platform: $OS_TYPE"
        echo ""
        echo "Supported platforms:"
        echo "  - Linux (for .deb and .rpm packages)"
        echo "  - Darwin/macOS (for .dmg packages)"
        echo "  - Windows (for .exe installers)"
        echo ""
        echo "For cross-platform builds, use GitHub Actions:"
        echo "  1. Push code to GitHub"
        echo "  2. Create a tag: git tag v4.0.0 && git push origin v4.0.0"
        echo "  3. GitHub Actions will build all platforms automatically"
        exit 1
        ;;
esac

echo ""
echo "=== Build Complete ==="
echo "Installer created in current directory"
