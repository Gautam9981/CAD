package cad.core;

import com.jogamp.opengl.GL2;
import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
// import java.awt.Graphics; // No longer directly used for OpenGL drawing

/**
 * 2D sketching system for creating and managing geometric entities.
 * Supports points, lines, circles, polygons with DXF import/export capabilities.
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
     */
    public static class PointEntity extends Entity {
        float x, y;
        public PointEntity(float x, float y) {
            this.type = TypeSketch.POINT;
            this.x = x;
            this.y = y;
        }
        @Override
        public String toString() {
            return String.format("Point at (%.3f, %.3f)", x, y);
        }
    }

    /**
     * Represents a line segment in the 2D sketch.
     */
    public static class Line extends Entity {
        float x1, y1, x2, y2;
        public Line(float x1, float y1, float x2, float y2) {
            this.type = TypeSketch.LINE;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }
        public String toString() {
            return String.format("Line from (%.3f, %.3f) to (%.3f, %.3f)", x1, y1, x2, y2);
        }
    }

    /**
     * Represents a circle in the 2D sketch.
     */
    public static class Circle extends Entity {
        float x, y, r;
        public Circle(float x, float y, float r) {
            this.type = TypeSketch.CIRCLE;
            this.x = x;
            this.y = y;
            this.r = r;
        }
        public String toString() {
            return String.format("Circle at (%.3f, %.3f) with radius %.3f", x, y, r);
        }
    }

    /**
     * Represents a closed polygon in the 2D sketch, defined by a list of points.
     */
    public static class Polygon extends Entity {
        List < PointEntity > points;
        public Polygon(List < PointEntity > points) {
            if (points == null || points.size() < 3 || points.size() > 25) {
                throw new IllegalArgumentException("Polygon must have between 3 and 25 points.");
            }
            this.type = TypeSketch.POLYGON;
            this.points = new ArrayList < > (points);
        }
        public String toString() {
            StringBuilder sb = new StringBuilder("Polygon with points: ");
            for (PointEntity p: points) {
                sb.append(String.format("(%.2f, %.2f) ", p.x, p.y));
            }
            return sb.toString();
        }

        // Add getPoints() method to Polygon
        public List<Point2D> getPoints() {
            List<Point2D> p2dPoints = new ArrayList<>();
            for (PointEntity p : points) {
                p2dPoints.add(new Point2D(p.x, p.y));
            }
            return p2dPoints;
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

        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
    }

    /**
     * Represents a 3D face, defined by a list of 3D points.
     * Assumes points are ordered to define the face (e.g., clockwise or counter-clockwise).
     */
    public static class Face3D {
        private List<Point3D> vertices;

        // Constructor for a quad face (4 points)
        public Face3D(Point3D p1, Point3D p2, Point3D p3, Point3D p4) {
            this.vertices = new ArrayList<>();
            this.vertices.add(p1);
            this.vertices.add(p2);
            this.vertices.add(p3);
            this.vertices.add(p4);
        }

        // Constructor for a general polygonal face (list of points)
        public Face3D(List<Point3D> vertices) {
            if (vertices == null || vertices.size() < 3) {
                throw new IllegalArgumentException("A Face3D must have at least 3 vertices.");
            }
            this.vertices = new ArrayList<>(vertices);
        }

        public List<Point3D> getVertices() {
            return vertices;
        }
    }
    // End of new class definitions

    private static final int MAX_SKETCH_ENTITIES = 1000;
    private final List < Entity > sketchEntities = new ArrayList < > ();

    // New fields to resolve errors
    public final List < Polygon > polygons = new ArrayList < > (); // To store only Polygon entities for extrusion
    public List<Face3D> extrudedFaces = new ArrayList < > (); // To store the result of extrusion
    // End of new fields

    /**
     * Adds a point entity to the sketch.
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
     * @param points List of PointEntity forming the polygon.
     * @return 0 on success, 1 if sketch buffer is full or invalid points.
     */
    public int addPolygon(List < PointEntity > points) {
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

    /**
     * Adds an N-sided regular polygon to the sketch.
     * @param centerX X-coordinate of the polygon's center.
     * @param centerY Y-coordinate of the polygon's center.
     * @param radius Radius of the circumcircle.
     * @param sides Number of sides (3-25).
     * @return 0 on success, 1 on error.
     */
    public int addNSidedPolygon(float centerX, float centerY, float radius, int sides) {
        if (sides < 3 || sides > 25) {
            System.out.println("Polygon must have between 3 and 25 sides.");
            return 1;
        }
        if (radius <= 0) {
            System.out.println("Radius must be positive.");
            return 1;
        }

        List < PointEntity > points = new ArrayList < > ();
        for (int i = 0; i < sides; i++) {
            double angle = 2 * Math.PI * i / sides;
            float x = centerX + (float)(radius * Math.cos(angle));
            float y = centerY + (float)(radius * Math.sin(angle));
            points.add(new PointEntity(x, y));
        }

        return addPolygon(points);
    }

    /**
     * Clears all entities from the sketch.
     */
    public void clearSketch() {
        sketchEntities.clear();
        polygons.clear(); // Clear polygons list as well
        extrudedFaces.clear(); // Clear extruded faces
    }

    /**
     * Returns a list of string representations of all entities in the sketch.
     * @return List of strings describing each sketch entity.
     */
    public List < String > listSketch() {
        List < String > output = new ArrayList < > ();

        if (sketchEntities.isEmpty()) {
            output.add("Sketch is empty.");
            return output;
        }

        for (Entity e: sketchEntities) {
            output.add(e.toString());
        }

        return output;
    }

    /**
     * Gets all entities in the sketch.
     * @return List of all sketch entities.
     */
    public List<Entity> getEntities() {
        return new ArrayList<>(sketchEntities);
    }

    /**
     * Checks if the sketch contains extrudable shapes.
     * Extrudable shapes include polygons and circles.
     * @return true if sketch contains extrudable shapes (polygons or circles), false otherwise.
     */
    public boolean isClosedLoop() {
        for (Entity entity : sketchEntities) {
            if (entity instanceof Polygon || entity instanceof Circle) {
                return true;
            }
        }
        return false;
    }

    private String units = "mm";

    /**
     * Sets the units for the sketch.
     * @param units String representing the units (e.g., "mm", "in").
     */
    public void setUnits(String units) {
        this.units = units.toLowerCase();
    }

    /**
     * Gets the current units of the sketch.
     * @return Current units string.
     */
    public String getUnits() {
        return this.units;
    }

    /**
     * Converts a unit string to its corresponding DXF unit code.
     * @param unitStr The unit string.
     * @return The DXF unit code.
     */
    private int getDXFUnitCode(String unitStr) {
        return switch (unitStr.toLowerCase()) {
        case "in", "inch", "inches" -> 1;
        case "ft", "feet" -> 2;
        case "mm", "millimeter", "millimeters" -> 4;
        case "cm", "centimeter", "centimeters" -> 5;
        case "m", "meter", "meters" -> 6;
        default -> 0; // Unitless
        };
    }

    /**
     * Converts a DXF unit code to its corresponding unit string.
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
            out.println(getDXFUnitCode(this.units));

            out.println("0");
            out.println("ENDSEC");

            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("ENTITIES");

            for (Entity e: sketchEntities) {
                if (e instanceof PointEntity p) {
                    out.println("0");
                    out.println("POINT");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("10");
                    out.println(p.x);
                    out.println("20");
                    out.println(p.y);
                } else if (e instanceof Line l) {
                    out.println("0");
                    out.println("LINE");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("10");
                    out.println(l.x1);
                    out.println("20");
                    out.println(l.y1);
                    out.println("11");
                    out.println(l.x2);
                    out.println("21");
                    out.println(l.y2);
                } else if (e instanceof Circle c) {
                    out.println("0");
                    out.println("CIRCLE");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("10");
                    out.println(c.x);
                    out.println("20");
                    out.println(c.y);
                    out.println("40");
                    out.println(c.r);
                } else if (e instanceof Polygon poly) {
                    out.println("0");
                    out.println("POLYLINE");
                    out.println("8");
                    out.println("0"); // Layer "0"
                    out.println("66"); // Entities follow (for old-style polylines)
                    out.println("1");
                    out.println("70"); // Polyline flags
                    out.println("1"); // Flag 1 for closed polyline (if appropriate, 0 for open)

                    for (PointEntity p: poly.points) {
                        out.println("0");
                        out.println("VERTEX");
                        out.println("8");
                        out.println("0"); // Layer "0"
                        out.println("10");
                        out.println(p.x);
                        out.println("20");
                        out.println(p.y);
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
     * @param x X-coordinate of the center.
     * @param y Y-coordinate of the center.
     * @param radius Radius of the circumcircle.
     * @param sides Number of sides (3-25).
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

        List < PointEntity > points = new ArrayList < > ();
        double angleStep = 2 * Math.PI / sides;

        for (int i = 0; i < sides; i++) {
            double angle = i * angleStep;
            float px = x + (float)(radius * Math.cos(angle));
            float py = y + (float)(radius * Math.sin(angle));
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
        this.units = "unitless"; // Reset units before loading

        // Single pass: Read both header and entities
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntity = null;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List < PointEntity > polyPoints = null;
            float tempVertexX = 0, tempVertexY = 0;

            boolean inHeaderSection = false;
            boolean inEntitiesSection = false;
            boolean inPolylineEntity = false; // Flag to track if we are currently parsing a POLYLINE's vertices
            boolean waitingForVertexCoords = false; // Flag to indicate that the next 10/20 group codes are for a vertex

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

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
                                float scale = unitScaleFactor(units);
                                System.out.println("Starting DXF entity parsing with units: " + units + " (scale factor: " + scale + ")");
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
                        if (currentEntity != null && !inPolylineEntity && !currentEntity.equals("VERTEX") && !currentEntity.equals("SKIP_VERTEX") && !currentEntity.equals("SEQEND")) {
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

                    // If we were parsing a non-POLYLINE entity, add it now before processing the new one
                    if (currentEntity != null && !inPolylineEntity && !currentEntity.equals("VERTEX") && !currentEntity.equals("SKIP_VERTEX") && !currentEntity.equals("SEQEND")) { // Exclude VERTEX, SKIP_VERTEX, and SEQEND
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
                            polyPoints = new ArrayList < > (); // Initialize points list for this polyline
                            // Consume the next group codes for POLYLINE if any (e.g., 66, 70)
                            // Loop continues to read properties of POLYLINE or its VERTEX entities
                        } else if (currentEntity.equals("VERTEX")) {
                            // This VERTEX is encountered directly, likely part of an already active POLYLINE
                            if (!inPolylineEntity) {
                                System.err.println("Warning: Found VERTEX entity outside of POLYLINE. Skipping.");
                                // We need to skip all the group codes for this VERTEX until the next "0" group code
                                // by setting a flag and not setting currentEntity to null immediately
                                currentEntity = "SKIP_VERTEX"; // Special marker to skip this entity's data
                            } else {
                                waitingForVertexCoords = true; // Set flag to expect 10, 20
                            }
                        } else if (currentEntity.equals("SEQEND")) {
                            // End of POLYLINE sequence
                            if (inPolylineEntity && polyPoints != null) {
                                addPolygon(polyPoints); // Add the accumulated polygon
                                // System.out.println("DBG: Finished POLYLINE, added " + polyPoints.size() + " points.");
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
                                this.units = getUnitsFromDXFCode(code);
                                System.out.println("DXF Header Units: " + this.units + " (Code: " + code + ")");
                            } catch (NumberFormatException e) {
                                System.err.println("Warning: Invalid $INSUNITS code in DXF header. Using default units. Error: " + e.getMessage());
                            }
                        }
                    }
                    continue;
                }

                // If we are inside an entity definition (not a '0' group code line)
                if (currentEntity != null && inEntitiesSection) {
                    float scale = unitScaleFactor(units); // Calculate scale factor for this parsing
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

                        // If we're skipping this entity, just consume the group code/value pair and continue
                        if (currentEntity.equals("SKIP_VERTEX") || currentEntity.equals("SEQEND")) {
                            continue; // Skip processing this group code/value pair
                        }

                        switch (groupCode) {
                        case 10: // X coordinate of start point (POINT, LINE) or center (CIRCLE) or vertex (VERTEX)
                            if (currentEntity.equals("VERTEX") && waitingForVertexCoords) {
                                tempVertexX = Float.parseFloat(valueLine) * scale;
                            } else {
                                if (currentEntity.equals("POINT") || currentEntity.equals("LINE")) x1 = Float.parseFloat(valueLine) * scale;
                                else if (currentEntity.equals("CIRCLE")) cx = Float.parseFloat(valueLine) * scale;
                            }
                            break;
                        case 20: // Y coordinate of start point (POINT, LINE) or center (CIRCLE) or vertex (VERTEX)
                            if (currentEntity.equals("VERTEX") && waitingForVertexCoords) {
                                tempVertexY = Float.parseFloat(valueLine) * scale;
                                // If we have both X and Y for a vertex, add it to the polyline
                                if (polyPoints != null) {
                                    polyPoints.add(new PointEntity(tempVertexX, tempVertexY));
                                }
                                waitingForVertexCoords = false; // Reset for next vertex
                            } else {
                                if (currentEntity.equals("POINT") || currentEntity.equals("LINE")) y1 = Float.parseFloat(valueLine) * scale;
                                else if (currentEntity.equals("CIRCLE")) cy = Float.parseFloat(valueLine) * scale;
                            }
                            break;
                        case 11: // X coordinate of end point (LINE)
                            if (currentEntity.equals("LINE")) x2 = Float.parseFloat(valueLine) * scale;
                            break;
                        case 21: // Y coordinate of end point (LINE)
                            if (currentEntity.equals("LINE")) y2 = Float.parseFloat(valueLine) * scale;
                            break;
                        case 40: // Radius (CIRCLE) or sometimes thickness for other entities, or start/end width for POLYLINE
                            if (currentEntity.equals("CIRCLE")) radius = Float.parseFloat(valueLine) * scale;
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
                        System.err.println("Warning: Invalid number format for DXF value near group code: " + line + ", value: " + valueLine + ". Error: " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Error parsing DXF line '" + line + "': " + e.getMessage());
                        e.printStackTrace();
                        break; // Stop parsing on critical error
                    }
                }
            } // end of while (line = reader.readLine()) != null

            // After loop, if there was an active non-polyline entity
            if (currentEntity != null && !inPolylineEntity && !currentEntity.equals("VERTEX") && !currentEntity.equals("SKIP_VERTEX") && !currentEntity.equals("SEQEND")) {
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
     * @param type The type of entity as a string ("POINT", "LINE", "CIRCLE", "POLYLINE").
     * @param x1 Start X-coordinate or Point X.
     * @param y1 Start Y-coordinate or Point Y.
     * @param x2 End X-coordinate (for Line).
     * @param y2 End Y-coordinate (for Line).
     * @param cx Center X-coordinate (for Circle).
     * @param cy Center Y-coordinate (for Circle).
     * @param radius Radius (for Circle).
     * @param polyPoints List of points for a Polygon (null for other types).
     */
    private void addEntity(String type, float x1, float y1, float x2, float y2, float cx, float cy, float radius, List < PointEntity > polyPoints) {
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
        case "POLYLINE": // This case handles the final addition of the polygon after all vertices are parsed
            if (polyPoints != null) {
                addPolygon(polyPoints);
            }
            break;
            // VERTEX and SEQEND are control entities for POLYLINE, not standalone entities to be added here.
        default:
            System.out.println("Unsupported DXF entity type for adding: " + type);
        }
    }

    /**
     * Provides a scaling factor to convert DXF units to an internal standard unit (e.g., millimeters).
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
     * It converts Face3D objects to triangles and renders them with proper lighting.
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    public void draw3D(GL2 gl) {
        if (extrudedFaces.isEmpty()) {
            return; // Nothing to render
        }

        // Set material properties for extruded geometry
        float[] materialAmbient = {0.2f, 0.4f, 0.6f, 1.0f}; // Blue-ish ambient
        float[] materialDiffuse = {0.4f, 0.6f, 0.8f, 1.0f}; // Blue-ish diffuse
        float[] materialSpecular = {0.8f, 0.8f, 0.8f, 1.0f}; // White specular
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
     * @param gl The GL2 object for OpenGL rendering.
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
     * Calculates the normal vector for a triangular face defined by three 3D points.
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
        
        return new float[]{nx, ny, nz};
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
        for (Entity e: sketchEntities) {
            switch (e.type) {
            case POINT:
                PointEntity p = (PointEntity) e;
                gl.glPointSize(5.0f); // Make points visible
                gl.glBegin(GL2.GL_POINTS);
                gl.glVertex2f(p.x, p.y);
                gl.glEnd();
                break;
            case LINE:
                Line l = (Line) e;
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex2f(l.x1, l.y1);
                gl.glVertex2f(l.x2, l.y2);
                gl.glEnd();
                break;
            case CIRCLE:
                Circle c = (Circle) e;
                gl.glBegin(GL2.GL_LINE_LOOP);
                int segments = 50; // Resolution of the circle
                for (int i = 0; i < segments; i++) {
                    double angle = 2.0 * Math.PI * i / segments;
                    float x = c.x + c.r * (float) Math.cos(angle);
                    float y = c.y + c.r * (float) Math.sin(angle);
                    gl.glVertex2f(x, y);
                }
                gl.glEnd();
                break;
            case POLYGON:
                Polygon poly = (Polygon) e;
                gl.glBegin(GL2.GL_LINE_LOOP); // Draw as a closed loop
                for (PointEntity vert: poly.points) {
                    gl.glVertex2f(vert.x, vert.y);
                }
                gl.glEnd();
                break;
            }
        }
    }
    /**
     * Extrudes all closed shapes in the sketch to create 3D faces.
     * Handles polygons, circles, and potentially connected line loops.
     * Creates side faces, top faces, and bottom faces for each extrudable shape.
     * @param height The height of the extrusion in the Z direction.
     */
    public void extrude(double height) {
        // Clear previous extruded faces
        this.extrudedFaces.clear();

        // Extrude existing polygons (POLYLINE entities from DXF)
        for (Polygon polygon: this.polygons) {
            extrudePolygon(polygon, height);
        }
        
        // Extrude circles as cylindrical shapes
        for (Entity entity : sketchEntities) {
            if (entity instanceof Circle) {
                Circle circle = (Circle) entity;
                extrudeCircle(circle, height);
            }
        }
        
        // TODO: Add logic to detect and extrude closed loops formed by connected lines
        // This would handle cases where multiple LINE entities form a closed shape
    }
    
    /**
     * Extrudes a single polygon to create 3D faces.
     * @param polygon The polygon to extrude
     * @param height The extrusion height
     */
    private void extrudePolygon(Polygon polygon, double height) {
        List < Point2D > points = polygon.getPoints();
        int n = points.size();

        List < Point3D > bottom = points.stream()
            .map(p -> new Point3D(p.getX(), p.getY(), 0))
            .toList();

        List < Point3D > top = points.stream()
            .map(p -> new Point3D(p.getX(), p.getY(), (float)height))
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
     * @param circle The circle to extrude
     * @param height The extrusion height
     */
    private void extrudeCircle(Circle circle, double height) {
        int segments = 32; // Number of segments to approximate the circle
        List<Point2D> circlePoints = new ArrayList<>();
        
        // Generate points around the circle
        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float x = circle.x + circle.r * (float) Math.cos(angle);
            float y = circle.y + circle.r * (float) Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }
        
        // Create bottom and top point lists
        List<Point3D> bottom = circlePoints.stream()
            .map(p -> new Point3D(p.getX(), p.getY(), 0))
            .toList();
            
        List<Point3D> top = circlePoints.stream()
            .map(p -> new Point3D(p.getX(), p.getY(), (float)height))
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
}