package cad.core;

import com.jogamp.opengl.GL2;

public class LinearDimension extends Dimension {

    public enum Alignment {
        ALIGNED, HORIZONTAL, VERTICAL
    }

    private float x1, y1, z1; // Start point
    private float x2, y2, z2; // End point
    private float offsetDistance = 5.0f; // Distance from entity to dimension line
    private boolean is3D = false;
    private Alignment alignment = Alignment.ALIGNED;

    /**
     * Updates the dimension type and position based on mouse cursor text position.
     * Logic mimics SolidWorks Smart Dimension:
     * - Drag off to side -> Vertical
     * - Drag off to top/bottom -> Horizontal
     * - Drag diagonally/perp -> Aligned
     */
    public void updateSmartDimension(float mx, float my) {
        // Calculate bounding box center
        float cx = (x1 + x2) / 2.0f;
        float cy = (y1 + y2) / 2.0f;

        // Vectors
        float dx = Math.abs(mx - cx);
        float dy = Math.abs(my - cy);

        // Angle check or region check
        // Simple logic:
        // If mouse is within the X-span of the points but far in Y -> Horizontal
        // If mouse is within Y-span but far in X -> Vertical
        // Else -> Aligned

        float width = Math.abs(x2 - x1);
        float height = Math.abs(y2 - y1);

        // Thresholds for snapping to Horiz/Vert
        boolean forcesHoriz = (dx < width * 0.8 && dy > height * 0.5);
        boolean forcesVert = (dy < height * 0.8 && dx > width * 0.5);

        // "Cone" logic is better:
        // Angle of mouse vector relative to element vector
        double angleLine = Math.atan2(y2 - y1, x2 - x1);
        double angleMouse = Math.atan2(my - cy, mx - cx);
        double diffAngle = Math.abs(angleMouse - angleLine);
        while (diffAngle > Math.PI)
            diffAngle -= 2 * Math.PI;
        diffAngle = Math.abs(diffAngle);

        // If mostly perpendicular to line -> Aligned
        // If mostly horizontal/vertical relative to screen -> Horiz/Vert

        // Simplified Logic V2 (Robust):
        // 1. Calculate what the dimension VALUE would be for each type
        // 2. See where the user is dragging relative to the feature

        // Determine alignment based on "sectors"
        // 4 Quadrants relative to the line segment center? No.

        // Let's use the SolidWorks logic approximation:
        // If the vector (P1->P2) is roughly vertical, prioritize Vertical/Aligned.
        // If roughly horizontal, prioritize Horizontal/Aligned.

        // Actually, easiest valid UX:
        // Default to Aligned.
        // If Mouse is distinctly outside the "perp limits" of the segment, switch.

        // Let's force Horizontal/Vertical based on dominant mouse offset direction
        // relative to the segment's aspect ratio.

        // Override for simplicity:
        // User moves mostly in Y direction away from line? Horizontal Dim.
        // User moves mostly in X direction away from line? Vertical Dim.

        // We define the "Primary Direction" of the line.
        boolean lineIsMoreVert = height > width;

        if (lineIsMoreVert) {
            // Line is Vertical-ish.
            // Dragging right/left (DX > DY) -> Vertical Dimension (measuring Y-height? No)
            // Vertical Dimension measures Y distance. Extension lines are Horizontal.
            // So if line is vertical, we pull extension lines out horizontally.
            // So if DX is dominant, we are creating a Vertical Dims.
            if (dx > dy) {
                alignment = Alignment.VERTICAL;
            } else {
                alignment = Alignment.ALIGNED;
            }
        } else {
            // Line is Horizontal-ish
            // Dragging up/down (DY > DX) -> Horizontal Dimension (measures X-width? No)
            // Horizontal Dimension measures X distance. Extension lines are Vertical.
            // So dragging Up/Down creates Horizontal Dimension.
            if (dy > dx) {
                alignment = Alignment.HORIZONTAL;
            } else {
                alignment = Alignment.ALIGNED;
            }
        }

        // Also support forced override if user drags WAY out?
        // Let's try Angle-based approach which feels most natural
        // 0 deg = Right, 90 = Up.
        // Horizontal Dim implies text is above/below. Mouse angle near 90/270.
        // Vertical Dim implies text is left/right. Mouse angle near 0/180.

        double absSlope = (dx == 0) ? 999 : Math.abs((my - cy) / (mx - cx));

        // If slope is high (Vertical mouse move) -> Horizontal Dimension
        if (absSlope > 1.5) { // Steep
            alignment = Alignment.HORIZONTAL;
        } else if (absSlope < 0.5) { // Shallow
            alignment = Alignment.VERTICAL;
        } else {
            alignment = Alignment.ALIGNED;
        }

        // Recalculate Value and Position
        if (alignment == Alignment.HORIZONTAL) {
            this.value = Math.abs(x2 - x1);
            // Offset is vertical distance from center
            this.offsetDistance = (my - cy);
            // Correction: offsetDistance logic in 'draw' expects perp distance for Aligned
            // For Axis-Aligned, we handle differently in draw() or map it here.
        } else if (alignment == Alignment.VERTICAL) {
            this.value = Math.abs(y2 - y1);
            this.offsetDistance = (mx - cx);
        } else {
            this.value = calculateLength(x1, y1, z1, x2, y2, z2);
            // Project mouse point onto perpendicular vector to get offset
            float ldx = x2 - x1;
            float ldy = y2 - y1;
            float length = (float) Math.sqrt(ldx * ldx + ldy * ldy);
            float perpX = -ldy / length;
            float perpY = ldx / length;
            // Dot product of (Mouse-Center) and PerpVector
            this.offsetDistance = (mx - cx) * perpX + (my - cy) * perpY;
        }
    }

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

