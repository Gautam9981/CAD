package core; // Declares the package for the Sketch class.

import java.util.ArrayList;
import java.util.List; 
import java.io.FileWriter; 
import java.io.IOException; 
import java.io.PrintWriter; 
import java.io.FileReader; 
import java.io.BufferedReader; 
import java.awt.Graphics; 
import java.awt.Component; 

/**
 * The `Sketch` class manages a collection of geometric entities,
 * provides methods for adding and manipulating them,
 * and supports exporting/importing sketches to/from DXF format.
 * It also includes functionality for drawing the sketch on a Graphics context.
 */
public class Sketch {

    /**
     * Enumerates the types of geometric entities supported in a sketch.
     */
    public enum TypeSketch {
        POINT,
        LINE,
        CIRCLE,
        POLYGON
    }

    /**
     * Abstract base class for all geometric entities in the sketch.
     * Each entity has a `TypeSketch` to identify its specific geometric form.
     */
    public static abstract class Entity {
        public TypeSketch type; // Public for direct access (could be protected with a getter).
    }

    /**
     * Represents a point entity with X and Y coordinates.
     */
    public static class PointEntity extends Entity {
        public float x, y; // Public for direct access.

        /**
         * Constructs a new PointEntity.
         * @param x The X-coordinate of the point.
         * @param y The Y-coordinate of the point.
         */
        public PointEntity(float x, float y) {
            this.type = TypeSketch.POINT;
            this.x = x;
            this.y = y;
        }

        /**
         * Returns a string representation of the PointEntity.
         * @return A formatted string showing the point's coordinates.
         */
        @Override
        public String toString() {
            return String.format("Point at (%.3f, %.3f)", x, y);
        }
    }

    /**
     * Represents a line entity defined by two points (x1, y1) and (x2, y2).
     */
    public static class Line extends Entity {
        public float x1, y1, x2, y2; // Public for direct access.

        /**
         * Constructs a new Line entity.
         * @param x1 The X-coordinate of the start point.
         * @param y1 The Y-coordinate of the start point.
         * @param x2 The X-coordinate of the end point.
         * @param y2 The Y-coordinate of the end point.
         */
        public Line(float x1, float y1, float x2, float y2) {
            this.type = TypeSketch.LINE;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        /**
         * Returns a string representation of the Line entity.
         * @return A formatted string showing the line's start and end coordinates.
         */
        @Override
        public String toString() {
            return String.format("Line from (%.3f, %.3f) to (%.3f, %.3f)", x1, y1, x2, y2);
        }
    }

    /**
     * Represents a circle entity defined by its center (x, y) and radius (r).
     */
    public static class Circle extends Entity {
        public float x, y, r; // Public for direct access.

        /**
         * Constructs a new Circle entity.
         * @param x The X-coordinate of the circle's center.
         * @param y The Y-coordinate of the circle's center.
         * @param r The radius of the circle.
         */
        public Circle(float x, float y, float r) {
            this.type = TypeSketch.CIRCLE;
            this.x = x;
            this.y = y;
            this.r = r;
        }

        /**
         * Returns a string representation of the Circle entity.
         * @return A formatted string showing the circle's center and radius.
         */
        @Override
        public String toString() {
            return String.format("Circle at (%.3f, %.3f) with radius %.3f", x, y, r);
        }
    }

    /**
     * Represents a polygon entity defined by a list of PointEntity objects.
     * A polygon must have between 3 and 25 points.
     */
    public static class Polygon extends Entity {
        public List<PointEntity> points; // Public for direct access.

        /**
         * Constructs a new Polygon entity.
         * @param points A list of PointEntity objects defining the polygon's vertices.
         * @throws IllegalArgumentException if the number of points is less than 3 or greater than 25.
         */
        public Polygon(List<PointEntity> points) {
            if (points == null || points.size() < 3 || points.size() > 25) {
                throw new IllegalArgumentException("Polygon must have between 3 and 25 points.");
            }
            this.type = TypeSketch.POLYGON;
            // Create a new ArrayList to ensure immutability from outside modifications.
            this.points = new ArrayList<>(points);
        }

