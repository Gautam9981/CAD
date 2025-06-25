package core;

import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;

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
        @Override
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
        @Override
        public String toString() {
            return String.format("Circle at (%.3f, %.3f) with radius %.3f", x, y, r);
        }
    }

    public static class Polygon extends Entity {
        List < PointEntity > points;
        public Polygon(List < PointEntity > points) {
            if (points.size() < 3 || points.size() > 25) {
                throw new IllegalArgumentException("Polygon must have between 3 and 25 points.");
            }
            this.type = TypeSketch.POLYGON;
            this.points = new ArrayList < > (points);
        }
        @Override
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
        sketchEntities.add(new Polygon(points));
        return 0;
    }

    public int addNSidedPolygon(float centerX, float centerY, float radius, int sides) {
        if (sides < 3 || sides > 25) {
            System.out.println("Polygon must have between 3 and 25 sides.");
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

    public void listSketch() {
        if (sketchEntities.isEmpty()) {
            System.out.println("Sketch is empty.");
            return;
        }
        for (Entity e: sketchEntities) {
            System.out.println(e);
        }
    }

    public void exportSketchToDXF(String filename) {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("HEADER");
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
                    // Start POLYLINE entity
                    out.println("0");
                    out.println("POLYLINE");
                    out.println("8");
                    out.println("0");
                    out.println("66"); // Indicates vertices follow
                    out.println("1");
                    out.println("70"); // Polyline flag - 1 means closed polygon
                    out.println("1");
                    out.println("90"); // Number of vertices
                    out.println(poly.points.size());

                    // Write each point as a VERTEX entity
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

                    // End of vertices sequence
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
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntity = null;


            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List < PointEntity > polyPoints = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // DXF uses pairs of lines: group code, then value
                // So read group code and then value
                if (line.isEmpty()) continue;
                String groupCode = line;
                String value = reader.readLine();
                if (value == null) break;
                value = value.trim();

                switch (groupCode) {
                    case "0": // Start of a new entity or section
                        if (currentEntity != null) {
                            // Previous entity finished, add it
                            addEntity(currentEntity, x1, y1, x2, y2, cx, cy, radius, polyPoints);
                            // Reset polyPoints
                            polyPoints = null;
                        }
                        currentEntity = value.toUpperCase();
                        break;


                    case "10":
                        float x = Float.parseFloat(value);
                        if ("POINT".equals(currentEntity)) {
                            x1 = x;
                        } else if ("LINE".equals(currentEntity)) {
                            x1 = x;
                        } else if ("CIRCLE".equals(currentEntity)) {
                            cx = x;
                        } else if ("LWPOLYLINE".equals(currentEntity)) {
                            if (polyPoints == null) polyPoints = new ArrayList < > ();
                            polyPoints.add(new PointEntity(x, 0));
                        }
                        break;

                    case "20":
                        float y = Float.parseFloat(value);
                        if ("POINT".equals(currentEntity)) {
                            y1 = y;
                        } else if ("LINE".equals(currentEntity)) {
                            y1 = y;
                        } else if ("CIRCLE".equals(currentEntity)) {
                            cy = y;
                        } else if ("LWPOLYLINE".equals(currentEntity)) {
                            if (polyPoints != null && !polyPoints.isEmpty()) {
                                PointEntity last = polyPoints.get(polyPoints.size() - 1);
                                polyPoints.set(polyPoints.size() - 1, new PointEntity(last.x, y));
                            }
                        }
                        break;

                    case "11":
                        if ("LINE".equals(currentEntity)) {
                            x2 = Float.parseFloat(value);
                        }
                        break;

                    case "21":
                        if ("LINE".equals(currentEntity)) {
                            y2 = Float.parseFloat(value);
                        }
                        break;

                    case "40":
                        if ("CIRCLE".equals(currentEntity)) {
                            radius = Float.parseFloat(value);
                        }
                        break;

                    case "90":
                        break;


                    default:
                        break;
                }
            }

            // After file read, add last entity if pending
            if (currentEntity != null) {
                addEntity(currentEntity, x1, y1, x2, y2, cx, cy, radius, polyPoints);
            }
        }
    }

    private void addEntity(String entityType, float x1, float y1, float x2, float y2,
        float cx, float cy, float radius, List < PointEntity > polyPoints) {
        switch (entityType) {
            case "POINT":
                this.sketchPoint(new String[] {
                    String.valueOf(x1), String.valueOf(y1)
                });
                break;
            case "LINE":
                this.sketchLine(new String[] {
                    String.valueOf(x1), String.valueOf(y1), String.valueOf(x2), String.valueOf(y2)
                });
                break;
            case "CIRCLE":
                this.sketchCircle(new String[] {
                    String.valueOf(cx), String.valueOf(cy), String.valueOf(radius)
                });
                break;
            case "LWPOLYLINE":
                if (polyPoints != null && polyPoints.size() >= 3) {
                    this.addPolygon(polyPoints);
                }
                break;
            default:
                break;
        }
    }

}
