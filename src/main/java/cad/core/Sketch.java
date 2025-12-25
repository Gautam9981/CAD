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

public class Sketch {

    public enum TypeSketch {
        POINT,
        LINE,
        CIRCLE,
        ARC,
        POLYGON,
        SPLINE
    }

    public static abstract class Entity {
        TypeSketch type;
    }

    public static class PointEntity extends Entity {
        private final Point point;

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

        public Point getStartPoint() {
            return start;
        }

        public Point getEndPoint() {
            return end;
        }

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

    public static class Circle extends Entity {
        private final Point center;
        private float r;

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

    private boolean isModified = false;

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean modified) {
        this.isModified = modified;
    }

    public static class Arc extends Entity {
        private final PointEntity center;
        private final PointEntity startPoint;
        private final PointEntity endPoint;
        private float r;
        private float startAngle;
        private float endAngle;

        public Arc(float cx, float cy, float r, float startAngle, float endAngle) {
            this.type = TypeSketch.ARC;
            this.center = new PointEntity(cx, cy);
            this.r = r;
            this.startAngle = startAngle;
            this.endAngle = endAngle;

            float sx = cx + r * (float) Math.cos(Math.toRadians(startAngle));
            float sy = cy + r * (float) Math.sin(Math.toRadians(startAngle));
            this.startPoint = new PointEntity(sx, sy);

            float ex = cx + r * (float) Math.cos(Math.toRadians(endAngle));
            float ey = cy + r * (float) Math.sin(Math.toRadians(endAngle));
            this.endPoint = new PointEntity(ex, ey);
        }

        public PointEntity getCenterPoint() {
            return center;
        }

        public PointEntity getStartPoint() {
            return startPoint;
        }

        public PointEntity getEndPoint() {
            return endPoint;
        }

        public float getX() {
            return center.getX();
        }

        public float getY() {
            return center.getY();
        }

        public float getRadius() {
            return r;
        }

        public float getStartAngle() {
            return startAngle;
        }

        public float getEndAngle() {
            return endAngle;
        }

        public void setRadius(float r) {
            this.r = r;
            updateEndpoints();
        }

        public void setAngles(float start, float end) {
            this.startAngle = start;
            this.endAngle = end;
            updateEndpoints();
        }

        private void updateEndpoints() {
            float sx = center.getX() + r * (float) Math.cos(Math.toRadians(startAngle));
            float sy = center.getY() + r * (float) Math.sin(Math.toRadians(startAngle));
            startPoint.setPoint(sx, sy);

            float ex = center.getX() + r * (float) Math.cos(Math.toRadians(endAngle));
            float ey = center.getY() + r * (float) Math.sin(Math.toRadians(endAngle));
            endPoint.setPoint(ex, ey);
        }

        public String toString() {
            return String.format("Arc at %s, r=%.3f, %.1f to %.1f deg", center, r, startAngle, endAngle);
        }
    }

    public static class Polygon extends Entity {
        List<PointEntity> points;

        public Polygon(List<PointEntity> points) {
            if (points == null || points.size() < 3 || points.size() > 200) {
                throw new IllegalArgumentException("Polygon must have between 3 and 200 points.");
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

    public static class Spline extends Entity {
        private final List<PointEntity> controlPoints;
        private final boolean closed;

        public Spline(List<PointEntity> controlPoints, boolean closed) {
            this.type = TypeSketch.SPLINE;
            this.controlPoints = new ArrayList<>(controlPoints);
            this.closed = closed;
        }

        public List<PointEntity> getControlPoints() {
            return controlPoints;
        }

        public boolean isClosed() {
            return closed;
        }

        @Override
        public String toString() {
            return "Spline with " + controlPoints.size() + " points";
        }
    }

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

    public static class Face3D {
        private List<Point3D> vertices;

        private List<float[]> vertexNormals;

        public Face3D(Point3D p1, Point3D p2, Point3D p3, Point3D p4) {
            this.vertices = new ArrayList<>();
            this.vertices.add(p1);
            this.vertices.add(p2);
            this.vertices.add(p3);
            this.vertices.add(p4);
            this.vertexNormals = null;
        }

        public Face3D(List<Point3D> vertices) {
            if (vertices == null || vertices.size() < 3) {
                throw new IllegalArgumentException("A Face3D must have at least 3 vertices.");
            }
            this.vertices = new ArrayList<>(vertices);
            this.vertexNormals = null;
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

    public void computePerVertexNormals() {

        java.util.Map<Point3D, List<Face3D>> vertexToFaces = new java.util.HashMap<>();

        java.util.Map<Face3D, float[]> faceNormals = new java.util.HashMap<>();

        for (Face3D face : extrudedFaces) {
            List<Point3D> verts = face.getVertices();
            if (verts.size() < 3)
                continue;

            float[] normal = calculateFaceNormal(verts.get(0), verts.get(1), verts.get(2));
            faceNormals.put(face, normal);

            for (Point3D v : verts) {
                vertexToFaces.computeIfAbsent(v, k -> new java.util.ArrayList<>()).add(face);
            }
        }

        double creaseThreshold = Math.cos(Math.toRadians(45));

        for (Face3D face : extrudedFaces) {
            List<Point3D> verts = face.getVertices();
            List<float[]> vertexNormals = new ArrayList<>();
            float[] currentFaceNormal = faceNormals.get(face);

            if (currentFaceNormal == null) {

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

                        float dot = currentFaceNormal[0] * nNormal[0] +
                                currentFaceNormal[1] * nNormal[1] +
                                currentFaceNormal[2] * nNormal[2];

                        if (dot > creaseThreshold) {
                            nx += nNormal[0];
                            ny += nNormal[1];
                            nz += nNormal[2];
                        }
                    }
                }

                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (len > 1e-6) {
                    nx /= len;
                    ny /= len;
                    nz /= len;
                } else {

                    nx = currentFaceNormal[0];
                    ny = currentFaceNormal[1];
                    nz = currentFaceNormal[2];
                }
                vertexNormals.add(new float[] { nx, ny, nz });
            }
            face.setVertexNormals(vertexNormals);
        }
    }

    private static final int MAX_SKETCH_ENTITIES = 1000;
    private final List<Entity> sketchEntities = new CopyOnWriteArrayList<>();
    public final List<Entity> tempEntities = new CopyOnWriteArrayList<>();

    public void addTempEntity(Entity e) {
        tempEntities.add(e);
    }

    public void clearTempEntities() {
        tempEntities.clear();
    }

    public final List<Polygon> polygons = new CopyOnWriteArrayList<>();
    public final List<Spline> splines = new CopyOnWriteArrayList<>();
    public List<Face3D> extrudedFaces = new ArrayList<>();

    private final List<Dimension> dimensions = new ArrayList<>();

    private final List<Constraint> constraints = new CopyOnWriteArrayList<>();

    private Material material = null;
    private double thickness = 5.0;
    private MassProperties cachedMassProperties = null;

    private boolean isDirty = false;

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        this.isDirty = dirty;
    }

    public void addConstraint(Constraint c) {
        constraints.add(c);
        solveConstraints();
        setDirty(true);
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
        setDirty(true);
    }

    public void addEntity(Entity e) {
        if (!sketchEntities.contains(e)) {
            sketchEntities.add(e);

            if (e instanceof Polygon) {
                polygons.add((Polygon) e);
            } else if (e instanceof Spline) {
                splines.add((Spline) e);
            }
            setModified(true);
        }
    }

    public int addPoint(float x, float y) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new PointEntity(x, y));
        setDirty(true);
        return 0;
    }

