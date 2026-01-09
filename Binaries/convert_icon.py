from PIL import Image
import os
import platform

# Paths - Updated to standard Maven structure
SOURCE_IMG = '../src/main/resources/sketchapp-icon.png'
ICO_PATH = '../src/main/resources/sketchapp-icon.ico'
ICNS_PATH = '../src/main/resources/sketchapp-icon.icns'

def convert_icons():
    if not os.path.exists(SOURCE_IMG):
        print(f"Error: {SOURCE_IMG} not found")
        return

    img = Image.open(SOURCE_IMG)
    system = platform.system()
    print(f"Detected OS: {system}")

    if system == 'Windows':
        print(f"Generating {ICO_PATH} for Windows...")
        img.save(ICO_PATH, format='ICO')
        print(f"Generated {ICO_PATH}")
    
    elif system == 'Darwin':
        print(f"Generating {ICNS_PATH} for macOS...")
        try:
            img.save(ICNS_PATH, format='ICNS')
            print(f"Generated {ICNS_PATH}")
        except Exception as e:
            print(f"Error generating ICNS: {e}")
            print("Note: Basic PIL ICNS support might be limited.")
    
    else:
        print(f"No icon conversion needed for {system} (uses PNG natively).")

if __name__ == "__main__":
    convert_icons()
