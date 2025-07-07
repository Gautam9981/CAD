package core;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.awt.Graphics;

/**
 * Represents a 2D Sketch consisting of various geometric entities
 * like points, lines, circles, and polygons.
 */
public class Sketch {

    /**
     * Enumeration of supported sketch entity types.
     */
    public enum TypeSketch {
        POINT,
        LINE,
        CIRCLE,
        POLYGON
    }

    /**
     * Abstract base class for sketch entities.
     * Each entity has a TypeSketch type.
     */
    public static abstract class Entity {
        TypeSketch type;
    }

    /**
     * Represents a 2D point entity.
     */
    public static class PointEntity extends Entity {
        float x, y;

        /**
         * Constructs a PointEntity at coordinates (x, y).
         */
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
     * Represents a 2D line segment defined by two endpoints.
     */
    public static class Line extends Entity {
        float x1, y1, x2, y2;

        /**
         * Constructs a Line from (x1, y1) to (x2, y2).
         */
        public Line(float x1, float y1, float x2, float y2) {
            this.type = TypeSketch.LINE;
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        public String toString() {
            return String.format("Line from (%.3f, %.3f) to (%.3f, %.3f)", x1, y1, x2, y2);
        }
    }

    /**
     * Represents a 2D circle defined by center and radius.
     */
    public static class Circle extends Entity {
        float x, y, r;

        /**
         * Constructs a Circle at (x, y) with radius r.
         */
        public Circle(float x, float y, float r) {
            this.type = TypeSketch.CIRCLE;
            this.x = x;
            this.y = y;
            this.r = r;
        }

        @Override
        public String toString() {
            return String.format("Circle at (%.3f, %.3f) with radius %.3f", x, y, r);
        }
    }

    /**
     * Represents a polygon defined by a list of vertices.
     * Polygon must have between 3 and 25 points.
     */
    public static class Polygon extends Entity {
        List<PointEntity> points;

        /**
         * Constructs a Polygon from a list of PointEntity objects.
         * @param points list of vertices
         * @throws IllegalArgumentException if points are null or count not in [3, 25]
         */
        public Polygon(List<PointEntity> points) {
            if (points == null || points.size() < 3 || points.size() > 25) {
                throw new IllegalArgumentException("Polygon must have between 3 and 25 points.");
            }
            this.type = TypeSketch.POLYGON;
            this.points = new ArrayList<>(points);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Polygon with points: ");
            for (PointEntity p : points) {
                sb.append(String.format("(%.2f, %.2f) ", p.x, p.y));
            }
            return sb.toString();
        }
    }

    // Maximum number of entities allowed in the sketch buffer
    private static final int MAX_SKETCH_ENTITIES = 1000;

    // List holding all sketch entities
    private final List<Entity> sketchEntities = new ArrayList<>();

    /**
     * Adds a point to the sketch.
     * @param x x-coordinate
     * @param y y-coordinate
     * @return 0 if success, 1 if buffer full
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
     * Adds a line segment to the sketch.
     * @param x1 start x-coordinate
     * @param y1 start y-coordinate
     * @param x2 end x-coordinate
     * @param y2 end y-coordinate
     * @return 0 if success, 1 if buffer full
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
     * Adds a circle to the sketch.
     * @param x center x-coordinate
     * @param y center y-coordinate
     * @param r radius (must be positive)
     * @return 0 if success, 1 if buffer full
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
     * Adds a polygon to the sketch.
     * @param points list of polygon vertices
     * @return 0 if success, 1 if buffer full or invalid polygon
     */
    public int addPolygon(List<PointEntity> points) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
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
     * Adds a regular N-sided polygon to the sketch.
     * @param centerX x-coordinate of polygon center
     * @param centerY y-coordinate of polygon center
     * @param radius radius of the polygon (must be positive)
     * @param sides number of sides (between 3 and 25)
     * @return 0 if success, 1 if invalid input or buffer full
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
    }

