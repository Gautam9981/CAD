package cad.core;

import com.jogamp.opengl.GL2;

public class RadialDimension extends Dimension {
    private float centerX, centerY;
    private float radius;
    private boolean showDiameter;
    private float angle = 45.0f;

    public RadialDimension(float centerX, float centerY, float radius, boolean showDiameter, String unit) {
        super(showDiameter ? DimensionType.DIAMETER : DimensionType.RADIAL,
                showDiameter ? radius * 2 : radius, unit);
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        this.showDiameter = showDiameter;
        calculatePosition();
    }

    public RadialDimension(cad.core.Sketch.Arc arc, boolean showDiameter, String unit) {
        super(showDiameter ? DimensionType.DIAMETER : DimensionType.RADIAL,
                showDiameter ? arc.getRadius() * 2 : arc.getRadius(), unit);
        cad.core.Point center = arc.getCenterPoint().getPoint();
        this.centerX = center.x;
        this.centerY = center.y;
        this.radius = arc.getRadius();
        this.showDiameter = showDiameter;
        calculatePosition();
    }

    @Override
    public void calculatePosition() {

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
        gl.glColor3f(0.3f, 0.3f, 0.3f);
        gl.glLineWidth(1.0f);

        double angleRad = Math.toRadians(angle);
        float endX = centerX + (float) (Math.cos(angleRad) * radius);
        float endY = centerY + (float) (Math.sin(angleRad) * radius);

        float dirX = (float) Math.cos(angleRad);
        float dirY = (float) Math.sin(angleRad);

        gl.glLineWidth(1.2f);
        gl.glBegin(GL2.GL_LINES);
        if (showDiameter) {

            float startX = centerX - (float) (Math.cos(angleRad) * radius);
            float startY = centerY - (float) (Math.sin(angleRad) * radius);
            gl.glVertex2f(startX, startY);
            gl.glVertex2f(endX, endY);
        } else {

            gl.glVertex2f(centerX, centerY);
            gl.glVertex2f(endX, endY);
        }
        gl.glEnd();

        float arrowLength = 0.8f;
        float arrowWidth = 0.3f;
        drawProfessionalArrow(gl, endX, endY, dirX, dirY, arrowLength, arrowWidth);

        float markSize = 0.4f;
        gl.glLineWidth(1.0f);
        gl.glBegin(GL2.GL_LINES);

        gl.glVertex2f(centerX - markSize, centerY);
        gl.glVertex2f(centerX + markSize, centerY);

        gl.glVertex2f(centerX, centerY - markSize);
        gl.glVertex2f(centerX, centerY + markSize);
        gl.glEnd();

        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private void drawProfessionalArrow(GL2 gl, float x, float y, float dirX, float dirY,
            float length, float width) {

        float perpX = -dirY * width;
        float perpY = dirX * width;

        float baseX = x - dirX * length;
        float baseY = y - dirY * length;

        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glVertex2f(x, y);
        gl.glVertex2f(baseX + perpX, baseY + perpY);
        gl.glVertex2f(baseX - perpX, baseY - perpY);
        gl.glEnd();
    }

    @Override
    public String getLabel() {

        String prefix = showDiameter ? "Ø" : "R";
        return prefix + formatValue(value) + " " + unitAbbreviation;
    }

    @Override
    public boolean contains(float x, float y, float tolerance) {

        double dist = Math.sqrt(Math.pow(x - textX, 2) + Math.pow(y - textY, 2));
        return dist < (tolerance * 10.0f);
    }

    public void setAngle(float angle) {
        this.angle = angle % 360;
        calculatePosition();
    }
}
