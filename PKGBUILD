# Maintainer: Gautam Mahajan <mahajanga801@gmail.com>

pkgname=SketchApp
pkgver=4.0.0
pkgrel=1
pkgdesc="A vector design tool for macOS, now available for Linux (Version 4.0.0)"
arch=('x86_64' 'aarch64')
url="https://github.com/Gautam9981/CAD"
license=('GPL3' 'MIT' 'custom')

# We need 'unzip' to handle the .zip source archives
makedepends=('unzip')

# Architecture-specific sources (Zip files)
source_x86_64=("https://github.com/Gautam9981/CAD/releases/download/v${pkgver}/SketchApp-Linux-x64-tar.zip")
source_aarch64=("https://github.com/Gautam9981/CAD/releases/download/v${pkgver}/SketchApp-Linux-arm64-tar.zip")

# Skip checksums for development
sha256sums_x86_64=("c91008a5fd92c29adecf11e67064b920f698dbc8910ee34c62fe431e00934698")
sha256sums_aarch64=("4fce0ec808d0a2fb52a116e939b03b1c2ace899e26e92dd9108d828ea6392e9a")

prepare() {
  cd "$srcdir"

  # Unzip the .zip file manually to extract the .tar.gz file
  echo "Unzipping the .zip file..."
  unzip -q SketchApp-Linux-x64-tar.zip

  # List contents after unzip to verify it's been extracted
  echo "Contents of $srcdir after unzip:"
  ls -la

  # Now look for the .tar.gz file in the extracted files
  local _tar_file=$(find . -maxdepth 1 -name "SketchApp-Linux-x64-4.0.0.tar.gz" -print -quit)

  if [ -n "$_tar_file" ]; then
    echo "Found nested tarball: $_tar_file. Extracting..."
    tar -xvzf "$_tar_file"  # Extract the .tar.gz file
  else
    echo "Error: No .tar.gz file found inside the .zip."
    ls -la
    return 1
  fi

  # Verify the directory exists after extraction
  if [ ! -d "SketchApp" ]; then
    echo "Error: Directory 'SketchApp' not found after extraction."
    ls -la
    return 1
  fi
}

package() {
  # Define the extracted directory name
  local _app_dir="SketchApp"

  cd "$srcdir/$_app_dir"

  # 1. Create directory structure
  install -d "$pkgdir/usr/bin"
  install -d "$pkgdir/usr/share/sketchapp"
  install -d "$pkgdir/usr/share/pixmaps"
  install -d "$pkgdir/usr/share/applications"

  # 2. Copy application files to /usr/share/sketchapp
  cp -r * "$pkgdir/usr/share/sketchapp/"

  # Ensure the binary is executable
  chmod +x "$pkgdir/usr/share/sketchapp/bin/SketchApp"

  # 3. Symlink binary (Optional convenience, allows typing 'sketchapp' in terminal)
  ln -s "/usr/share/sketchapp/bin/SketchApp" "$pkgdir/usr/bin/sketchapp"

  # 4. Install the icon
  if [ -f "$srcdir/$_app_dir/lib/SketchApp.png" ]; then
      install -m644 "$srcdir/$_app_dir/lib/SketchApp.png" "$pkgdir/usr/share/pixmaps/sketchapp.png"
  fi

  # 5. Create the .desktop file
  # Restored Exec path to /usr/share/sketchapp/bin/SketchApp per original
  cat > "$pkgdir/usr/share/applications/sketchapp.desktop" <<EOF
[Desktop Entry]
Version=${pkgver}
Name=SketchApp
Comment=A vector design tool for macOS, now available for Linux
Exec=/usr/share/sketchapp/bin/SketchApp
Icon=sketchapp
Terminal=false
Type=Application
Categories=Graphics;Design;
MimeType=application/x-sketch;
EOF
}
