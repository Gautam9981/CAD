package cad.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

import cad.gui.GuiFX;

import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Cube;
import eu.mihosoft.jcsg.Sphere;
import eu.mihosoft.jcsg.Polygon;
import eu.mihosoft.jcsg.Extrude;
import eu.mihosoft.jcsg.Vertex;
import eu.mihosoft.vvecmath.Vector3d;
import java.util.stream.Collectors;

public class Geometry {

    public enum Shape {
        NONE,
        CUBE,
        SPHERE,
        STL_LOADED,
        EXTRUDED,
        CSG_RESULT
    }

    public enum BooleanOp {
        NONE, UNION, DIFFERENCE, INTERSECTION
    }

    private static Shape currShape = Shape.NONE;
    private static Shape primitiveShapeType = Shape.NONE;
    private static float param = 0.0f;
    public static int cubeDivisions = 1;
    public static int sphereLatDiv = 30;
    public static int sphereLonDiv = 30;

    private static CSG currentCSG = null;

    private static List<float[]> extrudedTriangles = new ArrayList<>();

    private static List<float[]> loadedStlTriangles = new ArrayList<>();

    public static Shape getCurrentShape() {
        return currShape;
    }

    public static Shape getPrimitiveShapeType() {
        return primitiveShapeType;
    }

    public static float getParam() {
        return param;
    }

    public static int getCubeDivisions() {
        return cubeDivisions;
    }

    public static List<float[]> getLoadedStlTriangles() {
        return loadedStlTriangles;
    }

    public static List<float[]> getExtrudedTriangles() {
        return extrudedTriangles;
    }

    public static float getModelMaxDimension() {
        List<float[]> trianglesToCheck = null;

        switch (currShape) {
            case CUBE:
                return param;
            case SPHERE:
                return param * 2;
            case STL_LOADED:
                if (!loadedStlTriangles.isEmpty()) {
                    trianglesToCheck = loadedStlTriangles;
                }
                break;
            case EXTRUDED:
            case CSG_RESULT:
                if (!extrudedTriangles.isEmpty()) {
                    trianglesToCheck = extrudedTriangles;
                }
                break;
            default:
                return 2.0f;
        }

        if (trianglesToCheck == null || trianglesToCheck.isEmpty()) {
            return 2.0f;
        }

        List<float[]> trianglesCopy;
        synchronized (trianglesToCheck) {
            trianglesCopy = new ArrayList<>(trianglesToCheck);
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (float[] triData : trianglesCopy) {

            for (int i = 0; i < 3; i++) {
                float x = triData[3 + i * 3];
                float y = triData[4 + i * 3];
                float z = triData[5 + i * 3];

                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }

        float sizeX = maxX - minX;
        float sizeY = maxY - minY;
        float sizeZ = maxZ - minZ;

        return Math.max(Math.max(sizeX, sizeY), sizeZ);
    }

    public static List<float[]> loadStl(String filename) throws IOException {
        loadedStlTriangles.clear();
        currShape = Shape.NONE;

        System.out.println("Loading STL file: " + filename);

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            float[] currentNormal = new float[3];
            float[] currentVertices = new float[9];
            int facetCount = 0;
            int errorCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();

                if (line.startsWith("facet normal")) {
                    facetCount++;

                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5) {
                        try {
                            currentNormal[0] = Float.parseFloat(parts[2]);
                            currentNormal[1] = Float.parseFloat(parts[3]);
                            currentNormal[2] = Float.parseFloat(parts[4]);
                        } catch (NumberFormatException e) {
                            System.err.println(
                                    "Warning: Invalid normal values in facet " + facetCount + ", using default normal");
                            currentNormal[0] = 0.0f;
                            currentNormal[1] = 0.0f;
                            currentNormal[2] = 1.0f;
                            errorCount++;
                        }
                    } else {
                        System.err.println("Warning: Malformed facet normal line in facet " + facetCount
                                + ", using default normal");
                        currentNormal[0] = 0.0f;
                        currentNormal[1] = 0.0f;
                        currentNormal[2] = 1.0f;
                        errorCount++;
                    }

                    String outerLoop = reader.readLine();
                    if (outerLoop == null || !outerLoop.trim().toLowerCase().contains("outer loop")) {
                        System.err.println("Warning: Expected 'outer loop' after facet normal in facet " + facetCount);
                        errorCount++;
                    }

                    boolean validFacet = true;
                    for (int i = 0; i < 3; i++) {
                        String vertexLine = reader.readLine();
                        if (vertexLine == null) {
                            System.err.println(
                                    "Error: Unexpected end of file while reading vertices in facet " + facetCount);
                            validFacet = false;
                            break;
                        }

                        vertexLine = vertexLine.trim().toLowerCase();
                        String[] vertexParts = vertexLine.split("\\s+");
                        if (vertexParts.length >= 4 && vertexParts[0].equals("vertex")) {
                            try {
                                currentVertices[i * 3] = Float.parseFloat(vertexParts[1]);
                                currentVertices[i * 3 + 1] = Float.parseFloat(vertexParts[2]);
                                currentVertices[i * 3 + 2] = Float.parseFloat(vertexParts[3]);
                            } catch (NumberFormatException e) {
                                System.err.println("Warning: Invalid vertex coordinates in facet " + facetCount
                                        + ", vertex " + (i + 1));
                                currentVertices[i * 3] = 0.0f;
                                currentVertices[i * 3 + 1] = 0.0f;
                                currentVertices[i * 3 + 2] = 0.0f;
                                errorCount++;
                            }
                        } else {
                            System.err.println("Warning: Malformed vertex line in facet " + facetCount + ", vertex "
                                    + (i + 1) + ": " + vertexLine);
                            currentVertices[i * 3] = 0.0f;
                            currentVertices[i * 3 + 1] = 0.0f;
                            currentVertices[i * 3 + 2] = 0.0f;
                            errorCount++;
                        }
                    }

                    if (validFacet) {

                        float[] fullTriangleData = new float[12];
                        System.arraycopy(currentNormal, 0, fullTriangleData, 0, 3);
                        System.arraycopy(currentVertices, 0, fullTriangleData, 3, 9);
                        loadedStlTriangles.add(fullTriangleData);
                    }

                    String endLoop = reader.readLine();
                    String endFacet = reader.readLine();
                    if (endLoop == null || endFacet == null) {
                        System.err.println("Warning: Missing endloop/endfacet for facet " + facetCount);
                        errorCount++;
                    }
                }
            }

            System.out.println("Finished reading STL. Facets processed: " + facetCount + ", Triangles loaded: "
                    + loadedStlTriangles.size());
            if (errorCount > 0) {
                System.out.println("Warning: " + errorCount + " errors encountered during parsing");
            }

            if (!loadedStlTriangles.isEmpty()) {
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

                for (float[] triData : loadedStlTriangles) {

                    for (int i = 0; i < 3; i++) {
                        float x = triData[3 + i * 3];
                        float y = triData[4 + i * 3];
                        float z = triData[5 + i * 3];

                        minX = Math.min(minX, x);
                        maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y);
                        maxY = Math.max(maxY, y);
                        minZ = Math.min(minZ, z);
                        maxZ = Math.max(maxZ, z);
                    }
                }

                float sizeX = maxX - minX;
                float sizeY = maxY - minY;
                float sizeZ = maxZ - minZ;
                float centerX = (minX + maxX) / 2;
                float centerY = (minY + maxY) / 2;
                float centerZ = (minZ + maxZ) / 2;

                System.out.println("STL Model Bounds:");
                System.out.println("  X: " + minX + " to " + maxX + " (size: " + sizeX + ")");
                System.out.println("  Y: " + minY + " to " + maxY + " (size: " + sizeY + ")");
                System.out.println("  Z: " + minZ + " to " + maxZ + " (size: " + sizeZ + ")");
                System.out.println("  Center: (" + centerX + ", " + centerY + ", " + centerZ + ")");
                System.out.println("  Max dimension: " + Math.max(Math.max(sizeX, sizeY), sizeZ));

                centerModel(centerX, centerY, centerZ);
                System.out.println("Model centered at origin for proper rotation");
            }

