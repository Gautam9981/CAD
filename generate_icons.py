from PIL import Image
import os

def create_icons():
    base_img = Image.open('Binaries/assets/logo.png')
    
    # Generate multi-size ICO for Windows
    # Windows standard sizes: 16, 24, 32, 48, 64, 128, 256
    ico_sizes = [(16,16), (24,24), (32,32), (48,48), (64,64), (128,128), (256,256)]
    base_img.save('Binaries/assets/logo.ico', format='ICO', sizes=ico_sizes)
    
    # Generate multi-size ICNS for macOS
    # Pillow handles ICNS sizes automatically if the source is large enough (e.g. 1024x1024)
    base_img.save('Binaries/assets/logo.icns', format='ICNS')

if __name__ == '__main__':
    create_icons()
    print("Icons generated successfully with clean scaling.")
