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

public class SketchInteractionManager {

    public enum InteractionMode {
        IDLE,
        VIEW_ROTATE, // Standard rotation/pan/zoom
        SKETCH_LINE,
        SKETCH_CIRCLE,
        SKETCH_POLYGON,
        SKETCH_RECTANGLE, // Potential future addition
        SKETCH_POINT, // Click to create point
        SKETCH_KITE, // Click and drag for kite (if implemented that way, or just dialog)
        DIMENSION_TOOL, // Dimensioning mode
        SELECT // Select entities for constraints/properties
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

            // Require minimum radius to avoid degenerate polygons
            if (radius < 0.1f) {
                System.out.println("Polygon radius too small. Drag further from center.");
                isDrawing = false;
                return;
            }

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

    public void handleMouseClick(MouseEvent e, float worldX, float worldY) {
        // No-op for now to enforce drag behavior
    }

    public void handleMouseMove(float worldX, float worldY) {
        if (isDrawing) {
            currentX = worldX;
            currentY = worldY;
        }
    }

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

    public void setPolygonSides(int sides) {
        if (sides >= 3) {
            this.polygonSides = sides;
        }
    }

    public int getPolygonSides() {
        return polygonSides;
    }

    private void handleDimensionClick(float x, float y) {
        float tolerance = 0.5f;
        String unit = sketch.getUnitSystem().getAbbreviation();

        // 1. Priority: Try to snap to a specific VERTEX (Point)
        // This allows precise Point-to-Point dimensioning on any shape (polygons,
        // lines, etc.)
        PointEntity clickedPoint = sketch.getClosestVertex(x, y, tolerance);

        if (clickedPoint != null) {
            if (dimFirstEntity == null) {
                // First point selected
                dimFirstEntity = clickedPoint;
                System.out.println("First point selected for dimension. Click second point.");
            } else if (dimFirstEntity instanceof PointEntity) {
                // Second point selected - Create dimension
                PointEntity p1 = (PointEntity) dimFirstEntity;
                if (p1 != clickedPoint) {
                    Dimension d = new LinearDimension(p1.getX(), p1.getY(), clickedPoint.getX(), clickedPoint.getY(),
                            unit);
                    if (commandManager != null) {
                        commandManager.executeCommand(new AddDimensionCommand(sketch, d));
                    } else {
                        sketch.addDimension(d);
                    }
                    System.out.println("Added Linear Dimension between Points.");
                } else {
                    System.out.println("Same point clicked twice.");
                }
                dimFirstEntity = null; // Reset
            } else {
                // Reset if switching from entity mode to point mode
                dimFirstEntity = clickedPoint;
                System.out.println("Switched to point dimensioning mode.");
            }
            return; // Handled as point click
        }

        // 2. Priority: Try to select a specific LINE SEGMENT (Edge) with Curvature
        // Check
        Sketch.PolygonEdgeContext polyEdge = sketch.getClosestPolygonEdge(x, y, tolerance);

        if (polyEdge != null) {
            // Check if we should dimension this as a Curve (Radius) or a Line
            // For airfoils/curved polygons, we approximate local curvature using 3 points:
            // (i-1), (i), (i+1) - wait, edge is between i and i+1.
            // Let's use i, i+1, i+2 to get curvature at the edge location roughly?
            // Actually, best is to check the angle between adjacent segments.
            // Better: Fit a circle through 3 points: P(i-1), P(i), P(i+1) or P(i), P(i+1),
            // P(i+2).
            // Let's use P(i), P(i+1) and one neighbor P(i-1) to estimate curvature at P(i).
            // Or use P(i-1), P(i), P(i+1) to get curvature at vertex P(i) and assume it
            // applies to connected edges?
            // For the edge P1-P2, let's look at P0-P1-P2-P3.

            // To be safe and simple: Let's calculate Radius through P(i-1), P(i), P(i+1).
            // If the radius is reasonable (< 1000 units), we offer a Radial Dimension.
            // If it's huge (effectively straight), we use Linear.

            List<Sketch.PointEntity> pts = polyEdge.polygon.getSketchPoints();
            int idx1 = polyEdge.index1;
            int idx2 = polyEdge.index2;
            int idx0 = (idx1 - 1 + pts.size()) % pts.size(); // Previous point

            Sketch.PointEntity p0 = pts.get(idx0);
            Sketch.PointEntity p1 = pts.get(idx1); // Start of edge
            Sketch.PointEntity p2 = pts.get(idx2); // End of edge

            // Calculate circle through p0, p1, p2
            double x1 = p0.getX(), y1 = p0.getY();
            double x2 = p1.getX(), y2 = p1.getY();
            double x3 = p2.getX(), y3 = p2.getY();

            // Formula for circle from 3 points
            double D = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
            if (Math.abs(D) > 0.001) {
                double centerX = ((x1 * x1 + y1 * y1) * (y2 - y3) + (x2 * x2 + y2 * y2) * (y3 - y1)
                        + (x3 * x3 + y3 * y3) * (y1 - y2)) / D;
                double centerY = ((x1 * x1 + y1 * y1) * (x3 - x2) + (x2 * x2 + y2 * y2) * (x1 - x3)
                        + (x3 * x3 + y3 * y3) * (x2 - x1)) / D;
                double radius = Math.sqrt(Math.pow(centerX - x2, 2) + Math.pow(centerY - y2, 2));

                // Heuristic: If radius is reasonable (e.g., < 1000) and user clicked a polygon,
                // they might want the radius. BUT they might also want the length.
                // Airfoils have varying radii.
                // Let's bias towards Radial for "curved" looking polygons (many small
                // segments).
                // Or simply create a Radial Dimension!

                // Since this is a specialized "Curve", let's use RadialDimension.
                // Center is (centerX, centerY).

                System.out.println("Detected Polygon Curve: Radius " + radius);

                // Check if user clicked closer to the edge center (Length) or just on the
                // curve.
                // Let's assume Radial for curved polygons.
                Dimension d = new RadialDimension((float) centerX, (float) centerY, (float) radius, false, unit);
                if (commandManager != null) {
                    commandManager.executeCommand(new AddDimensionCommand(sketch, d));
                } else {
                    sketch.addDimension(d);
                }
                System.out.println("Added Local Radial Dimension to Polygon Edge.");
                dimFirstEntity = null;
                return;
            }
        }

        // Fallback to standard line segment logic if not a curve or curvature
        // calculation failed
        Line clickedLine = sketch.getClosestLineSegment(x, y, tolerance);

        if (clickedLine != null) {
            // Create linear dimension aligned with the line/edge
            Dimension d = new LinearDimension(clickedLine.getX1(), clickedLine.getY1(),
                    clickedLine.getX2(), clickedLine.getY2(), unit);
            if (commandManager != null) {
                commandManager.executeCommand(new AddDimensionCommand(sketch, d));
            } else {
                sketch.addDimension(d);
            }
            System.out.println("Added Linear Dimension to Line/Edge.");
            dimFirstEntity = null; // Reset
            return;
        }

        // 3. Priority: Check generally for other entities (mainly Circles)
        Entity clicked = sketch.getClosestEntity(x, y, tolerance);

        if (clicked != null) {
            if (clicked instanceof Circle) {
                Circle c = (Circle) clicked;
                // Create radial dimension
                Dimension d = new RadialDimension(c.getX(), c.getY(), c.getRadius(), false, unit);
                if (commandManager != null) {
                    commandManager.executeCommand(new AddDimensionCommand(sketch, d));
                } else {
                    sketch.addDimension(d);
                }
                System.out.println("Added Radial Dimension to Circle.");
                dimFirstEntity = null; // Reset
            }
        } else {
            // Clicked empty space
            if (dimFirstEntity != null) {
                dimFirstEntity = null;
                System.out.println("Dimension selection cleared.");
            }
        }
    }

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
