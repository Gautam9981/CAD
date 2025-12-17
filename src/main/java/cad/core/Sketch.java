package cad.core;

import com.jogamp.opengl.GL2;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
// import java.awt.Graphics; // No longer directly used for OpenGL drawing

/**
 * 2D sketching system for creating and managing geometric entities.
 * Supports points, lines, circles, polygons with DXF import/export
 * capabilities.
 */
public class Sketch {

    /**
     * Enum for different types of sketch entities.
     */
    public enum TypeSketch {
        POINT,
        LINE,
        CIRCLE,
        POLYGON
    }

    /**
     * Abstract base class for all sketch entities.
     */
    public static abstract class Entity {
        TypeSketch type;
    }

    /**
     * Represents a point in the 2D sketch.
     * Wrapper around the core Point object.
     */
    public static class PointEntity extends Entity {
        private final Point point;

        // Constraint-ready constructor
        public PointEntity(Point p) {
            this.type = TypeSketch.POINT;
            this.point = p;
        }

        public PointEntity(float x, float y) {
            this(new Point(x, y));
        }

        public float getX() {
            return point.x;
        }

        public float getY() {
            return point.y;
        }

        public Point getPoint() {
            return point;
        }

        public void setPoint(float x, float y) {
            point.set(x, y);
        }

        @Override
        public String toString() {
            return String.format("Point at %s", point);
        }
    }

    /**
     * Represents a line segment in the 2D sketch.
     * Uses two Point objects for endpoints.
     */
    public static class Line extends Entity {
        private final Point start;
        private final Point end;

        public Line(Point start, Point end) {
            this.type = TypeSketch.LINE;
            this.start = start;
            this.end = end;
        }

        public Line(float x1, float y1, float x2, float y2) {
            this(new Point(x1, y1), new Point(x2, y2));
        }

        // Getters for topology
        public Point getStartPoint() {
            return start;
        }

        public Point getEndPoint() {
            return end;
        }

        // Convenience getters for values (used by renderer)
        public float getX1() {
            return start.x;
        }

        public float getY1() {
            return start.y;
        }

        public float getX2() {
            return end.x;
        }

        public float getY2() {
            return end.y;
        }

        public void setStart(float x, float y) {
            start.set(x, y);
        }

        public void setEnd(float x, float y) {
            end.set(x, y);
        }

        public String toString() {
            return String.format("Line from %s to %s", start, end);
        }
    }

    /**
     * Represents a circle in the 2D sketch.
     */
    public static class Circle extends Entity {
        private final Point center;
        private float r; // Radius is scalar, not a Point

        public Circle(Point center, float r) {
            this.type = TypeSketch.CIRCLE;
            this.center = center;
            this.r = r;
        }

        public Circle(float x, float y, float r) {
            this(new Point(x, y), r);
        }

        public Point getCenterPoint() {
            return center;
        }

        public float getX() {
            return center.x;
        }

        public float getY() {
            return center.y;
        }

        public float getRadius() {
            return r;
        }

        public void setCenter(float x, float y) {
            center.set(x, y);
        }

        public void setRadius(float r) {
            this.r = r;
        }

        public String toString() {
            return String.format("Circle at %s with radius %.3f", center, r);
        }
    }

    /**
     * Represents a closed polygon in the 2D sketch, defined by a list of points.
     */
    public static class Polygon extends Entity {
        List<PointEntity> points;

        public Polygon(List<PointEntity> points) {
            if (points == null || points.size() < 3 || points.size() > 25) {
                throw new IllegalArgumentException("Polygon must have between 3 and 25 points.");
            }
            this.type = TypeSketch.POLYGON;
            this.points = new ArrayList<>(points);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Polygon with points: ");
            for (PointEntity p : points) {
                sb.append(String.format("(%.2f, %.2f) ", p.getX(), p.getY()));
            }
            return sb.toString();
        }

        // Add getPoints() method to Polygon
        public List<Point2D> getPoints() {
            List<Point2D> p2dPoints = new ArrayList<>();
            for (PointEntity p : points) {
                p2dPoints.add(new Point2D(p.getX(), p.getY()));
            }
            return p2dPoints;
        }

        public List<PointEntity> getSketchPoints() {
            return points;
        }
    }

    // New class definitions to resolve errors

    /**
     * Simple class to represent a 2D point for geometric operations.
     * This is distinct from PointEntity which is a Sketch entity.
     */
    public static class Point2D {
        private float x, y;

        public Point2D(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    /**
     * Simple class to represent a 3D point.
     */
    public static class Point3D {
        private float x, y, z;

        public Point3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public float getZ() {
            return z;
        }
    }

    /**
     * Represents a 3D face, defined by a list of 3D points.
     * Assumes points are ordered to define the face (e.g., clockwise or
     * counter-clockwise).
     */
    public static class Face3D {
        private List<Point3D> vertices;
        // List of normals, one per vertex (for smooth shading)
        private List<float[]> vertexNormals;

        // Constructor for a quad face (4 points)
        public Face3D(Point3D p1, Point3D p2, Point3D p3, Point3D p4) {
            this.vertices = new ArrayList<>();
            this.vertices.add(p1);
            this.vertices.add(p2);
            this.vertices.add(p3);
            this.vertices.add(p4);
            this.vertexNormals = null; // To be set later
        }

        // Constructor for a general polygonal face (list of points)
        public Face3D(List<Point3D> vertices) {
            if (vertices == null || vertices.size() < 3) {
                throw new IllegalArgumentException("A Face3D must have at least 3 vertices.");
            }
            this.vertices = new ArrayList<>(vertices);
            this.vertexNormals = null; // To be set later
        }

        public List<Point3D> getVertices() {
            return vertices;
        }

        public void setVertexNormals(List<float[]> normals) {
            this.vertexNormals = normals;
        }

        public List<float[]> getVertexNormals() {
            return vertexNormals;
        }
    }

    /**
     * Computes per-vertex normals for all extruded faces (for smooth shading).
     * This method should be called after extrusion and before uploading to
     * VBOManager.
     * It averages the normals of all faces sharing a vertex.
     */
    /**
     * Computes per-vertex normals for all extruded faces (for smooth shading)
     * utilizing a crease angle threshold to preserve sharp edges.
     */
    public void computePerVertexNormals() {
        // Map from Point3D to list of faces sharing that vertex
        java.util.Map<Point3D, List<Face3D>> vertexToFaces = new java.util.HashMap<>();

        // Cache face normals to avoid re-calculating
        java.util.Map<Face3D, float[]> faceNormals = new java.util.HashMap<>();

        // 1. Build adjacency map and cache face normals
        for (Face3D face : extrudedFaces) {
            List<Point3D> verts = face.getVertices();
            if (verts.size() < 3)
                continue;

            // Calculate normal using first 3 points (assuming planar face)
            float[] normal = calculateFaceNormal(verts.get(0), verts.get(1), verts.get(2));
            faceNormals.put(face, normal);

            for (Point3D v : verts) {
                vertexToFaces.computeIfAbsent(v, k -> new java.util.ArrayList<>()).add(face);
            }
        }

        // Crease threshold: Cosine of 45 degrees
        double creaseThreshold = Math.cos(Math.toRadians(45));

        // 2. Compute vertex normals for each face
        for (Face3D face : extrudedFaces) {
            List<Point3D> verts = face.getVertices();
            List<float[]> vertexNormals = new ArrayList<>();
            float[] currentFaceNormal = faceNormals.get(face);

            if (currentFaceNormal == null) {
                // Fallback
                for (int i = 0; i < verts.size(); i++)
                    vertexNormals.add(new float[] { 0, 0, 1 });
                face.setVertexNormals(vertexNormals);
                continue;
            }

            for (Point3D v : verts) {
                float nx = 0, ny = 0, nz = 0;
                List<Face3D> neighbors = vertexToFaces.get(v);

                if (neighbors != null) {
                    for (Face3D neighbor : neighbors) {
                        float[] nNormal = faceNormals.get(neighbor);
                        if (nNormal == null)
                            continue;

                        // Dot product to check angle
                        float dot = currentFaceNormal[0] * nNormal[0] +
                                currentFaceNormal[1] * nNormal[1] +
                                currentFaceNormal[2] * nNormal[2];

                        // If angle is small (dot product large), blend the normal
                        if (dot > creaseThreshold) {
                            nx += nNormal[0];
                            ny += nNormal[1];
                            nz += nNormal[2];
                        }
                    }
                }

                // Normalize
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 1e-6) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                } else {
                    // Fallback to face normal if degenerate
                    nx = currentFaceNormal[0];
                    ny = currentFaceNormal[1];
                    nz = currentFaceNormal[2];
                }
                vertexNormals.add(new float[] { nx, ny, nz });
            }
            face.setVertexNormals(vertexNormals);
        }
    }
    // End of new class definitions

