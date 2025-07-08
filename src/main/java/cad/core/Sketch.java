package cad.core;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.awt.Graphics; // Ensure this import is present if you use Graphics

public class Sketch {

    public enum TypeSketch {
        POINT,
        LINE,
        CIRCLE,
        POLYGON
    }

    public static abstract class Entity {
        TypeSketch type;
    }

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
    }

    private static final int MAX_SKETCH_ENTITIES = 1000;
    private final List < Entity > sketchEntities = new ArrayList < > ();

    public int addPoint(float x, float y) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new PointEntity(x, y));
        return 0;
    }

    public int addLine(float x1, float y1, float x2, float y2) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Line(x1, y1, x2, y2));
        return 0;
    }

    public int addCircle(float x, float y, float r) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Circle(x, y, r));
        return 0;
    }

    public int addPolygon(List < PointEntity > points) {
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

    public void clearSketch() {
        sketchEntities.clear();
    }

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

    private String units = "mm";

    public void setUnits(String units) {
        this.units = units.toLowerCase();
    }

    public String getUnits() {
        return this.units;
    }

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

    public void exportSketchToDXF(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("HEADER");

            out.println("9");
            out.println("$INSUNITS");
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
                    out.println("66"); // Entities follow (if vertices are separate)
                    out.println("1");
                    out.println("70"); // Polyline flags (1 = closed)
                    out.println("1"); // 1 for closed polyline (often 0 for open)

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
                    out.println("SEQEND");
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

    public void loadDXF(String filename) throws IOException {
        System.out.println("Attempting to load DXF file: " + filename);
        java.io.File file = new java.io.File(filename);
        System.out.println("Absolute path of file provided: " + file.getAbsolutePath());
        if (!file.exists()) {
            System.err.println("ERROR: File does not exist at path: " + file.getAbsolutePath());
            throw new java.io.FileNotFoundException("DXF file not found: " + filename); // Throw exception to stop
        }
        if (!file.canRead()) {
            System.err.println("ERROR: No read permission for file: " + file.getAbsolutePath());
            throw new IOException("No read permission for DXF file: " + filename); // Throw exception to stop
        }

        clearSketch();
        this.units = "unitless"; // Reset units before loading

        // First pass: Read header for units
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            boolean inHeaderSection = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("0")) {
                    String nextLine = reader.readLine();
                    if (nextLine != null && nextLine.equalsIgnoreCase("SECTION")) {
                        nextLine = reader.readLine(); // Group code 2
                        if (nextLine != null && nextLine.equalsIgnoreCase("2")) {
                            String sectionName = reader.readLine();
                            if (sectionName != null && sectionName.equalsIgnoreCase("HEADER")) {
                                inHeaderSection = true;
                            }
                        }
                    } else if (nextLine != null && nextLine.equalsIgnoreCase("ENDSEC") && inHeaderSection) {
                        inHeaderSection = false;
                        break; // Exit header parsing after ENDSEC
                    }
                } else if (inHeaderSection && line.equalsIgnoreCase("9")) {
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
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading DXF header: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to indicate failure
        }

        float scale = unitScaleFactor(units);
        System.out.println("Starting DXF entity parsing with units: " + units + " (scale factor: " + scale + ")");

        // Second pass: Read entities
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntity = null;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List < PointEntity > polyPoints = null;
            float tempVertexX = 0, tempVertexY = 0;

            boolean inEntitiesSection = false;
            boolean inPolylineEntity = false; // Renamed to avoid confusion with currentEntity="POLYLINE"
            boolean waitingForVertexCoords = false; // New flag for vertex parsing state

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // System.out.println("DBG: Processing line: '" + line + "'"); // Uncomment for detailed line-by-line debug

                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("0")) {
                    // This is a new entity or section start/end
                    String entityTypeOrSection = reader.readLine();
                    if (entityTypeOrSection == null) {
                        System.err.println("ERROR: Unexpected EOF after group code 0.");
                        break;
                    }
                    entityTypeOrSection = entityTypeOrSection.trim().toUpperCase();
                    // System.out.println("DBG: Found 0, next is: " + entityTypeOrSection);

                    // If we were parsing a non-POLYLINE entity, add it now
                    if (currentEntity != null && !inPolylineEntity) {
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
                            // POLYLINE handled by SEQEND
                        }
                        currentEntity = null; // Reset for the next entity
                    }


                    if (entityTypeOrSection.equals("SECTION")) {
                        reader.readLine(); // Group code 2
                        String sectionName = reader.readLine();
                        if (sectionName != null && sectionName.equalsIgnoreCase("ENTITIES")) {
                            inEntitiesSection = true;
                            // System.out.println("DBG: Entered ENTITIES section.");
                        } else {
                            inEntitiesSection = false; // Not the entities section, skip content
                        }
                        continue;
                    } else if (entityTypeOrSection.equals("ENDSEC")) {
                        // System.out.println("DBG: Exited current SECTION.");
                        inEntitiesSection = false;
                        continue;
                    } else if (entityTypeOrSection.equals("EOF")) {
                        // System.out.println("DBG: Reached EOF.");
                        break;
                    }

                    if (inEntitiesSection) {
                        currentEntity = entityTypeOrSection;
                        // System.out.println("DBG: Current entity type: " + currentEntity);

                        // Reset coordinates for new entity
                        x1 = y1 = x2 = y2 = cx = cy = radius = 0;

                        if (currentEntity.equals("POLYLINE")) {
                            inPolylineEntity = true;
                            polyPoints = new ArrayList<>();
                            // System.out.println("DBG: Started POLYLINE parsing.");
                        } else if (currentEntity.equals("VERTEX")) {
                            if (!inPolylineEntity) {
                                System.err.println("Warning: Encountered VERTEX outside of POLYLINE. Skipping.");
                                currentEntity = null; // Treat as invalid entity
                                continue;
                            }
                            waitingForVertexCoords = true;
                            tempVertexX = 0;
                            tempVertexY = 0;
                            // System.out.println("DBG: Started VERTEX parsing.");
                        } else if (currentEntity.equals("SEQEND")) {
                            if (inPolylineEntity && polyPoints != null && polyPoints.size() >= 3) {
                                addEntity("POLYLINE", 0, 0, 0, 0, 0, 0, 0, scalePoints(polyPoints, scale));
                                // System.out.println("DBG: Added POLYLINE with " + polyPoints.size() + " points.");
                            } else {
                                System.err.println("Warning: Invalid or empty POLYLINE before SEQEND. Skipping.");
                            }
                            inPolylineEntity = false;
                            polyPoints = null;
                            currentEntity = null; // Clear entity state
                            waitingForVertexCoords = false;
                            // System.out.println("DBG: Ended POLYLINE parsing (SEQEND).");
                        }
                    }

                } else if (inEntitiesSection && currentEntity != null) {
                    int groupCode = 0;
                    String valueString = null;
                    try {
                        groupCode = Integer.parseInt(line);
                        valueString = reader.readLine();
                        if (valueString == null) {
                            System.err.println("ERROR: Unexpected EOF after group code " + groupCode + " for entity " + currentEntity);
                            break;
                        }
                        valueString = valueString.trim();

                        switch (groupCode) {
                            case 10: // X coordinate for point, start of line, center of circle, or vertex
                                if (waitingForVertexCoords) tempVertexX = Float.parseFloat(valueString);
                                else if ("POINT".equals(currentEntity)) x1 = Float.parseFloat(valueString);
                                else if ("LINE".equals(currentEntity)) x1 = Float.parseFloat(valueString);
                                else if ("CIRCLE".equals(currentEntity)) cx = Float.parseFloat(valueString);
                                break;
                            case 20: // Y coordinate
                                if (waitingForVertexCoords) {
                                    tempVertexY = Float.parseFloat(valueString);
                                    if (polyPoints != null) {
                                        polyPoints.add(new PointEntity(tempVertexX, tempVertexY));
                                        // System.out.println("DBG: Added vertex (" + tempVertexX + ", " + tempVertexY + ") to polyline.");
                                    }
                                    waitingForVertexCoords = false; // Vertex complete
                                } else if ("POINT".equals(currentEntity)) y1 = Float.parseFloat(valueString);
                                else if ("LINE".equals(currentEntity)) y1 = Float.parseFloat(valueString);
                                else if ("CIRCLE".equals(currentEntity)) cy = Float.parseFloat(valueString);
                                break;
                            case 11: // Second X coordinate for line
                                if ("LINE".equals(currentEntity)) x2 = Float.parseFloat(valueString);
                                break;
                            case 21: // Second Y coordinate for line
                                if ("LINE".equals(currentEntity)) y2 = Float.parseFloat(valueString);
                                break;
                            case 40: // Radius for circle
                                if ("CIRCLE".equals(currentEntity)) radius = Float.parseFloat(valueString);
                                break;
                            case 66: // "Entities follow" for polyline
                            case 70: // Polyline flags
                                // Handled by state changes, no direct value assignment needed for geometry
                                break;
                            case 8: // Layer name (usually "0") - we don't store it, just consume the value
                                // valueString contains the layer name, consume it
                                break;
                            default:
                                // System.out.println("DBG: Unhandled group code: " + groupCode + " for entity: " + currentEntity);
                                break;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("ERROR: Invalid number format for group code: '" + groupCode + "', value: '" + valueString + "' for entity: " + currentEntity + ". " + e.getMessage());
                        e.printStackTrace();
                        // Depending on severity, you might want to break or continue to try parsing next entity
                        // For now, we continue to try parsing the next line, but the current entity might be incomplete
                        // If this happens frequently, you might need more robust error recovery.
                    }
                }
            } // End while loop

            // After loop, if there's a pending non-POLYLINE entity
            if (currentEntity != null && !inPolylineEntity) {
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
        } catch (IOException e) {
            System.err.println("Error parsing DXF entities: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to indicate failure
        }

        System.out.println("DXF loaded successfully with units: " + units + " (scale factor applied: " + scale + ")");
        System.out.println("Total entities loaded: " + sketchEntities.size()); // Final count
    }


    private float unitScaleFactor(String units) {
        return switch (units) {
            case "in" -> 25.4f;
            case "ft" -> 304.8f;
            case "mm" -> 1.0f;
            case "cm" -> 10.0f;
            case "m" -> 1000.0f;
            default -> 1.0f; // Default to 1.0 if unitless or unknown
        };
    }

    private List < PointEntity > scalePoints(List < PointEntity > points, float scale) {
        List < PointEntity > scaled = new ArrayList < > ();
        for (PointEntity p: points) {
            scaled.add(new PointEntity(p.x * scale, p.y * scale));
        }
        return scaled;
    }

    private void addEntity(String entityType, float x1, float y1, float x2, float y2,
                           float cx, float cy, float radius, List < PointEntity > polyPoints) {
        // Apply scaling *before* adding the entity to the sketchEntities list
        float scale = unitScaleFactor(this.units); // Get the current unit scale factor

        switch (entityType) {
            case "POINT":
                this.addPoint(x1 * scale, y1 * scale);
                System.out.println("Loaded: Point (scaled)");
                break;
            case "LINE":
                this.addLine(x1 * scale, y1 * scale, x2 * scale, y2 * scale);
                System.out.println("Loaded: Line (scaled)");
                break;
            case "CIRCLE":
                this.addCircle(cx * scale, cy * scale, radius * scale);
                System.out.println("Loaded: Circle (scaled)");
                break;
            case "POLYLINE":
                // polyPoints are already scaled in the loadDXF method if they come from there
                if (polyPoints != null && polyPoints.size() >= 3) {
                    this.addPolygon(polyPoints); // addPolygon handles its own validation
                    System.out.println("Loaded: Polygon with " + polyPoints.size() + " points (scaled)");
                } else {
                    System.err.println("Warning: Skipping invalid polygon with less than 3 points during addEntity call.");
                }
                break;
            default:
                System.out.println("Warning: Unknown entity type encountered during addEntity: " + entityType);
                break;
        }
    }

    public void draw(Graphics g) {
        if (sketchEntities.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
        float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;

        boolean firstEntity = true;

        for (Entity e: sketchEntities) {
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
                    maxY = Math.max(maxY, Math.max(l.y1, l.y2)); // Fixed bug: was maxY = Math.max(maxY, l.y2);
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
                for (PointEntity p: poly.points) {
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

        // If after checking all entities, firstEntity is still true, it means sketchEntities was empty,
        // or contained only invalid/unhandled entity types for bounding box calculation.
        // This block should ideally not be needed if the loop correctly calculates bounds for valid entities.
        if (firstEntity) {
            // This part of your original code suggests a fallback if no bounds were found.
            // It might be better to ensure min/max are correctly initialized or throw an error.
            // For now, retaining a simplified version if the sketch is truly empty.
            if (!sketchEntities.isEmpty()) {
                // If there are entities but bounds couldn't be calculated, it's an edge case.
                // You might default to a small arbitrary range.
                minX = -10; maxX = 10;
                minY = -10; maxY = 10;
            } else {
                return; // Nothing to draw
            }
        }


        int canvasWidth = 800;
        int canvasHeight = 800;

        if (g.getClipBounds() != null) {
            int clipWidth = g.getClipBounds().width;
            int clipHeight = g.getClipBounds().height;
            if (clipWidth > 0 && clipHeight > 0) {
                canvasWidth = clipWidth;
                canvasHeight = clipHeight;
            }
        }

        int margin = 20;

        float sketchWidth = maxX - minX;
        float sketchHeight = maxY - minY;

        if (sketchWidth == 0) sketchWidth = 1.0f;
        if (sketchHeight == 0) sketchHeight = 1.0f;

        float scaleX = (float)(canvasWidth - 2 * margin) / sketchWidth;
        float scaleY = (float)(canvasHeight - 2 * margin) / sketchHeight;
        float scale = Math.min(scaleX, scaleY);

        float offsetX = (canvasWidth - sketchWidth * scale) / 2 - minX * scale;
        float offsetY = (canvasHeight - sketchHeight * scale) / 2 - minY * scale;

        for (Entity e: sketchEntities) {
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

    private void drawPoint(Graphics g, PointEntity p, float offsetX, float offsetY, float scale) {
        int x = (int)(p.x * scale + offsetX);
        int y = (int)(p.y * scale + offsetY);
        int size = 4;
        g.fillOval(x - size / 2, y - size / 2, size, size);
    }

    private void drawLine(Graphics g, Line l, float offsetX, float offsetY, float scale) {
        int x1 = (int)(l.x1 * scale + offsetX);
        int y1 = (int)(l.y1 * scale + offsetY);
        int x2 = (int)(l.x2 * scale + offsetX);
        int y2 = (int)(l.y2 * scale + offsetY);
        g.drawLine(x1, y1, x2, y2);
    }

    private void drawCircle(Graphics g, Circle c, float offsetX, float offsetY, float scale) {
        int x = (int)(c.x * scale + offsetX);
        int y = (int)(c.y * scale + offsetY);
        int r = (int)(c.r * scale);
        g.drawOval(x - r, y - r, 2 * r, 2 * r);
    }

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