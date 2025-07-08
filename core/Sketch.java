package core;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.awt.Graphics;

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
                if (e instanceof PointEntity) {
                    PointEntity p = (PointEntity) e;
                    out.println("0");
                    out.println("POINT");
                    out.println("8");
                    out.println("0");
                    out.println("10");
                    out.println(p.x);
                    out.println("20");
                    out.println(p.y);
                } else if (e instanceof Line) {
                    Line l = (Line) e;
                    out.println("0");
                    out.println("LINE");
                    out.println("8");
                    out.println("0");
                    out.println("10");
                    out.println(l.x1);
                    out.println("20");
                    out.println(l.y1);
                    out.println("11");
                    out.println(l.x2);
                    out.println("21");
                    out.println(l.y2);
                } else if (e instanceof Circle) {
                    Circle c = (Circle) e;
                    out.println("0");
                    out.println("CIRCLE");
                    out.println("8");
                    out.println("0");
                    out.println("10");
                    out.println(c.x);
                    out.println("20");
                    out.println(c.y);
                    out.println("40");
                    out.println(c.r);
                } else if (e instanceof Polygon) {
                    Polygon poly = (Polygon) e;
                    out.println("0");
                    out.println("POLYLINE");
                    out.println("8");
                    out.println("0");
                    out.println("66");
                    out.println("1");
                    out.println("70");
                    out.println("1");

                    for (PointEntity p: poly.points) {
                        out.println("0");
                        out.println("VERTEX");
                        out.println("8");
                        out.println("0");
                        out.println("10");
                        out.println(p.x);
                        out.println("20");
                        out.println(p.y);
                    }

                    out.println("0");
                    out.println("SEQEND");
                    out.println("8");
                    out.println("0");
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
        clearSketch();
        this.units = "unitless";

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("0")) {
                    String nextLine = reader.readLine();
                    if (nextLine != null && nextLine.equalsIgnoreCase("SECTION")) {
                        nextLine = reader.readLine();
                        if (nextLine != null && nextLine.equalsIgnoreCase("2")) {
                            nextLine = reader.readLine();
                            if (nextLine != null && nextLine.equalsIgnoreCase("HEADER")) {
                                while ((line = reader.readLine()) != null) {
                                    line = line.trim();
                                    if (line.equalsIgnoreCase("0")) {
                                        nextLine = reader.readLine();
                                        if (nextLine != null && nextLine.equalsIgnoreCase("ENDSEC")) {
                                            break;
                                        }
                                    } else if (line.equalsIgnoreCase("9")) {
                                        nextLine = reader.readLine();
                                        if (nextLine != null && nextLine.equalsIgnoreCase("$INSUNITS")) {
                                            reader.readLine();
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
                                break;
                            }
                        }
                    }
                }
            }
        }

        float scale = unitScaleFactor(units);
        System.out.println("Loading DXF with units: " + units + " (scale factor: " + scale + ")");

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntity = null;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List < PointEntity > polyPoints = null;
            float tempVertexX = 0, tempVertexY = 0;

            boolean inEntitiesSection = false;
            boolean inPolyline = false;
            boolean inVertex = false;

            String valueString = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty()) continue;

                if (line.equalsIgnoreCase("0")) {
                    String entityType = reader.readLine();
                    if (entityType == null) break;
                    entityType = entityType.trim().toUpperCase();

                    if (entityType.equals("SECTION")) {
                      //  String sectionNameCode = reader.readLine();
                        String sectionName = reader.readLine();
                        if (sectionName != null && sectionName.equalsIgnoreCase("ENTITIES")) {
                            inEntitiesSection = true;
                        } else {
                            inEntitiesSection = false;
                        }
                        continue;
                    } else if (entityType.equals("ENDSEC")) {
                        inEntitiesSection = false;
                        continue;
                    } else if (entityType.equals("EOF")) {
                        break;
                    }

                    if (!inEntitiesSection) {
                        continue;
                    }

                    if (currentEntity != null && !inPolyline && !entityType.equals("VERTEX") && !entityType.equals("SEQEND")) {
                        addEntity(
                            currentEntity,
                            x1 * scale, y1 * scale, x2 * scale, y2 * scale,
                            cx * scale, cy * scale, radius * scale,
                            null
                        );
                    }

                    currentEntity = entityType;
                    x1 = y1 = x2 = y2 = cx = cy = radius = 0;

                    if (currentEntity.equals("POLYLINE")) {
                        inPolyline = true;
                        polyPoints = new ArrayList<>();
                    } else if (currentEntity.equals("VERTEX") && inPolyline) {
                        inVertex = true;
                        tempVertexX = 0;
                        tempVertexY = 0;
                    } else if (currentEntity.equals("SEQEND") && inPolyline) {
                        if (polyPoints != null && polyPoints.size() >= 3) {
                            addEntity("POLYLINE", 0, 0, 0, 0, 0, 0, 0, scalePoints(polyPoints, scale));
                        }
                        inPolyline = false;
                        polyPoints = null;
                        currentEntity = null;
                    }

                } else if (inEntitiesSection) {
                    int groupCode = 0;

                    try {
                        groupCode = Integer.parseInt(line);
                        valueString = reader.readLine();
                        if (valueString == null) break;
                        valueString = valueString.trim();

                        switch (groupCode) {
                            case 10:
                                if (inVertex && inPolyline) {
                                    tempVertexX = Float.parseFloat(valueString);
                                } else {
                                    if ("POINT".equals(currentEntity)) x1 = Float.parseFloat(valueString);
                                    else if ("LINE".equals(currentEntity)) x1 = Float.parseFloat(valueString);
                                    else if ("CIRCLE".equals(currentEntity)) cx = Float.parseFloat(valueString);
                                }
                                break;
                            case 20:
                                if (inVertex && inPolyline) {
                                    tempVertexY = Float.parseFloat(valueString);
                                    polyPoints.add(new PointEntity(tempVertexX, tempVertexY));
                                    inVertex = false;
                                } else {
                                    if ("POINT".equals(currentEntity)) y1 = Float.parseFloat(valueString);
                                    else if ("LINE".equals(currentEntity)) y1 = Float.parseFloat(valueString);
                                    else if ("CIRCLE".equals(currentEntity)) cy = Float.parseFloat(valueString);
                                }
                                break;
                            case 11:
                                if ("LINE".equals(currentEntity)) x2 = Float.parseFloat(valueString);
                                break;
                            case 21:
                                if ("LINE".equals(currentEntity)) y2 = Float.parseFloat(valueString);
                                break;
                            case 40:
                                if ("CIRCLE".equals(currentEntity)) radius = Float.parseFloat(valueString);
                                break;
                            case 70:
                                break;
                            default:
                                break;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number format encountered. Group code line: '" + line + "', Value line: '" + valueString + "'. Error: " + e.getMessage());
                    }
                }
            }

            if (currentEntity != null && !inPolyline) {
                addEntity(
                    currentEntity,
                    x1 * scale, y1 * scale, x2 * scale, y2 * scale,
                    cx * scale, cy * scale, radius * scale,
                    null
                );
            }
        }

        System.out.println("DXF loaded with units: " + units + " (scale factor applied: " + scale + ")");
    }

    private float unitScaleFactor(String units) {
        return switch (units) {
            case "in" -> 25.4f;
            case "ft" -> 304.8f;
            case "mm" -> 1.0f;
            case "cm" -> 10.0f;
            case "m" -> 1000.0f;
            default -> 1.0f;
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
                    System.err.println("Warning: Skipping invalid polygon with less than 3 points.");
                }
                break;
            default:
                System.out.println("Unknown entity type encountered: " + entityType);
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
                    maxY = Math.max(maxY, l.y2);
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

        if (firstEntity) {
            if (!sketchEntities.isEmpty()) {
                Entity first = sketchEntities.get(0);
                if (first instanceof PointEntity p) {
                    minX = p.x - 10; maxX = p.x + 10;
                    minY = p.y - 10; maxY = p.y + 10;
                } else if (first instanceof Line l) {
                    minX = Math.min(l.x1, l.x2) - 10; maxX = Math.max(l.x1, l.x2) + 10;
                    minY = Math.min(l.y1, l.y2) - 10; maxY = Math.max(l.y1, l.y2) + 10;
                } else if (first instanceof Circle c) {
                    minX = c.x - c.r - 10; maxX = c.x + c.r + 10;
                    minY = c.y - c.r - 10; maxY = c.y + c.r + 10;
                } else if (first instanceof Polygon poly && !poly.points.isEmpty()) {
                    PointEntity p = poly.points.get(0);
                    minX = p.x - 10; maxX = p.x + 10;
                    minY = p.y - 10; maxY = p.y + 10;
                }
            } else {
                return;
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
