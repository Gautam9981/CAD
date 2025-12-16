package cad.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

// JOGL imports for rendering methods in Geometry
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

// GUI import to access sketch data
import cad.gui.GuiFX;

/**
 * Core geometry engine for 3D shape creation, STL file operations, and model
 * management.
 * Provides procedural generation of cubes and spheres, STL import/export, and
 * rendering support.
 */
public class Geometry {

    /**
     * Supported 3D shapes and model types.
     * STL_LOADED represents externally loaded models.
     * // EXTRUDED represents extruded 2D sketches.
     */
    public enum Shape {
        NONE,
        CUBE,
        SPHERE,
        STL_LOADED, // Added for loaded STL models
        EXTRUDED // Added for extruded sketches
    }

    private static Shape currShape = Shape.NONE; // Currently active shape
    private static float param = 0.0f; // Size parameter (size or radius)
    public static int cubeDivisions = 1; // Cube subdivisions per edge
    public static int sphereLatDiv = 30; // Sphere latitude divisions
    public static int sphereLonDiv = 30; // Sphere longitude divisions
    private static List<float[]> extrudedTriangles = new ArrayList<>(); // Extruded geometry storage

    /**
     * Triangle data from loaded STL files. Each array contains:
     * [normalX, normalY, normalZ, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
     */
    private static List<float[]> loadedStlTriangles = new ArrayList<>();

    /**
     * Gets the currently active shape type for rendering decisions.
     * 
     * @return Current Shape enum value
     */
    public static Shape getCurrentShape() {
        return currShape;
    }

    /**
     * Gets the size parameter of current shape (cube size or sphere radius).
     * 
     * @return Size/radius parameter value
     */
    public static float getParam() {
        return param;
    }

    /**
     * Gets triangles from loaded STL file for rendering.
     * 
     * @return List of triangle arrays with format [nx, ny, nz, v1x, v1y, v1z, v2x,
     *         v2y, v2z, v3x, v3y, v3z]
     */
    public static List<float[]> getLoadedStlTriangles() {
        return loadedStlTriangles;
    }

    /**
     * Gets triangles from extruded sketch for rendering.
     * 
     * @return List of triangle arrays with format [nx, ny, nz, v1x, v1y, v1z, v2x,
     *         v2y, v2z, v3x, v3y, v3z]
     */
    public static List<float[]> getExtrudedTriangles() {
        return extrudedTriangles;
    }