        /**
         * Returns a string representation of the Polygon entity.
         * @return A formatted string showing the polygon's points.
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Polygon with points: ");
            for (PointEntity p : points) {
                sb.append(String.format("(%.2f, %.2f) ", p.x, p.y));
            }
            return sb.toString().trim(); // Trim trailing space.
        }
    }

    // --- Sketch Management ---

    private static final int MAX_SKETCH_ENTITIES = 1000; // Maximum number of entities allowed in the sketch.
    private final List<Entity> sketchEntities = new ArrayList<>(); // The main list holding all geometric entities.
    private String units = "mm"; // Current units for the sketch, default to millimeters.

    /**
     * Attempts to add a new PointEntity to the sketch.
     * @param x The X-coordinate of the point.
     * @param y The Y-coordinate of the point.
     * @return 0 if successful, 1 if the sketch buffer is full.
     */
    public int addPoint(float x, float y) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full: Cannot add more points.");
            return 1;
        }
        sketchEntities.add(new PointEntity(x, y));
        return 0;
    }

    /**
     * Attempts to add a new Line entity to the sketch.
     * @param x1 The X-coordinate of the start point.
     * @param y1 The Y-coordinate of the start point.
     * @param x2 The X-coordinate of the end point.
     * @param y2 The Y-coordinate of the end point.
     * @return 0 if successful, 1 if the sketch buffer is full.
     */
    public int addLine(float x1, float y1, float x2, float y2) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full: Cannot add more lines.");
            return 1;
        }
        sketchEntities.add(new Line(x1, y1, x2, y2));
        return 0;
    }

    /**
     * Attempts to add a new Circle entity to the sketch.
     * @param x The X-coordinate of the circle's center.
     * @param y The Y-coordinate of the circle's center.
     * @param r The radius of the circle.
     * @return 0 if successful, 1 if the sketch buffer is full.
     */
    public int addCircle(float x, float y, float r) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full: Cannot add more circles.");
            return 1;
        }
        sketchEntities.add(new Circle(x, y, r));
        return 0;
    }

    /**
     * Attempts to add a new Polygon entity to the sketch.
     * @param points A list of PointEntity objects defining the polygon's vertices.
     * @return 0 if successful, 1 if the sketch buffer is full or input points are invalid.
     */
    public int addPolygon(List<PointEntity> points) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full: Cannot add more polygons.");
            return 1;
        }
        try {
            sketchEntities.add(new Polygon(points));
            return 0;
        } catch (IllegalArgumentException e) {
            System.out.println("Error adding polygon: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Adds an N-sided regular polygon to the sketch.
     * @param centerX The X-coordinate of the polygon's center.
     * @param centerY The Y-coordinate of the polygon's center.
     * @param radius The radius of the circumcircle of the polygon.
     * @param sides The number of sides for the polygon (must be between 3 and 25).
     * @return 0 if successful, 1 if input parameters are invalid or sketch buffer is full.
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

        List<PointEntity> points = new ArrayList<>();
        double angleStep = 2 * Math.PI / sides; // Calculate the angle between vertices.

        for (int i = 0; i < sides; i++) {
            double angle = i * angleStep;
            float x = centerX + (float)(radius * Math.cos(angle));
            float y = centerY + (float)(radius * Math.sin(angle));
            points.add(new PointEntity(x, y));
        }

        return addPolygon(points); // Reuse the existing addPolygon method.
    }

    /**
     * Clears all entities from the sketch.
     */
    public void clearSketch() {
        sketchEntities.clear();
        System.out.println("Sketch cleared.");
    }

    /**
     * Returns a list of string representations for all entities currently in the sketch.
     * @return A list of strings, each representing an entity, or a message if the sketch is empty.
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

    // --- Unit Management ---

    /**
     * Sets the current units for the sketch.
     * @param units The unit string (e.g., "mm", "in", "cm"). Case-insensitive.
     */
    public void setUnits(String units) {
        this.units = units.toLowerCase();
        System.out.println("Sketch units set to: " + this.units);
    }

    /**
     * Gets the current units of the sketch.
     * @return The current unit string.
     */
    public String getUnits() {
        return this.units;
    }

    /**
     * Converts a unit string to its corresponding DXF unit code.
     * @param unitStr The unit string (e.g., "mm", "in").
     * @return The DXF unit code, or 0 if unit is unknown.
     */
    private int getDXFUnitCode(String unitStr) {
        return switch (unitStr.toLowerCase()) {
            case "in", "inch", "inches" -> 1; // Inches
            case "ft", "feet" -> 2; // Feet
            case "mm", "millimeter", "millimeters" -> 4; // Millimeters
            case "cm", "centimeter", "centimeters" -> 5; // Centimeters
            case "m", "meter", "meters" -> 6; // Meters
            default -> 0; // Unitless (or other unhandled units)
        };
    }

    /**
     * Converts a DXF unit code to its corresponding unit string.
     * @param code The DXF unit code.
     * @return The unit string (e.g., "in", "mm"), or "unitless" if the code is unknown.
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
     * Calculates the scale factor to convert given units to millimeters.
     * @param units The unit string (e.g., "in", "ft", "mm").
     * @return The conversion factor to multiply by to get millimeters.
     */
    private float unitScaleFactor(String units) {
        return switch (units) {
            case "in" -> 25.4f; // 1 inch = 25.4 mm
            case "ft" -> 304.8f; // 1 foot = 304.8 mm
            case "mm" -> 1.0f; // 1 millimeter = 1 mm
            case "cm" -> 10.0f; // 1 centimeter = 10 mm
            case "m" -> 1000.0f; // 1 meter = 1000 mm
            default -> 1.0f; // Default to 1.0 if units are unknown (assuming no conversion needed)
        };
    }

    /**
     * Scales a list of PointEntity objects by a given factor.
     * @param points The original list of points.
     * @param scale The scale factor.
     * @return A new list of scaled PointEntity objects.
     */
    private List<PointEntity> scalePoints(List<PointEntity> points, float scale) {
        List<PointEntity> scaled = new ArrayList<>();
        for (PointEntity p : points) {
            scaled.add(new PointEntity(p.x * scale, p.y * scale));
        }
        return scaled;
    }

    /**
     * Helper method to add an entity to the sketch based on parsed DXF data.
     * This method centralizes the logic for adding different entity types after parsing.
     * @param entityType The type of entity as a string (e.g., "POINT", "LINE").
     * @param x1 X-coordinate for points and line start.
     * @param y1 Y-coordinate for points and line start.
     * @param x2 X-coordinate for line end.
     * @param y2 Y-coordinate for line end.
     * @param cx X-coordinate for circle center.
     * @param cy Y-coordinate for circle center.
     * @param radius Radius for circle.
     * @param polyPoints List of points for a polygon (can be null for non-polygon entities).
     */
    private void addEntity(String entityType, float x1, float y1, float x2, float y2,
                           float cx, float cy, float radius, List<PointEntity> polyPoints) {
        switch (entityType) {
            case "POINT":
                this.addPoint(x1, y1);
                break;
            case "LINE":
                this.addLine(x1, y1, x2, y2);
                break;
            case "CIRCLE":
                this.addCircle(cx, cy, radius);
                break;
            case "POLYLINE":
                if (polyPoints != null && polyPoints.size() >= 3) {
                    this.addPolygon(polyPoints);
                } else {
                    System.err.println("Warning: Skipping invalid polygon (POLYLINE) with less than 3 points.");
                }
                break;
            default:
                System.out.println("Unknown entity type encountered during DXF load: " + entityType);
                break;
        }
    }

    // --- DXF Import/Export ---

    /**
     * Exports the current sketch to a DXF file.
     * The DXF file will include header information with the current units and entity data.
     * @param filename The name of the DXF file to create.
     */
    public void exportSketchToDXF(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            // DXF Header Section
            out.println("0\nSECTION\n2\nHEADER");
            out.println("9\n$INSUNITS"); // Units header variable
            out.println("70");
            out.println(getDXFUnitCode(this.units)); // Write current units code
            out.println("0\nENDSEC");

            // DXF Entities Section
            out.println("0\nSECTION\n2\nENTITIES");

            for (Entity e : sketchEntities) {
                if (e instanceof PointEntity p) {
                    out.println("0\nPOINT\n8\n0"); // Entity type and layer
                    out.println("10\n" + p.x); // X coordinate
                    out.println("20\n" + p.y); // Y coordinate
                } else if (e instanceof Line l) {
                    out.println("0\nLINE\n8\n0"); // Entity type and layer
                    out.println("10\n" + l.x1); // Start X
                    out.println("20\n" + l.y1); // Start Y
                    out.println("11\n" + l.x2); // End X
                    out.println("21\n" + l.y2); // End Y
                } else if (e instanceof Circle c) {
                    out.println("0\nCIRCLE\n8\n0"); // Entity type and layer
                    out.println("10\n" + c.x); // Center X
                    out.println("20\n" + c.y); // Center Y
                    out.println("40\n" + c.r); // Radius
                } else if (e instanceof Polygon poly) {
                    out.println("0\nPOLYLINE\n8\n0"); // Entity type and layer
                    out.println("66\n1"); // Entities follow (polyline specific)
                    out.println("70\n1"); // Closed polyline flag (1 for closed, 0 for open)

                    for (PointEntity p : poly.points) {
                        out.println("0\nVERTEX\n8\n0"); // Vertex entity and layer
                        out.println("10\n" + p.x); // Vertex X
                        out.println("20\n" + p.y); // Vertex Y
                    }
                    out.println("0\nSEQEND\n8\n0"); // End of sequence for polyline
                }
            }

            out.println("0\nENDSEC"); // End of Entities Section
            out.println("0\nEOF"); // End of file

            System.out.println("Sketch exported to " + filename);
        } catch (IOException e) {
            System.err.println("Error exporting DXF to " + filename + ": " + e.getMessage());
        }
    }

    /**
     * Loads a sketch from a DXF file, clearing the current sketch and applying scaling based on DXF units.
     * @param filename The name of the DXF file to load.
     * @throws IOException if an I/O error occurs during file reading.
     */
    public void loadDXF(String filename) throws IOException {
        clearSketch(); // Start with a clean slate.
        this.units = "unitless"; // Default unit until read from DXF header.

        // First pass: Read header to determine units.
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean inHeaderSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("0")) {
                    String nextLine = reader.readLine();
                    if (nextLine != null && nextLine.equalsIgnoreCase("SECTION")) {
                        reader.readLine(); // Read group code 2
                        String sectionName = reader.readLine(); // Read section name
                        if (sectionName != null && sectionName.equalsIgnoreCase("HEADER")) {
                            inHeaderSection = true;
                        } else if (sectionName != null && sectionName.equalsIgnoreCase("ENTITIES")) {
                            // No need to process further in the first pass once entities section starts
                            break;
                        }
                    }
                } else if (inHeaderSection) {
                    if (line.equalsIgnoreCase("9")) { // Group code for system variable name
                        String variableName = reader.readLine();
                        if (variableName != null && variableName.equalsIgnoreCase("$INSUNITS")) {
                            reader.readLine(); // Group code 70
                            String valueLine = reader.readLine(); // The actual unit code
                            if (valueLine != null) {
                                try {
                                    int code = Integer.parseInt(valueLine.trim());
                                    this.units = getUnitsFromDXFCode(code);
                                } catch (NumberFormatException e) {
                                    System.err.println("Warning: Invalid $INSUNITS code in DXF header. Using default units.");
                                }
                            }
                        }
                    } else if (line.equalsIgnoreCase("0")) {
                        String nextLine = reader.readLine();
                        if (nextLine != null && nextLine.equalsIgnoreCase("ENDSEC")) {
                            inHeaderSection = false; // End of header section
                            break; // Exit header parsing
                        }
                    }
                }
            }
        }

        // Determine the scale factor based on the units read from the header.
        float scale = unitScaleFactor(units);
        System.out.println("Loading DXF with units: " + units + " (scale factor: " + scale + ")");

        // Second pass: Read entities.
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntityType = null; // Stores the type of the entity currently being parsed.
            // Coordinates and radius for various entities, initialized to 0.
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List<PointEntity> polyPoints = null; // List to accumulate points for a polygon.
            float tempVertexX = 0, tempVertexY = 0; // Temporary storage for vertex coordinates.

            boolean inEntitiesSection = false;
            boolean inPolyline = false; // Flag to indicate if we are inside a POLYLINE definition.
            boolean inVertex = false;   // Flag to indicate if we are inside a VERTEX definition within a POLYLINE.

            String valueString = null; // Stores the value corresponding to a group code.

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) {
                    continue; // Skip empty lines.
                }

                if (line.equalsIgnoreCase("0")) { // Group code 0 always indicates a new object or section.
                    String entityType = reader.readLine(); // Read the next line which is the entity type.
                    if (entityType == null) break; // End of file.
                    entityType = entityType.trim().toUpperCase();

                    if (entityType.equals("SECTION")) {
                        reader.readLine(); // Read group code 2
                        String sectionName = reader.readLine(); // Read section name
                        if (sectionName != null && sectionName.equalsIgnoreCase("ENTITIES")) {
                            inEntitiesSection = true; // Enter entities section.
                        } else {
                            inEntitiesSection = false; // Exit entities section.
                        }
                        continue;
                    } else if (entityType.equals("ENDSEC")) {
                        // If we were in a polyline and finished the section, add it.
                        if (inPolyline && polyPoints != null && polyPoints.size() >= 3) {
                            addEntity("POLYLINE", 0, 0, 0, 0, 0, 0, 0, scalePoints(polyPoints, scale));
                        }
                        inEntitiesSection = false;
                        inPolyline = false;
                        polyPoints = null;
                        currentEntityType = null; // Reset current entity type.
                        continue;
                    } else if (entityType.equals("EOF")) {
                        break; // End of file.
                    }

                    if (!inEntitiesSection) {
                        continue; // Only process entities within the ENTITIES section.
                    }

                    // If we finished parsing a non-polyline entity, add it before starting a new one.
                    if (currentEntityType != null && !inPolyline && !entityType.equals("VERTEX") && !entityType.equals("SEQEND")) {
                        addEntity(currentEntityType, x1 * scale, y1 * scale, x2 * scale, y2 * scale,
                                cx * scale, cy * scale, radius * scale, null);
                    }

                    currentEntityType = entityType; // Set the new entity type.
                    // Reset coordinates for the new entity.
                    x1 = y1 = x2 = y2 = cx = cy = radius = 0;

                    if (currentEntityType.equals("POLYLINE")) {
                        inPolyline = true;
                        polyPoints = new ArrayList<>(); // Initialize list for polyline vertices.
                    } else if (currentEntityType.equals("VERTEX") && inPolyline) {
                        // This indicates a vertex within a polyline.
                        inVertex = true;
                        tempVertexX = 0;
                        tempVertexY = 0;
                    } else if (currentEntityType.equals("SEQEND") && inPolyline) {
                        // End of a POLYLINE entity.
                        if (polyPoints != null && polyPoints.size() >= 3) {
                            addEntity("POLYLINE", 0, 0, 0, 0, 0, 0, 0, scalePoints(polyPoints, scale));
                        }
                        inPolyline = false;
                        polyPoints = null;
                        currentEntityType = null; // Reset to avoid re-adding in the next loop.
                    }

                } else if (inEntitiesSection) {
                    // Process group codes and their values within the entities section.
                    int groupCode;
                    try {
                        groupCode = Integer.parseInt(line);
                        valueString = reader.readLine();
                        if (valueString == null) break;
                        valueString = valueString.trim();

                        switch (groupCode) {
                            case 10: // X coordinate (for POINT, LINE start, CIRCLE center)
                                if (inVertex && inPolyline) {
                                    tempVertexX = Float.parseFloat(valueString);
                                } else {
                                    if ("POINT".equals(currentEntityType)) x1 = Float.parseFloat(valueString);
                                    else if ("LINE".equals(currentEntityType)) x1 = Float.parseFloat(valueString);
                                    else if ("CIRCLE".equals(currentEntityType)) cx = Float.parseFloat(valueString);
                                }
                                break;
                            case 20: // Y coordinate (for POINT, LINE start, CIRCLE center)
                                if (inVertex && inPolyline) {
                                    tempVertexY = Float.parseFloat(valueString);
                                    polyPoints.add(new PointEntity(tempVertexX, tempVertexY)); // Add vertex to polygon.
                                    inVertex = false; // Finished reading vertex coordinates.
                                } else {
                                    if ("POINT".equals(currentEntityType)) y1 = Float.parseFloat(valueString);
                                    else if ("LINE".equals(currentEntityType)) y1 = Float.parseFloat(valueString);
                                    else if ("CIRCLE".equals(currentEntityType)) cy = Float.parseFloat(valueString);
                                }
                                break;
                            case 11: // X coordinate for LINE end point.
                                if ("LINE".equals(currentEntityType)) x2 = Float.parseFloat(valueString);
                                break;
                            case 21: // Y coordinate for LINE end point.
                                if ("LINE".equals(currentEntityType)) y2 = Float.parseFloat(valueString);
                                break;
                            case 40: // Radius for CIRCLE.
                                if ("CIRCLE".equals(currentEntityType)) radius = Float.parseFloat(valueString);
                                break;
                            case 70: // Polyline flag (for POLYLINE). Not directly used for geometry, but parsed.
                                break;
                            default:
                                // Ignore other group codes for now.
                                break;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number format encountered during DXF load. Group code line: '" + line + "', Value line: '" + valueString + "'. Error: " + e.getMessage());
                    }
                }
            }

            // Add the last parsed entity if it was not a polyline and not already added.
            if (currentEntityType != null && !inPolyline) {
                addEntity(currentEntityType, x1 * scale, y1 * scale, x2 * scale, y2 * scale,
                        cx * scale, cy * scale, radius * scale, null);
            }
        }
        System.out.println("DXF file '" + filename + "' loaded successfully.");
    }

    // --- Command Line Utilities (for hypothetical console interaction) ---

    /**
     * Parses parameters and adds a PointEntity to the sketch.
     * @param params An array of strings containing x and y coordinates.
     * @return 0 if successful, 1 if parameters are invalid.
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
            System.out.println("Invalid coordinates for point. Please enter numeric values.");
            return 1;
        }
    }

    /**
     * Parses parameters and adds a Line entity to the sketch.
     * @param params An array of strings containing x1, y1, x2, y2 coordinates.
     * @return 0 if successful, 1 if parameters are invalid.
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
            System.out.println("Invalid coordinates for line. Please enter numeric values.");
            return 1;
        }
    }

    /**
     * Parses parameters and adds a Circle entity to the sketch.
     * @param params An array of strings containing x, y (center) and r (radius).
     * @return 0 if successful, 1 if parameters are invalid.
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
            System.out.println("Invalid parameters for circle. Please enter numeric values.");
            return 1;
        }
    }

    /**
     * Creates and adds a regular N-sided polygon to the sketch based on parsed parameters.
     * This method is an overload for adding polygons with a center, radius, and number of sides.
     * @param x The X-coordinate of the polygon's center.
     * @param y The Y-coordinate of the polygon's center.
     * @param radius The radius of the circumcircle.
     * @param sides The number of sides.
     * @return 0 if successful, 1 if parameters are invalid.
     */
    public int sketchPolygon(float x, float y, float radius, int sides) {
        return addNSidedPolygon(x, y, radius, sides);
    }

    // --- Graphics Rendering ---

    /**
     * Draws all entities in the sketch onto the given Graphics context.
     * It automatically calculates a scaling factor and offset to fit the sketch
     * within the drawing area, centering the sketch.
     * @param g The Graphics context to draw on.
     */
    public void draw(Graphics g) {
        if (sketchEntities.isEmpty()) {
            return; // Nothing to draw.
        }

        // Determine the bounding box of all entities to calculate appropriate scaling.
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
        boolean firstEntity = true;

        for (Entity e : sketchEntities) {
            if (e instanceof PointEntity p) {
                if (firstEntity) {
                    minX = p.x; maxX = p.x;
                    minY = p.y; maxY = p.y;
                    firstEntity = false;
                } else {
                    minX = Math.min(minX, p.x);
                    maxX = Math.max(maxX, p.x);
                    minY = Math.min(minY, p.y);
                    maxY = Math.max(maxY, p.y);
                }
            } else if (e instanceof Line l) {
                if (firstEntity) {
                    minX = Math.min(l.x1, l.x2); maxX = Math.max(l.x1, l.x2);
                    minY = Math.min(l.y1, l.y2); maxY = Math.max(l.y1, l.y2);
                    firstEntity = false;
                } else {
                    minX = Math.min(minX, Math.min(l.x1, l.x2));
                    maxX = Math.max(maxX, Math.max(l.x1, l.x2));
                    minY = Math.min(minY, Math.min(l.y1, l.y2));
                    maxY = Math.max(maxY, Math.max(l.y1, l.y2)); // Corrected Math.max for y2
                }
            } else if (e instanceof Circle c) {
                if (firstEntity) {
                    minX = c.x - c.r; maxX = c.x + c.r;
                    minY = c.y - c.r; maxY = c.y + c.r;
                    firstEntity = false;
                } else {
                    minX = Math.min(minX, c.x - c.r);
                    maxX = Math.max(maxX, c.x + c.r);
                    minY = Math.min(minY, c.y - c.r);
                    maxY = Math.max(maxY, c.y + c.r);
                }
            } else if (e instanceof Polygon poly) {
                for (PointEntity p : poly.points) {
                    if (firstEntity) {
                        minX = p.x; maxX = p.x;
                        minY = p.y; maxY = p.y;
                        firstEntity = false;
                    } else {
                        minX = Math.min(minX, p.x);
                        maxX = Math.max(maxX, p.x);
                        minY = Math.min(minY, p.y);
                        maxY = Math.max(maxY, p.y);
                    }
                }
            }
        }

        // Handle cases where sketch might be a single point or a vertical/horizontal line
        // to avoid division by zero or extreme scaling.
        if (maxX == minX) {
            minX -= 10; // Add some arbitrary buffer for display
            maxX += 10;
        }
        if (maxY == minY) {
            minY -= 10;
            maxY += 10;
        }

        int canvasWidth = 800; // Default canvas width
        int canvasHeight = 800; // Default canvas height

        // Try to get actual canvas dimensions from the Graphics clip bounds if available.
        if (g.getClipBounds() != null) {
            int clipWidth = g.getClipBounds().width;
            int clipHeight = g.getClipBounds().height;
            if (clipWidth > 0 && clipHeight > 0) {
                canvasWidth = clipWidth;
                canvasHeight = clipHeight;
            }
        }

        int margin = 20; // Margin around the sketch within the canvas.

        float sketchWidth = maxX - minX;
        float sketchHeight = maxY - minY;

        // Ensure sketch dimensions are not zero for scaling calculation.
        if (sketchWidth == 0) sketchWidth = 1.0f;
        if (sketchHeight == 0) sketchHeight = 1.0f;

        // Calculate scaling factors for X and Y, then take the minimum to maintain aspect ratio.
        float scaleX = (float)(canvasWidth - 2 * margin) / sketchWidth;
        float scaleY = (float)(canvasHeight - 2 * margin) / sketchHeight;
        float scale = Math.min(scaleX, scaleY);

        // Calculate offset to center the sketch.
        float offsetX = (canvasWidth - sketchWidth * scale) / 2 - minX * scale;
        float offsetY = (canvasHeight - sketchHeight * scale) / 2 - minY * scale;

        // Draw each entity using the calculated scale and offset.
        for (Entity e : sketchEntities) {
            if (e instanceof PointEntity p) {
                drawPoint(g, p, offsetX, offsetY, scale);
            } else if (e instanceof Line l) {
                drawLine(g, l, offsetX, offsetY, scale);
            } else if (e instanceof Circle c) {
                drawCircle(g, c, offsetX, offsetY, scale);
            } else if (e instanceof Polygon poly) {
                drawPolygon(g, poly, offsetX, offsetY, scale);
            }
        }
    }

    /**
     * Draws a PointEntity on the Graphics context.
     * @param g The Graphics context.
     * @param p The PointEntity to draw.
     * @param offsetX The X-offset for translation.
     * @param offsetY The Y-offset for translation.
     * @param scale The scaling factor.
     */
    private void drawPoint(Graphics g, PointEntity p, float offsetX, float offsetY, float scale) {
        int x = (int)(p.x * scale + offsetX);
        int y = (int)(p.y * scale + offsetY);
        int size = 4; // Size of the point representation.
        g.fillOval(x - size / 2, y - size / 2, size, size); // Draw a small filled circle for the point.
    }

    /**
     * Draws a Line entity on the Graphics context.
     * @param g The Graphics context.
     * @param l The Line entity to draw.
     * @param offsetX The X-offset for translation.
     * @param offsetY The Y-offset for translation.
     * @param scale The scaling factor.
     */
    private void drawLine(Graphics g, Line l, float offsetX, float offsetY, float scale) {
        int x1 = (int)(l.x1 * scale + offsetX);
        int y1 = (int)(l.y1 * scale + offsetY);
        int x2 = (int)(l.x2 * scale + offsetX);
        int y2 = (int)(l.y2 * scale + offsetY);
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     * Draws a Circle entity on the Graphics context.
     * @param g The Graphics context.
     * @param c The Circle entity to draw.
     * @param offsetX The X-offset for translation.
     * @param offsetY The Y-offset for translation.
     * @param scale The scaling factor.
     */
    private void drawCircle(Graphics g, Circle c, float offsetX, float offsetY, float scale) {
        int x = (int)(c.x * scale + offsetX);
        int y = (int)(c.y * scale + offsetY);
        int r = (int)(c.r * scale); // Scaled radius.
        g.drawOval(x - r, y - r, 2 * r, 2 * r); // Draw an oval (circle if width=height).
    }

    /**
     * Draws a Polygon entity on the Graphics context.
     * @param g The Graphics context.
     * @param poly The Polygon entity to draw.
     * @param offsetX The X-offset for translation.
     * @param offsetY The Y-offset for translation.
     * @param scale The scaling factor.
     */
    private void drawPolygon(Graphics g, Polygon poly, float offsetX, float offsetY, float scale) {
        int n = poly.points.size();
        int[] xPoints = new int[n];
        int[] yPoints = new int[n];
        for (int i = 0; i < n; i++) {
            xPoints[i] = (int)(poly.points.get(i).x * scale + offsetX);
            yPoints[i] = (int)(poly.points.get(i).y * scale + offsetY);
        }
        g.drawPolygon(xPoints, yPoints, n);
    }
}
