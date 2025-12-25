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

public class SketchInteractionManager {

    public enum InteractionMode {
        IDLE,
        VIEW_ROTATE,
        SKETCH_LINE,
        SKETCH_CIRCLE,
        SKETCH_POLYGON,
        SKETCH_SPLINE,
        SKETCH_RECTANGLE,
        SKETCH_POINT,
        SKETCH_KITE,
        SKETCH_ARC,
        DIMENSION_TOOL,
        SELECT
    }

    private InteractionMode currentMode = InteractionMode.VIEW_ROTATE;
    private final Sketch sketch;
    private final CommandManager commandManager;

    private boolean isDrawing = false;
    private float startX, startY;
    private float currentX, currentY;

    private int polygonSides = 6;

    private List<Sketch.Entity> selectedEntities = new ArrayList<>();

    private Entity dimFirstEntity = null;

    private List<PointEntity> tempSplinePoints = new ArrayList<>();

    private PointEntity arcCenter = null;
    private float arcStartAngle = 0;
    private float arcRadius = 0;
    private int arcClickCount = 0;

    public SketchInteractionManager(Sketch sketch, CommandManager commandManager) {
        this.sketch = sketch;
        this.commandManager = commandManager;
    }

    public void setMode(InteractionMode mode) {
        this.currentMode = mode;
        this.isDrawing = false;
        this.tempSplinePoints.clear();
        if (sketch != null)
            sketch.clearTempEntities();
        this.arcClickCount = 0;
        this.arcCenter = null;
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
        } else if (currentMode == InteractionMode.DIMENSION_TOOL) {
            handleDimensionClick(worldX, worldY);
        } else if (currentMode == InteractionMode.SKETCH_ARC) {
            handleArcClick(worldX, worldY);
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

        isDrawing = false;
    }

    public void handleMouseClick(MouseEvent e, float worldX, float worldY) {
        if (currentMode == InteractionMode.SKETCH_SPLINE) {
            boolean isDoubleClick = e.getClickCount() == 2;

            boolean isDuplicate = false;
            if (!tempSplinePoints.isEmpty()) {
                PointEntity last = tempSplinePoints.get(tempSplinePoints.size() - 1);

                float dx = worldX - last.getX();
                float dy = worldY - last.getY();
                if (Math.sqrt(dx * dx + dy * dy) < 0.1f) {
                    isDuplicate = true;
                }
            }

            if (!isDuplicate) {
                tempSplinePoints.add(new PointEntity(worldX, worldY));
                System.out.println("Spline Point Added. Total: " + tempSplinePoints.size());
            }

            if (isDoubleClick) {
                if (tempSplinePoints.size() >= 2) {
                    sketch.addSpline(new ArrayList<>(tempSplinePoints), false);
                    System.out.println("Spline Finished.");
                } else {
                    System.out.println("Spline requires at least 2 points.");
                }
                tempSplinePoints.clear();
            }
        }
    }

    private void handleArcClick(float worldX, float worldY) {
        if (arcClickCount == 0) {
            arcCenter = new PointEntity(worldX, worldY);
            arcClickCount = 1;
            System.out.println("Arc Center set at: " + worldX + ", " + worldY);
        } else if (arcClickCount == 1) {
            float dx = worldX - arcCenter.getX();
            float dy = worldY - arcCenter.getY();
            arcRadius = (float) Math.sqrt(dx * dx + dy * dy);
            arcStartAngle = (float) Math.toDegrees(Math.atan2(dy, dx));
            arcClickCount = 2;
            System.out.println("Arc Start set. Radius: " + arcRadius + ", Start Angle: " + arcStartAngle);
        } else if (arcClickCount == 2) {
            float dx = worldX - arcCenter.getX();
            float dy = worldY - arcCenter.getY();
            float endAngle = (float) Math.toDegrees(Math.atan2(dy, dx));

            double diff = endAngle - arcStartAngle;
            while (diff <= -180)
                diff += 360;
            while (diff > 180)
                diff -= 360;

            float finalStart, finalEnd;
            if (diff < 0) {
                finalStart = endAngle;
                finalEnd = arcStartAngle;
            } else {
                finalStart = arcStartAngle;
                finalEnd = endAngle;
            }

            Sketch.Arc arc = new Sketch.Arc(arcCenter.getX(), arcCenter.getY(), arcRadius, finalStart, finalEnd);
            if (commandManager != null) {
                commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, arc, "Arc"));
            } else {
                sketch.addEntity(arc);
            }
            System.out.println("Arc Created: " + arc);