    /**
     * Calculates maximum dimension of current model for auto-scaling views.
     * 
     * @return Maximum width, height, or depth of the model
     */
    public static float getModelMaxDimension() {
        List<float[]> trianglesToCheck = null;

        // Determine which triangle list to use based on current shape
        switch (currShape) {
            case CUBE:
                return param; // For cube, param is the edge length
            case SPHERE:
                return param * 2; // For sphere, param is radius, so diameter is max dimension
            case STL_LOADED:
                if (!loadedStlTriangles.isEmpty()) {
                    trianglesToCheck = loadedStlTriangles;
                }
                break;
            // case EXTRUDED:
            // if (!extrudedTriangles.isEmpty()) {
            // trianglesToCheck = extrudedTriangles;
            // }
            // break;
            default:
                return 2.0f; // Default fallback
        }

        // If no triangles to check, return default
        if (trianglesToCheck == null || trianglesToCheck.isEmpty()) {
            return 2.0f;
        }

        // Calculate bounds for triangles - create a copy to avoid
        // ConcurrentModificationException
        List<float[]> trianglesCopy;
        synchronized (trianglesToCheck) {
            trianglesCopy = new ArrayList<>(trianglesToCheck);
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (float[] triData : trianglesCopy) {
            // Check all 3 vertices of each triangle (indices 3-11)
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

    /**
     * Loads STL file and parses triangles into internal format.
     * Sets current shape to STL_LOADED on success.
     * 
     * @param filename Path to STL file
     * @throws IOException if file reading fails or format is invalid
     */
    public static List<float[]> loadStl(String filename) throws IOException {
        loadedStlTriangles.clear(); // Clear any previously loaded data
        currShape = Shape.NONE; // Reset shape type while loading

        System.out.println("Loading STL file: " + filename);

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            float[] currentNormal = new float[3]; // To store the normal for the current facet
            float[] currentVertices = new float[9]; // To store the 3 vertices for the current facet
            int facetCount = 0;
            int errorCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();

                if (line.startsWith("facet normal")) {
                    facetCount++;
                    // Parse the normal components
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

                    // Skip "outer loop" line
                    String outerLoop = reader.readLine();
                    if (outerLoop == null || !outerLoop.trim().toLowerCase().contains("outer loop")) {
                        System.err.println("Warning: Expected 'outer loop' after facet normal in facet " + facetCount);
                        errorCount++;
                    }

                    // Read 3 vertices for the current facet
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
                        // Combine normal and vertices into one array for storage
                        float[] fullTriangleData = new float[12]; // 3 normal + 9 vertices
                        System.arraycopy(currentNormal, 0, fullTriangleData, 0, 3);
                        System.arraycopy(currentVertices, 0, fullTriangleData, 3, 9);
                        loadedStlTriangles.add(fullTriangleData);
                    }

                    // Skip "endloop" and "endfacet" lines
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

            // Debug: Print model bounds
            if (!loadedStlTriangles.isEmpty()) {
                float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
                float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

                for (float[] triData : loadedStlTriangles) {
                    // Check all 3 vertices of each triangle (indices 3-11)
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

                // Center the model by translating all vertices
                centerModel(centerX, centerY, centerZ);
                System.out.println("Model centered at origin for proper rotation");
            }

            currShape = Shape.STL_LOADED; // Set the current shape to STL_LOADED
            return loadedStlTriangles;
        } catch (NumberFormatException e) {
            throw new IOException("Error parsing numeric data in STL file: " + e.getMessage(), e);
        }
    }

    /**
     * Centers the loaded STL model by translating all vertices so that the model's
     * center becomes the origin (0, 0, 0). This ensures proper rotation around the
     * center.
     *
     * @param centerX The X-coordinate of the model's current center
     * @param centerY The Y-coordinate of the model's current center
     * @param centerZ The Z-coordinate of the model's current center
     */
    private static void centerModel(float centerX, float centerY, float centerZ) {
        // Translate all vertices by subtracting the center coordinates
        for (float[] triData : loadedStlTriangles) {
            // Update all 3 vertices of each triangle (indices 3-11)
            for (int i = 0; i < 3; i++) {
                triData[3 + i * 3] -= centerX; // X coordinate
                triData[4 + i * 3] -= centerY; // Y coordinate
                triData[5 + i * 3] -= centerZ; // Z coordinate
            }
        }
    }

    /**
     * Creates a procedural cube with specified size and subdivisions.
     * 
     * @param size      Edge length of the cube (0.1-100.0)
     * @param divisions Number of subdivisions per edge (1-200)
     */
    public static void createCube(float size, int divisions) {
        if (size < 0.1f || size > 100.0f) {
            throw new IllegalArgumentException("Cube size must be between 0.1 and 100.0");
        }
        if (divisions < 1 || divisions > 200) {
            throw new IllegalArgumentException("Cube divisions must be between 1 and 200");
        }

        // Warn about performance for high subdivision counts
        int totalTriangles = divisions * divisions * 6 * 2; // 6 faces * divisions^2 * 2 triangles per quad
        if (totalTriangles > 50000) {
            System.out.println(
                    "Warning: Creating cube with " + totalTriangles + " triangles. This may impact performance.");
        }

        cubeDivisions = divisions;
        param = size;
        currShape = Shape.CUBE;

        // Generate cube triangles centered at origin
        generateCubeTriangles(size, divisions);

        System.out.printf("Cube created with size %.2f and %d subdivisions (%d triangles)%n", size, divisions,
                totalTriangles);
    }

    /**
     * Creates a procedural UV sphere with specified radius and subdivisions.
     * 
     * @param radius    Radius of the sphere (0.1-50.0)
     * @param divisions Number of latitude/longitude subdivisions (3-100)
     */
    public static void createSphere(float radius, int divisions) {
        createSphere(radius, divisions, divisions);
    }

    /**
     * Creates a procedural UV sphere with specified radius and separate lat/lon subdivisions.
     * 
     * @param radius    Radius of the sphere (0.1-50.0)
     * @param latDiv    Number of latitude subdivisions (3-100)
     * @param lonDiv    Number of longitude subdivisions (3-100)
     */
    public static void createSphere(float radius, int latDiv, int lonDiv) {
        if (radius < 0.1f || radius > 80.0f) {
            throw new IllegalArgumentException("Sphere radius must be between 0.1 and 80.0");
        }
        if (latDiv < 3 || latDiv > 100 || lonDiv < 3 || lonDiv > 100) {
            throw new IllegalArgumentException("Sphere divisions must be between 3 and 100");
        }
        sphereLatDiv = latDiv;
        sphereLonDiv = lonDiv;
        param = radius;
        currShape = Shape.SPHERE;

        // Generate sphere triangles centered at origin
        generateSphereTriangles(radius, latDiv, lonDiv);

        System.out.printf("Sphere created with radius %.2f and %dx%d subdivisions%n", radius, latDiv, lonDiv);
    }

    /**
     * Generates triangle data for a cube centered at the origin and stores it in
     * loadedStlTriangles.
     * The cube extends from -size/2 to +size/2 in all dimensions.
     *
     * @param size      The size of the cube's edge.
     * @param divisions Number of subdivisions per edge.
     */
    private static void generateCubeTriangles(float size, int divisions) {
        loadedStlTriangles.clear(); // Clear any previous data

        float halfSize = size / 2.0f;
        float step = size / divisions;

        // Generate triangles for all 6 faces of the cube
        // Each face is subdivided into divisions x divisions quads, each made of 2
        // triangles

        for (int face = 0; face < 6; face++) {
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float u1 = -halfSize + i * step;
                    float u2 = -halfSize + (i + 1) * step;
                    float v1 = -halfSize + j * step;
                    float v2 = -halfSize + (j + 1) * step;

                    float[] p1 = new float[3], p2 = new float[3], p3 = new float[3], p4 = new float[3];
                    float[] normal = new float[3];

                    // Define vertices and normals for each face
                    switch (face) {
                        case 0: // Front face (Z = +halfSize)
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
                        case 1: // Back face (Z = -halfSize)
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
                        case 2: // Right face (X = +halfSize)
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
                        case 3: // Left face (X = -halfSize)
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
                        case 4: // Top face (Y = +halfSize)
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
                        case 5: // Bottom face (Y = -halfSize)
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

                    // Create two triangles for each quad
                    // Triangle 1: p1, p2, p3
                    float[] tri1 = new float[12];
                    System.arraycopy(normal, 0, tri1, 0, 3);
                    System.arraycopy(p1, 0, tri1, 3, 3);
                    System.arraycopy(p2, 0, tri1, 6, 3);
                    System.arraycopy(p3, 0, tri1, 9, 3);
                    loadedStlTriangles.add(tri1);

                    // Triangle 2: p1, p3, p4
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

    /**
     * Generates triangle data for a sphere centered at the origin and stores it in
     * loadedStlTriangles.
     *
     * @param radius Radius of the sphere.
     * @param latDiv Number of latitude divisions.
     * @param lonDiv Number of longitude divisions.
     */
    private static void generateSphereTriangles(float radius, int latDiv, int lonDiv) {
        loadedStlTriangles.clear(); // Clear any previous data

        for (int lat = 0; lat < latDiv; lat++) {
            for (int lon = 0; lon < lonDiv; lon++) {
                // Calculate angles
                float lat1 = (float) (Math.PI * lat / latDiv - Math.PI / 2);
                float lat2 = (float) (Math.PI * (lat + 1) / latDiv - Math.PI / 2);
                float lon1 = (float) (2 * Math.PI * lon / lonDiv);
                float lon2 = (float) (2 * Math.PI * (lon + 1) / lonDiv);

                // Calculate vertices
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

                // Create triangles for the quad (avoiding degenerate triangles at poles)
                if (lat > 0) { // Skip top pole
                    // Triangle 1: (x1,y1,z1), (x2,y2,z2), (x3,y3,z3)
                    float[] normal1 = calculateNormal(x1, y1, z1, x2, y2, z2, x3, y3, z3);
                    float[] tri1 = { normal1[0], normal1[1], normal1[2], x1, y1, z1, x2, y2, z2, x3, y3, z3 };
                    loadedStlTriangles.add(tri1);
                }

                if (lat < latDiv - 1) { // Skip bottom pole
                    // Triangle 2: (x2,y2,z2), (x4,y4,z4), (x3,y3,z3)
                    float[] normal2 = calculateNormal(x2, y2, z2, x4, y4, z4, x3, y3, z3);
                    float[] tri2 = { normal2[0], normal2[1], normal2[2], x2, y2, z2, x4, y4, z4, x3, y3, z3 };
                    loadedStlTriangles.add(tri2);
                }
            }
        }
    }

    /**
     * Calculates the normal vector for a triangle defined by three vertices.
     * Uses cross product of two edge vectors.
     */
    private static float[] calculateNormal(float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3) {
        // Edge vectors
        float ex1 = x2 - x1, ey1 = y2 - y1, ez1 = z2 - z1;
        float ex2 = x3 - x1, ey2 = y3 - y1, ez2 = z3 - z1;

        // Cross product
        float nx = ey1 * ez2 - ez1 * ey2;
        float ny = ez1 * ex2 - ex1 * ez2;
        float nz = ex1 * ey2 - ey1 * ex2;

        // Normalize
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[] { nx, ny, nz };
    }

    /**
     * Extrude a closed sketch to create a 3D shape.
     * Converts closed polygons in the sketch into 3D geometry.
     *
     * @param sketch Sketch to extrude.
     * @param height Extrusion height.
     */
    // public static void extrude(Sketch sketch, float height) {
    // if (!sketch.isClosedLoop()) {
    // System.out.println("Sketch must contain at least one closed loop (polygon) to
    // extrude.");
    // return;
    // }

    // extrudedTriangles.clear();

    // // Use the sketch's built-in extrude functionality
    // sketch.extrude(height);

    // // Convert Face3D objects from sketch to triangle format for rendering
    // for (Sketch.Face3D face : sketch.extrudedFaces) {
    // convertFaceToTriangles(face);
    // }

    // currShape = Shape.EXTRUDED;
    // System.out.println("Sketch extruded successfully. Generated " +
    // extrudedTriangles.size() + " triangles.");
    // }

    /**
     * Converts a Face3D object to triangles and adds them to extrudedTriangles.
     * Uses triangle fan approach for faces with more than 3 vertices.
     *
     * @param face Face3D object to convert.
     */
    // private static void convertFaceToTriangles(Sketch.Face3D face) {
    // List<Sketch.Point3D> vertices = face.getVertices();
    // int numVertices = vertices.size();

    // if (numVertices < 3) {
    // System.err.println("Warning: Face with less than 3 vertices cannot be
    // triangulated.");
    // return;
    // }

    // if (numVertices == 3) {
    // // Already a triangle
    // addTriangleToExtruded(vertices.get(0), vertices.get(1), vertices.get(2));
    // } else {
    // // Triangle fan: connect all vertices to the first vertex
    // for (int i = 1; i < numVertices - 1; i++) {
    // addTriangleToExtruded(vertices.get(0), vertices.get(i), vertices.get(i + 1));
    // }
    // }
    // }

    /**
     * Adds a triangle to the extruded triangles list in the correct format.
     * Format: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
     *
     * @param p1 First vertex
     * @param p2 Second vertex
     * @param p3 Third vertex
     */
    // private static void addTriangleToExtruded(Sketch.Point3D p1, Sketch.Point3D
    // p2, Sketch.Point3D p3) {
    // // Calculate normal vector using cross product
    // float[] normal = calculateTriangleNormal(p1, p2, p3);

    // // Create triangle array with normal and vertices
    // float[] triangle = new float[12];
    // triangle[0] = normal[0]; // nx
    // triangle[1] = normal[1]; // ny
    // triangle[2] = normal[2]; // nz
    // triangle[3] = p1.getX(); // v1x
    // triangle[4] = p1.getY(); // v1y
    // triangle[5] = p1.getZ(); // v1z
    // triangle[6] = p2.getX(); // v2x
    // triangle[7] = p2.getY(); // v2y
    // triangle[8] = p2.getZ(); // v2z
    // triangle[9] = p3.getX(); // v3x
    // triangle[10] = p3.getY(); // v3y
    // triangle[11] = p3.getZ(); // v3z

    // extrudedTriangles.add(triangle);
    // }

    /**
     * Calculates the normal vector for a triangle defined by three 3D points.
     *
     * @param p1 First vertex
     * @param p2 Second vertex
     * @param p3 Third vertex
     * @return Normal vector as float array [nx, ny, nz]
     */
    private static float[] calculateTriangleNormal(Sketch.Point3D p1, Sketch.Point3D p2, Sketch.Point3D p3) {
        // Edge vectors
        float ex1 = p2.getX() - p1.getX(), ey1 = p2.getY() - p1.getY(), ez1 = p2.getZ() - p1.getZ();
        float ex2 = p3.getX() - p1.getX(), ey2 = p3.getY() - p1.getY(), ez2 = p3.getZ() - p1.getZ();

        // Cross product
        float nx = ey1 * ez2 - ez1 * ey2;
        float ny = ez1 * ex2 - ex1 * ez2;
        float nz = ex1 * ey2 - ey1 * ex2;

        // Normalize
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length > 0) {
            nx /= length;
            ny /= length;
            nz /= length;
        }

        return new float[] { nx, ny, nz };
    }

    /**
     * Saves current geometry (cube, sphere, loaded STL, or extruded shapes) to STL
     * file.
     * 
     * @param filename Output file path
     * @throws IOException if file writing fails
     */
    public static void saveStl(String filename) throws IOException {
        // Check if we have extruded faces available from GuiFX.sketch
        boolean hasExtrudedGeometry = GuiFX.sketch != null && !GuiFX.sketch.extrudedFaces.isEmpty();

        if (currShape == Shape.NONE && !hasExtrudedGeometry) {
            System.out.println("No shape created yet");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("solid shape");

            if (hasExtrudedGeometry) {
                // Export extruded geometry directly from the sketch
                writeExtrudedSketchToStl(out, GuiFX.sketch);
            } else if (currShape == Shape.CUBE) {
                generateCubeStl(out, param, cubeDivisions);
            } else if (currShape == Shape.SPHERE) {
                generateSphereStl(out, param, sphereLatDiv, sphereLonDiv);
            } else if (currShape == Shape.STL_LOADED && !loadedStlTriangles.isEmpty()) {
                // If a loaded STL is the current shape, just write its triangles back out
                writeLoadedStlTriangles(out);
            }

            out.println("endsolid shape");
        }

        System.out.println("Saved STL file: " + filename);
    }

    /**
     * Generates and writes STL facets for the current `loadedStlTriangles` data.
     * This is used when saving a previously loaded STL file.
     *
     * @param out PrintWriter to write STL data.
     */
    private static void writeLoadedStlTriangles(PrintWriter out) {
        for (float[] triData : loadedStlTriangles) {
            // triData format: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
            writeTriangle(out,
                    triData[0], triData[1], triData[2], // Normal
                    triData[3], triData[4], triData[5], // V1
                    triData[6], triData[7], triData[8], // V2
                    triData[9], triData[10], triData[11]);// V3
        }
    }

    /**
     * Generates and writes STL facets for the current `extrudedTriangles` data.
     * This is used when saving extruded geometry to STL format.
     *
     * @param out PrintWriter to write STL data.
     */
    /**
     * Writes extruded sketch geometry directly to STL format.
     * Takes the Face3D objects from the sketch and converts them to triangles.
     *
     * @param out    PrintWriter to write STL data.
     * @param sketch The sketch containing extruded faces.
     */
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
                // Already a triangle - write directly
                writeTriangleFromPoints(out, vertices.get(0), vertices.get(1), vertices.get(2));
                triangleCount++;
            } else {
                // Triangle fan: connect all vertices to the first vertex
                for (int i = 1; i < numVertices - 1; i++) {
                    writeTriangleFromPoints(out, vertices.get(0), vertices.get(i), vertices.get(i + 1));
                    triangleCount++;
                }
            }
        }

        System.out.println("Wrote " + triangleCount + " triangles from extruded sketch to STL.");
    }

    /**
     * Helper method to write a triangle to STL from three Point3D objects.
     * Calculates the normal automatically.
     *
     * @param out PrintWriter to write STL data.
     * @param p1  First vertex
     * @param p2  Second vertex
     * @param p3  Third vertex
     */
    private static void writeTriangleFromPoints(PrintWriter out, Sketch.Point3D p1, Sketch.Point3D p2,
            Sketch.Point3D p3) {
        // Calculate normal vector using cross product
        float[] normal = calculateTriangleNormal(p1, p2, p3);

        // Write triangle to STL
        writeTriangle(out,
                normal[0], normal[1], normal[2], // Normal
                p1.getX(), p1.getY(), p1.getZ(), // V1
                p2.getX(), p2.getY(), p2.getZ(), // V2
                p3.getX(), p3.getY(), p3.getZ());// V3
    }

    /**
     * Generate STL triangles for a cube and write to output.
     *
     * @param out       PrintWriter to write STL.
     * @param size      Edge length of the cube.
     * @param divisions Number of subdivisions per edge.
     */
    private static void generateCubeStl(PrintWriter out, float size, int divisions) {
        float half = size / 2.0f;
        float step = size / divisions;

        // Front face (+Z)
        for (int i = 0; i < divisions; i++) {
            for (int j = 0; j < divisions; j++) {
                float x0 = -half + i * step;
                float x1 = x0 + step;
                float y0 = -half + j * step;
                float y1 = y0 + step;
                // Two triangles for each square subdivision
                writeTriangle(out, 0, 0, 1, x0, y0, half, x1, y0, half, x1, y1, half);
                writeTriangle(out, 0, 0, 1, x0, y0, half, x1, y1, half, x0, y1, half);
            }
        }
        // Back face (-Z)
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
        // Right face (+X)
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
        // Left face (-X)
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
        // Top face (+Y)
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
        // Bottom face (-Y)
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

    /**
     * Generate STL triangles for a sphere and write to output.
     *
     * @param out    PrintWriter to write STL.
     * @param radius Sphere radius.
     * @param latDiv Latitude subdivisions.
     * @param lonDiv Longitude subdivisions.
     */
    private static void generateSphereStl(PrintWriter out, float radius, int latDiv, int lonDiv) {
        // This method generates sphere geometry. For robust results, it might be better
        // to pre-calculate normals based on the sphere's surface (normalized vertex
        // position)
        // rather than using cross product of triangle vertices if the triangulation is
        // poor.
        // However, for standard subdivision, cross product gives decent results.
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

                // For top/bottom caps, triangles share a pole vertex
                if (i == 0) { // Top cap
                    // v1, v2, v3 form a triangle that connects to the top pole implicitly
                    // Normal for top pole region can be (0,1,0) or calculated from triangle.
                    writeTriangle(out, v1, v2, v3);
                } else if (i + 1 == latDiv) { // Bottom cap
                    // v1, v2, v4 form a triangle that connects to the bottom pole implicitly
                    writeTriangle(out, v1, v2, v4);
                } else { // Middle sections, form two triangles (a quad)
                    writeTriangle(out, v1, v2, v3);
                    writeTriangle(out, v1, v3, v4);
                }
            }
        }
    }

    /**
     * Calculate Cartesian coordinates for a point on a sphere given spherical
     * angles.
     *
     * @param r     Radius of the sphere.
     * @param theta Polar angle (latitude), from 0 (north pole) to PI (south pole).
     * @param phi   Azimuthal angle (longitude), from 0 to 2*PI.
     * @return Cartesian coordinates as float array [x, y, z].
     */
    private static float[] sph(float r, float theta, float phi) {
        return new float[] {
                r * (float) Math.sin(theta) * (float) Math.cos(phi),
                r * (float) Math.cos(theta),
                r * (float) Math.sin(theta) * (float) Math.sin(phi)
        };
    }

    /**
     * Write a triangle facet to the STL file, computing its normal automatically.
     * This method is used when generating new geometry (e.g., cube, sphere).
     *
     * @param out PrintWriter to write STL.
     * @param a   Vertex A coordinates as float array [x, y, z].
     * @param b   Vertex B coordinates as float array [x, y, z].
     * @param c   Vertex C coordinates as float array [x, y, z].
     */
    private static void writeTriangle(PrintWriter out, float[] a, float[] b, float[] c) {
        writeTriangle(out, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]);
    }

