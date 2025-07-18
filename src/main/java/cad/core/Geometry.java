package cad.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors; // Added for stream operations in new code

// JOGL imports for rendering methods in Geometry
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

/**
 * Geometry class provides functionality to create basic 3D shapes,
 * extrude sketches (currently disabled), and save/load STL files.
 */
public class Geometry {

    /**
     * Supported shapes enumeration.
     * STL_LOADED: New enum member to represent a loaded STL model.
     */
    public enum Shape {
        NONE,
        CUBE,
        SPHERE,
        STL_LOADED // Added for loaded STL models
    }

    private static Shape currShape = Shape.NONE;       // Currently active shape
    private static float param = 0.0f;                  // Size parameter (size or radius)
    public static int cubeDivisions = 1;                // Cube subdivisions per edge
    public static int sphereLatDiv = 30;                // Sphere latitude divisions
    public static int sphereLonDiv = 30;                // Sphere longitude divisions
    private static List<float[]> extrudedTriangles = new ArrayList<>(); // Extruded geometry storage (old format)

    /**
     * Stores triangles loaded from the last STL file. Each float[] contains:
     * [normalX, normalY, normalZ, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
     * This format includes the facet normal directly with the vertices for easier rendering.
     */
    private static List<float[]> loadedStlTriangles = new ArrayList<>(); // Old format for STL

    // --- NEW ADDITIONS FOR SCALABILITY ---

    /**
     * Simple class to represent a 3D point.
     * Added for the new scalable geometry system.
     */
    public static class Point3D {
        private float x, y, z;

        public Point3D(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public float getX() { return x; }
        public float getY() { return y; }
        public float getZ() { return z; }
    }

    /**
     * Represents a 3D triangle, defined by three 3D points.
     * Added for the new scalable geometry system.
     */
    public static class Triangle3D {
        private Point3D p1, p2, p3;

        public Triangle3D(Point3D p1, Point3D p2, Point3D p3) {
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        public Point3D getP1() { return p1; }
        public Point3D getP2() { return p2; }
        public Point3D getP3() { return p3; }

        /**
         * Calculates the normal vector for the triangle.
         * The normal is calculated using the cross product of two edges (p2-p1 and p3-p1).
         * The vector is then normalized to unit length.
         * @return A float array {nx, ny, nz} representing the normal vector.
         */
        public float[] getNormal() {
            // Vector from p1 to p2
            float v1x = p2.getX() - p1.getX();
            float v1y = p2.getY() - p1.getY();
            float v1z = p2.getZ() - p1.getZ();

            // Vector from p1 to p3
            float v2x = p3.getX() - p1.getX();
            float v2y = p3.getY() - p1.getY();
            float v2z = p3.getZ() - p1.getZ();

            // Cross product (v1 x v2)
            float nx = v1y * v2z - v1z * v2y;
            float ny = v1z * v2x - v1x * v2z;
            float nz = v1x * v2y - v1y * v2x;

            // Normalize the vector
            float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (length == 0) return new float[] {0, 0, 0}; // Avoid division by zero
            return new float[] {nx / length, ny / length, nz / length};
        }
    }

    /**
     * Interface for any 3D object that can provide its geometry as a list of triangles.
     * This makes the `saveStl` and `drawCurrentShape` methods more scalable.
     * Added for the new scalable geometry system.
     */
    public interface Renderable3D {
        List<Triangle3D> getTriangles();
    }

    // Concrete implementations of Renderable3D for various shapes
    // Added for the new scalable geometry system.

    /**
     * Represents a cube model that can provide its triangles.
     */
    public static class CubeModel implements Renderable3D {
        private float size;
        private int divisions;

        public CubeModel(float size, int divisions) {
            this.size = size;
            this.divisions = divisions;
        }

