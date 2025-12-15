from PIL import Image
import os
import platform

img_path = '../resources/sketchapp-icon.png'
ico_path = '../resources/sketchapp-icon.ico'
icns_path = '../resources/sketchapp-icon.icns'

def convert_icons():
    if not os.path.exists(img_path):
        print(f"Error: {img_path} not found")
        return

    img = Image.open(img_path)

    # Convert to ICO (Windows)
    if not os.path.exists(ico_path):
        img.save(ico_path, format='ICO')
        print(f"Generated {ico_path}")
    
    # Convert to ICNS (MacOS) - Requires Mac or external tools usually, but PIL might handle saving if supported
    # We will try to save it so it is ready for the Mac build even if generated on Windows.
    if not os.path.exists(icns_path):
        try:
            # For robust ICNS, we usually need iconutil. 
            # But let's try basic save first or warn user.
            img.save(icns_path, format='ICNS')
            print(f"Generated {icns_path}")
        except Exception as e:
            print(f"Could not generate ICNS directly: {e}")
            print("Please run 'mkdir sketchapp.iconset' and use 'iconutil -c icns sketchapp.iconset' on macOS.")

if __name__ == "__main__":
    convert_icons()
