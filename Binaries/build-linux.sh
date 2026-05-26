#!/bin/bash

# Ensure we are in the Binaries directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

ARCH=${1:-$(uname -m)}
if [ "$ARCH" = "x86_64" ]; then ARCH="x64"; fi
if [ "$ARCH" = "aarch64" ]; then ARCH="arm64"; fi

VERSION="5.1.0"
APP_NAME="sketchapp"
MAIN_CLASS="cad.Main"
MAIN_JAR="SketchApp.jar"
INPUT_DIR="../target"
DEST_DIR="installer"

mkdir -p "$DEST_DIR"

echo "Building Linux Installers for architecture: $ARCH..."

# 1. Build .deb
echo "Building .deb package..."
jpackage \
  --type deb \
  --dest "$DEST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --main-class "$MAIN_CLASS" \
  --main-jar "$MAIN_JAR" \
  --app-version "$VERSION" \
  --linux-shortcut \
  --linux-menu-group "Graphics"

# 2. Build .rpm
echo "Building .rpm package..."
jpackage \
  --type rpm \
  --dest "$DEST_DIR" \
  --input "$INPUT_DIR" \
  --name "$APP_NAME" \
  --main-class "$MAIN_CLASS" \
  --main-jar "$MAIN_JAR" \
  --app-version "$VERSION" \
  --linux-shortcut \
  --linux-menu-group "Graphics"

# 3. Build portable .tar.gz
echo "Building .tar.gz portable archive..."
TAR_DIR="SketchApp-Linux-$ARCH"
mkdir -p "$TAR_DIR"
cp "$INPUT_DIR/$MAIN_JAR" "$TAR_DIR/"
cat << 'EOF' > "$TAR_DIR/sketchapp.sh"
#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -jar "$DIR/SketchApp.jar" "$@"
EOF
chmod +x "$TAR_DIR/sketchapp.sh"
tar -czvf "$DEST_DIR/SketchApp-Linux-$ARCH.tar.gz" "$TAR_DIR/"
rm -rf "$TAR_DIR"

echo "Linux Build Complete! Packages are located in $DEST_DIR/"