        @Override
        public List<Triangle3D> getTriangles() {
            List<Triangle3D> triangles = new ArrayList<>();
            float half = size / 2.0f;
            float step = size / divisions;

            // Front face (+Z)
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float x0 = -half + i * step;
                    float x1 = x0 + step;
                    float y0 = -half + j * step;
                    float y1 = y0 + step;
                    triangles.add(new Triangle3D(new Point3D(x0, y0, half), new Point3D(x1, y0, half), new Point3D(x1, y1, half)));
                    triangles.add(new Triangle3D(new Point3D(x0, y0, half), new Point3D(x1, y1, half), new Point3D(x0, y1, half)));
                }
            }
            // Back face (-Z)
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float x0 = -half + i * step;
                    float x1 = x0 + step;
                    float y0 = -half + j * step;
                    float y1 = y0 + step;
                    triangles.add(new Triangle3D(new Point3D(x0, y0, -half), new Point3D(x0, y1, -half), new Point3D(x1, y1, -half)));
                    triangles.add(new Triangle3D(new Point3D(x0, y0, -half), new Point3D(x1, y1, -half), new Point3D(x1, y0, -half)));
                }
            }
            // Right face (+X)
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float y0 = -half + i * step;
                    float y1 = y0 + step;
                    float z0 = -half + j * step;
                    float z1 = z0 + step;
                    triangles.add(new Triangle3D(new Point3D(half, y0, z0), new Point3D(half, y1, z0), new Point3D(half, y1, z1)));
                    triangles.add(new Triangle3D(new Point3D(half, y0, z0), new Point3D(half, y1, z1), new Point3D(half, y0, z1)));
                }
            }
            // Left face (-X)
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float y0 = -half + i * step;
                    float y1 = y0 + step;
                    float z0 = -half + j * step;
                    float z1 = z0 + step;
                    triangles.add(new Triangle3D(new Point3D(-half, y0, z0), new Point3D(-half, y0, z1), new Point3D(-half, y1, z1)));
                    triangles.add(new Triangle3D(new Point3D(-half, y0, z0), new Point3D(-half, y1, z1), new Point3D(-half, y1, z0)));
                }
            }
            // Top face (+Y)
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float x0 = -half + i * step;
                    float x1 = x0 + step;
                    float z0 = -half + j * step;
                    float z1 = z0 + step;
                    triangles.add(new Triangle3D(new Point3D(x0, half, z0), new Point3D(x1, half, z0), new Point3D(x1, half, z1)));
                    triangles.add(new Triangle3D(new Point3D(x0, half, z0), new Point3D(x1, half, z1), new Point3D(x0, half, z1)));
                }
            }
            // Bottom face (-Y)
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float x0 = -half + i * step;
                    float x1 = x0 + step;
                    float z0 = -half + j * step;
                    float z1 = z0 + step;
                    triangles.add(new Triangle3D(new Point3D(x0, -half, z0), new Point3D(x0, -half, z1), new Point3D(x1, -half, z1)));
                    triangles.add(new Triangle3D(new Point3D(x0, -half, z0), new Point3D(x1, -half, z1), new Point3D(x1, -half, z0)));
                }
            }
            return triangles;
        }
    }

    /**
     * Represents a sphere model that can provide its triangles.
     */
    public static class SphereModel implements Renderable3D {
        private float radius;
        private int latDiv;
        private int lonDiv;

        public SphereModel(float radius, int latDiv, int lonDiv) {
            this.radius = radius;
            this.latDiv = latDiv;
            this.lonDiv = lonDiv;
        }

        /**
         * Calculate Cartesian coordinates for a point on a sphere given spherical angles.
         * @param r Radius of the sphere.
         * @param theta Polar angle (latitude), from 0 (north pole) to PI (south pole).
         * @param phi Azimuthal angle (longitude), from 0 to 2*PI.
         * @return Cartesian coordinates as Point3D.
         */
        private Point3D sph(float r, float theta, float phi) {
            return new Point3D(
                r * (float) Math.sin(theta) * (float) Math.cos(phi),
                r * (float) Math.cos(theta),
                r * (float) Math.sin(theta) * (float) Math.sin(phi)
            );
        }

        @Override
        public List<Triangle3D> getTriangles() {
            List<Triangle3D> triangles = new ArrayList<>();

            for (int i = 0; i < latDiv; i++) {
                float theta1 = (float) Math.PI * i / latDiv;
                float theta2 = (float) Math.PI * (i + 1) / latDiv;

                for (int j = 0; j < lonDiv; j++) {
                    float phi1 = 2 * (float) Math.PI * j / lonDiv;
                    float phi2 = 2 * (float) Math.PI * (j + 1) / lonDiv;

                    Point3D v1 = sph(radius, theta1, phi1);
                    Point3D v2 = sph(radius, theta2, phi1);
                    Point3D v3 = sph(radius, theta2, phi2);
                    Point3D v4 = sph(radius, theta1, phi2);

                    if (i == 0) { // Top cap
                        triangles.add(new Triangle3D(v1, v2, v3));
                    } else if (i + 1 == latDiv) { // Bottom cap
                        triangles.add(new Triangle3D(v1, v4, v3)); // Changed order for outward normal
                    } else { // Middle sections, form two triangles (a quad)
                        triangles.add(new Triangle3D(v1, v2, v3));
                        triangles.add(new Triangle3D(v1, v3, v4));
                    }
                }
            }
            return triangles;
        }
    }

    /**
     * Represents a 3D model generated by extruding a 2D sketch.
     */
    public static class ExtrudedModel implements Renderable3D {
        private List<Triangle3D> triangles;

        public ExtrudedModel(List<Triangle3D> triangles) {
            this.triangles = new ArrayList<>(triangles);
        }

        @Override
        public List<Triangle3D> getTriangles() {
            return triangles;
        }
    }

    /**
     * Represents a 3D model loaded from an STL file.
     */
    public static class LoadedStlModel implements Renderable3D {
        private List<Triangle3D> triangles;

        // The constructor now takes a list of Triangle3D, meaning loadStl will convert
        public LoadedStlModel(List<Triangle3D> triangles) {
            this.triangles = new ArrayList<>(triangles);
        }

        @Override
        public List<Triangle3D> getTriangles() {
            return triangles;
        }
    }

    // This will hold the currently active 3D model for saving/drawing (new scalable approach)
    private static Renderable3D activeRenderableModel = null;


    // --- ORIGINAL METHODS (kept as per instruction) ---

    /**
     * Returns the currently active shape type.
     * This is used by JOGLCadCanvas to determine what to draw.
     * @return The current Shape enum.
     */
    public static Shape getCurrentShape() {
        return currShape;
    }

    /**
     * Returns the size parameter of the current shape (e.g., cube size, sphere radius).
     * @return The size/radius parameter.
     */
    public static float getParam() {
        return param;
    }

    /**
     * Returns the triangles loaded from the last STL file.
     * Used by the viewer to render STL geometry.
     * @return List of float[] where each array represents a triangle with its normal and 3 vertices.
     * Format: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
     */
    public static List<float[]> getLoadedStlTriangles() {
        return loadedStlTriangles;
    }

    /**
     * Loads an STL file, parses its facets (normals and vertices), and stores them
     * in `loadedStlTriangles` in a format suitable for OpenGL rendering.
     * After successful loading, sets the `currShape` to `STL_LOADED`.
     * ALSO, populates `activeRenderableModel` for the new scalable system.
     *
     * @param filename STL file to read.
     * @throws IOException if file reading fails or file is malformed.
     */
    public static void loadStl(String filename) throws IOException {
        loadedStlTriangles.clear(); // Clear any previously loaded data (old format)
        List<Triangle3D> newStlTriangles = new ArrayList<>(); // For the new format
        currShape = Shape.NONE;     // Reset shape type while loading
        activeRenderableModel = null; // Reset new model

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            float[] currentNormal = new float[3]; // To store the normal for the current facet
            float[] currentVertices = new float[9]; // To store the 3 vertices for the current facet

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.startsWith("facet normal")) {
                    // Parse the normal components
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 5) {
                        currentNormal[0] = Float.parseFloat(parts[2]);
                        currentNormal[1] = Float.parseFloat(parts[3]);
                        currentNormal[2] = Float.parseFloat(parts[4]);
                    } else {
                        System.err.println("Warning: Malformed facet normal line, skipping: " + line);
                        continue; // Skip this malformed facet
                    }

                    reader.readLine(); // Skip "outer loop"

                    // Read 3 vertices for the current facet
                    boolean facetOk = true;
                    for (int i = 0; i < 3; i++) {
                        String vertexLine = reader.readLine();
                        if (vertexLine == null) {
                            System.err.println("ERROR: Unexpected EOF while reading vertices for a facet.");
                            throw new IOException("Unexpected EOF in STL file.");
                        }
                        vertexLine = vertexLine.trim();
                        String[] vertexParts = vertexLine.split("\\s+");
                        if (vertexParts.length >= 4) {
                            currentVertices[i * 3] = Float.parseFloat(vertexParts[1]);
                            currentVertices[i * 3 + 1] = Float.parseFloat(vertexParts[2]);
                            currentVertices[i * 3 + 2] = Float.parseFloat(vertexParts[3]);
                        } else {
                            System.err.println("Warning: Malformed vertex line, skipping facet: " + vertexLine);
                            facetOk = false;
                            // Consume remaining lines of this facet
                            for (int j = i; j < 3; j++) { if (reader.readLine() == null) break; }
                            reader.readLine(); // Skip "endloop"
                            reader.readLine(); // Skip "endfacet"
                            break; // Exit vertex reading loop
                        }
                    }

                    if (facetOk) {
                        // Combine normal and vertices into one array for storage (old format)
                        float[] fullTriangleData = new float[12]; // 3 normal + 9 vertices
                        System.arraycopy(currentNormal, 0, fullTriangleData, 0, 3);
                        System.arraycopy(currentVertices, 0, fullTriangleData, 3, 9);
                        loadedStlTriangles.add(fullTriangleData); // Add to old list

                        // Create Triangle3D for the new format
                        newStlTriangles.add(new Triangle3D(
                            new Point3D(currentVertices[0], currentVertices[1], currentVertices[2]),
                            new Point3D(currentVertices[3], currentVertices[4], currentVertices[5]),
                            new Point3D(currentVertices[6], currentVertices[7], currentVertices[8])
                        ));
                        reader.readLine(); // Skip "endloop"
                        reader.readLine(); // Skip "endfacet"
                    }
                }
            }
            System.out.println("Finished reading STL. Triangles loaded: " + loadedStlTriangles.size());
            currShape = Shape.STL_LOADED; // Set the current shape to STL_LOADED (old way)

            if (!newStlTriangles.isEmpty()) {
                activeRenderableModel = new LoadedStlModel(newStlTriangles); // Set new model
            }
        } catch (NumberFormatException e) {
            throw new IOException("Error parsing numeric data in STL file: " + e.getMessage(), e);
        }
    }

    /**
     * Create a cube with specified size and subdivisions.
     * Sets both old and new internal representations.
     *
     * @param size      The size of the cube's edge.
     * @param divisions Number of subdivisions per edge (1-200).
     */
    public static void createCube(float size, int divisions) {
        if (divisions < 1 || divisions > 200) {
            throw new IllegalArgumentException("Cube divisions must be between 1 and 200");
        }
        cubeDivisions = divisions; // Old parameter
        param = size;              // Old parameter
        currShape = Shape.CUBE;    // Old shape type
        activeRenderableModel = new CubeModel(size, divisions); // New model
        System.out.printf("Cube created with size %.2f and %d subdivisions%n", size, divisions);
    }

    /**
     * Create a sphere with specified radius and subdivisions.
     * Sets both old and new internal representations.
     *
     * @param radius    Radius of the sphere.
     * @param divisions Number of subdivisions (latitude and longitude, 3-100).
     */
    public static void createSphere(float radius, int divisions) {
        if (divisions < 3 || divisions > 100) {
            throw new IllegalArgumentException("Sphere divisions must be between 3 and 100");
        }
        sphereLatDiv = sphereLonDiv = divisions; // Old parameters
        param = radius;                          // Old parameter
        currShape = Shape.SPHERE;                // Old shape type
        activeRenderableModel = new SphereModel(radius, divisions, divisions); // New model
        System.out.printf("Sphere created with radius %.2f and %d subdivisions%n", radius, divisions);
    }

    /**
     * Extrude a closed sketch to create a 3D shape.
     * This method now generates and sets the `ExtrudedModel` as the current active model.
     *
     * @param sketch Sketch to extrude.
     * @param height Extrusion height.
     */
    public static void extrude(Sketch sketch, float height) {
        if (sketch == null || sketch.polygons.isEmpty()) {
            System.out.println("No polygons in sketch to extrude.");
            return;
        }

        List<Triangle3D> generatedExtrudedTriangles = new ArrayList<>();

        for (Sketch.Polygon polygon : sketch.polygons) {
            List<Sketch.Point2D> points = polygon.getPoints();
            int n = points.size();

            // Convert 2D points to 3D points at z=0 for bottom face
            List<Point3D> bottomPoints = points.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), 0))
                .collect(Collectors.toList());

            // Convert 2D points to 3D points at z=height for top face
            List<Point3D> topPoints = points.stream()
                .map(p -> new Point3D(p.getX(), p.getY(), height))
                .collect(Collectors.toList());

            // Generate side faces (quads, triangulated into two triangles each)
            for (int i = 0; i < n; i++) {
                Point3D p1 = bottomPoints.get(i);
                Point3D p2 = bottomPoints.get((i + 1) % n);
                Point3D p3 = topPoints.get((i + 1) % n);
                Point3D p4 = topPoints.get(i);

                // Triangulate the quad (p1, p2, p3, p4) into two triangles
                generatedExtrudedTriangles.add(new Triangle3D(p1, p2, p3));
                generatedExtrudedTriangles.add(new Triangle3D(p1, p3, p4));
            }

            // Generate top face (fan triangulation)
            if (topPoints.size() >= 3) {
                Point3D fanOrigin = topPoints.get(0); // Use first point as fan center
                for (int i = 1; i < topPoints.size() - 1; i++) {
                    generatedExtrudedTriangles.add(new Triangle3D(fanOrigin, topPoints.get(i), topPoints.get(i + 1)));
                }
            }

            // Generate bottom face (fan triangulation, points in reverse order for consistent normals)
            if (bottomPoints.size() >= 3) {
                Point3D fanOrigin = bottomPoints.get(0); // Use first point as fan center
                for (int i = 1; i < bottomPoints.size() - 1; i++) {
                    generatedExtrudedTriangles.add(new Triangle3D(fanOrigin, bottomPoints.get(i + 1), bottomPoints.get(i)));
                }
            }
        }
        // Set the active renderable model to the newly extruded model
        activeRenderableModel = new ExtrudedModel(generatedExtrudedTriangles);
        currShape = Shape.NONE; // Extrusion doesn't fit the simple enum, so NONE for old system
        System.out.println("Extruded sketch to create a 3D model.");
        // Note: The original 'extrudedTriangles' List<float[]> is not directly populated by this new logic,
        // as the new system uses Geometry.Triangle3D. If backward compatibility for that specific list
        // is needed, conversion would be required here. For saving/drawing, the new `activeRenderableModel` is used.
    }

    /**
     * Save the currently created shape (cube, sphere) or extruded geometry to an STL file.
     * This method prioritizes saving from `activeRenderableModel` (new system),
     * otherwise falls back to the old `currShape` logic.
     *
     * @param filename Path to the output STL file.
     * @throws IOException if file writing fails.
     */
    public static void saveStl(String filename) throws IOException {
        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("solid shape");

            if (activeRenderableModel != null && !activeRenderableModel.getTriangles().isEmpty()) {
                // Use the new scalable system if a model is active
                for (Triangle3D triangle : activeRenderableModel.getTriangles()) {
                    writeTriangleStl(out, triangle);
                }
            } else if (currShape == Shape.CUBE) {
                generateCubeStl(out, param, cubeDivisions);
            } else if (currShape == Shape.SPHERE) {
                generateSphereStl(out, param, sphereLatDiv, sphereLonDiv);
            } else if (currShape == Shape.STL_LOADED && !loadedStlTriangles.isEmpty()) {
                writeLoadedStlTriangles(out);
            } else if (!extrudedTriangles.isEmpty()) { // Check for old extruded triangles if they were ever populated
                 System.out.println("Skipping export of old-format extruded geometry (not implemented for save).");
            } else {
                System.out.println("No shape created yet or no active model to save.");
                return; // Exit if nothing to save
            }

            out.println("endsolid shape");
        }

        System.out.println("Saved STL file: " + filename);
    }

    /**
     * Generates and writes STL facets for the current `loadedStlTriangles` data.
     * This is used when saving a previously loaded STL file (old format).
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
     * Helper method to write a single Triangle3D to the STL file (for the new system).
     *
     * @param out PrintWriter to write STL data.
     * @param triangle The Triangle3D object to write.
     */
    private static void writeTriangleStl(PrintWriter out, Triangle3D triangle) {
        float[] normal = triangle.getNormal();
        out.printf("  facet normal %f %f %f%n", normal[0], normal[1], normal[2]);
        out.println("    outer loop");
        out.printf("      vertex %f %f %f%n", triangle.getP1().getX(), triangle.getP1().getY(), triangle.getP1().getZ());
        out.printf("      vertex %f %f %f%n", triangle.getP2().getX(), triangle.getP2().getY(), triangle.getP2().getZ());
        out.printf("      vertex %f %f %f%n", triangle.getP3().getX(), triangle.getP3().getY(), triangle.getP3().getZ());
        out.println("    endloop");
        out.println("  endfacet");
    }

    /**
     * Generate STL triangles for a cube and write to output (old system helper).
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
     * Generate STL triangles for a sphere and write to output (old system helper).
     *
     * @param out     PrintWriter to write STL.
     * @param radius  Sphere radius.
     * @param latDiv  Latitude subdivisions.
     * @param lonDiv  Longitude subdivisions.
     */
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

                if (i == 0) { // Top cap
                    writeTriangle(out, v1, v2, v3);
                } else if (i + 1 == latDiv) { // Bottom cap
                    writeTriangle(out, v1, v2, v4);
                } else { // Middle sections, form two triangles (a quad)
                    writeTriangle(out, v1, v2, v3);
                    writeTriangle(out, v1, v3, v4);
                }
            }
        }
    }

    /**
     * Calculate Cartesian coordinates for a point on a sphere given spherical angles (old system helper).
     *
     * @param r     Radius of the sphere.
     * @param theta Polar angle (latitude), from 0 (north pole) to PI (south pole).
     * @param phi   Azimuthal angle (longitude), from 0 to 2*PI.
     * @return Cartesian coordinates as float array [x, y, z].
     */
    private static float[] sph(float r, float theta, float phi) {
        return new float[]{
            r * (float) Math.sin(theta) * (float) Math.cos(phi),
            r * (float) Math.cos(theta),
            r * (float) Math.sin(theta) * (float) Math.sin(phi)
        };
    }

    /**
     * Write a triangle facet to the STL file, computing its normal automatically (old system helper).
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
     * Write a triangle facet to the STL file with an explicitly provided normal (old system helper).
     * This overload is useful when the normal is already known (e.g., from loaded STL data)
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
     * Write a triangle facet to the STL file, computing its normal based on vertex winding (old system helper).
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
     * Draws the currently selected 3D shape using OpenGL. This method is called by JOGLCadCanvas's display method.
     * It handles drawing of cubes, spheres, and loaded STL models.
     * It prioritizes drawing from `activeRenderableModel` (new system),
     * otherwise falls back to the old `currShape` logic.
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    public static void drawCurrentShape(GL2 gl) {
        if (activeRenderableModel != null && !activeRenderableModel.getTriangles().isEmpty()) {
            // Use the new scalable system if a model is active
            drawTrianglesFromModel(gl, activeRenderableModel.getTriangles());
        } else {
            // Fallback to old system for backward compatibility
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
                    drawLoadedStl(gl); // Call the specific STL drawing method
                    break;
                case NONE:
                default:
                    // No shape to draw
                    break;
            }
        }
    }

    /**
     * Helper to draw a list of Triangle3D objects using OpenGL (for the new system).
     * @param gl The GL2 object.
     * @param triangles The list of Triangle3D objects to draw.
     */
    private static void drawTrianglesFromModel(GL2 gl, List<Triangle3D> triangles) {
        gl.glBegin(GL2.GL_TRIANGLES);
        for (Triangle3D triangle : triangles) {
            float[] normal = triangle.getNormal();
            gl.glNormal3f(normal[0], normal[1], normal[2]);

            gl.glVertex3f(triangle.getP1().getX(), triangle.getP1().getY(), triangle.getP1().getZ());
            gl.glVertex3f(triangle.getP2().getX(), triangle.getP2().getY(), triangle.getP2().getZ());
            gl.glVertex3f(triangle.getP3().getX(), triangle.getP3().getY(), triangle.getP3().getZ());
        }
        gl.glEnd();
    }


    /**
     * Draws a cube using OpenGL. Vertices and normals are calculated on the fly (old system helper).
     * For high performance with many cubes, consider using VBOs.
     *
     * @param gl        The GL2 object.
     * @param size      Edge length of the cube.
     * @param divisions Number of subdivisions per edge (for finer detail, though simple cube just draws 6 faces).
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
     * Draws a sphere using OpenGL's GLU library (old system helper).
     *
     * @param gl        The GL2 object.
     * @param radius    Radius of the sphere.
     * @param latDiv    Number of latitude subdivisions.
     * @param lonDiv    Number of longitude subdivisions.
     */
    public static void drawSphere(GL2 gl, float radius, int latDiv, int lonDiv) {
        GLU glu = new GLU(); // GLU instance (can be reused if stored as a static member)
        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(quadric, GLU.GLU_FILL);     // Draw as filled polygons
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);     // Generate smooth normals for lighting
        glu.gluQuadricOrientation(quadric, GLU.GLU_OUTSIDE); // Normals point outwards
        glu.gluSphere(quadric, radius, latDiv, lonDiv);     // Draw the sphere
        glu.gluDeleteQuadric(quadric);                      // Clean up the quadric object
    }

    /**
     * Draws a loaded STL model using OpenGL. It iterates through the
     * `loadedStlTriangles` list and renders each triangle with its associated normal (old system helper).
     * This uses immediate mode (`glBegin`/`glEnd`), which is simple but less performant
     * for very large models compared to Vertex Buffer Objects (VBOs).
     *
     * @param gl The GL2 object.
     */
    public static void drawLoadedStl(GL2 gl) {
        if (loadedStlTriangles.isEmpty()) {
            return; // Nothing to draw
        }

        gl.glBegin(GL2.GL_TRIANGLES);
        for (float[] triData : loadedStlTriangles) {
            // Each triData array contains: [nx, ny, nz, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
            // Set the normal for the entire triangle
            gl.glNormal3f(triData[0], triData[1], triData[2]);

            // Define the three vertices of the triangle
            gl.glVertex3f(triData[3], triData[4], triData[5]);   // Vertex 1
            gl.glVertex3f(triData[6], triData[7], triData[8]);   // Vertex 2
            gl.glVertex3f(triData[9], triData[10], triData[11]); // Vertex 3
        }
        gl.glEnd();
    }
}