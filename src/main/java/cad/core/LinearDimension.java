package cad.core;

import com.jogamp.opengl.GL2;

public class LinearDimension extends Dimension {

    public enum Alignment {
        ALIGNED, HORIZONTAL, VERTICAL
    }

    private float x1, y1, z1; 
    private float x2, y2, z2; 
    private float offsetDistance = 5.0f; 
    private boolean is3D = false;
    private Alignment alignment = Alignment.ALIGNED;

    
    public void updateSmartDimension(float mx, float my) {
        
        float cx = (x1 + x2) / 2.0f;
        float cy = (y1 + y2) / 2.0f;

        
        float dx = Math.abs(mx - cx);
        float dy = Math.abs(my - cy);

        
        
        
        
        

        float width = Math.abs(x2 - x1);
        float height = Math.abs(y2 - y1);

        
        boolean forcesHoriz = (dx < width * 0.8 && dy > height * 0.5);
        boolean forcesVert = (dy < height * 0.8 && dx > width * 0.5);

        
        
        double angleLine = Math.atan2(y2 - y1, x2 - x1);
        double angleMouse = Math.atan2(my - cy, mx - cx);
        double diffAngle = Math.abs(angleMouse - angleLine);
        while (diffAngle > Math.PI)
            diffAngle -= 2 * Math.PI;
        diffAngle = Math.abs(diffAngle);

        
        

        
        
        

        
        

        
        
        

        
        
        

        
        

        
        
        

        
        boolean lineIsMoreVert = height > width;

        if (lineIsMoreVert) {
            
            
            
            
            
            if (dx > dy) {
                alignment = Alignment.VERTICAL;
            } else {
                alignment = Alignment.ALIGNED;
            }
        } else {
            
            
            
            
            if (dy > dx) {
                alignment = Alignment.HORIZONTAL;
            } else {
                alignment = Alignment.ALIGNED;
            }
        }

        
        
        
        
        

        double absSlope = (dx == 0) ? 999 : Math.abs((my - cy) / (mx - cx));

        
        if (absSlope > 1.5) { 
            alignment = Alignment.HORIZONTAL;
        } else if (absSlope < 0.5) { 
            alignment = Alignment.VERTICAL;
        } else {
            alignment = Alignment.ALIGNED;
        }

        
        if (alignment == Alignment.HORIZONTAL) {
            this.value = Math.abs(x2 - x1);
            
            this.offsetDistance = (my - cy);
            
            
        } else if (alignment == Alignment.VERTICAL) {
            this.value = Math.abs(y2 - y1);
            this.offsetDistance = (mx - cx);
        } else {
            this.value = calculateLength(x1, y1, z1, x2, y2, z2);
            
            float ldx = x2 - x1;
            float ldy = y2 - y1;
            float length = (float) Math.sqrt(ldx * ldx + ldy * ldy);
            float perpX = -ldy / length;
            float perpY = ldx / length;
            
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
        
        float midX = (x1 + x2) / 2.0f;
        float midY = (y1 + y2) / 2.0f;
        float midZ = (z1 + z2) / 2.0f;

        textX = midX;
        textY = midY;
        textZ = midZ;

        
        float dirX = 0, dirY = 0;
        float perpX = 0, perpY = 0;

        if (alignment == Alignment.HORIZONTAL) {
            
            float dx = x2 - x1;
            
            dirX = (dx >= 0) ? 1.0f : -1.0f;
            dirY = 0.0f;

            
            
            perpX = 0.0f;
            perpY = 1.0f; 

            
            textY += offsetDistance;

        } else if (alignment == Alignment.VERTICAL) {
            
            float dy = y2 - y1;
            
            dirX = 0.0f;
            dirY = (dy >= 0) ? 1.0f : -1.0f;

            
            perpX = 1.0f;
            perpY = 0.0f;

            
            textX += offsetDistance;

        } else {
            
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);

            if (len > 0.0001f) {
                dirX = dx / len;
                dirY = dy / len;
                
                perpX = -dirY;
                perpY = dirX;

                textX += perpX * offsetDistance;
                textY += perpY * offsetDistance;
            }
        }

        
        float arrowLength = 0.8f;
        if (value < (4.0f * arrowLength)) {
            
            
            
            

            
            float dx = x2 - x1;
            float dy = y2 - y1;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (alignment == Alignment.HORIZONTAL)
                dist = Math.abs(dx);
            if (alignment == Alignment.VERTICAL)
                dist = Math.abs(dy);

            
            float shift = (dist * 0.5f) + (arrowLength * 2.0f) + 1.2f;

            textX += dirX * shift;
            textY += dirY * shift;
        }
    }