            arcClickCount = 0;
            arcCenter = null;
            isDrawing = false;
            sketch.clearTempEntities();
        }
    }

    public void handleMouseMove(float worldX, float worldY) {
        if (isDrawing) {
            currentX = worldX;
            currentY = worldY;
            sketch.clearTempEntities();

            if (currentMode == InteractionMode.SKETCH_ARC) {
                if (arcClickCount == 1) {
                    sketch.addTempEntity(new Sketch.Line(arcCenter.getX(), arcCenter.getY(), currentX, currentY));
                } else if (arcClickCount == 2) {
                    float dx = worldX - arcCenter.getX();
                    float dy = worldY - arcCenter.getY();
                    float endAngle = (float) Math.toDegrees(Math.atan2(dy, dx));

                    double diff = endAngle - arcStartAngle;
                    while (diff <= -180)
                        diff += 360;
                    while (diff > 180)
                        diff -= 360;

                    float finalStart, finalEnd;
                    if (diff < 0) {
                        finalStart = endAngle;
                        finalEnd = arcStartAngle;
                    } else {
                        finalStart = arcStartAngle;
                        finalEnd = endAngle;
                    }

                    Sketch.Arc tempArc = new Sketch.Arc(arcCenter.getX(), arcCenter.getY(), arcRadius, finalStart,
                            finalEnd);
                    sketch.addTempEntity(tempArc);
                    sketch.addTempEntity(new Sketch.Line(arcCenter.getX(), arcCenter.getY(), currentX, currentY));
                }
            }
        }
    }

    public boolean isDrawing() {
        return isDrawing;
    }

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

        PointEntity clickedPoint = sketch.getClosestVertex(x, y, tolerance);

        if (clickedPoint != null) {
            if (dimFirstEntity == null) {

                dimFirstEntity = clickedPoint;
                System.out.println("First point selected for dimension. Click second point.");
            } else if (dimFirstEntity instanceof PointEntity) {

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
                dimFirstEntity = null;
            } else {

                dimFirstEntity = clickedPoint;
                System.out.println("Switched to point dimensioning mode.");
            }
            return;
        }

        Sketch.PolygonEdgeContext polyEdge = sketch.getClosestPolygonEdge(x, y, tolerance);

        if (polyEdge != null) {

            List<Sketch.PointEntity> pts = polyEdge.polygon.getSketchPoints();
            int idx1 = polyEdge.index1;
            int idx2 = polyEdge.index2;
            int idx0 = (idx1 - 1 + pts.size()) % pts.size();

            Sketch.PointEntity p0 = pts.get(idx0);
            Sketch.PointEntity p1 = pts.get(idx1);
            Sketch.PointEntity p2 = pts.get(idx2);

            double x1 = p0.getX(), y1 = p0.getY();
            double x2 = p1.getX(), y2 = p1.getY();
            double x3 = p2.getX(), y3 = p2.getY();

            double D = 2 * (x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2));
            if (Math.abs(D) > 0.001) {
                double centerX = ((x1 * x1 + y1 * y1) * (y2 - y3) + (x2 * x2 + y2 * y2) * (y3 - y1)
                        + (x3 * x3 + y3 * y3) * (y1 - y2)) / D;
                double centerY = ((x1 * x1 + y1 * y1) * (x3 - x2) + (x2 * x2 + y2 * y2) * (x1 - x3)
                        + (x3 * x3 + y3 * y3) * (x2 - x1)) / D;
                double radius = Math.sqrt(Math.pow(centerX - x2, 2) + Math.pow(centerY - y2, 2));

                System.out.println("Detected Polygon Curve: Radius " + radius);

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

        Line clickedLine = sketch.getClosestLineSegment(x, y, tolerance);

        if (clickedLine != null) {

            Dimension d = new LinearDimension(clickedLine.getX1(), clickedLine.getY1(),
                    clickedLine.getX2(), clickedLine.getY2(), unit);
            if (commandManager != null) {
                commandManager.executeCommand(new AddDimensionCommand(sketch, d));
            } else {
                sketch.addDimension(d);
            }
            System.out.println("Added Linear Dimension to Line/Edge.");
            dimFirstEntity = null;
            return;
        }

        Entity clicked = sketch.getClosestEntity(x, y, tolerance);

        if (clicked != null) {
            if (clicked instanceof Circle) {
                Dimension d = sketch.createDimensionFor(clicked, x, y);
                if (commandManager != null) {
                    commandManager.executeCommand(new AddDimensionCommand(sketch, d));
                } else {
                    sketch.addDimension(d);
                }
                System.out.println("Added Radial Dimension to Circle.");
            } else if (clicked instanceof Sketch.Arc) {
                Dimension d = sketch.createDimensionFor(clicked, x, y);
                if (commandManager != null) {
                    commandManager.executeCommand(new AddDimensionCommand(sketch, d));
                } else {
                    sketch.addDimension(d);
                }
                System.out.println("Added Radial Dimension to Arc.");
            }
            dimFirstEntity = null;
        } else {

            if (dimFirstEntity != null) {
                dimFirstEntity = null;
                System.out.println("Dimension selection cleared.");
            }
        }
    }

    private void handleSelectionClick(float x, float y) {
        float tolerance = 0.5f;

        Entity entity = sketch.getClosestVertex(x, y, tolerance);

        if (entity == null) {
            entity = sketch.getClosestLineSegment(x, y, tolerance);
        }

        if (entity == null) {
            entity = sketch.getClosestEntity(x, y, tolerance);
        }

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