            currShape = Shape.STL_LOADED;
            return loadedStlTriangles;
        } catch (NumberFormatException e) {
            throw new IOException("Error parsing numeric data in STL file: " + e.getMessage(), e);
        }
    }

    private static void centerModel(float centerX, float centerY, float centerZ) {

        for (float[] triData : loadedStlTriangles) {

            for (int i = 0; i < 3; i++) {
                triData[3 + i * 3] -= centerX;
                triData[4 + i * 3] -= centerY;
                triData[5 + i * 3] -= centerZ;
            }
        }
    }

    public static void createCube(float size, int divisions, BooleanOp op) {
        if (size < 0.1f || size > 100.0f) {
            throw new IllegalArgumentException("Cube size must be between 0.1 and 100.0");
        }

        cubeDivisions = divisions;
        param = size;

        CSG newShape;
        if (divisions > 1) {

            generateCubeTriangles(size, divisions);
            List<Polygon> polygons = new ArrayList<>();

            for (float[] tri : loadedStlTriangles) {

                Vector3d p1 = Vector3d.xyz(tri[3], tri[4], tri[5]);
                Vector3d p2 = Vector3d.xyz(tri[6], tri[7], tri[8]);
                Vector3d p3 = Vector3d.xyz(tri[9], tri[10], tri[11]);

                polygons.add(Polygon.fromPoints(p1, p2, p3));
            }
            newShape = CSG.fromPolygons(polygons);
        } else {

            newShape = new Cube(size, size, size).toCSG();
        }

        applyBooleanOperation(newShape, op);

        updateMeshFromCSG();

        currShape = Shape.CSG_RESULT;
        primitiveShapeType = Shape.CUBE;
        System.out.println("Cube created with size " + size + ", divisions " + divisions + " Op: " + op);
    }

    public static void createCube(float size, int divisions) {
        createCube(size, divisions, BooleanOp.NONE);
    }

    public static void createSphere(float radius, int divisions) {
        createSphere(radius, divisions, divisions, BooleanOp.NONE);
    }

    public static void createSphere(float radius, int latDiv, int lonDiv) {
        createSphere(radius, latDiv, lonDiv, BooleanOp.NONE);
    }

    public static void createSphere(float radius, int latDiv, int lonDiv, BooleanOp op) {
        if (radius < 0.1f || radius > 80.0f) {
            throw new IllegalArgumentException("Sphere radius must be between 0.1 and 80.0");
        }

        sphereLatDiv = latDiv;
        sphereLonDiv = lonDiv;
        param = radius;

        CSG newShape = new Sphere(radius, latDiv, lonDiv).toCSG();

        applyBooleanOperation(newShape, op);

        updateMeshFromCSG();

        currShape = Shape.CSG_RESULT;
        primitiveShapeType = Shape.SPHERE;
        System.out.printf("Sphere created (JCSG) with radius %.2f Op: %s%n", radius, op);
    }

    private static void generateCubeTriangles(float size, int divisions) {
        loadedStlTriangles.clear();

        float halfSize = size / 2.0f;
        float step = size / divisions;

        for (int face = 0; face < 6; face++) {
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float u1 = -halfSize + i * step;
                    float u2 = -halfSize + (i + 1) * step;
                    float v1 = -halfSize + j * step;
                    float v2 = -halfSize + (j + 1) * step;

                    float[] p1 = new float[3], p2 = new float[3], p3 = new float[3], p4 = new float[3];
                    float[] normal = new float[3];

                    switch (face) {
                        case 0:
                            p1[0] = u1;
                            p1[1] = v1;
                            p1[2] = halfSize;
                            p2[0] = u2;
                            p2[1] = v1;
                            p2[2] = halfSize;
                            p3[0] = u2;
                            p3[1] = v2;
                            p3[2] = halfSize;
                            p4[0] = u1;
                            p4[1] = v2;
                            p4[2] = halfSize;
                            normal[0] = 0;
                            normal[1] = 0;
                            normal[2] = 1;
                            break;
                        case 1:
                            p1[0] = u2;
                            p1[1] = v1;
                            p1[2] = -halfSize;
                            p2[0] = u1;
                            p2[1] = v1;
                            p2[2] = -halfSize;
                            p3[0] = u1;
                            p3[1] = v2;
                            p3[2] = -halfSize;
                            p4[0] = u2;
                            p4[1] = v2;
                            p4[2] = -halfSize;
                            normal[0] = 0;
                            normal[1] = 0;
                            normal[2] = -1;
                            break;
                        case 2:
                            p1[0] = halfSize;
                            p1[1] = v1;
                            p1[2] = u1;
                            p2[0] = halfSize;
                            p2[1] = v1;
                            p2[2] = u2;
                            p3[0] = halfSize;
                            p3[1] = v2;
                            p3[2] = u2;
                            p4[0] = halfSize;
                            p4[1] = v2;
                            p4[2] = u1;
                            normal[0] = 1;
                            normal[1] = 0;
                            normal[2] = 0;
                            break;
                        case 3:
                            p1[0] = -halfSize;
                            p1[1] = v1;
                            p1[2] = u2;
                            p2[0] = -halfSize;
                            p2[1] = v1;
                            p2[2] = u1;
                            p3[0] = -halfSize;
                            p3[1] = v2;
                            p3[2] = u1;
                            p4[0] = -halfSize;
                            p4[1] = v2;
                            p4[2] = u2;
                            normal[0] = -1;
                            normal[1] = 0;
                            normal[2] = 0;
                            break;
                        case 4:
                            p1[0] = u1;
                            p1[1] = halfSize;
                            p1[2] = v1;
                            p2[0] = u1;
                            p2[1] = halfSize;
                            p2[2] = v2;
                            p3[0] = u2;
                            p3[1] = halfSize;
                            p3[2] = v2;
                            p4[0] = u2;
                            p4[1] = halfSize;
                            p4[2] = v1;
                            normal[0] = 0;
                            normal[1] = 1;
                            normal[2] = 0;
                            break;
                        case 5:
                            p1[0] = u1;
                            p1[1] = -halfSize;
                            p1[2] = v2;
                            p2[0] = u1;
                            p2[1] = -halfSize;
                            p2[2] = v1;
                            p3[0] = u2;
                            p3[1] = -halfSize;
                            p3[2] = v1;
                            p4[0] = u2;
                            p4[1] = -halfSize;
                            p4[2] = v2;
                            normal[0] = 0;
                            normal[1] = -1;
                            normal[2] = 0;
                            break;
                    }

                    float[] tri1 = new float[12];
                    System.arraycopy(normal, 0, tri1, 0, 3);
                    System.arraycopy(p1, 0, tri1, 3, 3);
                    System.arraycopy(p2, 0, tri1, 6, 3);
                    System.arraycopy(p3, 0, tri1, 9, 3);
                    loadedStlTriangles.add(tri1);

                    float[] tri2 = new float[12];
                    System.arraycopy(normal, 0, tri2, 0, 3);
                    System.arraycopy(p1, 0, tri2, 3, 3);
                    System.arraycopy(p3, 0, tri2, 6, 3);
                    System.arraycopy(p4, 0, tri2, 9, 3);
                    loadedStlTriangles.add(tri2);
                }
            }
        }
    }

    private static void generateSphereTriangles(float radius, int latDiv, int lonDiv) {
        loadedStlTriangles.clear();

        for (int lat = 0; lat < latDiv; lat++) {
            for (int lon = 0; lon < lonDiv; lon++) {

                float lat1 = (float) (Math.PI * lat / latDiv - Math.PI / 2);
                float lat2 = (float) (Math.PI * (lat + 1) / latDiv - Math.PI / 2);
                float lon1 = (float) (2 * Math.PI * lon / lonDiv);
                float lon2 = (float) (2 * Math.PI * (lon + 1) / lonDiv);

                float x1 = (float) (radius * Math.cos(lat1) * Math.cos(lon1));
                float y1 = (float) (radius * Math.sin(lat1));
                float z1 = (float) (radius * Math.cos(lat1) * Math.sin(lon1));

                float x2 = (float) (radius * Math.cos(lat1) * Math.cos(lon2));
                float y2 = (float) (radius * Math.sin(lat1));
                float z2 = (float) (radius * Math.cos(lat1) * Math.sin(lon2));

                float x3 = (float) (radius * Math.cos(lat2) * Math.cos(lon1));
                float y3 = (float) (radius * Math.sin(lat2));
                float z3 = (float) (radius * Math.cos(lat2) * Math.sin(lon1));

                float x4 = (float) (radius * Math.cos(lat2) * Math.cos(lon2));
                float y4 = (float) (radius * Math.sin(lat2));
                float z4 = (float) (radius * Math.cos(lat2) * Math.sin(lon2));

                if (lat > 0) {

                    float[] normal1 = calculateNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);
                    float[] tri1 = { normal1[0], normal1[1], normal1[2], x1, y1, z1, x2, y2, z2, x3, y3, z3 };
                    loadedStlTriangles.add(tri1);
                }

                if (lat < latDiv - 1) {

                    float[] normal2 = calculateNormal(x2, y2, z2, x4, y4, z4, x3, y3, z3);
                    float[] tri2 = { normal2[0], normal2[1], normal2[2], x2, y2, z2, x4, y4, z4, x3, y3, z3 };
                    loadedStlTriangles.add(tri2);
                }
            }
        }
    }

    private static float[] calculateNormal(float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3) {

        float ex1 = x2 - x1, ey1 = y2 - y1, ez1 = z2 - z1;
        float ex2 = x3 - x1, ey2 = y3 - y1, ez2 = z3 - z1;

        float nx = ey1 * ez2 - ez1 * ey2;
        float ny = ez1 * ex2 - ex1 * ez2;
        float nz = ex1 * ey2 - ey1 * ex2;

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[] { nx, ny, nz };
    }

    private static void applyBooleanOperation(CSG newShape, BooleanOp op) {
        if (currentCSG == null || op == BooleanOp.NONE) {
            currentCSG = newShape;
            return;
        }

        switch (op) {
            case UNION:
                currentCSG = currentCSG.union(newShape);
                break;
            case DIFFERENCE:
                currentCSG = currentCSG.difference(newShape);
                break;
            case INTERSECTION:
                currentCSG = currentCSG.intersect(newShape);
                break;
            default:
                currentCSG = newShape;
                break;
        }
    }

    public static void extrude(cad.core.Sketch sketch, float height, BooleanOp op) {
        if (sketch == null || sketch.getEntities().isEmpty()) {
            System.out.println("Sketch is empty, nothing to extrude.");
            return;
        }

        CSG sketchCSG = null;

        for (cad.core.Sketch.Entity entity : sketch.getEntities()) {
            CSG entityCSG = null;

            if (entity instanceof cad.core.Sketch.Polygon) {
                cad.core.Sketch.Polygon poly = (cad.core.Sketch.Polygon) entity;
                List<Vector3d> points = new ArrayList<>();
                for (cad.core.Sketch.PointEntity p : poly.getSketchPoints()) {
                    points.add(Vector3d.xyz(p.getX(), p.getY(), 0));
                }

                entityCSG = Extrude.points(Vector3d.xyz(0, 0, height), points);

            } else if (entity instanceof cad.core.Sketch.Circle) {
                cad.core.Sketch.Circle circle = (cad.core.Sketch.Circle) entity;

                entityCSG = new eu.mihosoft.jcsg.Cylinder(
                        Vector3d.xyz(circle.getX(), circle.getY(), 0),
                        Vector3d.xyz(circle.getX(), circle.getY(), height),
                        circle.getRadius(),
                        32).toCSG();
            }

            if (entityCSG != null) {
                if (sketchCSG == null) {
                    sketchCSG = entityCSG;
                } else {
                    sketchCSG = sketchCSG.union(entityCSG);
                }
            }
        }

        if (sketchCSG == null) {
            System.out.println("No valid extrudable entities found in sketch.");
            return;
        }

        applyBooleanOperation(sketchCSG, op);

        updateMeshFromCSG();

        currShape = Shape.CSG_RESULT;
        System.out.printf("Extrusion performed (Height: %.2f, Op: %s)%n", height, op);
    }

    public static void revolve(cad.core.Sketch sketch, String axisName, float angle, BooleanOp op) {
        if (sketch == null || sketch.getEntities().isEmpty()) {
            System.out.println("Sketch is empty, nothing to revolve.");
            return;
        }

        List<Polygon> allPolygons = new ArrayList<>();
        int steps = 32;
        double angleRad = Math.toRadians(angle);
        double stepAngle = angleRad / steps;

        for (cad.core.Sketch.Entity entity : sketch.getEntities()) {
            List<Vector3d> profilePoints = new ArrayList<>();
            if (entity instanceof cad.core.Sketch.Polygon) {
                cad.core.Sketch.Polygon poly = (cad.core.Sketch.Polygon) entity;
                for (cad.core.Sketch.PointEntity p : poly.getSketchPoints()) {
                    profilePoints.add(Vector3d.xyz(p.getX(), p.getY(), 0));
                }
            } else if (entity instanceof cad.core.Sketch.Circle) {

                cad.core.Sketch.Circle c = (cad.core.Sketch.Circle) entity;
                int circleSteps = 16;
                for (int i = 0; i < circleSteps; i++) {
                    double a = 2 * Math.PI * i / circleSteps;
                    profilePoints.add(Vector3d.xyz(
                            c.getX() + c.getRadius() * Math.cos(a),
                            c.getY() + c.getRadius() * Math.sin(a),
                            0));
                }
            } else {
                continue;
            }

            if (profilePoints.isEmpty())
                continue;

            for (int i = 0; i < steps; i++) {
                double theta1 = i * stepAngle;
                double theta2 = (i + 1) * stepAngle;

                for (int j = 0; j < profilePoints.size(); j++) {
                    int nextJ = (j + 1) % profilePoints.size();
                    Vector3d p1 = profilePoints.get(j);
                    Vector3d p2 = profilePoints.get(nextJ);

                    Vector3d p1_r1 = rotate(p1, axisName, theta1);
                    Vector3d p2_r1 = rotate(p2, axisName, theta1);
                    Vector3d p1_r2 = rotate(p1, axisName, theta2);
                    Vector3d p2_r2 = rotate(p2, axisName, theta2);

                    allPolygons.add(new Polygon(
                            new Vertex(p1_r1, Vector3d.zero()),
                            new Vertex(p2_r1, Vector3d.zero()),
                            new Vertex(p2_r2, Vector3d.zero()),
                            new Vertex(p1_r2, Vector3d.zero())));
                }
            }

        }

        if (allPolygons.isEmpty())
            return;

        CSG revolveCSG = CSG.fromPolygons(allPolygons);

        applyBooleanOperation(revolveCSG, op);

        updateMeshFromCSG();

        currShape = Shape.CSG_RESULT;
        System.out.println("Revolve performed.");
    }

    private static Vector3d rotate(Vector3d p, String axis, double theta) {
        double c = Math.cos(theta);
        double s = Math.sin(theta);

        if ("X".equalsIgnoreCase(axis)) {

            return Vector3d.xyz(p.x(), p.y() * c - p.z() * s, p.y() * s + p.z() * c);
        } else {

            return Vector3d.xyz(p.x() * c + p.z() * s, p.y(), -p.x() * s + p.z() * c);
        }
    }

    private static float[] calculateTriangleNormal(Sketch.Point3D p1, Sketch.Point3D p2, Sketch.Point3D p3) {

        float ex1 = p2.getX() - p1.getX(), ey1 = p2.getY() - p1.getY(), ez1 = p2.getZ() - p1.getZ();
        float ex2 = p3.getX() - p1.getX(), ey2 = p3.getY() - p1.getY(), ez2 = p3.getZ() - p1.getZ();

        float nx = ey1 * ez2 - ez1 * ey2;
        float ny = ez1 * ex2 - ex1 * ez2;
        float nz = ex1 * ey2 - ey1 * ex2;

        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[] { nx, ny, nz };
    }

    private static void updateMeshFromCSG() {
        if (currentCSG == null) {
            extrudedTriangles.clear();
            return;
        }

        extrudedTriangles.clear();

        for (Polygon p : currentCSG.getPolygons()) {
            List<Vertex> vertices = p.vertices;
            if (vertices.size() >= 3) {

                Vector3d n = p.getPlane().getNormal();
                Vector3d v0 = vertices.get(0).pos;

                for (int i = 1; i < vertices.size() - 1; i++) {
                    Vector3d v1 = vertices.get(i).pos;
                    Vector3d v2 = vertices.get(i + 1).pos;

                    float[] tri = new float[12];
                    tri[0] = (float) n.getX();
                    tri[1] = (float) n.getY();
                    tri[2] = (float) n.getZ();
                    tri[3] = (float) v0.getX();
                    tri[4] = (float) v0.getY();
                    tri[5] = (float) v0.getZ();
                    tri[6] = (float) v1.getX();
                    tri[7] = (float) v1.getY();
                    tri[8] = (float) v1.getZ();
                    tri[9] = (float) v2.getX();
                    tri[10] = (float) v2.getY();
                    tri[11] = (float) v2.getZ();

                    extrudedTriangles.add(tri);
                }
            }
        }
    }

    public static void performBoolean(String operation, CSG other) {
        if (currentCSG == null || other == null)
            return;

        switch (operation.toLowerCase()) {
            case "union":
                currentCSG = currentCSG.union(other);
                break;
            case "difference":
                currentCSG = currentCSG.difference(other);
                break;
            case "intersection":
                currentCSG = currentCSG.intersect(other);
                break;
        }
        updateMeshFromCSG();
    }

    public static void saveStl(String filename) throws IOException {

        boolean hasExtrudedGeometry = GuiFX.sketch != null && !GuiFX.sketch.extrudedFaces.isEmpty();

        if (currShape == Shape.NONE && !hasExtrudedGeometry) {
            System.out.println("No shape created yet");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("solid shape");

            if (hasExtrudedGeometry) {

                writeExtrudedSketchToStl(out, GuiFX.sketch);
            } else if (currShape == Shape.CUBE) {
                generateCubeStl(out, param, cubeDivisions);
            } else if (currShape == Shape.SPHERE) {
                generateSphereStl(out, param, sphereLatDiv, sphereLonDiv);
            } else if (currShape == Shape.STL_LOADED && !loadedStlTriangles.isEmpty()) {

                writeLoadedStlTriangles(out);
            }

            out.println("endsolid shape");
        }

        System.out.println("Saved STL file: " + filename);
    }

    private static void writeLoadedStlTriangles(PrintWriter out) {
        for (float[] triData : loadedStlTriangles) {

            writeTriangle(out,
                    triData[0], triData[1], triData[2],
                    triData[3], triData[4], triData[5],
                    triData[6], triData[7], triData[8],
                    triData[9], triData[10], triData[11]);
        }
    }

    private static void writeExtrudedSketchToStl(PrintWriter out, Sketch sketch) {
        int triangleCount = 0;

        for (Sketch.Face3D face : sketch.extrudedFaces) {
            List<Sketch.Point3D> vertices = face.getVertices();
            int numVertices = vertices.size();

            if (numVertices < 3) {
                System.err.println("Warning: Skipping face with less than 3 vertices.");
                continue;
            }

            if (numVertices == 3) {

                writeTriangleFromPoints(out, vertices.get(0), vertices.get(1), vertices.get(2));
                triangleCount++;
            } else {

                for (int i = 1; i < numVertices - 1; i++) {
                    writeTriangleFromPoints(out, vertices.get(0), vertices.get(i), vertices.get(i + 1));
                    triangleCount++;
                }
            }
        }

        System.out.println("Wrote " + triangleCount + " triangles from extruded sketch to STL.");
    }

    private static void writeTriangleFromPoints(PrintWriter out, Sketch.Point3D p1, Sketch.Point3D p2,
            Sketch.Point3D p3) {

        float[] normal = calculateTriangleNormal(p1, p2, p3);

        writeTriangle(out,
                normal[0], normal[1], normal[2],
                p1.getX(), p1.getY(), p1.getZ(),
                p2.getX(), p2.getY(), p2.getZ(),
                p3.getX(), p3.getY(), p3.getZ());
    }

    private static void generateCubeStl(PrintWriter out, float size, int divisions) {
        float half = size / 2.0f;
        float step = size / divisions;

        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float x0 = -half + i * step;
                float x1 = x0 + step;
                float y0 = -half + j * step;
                float y1 = y0 + step;

                writeTriangle(out, 0, 0, 1, x0, y0, half, x1, y0, half, x1, y1, half);
                writeTriangle(out, 0, 0, 1, x0, y0, half, x1, y1, half, x0, y1, half);
            }
        }

        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float x0 = -half + i * step;
                float x1 = x0 + step;
                float y0 = -half + j * step;
                float y1 = y0 + step;
                writeTriangle(out, 0, 0, -1, x0, y0, -half, x0, y1, -half, x1, y1, -half);
                writeTriangle(out, 0, 0, -1, x0, y0, -half, x1, y1, -half, x1, y0, -half);
            }
        }

        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float y0 = -half + i * step;
                float y1 = y0 + step;
                float z0 = -half + j * step;
                float z1 = z0 + step;
                writeTriangle(out, 1, 0, 0, half, y0, z0, half, y1, z0, half, y1, z1);
                writeTriangle(out, 1, 0, 0, half, y0, z0, half, y1, z1, half, y0, z1);
            }
        }

        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float y0 = -half + i * step;
                float y1 = y0 + step;
                float z0 = -half + j * step;
                float z1 = z0 + step;
                writeTriangle(out, -1, 0, 0, -half, y0, z0, -half, y0, z1, -half, y1, z1);
                writeTriangle(out, -1, 0, 0, -half, y0, z0, -half, y1, z1, -half, y1, z0);
            }
        }

        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float x0 = -half + i * step;
                float x1 = x0 + step;
                float z0 = -half + j * step;
                float z1 = z0 + step;
                writeTriangle(out, 0, 1, 0, x0, half, z0, x1, half, z0, x1, half, z1);
                writeTriangle(out, 0, 1, 0, x0, half, z0, x1, half, z1, x0, half, z1);
            }
        }

        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float x0 = -half + i * step;
                float x1 = x0 + step;
                float z0 = -half + j * step;
                float z1 = z0 + step;
                writeTriangle(out, 0, -1, 0, x0, -half, z0, x0, -half, z1, x1, -half, z1);
                writeTriangle(out, 0, -1, 0, x0, -half, z0, x1, -half, z1, x1, -half, z0);
            }
        }
    }

    private static void generateSphereStl(PrintWriter out, float radius, int latDiv, int lonDiv) {

        for (int i = 0; i < latDiv; i++) {
            float theta1 = (float) Math.PI * i / latDiv;
            float theta2 = (float) Math.PI * (i + 1) / latDiv;

            for (int j = 0; j < lonDiv; j++) {
                float phi1 = 2 * (float) Math.PI * j / lonDiv;
                float phi2 = 2 * (float) Math.PI * (j + 1) / lonDiv;

                float[] v1 = sph(radius, theta1, phi1);
                float[] v2 = sph(radius, theta2, phi1);
                float[] v3 = sph(radius, theta2, phi2);
                float[] v4 = sph(radius, theta1, phi2);

                if (i == 0) {

                    writeTriangle(out, v1, v2, v3);
                } else if (i + 1 == latDiv) {

                    writeTriangle(out, v1, v2, v4);
                } else {
                    writeTriangle(out, v1, v2, v3);
                    writeTriangle(out, v1, v3, v4);
                }
            }
        }
    }

    private static float[] sph(float r, float theta, float phi) {
        return new float[] {
                r * (float) Math.sin(theta) * (float) Math.cos(phi),
                r * (float) Math.cos(theta),
                r * (float) Math.sin(theta) * (float) Math.sin(phi)
        };
    }

    private static void writeTriangle(PrintWriter out, float[] a, float[] b, float[] c) {
        writeTriangle(out, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]);
    }

    private static void writeTriangle(PrintWriter out,
            float nx, float ny, float nz,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz) {

        out.printf("  facet normal %f %f %f%n", nx, ny, nz);
        out.println("    outer loop");
        out.printf("      vertex %f %f %f%n", ax, ay, az);
        out.printf("      vertex %f %f %f%n", bx, by, bz);
        out.printf("      vertex %f %f %f%n", cx, cy, cz);
        out.println("    endloop");
        out.println("  endfacet");
    }

    private static void writeTriangle(PrintWriter out,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz) {

        float ux = bx - ax, uy = by - ay, uz = bz - az;
        float vx = cx - ax, vy = cy - ay, vz = cz - az;

        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len != 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        }

        writeTriangle(out, nx, ny, nz, ax, ay, az, bx, by, bz, cx, cy, cz);
    }

    public static void drawCurrentShape(GL2 gl) {
        System.out.println("drawCurrentShape called with shape: " + currShape);
        switch (currShape) {
            case CUBE:
                if (param > 0) {
                    drawCube(gl, param, cubeDivisions);
                }
                break;
            case SPHERE:
                if (param > 0) {
                    drawSphere(gl, param, sphereLatDiv, sphereLonDiv);
                }
                break;
            case STL_LOADED:
                System.out.println("About to call drawLoadedStl");
                drawLoadedStl(gl);
                break;

            case NONE:
            default:
                System.out.println("No shape to draw (NONE or default)");

                break;
        }
    }

    public static void drawCube(GL2 gl, float size, int divisions) {
        float half = size / 2.0f;

        gl.glBegin(GL2.GL_QUADS);

        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3f(half, half, half);
        gl.glVertex3f(-half, half, half);
        gl.glVertex3f(-half, -half, half);
        gl.glVertex3f(half, -half, half);

        gl.glNormal3f(0.0f, 0.0f, -1.0f);
        gl.glVertex3f(half, -half, -half);
        gl.glVertex3f(-half, -half, -half);
        gl.glVertex3f(-half, half, -half);
        gl.glVertex3f(half, half, -half);

        gl.glNormal3f(0.0f, 1.0f, 0.0f);
        gl.glVertex3f(half, half, -half);
        gl.glVertex3f(-half, half, -half);
        gl.glVertex3f(-half, half, half);
        gl.glVertex3f(half, half, half);

        gl.glNormal3f(0.0f, -1.0f, 0.0f);
        gl.glVertex3f(half, -half, half);
        gl.glVertex3f(-half, -half, half);
        gl.glVertex3f(-half, -half, -half);
        gl.glVertex3f(half, -half, -half);

        gl.glNormal3f(1.0f, 0.0f, 0.0f);
        gl.glVertex3f(half, half, -half);
        gl.glVertex3f(half, half, half);
        gl.glVertex3f(half, -half, half);
        gl.glVertex3f(half, -half, -half);

        gl.glNormal3f(-1.0f, 0.0f, 0.0f);
        gl.glVertex3f(-half, half, half);
        gl.glVertex3f(-half, half, -half);
        gl.glVertex3f(-half, -half, -half);
        gl.glVertex3f(-half, -half, half);

        gl.glEnd();
    }

    public static void drawSphere(GL2 gl, float radius, int latDiv, int lonDiv) {
        GLU glu = new GLU();
        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL);
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
        glu.gluQuadricOrientation(quadric, GLU.GLU_OUTSIDE);
        glu.gluSphere(quadric, radius, latDiv, lonDiv);
        glu.gluDeleteQuadric(quadric);
    }

    public static void drawLoadedStl(GL2 gl) {
        System.out.println("drawLoadedStl called. Triangle count: " + loadedStlTriangles.size());
        if (loadedStlTriangles.isEmpty()) {
            System.out.println("No triangles to draw - returning early");
            return;
        }

        gl.glBegin(GL2.GL_TRIANGLES);
        for (float[] triData : loadedStlTriangles) {

            gl.glNormal3f(triData[0], triData[1], triData[2]);

            gl.glVertex3f(triData[3], triData[4], triData[5]);
            gl.glVertex3f(triData[6], triData[7], triData[8]);
            gl.glVertex3f(triData[9], triData[10], triData[11]);
        }
        gl.glEnd();
        System.out.println("Finished drawing " + loadedStlTriangles.size() + " triangles");
    }

    public static float[] calculateCentroid() {
        double totalVolume = 0;
        double momentX = 0;
        double momentY = 0;
        double momentZ = 0;

        List<float[]> triangles = new ArrayList<>();

        if (!loadedStlTriangles.isEmpty())
            triangles.addAll(loadedStlTriangles);
        if (!extrudedTriangles.isEmpty())
            triangles.addAll(extrudedTriangles);

        for (float[] tri : triangles) {

            float x1 = tri[3], y1 = tri[4], z1 = tri[5];
            float x2 = tri[6], y2 = tri[7], z2 = tri[8];
            float x3 = tri[9], y3 = tri[10], z3 = tri[11];

            double vTet = (x1 * (y2 * z3 - z2 * y3) +
                    x2 * (y3 * z1 - z3 * y1) +
                    x3 * (y1 * z2 - z1 * y2)) / 6.0;

            double cx = (x1 + x2 + x3) / 4.0;
            double cy = (y1 + y2 + y3) / 4.0;
            double cz = (z1 + z2 + z3) / 4.0;

            totalVolume += vTet;
            momentX += cx * vTet;
            momentY += cy * vTet;
            momentZ += cz * vTet;
        }

        if (Math.abs(totalVolume) < 1e-9) {
            return new float[] { 0, 0, 0 };
        }

        return new float[] {
                (float) (momentX / totalVolume),
                (float) (momentY / totalVolume),
                (float) (momentZ / totalVolume)
        };
    }

    public static void revolve(Sketch sketch, float angleDegrees, int steps) {
        extrudedTriangles.clear();

        List<Sketch.Polygon> polygons = sketch.getPolygons();
        if (polygons.isEmpty()) {
            System.out.println("No polygons to revolve.");
            return;
        }

        System.out.println("Revolve operation is currently disabled (dependency removed).");

    }

    private static float[] rotateY(Sketch.PointEntity p, float theta) {
        float x = p.getX();
        float y = p.getY();

        float z = 0;

        float cos = (float) Math.cos(theta);
        float sin = (float) Math.sin(theta);

        return new float[] {
                x * cos + z * sin,
                y,
                -x * sin + z * cos
        };
    }

    private static void addTriangleToExtruded(float[] v1, float[] v2, float[] v3) {

        float[] normal = calculateNormal(v1[0], v1[1], v1[2], v2[0], v2[1], v2[2], v3[0], v3[1], v3[2]);
        float[] tri = new float[12];
        System.arraycopy(normal, 0, tri, 0, 3);
        System.arraycopy(v1, 0, tri, 3, 3);
        System.arraycopy(v2, 0, tri, 6, 3);
        System.arraycopy(v3, 0, tri, 9, 3);
        extrudedTriangles.add(tri);
    }

    public static void loft(Sketch sketch, float height) {
        // JCSG Implementation
        List<Sketch.Polygon> polygons = sketch.getPolygons();
        if (polygons.size() < 2) {
            System.out.println("Need at least 2 polygons to loft.");
            return;
        }

        List<Sketch.PointEntity> bottom = polygons.get(0).getSketchPoints();
        List<Sketch.PointEntity> top = polygons.get(1).getSketchPoints();
        int n = Math.min(bottom.size(), top.size());

        List<Polygon> jcsgPolygons = new ArrayList<>();

        // Helper to make vector
        for (int i = 0; i < n; i++) {
            Sketch.PointEntity b1 = bottom.get(i);
            Sketch.PointEntity b2 = bottom.get((i + 1) % n);
            Sketch.PointEntity t1 = top.get(i);
            Sketch.PointEntity t2 = top.get((i + 1) % n);

            Vector3d vb1 = Vector3d.xyz(b1.getX(), b1.getY(), 0);
            Vector3d vb2 = Vector3d.xyz(b2.getX(), b2.getY(), 0);
            Vector3d vt1 = Vector3d.xyz(t1.getX(), t1.getY(), height);
            Vector3d vt2 = Vector3d.xyz(t2.getX(), t2.getY(), height);

            // Side Quad: vb1 -> vb2 -> vt2 -> vt1
            jcsgPolygons.add(new Polygon(
                    new Vertex(vb1, Vector3d.xyz(0, 0, 1)),
                    new Vertex(vb2, Vector3d.xyz(0, 0, 1)),
                    new Vertex(vt2, Vector3d.xyz(0, 0, 1)),
                    new Vertex(vt1, Vector3d.xyz(0, 0, 1))));
        }

        // Add caps if needed? For now just sides as per original "loft" which often
        // implied solid but here we just did sides.
        // A true solid loft needs caps. The previous implementation didn't seem to add
        // caps explicitly either (it used addTriangleToExtruded).
        // Let's add caps to make it a solid CSG.
        // Bottom Cap (Reverse winding: vb2 -> vb1 ...)
        List<Vertex> botCap = new ArrayList<>();
        List<Vertex> topCap = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            botCap.add(new Vertex(Vector3d.xyz(bottom.get(n - 1 - i).getX(), bottom.get(n - 1 - i).getY(), 0),
                    Vector3d.xyz(0, 0, -1)));
            topCap.add(new Vertex(Vector3d.xyz(top.get(i).getX(), top.get(i).getY(), height), Vector3d.xyz(0, 0, 1)));
        }
        jcsgPolygons.add(new Polygon(botCap));
        jcsgPolygons.add(new Polygon(topCap));

        if (!jcsgPolygons.isEmpty()) {
            currentCSG = CSG.fromPolygons(jcsgPolygons);
            updateMeshFromCSG();
            currShape = Shape.CSG_RESULT;
            System.out.println("Loft performed (JCSG).");
        }
    }

    public static void sweep(cad.core.Sketch profile, cad.core.Sketch path, boolean twist) {
        // JCSG Implementation
        List<Sketch.Polygon> profilePolys = profile.getPolygons();
        List<Sketch.Polygon> pathPolys = path.getPolygons();

        if (profilePolys.isEmpty() || pathPolys.isEmpty()) {
            System.out.println("Profile or Path empty.");
            return;
        }

        List<Sketch.PointEntity> profilePoints = profilePolys.get(0).getSketchPoints();
        List<Sketch.PointEntity> pathPoints = pathPolys.get(0).getSketchPoints();

        if (pathPoints.size() < 2)
            return;

        List<List<Vector3d>> frames = new ArrayList<>();

        for (int i = 0; i < pathPoints.size(); i++) {
            Sketch.PointEntity p = pathPoints.get(i);
            float tx, ty, tz;
            if (i < pathPoints.size() - 1) {
                tx = pathPoints.get(i + 1).getX() - p.getX();
                ty = pathPoints.get(i + 1).getY() - p.getY();
                tz = 0;
            } else {
                tx = p.getX() - pathPoints.get(i - 1).getX();
                ty = p.getY() - pathPoints.get(i - 1).getY();
                tz = 0;
            }

            float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (len > 0) {
                tx /= len;
                ty /= len;
                tz /= len;
            }

            // Standard Z-up frame
            float bx = ty;
            float by = -tx;
            float bz = 0;
            float nx = 0;
            float ny = 0;
            float nz = 1;

            List<Vector3d> framePoints = new ArrayList<>();
            for (Sketch.PointEntity pp : profilePoints) {
                float px = pp.getX();
                float py = pp.getY();

                float finalX = p.getX() + px * bx + py * nx;
                float finalY = p.getY() + px * by + py * ny;
                float finalZ = 0 + px * bz + py * nz;

                framePoints.add(Vector3d.xyz(finalX, finalY, finalZ));
            }
            frames.add(framePoints);
        }

        List<Polygon> jcsgPolygons = new ArrayList<>();

        for (int i = 0; i < frames.size() - 1; i++) {
            List<Vector3d> f1 = frames.get(i);
            List<Vector3d> f2 = frames.get(i + 1);
            int m = f1.size();
            for (int j = 0; j < m; j++) {
                Vector3d p1 = f1.get(j);
                Vector3d p2 = f1.get((j + 1) % m);
                Vector3d p3 = f2.get((j + 1) % m);
                Vector3d p4 = f2.get(j);

                // p1->p2->p3->p4
                jcsgPolygons.add(new Polygon(
                        new Vertex(p1, Vector3d.xyz(0, 0, 1)),
                        new Vertex(p2, Vector3d.xyz(0, 0, 1)),
                        new Vertex(p3, Vector3d.xyz(0, 0, 1)),
                        new Vertex(p4, Vector3d.xyz(0, 0, 1))));
            }
        }

        if (!jcsgPolygons.isEmpty()) {
            currentCSG = CSG.fromPolygons(jcsgPolygons);
            updateMeshFromCSG();
            currShape = Shape.CSG_RESULT;
            System.out.println("Sweep performed (JCSG) along path.");
        }
    }

    public static float calculateSurfaceArea() {
        List<float[]> trianglesToCheck = getActiveTriangles();
        if (trianglesToCheck == null || trianglesToCheck.isEmpty()) {
            return 0.0f;
        }

        float totalArea = 0.0f;
        for (float[] t : trianglesToCheck) {

            float x1 = t[3], y1 = t[4], z1 = t[5];
            float x2 = t[6], y2 = t[7], z2 = t[8];
            float x3 = t[9], y3 = t[10], z3 = t[11];

            float abx = x2 - x1, aby = y2 - y1, abz = z2 - z1;
            float acx = x3 - x1, acy = y3 - y1, acz = z3 - z1;

            float cx = aby * acz - abz * acy;
            float cy = abz * acx - abx * acz;
            float cz = abx * acy - aby * acx;

            totalArea += 0.5f * (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        }
        return totalArea;
    }

    public static float calculateVolume() {
        List<float[]> trianglesToCheck = getActiveTriangles();
        if (trianglesToCheck == null || trianglesToCheck.isEmpty()) {
            return 0.0f;
        }

        float totalVolume = 0.0f;
        for (float[] t : trianglesToCheck) {

            float x1 = t[3], y1 = t[4], z1 = t[5];
            float x2 = t[6], y2 = t[7], z2 = t[8];
            float x3 = t[9], y3 = t[10], z3 = t[11];

            float v321 = x3 * y2 * z1;
            float v231 = x2 * y3 * z1;
            float v312 = x3 * y1 * z2;
            float v132 = x1 * y3 * z2;
            float v213 = x2 * y1 * z3;
            float v123 = x1 * y2 * z3;

            totalVolume += (1.0f / 6.0f) * (-v321 + v231 + v312 - v132 - v213 + v123);
        }

        return Math.abs(totalVolume);
    }

    public static void extrudeCut(cad.core.Sketch sketch, float depth) {
        if (currentCSG == null) {
            System.out.println("No target body to cut from.");
            return;
        }

        // 1. Create Tool Body
        CSG toolCSG = null;
        for (cad.core.Sketch.Entity entity : sketch.getEntities()) {
            CSG entityCSG = null;
            if (entity instanceof cad.core.Sketch.Polygon) {
                cad.core.Sketch.Polygon poly = (cad.core.Sketch.Polygon) entity;
                List<Vector3d> points = new ArrayList<>();
                for (cad.core.Sketch.PointEntity p : poly.getSketchPoints()) {
                    points.add(Vector3d.xyz(p.getX(), p.getY(), 0));
                }
                // Extrude tool by depth
                entityCSG = Extrude.points(Vector3d.xyz(0, 0, depth), points);
            } else if (entity instanceof cad.core.Sketch.Circle) {
                cad.core.Sketch.Circle circle = (cad.core.Sketch.Circle) entity;
                entityCSG = new eu.mihosoft.jcsg.Cylinder(
                        Vector3d.xyz(circle.getX(), circle.getY(), 0),
                        Vector3d.xyz(circle.getX(), circle.getY(), depth),
                        circle.getRadius(), 32).toCSG();
            }

            if (entityCSG != null) {
                if (toolCSG == null)
                    toolCSG = entityCSG;
                else
                    toolCSG = toolCSG.union(entityCSG);
            }
        }

        if (toolCSG == null)
            return;

        // 2. Boolean Difference
        applyBooleanOperation(toolCSG, BooleanOp.DIFFERENCE);
        updateMeshFromCSG();
        System.out.println("Extrude Cut performed.");
    }

    public static void shell(float thickness) {
        if (currentCSG == null)
            return;

        List<Polygon> oldPolygons = currentCSG.getPolygons();
        List<Polygon> shellPolygons = new ArrayList<>();

        // Naive Shell: Duplicate polygons, offset vertices along normal, reverse
        // winding for inner shell
        // Using JCSG Polygon objects directly

        for (Polygon p : oldPolygons) {
            // 1. Outer face (keep original)
            shellPolygons.add(p.clone());

            // 2. Inner face (offset and reverse)
            // JCSG Polygon has vertices.
            List<Vertex> innerVertices = new ArrayList<>();
            Vector3d n = p.getPlane().getNormal();

            // Iterate vertices in reverse order for reverse winding
            List<Vertex> verts = p.vertices;
            for (int i = verts.size() - 1; i >= 0; i--) {
                Vertex v = verts.get(i);
                Vector3d pos = v.pos;
                // Offset inward
                Vector3d newPos = pos.minus(n.times(thickness));
                innerVertices.add(new Vertex(newPos, n.negated()));
            }
            shellPolygons.add(new Polygon(innerVertices));
        }

        if (!shellPolygons.isEmpty()) {
            currentCSG = CSG.fromPolygons(shellPolygons);
            updateMeshFromCSG();
            currShape = Shape.CSG_RESULT;
            System.out.println("Shell performed (JCSG) (Thickness: " + thickness + ")");
        }
    }

    private static List<float[]> getActiveTriangles() {
        if (currShape == Shape.STL_LOADED) {
            return loadedStlTriangles;
        } else if (currShape == Shape.EXTRUDED || currShape == Shape.CSG_RESULT) {
            return extrudedTriangles;
        } else if (currShape == Shape.CUBE || currShape == Shape.SPHERE) {
            return loadedStlTriangles;
        }
        return new ArrayList<>();
    }

    public static float[] pickFace(float[] rayOrigin, float[] rayDir) {
        List<float[]> triangles = getActiveTriangles();
        if (triangles == null || triangles.isEmpty())
            return null;

        float minDst = Float.MAX_VALUE;
        float[] closestTri = null;

        for (float[] tri : triangles) {

            float[] v0 = { tri[3], tri[4], tri[5] };
            float[] v1 = { tri[6], tri[7], tri[8] };
            float[] v2 = { tri[9], tri[10], tri[11] };

            Float t = intersectRayTriangle(rayOrigin, rayDir, v0, v1, v2);
            if (t != null && t < minDst) {
                minDst = t;
                closestTri = tri;
            }
        }
        return closestTri;
    }

    private static Float intersectRayTriangle(float[] rayOrigin, float[] rayVector,
            float[] v0, float[] v1, float[] v2) {
        float EPSILON = 0.0000001f;
        float[] edge1 = new float[3];
        float[] edge2 = new float[3];
        float[] h = new float[3];
        float[] s = new float[3];
        float[] q = new float[3];
        float a, f, u, v;

        edge1[0] = v1[0] - v0[0];
        edge1[1] = v1[1] - v0[1];
        edge1[2] = v1[2] - v0[2];

        edge2[0] = v2[0] - v0[0];
        edge2[1] = v2[1] - v0[1];
        edge2[2] = v2[2] - v0[2];

        h[0] = rayVector[1] * edge2[2] - rayVector[2] * edge2[1];
        h[1] = rayVector[2] * edge2[0] - rayVector[0] * edge2[2];
        h[2] = rayVector[0] * edge2[1] - rayVector[1] * edge2[0];

        a = edge1[0] * h[0] + edge1[1] * h[1] + edge1[2] * h[2];

        if (a > -EPSILON && a < EPSILON)
            return null;

        f = 1.0f / a;
        s[0] = rayOrigin[0] - v0[0];
        s[1] = rayOrigin[1] - v0[1];
        s[2] = rayOrigin[2] - v0[2];

        u = f * (s[0] * h[0] + s[1] * h[1] + s[2] * h[2]);

        if (u < 0.0 || u > 1.0)
            return null;

        q[0] = s[1] * edge1[2] - s[2] * edge1[1];
        q[1] = s[2] * edge1[0] - s[0] * edge1[2];
        q[2] = s[0] * edge1[1] - s[1] * edge1[0];

        v = f * (rayVector[0] * q[0] + rayVector[1] * q[1] + rayVector[2] * q[2]);

        if (v < 0.0 || u + v > 1.0)
            return null;

        float t = f * (edge2[0] * q[0] + edge2[1] * q[1] + edge2[2] * q[2]);

        if (t > EPSILON) {
            return t;
        } else {
            return null;
        }
    }

    public static float calculateTriangleArea(float[] tri) {

        float ax = tri[6] - tri[3];
        float ay = tri[7] - tri[4];
        float az = tri[8] - tri[5];

        float bx = tri[9] - tri[3];
        float by = tri[10] - tri[4];
        float bz = tri[11] - tri[5];

        float cx = ay * bz - az * by;
        float cy = az * bx - ax * bz;
        float cz = ax * by - ay * bx;

        return 0.5f * (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    public static void sweep(cad.core.Sketch profileSketch, cad.core.Sketch pathSketch, BooleanOp op) {
        List<Vector3d> pathPoints = extractPathPoints(pathSketch);
        List<Vector3d> baseProfile = extractProfilePoints(profileSketch);
        sweepInternal(baseProfile, pathPoints, op);
    }

    public static void sweep(cad.core.Sketch sketch, BooleanOp op) {
        if (sketch == null)
            return;

        List<Vector3d> baseProfile = null;
        List<Vector3d> pathPoints = null;

        for (cad.core.Sketch.Entity e : sketch.getEntities()) {
            if (e instanceof cad.core.Sketch.Polygon || e instanceof cad.core.Sketch.Circle) {

                if (baseProfile == null) {
                    baseProfile = extractPointsFromEntity(e);
                }
            } else if (e instanceof cad.core.Sketch.Line || e instanceof cad.core.Sketch.Spline) {

                if (pathPoints == null) {
                    pathPoints = extractPointsFromEntity(e);
                }
            }
        }

        if (baseProfile == null || pathPoints == null) {
            System.out.println("Sweep requires 1 Profile (Polygon/Circle) and 1 Path (Line/Spline) in the sketch.");
            return;
        }

        sweepInternal(baseProfile, pathPoints, op);
    }

    private static void sweepInternal(List<Vector3d> baseProfile, List<Vector3d> pathPoints, BooleanOp op) {
        if (baseProfile == null || baseProfile.size() < 3 || pathPoints == null || pathPoints.size() < 2) {
            System.out.println("Invalid Profile or Path for Sweep.");
            return;
        }

        List<List<Vector3d>> sections = new ArrayList<>();
        Vector3d pathStart = pathPoints.get(0);

        for (int i = 0; i < pathPoints.size(); i++) {
            Vector3d pCurrent = pathPoints.get(i);
            Vector3d translation = pCurrent.minus(pathStart);
            List<Vector3d> section = new ArrayList<>();
            for (Vector3d v : baseProfile) {
                section.add(v.plus(translation));
            }
            sections.add(section);
        }

        loftInternal(sections, op, true);
        System.out.println("Sweep operation completed.");
    }

    public static void loft(List<cad.core.Sketch> profiles, BooleanOp op) {
        List<List<Vector3d>> sections = new ArrayList<>();
        if (profiles != null) {
            for (cad.core.Sketch s : profiles) {
                List<Vector3d> p = extractProfilePoints(s);
                if (!p.isEmpty())
                    sections.add(p);
            }
        }
        loftFromSections(sections, op);
    }

    public static void loft(cad.core.Sketch sketch, BooleanOp op) {
        if (sketch == null)
            return;
        List<List<Vector3d>> sections = new ArrayList<>();
        for (cad.core.Sketch.Entity e : sketch.getEntities()) {
            List<Vector3d> p = extractPointsFromEntity(e);
            if (p != null && !p.isEmpty()
                    && (e instanceof cad.core.Sketch.Polygon || e instanceof cad.core.Sketch.Circle)) {
                sections.add(p);
            }
        }
        loftFromSections(sections, op);
    }

    public static void loftFromSections(List<List<Vector3d>> sections, BooleanOp op) {
        if (sections == null || sections.size() < 2) {
            System.out.println("Loft requires at least 2 profiles.");
            return;
        }
        loftInternal(sections, op, true);
        System.out.println("Loft operation completed.");
    }

    private static void loftInternal(List<List<Vector3d>> sections, BooleanOp op, boolean caps) {
        if (sections.size() < 2)
            return;

        List<Polygon> polygons = new ArrayList<>();

        for (int i = 0; i < sections.size() - 1; i++) {
            List<Vector3d> s1 = sections.get(i);
            List<Vector3d> s2 = sections.get(i + 1);

            int size = Math.min(s1.size(), s2.size());

            for (int j = 0; j < size; j++) {
                int next = (j + 1) % size;

                polygons.add(Polygon.fromPoints(
                        s1.get(j),
                        s1.get(next),
                        s2.get(next),
                        s2.get(j)));
            }
        }

        if (caps) {
            polygons.add(Polygon.fromPoints(sections.get(0)));
            List<Vector3d> endCap = new ArrayList<>(sections.get(sections.size() - 1));
            java.util.Collections.reverse(endCap);
            polygons.add(Polygon.fromPoints(endCap));
        }

        CSG newShape = CSG.fromPolygons(polygons);
        applyBooleanOperation(newShape, op);
        updateMeshFromCSG();
        currShape = Shape.CSG_RESULT;
    }

    private static List<Vector3d> extractPointsFromEntity(cad.core.Sketch.Entity e) {
        List<Vector3d> points = new ArrayList<>();
        if (e instanceof cad.core.Sketch.Polygon) {
            for (cad.core.Sketch.PointEntity pe : ((cad.core.Sketch.Polygon) e).getSketchPoints()) {
                points.add(Vector3d.xyz(pe.getX(), pe.getY(), 0));
            }
        } else if (e instanceof cad.core.Sketch.Circle) {
            cad.core.Sketch.Circle c = (cad.core.Sketch.Circle) e;
            int steps = 32;
            for (int i = 0; i < steps; i++) {
                double angle = 2 * Math.PI * i / steps;
                double x = c.getX() + c.getRadius() * Math.cos(angle);
                double y = c.getY() + c.getRadius() * Math.sin(angle);
                points.add(Vector3d.xyz(x, y, 0));
            }
        } else if (e instanceof cad.core.Sketch.Line) {
            cad.core.Sketch.Line l = (cad.core.Sketch.Line) e;
            points.add(Vector3d.xyz(l.getX1(), l.getY1(), 0));
            points.add(Vector3d.xyz(l.getX2(), l.getY2(), 0));
        } else if (e instanceof cad.core.Sketch.Spline) {
            cad.core.Sketch.Spline s = (cad.core.Sketch.Spline) e;

            for (cad.core.Sketch.PointEntity pe : s.getControlPoints()) {
                points.add(Vector3d.xyz(pe.getX(), pe.getY(), 0));
            }
        }
        return points;
    }

    private static List<Vector3d> extractPathPoints(cad.core.Sketch sketch) {
        List<Vector3d> points = new ArrayList<>();
        if (sketch == null)
            return points;
        for (cad.core.Sketch.Entity e : sketch.getEntities()) {
            if (e instanceof cad.core.Sketch.Line || e instanceof cad.core.Sketch.Spline) {
                points.addAll(extractPointsFromEntity(e));
            }
        }
        return points;
    }

    private static List<Vector3d> extractProfilePoints(cad.core.Sketch sketch) {
        List<Vector3d> points = new ArrayList<>();
        if (sketch == null)
            return points;
        for (cad.core.Sketch.Entity e : sketch.getEntities()) {
            if (e instanceof cad.core.Sketch.Polygon || e instanceof cad.core.Sketch.Circle) {
                points.addAll(extractPointsFromEntity(e));
            }
        }
        return points;
    }
}