    /**
     * Write a triangle facet to the STL file with an explicitly provided normal.
     * This overload is useful when the normal is already known (e.g., from loaded
     * STL data)
     * or for specific surface normals (like flat faces of a cube).
     *
     * @param out PrintWriter to write STL.
     * @param nx  Normal vector X component.
     * @param ny  Normal vector Y component.
     * @param nz  Normal vector Z component.
     * @param ax  X of vertex A.
     * @param ay  Y of vertex A.
     * @param az  Z of vertex A.
     * @param bx  X of vertex B.
     * @param by  Y of vertex B.
     * @param bz  Z of vertex B.
     * @param cx  X of vertex C.
     * @param cy  Y of vertex C.
     * @param cz  Z of vertex C.
     */
    private static void writeTriangle(PrintWriter out,
            float nx, float ny, float nz,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz) {
        // Write facet to STL file using the provided normal
        out.printf("  facet normal %f %f %f%n", nx, ny, nz);
        out.println("    outer loop");
        out.printf("      vertex %f %f %f%n", ax, ay, az);
        out.printf("      vertex %f %f %f%n", bx, by, bz);
        out.printf("      vertex %f %f %f%n", cx, cy, cz);
        out.println("    endloop");
        out.println("  endfacet");
    }

    /**
     * Write a triangle facet to the STL file, computing its normal based on vertex
     * winding.
     * This is useful for generated geometry where facet normals aren't pre-defined.
     *
     * @param out PrintWriter to write STL.
     * @param ax  X of vertex A.
     * @param ay  Y of vertex A.
     * @param az  Z of vertex A.
     * @param bx  X of vertex B.
     * @param by  Y of vertex B.
     * @param bz  Z of vertex B.
     * @param cx  X of vertex C.
     * @param cy  Y of vertex C.
     * @param cz  Z of vertex C.
     */
    private static void writeTriangle(PrintWriter out,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float cx, float cy, float cz) {
        // Compute vectors U and V
        float ux = bx - ax, uy = by - ay, uz = bz - az;
        float vx = cx - ax, vy = cy - ay, vz = cz - az;

        // Calculate normal vector (cross product U x V)
        float nx = uy * vz - uz * vy;
        float ny = uz * vx - ux * vz;
        float nz = ux * vy - uy * vx;

        // Normalize the normal vector
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len != 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        }

