import java.util.ArrayList;
import java.util.List;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.BufferedReader;

public class Sketch {

    public enum TypeSketch {
        POINT, LINE, CIRCLE
    }

    public static abstract class Entity {
        TypeSketch type;
    }

    public static class Point extends Entity {
        float x, y;
        public Point(float x, float y) {
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
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
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
            this.x = x; this.y = y; this.r = r;
        }
        @Override
        public String toString() {
            return String.format("Circle at (%.3f, %.3f) with radius %.3f", x, y, r);
        }
    }

    private static final int MAX_SKETCH_ENTITIES = 1000;
    private final List<Entity> sketchEntities = new ArrayList<>();

    public int addPoint(float x, float y) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Point(x, y));
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

    public void clearSketch() {
        sketchEntities.clear();
        System.out.println("Sketch cleared.");
    }

    public List<Entity> getSketchEntities() {
        return sketchEntities;
    }

    public int getSketchCount() {
        return sketchEntities.size();
    }

    public void listSketch() {
        if (sketchEntities.isEmpty()) {
            System.out.println("No sketch entities.");
            return;
        }
        for (Entity e : sketchEntities) {
            System.out.println(e);
        }
    }

    public int sketchPoint(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: sketchPoint <x> <y>");
            return 1;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            return addPoint(x, y);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format");
            return 1;
        }
    }

    public int sketchLine(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: sketchLine <x1> <y1> <x2> <y2>");
            return 1;
        }
        try {
            float x1 = Float.parseFloat(args[0]);
            float y1 = Float.parseFloat(args[1]);
            float x2 = Float.parseFloat(args[2]);
            float y2 = Float.parseFloat(args[3]);
            return addLine(x1, y1, x2, y2);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format");
            return 1;
        }
    }

    public int sketchCircle(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: sketchCircle <x> <y> <radius>");
            return 1;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            float r = Float.parseFloat(args[2]);
            return addCircle(x, y, r);
        } catch (NumberFormatException e) {
            System.out.println("Invalid number format");
            return 1;
        }
    }

    public void exportSketchToDXF(String filename) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("HEADER");
            out.println("9");
            out.println("$ACADVER");
            out.println("1");
            out.println("AC1009"); 
            out.println("0");
            out.println("ENDSEC");

            out.println("0");
            out.println("SECTION");
            out.println("2");
            out.println("ENTITIES");

            for (Entity e : sketchEntities) {
                switch (e.type) {
                    case POINT -> {
                        Point p = (Point) e;
                        out.println("0");
                        out.println("POINT");
                        out.println("8");
                        out.println("0");
                        out.println("10");
                        out.println(p.x);
                        out.println("20");
                        out.println(p.y);
                        out.println("30");
                        out.println("0.0");
                    }
                    case LINE -> {
                        Line l = (Line) e;
                        out.println("0");
                        out.println("LINE");
                        out.println("8");
                        out.println("0");
                        out.println("10");
                        out.println(l.x1);
                        out.println("20");
                        out.println(l.y1);
                        out.println("30");
                        out.println("0.0");
                        out.println("11");
                        out.println(l.x2);
                        out.println("21");
                        out.println(l.y2);
                        out.println("31");
                        out.println("0.0");
                    }
                    case CIRCLE -> {
                        Circle c = (Circle) e;
                        out.println("0");
                        out.println("CIRCLE");
                        out.println("8");
                        out.println("0");
                        out.println("10");
                        out.println(c.x);
                        out.println("20");
                        out.println(c.y);
                        out.println("30");
                        out.println("0.0");
                        out.println("40");
                        out.println(c.r);
                    }
                }
            }

            out.println("0");
            out.println("ENDSEC");
            out.println("0");
            out.println("EOF");
        }
        System.out.println("Sketch exported to DXF file: " + filename);
    }

    public void loadDxf(String filename) throws IOException {
        sketchEntities.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("POINT")) {
                    float x = 0, y = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        switch (line) {
                            case "10" -> x = Float.parseFloat(reader.readLine().trim());
                            case "20" -> y = Float.parseFloat(reader.readLine().trim());
                            case "0" -> { break; }
                        }
                        if (line.equals("0")) break;
                    }
                    addPoint(x, y);
                } else if (line.equalsIgnoreCase("LINE")) {
                    float x1 = 0, y1 = 0, x2 = 0, y2 = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        switch (line) {
                            case "10" -> x1 = Float.parseFloat(reader.readLine().trim());
                            case "20" -> y1 = Float.parseFloat(reader.readLine().trim());
                            case "11" -> x2 = Float.parseFloat(reader.readLine().trim());
                            case "21" -> y2 = Float.parseFloat(reader.readLine().trim());
                            case "0" -> { break; }
                        }
                        if (line.equals("0")) break;
                    }
                    addLine(x1, y1, x2, y2);
                } else if (line.equalsIgnoreCase("CIRCLE")) {
                    float x = 0, y = 0, r = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        switch (line) {
                            case "10" -> x = Float.parseFloat(reader.readLine().trim());
                            case "20" -> y = Float.parseFloat(reader.readLine().trim());
                            case "40" -> r = Float.parseFloat(reader.readLine().trim());
                            case "0" -> { break; }
                        }
                        if (line.equals("0")) break;
                    }
                    addCircle(x, y, r);
                }
            }
        }
    }
}