    private static double calculateLength(float x1, float y1, float z1,
            float x2, float y2, float z2) {
        return Math.sqrt(
                Math.pow(x2 - x1, 2) +
                        Math.pow(y2 - y1, 2) +
                        Math.pow(z2 - z1, 2));
    }

    @Override
    public void calculatePosition() {
        // Position text at midpoint, offset perpendicular to line
        float midX = (x1 + x2) / 2.0f;
        float midY = (y1 + y2) / 2.0f;
        float midZ = (z1 + z2) / 2.0f;

        textX = midX;
        textY = midY;
        textZ = midZ;

        // Calculate actual dimension line vector based on alignment
        float dirX = 0, dirY = 0;
        float perpX = 0, perpY = 0;

        if (alignment == Alignment.HORIZONTAL) {
            // Horizontal: Measure X distance
            float dx = x2 - x1;
            // Direction along X
            dirX = (dx >= 0) ? 1.0f : -1.0f;
            dirY = 0.0f;

            // Perpendicular is Y. Offset matches mouse Y direction relative to center
            // offsetDistance stores (My - Cy).
            perpX = 0.0f;
            perpY = 1.0f; // offsetDistance contains the sign/direction

            // Apply perpendicular offset
            textY += offsetDistance;

        } else if (alignment == Alignment.VERTICAL) {
            // Vertical: Measure Y distance
            float dy = y2 - y1;
            // Direction along Y
            dirX = 0.0f;
            dirY = (dy >= 0) ? 1.0f : -1.0f;

            // Perpendicular is X.
            perpX = 1.0f;
            perpY = 0.0f;

            // Apply perpendicular offset
            textX += offsetDistance;

        } else {
            // ALIGNED
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            if (len > 0.0001f) {
                dirX = dx / len;
                dirY = dy / len;
                // Perpendicular
                perpX = -dirY;
                perpY = dirX;

                textX += perpX * offsetDistance;
                textY += perpY * offsetDistance;
            }
        }

        // Small Dimension Logic: Shift text laterally if value is small
        float arrowLength = 0.8f;
        if (value < (4.0f * arrowLength)) {
            // Shift text to the "end" side (point 2)
            // Distance from midpoint to end is value / 2 approx (or length / 2)
            // We want to be past the arrow tail.
            // Tail length is 2.0 * arrowLength.

            // Calculate 2D length of the dimension line projected
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (alignment == Alignment.HORIZONTAL)
                dist = Math.abs(dx);
            if (alignment == Alignment.VERTICAL)
                dist = Math.abs(dy);

            // Shift = half_dist + tail_len + extra_margin
            float shift = (dist * 0.5f) + (arrowLength * 2.0f) + 1.2f;

            textX += dirX * shift;
            textY += dirY * shift;
        }
    }