        // Write facet to STL file using the calculated normal
        writeTriangle(out, nx, ny, nz, ax, ay, az, bx, by, bz, cx, cy, cz);
    }

    /**
     * Draws the currently selected 3D shape using OpenGL. This method is called by
     * JOGLCadCanvas's display method.
     * It handles drawing of cubes, spheres, loaded STL models, and extruded
     * sketches.
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    public static void drawCurrentShape(GL2 gl) { // Renamed from drawLoadedStl to reflect general purpose
        System.out.println("drawCurrentShape called with shape: " + currShape); // Debug
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
                System.out.println("About to call drawLoadedStl"); // Debug
                drawLoadedStl(gl); // Call the specific STL drawing method
                break;
            // case EXTRUDED:
            // System.out.println("About to call drawExtruded"); // Debug
            // drawExtruded(gl); // Draw extruded geometry
            // break;
            case NONE:
            default:
                System.out.println("No shape to draw (NONE or default)"); // Debug
                // No shape to draw
                break;
        }
    }

    /**
     * Draws a cube using OpenGL. Vertices and normals are calculated on the fly.
     * For high performance with many cubes, consider using VBOs.
     *
     * @param gl        The GL2 object.
     * @param size      Edge length of the cube.
     * @param divisions Number of subdivisions per edge (for finer detail, though
     *                  simple cube just draws 6 faces).
     * 
     */

    public static void drawCube(GL2 gl, float size, int divisions) {
        float half = size / 2.0f;

        gl.glBegin(GL2.GL_QUADS); // Use GL_QUADS for simplicity, or GL_TRIANGLES for robustness

        // Front Face (+Z)
        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3f(half, half, half);
        gl.glVertex3f(-half, half, half);
        gl.glVertex3f(-half, -half, half);
        gl.glVertex3f(half, -half, half);

        // Back Face (-Z)
        gl.glNormal3f(0.0f, 0.0f, -1.0f);
        gl.glVertex3f(half, -half, -half);
        gl.glVertex3f(-half, -half, -half);
        gl.glVertex3f(-half, half, -half);
        gl.glVertex3f(half, half, -half);

        // Top Face (+Y)
        gl.glNormal3f(0.0f, 1.0f, 0.0f);
        gl.glVertex3f(half, half, -half);
        gl.glVertex3f(-half, half, -half);
        gl.glVertex3f(-half, half, half);
        gl.glVertex3f(half, half, half);

        // Bottom Face (-Y)
        gl.glNormal3f(0.0f, -1.0f, 0.0f);
        gl.glVertex3f(half, -half, half);
        gl.glVertex3f(-half, -half, half);
        gl.glVertex3f(-half, -half, -half);
        gl.glVertex3f(half, -half, -half);

        // Right Face (+X)
        gl.glNormal3f(1.0f, 0.0f, 0.0f);
        gl.glVertex3f(half, half, -half);
        gl.glVertex3f(half, half, half);
        gl.glVertex3f(half, -half, half);
        gl.glVertex3f(half, -half, -half);

        // Left Face (-X)
        gl.glNormal3f(-1.0f, 0.0f, 0.0f);
        gl.glVertex3f(-half, half, half);
        gl.glVertex3f(-half, half, -half);
        gl.glVertex3f(-half, -half, -half);
        gl.glVertex3f(-half, -half, half);

        gl.glEnd();
    }

    /**
     * Draws a sphere using OpenGL's GLU library.
     *
     * @param gl     The GL2 object.
     * @param radius Radius of the sphere.
     * @param latDiv Number of latitude subdivisions.
     * @param lonDiv Number of longitude subdivisions.
     */
    // CHANGE: Made private to public to allow JOGLCadCanvas to call it directly if
    // desired.
    // However, the preferred method is to call drawCurrentShape.
    public static void drawSphere(GL2 gl, float radius, int latDiv, int lonDiv) {
        GLU glu = new GLU(); // GLU instance (can be reused if stored as a static member)
        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL); // Draw as filled polygons
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH); // Generate smooth normals for lighting
        glu.gluQuadricOrientation(quadric, GLU.GLU_OUTSIDE); // Normals point outwards
        glu.gluSphere(quadric, radius, latDiv, lonDiv); // Draw the sphere
        glu.gluDeleteQuadric(quadric); // Clean up the quadric object
    }

    /**
     * Draws a loaded STL model using OpenGL. It iterates through the
     * `loadedStlTriangles` list and renders each triangle with its associated
     * normal.
     * This uses immediate mode (`glBegin`/`glEnd`), which is simple but less
     * performant
     * for very large models compared to Vertex Buffer Objects (VBOs).
     *
     * @param gl The GL2 object.
     */
    // CHANGE: Made private to public to allow JOGLCadCanvas to call it directly if
    // desired.
    // However, the preferred method is to call drawCurrentShape.
    public static void drawLoadedStl(GL2 gl) {
        System.out.println("drawLoadedStl called. Triangle count: " + loadedStlTriangles.size()); // Debug
        if (loadedStlTriangles.isEmpty()) {
            System.out.println("No triangles to draw - returning early"); // Debug
            return; // Nothing to draw
        }

        gl.glBegin(GL2.GL_TRIANGLES);
        for (float[] triData : loadedStlTriangles) {
            // Each triData array contains: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x,
            // v3y, v3z]
            // Set the normal for the entire triangle
            gl.glNormal3f(triData[0], triData[1], triData[2]);

            // Define the three vertices of the triangle
            gl.glVertex3f(triData[3], triData[4], triData[5]); // Vertex 1
            gl.glVertex3f(triData[6], triData[7], triData[8]); // Vertex 2
            gl.glVertex3f(triData[9], triData[10], triData[11]); // Vertex 3
        }
        gl.glEnd();
        System.out.println("Finished drawing " + loadedStlTriangles.size() + " triangles"); // Debug
    }

    /**
     * Draws extruded geometry using OpenGL. Similar to drawLoadedStl but for
     * extruded shapes.
     * This method renders triangles from the extrudedTriangles list.
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    // public static void drawExtruded(GL2 gl) {
    // System.out.println("drawExtruded called. Triangle count: " +
    // extrudedTriangles.size()); // Debug
    // if (extrudedTriangles.isEmpty()) {
    // System.out.println("No extruded triangles to draw - returning early"); //
    // Debug
    // return; // Nothing to draw
    // }

    // gl.glBegin(GL2.GL_TRIANGLES);
    // for (float[] triData : extrudedTriangles) {
    // // Each triData array contains: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z,
    // v3x, v3y, v3z]
    // // Set the normal for the entire triangle
    // gl.glNormal3f(triData[0], triData[1], triData[2]);

    // // Define the three vertices of the triangle
    // gl.glVertex3f(triData[3], triData[4], triData[5]); // Vertex 1
    // gl.glVertex3f(triData[6], triData[7], triData[8]); // Vertex 2
    // gl.glVertex3f(triData[9], triData[10], triData[11]); // Vertex 3
    // }
    // gl.glEnd();
    // System.out.println("Finished drawing " + extrudedTriangles.size() + "
    // extruded triangles"); // Debug
    // }
    /**
     * Calculates the Center of Gravity (Volumetric Centroid) of the loaded 3D
     * model.
     * Uses the signed volume of tetrahedrons method.
     * 
     * @return float array [x, y, z] representing the CG coordinates.
     */
    public static float[] calculateCentroid() {
        double totalVolume = 0;
        double momentX = 0;
        double momentY = 0;
        double momentZ = 0;

        List<float[]> triangles = new ArrayList<>();
        // Combine all available geometry
        if (!loadedStlTriangles.isEmpty())
            triangles.addAll(loadedStlTriangles);
        if (!extrudedTriangles.isEmpty())
            triangles.addAll(extrudedTriangles);

        for (float[] tri : triangles) {
            // tri format: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
            float x1 = tri[3], y1 = tri[4], z1 = tri[5];
            float x2 = tri[6], y2 = tri[7], z2 = tri[8];
            float x3 = tri[9], y3 = tri[10], z3 = tri[11];

            // Signed volume of tetrahedron from origin (0,0,0)
            double vTet = (x1 * (y2 * z3 - z2 * y3) +
                    x2 * (y3 * z1 - z3 * y1) +
                    x3 * (y1 * z2 - z1 * y2)) / 6.0;

            // Centroid of tetrahedron
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

    /**
     * Revolves a 2D sketch around the Y-axis to create a 3D mesh.
     * 
     * @param sketch       The sketch containing the profile to revolve.
     * @param angleDegrees Angle to revolve (e.g., 360).
     * @param steps        Number of steps for the revolution.
     */
    public static void revolve(Sketch sketch, float angleDegrees, int steps) {
        extrudedTriangles.clear();

        List<Sketch.Polygon> polygons = sketch.getPolygons();
        if (polygons.isEmpty()) {
            System.out.println("No polygons to revolve.");
            return;
        }

        // For simplicity, revolve the first polygon
        Sketch.Polygon poly = polygons.get(0);
        List<Sketch.PointEntity> points = poly.getSketchPoints();
        int nPoints = points.size();

        float angleRad = (float) Math.toRadians(angleDegrees);
        float stepAngle = angleRad / steps;

        for (int i = 0; i < steps; i++) {
            float theta1 = i * stepAngle;
            float theta2 = (i + 1) * stepAngle;

            for (int j = 0; j < nPoints; j++) {
                Sketch.PointEntity p1 = points.get(j);
                Sketch.PointEntity p2 = points.get((j + 1) % nPoints); // Close loop

                // Rotate p1 and p2 by theta1
                float[] v1 = rotateY(p1, theta1);
                float[] v2 = rotateY(p2, theta1);
                // Rotate p1 and p2 by theta2
                float[] v3 = rotateY(p2, theta2);
                float[] v4 = rotateY(p1, theta2);

                // Create 2 triangles for the quad (v1, v2, v3, v4)
                addTriangleToExtruded(v1, v2, v3);
                addTriangleToExtruded(v1, v3, v4);
            }
        }
        currShape = Shape.EXTRUDED; // Reuse EXTRUDED for revolve results
        System.out.println("Revolved sketch. Generated " + extrudedTriangles.size() + " triangles.");
    }

    private static float[] rotateY(Sketch.PointEntity p, float theta) {
        float x = p.getX();
        float y = p.getY();
        // Rotation around Y axis: x' = x*cos(t) + z*sin(t), y' = y, z' = -x*sin(t) +
        // z*cos(t)
        // Assuming sketch is on Z=0 plane initially
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
        // Calculate normal
        float[] normal = calculateNormal(v1[0], v1[1], v1[2], v2[0], v2[1], v2[2], v3[0], v3[1], v3[2]);
        float[] tri = new float[12];
        System.arraycopy(normal, 0, tri, 0, 3);
        System.arraycopy(v1, 0, tri, 3, 3);
        System.arraycopy(v2, 0, tri, 6, 3);
        System.arraycopy(v3, 0, tri, 9, 3);
        extrudedTriangles.add(tri);
    }

    /**
     * Lofts between the first two polygons in the sketch over a specified height.
     * 
     * @param sketch The sketch containing profiles.
     * @param height The height difference between the two profiles.
     */
    public static void loft(Sketch sketch, float height) {
        extrudedTriangles.clear();

        List<Sketch.Polygon> polygons = sketch.getPolygons();
        if (polygons.size() < 2) {
            System.out.println("Need at least 2 polygons to loft.");
            return;
        }

        List<Sketch.PointEntity> bottom = polygons.get(0).getSketchPoints();
        List<Sketch.PointEntity> top = polygons.get(1).getSketchPoints();

        int n = Math.min(bottom.size(), top.size());

        for (int i = 0; i < n; i++) {
            Sketch.PointEntity b1 = bottom.get(i);
            Sketch.PointEntity b2 = bottom.get((i + 1) % n);
            Sketch.PointEntity t1 = top.get(i);
            Sketch.PointEntity t2 = top.get((i + 1) % n);

            // Wall Quad: b1, b2, t2, t1 (assuming CCW winding)
            // Positions: b are at Z=0, t are at Z=height

            float[] vb1 = { b1.getX(), b1.getY(), 0 };
            float[] vb2 = { b2.getX(), b2.getY(), 0 };
            float[] vt1 = { t1.getX(), t1.getY(), height };
            float[] vt2 = { t2.getX(), t2.getY(), height };

            addTriangleToExtruded(vb1, vb2, vt1);
            addTriangleToExtruded(vb2, vt2, vt1);
        }

        currShape = Shape.EXTRUDED; // Reuse EXTRUDED for loft results
        System.out.println("Loft created. Generated " + extrudedTriangles.size() + " triangles.");
    }
    /**
     * Calculates the total surface area of the current model.
     * 
     * @return Surface area in square units.
     */
    public static float calculateSurfaceArea() {
        List<float[]> trianglesToCheck = getActiveTriangles();
        if (trianglesToCheck == null || trianglesToCheck.isEmpty()) {
            return 0.0f;
        }

        float totalArea = 0.0f;
        for (float[] t : trianglesToCheck) {
            // Vertices (indices 3-11)
            float x1 = t[3], y1 = t[4], z1 = t[5];
            float x2 = t[6], y2 = t[7], z2 = t[8];
            float x3 = t[9], y3 = t[10], z3 = t[11];

            // Edge vectors
            float abx = x2 - x1, aby = y2 - y1, abz = z2 - z1;
            float acx = x3 - x1, acy = y3 - y1, acz = z3 - z1;

            // Cross product magnitudes
            float cx = aby * acz - abz * acy;
            float cy = abz * acx - abx * acz;
            float cz = abx * acy - aby * acx;

            totalArea += 0.5f * (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        }
        return totalArea;
    }

    /**
     * Calculates the volume of the current model using the divergence theorem.
     * Assumes a closed, watertight mesh.
     * 
     * @return Volume in cubic units.
     */
    public static float calculateVolume() {
        List<float[]> trianglesToCheck = getActiveTriangles();
        if (trianglesToCheck == null || trianglesToCheck.isEmpty()) {
            return 0.0f;
        }

        float totalVolume = 0.0f;
        for (float[] t : trianglesToCheck) {
            // Vertices
            float x1 = t[3], y1 = t[4], z1 = t[5];
            float x2 = t[6], y2 = t[7], z2 = t[8];
            float x3 = t[9], y3 = t[10], z3 = t[11];

            // Signed volume of tetrahedron formed by origin and triangle
            // V = (p1 . (p2 x p3)) / 6
            // Scalar triple product strategy
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

    /**
     * Helper to get the currently active list of triangles.
     */
    private static List<float[]> getActiveTriangles() {
        if (currShape == Shape.STL_LOADED) {
            return loadedStlTriangles;
        } else if (currShape == Shape.EXTRUDED) {
             return extrudedTriangles;
        } else if (currShape == Shape.CUBE || currShape == Shape.SPHERE) {
             return loadedStlTriangles; 
        }
        return new ArrayList<>();
    }

    // --- 3D Selection / Ray Picking ---

    /**
     * Casts a ray into the scene and finds the closest intersected triangle.
     * @param rayOrigin Origin of the ray (camera position).
     * @param rayDir Normalized direction of the ray.
     * @return The data of the closest triangle, or null if no hit.
     */
    public static float[] pickFace(float[] rayOrigin, float[] rayDir) {
        List<float[]> triangles = getActiveTriangles();
        if (triangles == null || triangles.isEmpty()) return null;

        float minDst = Float.MAX_VALUE;
        float[] closestTri = null;

        for (float[] tri : triangles) {
            // tri structure: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
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

    /**
     * Möller–Trumbore ray-triangle intersection algorithm.
     * @param rayOrigin Ray origin.
     * @param rayVector Ray direction.
     * @param v0 Triangle vertex 0.
     * @param v1 Triangle vertex 1.
     * @param v2 Triangle vertex 2.
     * @return Distance to intersection (t), or null if no intersection.
     */
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

        // h = rayVector CROSS edge2
        h[0] = rayVector[1] * edge2[2] - rayVector[2] * edge2[1];
        h[1] = rayVector[2] * edge2[0] - rayVector[0] * edge2[2];
        h[2] = rayVector[0] * edge2[1] - rayVector[1] * edge2[0];

        a = edge1[0] * h[0] + edge1[1] * h[1] + edge1[2] * h[2]; // DOT product

        if (a > -EPSILON && a < EPSILON)
            return null; // This ray is parallel to this triangle.

        f = 1.0f / a;
        s[0] = rayOrigin[0] - v0[0];
        s[1] = rayOrigin[1] - v0[1];
        s[2] = rayOrigin[2] - v0[2];

        u = f * (s[0] * h[0] + s[1] * h[1] + s[2] * h[2]);

        if (u < 0.0 || u > 1.0)
            return null;

        // q = s CROSS edge1
        q[0] = s[1] * edge1[2] - s[2] * edge1[1];
        q[1] = s[2] * edge1[0] - s[0] * edge1[2];
        q[2] = s[0] * edge1[1] - s[1] * edge1[0];

        v = f * (rayVector[0] * q[0] + rayVector[1] * q[1] + rayVector[2] * q[2]);

        if (v < 0.0 || u + v > 1.0)
            return null;

        // At this stage we can compute t to find out where the intersection point is on the line.
        float t = f * (edge2[0] * q[0] + edge2[1] * q[1] + edge2[2] * q[2]);

        if (t > EPSILON) // ray intersection
        {
            return t;
        } else // This means that there is a line intersection but not a ray intersection.
        {
            return null;
        }
    }

    /**
     * Calculates the area of a triangle in 3D.
     */
    public static float calculateTriangleArea(float[] tri) {
        // v1 = (tri[3], tri[4], tri[5])
        // v2 = (tri[6], tri[7], tri[8])
        // v3 = (tri[9], tri[10], tri[11])
        float ax = tri[6] - tri[3];
        float ay = tri[7] - tri[4];
        float az = tri[8] - tri[5];
        
        float bx = tri[9] - tri[3];
        float by = tri[10] - tri[4];
        float bz = tri[11] - tri[5];
        
        // Area = 0.5 * |A x B|
        float cx = ay*bz - az*by;
        float cy = az*bx - ax*bz;
        float cz = ax*by - ay*bx;
        
        return 0.5f * (float)Math.sqrt(cx*cx + cy*cy + cz*cz);
    }
}
