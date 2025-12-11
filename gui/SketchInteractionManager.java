package cad.gui;

import cad.core.Sketch;
import cad.core.Sketch.PointEntity;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages interactive sketching states (e.g., clicking to start a line,
 * dragging to preview).
 * Handles the "rubber band" effect and commits entities to the Sketch object.
 */
public class SketchInteractionManager {

    public enum InteractionMode {
        IDLE,
        VIEW_ROTATE, // Standard rotation/pan/zoom
        SKETCH_LINE,
        SKETCH_CIRCLE,
        SKETCH_POLYGON,
        SKETCH_RECTANGLE // Potential future addition
    }

    private InteractionMode currentMode = InteractionMode.VIEW_ROTATE;
    private final Sketch sketch;

    // Scratchpad variables for interactive drawing
    private boolean isDrawing = false;
    private float startX, startY;
    private float currentX, currentY;

    // For storing the temporary shape being drawn (ghost)
    // We will construct a temporary entity to render

    public SketchInteractionManager(Sketch sketch) {
        this.sketch = sketch;
    }

    public void setMode(InteractionMode mode) {
        this.currentMode = mode;
        this.isDrawing = false; // Reset any active drawing
        System.out.println("Interaction Mode set to: " + mode);
    }

    public InteractionMode getMode() {
        return currentMode;
    }

    /**
     * Handles mouse click events from the canvas.
     * 
     * @param e      MouseEvent
     * @param worldX Projected world X coordinate from mouse
     * @param worldY Projected world Y coordinate from mouse
     */
    /**
     * Handles mouse press events to START drawing.
     */
    public void handleMousePress(float worldX, float worldY) {
        if (currentMode == InteractionMode.IDLE || currentMode == InteractionMode.VIEW_ROTATE)
            return;

        startX = worldX;
        startY = worldY;
        currentX = worldX;
        currentY = worldY;
        isDrawing = true;
        System.out.println("Sketch Action Started at: " + startX + ", " + startY);
    }

    /**
     * Handles mouse release events to FINISH drawing.
     */
    public void handleMouseRelease(float worldX, float worldY) {
        if (!isDrawing)
            return;

        if (currentMode == InteractionMode.SKETCH_LINE) {
            sketch.addLine(startX, startY, worldX, worldY);
            System.out.println("Line Added.");
        } else if (currentMode == InteractionMode.SKETCH_CIRCLE) {
            float radius = (float) Math.sqrt(Math.pow(worldX - startX, 2) + Math.pow(worldY - startY, 2));
            if (radius > 0)
                sketch.addCircle(startX, startY, radius);
            System.out.println("Circle Added.");
        } else if (currentMode == InteractionMode.SKETCH_POLYGON) {
            float radius = (float) Math.sqrt(Math.pow(worldX - startX, 2) + Math.pow(worldY - startY, 2));
            if (radius > 0)
                sketch.addNSidedPolygon(startX, startY, radius, 6);
            System.out.println("Polygon Added.");
        }

        isDrawing = false; // Reset state
    }

    /**
     * Legacy handler - kept for compatibility but largely superseded by
     * Press/Release for dragging.
     */
    public void handleMouseClick(MouseEvent e, float worldX, float worldY) {
        // No-op for now to enforce drag behavior
    }

    /**
     * Handles mouse move/drag events to update the "rubber band" preview.
     */
    public void handleMouseMove(float worldX, float worldY) {
        if (isDrawing) {
            currentX = worldX;
            currentY = worldY;
        }
    }

    /**
     * Returns true if there is an active ghost shape to render.
     */
    public boolean isDrawing() {
        return isDrawing;
    }

    // Getters for render loop to draw the ghost shape
    public float getStartX() {
        return startX;
    }

    public float getStartY() {
        return startY;
    }

    public float getCurrentX() {
        return currentX;
    }

    public float getCurrentY() {
        return currentY;
    }
}
