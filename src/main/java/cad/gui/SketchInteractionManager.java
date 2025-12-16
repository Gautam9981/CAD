package cad.gui;

import cad.core.Sketch;
import cad.core.Sketch.PointEntity;
import cad.core.CommandManager;
import cad.core.AddDimensionCommand;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import cad.core.Sketch.Entity;
import cad.core.Sketch.Line;
import cad.core.Sketch.Circle;
import cad.core.Sketch.PointEntity;
import cad.core.Dimension;
import cad.core.LinearDimension;
import cad.core.RadialDimension;
import cad.core.UnitSystem;

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
        SKETCH_RECTANGLE, // Potential future addition
        SKETCH_POINT,    // Click to create point
        SKETCH_KITE,     // Click and drag for kite (if implemented that way, or just dialog)
        DIMENSION_TOOL, // Dimensioning mode
        SELECT         // Select entities for constraints/properties
    }

    private InteractionMode currentMode = InteractionMode.VIEW_ROTATE;
    private final Sketch sketch;
    private final CommandManager commandManager;

    // Scratchpad variables for interactive drawing
    private boolean isDrawing = false;
    private float startX, startY;
    private float currentX, currentY;
    
    // Shape parameters
    private int polygonSides = 6; // Default hexagon
    
    // Selection state
    private List<Sketch.Entity> selectedEntities = new ArrayList<>();
    
    // Dimensioning state
    private Entity dimFirstEntity = null;

    // For storing the temporary shape being drawn (ghost)
    // We will construct a temporary entity to render

    public SketchInteractionManager(Sketch sketch, CommandManager commandManager) {
        this.sketch = sketch;
        this.commandManager = commandManager;
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
        
        if (currentMode == InteractionMode.SELECT) {
            handleSelectionClick(worldX, worldY);
        } else if (currentMode == InteractionMode.DIMENSION_TOOL) {
            handleDimensionClick(worldX, worldY);
        }
    }

    /**
     * Handles mouse release events to FINISH drawing.
     */
    public void handleMouseRelease(float worldX, float worldY) {
        if (!isDrawing)
            return;

        if (currentMode == InteractionMode.SKETCH_LINE) {
            Line line = new Line(startX, startY, worldX, worldY);
            if (commandManager != null) {
                commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, line, "Line"));
            } else {
                sketch.addEntity(line);
            }
            System.out.println("Line Added.");
        } else if (currentMode == InteractionMode.SKETCH_CIRCLE) {
            float radius = (float) Math.sqrt(Math.pow(worldX - startX, 2) + Math.pow(worldY - startY, 2));
            if (radius > 0) {
                Circle circle = new Circle(startX, startY, radius);
                if (commandManager != null) {
                    commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, circle, "Circle"));
                } else {
                    sketch.addEntity(circle);
                }
                System.out.println("Circle Added.");
            }
        } else if (currentMode == InteractionMode.SKETCH_POLYGON) {
            float radius = (float) Math.sqrt(Math.pow(worldX - startX, 2) + Math.pow(worldY - startY, 2));
            
            List<PointEntity> points = new ArrayList<>();
            for (int i = 0; i < polygonSides; i++) {
                double angle = 2 * Math.PI * i / polygonSides;
                float px = startX + (float) (radius * Math.cos(angle));
                float py = startY + (float) (radius * Math.sin(angle));
                points.add(new PointEntity(px, py));
            }
            
            cad.core.Sketch.Polygon poly = new cad.core.Sketch.Polygon(points);
            
            if (commandManager != null) {
                commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, poly, "Polygon"));
            } else {
                sketch.addEntity(poly);
            }
            System.out.println(polygonSides + "-sided Polygon Added.");
        } else if (currentMode == InteractionMode.SKETCH_POINT) {
            PointEntity point = new PointEntity(worldX, worldY);
            if (commandManager != null) {
                commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, point, "Point"));
            } else {
                sketch.addEntity(point);
            }
            System.out.println("Point Added at " + worldX + ", " + worldY);
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
    
    /**
     * Sets the number of sides for polygon creation.
     * @param sides Number of sides (must be >= 3)
     */
    public void setPolygonSides(int sides) {
        if (sides >= 3) {
            this.polygonSides = sides;
        }
    }
    
    /**
     * Gets the current number of sides for polygon creation.
     * @return Number of sides
     */
    public int getPolygonSides() {
        return polygonSides;
    }
    
    /**
     * Handles clicks for the smart dimension tool.
     */
    private void handleDimensionClick(float x, float y) {
        float tolerance = 0.5f;
        Entity clicked = sketch.getClosestEntity(x, y, tolerance);
        String unit = sketch.getUnitSystem().getAbbreviation();
        
        if (clicked == null) {
            // Clicked empty space - if we had a first point selected, clear it
            if (dimFirstEntity != null) {
                dimFirstEntity = null;
                System.out.println("Dimension selection cleared.");
            }
            return;
        }
        
        if (clicked instanceof Line) {
            // Create linear dimension aligned with the line

            Line l = (Line) clicked;
            Dimension d = new LinearDimension(l.getX1(), l.getY1(), l.getX2(), l.getY2(), unit);
            if (commandManager != null) {
                commandManager.executeCommand(new AddDimensionCommand(sketch, d));
            } else {
                sketch.addDimension(d);
            }
            System.out.println("Added Linear Dimension to Line.");
            dimFirstEntity = null; // Reset
        } else if (clicked instanceof Circle) {
            Circle c = (Circle) clicked;
            // Create radial dimension
            // Create radial dimension
            Dimension d = new RadialDimension(c.getX(), c.getY(), c.getRadius(), false, unit);
            if (commandManager != null) {
                commandManager.executeCommand(new AddDimensionCommand(sketch, d));
            } else {
                sketch.addDimension(d);
            }
             System.out.println("Added Radial Dimension to Circle.");
             dimFirstEntity = null; // Reset
        } else if (clicked instanceof PointEntity) {
            PointEntity p = (PointEntity) clicked;
            
            if (dimFirstEntity == null) {
                // First point selected
                dimFirstEntity = p;
                System.out.println("First point selected for dimension. Click second point.");
            } else if (dimFirstEntity instanceof PointEntity) {
                // Second point selected
                PointEntity p1 = (PointEntity) dimFirstEntity;
                if (p1 != p) {
                     Dimension d = new LinearDimension(p1.getX(), p1.getY(), p.getX(), p.getY(), unit);
                     if (commandManager != null) {
                        commandManager.executeCommand(new AddDimensionCommand(sketch, d));
                     } else {
                        sketch.addDimension(d);
                     }
                     System.out.println("Added Linear Dimension betweeen Points.");
                } else {
                    System.out.println("Same point clicked twice.");
                }
                dimFirstEntity = null; // Reset
            } else {
                // Should not happen if logic is correct, but safe reset
                dimFirstEntity = p;
            }
        }
    }

    /**
     * Handles selection click.
     */
    private void handleSelectionClick(float x, float y) {
        float tolerance = 0.5f; // Selection tolerance
        Entity entity = sketch.getClosestEntity(x, y, tolerance);
        
        if (entity != null) {
            if (selectedEntities.contains(entity)) {
                selectedEntities.remove(entity);
                System.out.println("Deselected entity.");
            } else {
                selectedEntities.add(entity);
                System.out.println("Selected entity: " + entity.getClass().getSimpleName());
            }
        } else {
            selectedEntities.clear();
            System.out.println("Selection cleared.");
        }
    }

    public List<Entity> getSelectedEntities() {
        return selectedEntities;
    }

    public void clearSelection() {
        selectedEntities.clear();
    }
}
