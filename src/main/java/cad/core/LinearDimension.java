package cad.core;

import com.jogamp.opengl.GL2;

/**
 * Linear dimension for measuring distances between two points or line lengths.
 * Renders dimension line parallel to the measured entity with extension lines and arrows.
 */
public class LinearDimension extends Dimension {
    private float x1, y1, z1; // Start point
    private float x2, y2, z2; // End point
    private float offsetDistance = 2.0f; // Distance from entity to dimension line
    private boolean is3D = false;
    
    /**
     * 2D Constructor for sketch entities
     * @param unit Unit abbreviation for display
     */
    public LinearDimension(float x1, float y1, float x2, float y2, String unit) {
        super(DimensionType.LINEAR, calculateLength(x1, y1, 0, x2, y2, 0), unit);
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = 0;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = 0;
        this.is3D = false;
        calculatePosition();
    }
    
    /**
     * 3D Constructor for model entities
     * @param unit Unit abbreviation for display
     */
    public LinearDimension(float x1, float y1, float z1, float x2, float y2, float z2, String unit) {
        super(DimensionType.LINEAR, calculateLength(x1, y1, z1, x2, y2, z2), unit);
        this.x1 = x1;
        this.y1 = y1;
        this.z1 = z1;
        this.x2 = x2;
        this.y2 = y2;
        this.z2 = z2;
        this.is3D = true;
        calculatePosition();
    }
    
    /**
     * Calculate Euclidean distance between two points
     */
    private static double calculateLength(float x1, float y1, float z1, 
                                         float x2, float y2, float z2) {
        return Math.sqrt(
            Math.pow(x2 - x1, 2) + 
            Math.pow(y2 - y1, 2) + 
            Math.pow(z2 - z1, 2)
        );
    }
    
    @Override
    public void calculatePosition() {
        // Position text at midpoint, offset perpendicular to line
        textX = (x1 + x2) / 2.0f;
        textY = (y1 + y2) / 2.0f;
        textZ = (z1 + z2) / 2.0f;
        
        // Calculate perpendicular offset direction
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) value;
        
        if (len > 0) {
            // Perpendicular vector (rotate 90 degrees in 2D)
            float perpX = -dy / len;
            float perpY = dx / len;
            
            textX += perpX * offsetDistance;
            textY += perpY * offsetDistance;
        }
    }
    
    @Override
    public void draw(GL2 gl) {
        if (!visible) return;
        
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.3f, 0.3f, 0.3f); // Professional dark gray (SolidWorks style)
        
        // Calculate direction and perpendicular vectors
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) value;
        
        if (len < 0.001f) return; // Skip degenerate dimensions
        
        // Normalized direction
        float dirX = dx / len;
        float dirY = dy / len;
        
        // Perpendicular vector (for offset)
        float perpX = -dirY * offsetDistance;
        float perpY = dirX * offsetDistance;
        
        // Dimension line endpoints (offset from entity)
        float lineX1 = x1 + perpX;
        float lineY1 = y1 + perpY;
        float lineX2 = x2 + perpX;
        float lineY2 = y2 + perpY;
        
        // Extension/Witness line parameters
        float extensionGap = 0.3f;      // Gap between entity and extension line start
        float extensionOverhang = 1.0f;  // Extension beyond dimension line (SolidWorks style)
        
        gl.glLineWidth(1.0f); // Thin lines for professional look
        
        // Draw witness/extension lines (extend beyond dimension line)
        gl.glBegin(GL2.GL_LINES);
        // Extension from point 1 (starts with gap, extends beyond)
        float ext1StartX = x1 + perpX * (extensionGap / offsetDistance);
        float ext1StartY = y1 + perpY * (extensionGap / offsetDistance);
        float ext1EndX = lineX1 + perpX * (extensionOverhang / offsetDistance);
        float ext1EndY = lineY1 + perpY * (extensionOverhang / offsetDistance);
        gl.glVertex2f(ext1StartX, ext1StartY);
        gl.glVertex2f(ext1EndX, ext1EndY);
        
        // Extension from point 2
        float ext2StartX = x2 + perpX * (extensionGap / offsetDistance);
        float ext2StartY = y2 + perpY * (extensionGap / offsetDistance);
        float ext2EndX = lineX2 + perpX * (extensionOverhang / offsetDistance);
        float ext2EndY = lineY2 + perpY * (extensionOverhang / offsetDistance);
        gl.glVertex2f(ext2StartX, ext2StartY);
        gl.glVertex2f(ext2EndX, ext2EndY);
        gl.glEnd();
        
        // Draw dimension line WITH GAP FOR TEXT (SolidWorks style)
        // Calculate text position (midpoint)
        float midX = (lineX1 + lineX2) / 2.0f;
        float midY = (lineY1 + lineY2) / 2.0f;
        
        // Estimate text width (rough approximation)
        float textGap = Math.max(2.0f, (float) value * 0.15f); // Proportional to value
        
        // Draw dimension line in two segments (with gap for text)
        gl.glLineWidth(1.2f); // Slightly thicker for dimension line
        gl.glBegin(GL2.GL_LINES);
        // Left segment
        gl.glVertex2f(lineX1, lineY1);
        gl.glVertex2f(midX - dirX * textGap, midY - dirY * textGap);
        // Right segment  
        gl.glVertex2f(midX + dirX * textGap, midY + dirY * textGap);
        gl.glVertex2f(lineX2, lineY2);
        gl.glEnd();
        
        // Draw professional arrows (filled, proper proportions)
        float arrowLength = 0.8f;   // Arrow length (SolidWorks proportion)
        float arrowWidth = 0.3f;    // Arrow width
        
        drawProfessionalArrow(gl, lineX1, lineY1, dirX, dirY, arrowLength, arrowWidth);
        drawProfessionalArrow(gl, lineX2, lineY2, -dirX, -dirY, arrowLength, arrowWidth);
        
        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }
    
    /**
     * Draws a filled triangular arrow with professional CAD proportions.
     */
    private void drawProfessionalArrow(GL2 gl, float x, float y, float dirX, float dirY, 
                                      float length, float width) {
        // Perpendicular vector for arrow wings
        float perpX = -dirY * width;
        float perpY = dirX * width;
        
        // Arrow tip at (x, y), base extends in direction
        float baseX = x + dirX * length;
        float baseY = y + dirY * length;
        
        // Draw filled triangle
        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glVertex2f(x, y);                        // Tip
        gl.glVertex2f(baseX + perpX, baseY + perpY); // Wing 1
        gl.glVertex2f(baseX - perpX, baseY - perpY); // Wing 2
        gl.glEnd();
    }
    
    public void setOffsetDistance(float offset) {
        this.offsetDistance = offset;
        calculatePosition();
    }
}