    @Override
    public boolean contains(float x, float y, float tolerance) {
        // Simple hit test against the text position
        // In a real app, we'd check bounding box of text + dimension line
        double dist = Math.sqrt(Math.pow(x - textX, 2) + Math.pow(y - textY, 2));
        // Be generous with tolerance for text selection
        return dist < (tolerance * 10.0f); // Assuming standard tolerance is small (~0.5)
    }

    @Override
    public void draw(GL2 gl) {
        if (!visible)
            return;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.3f, 0.3f, 0.3f); // Professional dark gray

        // Determine Geometry based on Alignment
        float dirX, dirY, perpX, perpY;
        float lineX1, lineY1, lineX2, lineY2;
        float dimLinkX1, dimLinkY1, dimLinkX2, dimLinkY2; // Start of ext lines at object

        if (alignment == Alignment.HORIZONTAL) {
            // Horizontal Dims: Line is horizontal (1, 0)
            // Extension lines are Vertical (0, 1)
            dirX = 1;
            dirY = 0;
            perpX = 0;
            perpY = 1;

            // Extension lines start at the points' X, but vertical Y?
            // No, extension lines start AT the points (x1, y1) and (x2, y2)
            // And go vertically to the dimension line Y level.

            // Current 'offsetDistance' stores (MouseY - CenterY)
            // So dimLine Y = CenterY + offsetDistance = MouseY
            float dimY = (y1 + y2) / 2.0f + offsetDistance;

            dimLinkX1 = x1;
            dimLinkY1 = y1;
            dimLinkX2 = x2;
            dimLinkY2 = y2;

            // Dimension Line Endpoints
            lineX1 = x1;
            lineY1 = dimY;
            lineX2 = x2;
            lineY2 = dimY;

            // Override perp to be vertical for drawing logic below
            perpX = 0;
            perpY = (offsetDistance > 0 ? 1 : -1); // Just direction

        } else if (alignment == Alignment.VERTICAL) {
            // Vertical Dims: Line is Vertical (0, 1)
            // Extension lines are Horizontal (1, 0)
            dirX = 0;
            dirY = 1;
            perpX = 1;
            perpY = 0;

            float dimX = (x1 + x2) / 2.0f + offsetDistance;

            dimLinkX1 = x1;
            dimLinkY1 = y1;
            dimLinkX2 = x2;
            dimLinkY2 = y2;

            lineX1 = dimX;
            lineY1 = y1;
            lineX2 = dimX;
            lineY2 = y2;

            perpX = (offsetDistance > 0 ? 1 : -1);
            perpY = 0;

        } else {
            // ALIGNED (Standard)
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001f)
                return;

            dirX = dx / len;
            dirY = dy / len;
            perpX = -dirY; // Unit perp
            perpY = dirX;

            // Use calculated offset
            float px = perpX * offsetDistance;
            float py = perpY * offsetDistance;

            dimLinkX1 = x1;
            dimLinkY1 = y1;
            dimLinkX2 = x2;
            dimLinkY2 = y2;

