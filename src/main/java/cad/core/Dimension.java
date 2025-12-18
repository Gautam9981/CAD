package cad.core;

import com.jogamp.opengl.GL2;


public abstract class Dimension {
    
    public enum DimensionType {
        LINEAR,          // Line length, distance between points
        RADIAL,          // Circle radius
        DIAMETER,        // Circle diameter
        ANGULAR,         // Angle between entities
        COORDINATE,      // X, Y, or Z position
        AREA,            // Face area (3D)
        VOLUME           // Solid volume (3D)
    }
    
    protected DimensionType type;
    protected double value;
    protected String label;
    protected float textX, textY, textZ; // Position for dimension text (world coordinates)
    protected boolean visible = true;
    protected String unitAbbreviation = "mm"; // Default unit
    
    
    public Dimension(DimensionType type, double value, String unit) {
        this.type = type;
        this.value = value;
        this.unitAbbreviation = unit;
        updateLabel();
    }
    
    
    public abstract void draw(GL2 gl);
    
    
    public abstract void calculatePosition();

    public abstract boolean contains(float x, float y, float tolerance);
    
    
    protected String formatValue(double val) {
        // Format with appropriate precision based on magnitude
        if (Math.abs(val) < 0.01) {
            return String.format("%.4f", val);
        } else if (Math.abs(val) < 1.0) {
            return String.format("%.3f", val);
        } else if (Math.abs(val) < 100.0) {
            return String.format("%.2f", val);
        } else {
            return String.format("%.1f", val);
        }
    }
    
    
    protected void updateLabel() {
        this.label = formatValue(value) + " " + unitAbbreviation;
    }
    
    
    public void setUnit(String unit) {
        this.unitAbbreviation = unit;
        updateLabel();
    }
    
    // Getters and setters
    
    public DimensionType getType() {
        return type;
    }
    
    public double getValue() {
        return value;
    }
    
    public String getLabel() {
        return label;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public float getTextX() {
        return textX;
    }
    
    public float getTextY() {
        return textY;
    }
    
    public float getTextZ() {
        return textZ;
    }
}