    /**
     * Prints all entities currently in the sketch to standard output.
     */
    public void listSketch() {
        if (sketchEntities.isEmpty()) {
            System.out.println("Sketch is empty.");
            return;
        }
        for (Entity e : sketchEntities) {
            System.out.println(e);
        }
    }

    // Current units for this sketch (default: millimeters)
    private String units = "mm";

    /**
     * Sets the current unit string for the sketch.
     * @param units unit string (e.g. "mm", "cm", "in", "ft", "m")
     */
    public void setUnits(String units) {
        this.units = units.toLowerCase();
    }

    /**
     * Gets the current unit string used by the sketch.
     * @return current units string
     */
    public String getUnits() {
        return this.units;
    }

    /**
     * Converts unit string to DXF code.
     * @param unitStr unit string
     * @return corresponding DXF unit code
     */
    private int getDXFUnitCode(String unitStr) {
        return switch (unitStr.toLowerCase()) {
            case "in", "inch", "inches" -> 1;
            case "ft", "feet" -> 2;
            case "mm", "millimeter", "millimeters" -> 4;
            case "cm", "centimeter", "centimeters" -> 5;
            case "m", "meter", "meters" -> 6;
            default -> 0;
        };
    }

    /**
     * Converts DXF unit code to unit string.
     * @param code DXF unit code
     * @return string representation of units
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
 * Exports the current sketch entities to a DXF file.
 * Supports POINT, LINE, CIRCLE, and POLYGON entities.
 * 
 * @param filename the output DXF filename
 */
public void exportSketchToDXF(String filename) {
    try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
        // Start the HEADER section of the DXF file
        out.println("0");
        out.println("SECTION");
        out.println("2");
        out.println("HEADER");

        // Write the insertion units variable ($INSUNITS)
        out.println("9");
        out.println("$INSUNITS");
        out.println("70");
        out.println(getDXFUnitCode(this.units));  // Convert current units to DXF code

        // End HEADER section
        out.println("0");
        out.println("ENDSEC");

        // Start ENTITIES section where all shapes are defined
        out.println("0");
        out.println("SECTION");
        out.println("2");
        out.println("ENTITIES");

        // Loop through each entity in the sketch
        for (Entity e : sketchEntities) {
            if (e instanceof PointEntity) {
                PointEntity p = (PointEntity) e;
                // Write a POINT entity in DXF format
                out.println("0");
                out.println("POINT");
                out.println("8");  // Layer name group code
                out.println("0");
                out.println("10"); // X coordinate group code
                out.println(p.x);
                out.println("20"); // Y coordinate group code
                out.println(p.y);
            } else if (e instanceof Line) {
                Line l = (Line) e;
                // Write a LINE entity
                out.println("0");
                out.println("LINE");
                out.println("8");
                out.println("0");
                out.println("10");
                out.println(l.x1);
                out.println("20");
                out.println(l.y1);
                out.println("11"); // Endpoint X
                out.println(l.x2);
                out.println("21"); // Endpoint Y
                out.println(l.y2);
            } else if (e instanceof Circle) {
                Circle c = (Circle) e;
                // Write a CIRCLE entity
                out.println("0");
                out.println("CIRCLE");
                out.println("8");
                out.println("0");
                out.println("10"); // Center X
                out.println(c.x);
                out.println("20"); // Center Y
                out.println(c.y);
                out.println("40"); // Radius group code
                out.println(c.r);
            } else if (e instanceof Polygon) {
                Polygon poly = (Polygon) e;
                // Start POLYLINE entity for the polygon
                out.println("0");
                out.println("POLYLINE");
                out.println("8");
                out.println("0");
                out.println("66"); // Indicates presence of vertex entities
                out.println("1");
                out.println("70"); // Flags - 1 means closed polygon
                out.println("1");

                // Write each vertex as a VERTEX entity
                for (PointEntity p : poly.points) {
                    out.println("0");
                    out.println("VERTEX");
                    out.println("8");
                    out.println("0");
                    out.println("10"); // Vertex X
                    out.println(p.x);
                    out.println("20"); // Vertex Y
                    out.println(p.y);
                }

                // End of vertex sequence
                out.println("0");
                out.println("SEQEND");
                out.println("8");
                out.println("0");
            }
        }

        // End ENTITIES section
        out.println("0");
        out.println("ENDSEC");
        // End of DXF file
        out.println("0");
        out.println("EOF");

        System.out.println("Sketch exported to " + filename);
    } catch (IOException e) {
        System.out.println("Error exporting DXF: " + e.getMessage());
    }
}

/**
 * Parses parameters to add a point to the sketch.
 * Validates input length and numeric parsing.
 * 
 * @param params array containing [x, y] as strings
 * @return 0 if point added successfully, 1 otherwise
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
 * Parses parameters to add a line to the sketch.
 * Validates input length and numeric parsing.
 * 
 * @param params array containing [x1, y1, x2, y2] as strings
 * @return 0 if line added successfully, 1 otherwise
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
 * Parses parameters to add a circle to the sketch.
 * Validates input length and numeric parsing.
 * 
 * @param params array containing [x, y, r] as strings
 * @return 0 if circle added successfully, 1 otherwise
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
 * Creates a regular polygon centered at (x, y) with the specified radius and number of sides.
 * Generates polygon vertices evenly spaced around the circle.
 * 
 * @param x center x-coordinate
 * @param y center y-coordinate
 * @param radius positive radius of polygon
 * @param sides number of sides, must be between 3 and 25
 * @return 0 if polygon added successfully, 1 otherwise
 */
public int sketchPolygon(float x, float y, float radius, int sides) {
    // Validate number of sides
    if (sides < 3 || sides > 25) {
        System.out.println("Polygon must have between 3 and 25 sides.");
        return 1;
    }
    // Validate radius
    if (radius <= 0) {
        System.out.println("Radius must be positive.");
        return 1;
    }

    List<PointEntity> points = new ArrayList<>();
    double angleStep = 2 * Math.PI / sides;  // Angle between each vertex

    // Calculate and add each vertex point
    for (int i = 0; i < sides; i++) {
        double angle = i * angleStep;
        float px = x + (float) (radius * Math.cos(angle));
        float py = y + (float) (radius * Math.sin(angle));
        points.add(new PointEntity(px, py));
    }

    // Add the constructed polygon to sketch
    return addPolygon(points);
}

/**
 * Loads a DXF file, reads header to set units, and clears current sketch.
 * Note: Only reads the HEADER section for units, does not parse entities.
 * 
 * @param filename path to the DXF file
 * @throws IOException if file cannot be read
 */
public void loadDXF(String filename) throws IOException {
    clearSketch(); // Clear existing sketch before loading new data
    this.units = "unitless"; // Default units before reading from file

    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
        String line;

        // Read the file line by line
        while ((line = reader.readLine()) != null) {
            line = line.trim();

            // Look for the start of a SECTION block
            if (line.equalsIgnoreCase("0")) {
                String nextLine = reader.readLine();
                if (nextLine != null && nextLine.equalsIgnoreCase("SECTION")) {
                    nextLine = reader.readLine();
                    if (nextLine != null && nextLine.equalsIgnoreCase("2")) {
                        nextLine = reader.readLine();
                        // Check if this is the HEADER section
                        if (nextLine != null && nextLine.equalsIgnoreCase("HEADER")) {
                            // Read inside HEADER section
                            while ((line = reader.readLine()) != null) {
                                line = line.trim();

                                // End of HEADER section
                                if (line.equalsIgnoreCase("0")) {
                                    nextLine = reader.readLine();
                                    if (nextLine != null && nextLine.equalsIgnoreCase("ENDSEC")) {
                                        break;
                                    }
                                } 
                                // Check for $INSUNITS variable code
                                else if (line.equalsIgnoreCase("9")) {
                                    nextLine = reader.readLine();
                                    if (nextLine != null && nextLine.equalsIgnoreCase("$INSUNITS")) {
                                        reader.readLine(); // Skip group code 70
                                        String valueLine = reader.readLine();
                                        if (valueLine != null) {
                                            try {
                                                int code = Integer.parseInt(valueLine.trim());
                                                this.units = getUnitsFromDXFCode(code);
                                            } catch (NumberFormatException e) {
                                                System.err.println("Warning: Invalid $INSUNITS code in DXF header. Using default units.");
                                            }
                                        }
                                    }
                                }
                            }
                            break; // Done with HEADER, exit outer loop
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns the scale factor to convert from the given units to millimeters.
 *
 * @param units Unit string like "mm", "cm", "in"
 * @return Corresponding scale factor
 */
private float unitScaleFactor(String units) {
    return switch (units) {
        case "in" -> 25.4f;       // inches to mm
        case "ft" -> 304.8f;      // feet to mm
        case "mm" -> 1.0f;        // mm to mm
        case "cm" -> 10.0f;       // cm to mm
        case "m"  -> 1000.0f;     // meters to mm
        default   -> 1.0f;        // unknown units, assume mm
    };
}

/**
 * Scales a list of points by the given scale factor.
 *
 * @param points List of original points
 * @param scale Scale factor to apply
 * @return New list of scaled points
 */
private List<PointEntity> scalePoints(List<PointEntity> points, float scale) {
    List<PointEntity> scaled = new ArrayList<>();
    for (PointEntity p : points) {
        scaled.add(new PointEntity(p.x * scale, p.y * scale));
    }
    return scaled;
}

/**
 * Adds a sketch entity to the internal list, based on type and parameters.
 *
 * @param entityType DXF entity type ("POINT", "LINE", "CIRCLE", "POLYLINE")
 * @param x1 start x, y1 start y (used by POINT, LINE)
 * @param x2 end x, y2 end y (used by LINE)
 * @param cx center x, cy center y (used by CIRCLE)
 * @param radius radius (used by CIRCLE)
 * @param polyPoints list of points (used by POLYLINE)
 */
private void addEntity(String entityType, float x1, float y1, float x2, float y2,
                       float cx, float cy, float radius, List<PointEntity> polyPoints) {
    switch (entityType) {
        case "POINT" -> this.addPoint(x1, y1);
        case "LINE"  -> this.addLine(x1, y1, x2, y2);
        case "CIRCLE"-> this.addCircle(cx, cy, radius);
        case "POLYLINE" -> {
            if (polyPoints != null && polyPoints.size() >= 3) {
                this.addPolygon(polyPoints);
            } else {
                System.err.println("Warning: Skipping invalid polygon with less than 3 points.");
            }
        }
        default -> System.out.println("Unknown entity type encountered: " + entityType);
    }
}

/**
 * Draws the sketch entities on a Graphics context.
 *
 * @param g The Graphics context where the entities will be rendered.
 */
public void draw(Graphics g) {
    if (sketchEntities.isEmpty()) return;

    // Initialize bounding box
    float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
    float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

    boolean hasValidBounds = false;

    // Calculate bounding box
    for (Entity e : sketchEntities) {
        float[] bounds = getEntityBounds(e);
        if (bounds == null) continue;

        float exMinX = bounds[0], exMaxX = bounds[1];
        float exMinY = bounds[2], exMaxY = bounds[3];

        if (!hasValidBounds) {
            minX = exMinX;
            maxX = exMaxX;
            minY = exMinY;
            maxY = exMaxY;
            hasValidBounds = true;
        } else {
            minX = Math.min(minX, exMinX);
            maxX = Math.max(maxX, exMaxX);
            minY = Math.min(minY, exMinY);
            maxY = Math.max(maxY, exMaxY);
        }
    }

    // Fallback bounding box if entities exist but all were null-bounded
    if (!hasValidBounds && !sketchEntities.isEmpty()) {
        float[] fallbackBounds = getEntityBounds(sketchEntities.get(0));
        if (fallbackBounds != null) {
            minX = fallbackBounds[0] - 10;
            maxX = fallbackBounds[1] + 10;
            minY = fallbackBounds[2] - 10;
            maxY = fallbackBounds[3] + 10;
        } else {
            return;
        }
    }

    // Determine canvas size
    int canvasWidth = 800, canvasHeight = 800;
    if (g.getClipBounds() != null) {
        canvasWidth = g.getClipBounds().width;
        canvasHeight = g.getClipBounds().height;
    }

    // Calculate scale and offset
    int margin = 20;
    float sketchWidth = Math.max(1.0f, maxX - minX);
    float sketchHeight = Math.max(1.0f, maxY - minY);

    float scaleX = (canvasWidth - 2f * margin) / sketchWidth;
    float scaleY = (canvasHeight - 2f * margin) / sketchHeight;
    float scale = Math.min(scaleX, scaleY);

    float offsetX = (canvasWidth - sketchWidth * scale) / 2 - minX * scale;
    float offsetY = (canvasHeight - sketchHeight * scale) / 2 - minY * scale;

    // Draw all entities
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
 * Returns the bounding box of an entity as [minX, maxX, minY, maxY].
 */
private float[] getEntityBounds(Entity e) {
    if (e instanceof PointEntity p) {
        return new float[]{p.x, p.x, p.y, p.y};
    } else if (e instanceof Line l) {
        float minX = Math.min(l.x1, l.x2);
        float maxX = Math.max(l.x1, l.x2);
        float minY = Math.min(l.y1, l.y2);
        float maxY = Math.max(l.y1, l.y2);
        return new float[]{minX, maxX, minY, maxY};
    } else if (e instanceof Circle c) {
        return new float[]{c.x - c.r, c.x + c.r, c.y - c.r, c.y + c.r};
    } else if (e instanceof Polygon poly && !poly.points.isEmpty()) {
        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        for (PointEntity p : poly.points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        return new float[]{minX, maxX, minY, maxY};
    }
    return null;
}

/**
 * Draws a point as a small filled circle.
 */
private void drawPoint(Graphics g, PointEntity p, float offsetX, float offsetY, float scale) {
    int x = (int) (p.x * scale + offsetX);
    int y = (int) (p.y * scale + offsetY);
    int size = 4;
    g.fillOval(x - size / 2, y - size / 2, size, size);
}

/**
 * Draws a line segment.
 */
private void drawLine(Graphics g, Line l, float offsetX, float offsetY, float scale) {
    int x1 = (int) (l.x1 * scale + offsetX);
    int y1 = (int) (l.y1 * scale + offsetY);
    int x2 = (int) (l.x2 * scale + offsetX);
    int y2 = (int) (l.y2 * scale + offsetY);
    g.drawLine(x1, y1, x2, y2);
}

/**
 * Draws a circle using center and radius.
 */
private void drawCircle(Graphics g, Circle c, float offsetX, float offsetY, float scale) {
    int x = (int) (c.x * scale + offsetX);
    int y = (int) (c.y * scale + offsetY);
    int r = (int) (c.r * scale);
    g.drawOval(x - r, y - r, 2 * r, 2 * r);
}

/**
 * Draws a polygon from a list of point entities.
 */
private void drawPolygon(Graphics g, Polygon poly, float offsetX, float offsetY, float scale) {
    int n = poly.points.size();
    int[] xPoints = new int[n];
    int[] yPoints = new int[n];

    for (int i = 0; i < n; i++) {
        PointEntity p = poly.points.get(i);
        xPoints[i] = (int) (p.x * scale + offsetX);
        yPoints[i] = (int) (p.y * scale + offsetY);
    }

    g.drawPolygon(xPoints, yPoints, n);
}

}