            lineX1 = x1 + px;
            lineY1 = y1 + py;
            lineX2 = x2 + px;
            lineY2 = y2 + py;
        }

        // Common Drawing Logic using computed endpoints (lineX, lineY) and directions

        // Extension/Witness line parameters
        float extensionGap = 1.0f;
        float extensionOverhang = 1.0f;

        gl.glLineWidth(1.0f);

        // Draw witness/extension lines
        gl.glBegin(GL2.GL_LINES);

        // We need to draw from Object (dimLink) to Dimension Line (line)
        // ideally with a gap.
        // Direction is (line - dimLink).

        // Ext 1
        drawExtensionLine(gl, dimLinkX1, dimLinkY1, lineX1, lineY1, extensionGap, extensionOverhang);
        // Ext 2
        drawExtensionLine(gl, dimLinkX2, dimLinkY2, lineX2, lineY2, extensionGap, extensionOverhang);

        gl.glEnd();

        // Draw dimension line WITH GAP FOR TEXT
        float midX = (lineX1 + lineX2) / 2.0f;
        float midY = (lineY1 + lineY2) / 2.0f;
        float textGap = Math.max(3.0f, (float) value * 0.2f);

        // Recalculate Dir for Arrow Drawing (normalized vector along dim line)
        float ldx = lineX2 - lineX1;
        float ldy = lineY2 - lineY1;
        float lLen = (float) Math.sqrt(ldx * ldx + ldy * ldy);
        if (lLen > 0.001) {
            dirX = ldx / lLen;
            dirY = ldy / lLen;
        }

        gl.glLineWidth(1.2f);
        float arrowLength = 0.8f;
        float arrowWidth = 0.3f;
        boolean isSmall = lLen < (4.0f * arrowLength); // Heuristic for "small" dimension

        if (isSmall) {
            // --- Small Dimension Sytle: Arrows Outside, Text Inside (or overlapping) ---

            // Draw full line between witness lines (no gap) - actually, usually we want a
            // gap for text?
            // If it's small, text will definitely overlap.
            // Let's draw the line continuous between points? Or keep typical gap?
            // If we keep typical gap, almost nothing is drawn.

            // Draw "tails" outside
            float tailLen = arrowLength * 2.0f;
            gl.glBegin(GL2.GL_LINES);
            // Tail Left (from X1 outwards to Left)
            gl.glVertex2f(lineX1, lineY1);
            gl.glVertex2f(lineX1 - dirX * tailLen, lineY1 - dirY * tailLen);
            // Tail Right (from X2 outwards to Right)
            gl.glVertex2f(lineX2, lineY2);
            gl.glVertex2f(lineX2 + dirX * tailLen, lineY2 + dirY * tailLen);
            gl.glEnd();

            // Draw Arrows pointing IN (towards the center)
            // Left Arrow at X1: We want it pointing Right (->).
            // drawProfessionalArrow with 'dir' creates '<-'. So we pass '-dir'.
            drawProfessionalArrow(gl, lineX1, lineY1, -dirX, -dirY, arrowLength, arrowWidth);

            // Right Arrow at X2: We want it pointing Left (<-).
            // drawProfessionalArrow with '-dir' creates '->'. So we pass 'dir'.
            drawProfessionalArrow(gl, lineX2, lineY2, dirX, dirY, arrowLength, arrowWidth);

        } else {
            // --- Standard Style: Arrows Inside, Text Gap ---

            gl.glBegin(GL2.GL_LINES);
            // Left segment
            gl.glVertex2f(lineX1, lineY1);
            gl.glVertex2f(midX - dirX * textGap, midY - dirY * textGap);
            // Right segment
            gl.glVertex2f(midX + dirX * textGap, midY + dirY * textGap);
            gl.glVertex2f(lineX2, lineY2);
            gl.glEnd();

            // Draw arrows
            drawProfessionalArrow(gl, lineX1, lineY1, dirX, dirY, arrowLength, arrowWidth);
            drawProfessionalArrow(gl, lineX2, lineY2, -dirX, -dirY, arrowLength, arrowWidth);
        }

        gl.glLineWidth(1.0f);
        gl.glEnable(GL2.GL_LIGHTING);
    }

    private void drawExtensionLine(GL2 gl, float x1, float y1, float x2, float y2, float gap, float over) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001)
            return;

        float uX = dx / len;
        float uY = dy / len;

        // Gap start
        float sx = x1 + uX * gap;
        float sy = y1 + uY * gap;

        // Overhang end
        float ex = x2 + uX * over;
        float ey = y2 + uY * over;

        gl.glVertex2f(sx, sy);
        gl.glVertex2f(ex, ey);
    }

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
        gl.glVertex2f(x, y); // Tip
        gl.glVertex2f(baseX + perpX, baseY + perpY); // Wing 1
        gl.glVertex2f(baseX - perpX, baseY - perpY); // Wing 2
        gl.glEnd();
    }

    public void setOffsetDistance(float offset) {
        this.offsetDistance = offset;
        calculatePosition();
    }
}
