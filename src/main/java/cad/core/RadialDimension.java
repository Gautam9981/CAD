package cad.core;

import com.jogamp.opengl.GL2;

public class RadialDimension extends Dimension {
    private float centerX, centerY;
    private float radius;
    private boolean showDiameter; // true = diameter (Ø), false = radius (R)
    private float angle = 45.0f; // Angle for dimension line placement (degrees)

    public RadialDimension(float centerX, float centerY, float radius, boolean showDiameter, String unit) {
        super(showDiameter ? DimensionType.DIAMETER : DimensionType.RADIAL,
                showDiameter ? radius * 2 : radius, unit);
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.showDiameter = showDiameter;
        calculatePosition();
    }

    @Override
    public void calculatePosition() {
        // Position text along radius at specified angle
        double angleRad = Math.toRadians(angle);
        textX = centerX + (float) (Math.cos(angleRad) * radius * 0.7);
        textY = centerY + (float) (Math.sin(angleRad) * radius * 0.7);
        textZ = 0;
    }

    @Override
    public void draw(GL2 gl) {
        if (!visible)
            return;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.3f, 0.3f, 0.3f); // Professional dark gray
        gl.glLineWidth(1.0f);

        double angleRad = Math.toRadians(angle);
        float endX = centerX + (float) (Math.cos(angleRad) * radius);
        float endY = centerY + (float) (Math.sin(angleRad) * radius);

        // Direction vector
        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        // Draw radius/diameter line
        gl.glLineWidth(1.2f); // Slightly thicker for main dimension line
        gl.glBegin(GL2.GL_LINES);
        if (showDiameter) {
            // Draw full diameter line through center
            float startX = centerX - (float) (Math.cos(angleRad) * radius);
            float startY = centerY - (float) (Math.sin(angleRad) * radius);
            gl.glVertex2f(startX, startY);
            gl.glVertex2f(endX, endY);
        } else {
            // Draw radius from center to edge
            gl.glVertex2f(centerX, centerY);
            gl.glVertex2f(endX, endY);
        }
        gl.glEnd();

        // Draw professional arrow at end
        float arrowLength = 0.8f;
        float arrowWidth = 0.3f;
        drawProfessionalArrow(gl, endX, endY, dirX, dirY, arrowLength, arrowWidth);

        // Draw center mark (professional crosshair style)
        float markSize = 0.4f;
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINES);
        // Horizontal line
        gl.glVertex2f(centerX - markSize, centerY);
        gl.glVertex2f(centerX + markSize, centerY);
        // Vertical line
        gl.glVertex2f(centerX, centerY - markSize);
        gl.glVertex2f(centerX, centerY + markSize);
        gl.glEnd();

        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private void drawProfessionalArrow(GL2 gl, float x, float y, float dirX, float dirY,
            float length, float width) {
        // Perpendicular vector for arrow wings
        float perpX = -dirY * width;
        float perpY = dirX * width;

        // Arrow tip at (x, y), pointing outward from circle
        float baseX = x - dirX * length; // Base is inside the circle
        float baseY = y - dirY * length;

        // Draw filled triangle
        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glVertex2f(x, y); // Tip (at circle edge)
        gl.glVertex2f(baseX + perpX, baseY + perpY); // Wing 1
        gl.glVertex2f(baseX - perpX, baseY - perpY); // Wing 2
        gl.glEnd();
    }

    @Override
    public String getLabel() {
        // Use standard CAD symbols: Ø for diameter, R for radius
        String prefix = showDiameter ? "Ø" : "R";
        return prefix + formatValue(value) + " " + unitAbbreviation;
    }

    @Override
    public boolean contains(float x, float y, float tolerance) {
        // Simple hit test against the text position
        double dist = Math.sqrt(Math.pow(x - textX, 2) + Math.pow(y - textY, 2));
        return dist < (tolerance * 10.0f);
    }

    public void setAngle(float angle) {
        this.angle = angle % 360;
        calculatePosition();
    }
}