    public int addLine(float x1, float y1, float x2, float y2) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Line(x1, y1, x2, y2));
        setDirty(true);
        return 0;
    }

    public int addCircle(float x, float y, float r) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        sketchEntities.add(new Circle(x, y, r));
        setDirty(true);
        return 0;
    }

    public int addPolygon(List<PointEntity> points) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        try {
            Polygon newPolygon = new Polygon(points);
            sketchEntities.add(newPolygon);
            this.polygons.add(newPolygon);
            setDirty(true);
            return 0;
        } catch (IllegalArgumentException e) {
            System.out.println("Error adding polygon: " + e.getMessage());
            return 1;
        }
    }

    public int addSpline(List<PointEntity> controlPoints, boolean closed) {
        if (sketchEntities.size() >= MAX_SKETCH_ENTITIES) {
            System.out.println("Sketch buffer full");
            return 1;
        }
        if (controlPoints == null || controlPoints.size() < 2)
            return 1;
        Spline spline = new Spline(controlPoints, closed);
        addEntity(spline);
        setDirty(true);
        return 0;
    }

    public int addNSidedPolygon(float centerX, float centerY, float radius, int sides) {
        return addNSidedPolygon(centerX, centerY, radius, sides, false);
    }

    public int addNSidedPolygon(float centerX, float centerY, float radius, int sides, boolean circumscribed) {
        if (sides < 3 || sides > 200) {
            System.out.println("Polygon must have between 3 and 200 sides.");
            return 1;
        }
        if (radius <= 0) {
            System.out.println("Radius must be positive.");
            return 1;
        }

        double effectiveRadius = radius;
        if (circumscribed) {

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

        PointEntity p1 = new PointEntity(centerX + dxMain, centerY + dyMain);
        PointEntity p2 = new PointEntity(centerX + dxCross, centerY + dyCross);
        PointEntity p3 = new PointEntity(centerX - dxMain, centerY - dyMain);
        PointEntity p4 = new PointEntity(centerX - dxCross, centerY - dyCross);
        List<PointEntity> points = new ArrayList<>();
        points.add(p1);
        points.add(p2);
        points.add(p3);
        points.add(p4);
        int result = addPolygon(points);

        addLine(p1.getX(), p1.getY(), p3.getX(), p3.getY());

        addLine(p2.getX(), p2.getY(), p4.getX(), p4.getY());

        float tailLength = mainDiagonal * 0.7f;
        float tailAngle = (float) (angleRad + Math.PI);
        float tailEndX = p3.getX() + (float) (Math.cos(tailAngle) * tailLength);
        float tailEndY = p3.getY() + (float) (Math.sin(tailAngle) * tailLength);
        addLine(p3.getX(), p3.getY(), tailEndX, tailEndY);

        for (int i = 1; i <= 3; i++) {
            float t = i / 4.0f;
            float bx = p3.getX() + (float) (Math.cos(tailAngle) * tailLength * t);
            float by = p3.getY() + (float) (Math.sin(tailAngle) * tailLength * t);
            float bowSize = mainDiagonal * 0.07f;

            addLine(bx - bowSize, by - bowSize, bx + bowSize, by + bowSize);
            addLine(bx - bowSize, by + bowSize, bx + bowSize, by - bowSize);
        }

        float bridleLength = mainDiagonal * 0.5f;
        float bridleAngle = (float) (angleRad - Math.PI / 2.0);
        float bridleX = centerX + (float) (Math.cos(bridleAngle) * bridleLength);
        float bridleY = centerY + (float) (Math.sin(bridleAngle) * bridleLength);
        addLine(p1.getX(), p1.getY(), bridleX, bridleY);
        addLine(p2.getX(), p2.getY(), bridleX, bridleY);
        return result;
    }

    public void clearSketch() {
        sketchEntities.clear();
        polygons.clear();
        dimensions.clear();
        setDirty(true);

    }

    public void clearAll() {
        clearSketch();
        extrudedFaces.clear();
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

    public List<Entity> getEntities() {
        return new ArrayList<>(sketchEntities);
    }

    public boolean removeEntity(Entity entity) {
        boolean removed = sketchEntities.remove(entity);
        if (removed && entity instanceof Polygon) {
            polygons.remove((Polygon) entity);
        }
        if (removed) {
            setDirty(true);
            setModified(true);
        }
        return removed;
    }

    public boolean isClosedLoop() {
        for (Entity entity : sketchEntities) {
            if (entity instanceof Polygon || entity instanceof Circle) {
                return true;
            }
        }
        return false;
    }

    private UnitSystem unitSystem = UnitSystem.MMGS;

    public void setUnitSystem(UnitSystem unitSystem) {
        this.unitSystem = unitSystem;
    }

    public UnitSystem getUnitSystem() {
        return this.unitSystem;
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
                    out.println("0");
                    out.println("10");
                    out.println(p.getX());
                    out.println("20");
                    out.println(p.getY());
                } else if (e instanceof Line l) {
                    out.println("0");
                    out.println("LINE");
                    out.println("8");
                    out.println("0");
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
                    out.println("0");
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
                    out.println("0");
                    out.println("66");
                    out.println("1");
                    out.println("70");
                    out.println("1");

                    for (PointEntity p : poly.points) {
                        out.println("0");
                        out.println("VERTEX");
                        out.println("8");
                        out.println("0");
                        out.println("10");
                        out.println(p.getX());
                        out.println("20");
                        out.println(p.getY());
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
        if (sides < 3 || sides > 200) {
            System.out.println("Polygon must have between 3 and 200 sides.");
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

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String currentEntity = null;
            float x1 = 0, y1 = 0, x2 = 0, y2 = 0, cx = 0, cy = 0, radius = 0;
            List<PointEntity> polyPoints = null;
            float tempVertexX = 0, tempVertexY = 0;

            boolean inHeaderSection = false;
            boolean inEntitiesSection = false;
            boolean inPolylineEntity = false;
            boolean waitingForVertexCoords = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty())
                    continue;

                if (line.equalsIgnoreCase("0")) {

                    String entityTypeOrSection = reader.readLine();
                    if (entityTypeOrSection == null) {
                        System.err.println("ERROR: Unexpected EOF after group code 0.");
                        break;
                    }
                    entityTypeOrSection = entityTypeOrSection.trim().toUpperCase();

                    if (entityTypeOrSection.equals("SECTION")) {
                        reader.readLine();
                        String sectionName = reader.readLine();
                        if (sectionName != null) {
                            if (sectionName.equalsIgnoreCase("HEADER")) {
                                inHeaderSection = true;
                                inEntitiesSection = false;
                            } else if (sectionName.equalsIgnoreCase("ENTITIES")) {
                                inHeaderSection = false;
                                inEntitiesSection = true;

                            } else {
                                inHeaderSection = false;
                                inEntitiesSection = false;
                            }
                        }
                        continue;
                    } else if (entityTypeOrSection.equals("ENDSEC")) {
                        if (inPolylineEntity && polyPoints != null) {
                            addPolygon(polyPoints);
                            polyPoints = null;
                            inPolylineEntity = false;
                        }
                        inHeaderSection = false;
                        inEntitiesSection = false;
                        continue;
                    } else if (entityTypeOrSection.equals("EOF")) {

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
                        break;
                    }

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
                        currentEntity = null;
                    }

                    if (inEntitiesSection) {
                        currentEntity = entityTypeOrSection;

                        x1 = y1 = x2 = y2 = cx = cy = radius = 0;

                        if (currentEntity.equals("POLYLINE")) {
                            inPolylineEntity = true;
                            polyPoints = new ArrayList<>();

                        } else if (currentEntity.equals("VERTEX")) {

                            if (!inPolylineEntity) {
                                System.err.println("Warning: Found VERTEX entity outside of POLYLINE. Skipping.");

                                currentEntity = "SKIP_VERTEX";
                            } else {
                                waitingForVertexCoords = true;
                            }
                        } else if (currentEntity.equals("SEQEND")) {

                            if (inPolylineEntity && polyPoints != null) {
                                addPolygon(polyPoints);

                            } else {
                                System.err.println("Warning: SEQEND encountered without active POLYLINE.");
                            }
                            polyPoints = null;
                            inPolylineEntity = false;

                        }
                    }
                    continue;
                }

                if (inHeaderSection && line.equalsIgnoreCase("9")) {
                    String varName = reader.readLine();
                    if (varName != null && varName.equalsIgnoreCase("$INSUNITS")) {
                        reader.readLine();
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

                if (currentEntity != null && inEntitiesSection) {
                    float scale = 1.0f;
                    String valueLine = null;
                    try {

                        int groupCode = Integer.parseInt(line);
                        valueLine = reader.readLine();
                        if (valueLine == null) {
                            System.err.println("ERROR: Unexpected EOF after group code " + groupCode);
                            break;
                        }
                        valueLine = valueLine.trim();

                        if (currentEntity.equals("SKIP_VERTEX") || currentEntity.equals("SEQEND")) {
                            continue;
                        }

                        switch (groupCode) {
                            case 10:

                                if (currentEntity.equals("VERTEX") && waitingForVertexCoords) {
                                    tempVertexX = Float.parseFloat(valueLine) * scale;
                                } else {
                                    if (currentEntity.equals("POINT") || currentEntity.equals("LINE"))
                                        x1 = Float.parseFloat(valueLine) * scale;
                                    else if (currentEntity.equals("CIRCLE"))
                                        cx = Float.parseFloat(valueLine) * scale;
                                }
                                break;
                            case 20:

                                if (currentEntity.equals("VERTEX") && waitingForVertexCoords) {
                                    tempVertexY = Float.parseFloat(valueLine) * scale;

                                    if (polyPoints != null) {
                                        polyPoints.add(new PointEntity(tempVertexX, tempVertexY));
                                    }
                                    waitingForVertexCoords = false;
                                } else {
                                    if (currentEntity.equals("POINT") || currentEntity.equals("LINE"))
                                        y1 = Float.parseFloat(valueLine) * scale;
                                    else if (currentEntity.equals("CIRCLE"))
                                        cy = Float.parseFloat(valueLine) * scale;
                                }
                                break;
                            case 11:
                                if (currentEntity.equals("LINE"))
                                    x2 = Float.parseFloat(valueLine) * scale;
                                break;
                            case 21:
                                if (currentEntity.equals("LINE"))
                                    y2 = Float.parseFloat(valueLine) * scale;
                                break;
                            case 40:

                                if (currentEntity.equals("CIRCLE"))
                                    radius = Float.parseFloat(valueLine) * scale;
                                break;
                            case 8:
                            case 6:
                            case 62:
                            case 39:
                            case 70:
                            case 66:

                                break;
                            default:

                                break;
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Warning: Invalid number format for DXF value near group code: " + line
                                + ", value: " + valueLine + ". Error: " + e.getMessage());
                    } catch (Exception e) {
                        System.err.println("Error parsing DXF line '" + line + "': " + e.getMessage());
                        e.printStackTrace();
                        break;
                    }
                }
            }

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

            if (inPolylineEntity && polyPoints != null && !polyPoints.isEmpty()) {
                System.err.println("Warning: POLYLINE did not end with SEQEND. Adding partially parsed polyline.");
                addPolygon(polyPoints);
            }

        } catch (IOException e) {
            System.err.println("Error reading DXF entities: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        System.out.println("Finished loading DXF. Entities loaded: " + sketchEntities.size());
    }

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
            case "POLYLINE":

                if (polyPoints != null) {
                    addPolygon(polyPoints);
                }
                break;

            default:
                System.out.println("Unsupported DXF entity type for adding: " + type);
        }
    }

    private float unitScaleFactor(String unitStr) {

        return switch (unitStr.toLowerCase()) {
            case "in" -> 25.4f;
            case "ft" -> 304.8f;
            case "mm" -> 1.0f;
            case "cm" -> 10.0f;
            case "m" -> 1000.0f;
            default -> 1.0f;
        };
    }

    public void draw3D(GL2 gl) {
        if (extrudedFaces.isEmpty()) {
            return;
        }

        float[] materialAmbient = { 0.2f, 0.4f, 0.6f, 1.0f };
        float[] materialDiffuse = { 0.4f, 0.6f, 0.8f, 1.0f };
        float[] materialSpecular = { 0.8f, 0.8f, 0.8f, 1.0f };
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, materialAmbient, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, materialDiffuse, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, materialSpecular, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, 60.0f);

        for (Face3D face : extrudedFaces) {
            renderFace3D(gl, face);
        }
    }

    private void renderFace3D(GL2 gl, Face3D face) {
        List<Point3D> vertices = face.getVertices();
        int numVertices = vertices.size();

        if (numVertices < 3) {
            return;
        }

        if (numVertices == 3) {

            gl.glBegin(GL2.GL_TRIANGLES);

            Point3D p1 = vertices.get(0);
            Point3D p2 = vertices.get(1);
            Point3D p3 = vertices.get(2);

            float[] normal = calculateFaceNormal(p1, p2, p3);
            gl.glNormal3f(normal[0], normal[1], normal[2]);

            gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
            gl.glVertex3f(p2.getX(), p2.getY(), p2.getZ());
            gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());

            gl.glEnd();
        } else if (numVertices == 4) {

            gl.glBegin(GL2.GL_TRIANGLES);

            Point3D p1 = vertices.get(0);
            Point3D p2 = vertices.get(1);
            Point3D p3 = vertices.get(2);
            Point3D p4 = vertices.get(3);

            float[] normal1 = calculateFaceNormal(p1, p2, p3);
            gl.glNormal3f(normal1[0], normal1[1], normal1[2]);
            gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
            gl.glVertex3f(p2.getX(), p2.getY(), p2.getZ());
            gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());

            float[] normal2 = calculateFaceNormal(p1, p3, p4);
            gl.glNormal3f(normal2[0], normal2[1], normal2[2]);
            gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
            gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());
            gl.glVertex3f(p4.getX(), p4.getY(), p4.getZ());

            gl.glEnd();
        } else {

            gl.glBegin(GL2.GL_TRIANGLES);

            Point3D center = vertices.get(0);

            for (int i = 1; i < numVertices - 1; i++) {
                Point3D p1 = center;
                Point3D p2 = vertices.get(i);
                Point3D p3 = vertices.get(i + 1);

                float[] normal = calculateFaceNormal(p1, p2, p3);
                gl.glNormal3f(normal[0], normal[1], normal[2]);

                gl.glVertex3f(p1.getX(), p1.getY(), p1.getZ());
                gl.glVertex3f(p2.getX(), p2.getY(), p2.getZ());
                gl.glVertex3f(p3.getX(), p3.getY(), p3.getZ());
            }

            gl.glEnd();
        }
    }

    private float[] calculateFaceNormal(Point3D p1, Point3D p2, Point3D p3) {

        float ex1 = p2.getX() - p1.getX();
        float ey1 = p2.getY() - p1.getY();
        float ez1 = p2.getZ() - p1.getZ();

        float ex2 = p3.getX() - p1.getX();
        float ey2 = p3.getY() - p1.getY();
        float ez2 = p3.getZ() - p1.getZ();

        float nx = ey1 * ez2 - ez1 * ey2;
        float ny = ez1 * ex2 - ex1 * ez2;
        float nz = ex1 * ey2 - ey1 * ex2;

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0.001f) {
            nx /= length;
            ny /= length;
            nz /= length;
        } else {

            nx = 0.0f;
            ny = 0.0f;
            nz = 1.0f;
        }

        return new float[] { nx, ny, nz };
    }

    public void draw(GL2 gl) {

        for (Entity e : sketchEntities) {
            switch (e.type) {
                case POINT:
                    PointEntity p = (PointEntity) e;
                    gl.glPointSize(5.0f);
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
                        int segments = 50;
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
                case ARC:
                    Arc arc = (Arc) e;
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    try {
                        int segments = 50;
                        float start = (float) Math.toRadians(arc.getStartAngle());
                        float end = (float) Math.toRadians(arc.getEndAngle());

                        if (end < start) {
                            end += 2.0 * Math.PI;
                        }

                        float angleDiff = end - start;

                        for (int i = 0; i <= segments; i++) {
                            float angle = start + (angleDiff * i / segments);
                            float x = arc.getX() + arc.getRadius() * (float) Math.cos(angle);
                            float y = arc.getY() + arc.getRadius() * (float) Math.sin(angle);
                            gl.glVertex2f(x, y);
                        }
                    } finally {
                        gl.glEnd();
                    }
                    break;
                case POLYGON:
                    Polygon poly = (Polygon) e;
                    gl.glBegin(GL2.GL_LINE_LOOP);
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
                case SPLINE:
                    Spline spline = (Spline) e;
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    try {
                        if (spline.getControlPoints() != null) {
                            for (PointEntity sp : spline.getControlPoints()) {
                                gl.glVertex2f(sp.getX(), sp.getY());
                            }
                            if (spline.isClosed() && !spline.getControlPoints().isEmpty()) {
                                PointEntity first = spline.getControlPoints().get(0);
                                gl.glVertex2f(first.getX(), first.getY());
                            }
                        }
                    } finally {
                        gl.glEnd();
                    }
                    break;
            }
        }

        gl.glColor3f(0.5f, 0.5f, 0.5f);
        for (

        Entity e : tempEntities) {
            switch (e.type) {
                case POINT:
                    PointEntity p = (PointEntity) e;
                    gl.glPointSize(5.0f);
                    gl.glBegin(GL2.GL_POINTS);
                    try {
                        gl.glVertex2f(p.getX(), p.getY());
                    } finally {
                        gl.glEnd();
                    }
                    break;
                case LINE:
                    Line l = (Line) e;
                    gl.glEnable(GL2.GL_LINE_STIPPLE);
                    gl.glLineStipple(1, (short) 0x00FF);
                    gl.glBegin(GL2.GL_LINES);
                    try {
                        gl.glVertex2f(l.getX1(), l.getY1());
                        gl.glVertex2f(l.getX2(), l.getY2());
                    } finally {
                        gl.glEnd();
                        gl.glDisable(GL2.GL_LINE_STIPPLE);
                    }
                    break;
                case CIRCLE:
                    Circle c = (Circle) e;
                    gl.glEnable(GL2.GL_LINE_STIPPLE);
                    gl.glLineStipple(1, (short) 0x00FF);
                    gl.glBegin(GL2.GL_LINE_LOOP);
                    try {
                        int segments = 50;
                        for (int i = 0; i < segments; i++) {
                            double angle = 2.0 * Math.PI * i / segments;
                            float x = c.getX() + c.getRadius() * (float) Math.cos(angle);
                            float y = c.getY() + c.getRadius() * (float) Math.sin(angle);
                            gl.glVertex2f(x, y);
                        }
                    } finally {
                        gl.glEnd();
                        gl.glDisable(GL2.GL_LINE_STIPPLE);
                    }
                    break;
                case ARC:
                    Arc arc = (Arc) e;
                    gl.glEnable(GL2.GL_LINE_STIPPLE);
                    gl.glLineStipple(1, (short) 0x00FF);
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    try {
                        int segments = 50;
                        float start = (float) Math.toRadians(arc.getStartAngle());
                        float end = (float) Math.toRadians(arc.getEndAngle());
                        if (end < start)
                            end += (float) (2.0 * Math.PI);
                        float angleDiff = end - start;

                        for (int i = 0; i <= segments; i++) {
                            float angle = start + (angleDiff * i / segments);
                            float x = arc.getX() + arc.getRadius() * (float) Math.cos(angle);
                            float y = arc.getY() + arc.getRadius() * (float) Math.sin(angle);
                            gl.glVertex2f(x, y);
                        }
                    } finally {
                        gl.glEnd();
                        gl.glDisable(GL2.GL_LINE_STIPPLE);
                    }
                    break;
                case SPLINE:
                    Spline spline = (Spline) e;
                    gl.glEnable(GL2.GL_LINE_STIPPLE);
                    gl.glLineStipple(1, (short) 0x00FF);
                    gl.glBegin(GL2.GL_LINE_STRIP);
                    try {
                        if (spline.getControlPoints() != null) {
                            for (PointEntity sp : spline.getControlPoints()) {
                                gl.glVertex2f(sp.getX(), sp.getY());
                            }
                            if (spline.isClosed() && !spline.getControlPoints().isEmpty()) {
                                PointEntity first = spline.getControlPoints().get(0);
                                gl.glVertex2f(first.getX(), first.getY());
                            }
                        }
                    } finally {
                        gl.glEnd();
                        gl.glDisable(GL2.GL_LINE_STIPPLE);
                    }
                    break;
                case POLYGON:
                    Polygon poly = (Polygon) e;
                    gl.glEnable(GL2.GL_LINE_STIPPLE);
                    gl.glLineStipple(1, (short) 0x00FF);
                    gl.glBegin(GL2.GL_LINE_LOOP);
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
                        gl.glDisable(GL2.GL_LINE_STIPPLE);
                    }
                    break;

            }
        }

        for (Dimension dim : dimensions) {
            if (dim != null) {
                dim.draw(gl);
            }
        }
    }

    public Entity findClosestEntity(float x, float y, float threshold) {
        Entity closest = null;
        float minDistance = threshold;

        for (Entity entity : sketchEntities) {
            
            if (entity instanceof Arc) {
                Arc arc = (Arc) entity;
                float dStart = calculateDistanceToEntity(arc.getStartPoint(), x, y);
                if (dStart < minDistance) {
                    minDistance = dStart;
                    closest = arc.getStartPoint();
                }
                float dEnd = calculateDistanceToEntity(arc.getEndPoint(), x, y);
                if (dEnd < minDistance) {
                    minDistance = dEnd;
                    closest = arc.getEndPoint();
                }
            }

            float distance = calculateDistanceToEntity(entity, x, y);
            if (distance < minDistance) {
                minDistance = distance;
                closest = entity;
            }
        }

        return closest;
    }

    private float calculateDistanceToEntity(Entity entity, float x, float y) {
        if (entity instanceof Line) {
            Line line = (Line) entity;
            return distanceToLineSegment(x, y, line.getX1(), line.getY1(), line.getX2(), line.getY2());
        } else if (entity instanceof Circle) {
            Circle circle = (Circle) entity;
            float dx = x - circle.getX();
            float dy = y - circle.getY();
            float distToCenter = (float) Math.sqrt(dx * dx + dy * dy);
            return Math.abs(distToCenter - circle.getRadius());
        } else if (entity instanceof PointEntity) {
            PointEntity point = (PointEntity) entity;
            float dx = x - point.getX();
            float dy = y - point.getY();
            return (float) Math.sqrt(dx * dx + dy * dy);
        } else if (entity instanceof Polygon) {

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
        } else if (entity instanceof Spline) {
            Spline s = (Spline) entity;
            List<PointEntity> pts = s.getControlPoints();
            if (pts.size() < 2)
                return Float.MAX_VALUE;
            float minDist = Float.MAX_VALUE;
            for (int i = 0; i < pts.size() - 1; i++) {
                PointEntity p1 = pts.get(i);
                PointEntity p2 = pts.get(i + 1);
                float dist = distanceToLineSegment(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                minDist = Math.min(minDist, dist);
            }
            if (s.isClosed() && pts.size() > 2) {
                PointEntity p1 = pts.get(pts.size() - 1);
                PointEntity p2 = pts.get(0);
                float dist = distanceToLineSegment(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                minDist = Math.min(minDist, dist);
            }
            return minDist;
        } else if (entity instanceof Arc) {
            Arc arc = (Arc) entity;
            float dx = x - arc.getX();
            float dy = y - arc.getY();
            float distToCenter = (float) Math.sqrt(dx * dx + dy * dy);
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

            
            if (angle < 0)
                angle += 360;

            float start = arc.getStartAngle();
            float end = arc.getEndAngle();

            
            
            
            
            
            
            
            

            
            while (start < 0)
                start += 360;
            while (start >= 360)
                start -= 360;
            while (end < 0)
                end += 360;
            while (end >= 360)
                end -= 360;

            boolean inSector = false;
            if (start < end) {
                inSector = (angle >= start && angle <= end);
            } else {
                
                inSector = (angle >= start || angle <= end);
            }

            if (inSector) {
                return Math.abs(distToCenter - arc.getRadius());
            } else {
                
                float startX = arc.getX() + arc.getRadius() * (float) Math.cos(Math.toRadians(start));
                float startY = arc.getY() + arc.getRadius() * (float) Math.sin(Math.toRadians(start));
                float endX = arc.getX() + arc.getRadius() * (float) Math.cos(Math.toRadians(end));
                float endY = arc.getY() + arc.getRadius() * (float) Math.sin(Math.toRadians(end));

                float d1 = (float) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
                float d2 = (float) Math.sqrt(Math.pow(x - endX, 2) + Math.pow(y - endY, 2));
                return Math.min(d1, d2);
            }
        }
        return Float.MAX_VALUE;
    }

    private float distanceToLineSegment(float px, float py, float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float lenSquared = dx * dx + dy * dy;

        if (lenSquared == 0) {

            return (float) Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        float t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lenSquared));

        float closestX = x1 + t * dx;
        float closestY = y1 + t * dy;

        return (float) Math.sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY));
    }

    public Dimension createDimensionFor(Entity entity, float clickX, float clickY) {
        String unit = unitSystem.getAbbreviation();

        if (entity instanceof Line) {
            Line line = (Line) entity;
            return new LinearDimension(line.getX1(), line.getY1(), line.getX2(), line.getY2(), unit);
        } else if (entity instanceof Circle) {
            Circle circle = (Circle) entity;
            float dx = clickX - circle.getX();
            float dy = clickY - circle.getY();
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            RadialDimension dim = new RadialDimension(circle.getX(), circle.getY(), circle.getRadius(), false, unit);
            dim.setAngle(angle);
            return dim;
        } else if (entity instanceof Arc) {
            Arc arc = (Arc) entity;
            float dx = clickX - arc.getX();
            float dy = clickY - arc.getY();
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
            RadialDimension dim = new RadialDimension(arc.getX(), arc.getY(), arc.getRadius(), false, unit);
            dim.setAngle(angle);
            return dim;
        } else if (entity instanceof Polygon) {
            Polygon poly = (Polygon) entity;
            if (poly.points.size() >= 2) {
                PointEntity p1 = poly.points.get(0);
                PointEntity p2 = poly.points.get(1);
                return new LinearDimension(p1.getX(), p1.getY(), p2.getX(), p2.getY(), unit);
            }
        }

        return null;
    }

    public void addDimension(Dimension dim) {
        if (dim != null) {
            dimensions.add(dim);
        }
    }

    public void removeDimension(Dimension dim) {
        dimensions.remove(dim);
    }

    public List<Dimension> getDimensions() {
        return new ArrayList<>(dimensions);
    }

    public void clearDimensions() {
        dimensions.clear();
    }

    public void setMaterial(Material material) {
        this.material = material;
        cachedMassProperties = null;
    }

    public Material getMaterial() {
        return material;
    }

    public void setThickness(double thickness) {
        this.thickness = thickness;
        cachedMassProperties = null;
    }

    public double getThickness() {
        return thickness;
    }

    public MassProperties calculateMassProperties() {
        if (material == null) {
            return null;
        }

        if (cachedMassProperties != null) {
            return cachedMassProperties;
        }

        cachedMassProperties = MassProperties.calculate(this, material, thickness, unitSystem);
        return cachedMassProperties;
    }

    public void extrude(double height) {

        for (Polygon polygon : this.polygons) {
            extrudePolygon(polygon, height);
        }

        for (Entity entity : sketchEntities) {
            if (entity instanceof Circle) {
                Circle circle = (Circle) entity;
                extrudeCircle(circle, height);
            }
        }

        List<List<Point2D>> closedLoops = findClosedLoopsFromLines();
        for (List<Point2D> loop : closedLoops) {
            extrudeClosedLoop(loop, height);
        }

        float plateWidth = 0.05f;
        for (Entity entity : sketchEntities) {
            if (entity instanceof Line) {
                Line line = (Line) entity;

                float dx = line.getX2() - line.getX1();
                float dy = line.getY2() - line.getY1();
                float length = (float) Math.sqrt(dx * dx + dy * dy);
                if (length < 1e-6f)
                    continue;

                float nx = -dy / length;
                float ny = dx / length;
                float w = plateWidth / 2.0f;

                float x1a = line.getX1() + nx * w;
                float y1a = line.getY1() + ny * w;
                float x1b = line.getX1() - nx * w;
                float y1b = line.getY1() - ny * w;
                float x2a = line.getX2() + nx * w;
                float y2a = line.getY2() + ny * w;
                float x2b = line.getX2() - nx * w;
                float y2b = line.getY2() - ny * w;

                Point3D p1 = new Point3D(x1a, y1a, 0);
                Point3D p2 = new Point3D(x1b, y1b, 0);
                Point3D p3 = new Point3D(x2b, y2b, 0);
                Point3D p4 = new Point3D(x2a, y2a, 0);

                Point3D q1 = new Point3D(x1a, y1a, (float) height);
                Point3D q2 = new Point3D(x1b, y1b, (float) height);
                Point3D q3 = new Point3D(x2b, y2b, (float) height);
                Point3D q4 = new Point3D(x2a, y2a, (float) height);

                extrudedFaces.add(new Face3D(p1, p2, q2, q1));
                extrudedFaces.add(new Face3D(p2, p3, q3, q2));
                extrudedFaces.add(new Face3D(p3, p4, q4, q3));
                extrudedFaces.add(new Face3D(p4, p1, q1, q4));

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

    private void extrudePolygon(Polygon polygon, double height) {
        List<Point2D> points = polygon.getPoints();
        int n = points.size();

        List<Point3D> bottom = points.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .toList();

        List<Point3D> top = points.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), (float) height))
                .toList();

        for (int i = 0; i < n; i++) {
            Point3D p1 = bottom.get(i);
            Point3D p2 = bottom.get((i + 1) % n);
            Point3D p3 = top.get((i + 1) % n);
            Point3D p4 = top.get(i);

            extrudedFaces.add(new Face3D(p1, p2, p3, p4));
        }

        extrudedFaces.add(new Face3D(top));

        List<Point3D> reversedBottom = new ArrayList<>(bottom);
        java.util.Collections.reverse(reversedBottom);
        extrudedFaces.add(new Face3D(reversedBottom));
    }

    private void extrudeCircle(Circle circle, double height) {
        int segments = 32;
        List<Point2D> circlePoints = new ArrayList<>();

        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float x = circle.getX() + circle.getRadius() * (float) Math.cos(angle);
            float y = circle.getY() + circle.getRadius() * (float) Math.sin(angle);
            circlePoints.add(new Point2D(x, y));
        }

        List<Point3D> bottom = circlePoints.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .toList();

        List<Point3D> top = circlePoints.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), (float) height))
                .toList();

        for (int i = 0; i < segments; i++) {
            Point3D p1 = bottom.get(i);
            Point3D p2 = bottom.get((i + 1) % segments);
            Point3D p3 = top.get((i + 1) % segments);
            Point3D p4 = top.get(i);

            extrudedFaces.add(new Face3D(p1, p2, p3, p4));
        }

        extrudedFaces.add(new Face3D(top));

        List<Point3D> reversedBottom = new ArrayList<>(bottom);
        java.util.Collections.reverse(reversedBottom);
        extrudedFaces.add(new Face3D(reversedBottom));
    }

    private List<List<Point2D>> findClosedLoopsFromLines() {
        List<List<Point2D>> closedLoops = new ArrayList<>();
        List<Line> lines = new ArrayList<>();

        for (Entity entity : sketchEntities) {
            if (entity instanceof Line) {
                lines.add((Line) entity);
            }
        }

        if (lines.isEmpty()) {
            return closedLoops;
        }

        Set<Line> usedLines = new HashSet<>();

        for (Line startLine : lines) {
            if (usedLines.contains(startLine)) {
                continue;
            }

            List<Point2D> currentLoop = new ArrayList<>();
            Set<Line> currentLoopLines = new HashSet<>();

            currentLoop.add(new Point2D(startLine.getX1(), startLine.getY1()));
            currentLoop.add(new Point2D(startLine.getX2(), startLine.getY2()));
            currentLoopLines.add(startLine);

            Point2D currentEndPoint = new Point2D(startLine.getX2(), startLine.getY2());
            Point2D startPoint = new Point2D(startLine.getX1(), startLine.getY1());

            boolean foundConnection = true;
            while (foundConnection) {
                foundConnection = false;

                for (Line nextLine : lines) {
                    if (currentLoopLines.contains(nextLine)) {
                        continue;
                    }

                    Point2D nextStart = new Point2D(nextLine.getX1(), nextLine.getY1());
                    Point2D nextEnd = new Point2D(nextLine.getX2(), nextLine.getY2());

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

            if (currentLoop.size() >= 3 && isPointsEqual(currentEndPoint, startPoint)) {

                currentLoop.remove(currentLoop.size() - 1);
                closedLoops.add(currentLoop);
                usedLines.addAll(currentLoopLines);
            }
        }

        return closedLoops;
    }

    private boolean isPointsEqual(Point2D p1, Point2D p2) {
        float tolerance = 1e-6f;
        return Math.abs(p1.getX() - p2.getX()) < tolerance &&
                Math.abs(p1.getY() - p2.getY()) < tolerance;
    }

    private void extrudeClosedLoop(List<Point2D> loop, double height) {
        if (loop.size() < 3) {
            return;
        }

        int n = loop.size();

        List<Point3D> bottom = loop.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .toList();

        List<Point3D> top = loop.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), (float) height))
                .toList();

        for (int i = 0; i < n; i++) {
            Point3D p1 = bottom.get(i);
            Point3D p2 = bottom.get((i + 1) % n);
            Point3D p3 = top.get((i + 1) % n);
            Point3D p4 = top.get(i);

            extrudedFaces.add(new Face3D(p1, p2, p3, p4));
        }

        extrudedFaces.add(new Face3D(top));

        List<Point3D> reversedBottom = new ArrayList<>(bottom);
        java.util.Collections.reverse(reversedBottom);
        extrudedFaces.add(new Face3D(reversedBottom));
    }

    public List<float[]> getExtrudedTriangles() {
        List<float[]> triangles = new ArrayList<>();

        for (Face3D face : extrudedFaces) {

            List<float[]> faceTriangles = triangulateFace(face);
            triangles.addAll(faceTriangles);
        }

        return triangles;
    }

    private List<float[]> triangulateFace(Face3D face) {
        List<float[]> triangles = new ArrayList<>();
        List<Point3D> vertices = face.vertices;

        if (vertices.size() < 3) {
            return triangles;
        }

        if (vertices.size() == 3) {

            triangles.add(createTriangle(vertices.get(0), vertices.get(1), vertices.get(2)));
        } else {

            Point3D center = vertices.get(0);
            for (int i = 1; i < vertices.size() - 1; i++) {
                triangles.add(createTriangle(center, vertices.get(i), vertices.get(i + 1)));
            }
        }

        return triangles;
    }

    private float[] createTriangle(Point3D p1, Point3D p2, Point3D p3) {

        float[] v1 = { p2.x - p1.x, p2.y - p1.y, p2.z - p1.z };
        float[] v2 = { p3.x - p1.x, p3.y - p1.y, p3.z - p1.z };

        float nx = v1[1] * v2[2] - v1[2] * v2[1];
        float ny = v1[2] * v2[0] - v1[0] * v2[2];
        float nz = v1[0] * v2[1] - v1[1] * v2[0];

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[] {
                nx, ny, nz,
                p1.x, p1.y, p1.z,
                p2.x, p2.y, p2.z,
                p3.x, p3.y, p3.z
        };
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    public int generateNaca4(String digit4, float chord, int pointsPerSide) {
        if (digit4.length() != 4)
            return -1;

        try {
            int mInt = Integer.parseInt(digit4.substring(0, 1));
            int pInt = Integer.parseInt(digit4.substring(1, 2));
            int tInt = Integer.parseInt(digit4.substring(2, 4));

            float m = mInt / 100.0f;
            float p = pInt / 10.0f;
            float t = tInt / 100.0f;

            List<PointEntity> upper = new ArrayList<>();
            List<PointEntity> lower = new ArrayList<>();

            for (int i = 0; i <= pointsPerSide; i++) {
                float beta = (float) (i * Math.PI / pointsPerSide);
                float x = (float) (chord * 0.5 * (1 - Math.cos(beta)));

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

            List<PointEntity> polyPoints = new ArrayList<>(upper);
            java.util.Collections.reverse(lower);
            polyPoints.addAll(lower);

            return addPolygon(polyPoints);

        } catch (NumberFormatException e) {
            return -1;
        }
    }

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
            } else if (e instanceof Arc) {
                Arc arc = (Arc) e;
                float dx = x - arc.getX();
                float dy = y - arc.getY();
                float distToCenter = (float) Math.sqrt(dx * dx + dy * dy);
                float distFromFile = Math.abs(distToCenter - arc.getRadius());

                double angleRad = Math.atan2(dy, dx);
                float angleDeg = (float) Math.toDegrees(angleRad);
                if (angleDeg < 0)
                    angleDeg += 360;

                float start = arc.getStartAngle();
                float end = arc.getEndAngle();

                while (start < 0)
                    start += 360;
                while (start >= 360)
                    start -= 360;
                while (end < 0)
                    end += 360;
                while (end >= 360)
                    end -= 360;

                boolean inAngle = false;
                if (start < end) {
                    inAngle = angleDeg >= start && angleDeg <= end;
                } else {
                    inAngle = angleDeg >= start || angleDeg <= end;
                }

                if (inAngle) {
                    dst = distFromFile;
                }
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

    public PointEntity getClosestVertex(float x, float y, float tolerance) {
        PointEntity closest = null;
        float minDst = tolerance;

        for (Entity e : sketchEntities) {
            if (e instanceof PointEntity) {
                PointEntity p = (PointEntity) e;
                float dist = (float) Math.sqrt(Math.pow(p.getX() - x, 2) + Math.pow(p.getY() - y, 2));
                if (dist < minDst) {
                    minDst = dist;
                    closest = p;
                }
            } else if (e instanceof Line) {
                Line l = (Line) e;

                float dist1 = (float) Math.sqrt(Math.pow(l.getX1() - x, 2) + Math.pow(l.getY1() - y, 2));
                if (dist1 < minDst) {
                    minDst = dist1;
                    closest = new PointEntity(l.getStartPoint());
                }

                float dist2 = (float) Math.sqrt(Math.pow(l.getX2() - x, 2) + Math.pow(l.getY2() - y, 2));
                if (dist2 < minDst) {
                    minDst = dist2;
                    closest = new PointEntity(l.getEndPoint());
                }
            } else if (e instanceof Polygon) {
                Polygon poly = (Polygon) e;
                for (PointEntity p : poly.points) {
                    float dist = (float) Math.sqrt(Math.pow(p.getX() - x, 2) + Math.pow(p.getY() - y, 2));
                    if (dist < minDst) {
                        minDst = dist;
                        closest = p;
                    }
                }
            }
        }
        return closest;
    }

    public Line getClosestLineSegment(float x, float y, float tolerance) {
        Line closest = null;
        float minDst = tolerance;

        for (Entity e : sketchEntities) {
            if (e instanceof Line) {
                Line l = (Line) e;
                float dist = distancePointToSegment(x, y, l.getX1(), l.getY1(), l.getX2(), l.getY2());
                if (dist < minDst) {
                    minDst = dist;
                    closest = l;
                }
            } else if (e instanceof Polygon) {
                Polygon poly = (Polygon) e;
                List<PointEntity> pts = poly.points;
                for (int i = 0; i < pts.size(); i++) {
                    PointEntity p1 = pts.get(i);
                    PointEntity p2 = pts.get((i + 1) % pts.size());
                    float dist = distancePointToSegment(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    if (dist < minDst) {
                        minDst = dist;

                        closest = new Line(p1.getPoint(), p2.getPoint());
                    }
                }
            }
        }
        return closest;
    }

    public static class PolygonEdgeContext {
        public Polygon polygon;
        public int index1;
        public int index2;
        public Line segment;

        public PolygonEdgeContext(Polygon p, int i1, int i2, Line s) {
            this.polygon = p;
            this.index1 = i1;
            this.index2 = i2;
            this.segment = s;
        }
    }

    public PolygonEdgeContext getClosestPolygonEdge(float x, float y, float tolerance) {
        PolygonEdgeContext closestInfo = null;
        float minDst = tolerance;

        for (Entity e : sketchEntities) {
            if (e instanceof Polygon) {
                Polygon poly = (Polygon) e;
                List<PointEntity> pts = poly.points;
                for (int i = 0; i < pts.size(); i++) {
                    PointEntity p1 = pts.get(i);
                    PointEntity p2 = pts.get((i + 1) % pts.size());
                    float dist = distancePointToSegment(x, y, p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    if (dist < minDst) {
                        minDst = dist;
                        closestInfo = new PolygonEdgeContext(poly, i, (i + 1) % pts.size(),
                                new Line(p1.getPoint(), p2.getPoint()));
                    }
                }
            }
        }
        return closestInfo;
    }
}
