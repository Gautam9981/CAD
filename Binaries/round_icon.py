from PIL import Image, ImageDraw, ImageFilter
import numpy as np

def add_rounded_corners(image_path, output_path, radius=50):
    """Add rounded corners to an image"""
    # Open the image
    img = Image.open(image_path).convert("RGBA")
    
    # Create a mask for rounded corners
    mask = Image.new('L', img.size, 0)
    draw = ImageDraw.Draw(mask)
    
    # Draw a rounded rectangle
    draw.rounded_rectangle([(0, 0), img.size], radius=radius, fill=255)
    
    # Apply the mask
    output = Image.new('RGBA', img.size, (0, 0, 0, 0))
    output.paste(img, (0, 0))
    output.putalpha(mask)
    
    # Save
    output.save(output_path, 'PNG')
    print(f"Saved rounded icon to {output_path}")

if __name__ == "__main__":
    add_rounded_corners(
        '../src/main/resources/sketchapp-icon.jpg',
        '../src/main/resources/sketchapp-icon.png',
        radius=150
    )