    private static final int MAX_SKETCH_ENTITIES = 1000;
    private final List<Entity> sketchEntities = new CopyOnWriteArrayList<>();

    // New fields to resolve errors
    public final List<Polygon> polygons = new CopyOnWriteArrayList<>(); // To store only Polygon entities for extrusion
    public List<Face3D> extrudedFaces = new ArrayList<>(); // To store the result of extrusion

    // Dimension support
    private final List<Dimension> dimensions = new ArrayList<>(); // Store all dimensions

    // Constraint support
    private final List<Constraint> constraints = new CopyOnWriteArrayList<>();

    public void addConstraint(Constraint c) {
        constraints.add(c);
        solveConstraints();
    }

    public void solveConstraints() {
        ConstraintSolver.solve(constraints);
    }

    public List<Constraint> getConstraints() {
        return constraints;
    }

    public void removeConstraint(Constraint c) {
        constraints.remove(c);
        solveConstraints();
    }

    // Generic entity management
    public void addEntity(Entity e) {
        if (!sketchEntities.contains(e)) {
            sketchEntities.add(e);
            // Fix: Ensure polygons are added to the dedicated list for extrusion
            if (e instanceof Polygon) {
                polygons.add((Polygon) e);
            }
        }
    }

    // End of new fields

    /**
     * Adds a point entity to the sketch.
     * 
     * @param x X-coordinate of the point.
     * @param y Y-coordinate of the point.
     * @return 0 on success, 1 if sketch buffer is full.
     */
    public int addPoint(float x, float y) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new PointEntity(x, y));
        return 0;
    }

    /**
     * Adds a line entity to the sketch.
     * 
     * @param x1 X-coordinate of the start point.
     * @param y1 Y-coordinate of the start point.
     * @param x2 X-coordinate of the end point.
     * @param y2 Y-coordinate of the end point.
     * @return 0 on success, 1 if sketch buffer is full.
     */
    public int addLine(float x1, float y1, float x2, float y2) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Line(x1, y1, x2, y2));
        return 0;
    }

    /**
     * Adds a circle entity to the sketch.
     * 
     * @param x X-coordinate of the center.
     * @param y Y-coordinate of the center.
     * @param r Radius of the circle.
     * @return 0 on success, 1 if sketch buffer is full.
     */
    public int addCircle(float x, float y, float r) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Circle(x, y, r));
        return 0;
    }

    /**
     * Adds a polygon entity to the sketch from a list of points.
     * 
     * @param points List of PointEntity forming the polygon.
     * @return 0 on success, 1 if sketch buffer is full or invalid points.
     */
    public int addPolygon(List<PointEntity> points) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        try {
            Polygon newPolygon = new Polygon(points);
            sketchEntities.add(newPolygon);
            this.polygons.add(newPolygon); // Also add to the dedicated polygons list
            return 0;
        } catch (IllegalArgumentException e) {
            System.out.println("Error adding polygon: " + e.getMessage());
            return 1;
        }
    }

    public int addNSidedPolygon(float centerX, float centerY, float radius, int sides) {
        return addNSidedPolygon(centerX, centerY, radius, sides, false);
    }

    /**
     * Adds an N-sided regular polygon to the sketch.
     * 
     * @param centerX       X-coordinate of the polygon's center.
     * @param centerY       Y-coordinate of the polygon's center.
     * @param radius        Radius (inscribed or circumscribed depending on flag).
     * @param sides         Number of sides (3-25).
     * @param circumscribed If true, the polygon is circumscribed about the circle
     *                      of
     *                      given radius.
     *                      If false, it is inscribed in the circle.
     * @return 0 on success, 1 on error.
     */
    public int addNSidedPolygon(float centerX, float centerY, float radius, int sides, boolean circumscribed) {
        if (sides < 3 || sides > 25) {
            System.out.println("Polygon must have between 3 and 25 sides.");
            return 1;
        }
        if (radius <= 0) {
            System.out.println("Radius must be positive.");
            return 1;
        }

        double effectiveRadius = radius;
        if (circumscribed) {
            // R_circum = R_inscribed / cos(PI / n)
            effectiveRadius = radius / Math.cos(Math.PI / sides);
        }

        List<PointEntity> points = new ArrayList<>();
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides;
            float x = centerX + (float) (effectiveRadius * Math.cos(angle));
            float y = centerY + (float) (effectiveRadius * Math.sin(angle));
            points.add(new PointEntity(x, y));
        }

        return addPolygon(points);
    }

    /**
     * Adds a kite-shaped quadrilateral to the sketch.
     * 
     * @param centerX       X-coordinate of the kite's center.
     * @param centerY       Y-coordinate of the kite's center.
     * @param mainDiagonal  Length of the main (vertical) diagonal.
     * @param crossDiagonal Length of the cross (horizontal) diagonal.
     * @param angleDegrees  Angle (in degrees) of the main diagonal with respect to
     *                      the X-axis.
     * @return 0 on success, 1 on error.
     */
    public int addKite(float centerX, float centerY, float mainDiagonal, float crossDiagonal, float angleDegrees) {
        if (mainDiagonal <= 0 || crossDiagonal <= 0) {
            System.out.println("Diagonals must be positive.");
            return 1;
        }
        double angleRad = Math.toRadians(angleDegrees);
        float dxMain = (float) (Math.cos(angleRad) * mainDiagonal / 2.0);
        float dyMain = (float) (Math.sin(angleRad) * mainDiagonal / 2.0);
        float dxCross = (float) (-Math.sin(angleRad) * crossDiagonal / 2.0);
        float dyCross = (float) (Math.cos(angleRad) * crossDiagonal / 2.0);
        // Four vertices: top, right, bottom, left (in order)
        PointEntity p1 = new PointEntity(centerX + dxMain, centerY + dyMain); // tip of main diagonal (top)
        PointEntity p2 = new PointEntity(centerX + dxCross, centerY + dyCross); // tip of cross diagonal (right)
        PointEntity p3 = new PointEntity(centerX - dxMain, centerY - dyMain); // other tip of main diagonal (bottom)
        PointEntity p4 = new PointEntity(centerX - dxCross, centerY - dyCross); // other tip of cross diagonal (left)
        List<PointEntity> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);
        points.add(p3);
        points.add(p4);
        int result = addPolygon(points);
        // Add spine (main diagonal)
        addLine(p1.getX(), p1.getY(), p3.getX(), p3.getY());
        // Add crossbar (cross diagonal)
        addLine(p2.getX(), p2.getY(), p4.getX(), p4.getY());
        // Add tail (from bottom vertex p3 downward, with bows)
        float tailLength = mainDiagonal * 0.7f;
        float tailAngle = (float) (angleRad + Math.PI); // tail points away from main diagonal
        float tailEndX = p3.getX() + (float) (Math.cos(tailAngle) * tailLength);
        float tailEndY = p3.getY() + (float) (Math.sin(tailAngle) * tailLength);
        addLine(p3.getX(), p3.getY(), tailEndX, tailEndY);
        // Add 3 bows along the tail
        for (int i = 1; i <= 3; i++) {
            float t = i / 4.0f;
            float bx = p3.getX() + (float) (Math.cos(tailAngle) * tailLength * t);
            float by = p3.getY() + (float) (Math.sin(tailAngle) * tailLength * t);
            float bowSize = mainDiagonal * 0.07f;
            // Draw a small X for each bow
            addLine(bx - bowSize, by - bowSize, bx + bowSize, by + bowSize);
            addLine(bx - bowSize, by + bowSize, bx + bowSize, by - bowSize);
        }
        // Add bridle lines (from p1 and p2 to a point in front of the kite)
        float bridleLength = mainDiagonal * 0.5f;
        float bridleAngle = (float) (angleRad - Math.PI / 2.0); // in front of kite
        float bridleX = centerX + (float) (Math.cos(bridleAngle) * bridleLength);
        float bridleY = centerY + (float) (Math.sin(bridleAngle) * bridleLength);
        addLine(p1.getX(), p1.getY(), bridleX, bridleY);
        addLine(p2.getX(), p2.getY(), bridleX, bridleY);
        return result;
    }

    /**
     * Clears all entities from the sketch.
     */
    /**
     * Clears 2D entities from the sketch but PRESERVES extruded 3D geometry.
     * This allows for multi-step macros where we sketch, extrude, clear sketch, and
     * sketch again.
     */
    public void clearSketch() {
        sketchEntities.clear();
        polygons.clear(); // Clear polygons list as well
        dimensions.clear(); // Clear dimensions
        // extrudedFaces.clear(); // DO NOT CLEAR 3D FACES here
    }

    /**
     * Clears EVERYTHING including 3D geometry.
     */
    public void clearAll() {
        clearSketch();
        extrudedFaces.clear();
    }

    /**
     * Returns a list of string representations of all entities in the sketch.
     * 
     * @return List of strings describing each sketch entity.
     */
    public List<String> listSketch() {
        List<String> output = new ArrayList<>();

        if (sketchEntities.isEmpty()) {
            output.add("Sketch is empty.");
            return output;
        }

        for (Entity e : sketchEntities) {
            output.add(e.toString());
        }

        return output;
    }

    /**
     * Gets all entities in the sketch.
     * 
     * @return List of all sketch entities.
     */
    public List<Entity> getEntities() {
        return new ArrayList<>(sketchEntities);
    }

    /**
     * Removes an entity from the sketch.
     * Used for undo operations.
     * 
     * @param entity The entity to remove
     * @return true if removed, false if not found
     */
    /**
     * Removes an entity from the sketch.
     * Used for undo operations.
     * 
     * @param entity The entity to remove
     * @return true if removed, false if not found
     */
    public boolean removeEntity(Entity entity) {
        boolean removed = sketchEntities.remove(entity);
        if (removed && entity instanceof Polygon) {
            polygons.remove((Polygon) entity);
        }
        return removed;
    }

    /**
     * Checks if the sketch contains extrudable shapes.
     * Extrudable shapes include polygons and circles.
     * 
     * @return true if sketch contains extrudable shapes (polygons or circles),
     *         false otherwise.
     */
    public boolean isClosedLoop() {
        for (Entity entity : sketchEntities) {
            if (entity instanceof Polygon || entity instanceof Circle) {
                return true;
            }
        }
        return false;
    }

    private UnitSystem unitSystem = UnitSystem.MMGS; // Default to MMGS

    /**
     * Sets the units for the sketch.
     * 
     * @param units String representing the units (e.g., "mm", "in").
     */
    /**
     * Sets the unit system for the sketch.
     * 
     * @param unitSystem The new unit system.
     */
    public void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    /**
     * Gets the current unit system.
     * 
     * @return Current UnitSystem.
     */
    public UnitSystem getUnitSystem() {
        return this.unitSystem;
    }

    /**
     * Converts a DXF unit code to its corresponding unit string.
     * 
     * @param code The DXF unit code.
     * @return The unit string.
     */
    private static String getUnitsFromDXFCode(int code) {
        return switch (code) {
            case 1 -> "in";
            case 2 -> "ft";
            case 4 -> "mm";
            case 5 -> "cm";
            case 6 -> "m";
            default -> "unitless";
        };
    }

    /**
     * Exports the current sketch to a DXF file.
     * It writes HEADER and ENTITIES sections, including unit information.
     *
     * @param filename Path to the output DXF file.
     */
    public void exportSketchToDXF(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("HEADER");

            out.println("9");
            out.println("$INSUNITS"); // Insertion units system
            out.println("70");
            out.println(this.unitSystem.getDXFCode());

            out.println("0");
            out.println("ENDSEC");

            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("ENTITIES");

            for (Entity e : sketchEntities) {
                if (e instanceof PointEntity) {
                    PointEntity p = (PointEntity) e;
                    out.println("0");
                    out.println("POINT");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("10");
                    out.println(p.getX());
                    out.println("20");
                    out.println(p.getY());
                } else if (e instanceof Line l) {
                    out.println("0");
                    out.println("LINE");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("10");
                    out.println(l.getX1());
                    out.println("20");
                    out.println(l.getY1());
                    out.println("11");
                    out.println(l.getX2());
                    out.println("21");
                    out.println(l.getY2());
                } else if (e instanceof Circle c) {
                    out.println("0");
                    out.println("CIRCLE");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("10");
                    out.println(c.getX());
                    out.println("20");
                    out.println(c.getY());
                    out.println("40");
                    out.println(c.getRadius());
                } else if (e instanceof Polygon poly) {
                    out.println("0");
                    out.println("POLYLINE");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("66"); // Entities follow (for old-style polylines)
                    out.println("1");
                    out.println("70"); // Polyline flags
                    out.println("1"); // Flag 1 for closed polyline (if appropriate, 0 for open)

                    for (PointEntity p : poly.points) {
                        out.println("0");
                        out.println("VERTEX");
                        out.println("8");
                        out.println("0"); // Layer "0"
                        out.println("10");
                        out.println(p.getX());
                        out.println("20");
                        out.println(p.getY());
                    }

                    out.println("0");
                    out.println("SEQEND"); // Marks the end of polyline vertices
                    out.println("8");
                    out.println("0"); // Layer "0" for SEQEND
                }
            }

            out.println("0");
            out.println("ENDSEC");
            out.println("0");
            out.println("EOF");

            System.out.println("Sketch exported to " + filename);
        } catch (IOException e) {
            System.out.println("Error exporting DXF: " + e.getMessage());
        }
    }

    /**
     * Parses parameters for adding a point from command-line like input.
     * 
     * @param params String array of parameters (x, y).
     * @return 0 on success, 1 on error.
     */
    public int sketchPoint(String[] params) {
        if (params.length < 2) {
            System.out.println("Usage: point x y");
            return 1;
        }
        try {
            float x = Float.parseFloat(params[0]);
            float y = Float.parseFloat(params[1]);
            return addPoint(x, y);
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinates for point.");
            return 1;
        }
    }

    /**
     * Parses parameters for adding a line from command-line like input.
     * 
     * @param params String array of parameters (x1, y1, x2, y2).
     * @return 0 on success, 1 on error.
     */
    public int sketchLine(String[] params) {
        if (params.length < 4) {
            System.out.println("Usage: line x1 y1 x2 y2");
            return 1;
        }
        try {
            float x1 = Float.parseFloat(params[0]);
            float y1 = Float.parseFloat(params[1]);
            float x2 = Float.parseFloat(params[2]);
            float y2 = Float.parseFloat(params[3]);
            return addLine(x1, y1, x2, y2);
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinates for line.");
            return 1;
        }
    }

    /**
     * Parses parameters for adding a circle from command-line like input.
     * 
     * @param params String array of parameters (x, y, r).
     * @return 0 on success, 1 on error.
     */
    public int sketchCircle(String[] params) {
        if (params.length < 3) {
            System.out.println("Usage: circle x y r");
            return 1;
        }
        try {
            float x = Float.parseFloat(params[0]);
            float y = Float.parseFloat(params[1]);
            float r = Float.parseFloat(params[2]);
            return addCircle(x, y, r);
        } catch (NumberFormatException e) {
            System.out.println("Invalid parameters for circle.");
            return 1;
        }
    }

    /**
     * Adds an N-sided polygon to the sketch based on provided parameters.
     * This method directly accepts float coordinates and int sides.
     * 
     * @param x      X-coordinate of the center.
     * @param y      Y-coordinate of the center.
     * @param radius Radius of the circumcircle.
     * @param sides  Number of sides (3-25).
     * @return 0 on success, 1 on error.
     */
    public int sketchPolygon(float x, float y, float radius, int sides) {
        if (sides < 3 || sides > 25) {
            System.out.println("Polygon must have between 3 and 25 sides.");
            return 1;
        }
        if (radius <= 0) {
            System.out.println("Radius must be positive.");
            return 1;
        }

        List<PointEntity> points = new ArrayList<>();
        double angleStep = 2 * Math.PI / sides;

        for (int i = 0; i < sides; i++) {
            double angle = i * angleStep;
            float px = x + (float) (radius * Math.cos(angle));
            float py = y + (float) (radius * Math.sin(angle));
            points.add(new PointEntity(px, py));
        }

        return addPolygon(points);
    }

    /**
     * Loads a DXF file, parses its header for units, and then its ENTITIES section
     * to populate the sketch with points, lines, circles, and polylines.
     * Includes robust error handling and unit scaling.
     *
     * @param filename Path to the DXF file to load.
     * @throws IOException if file reading fails.
     */
    public void loadDXF(String filename) throws IOException {
        System.out.println("Attempting to load DXF file: " + filename);
        java.io.File file = new java.io.File(filename);
        System.out.println("Absolute path of file provided: " + file.getAbsolutePath());
        if (!file.exists()) {
            System.err.println("ERROR: File does not exist at path: " + file.getAbsolutePath());
            throw new java.io.FileNotFoundException("DXF file not found: " + filename);
        }
        if (!file.canRead()) {
            System.err.println("ERROR: No read permission for file: " + file.getAbsolutePath());
            throw new IOException("No read permission for DXF file: " + filename);
        }

        clearSketch();
        // this.units = "unitless"; // Reset units handled by caller or kept as is

        // Single pass: Read both header and entities
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntity = null;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List<PointEntity> polyPoints = null;
            float tempVertexX = 0, tempVertexY = 0;

            boolean inHeaderSection = false;
            boolean inEntitiesSection = false;
            boolean inPolylineEntity = false; // Flag to track if we are currently parsing a POLYLINE's vertices
            boolean waitingForVertexCoords = false; // Flag to indicate that the next 10/20 group codes are for a vertex

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty())
                    continue;

                if (line.equalsIgnoreCase("0")) {
                    // This is a new entity or section start/end
                    String entityTypeOrSection = reader.readLine();
                    if (entityTypeOrSection == null) {
                        System.err.println("ERROR: Unexpected EOF after group code 0.");
                        break;
                    }
                    entityTypeOrSection = entityTypeOrSection.trim().toUpperCase();

                    // Handle SECTION starts
                    if (entityTypeOrSection.equals("SECTION")) {
                        reader.readLine(); // Group code 2
                        String sectionName = reader.readLine();
                        if (sectionName != null) {
                            if (sectionName.equalsIgnoreCase("HEADER")) {
                                inHeaderSection = true;
                                inEntitiesSection = false;
                            } else if (sectionName.equalsIgnoreCase("ENTITIES")) {
                                inHeaderSection = false;
                                inEntitiesSection = true;
                                // Calculate scale now that we've read the header
                                // float scale = unitScaleFactor(units); // TODO: Implement if import scaling
                                // needed
                                // System.out.println("Starting DXF entity parsing with units: " + units + "
                                // (scale factor: " + scale + ")");
                            } else {
                                inHeaderSection = false;
                                inEntitiesSection = false;
                            }
                        }
                        continue;
                    } else if (entityTypeOrSection.equals("ENDSEC")) {
                        if (inPolylineEntity && polyPoints != null) { // Ensure any ongoing polyline is added
                            addPolygon(polyPoints);
                            polyPoints = null;
                            inPolylineEntity = false;
                        }
                        inHeaderSection = false;
                        inEntitiesSection = false; // Exit current section
                        continue;
                    } else if (entityTypeOrSection.equals("EOF")) {
                        // Handle any last entity before EOF
                        if (currentEntity != null && !inPolylineEntity && !currentEntity.equals("VERTEX")
                                && !currentEntity.equals("SKIP_VERTEX") && !currentEntity.equals("SEQEND")) {
                            switch (currentEntity) {
                                case "POINT":
                                    addEntity("POINT", x1, y1, 0, 0, 0, 0, 0, null);
                                    break;
                                case "LINE":
                                    addEntity("LINE", x1, y1, x2, y2, 0, 0, 0, null);
                                    break;
                                case "CIRCLE":
                                    addEntity("CIRCLE", 0, 0, 0, 0, cx, cy, radius, null);
                                    break;
                            }
                        }
                        break; // End of file
                    }

                    // If we were parsing a non-POLYLINE entity, add it now before processing the
                    // new one
                    if (currentEntity != null && !inPolylineEntity && !currentEntity.equals("VERTEX")
                            && !currentEntity.equals("SKIP_VERTEX") && !currentEntity.equals("SEQEND")) { // Exclude
                                                                                                          // VERTEX,
                                                                                                          // SKIP_VERTEX,
                                                                                                          // and SEQEND
                        switch (currentEntity) {
                            case "POINT":
                                addEntity("POINT", x1, y1, 0, 0, 0, 0, 0, null);
                                break;
                            case "LINE":
                                addEntity("LINE", x1, y1, x2, y2, 0, 0, 0, null);
                                break;
                            case "CIRCLE":
                                addEntity("CIRCLE", 0, 0, 0, 0, cx, cy, radius, null);
                                break;
                            // POLYLINE and VERTEX are handled by SEQEND or when currentEntity changes
                        }
                        currentEntity = null; // Reset after processing the entity
                    }

                    if (inEntitiesSection) {
                        currentEntity = entityTypeOrSection; // Set the type of the new entity
                        // Reset coordinates for new entity
                        x1 = y1 = x2 = y2 = cx = cy = radius = 0;

                        if (currentEntity.equals("POLYLINE")) {
                            inPolylineEntity = true;
                            polyPoints = new ArrayList<>(); // Initialize points list for this polyline
                            // Consume the next group codes for POLYLINE if any (e.g., 66, 70)
                            // Loop continues to read properties of POLYLINE or its VERTEX entities
                        } else if (currentEntity.equals("VERTEX")) {
                            // This VERTEX is encountered directly, likely part of an already active
                            // POLYLINE
                            if (!inPolylineEntity) {
                                System.err.println("Warning: Found VERTEX entity outside of POLYLINE. Skipping.");
                                // We need to skip all the group codes for this VERTEX until the next "0" group
                                // code
                                // by setting a flag and not setting currentEntity to null immediately
                                currentEntity = "SKIP_VERTEX"; // Special marker to skip this entity's data
                            } else {
                                waitingForVertexCoords = true; // Set flag to expect 10, 20
                            }
                        } else if (currentEntity.equals("SEQEND")) {
                            // End of POLYLINE sequence
                            if (inPolylineEntity && polyPoints != null) {
                                addPolygon(polyPoints); // Add the accumulated polygon
                                // System.out.println("DBG: Finished POLYLINE, added " + polyPoints.size() + "
                                // points.");
                            } else {
                                System.err.println("Warning: SEQEND encountered without active POLYLINE.");
                            }
                            polyPoints = null; // Clear for next polyline
                            inPolylineEntity = false; // Exit polyline parsing state
                            // Keep currentEntity as "SEQEND" so we can consume its group codes (like layer)
                            // currentEntity will be reset when we encounter the next "0" group code
                        }
                    }
                    continue; // Skip to next line after processing a '0' group code
                }

                // Handle header variable parsing
                if (inHeaderSection && line.equalsIgnoreCase("9")) {
                    String varName = reader.readLine();
                    if (varName != null && varName.equalsIgnoreCase("$INSUNITS")) {
                        reader.readLine(); // Group code 70
                        String valueLine = reader.readLine();
                        if (valueLine != null) {
                            try {
                                int code = Integer.parseInt(valueLine.trim());
                                this.unitSystem = UnitSystem.fromDXFCode(code);
                                System.out.println(
                                        "DXF Header Units: " + this.unitSystem.name() + " (Code: " + code + ")");
                            } catch (NumberFormatException e) {
                                System.err.println(
                                        "Warning: Invalid $INSUNITS code in DXF header. Using default units. Error: "
                                                + e.getMessage());
                            }
                        }
                    }
                    continue;
                }

                // If we are inside an entity definition (not a '0' group code line)
                if (currentEntity != null && inEntitiesSection) {
                    float scale = 1.0f; // Scale handled by UnitSystem logic if needed later
                    String valueLine = null;
                    try {
                        // Parse the group code - it should be a number
                        int groupCode = Integer.parseInt(line);
                        valueLine = reader.readLine();
                        if (valueLine == null) {
                            System.err.println("ERROR: Unexpected EOF after group code " + groupCode);
                            break;
                        }
                        valueLine = valueLine.trim();

                        // If we're skipping this entity, just consume the group code/value pair and
                        // continue
                        if (currentEntity.equals("SKIP_VERTEX") || currentEntity.equals("SEQEND")) {
                            continue; // Skip processing this group code/value pair
                        }

                        switch (groupCode) {
                            case 10: // X coordinate of start point (POINT, LINE) or center (CIRCLE) or vertex
                                     // (VERTEX)
                                if (currentEntity.equals("VERTEX") && waitingForVertexCoords) {
                                    tempVertexX = Float.parseFloat(valueLine) * scale;
                                } else {
                                    if (currentEntity.equals("POINT") || currentEntity.equals("LINE"))
                                        x1 = Float.parseFloat(valueLine) * scale;
                                    else if (currentEntity.equals("CIRCLE"))
                                        cx = Float.parseFloat(valueLine) * scale;
                                }
                                break;
                            case 20: // Y coordinate of start point (POINT, LINE) or center (CIRCLE) or vertex
                                     // (VERTEX)
                                if (currentEntity.equals("VERTEX") && waitingForVertexCoords) {
                                    tempVertexY = Float.parseFloat(valueLine) * scale;
                                    // If we have both X and Y for a vertex, add it to the polyline
                                    if (polyPoints != null) {
                                        polyPoints.add(new PointEntity(tempVertexX, tempVertexY));
                                    }
                                    waitingForVertexCoords = false; // Reset for next vertex
                                } else {
                                    if (currentEntity.equals("POINT") || currentEntity.equals("LINE"))
                                        y1 = Float.parseFloat(valueLine) * scale;
                                    else if (currentEntity.equals("CIRCLE"))
                                        cy = Float.parseFloat(valueLine) * scale;
                                }
                                break;
                            case 11: // X coordinate of end point (LINE)
                                if (currentEntity.equals("LINE"))
                                    x2 = Float.parseFloat(valueLine) * scale;
                                break;
                            case 21: // Y coordinate of end point (LINE)
                                if (currentEntity.equals("LINE"))
                                    y2 = Float.parseFloat(valueLine) * scale;
                                break;
                            case 40: // Radius (CIRCLE) or sometimes thickness for other entities, or start/end width
                                     // for POLYLINE
                                if (currentEntity.equals("CIRCLE"))
                                    radius = Float.parseFloat(valueLine) * scale;
                                break;
                            case 8: // Layer name (usually '0') - consume
                            case 6: // Linetype name - consume
                            case 62: // Color number - consume
                            case 39: // Thickness - consume
                            case 70: // Polyline flags (e.g., 1 for closed) - consume
                            case 66: // Entities follow (for old POLYLINEs) - consume
                                // These are common group codes to consume
                                break;
                            default:
                                // System.out.println("DBG: Skipping unsupported group code: " + groupCode);
                                break; // Skip unsupported group codes
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number format for DXF value near group code: " + line
                                + ", value: " + valueLine + ". Error: " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Error parsing DXF line '" + line + "': " + e.getMessage());
                        e.printStackTrace();
                        break; // Stop parsing on critical error
                    }
                }
            } // end of while (line = reader.readLine()) != null

            // After loop, if there was an active non-polyline entity
            if (currentEntity != null && !inPolylineEntity && !currentEntity.equals("VERTEX")
                    && !currentEntity.equals("SKIP_VERTEX") && !currentEntity.equals("SEQEND")) {
                switch (currentEntity) {
                    case "POINT":
                        addEntity("POINT", x1, y1, 0, 0, 0, 0, 0, null);
                        break;
                    case "LINE":
                        addEntity("LINE", x1, y1, x2, y2, 0, 0, 0, null);
                        break;
                    case "CIRCLE":
                        addEntity("CIRCLE", 0, 0, 0, 0, cx, cy, radius, null);
                        break;
                }
            }
            // Also, if a polyline was active but didn't end with SEQEND (malformed file)
            if (inPolylineEntity && polyPoints != null && !polyPoints.isEmpty()) {
                System.err.println("Warning: POLYLINE did not end with SEQEND. Adding partially parsed polyline.");
                addPolygon(polyPoints);
            }

        } catch (IOException e) {
            System.err.println("Error reading DXF entities: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to indicate failure
        }
        System.out.println("Finished loading DXF. Entities loaded: " + sketchEntities.size());
    }

    /**
     * Helper method to add parsed entities to the sketchEntities list.
     * Used by `loadDXF` to centralize entity creation.
     * 
     * @param type       The type of entity as a string ("POINT", "LINE", "CIRCLE",
     *                   "POLYLINE").
     * @param x1         Start X-coordinate or Point X.
     * @param y1         Start Y-coordinate or Point Y.
     * @param x2         End X-coordinate (for Line).
     * @param y2         End Y-coordinate (for Line).
     * @param cx         Center X-coordinate (for Circle).
     * @param cy         Center Y-coordinate (for Circle).
     * @param radius     Radius (for Circle).
     * @param polyPoints List of points for a Polygon (null for other types).
     */
    private void addEntity(String type, float x1, float y1, float x2, float y2, float cx, float cy, float radius,
            List<PointEntity> polyPoints) {
        switch (type) {
            case "POINT":
                addPoint(x1, y1);
                break;
            case "LINE":
                addLine(x1, y1, x2, y2);
                break;
            case "CIRCLE":
                addCircle(cx, cy, radius);
                break;
            case "POLYLINE": // This case handles the final addition of the polygon after all vertices are
                             // parsed
                if (polyPoints != null) {
                    addPolygon(polyPoints);
                }
                break;
            // VERTEX and SEQEND are control entities for POLYLINE, not standalone entities
            // to be added here.
            default:
                System.out.println("Unsupported DXF entity type for adding: " + type);
        }
    }

    /**
     * Provides a scaling factor to convert DXF units to an internal standard unit
     * (e.g., millimeters).
     * 
     * @param unitStr The unit string from DXF header.
     * @return The conversion factor.
     */
    private float unitScaleFactor(String unitStr) {
        // Define your internal base unit, e.g., millimeters.
        // Convert all DXF units to your internal unit.
        return switch (unitStr.toLowerCase()) {
            case "in" -> 25.4f; // 1 inch = 25.4 mm
            case "ft" -> 304.8f; // 1 foot = 304.8 mm
            case "mm" -> 1.0f;
            case "cm" -> 10.0f; // 1 cm = 10 mm
            case "m" -> 1000.0f; // 1 m = 1000 mm
            default -> 1.0f; // Unitless or unknown, assume 1:1
        };
    }

    /**
     * Renders extruded 3D faces using OpenGL.
     * This method renders the 3D geometry created by the extrude operation.
     * It converts Face3D objects to triangles and renders them with proper
     * lighting.
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    public void draw3D(GL2 gl) {
        if (extrudedFaces.isEmpty()) {
            return; // Nothing to render
        }

        // Set material properties for extruded geometry
        float[] materialAmbient = { 0.2f, 0.4f, 0.6f, 1.0f }; // Blue-ish ambient
        float[] materialDiffuse = { 0.4f, 0.6f, 0.8f, 1.0f }; // Blue-ish diffuse
        float[] materialSpecular = { 0.8f, 0.8f, 0.8f, 1.0f }; // White specular
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, materialAmbient, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, materialDiffuse, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, materialSpecular, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 60.0f);

        // Render each face
        for (Face3D face : extrudedFaces) {
            renderFace3D(gl, face);
        }
    }

    /**
     * Renders a single Face3D object as triangles.
     * Uses triangle fan tessellation for faces with more than 3 vertices.
     *
     * @param gl   The GL2 object for OpenGL rendering.
     * @param face The Face3D object to render.
     */
    private void renderFace3D(GL2 gl, Face3D face) {
        List<Point3D> vertices = face.getVertices();
        int numVertices = vertices.size();

        if (numVertices < 3) {
            return; // Cannot render face with less than 3 vertices
        }

        if (numVertices == 3) {
            // Triangle - render directly
            gl.glBegin(GL2.GL_TRIANGLES);

            Point3D p1 = vertices.get(0);
            Point3D p2 = vertices.get(1);
            Point3D p3 = vertices.get(2);

            // Calculate and set normal
            float[] normal = calculateFaceNormal(p1, p2, p3);
            gl.glNormal3f(normal[0], normal[1], normal[2]);

            gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
            gl.glVertex3f(p2.getX(), p2.getY(), p2.getZ());
            gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());

            gl.glEnd();
        } else if (numVertices == 4) {
            // Quad - render as two triangles
            gl.glBegin(GL2.GL_TRIANGLES);

            Point3D p1 = vertices.get(0);
            Point3D p2 = vertices.get(1);
            Point3D p3 = vertices.get(2);
            Point3D p4 = vertices.get(3);

            // First triangle: p1, p2, p3
            float[] normal1 = calculateFaceNormal(p1, p2, p3);
            gl.glNormal3f(normal1[0], normal1[1], normal1[2]);
            gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
            gl.glVertex3f(p2.getX(), p2.getY(), p2.getZ());
            gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());

            // Second triangle: p1, p3, p4
            float[] normal2 = calculateFaceNormal(p1, p3, p4);
            gl.glNormal3f(normal2[0], normal2[1], normal2[2]);
            gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
            gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());
            gl.glVertex3f(p4.getX(), p4.getY(), p4.getZ());

            gl.glEnd();
        } else {
            // General polygon - use triangle fan
            gl.glBegin(GL2.GL_TRIANGLES);

            Point3D center = vertices.get(0); // Use first vertex as fan center

            for (int i = 1; i < numVertices - 1; i++) {
                Point3D p1 = center;
                Point3D p2 = vertices.get(i);
                Point3D p3 = vertices.get(i + 1);

                // Calculate and set normal for this triangle
                float[] normal = calculateFaceNormal(p1, p2, p3);
                gl.glNormal3f(normal[0], normal[1], normal[2]);

                gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
                gl.glVertex3f(p2.getX(), p2.getY(), p2.getZ());
                gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());
            }

            gl.glEnd();
        }
    }

    /**
     * Calculates the normal vector for a triangular face defined by three 3D
     * points.
     * Uses the cross product of two edge vectors.
     *
     * @param p1 First vertex of the triangle.
     * @param p2 Second vertex of the triangle.
     * @param p3 Third vertex of the triangle.
     * @return Normal vector as a float array [nx, ny, nz].
     */
    private float[] calculateFaceNormal(Point3D p1, Point3D p2, Point3D p3) {
        // Calculate two edge vectors
        float ex1 = p2.getX() - p1.getX();
        float ey1 = p2.getY() - p1.getY();
        float ez1 = p2.getZ() - p1.getZ();

        float ex2 = p3.getX() - p1.getX();
        float ey2 = p3.getY() - p1.getY();
        float ez2 = p3.getZ() - p1.getZ();

        // Calculate cross product
        float nx = ey1 * ez2 - ez1 * ey2;
        float ny = ez1 * ex2 - ex1 * ez2;
        float nz = ex1 * ey2 - ey1 * ex2;

        // Normalize the normal vector
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.001f) { // Avoid division by zero
            nx /= length;
            ny /= length;
            nz /= length;
        } else {
            // Default normal if calculation fails
            nx = 0.0f;
            ny = 0.0f;
            nz = 1.0f;
        }

        return new float[] { nx, ny, nz };
    }

    /**
     * Renders all entities in the sketch using OpenGL.
     * This method is designed to be called by JOGLCadCanvas's `display` method
     * when the 2D sketch view is active. It iterates through the `sketchEntities`
     * list and uses OpenGL primitives to draw each type of entity.
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    public void draw(GL2 gl) {
        // Iterate through all sketch entities and draw them
        for (Entity e : sketchEntities) {
            switch (e.type) {
                case POINT:
                    PointEntity p = (PointEntity) e;
                    gl.glPointSize(5.0f); // Make points visible
                    gl.glBegin(GL2.GL_POINTS);
                    try {
                        gl.glVertex2f(p.getX(), p.getY());
                    } finally {
                        gl.glEnd();
                    }
                    break;
                case LINE:
                    Line l = (Line) e;
                    gl.glBegin(GL2.GL_LINES);
                    try {
                        gl.glVertex2f(l.getX1(), l.getY1());
                        gl.glVertex2f(l.getX2(), l.getY2());
                    } finally {
                        gl.glEnd();
                    }
                    break;
                case CIRCLE:
                    Circle c = (Circle) e;
                    gl.glBegin(GL2.GL_LINE_LOOP);
                    try {
                        int segments = 50; // Resolution of the circle
                        for (int i = 0; i < segments; i++) {
                            double angle = 2.0 * Math.PI * i / segments;
                            float x = c.getX() + c.getRadius() * (float) Math.cos(angle);
                            float y = c.getY() + c.getRadius() * (float) Math.sin(angle);
                            gl.glVertex2f(x, y);
                        }
                    } finally {
                        gl.glEnd();
                    }
                    break;
                case POLYGON:
                    Polygon poly = (Polygon) e;
                    gl.glBegin(GL2.GL_LINE_LOOP); // Draw as a closed loop
                    try {
                        if (poly.points != null) {
                            for (PointEntity vert : poly.points) {
                                if (vert != null) {
                                    gl.glVertex2f(vert.getX(), vert.getY());
                                }
                            }
                        }
                    } finally {
                        gl.glEnd();
                    }
                    break;
            }
        }

        // Draw dimensions
        for (Dimension dim : dimensions) {
            if (dim != null) {
                dim.draw(gl);
            }
        }
    }

    /**
     * Finds the closest sketch entity to a given point within a threshold.
     * Used for entity selection when dimensioning.
     * 
     * @param x         Target X coordinate
     * @param y         Target Y coordinate
     * @param threshold Maximum distance to consider
     * @return The closest entity, or null if none within threshold
     */
    public Entity findClosestEntity(float x, float y, float threshold) {
        Entity closest = null;
        float minDistance = threshold;

        for (Entity entity : sketchEntities) {
            float distance = calculateDistanceToEntity(entity, x, y);
            if (distance < minDistance) {
                minDistance = distance;
                closest = entity;
            }
        }

        return closest;
    }

    /**
     * Calculates the distance from a point to an entity.
     */
    private float calculateDistanceToEntity(Entity entity, float x, float y) {
        if (entity instanceof Line) {
            Line line = (Line) entity;
            return distanceToLineSegment(x, y, line.getX1(), line.getY1(), line.getX2(), line.getY2());
        } else if (entity instanceof Circle) {
            Circle circle = (Circle) entity;
            float dx = x - circle.getX();
            float dy = y - circle.getY();
            float distToCenter = (float) Math.sqrt(dx * dx + dy * dy);
            return Math.abs(distToCenter - circle.getRadius()); // Distance to circumference
        } else if (entity instanceof PointEntity) {
            PointEntity point = (PointEntity) entity;
            float dx = x - point.getX();
            float dy = y - point.getY();
            return (float) Math.sqrt(dx * dx + dy * dy);
        } else if (entity instanceof Polygon) {
            // For polygons, find closest edge
            Polygon poly = (Polygon) entity;
            float minDist = Float.MAX_VALUE;
            List<PointEntity> pts = poly.points;
            for (int i = 0; i < pts.size(); i++) {
                PointEntity p1 = pts.get(i);
                PointEntity p2 = pts.get((i + 1) % pts.size());
                float dist = distanceToLineSegment(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                minDist = Math.min(minDist, dist);
            }
            return minDist;
        }
        return Float.MAX_VALUE;
    }

    /**
     * Calculates distance from a point to a line segment.
     */
    private float distanceToLineSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSquared = dx * dx + dy * dy;

        if (lenSquared == 0) {
            // Line is a point
            return (float) Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // Parameter t for closest point on line segment [0, 1]
        float t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lenSquared));

        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;

        return (float) Math.sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY));
    }

    /**
     * Creates an appropriate dimension for the given entity.
     * 
     * @param entity The entity to dimension
     * @return A dimension object, or null if entity type not supported
     */
    public Dimension createDimensionFor(Entity entity) {
        String unit = unitSystem.getAbbreviation();

        if (entity instanceof Line) {
            Line line = (Line) entity;
            return new LinearDimension(line.getX1(), line.getY1(), line.getX2(), line.getY2(), unit);
        } else if (entity instanceof Circle) {
            Circle circle = (Circle) entity;
            // Default to radius (user can configure later)
            return new RadialDimension(circle.getX(), circle.getY(), circle.getRadius(), false, unit);
        } else if (entity instanceof Polygon) {
            // Dimension the first edge of the polygon
            Polygon poly = (Polygon) entity;
            if (poly.points.size() >= 2) {
                PointEntity p1 = poly.points.get(0);
                PointEntity p2 = poly.points.get(1);
                return new LinearDimension(p1.getX(), p1.getY(), p2.getX(), p2.getY(), unit);
            }
        }
        return null;
    }

    /**
     * Adds a dimension to the sketch.
     */
    public void addDimension(Dimension dim) {
        if (dim != null) {
            dimensions.add(dim);
        }
    }

    /**
     * Removes a dimension from the sketch.
     */
    public void removeDimension(Dimension dim) {
        dimensions.remove(dim);
    }

    /**
     * Gets all dimensions in the sketch.
     */
    public List<Dimension> getDimensions() {
        return new ArrayList<>(dimensions);
    }

    /**
     * Clears all dimensions from the sketch.
     */
    public void clearDimensions() {
        dimensions.clear();
    }

    /**
     * Extrudes all closed shapes in the sketch to create 3D faces.
     * Handles polygons, circles, and potentially connected line loops.
     * Creates side faces, top faces, and bottom faces for each extrudable shape.
     * 
     * @param height The height of the extrusion in the Z direction.
     */
    public void extrude(double height) {
        // Clear previous extruded faces
        // this.extrudedFaces.clear(); // REMOVED: Do not clear faces to allow
        // multi-step macros

        // Extrude existing polygons (POLYLINE entities from DXF)
        for (Polygon polygon : this.polygons) {
            extrudePolygon(polygon, height);
        }

        // Extrude circles as cylindrical shapes
        for (Entity entity : sketchEntities) {
            if (entity instanceof Circle) {
                Circle circle = (Circle) entity;
                extrudeCircle(circle, height);
            }
        }

        // Detect and extrude closed loops formed by connected lines
        List<List<Point2D>> closedLoops = findClosedLoopsFromLines();
        for (List<Point2D> loop : closedLoops) {
            extrudeClosedLoop(loop, height);
        }

        // Extrude all individual lines as thin plates (for kite features, etc.)
        float plateWidth = 0.05f; // Thin plate width (adjust as needed)
        for (Entity entity : sketchEntities) {
            if (entity instanceof Line) {
                Line line = (Line) entity;
                // Compute direction vector
                float dx = line.getX2() - line.getX1();
                float dy = line.getY2() - line.getY1();
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length < 1e-6f)
                    continue; // Skip degenerate lines
                // Normalized perpendicular vector (for plate width)
                float nx = -dy / length;
                float ny = dx / length;
                float w = plateWidth / 2.0f;
                // Four corners of the plate (bottom)
                float x1a = line.getX1() + nx * w;
                float y1a = line.getY1() + ny * w;
                float x1b = line.getX1() - nx * w;
                float y1b = line.getY1() - ny * w;
                float x2a = line.getX2() + nx * w;
                float y2a = line.getY2() + ny * w;
                float x2b = line.getX2() - nx * w;
                float y2b = line.getY2() - ny * w;
                // Bottom face
                Point3D p1 = new Point3D(x1a, y1a, 0);
                Point3D p2 = new Point3D(x1b, y1b, 0);
                Point3D p3 = new Point3D(x2b, y2b, 0);
                Point3D p4 = new Point3D(x2a, y2a, 0);
                // Top face
                Point3D q1 = new Point3D(x1a, y1a, (float) height);
                Point3D q2 = new Point3D(x1b, y1b, (float) height);
                Point3D q3 = new Point3D(x2b, y2b, (float) height);
                Point3D q4 = new Point3D(x2a, y2a, (float) height);
                // Side faces
                extrudedFaces.add(new Face3D(p1, p2, q2, q1)); // side 1
                extrudedFaces.add(new Face3D(p2, p3, q3, q2)); // side 2
                extrudedFaces.add(new Face3D(p3, p4, q4, q3)); // side 3
                extrudedFaces.add(new Face3D(p4, p1, q1, q4)); // side 4
                // Top and bottom faces
                List<Point3D> topFace = new ArrayList<>();
                topFace.add(q1);
                topFace.add(q2);
                topFace.add(q3);
                topFace.add(q4);
                extrudedFaces.add(new Face3D(topFace));
                List<Point3D> bottomFace = new ArrayList<>();
                bottomFace.add(p1);
                bottomFace.add(p2);
                bottomFace.add(p3);
                bottomFace.add(p4);
                extrudedFaces.add(new Face3D(bottomFace));
            }
        }
    }

    /**
     * Extrudes a single polygon to create 3D faces.
     * 
     * @param polygon The polygon to extrude
     * @param height  The extrusion height
     */
    private void extrudePolygon(Polygon polygon, double height) {
        List<Point2D> points = polygon.getPoints();
        int n = points.size();

        List<Point3D> bottom = points.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .toList();

        List<Point3D> top = points.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), (float) height))
                .toList();

        // Side faces
        for (int i = 0; i < n; i++) {
            Point3D p1 = bottom.get(i);
            Point3D p2 = bottom.get((i + 1) % n);
            Point3D p3 = top.get((i + 1) % n);
            Point3D p4 = top.get(i);

            extrudedFaces.add(new Face3D(p1, p2, p3, p4));
        }

        // Top and bottom faces
        extrudedFaces.add(new Face3D(top));

        List<Point3D> reversedBottom = new ArrayList<>(bottom);
        java.util.Collections.reverse(reversedBottom);
        extrudedFaces.add(new Face3D(reversedBottom));
    }

    /**
     * Extrudes a circle to create a cylindrical shape.
     * 
     * @param circle The circle to extrude
     * @param height The extrusion height
     */
    private void extrudeCircle(Circle circle, double height) {
        int segments = 32; // Number of segments to approximate the circle
        List<Point2D> circlePoints = new ArrayList<>();

        // Generate points around the circle
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float x = circle.getX() + circle.getRadius() * (float) Math.cos(angle);
            float y = circle.getY() + circle.getRadius() * (float) Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        // Create bottom and top point lists
        List<Point3D> bottom = circlePoints.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .toList();

        List<Point3D> top = circlePoints.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), (float) height))
                .toList();

        // Create side faces (cylindrical surface)
        for (int i = 0; i < segments; i++) {
            Point3D p1 = bottom.get(i);
            Point3D p2 = bottom.get((i + 1) % segments);
            Point3D p3 = top.get((i + 1) % segments);
            Point3D p4 = top.get(i);

            extrudedFaces.add(new Face3D(p1, p2, p3, p4));
        }

        // Create top and bottom circular faces
        extrudedFaces.add(new Face3D(top));

        List<Point3D> reversedBottom = new ArrayList<>(bottom);
        java.util.Collections.reverse(reversedBottom);
        extrudedFaces.add(new Face3D(reversedBottom));
    }

    /**
     * Finds closed loops formed by connected lines in the sketch.
     * This method analyzes all Line entities and attempts to find sequences
     * of connected lines that form closed polygonal shapes.
     * 
     * @return A list of closed loops, where each loop is a list of points
     */
    private List<List<Point2D>> findClosedLoopsFromLines() {
        List<List<Point2D>> closedLoops = new ArrayList<>();
        List<Line> lines = new ArrayList<>();

        // Collect all line entities
        for (Entity entity : sketchEntities) {
            if (entity instanceof Line) {
                lines.add((Line) entity);
            }
        }

        if (lines.isEmpty()) {
            return closedLoops;
        }

        // Track which lines have been used
        Set<Line> usedLines = new HashSet<>();

        // Try to build closed loops starting from each unused line
        for (Line startLine : lines) {
            if (usedLines.contains(startLine)) {
                continue;
            }

            List<Point2D> currentLoop = new ArrayList<>();
            Set<Line> currentLoopLines = new HashSet<>();

            // Start the loop
            currentLoop.add(new Point2D(startLine.getX1(), startLine.getY1()));
            currentLoop.add(new Point2D(startLine.getX2(), startLine.getY2()));
            currentLoopLines.add(startLine);

            Point2D currentEndPoint = new Point2D(startLine.getX2(), startLine.getY2());
            Point2D startPoint = new Point2D(startLine.getX1(), startLine.getY1());

            boolean foundConnection = true;
            while (foundConnection) {
                foundConnection = false;

                // Look for a line that connects to our current end point
                for (Line nextLine : lines) {
                    if (currentLoopLines.contains(nextLine)) {
                        continue;
                    }

                    Point2D nextStart = new Point2D(nextLine.getX1(), nextLine.getY1());
                    Point2D nextEnd = new Point2D(nextLine.getX2(), nextLine.getY2());

                    // Check if this line connects to our current end point
                    if (isPointsEqual(currentEndPoint, nextStart)) {
                        currentLoop.add(nextEnd);
                        currentLoopLines.add(nextLine);
                        currentEndPoint = nextEnd;
                        foundConnection = true;
                        break;
                    } else if (isPointsEqual(currentEndPoint, nextEnd)) {
                        currentLoop.add(nextStart);
                        currentLoopLines.add(nextLine);
                        currentEndPoint = nextStart;
                        foundConnection = true;
                        break;
                    }
                }
            }

            // Check if we have a closed loop (end point connects back to start)
            if (currentLoop.size() >= 3 && isPointsEqual(currentEndPoint, startPoint)) {
                // Remove the duplicate end point
                currentLoop.remove(currentLoop.size() - 1);
                closedLoops.add(currentLoop);
                usedLines.addAll(currentLoopLines);
            }
        }

        return closedLoops;
    }

    /**
     * Helper method to check if two points are equal within a small tolerance.
     * 
     * @param p1 First point
     * @param p2 Second point
     * @return true if points are equal within tolerance
     */
    private boolean isPointsEqual(Point2D p1, Point2D p2) {
        float tolerance = 1e-6f;
        return Math.abs(p1.getX() - p2.getX()) < tolerance &&
                Math.abs(p1.getY() - p2.getY()) < tolerance;
    }

    /**
     * Extrudes a closed loop of points to create 3D faces.
     * This method treats the loop as a polygon and creates the corresponding
     * 3D geometry including side faces, top face, and bottom face.
     * 
     * @param loop   The closed loop of points to extrude
     * @param height The extrusion height
     */
    private void extrudeClosedLoop(List<Point2D> loop, double height) {
        if (loop.size() < 3) {
            return; // Need at least 3 points for a valid polygon
        }

        int n = loop.size();

        // Create bottom and top point lists
        List<Point3D> bottom = loop.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .toList();

        List<Point3D> top = loop.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), (float) height))
                .toList();

        // Create side faces
        for (int i = 0; i < n; i++) {
            Point3D p1 = bottom.get(i);
            Point3D p2 = bottom.get((i + 1) % n);
            Point3D p3 = top.get((i + 1) % n);
            Point3D p4 = top.get(i);

            extrudedFaces.add(new Face3D(p1, p2, p3, p4));
        }

        // Create top and bottom faces
        extrudedFaces.add(new Face3D(top));

        List<Point3D> reversedBottom = new ArrayList<>(bottom);
        java.util.Collections.reverse(reversedBottom);
        extrudedFaces.add(new Face3D(reversedBottom));
    }

    /**
     * Converts extruded 3D faces to triangles for OpenGL rendering.
     * Each face is triangulated and returned as an array of float values
     * compatible with STL triangle format (normal + 3 vertices).
     * 
     * @return List of triangle arrays for OpenGL rendering
     */
    public List<float[]> getExtrudedTriangles() {
        List<float[]> triangles = new ArrayList<>();

        for (Face3D face : extrudedFaces) {
            // Triangulate the face (convert n-sided face to triangles)
            List<float[]> faceTriangles = triangulateFace(face);
            triangles.addAll(faceTriangles);
        }

        return triangles;
    }

    /**
     * Triangulates a 3D face into triangles for rendering.
     * Uses fan triangulation for faces with more than 3 vertices.
     * 
     * @param face The face to triangulate
     * @return List of triangles representing the face
     */
    private List<float[]> triangulateFace(Face3D face) {
        List<float[]> triangles = new ArrayList<>();
        List<Point3D> vertices = face.vertices;

        if (vertices.size() < 3) {
            return triangles; // Cannot create triangles from less than 3 vertices
        }

        if (vertices.size() == 3) {
            // Already a triangle
            triangles.add(createTriangle(vertices.get(0), vertices.get(1), vertices.get(2)));
        } else {
            // Fan triangulation: connect all vertices to the first vertex
            Point3D center = vertices.get(0);
            for (int i = 1; i < vertices.size() - 1; i++) {
                triangles.add(createTriangle(center, vertices.get(i), vertices.get(i + 1)));
            }
        }

        return triangles;
    }

    /**
     * Creates a triangle array from three 3D points.
     * Calculates the normal vector and formats as STL triangle.
     * 
     * @param p1 First vertex
     * @param p2 Second vertex
     * @param p3 Third vertex
     * @return Triangle array [nx, ny, nz, x1, y1, z1, x2, y2, z2, x3, y3, z3]
     */
    private float[] createTriangle(Point3D p1, Point3D p2, Point3D p3) {
        // Calculate normal vector using cross product
        float[] v1 = { p2.x - p1.x, p2.y - p1.y, p2.z - p1.z };
        float[] v2 = { p3.x - p1.x, p3.y - p1.y, p3.z - p1.z };

        // Cross product: v1 × v2
        float nx = v1[1] * v2[2] - v1[2] * v2[1];
        float ny = v1[2] * v2[0] - v1[0] * v2[2];
        float nz = v1[0] * v2[1] - v1[1] * v2[0];

        // Normalize the normal vector
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        // Return triangle in STL format: normal + 3 vertices
        return new float[] {
                nx, ny, nz, // Normal vector
                p1.x, p1.y, p1.z, // Vertex 1
                p2.x, p2.y, p2.z, // Vertex 2
                p3.x, p3.y, p3.z // Vertex 3
        };
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    /**
     * Generates a 4-digit NACA airfoil profile.
     * 
     * @param digit4        4-digit NACA code string (e.g., "2412").
     * @param chord         Chord length.
     * @param pointsPerSide Number of points for upper/lower surface.
     * @return 0 on success, -1 on failure.
     */
    public int generateNaca4(String digit4, float chord, int pointsPerSide) {
        if (digit4.length() != 4)
            return -1;

        try {
            int mInt = Integer.parseInt(digit4.substring(0, 1));
            int pInt = Integer.parseInt(digit4.substring(1, 2));
            int tInt = Integer.parseInt(digit4.substring(2, 4));

            float m = mInt / 100.0f; // Max camber
            float p = pInt / 10.0f; // Location of max camber
            float t = tInt / 100.0f; // Max thickness

            List<PointEntity> upper = new ArrayList<>();
            List<PointEntity> lower = new ArrayList<>();

            for (int i = 0; i <= pointsPerSide; i++) {
                float beta = (float) (i * Math.PI / pointsPerSide);
                float x = (float) (chord * 0.5 * (1 - Math.cos(beta))); // Cosine spacing

                float yt = (float) (5 * t * chord * (0.2969 * Math.sqrt(x / chord)
                        - 0.1260 * (x / chord)
                        - 0.3516 * Math.pow(x / chord, 2)
                        + 0.2843 * Math.pow(x / chord, 3)
                        - 0.1015 * Math.pow(x / chord, 4)));

                float yc = 0;
                float dyc_dx = 0;

                if (p != 0) {
                    if (x <= p * chord) {
                        yc = (float) (m / (p * p) * (2 * p * (x / chord) - Math.pow(x / chord, 2)));
                        dyc_dx = (float) (2 * m / (p * p) * (p - x / chord));
                    } else {
                        yc = (float) (m / ((1 - p) * (1 - p))
                                * ((1 - 2 * p) + 2 * p * (x / chord) - Math.pow(x / chord, 2)));
                        dyc_dx = (float) (2 * m / ((1 - p) * (1 - p)) * (p - x / chord));
                    }
                }

                float theta = (float) Math.atan(dyc_dx);

                upper.add(new PointEntity((float) (x - yt * Math.sin(theta)), (float) (yc + yt * Math.cos(theta))));
                lower.add(new PointEntity((float) (x + yt * Math.sin(theta)), (float) (yc - yt * Math.cos(theta))));
            }

            // Combine into one polygon (CCW)
            List<PointEntity> polyPoints = new ArrayList<>(upper);
            java.util.Collections.reverse(lower);
            polyPoints.addAll(lower);

            return addPolygon(polyPoints);

        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Finds the closest entity to the given coordinates within a tolerance.
     * 
     * @param x         X coordinate
     * @param y         Y coordinate
     * @param tolerance Maximum distance to consider
     * @return The closest Entity or null if none found within tolerance
     */
    public Entity getClosestEntity(float x, float y, float tolerance) {
        Entity closest = null;
        float minDst = tolerance;

        for (Entity e : sketchEntities) {
            float dst = Float.MAX_VALUE;
            if (e instanceof PointEntity) {
                PointEntity p = (PointEntity) e;
                float dx = p.getX() - x;
                float dy = p.getY() - y;
                dst = (float) Math.sqrt(dx * dx + dy * dy);
            } else if (e instanceof Line) {
                Line l = (Line) e;
                dst = distancePointToSegment(x, y, l.getX1(), l.getY1(), l.getX2(), l.getY2());
            } else if (e instanceof Circle) {
                Circle c = (Circle) e;
                float dx = c.getX() - x;
                float dy = c.getY() - y;
                float distToCenter = (float) Math.sqrt(dx * dx + dy * dy);
                dst = Math.abs(distToCenter - c.getRadius());
            }

            if (dst < minDst) {
                minDst = dst;
                closest = e;
            }
        }
        return closest;
    }

    private float distancePointToSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float l2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
        if (l2 == 0)
            return (float) Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        float t = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / l2;
        t = Math.max(0, Math.min(1, t));
        float projX = x1 + t * (x2 - x1);
        float projY = y1 + t * (y2 - y1);
        return (float) Math.sqrt((px - projX) * (px - projX) + (py - projY) * (py - projY));
    }
}
