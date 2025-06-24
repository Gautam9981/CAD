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

    public List<Entity> getEntities() {
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
        String groupCode = null;
        String value = null;
        String currentEntityType = null;

        float x1 = 0, y1 = 0, x2 = 0, y2 = 0, x = 0, y = 0, r = 0;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (groupCode == null) {
                groupCode = line;  // read group code
            } else {
                value = line;  // read value for group code

                if (groupCode.equals("0")) {
                    // If previous entity pending, add it before processing next
                    if (currentEntityType != null) {
                        switch (currentEntityType) {
                            case "POINT" -> addPoint(x, y);
                            case "LINE" -> addLine(x1, y1, x2, y2);
                            case "CIRCLE" -> addCircle(x, y, r);
                        }
                    }
                    currentEntityType = value;  // New entity type or section

                    // Reset values
                    x1 = y1 = x2 = y2 = x = y = r = 0;
                } else {
                    // Parse coordinates or radius according to currentEntityType and groupCode
                    if (currentEntityType != null) {
                        switch (currentEntityType) {
                            case "POINT" -> {
                                if (groupCode.equals("10")) x = Float.parseFloat(value);
                                else if (groupCode.equals("20")) y = Float.parseFloat(value);
                            }
                            case "LINE" -> {
                                if (groupCode.equals("10")) x1 = Float.parseFloat(value);
                                else if (groupCode.equals("20")) y1 = Float.parseFloat(value);
                                else if (groupCode.equals("11")) x2 = Float.parseFloat(value);
                                else if (groupCode.equals("21")) y2 = Float.parseFloat(value);
                            }
                            case "CIRCLE" -> {
                                if (groupCode.equals("10")) x = Float.parseFloat(value);
                                else if (groupCode.equals("20")) y = Float.parseFloat(value);
                                else if (groupCode.equals("40")) r = Float.parseFloat(value);
                            }
                        }
                    }
                }
                groupCode = null; // reset for next pair
            }
        }

        // Add the last entity if pending
        if (currentEntityType != null) {
            switch (currentEntityType) {
                case "POINT" -> addPoint(x, y);
                case "LINE" -> addLine(x1, y1, x2, y2);
                case "CIRCLE" -> addCircle(x, y, r);
            }
        }
    }
    System.out.println("Loaded sketch with " + sketchEntities.size() + " entities.");
}

    public boolean isClosedLoop() {
        // Extract all lines only
        List<Line> lines = new ArrayList<>();
        for (Entity e : sketchEntities) {
            if (e instanceof Line) {
                lines.add((Line) e);
            }
        }
        if (lines.isEmpty()) return false;

        // Check connectivity of consecutive lines
        for (int i = 0; i < lines.size() - 1; i++) {
            Line current = lines.get(i);
            Line next = lines.get(i + 1);
            if (!pointsAreClose(current.x2, current.y2, next.x1, next.y1)) {
                return false;
            }
        }
        // Check if last line end connects to first line start
        Line first = lines.get(0);
        Line last = lines.get(lines.size() - 1);
        return pointsAreClose(last.x2, last.y2, first.x1, first.y1);
    }

    private boolean pointsAreClose(float x1, float y1, float x2, float y2) {
        final float tolerance = 0.001f;
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (dx * dx + dy * dy) < (tolerance * tolerance);
    }
}
