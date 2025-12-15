package cad.core;

import com.jogamp.opengl.GL2;

/**
 * Abstract base class for all dimension types (linear, radial, angular, etc.)
 * Dimensions are annotations that display measurements for sketch entities and 3D models.
 */
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
    
    /**
     * Constructor
     * @param type The type of dimension
     * @param value The measured value
     * @param unit The unit abbreviation (e.g., "mm", "in")
     */
    public Dimension(DimensionType type, double value, String unit) {
        this.type = type;
        this.value = value;
        this.unitAbbreviation = unit;
        updateLabel();
    }
    
    /**
     * Abstract method to draw the dimension lines, arrows, and geometry.
     * Subclasses implement their specific drawing logic.
     * @param gl OpenGL context
     */
    public abstract void draw(GL2 gl);
    
    /**
     * Abstract method to calculate the text position based on dimension geometry.
     * Called when dimension is created or entities are modified.
     */
    public abstract void calculatePosition();
    
    /**
     * Formats the numeric value with appropriate precision.
     * @param val The value to format
     * @return Formatted string
     */
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
    
    /**
     * Updates the label with current value and unit.
     */
    protected void updateLabel() {
        this.label = formatValue(value) + " " + unitAbbreviation;
    }
    
    /**
     * Sets the unit for this dimension.
     * @param unit Unit abbreviation (e.g., "mm", "in")
     */
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
