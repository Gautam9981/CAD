#!/bin/bash

# Ensure we are in the Binaries directory
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIR"

OS=$(uname -s)
ARCH=$(uname -m)

echo "Detected OS: $OS"
echo "Detected Architecture: $ARCH"

if [[ "$OS" == "Linux" ]]; then
    bash build-linux.sh "$ARCH"
elif [[ "$OS" == "Darwin" ]]; then
    bash build-macos.sh "$ARCH"
elif [[ "$OS" == *"MINGW"* ]] || [[ "$OS" == *"CYGWIN"* ]] || [[ "$OS" == *"MSYS"* ]]; then
    bash build-windows.sh
else
    echo "Unsupported OS: $OS"
    exit 1
fi