    @Override
    public boolean contains(float x, float y, float tolerance) {
        
        
        double dist = Math.sqrt(Math.pow(x - textX, 2) + Math.pow(y - textY, 2));
        
        return dist < (tolerance * 10.0f); 
    }

    @Override
    public void draw(GL2 gl) {
        if (!visible)
            return;

        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor3f(0.3f, 0.3f, 0.3f); 

        
        float dirX, dirY, perpX, perpY;
        float lineX1, lineY1, lineX2, lineY2;
        float dimLinkX1, dimLinkY1, dimLinkX2, dimLinkY2; 

        if (alignment == Alignment.HORIZONTAL) {
            
            
            dirX = 1;
            dirY = 0;
            perpX = 0;
            perpY = 1;

            
            
            

            
            
            float dimY = (y1 + y2) / 2.0f + offsetDistance;

            dimLinkX1 = x1;
            dimLinkY1 = y1;
            dimLinkX2 = x2;
            dimLinkY2 = y2;

            
            lineX1 = x1;
            lineY1 = dimY;
            lineX2 = x2;
            lineY2 = dimY;

            
            perpX = 0;
            perpY = (offsetDistance > 0 ? 1 : -1); 

        } else if (alignment == Alignment.VERTICAL) {
            
            
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
            
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len < 0.001f)
                return;

            dirX = dx / len;
            dirY = dy / len;
            perpX = -dirY; 
            perpY = dirX;

            
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

        

        
        float extensionGap = 1.0f;
        float extensionOverhang = 1.0f;

        gl.glLineWidth(1.0f);

        
        gl.glBegin(GL2.GL_LINES);

        
        
        

        
        drawExtensionLine(gl, dimLinkX1, dimLinkY1, lineX1, lineY1, extensionGap, extensionOverhang);
        
        drawExtensionLine(gl, dimLinkX2, dimLinkY2, lineX2, lineY2, extensionGap, extensionOverhang);

        gl.glEnd();

        
        float midX = (lineX1 + lineX2) / 2.0f;
        float midY = (lineY1 + lineY2) / 2.0f;
        float textGap = Math.max(3.0f, (float) value * 0.2f);

        
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
        boolean isSmall = lLen < (4.0f * arrowLength); 

        if (isSmall) {
            

            
            
            
            
            

            
            float tailLen = arrowLength * 2.0f;
            gl.glBegin(GL2.GL_LINES);
            
            gl.glVertex2f(lineX1, lineY1);
            gl.glVertex2f(lineX1 - dirX * tailLen, lineY1 - dirY * tailLen);
            
            gl.glVertex2f(lineX2, lineY2);
            gl.glVertex2f(lineX2 + dirX * tailLen, lineY2 + dirY * tailLen);
            gl.glEnd();

            
            
            
            drawProfessionalArrow(gl, lineX1, lineY1, -dirX, -dirY, arrowLength, arrowWidth);

            
            
            drawProfessionalArrow(gl, lineX2, lineY2, dirX, dirY, arrowLength, arrowWidth);

        } else {
            

            gl.glBegin(GL2.GL_LINES);
            
            gl.glVertex2f(lineX1, lineY1);
            gl.glVertex2f(midX - dirX * textGap, midY - dirY * textGap);
            
            gl.glVertex2f(midX + dirX * textGap, midY + dirY * textGap);
            gl.glVertex2f(lineX2, lineY2);
            gl.glEnd();

            
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

        
        float sx = x1 + uX * gap;
        float sy = y1 + uY * gap;

        
        float ex = x2 + uX * over;
        float ey = y2 + uY * over;

        gl.glVertex2f(sx, sy);
        gl.glVertex2f(ex, ey);
    }

    private void drawProfessionalArrow(GL2 gl, float x, float y, float dirX, float dirY,
            float length, float width) {
        
        float perpX = -dirY * width;
        float perpY = dirX * width;

        
        float baseX = x + dirX * length;
        float baseY = y + dirY * length;

        
        gl.glBegin(GL2.GL_TRIANGLES);
        gl.glVertex2f(x, y); 
        gl.glVertex2f(baseX + perpX, baseY + perpY); 
        gl.glVertex2f(baseX - perpX, baseY - perpY); 
        gl.glEnd();
    }

    public void setOffsetDistance(float offset) {
        this.offsetDistance = offset;
        calculatePosition();
    }
}
