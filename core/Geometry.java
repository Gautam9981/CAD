package core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Geometry class provides functionality to create basic 3D shapes,
 * extrude sketches (currently disabled), and save/load STL files.
 */
public class Geometry {

    /**
     * Supported shapes enumeration.
     */
    public enum Shape {
        NONE,
        CUBE,
        SPHERE
    }

    private static Shape currShape = Shape.NONE;       // Currently active shape
    private static float param = 0.0f;                  // Size parameter (size or radius)
    public static int cubeDivisions = 1;                // Cube subdivisions per edge
    public static int sphereLatDiv = 30;                // Sphere latitude divisions
    public static int sphereLonDiv = 30;                // Sphere longitude divisions
    private static List<float[]> extrudedTriangles = new ArrayList<>(); // Extruded geometry storage

    /**
     * Create a cube with specified size and subdivisions.
     *
     * @param size      The size of the cube's edge.
     * @param divisions Number of subdivisions per edge (1-200).
     */
    public static void createCube(float size, int divisions) {
        if (divisions < 1 || divisions > 200) {
            throw new IllegalArgumentException("Cube divisions must be between 1 and 200");
        }
        cubeDivisions = divisions;
        param = size;
        currShape = Shape.CUBE;
        System.out.printf("Cube created with size %.2f and %d subdivisions%n", size, divisions);
    }

    /**
     * Create a sphere with specified radius and subdivisions.
     *
     * @param radius    Radius of the sphere.
     * @param divisions Number of subdivisions (latitude and longitude, 3-100).
     */
    public static void createSphere(float radius, int divisions) {
        if (divisions < 3 || divisions > 100) {
            throw new IllegalArgumentException("Sphere divisions must be between 3 and 100");
        }
        sphereLatDiv = sphereLonDiv = divisions;
        param = radius;
        currShape = Shape.SPHERE;
        System.out.printf("Sphere created with radius %.2f and %d subdivisions%n", radius, divisions);
    }

    /**
     * Extrude a closed sketch to create a 3D shape.
     * Currently disabled due to incomplete/broken logic.
     *
     * @param sketch Sketch to extrude.
     * @param height Extrusion height.
     */
    public static void extrude(Sketch sketch, float height) {
        // TODO: Fix extrusion logic before enabling.
        /*
        if (!sketch.isClosedLoop()) {
            System.out.println("Sketch must be a closed loop to extrude.");
            return;
        }

        extrudedTriangles.clear();
        var entities = sketch.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            Sketch.Entity entity = entities.get(i);
            if (entity instanceof Sketch.Line line) {
                float x1 = line.x1, y1 = line.y1;
                float x2 = line.x2, y2 = line.y2;

                float z0 = 0f;
                float z1 = height;

                float[] p1 = new float[]{x1, y1, z0};
                float[] p2 = new float[]{x2, y2, z0};
                float[] p3 = new float[]{x2, y2, z1};
                float[] p4 = new float[]{x1, y1, z1};

                extrudedTriangles.addAll(List.of(p1, p2, p3));
                extrudedTriangles.addAll(List.of(p1, p3, p4));
            }
        }

        currShape = Shape.NONE;
        System.out.println("Extruded sketch stored in memory.");
        */
        System.out.println("Extrude logic is disabled (pending fix).");
    }

    /**
     * Save the currently created shape or extruded geometry to an STL file.
     *
     * @param filename Path to the output STL file.
     * @throws IOException if file writing fails.
     */
    public static void saveStl(String filename) throws IOException {
        if (currShape == Shape.NONE && extrudedTriangles.isEmpty()) {
            System.out.println("No shape created yet");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(filename))) {
            out.println("solid shape");

            if (currShape == Shape.CUBE) {
                generateCubeStl(out, param, cubeDivisions);
            } else if (currShape == Shape.SPHERE) {
                generateSphereStl(out, param, sphereLatDiv, sphereLonDiv);
            } else if (!extrudedTriangles.isEmpty()) {
                // TODO: Implement extrusion export once fixed
                System.out.println("Skipping export of extruded geometry (not implemented).");
            }

            out.println("endsolid shape");
        }

        System.out.println("Saved STL file: " + filename);
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

        for (int face = 0; face < 6; face++) {
            for (int i = 0; i < divisions; i++) {
                for (int j = 0; j < divisions; j++) {
                    float x0 = -half + i * step;
                    float x1 = x0 + step;
                    float y0 = -half + j * step;
                    float y1 = y0 + step;

                    switch (face) {
                        case 0 -> { // +X face
                            writeTriangle(out, half, x0, y0, half, x1, y0, half, x1, y1);
                            writeTriangle(out, half, x0, y0, half, x1, y1, half, x0, y1);
                        }
                        case 1 -> { // -X face
                            writeTriangle(out, -half, x0, y0, -half, x1, y1, -half, x1, y0);
                            writeTriangle(out, -half, x0, y0, -half, x0, y1, -half, x1, y1);
                        }
                        case 2 -> { // +Y face
                            writeTriangle(out, x0, half, y0, x1, half, y0, x1, half, y1);
                            writeTriangle(out, x0, half, y0, x1, half, y1, x0, half, y1);
                        }
                        case 3 -> { // -Y face
                            writeTriangle(out, x0, -half, y0, x1, -half, y1, x1, -half, y0);
                            writeTriangle(out, x0, -half, y0, x0, -half, y1, x1, -half, y1);
                        }
                        case 4 -> { // +Z face
                            writeTriangle(out, x0, y0, half, x1, y0, half, x1, y1, half);
                            writeTriangle(out, x0, y0, half, x1, y1, half, x0, y1, half);
                        }
                        case 5 -> { // -Z face
                            writeTriangle(out, x0, y0, -half, x1, y1, -half, x1, y0, -half);
                            writeTriangle(out, x0, y0, -half, x0, y1, -half, x1, y1, -half);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generate STL triangles for a sphere and write to output.
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

                if (i == 0) {
                    writeTriangle(out, v1, v2, v3);  // Top cap
                } else if (i + 1 == latDiv) {
                    writeTriangle(out, v1, v2, v4);  // Bottom cap
                } else {
                    writeTriangle(out, v1, v2, v3);
                    writeTriangle(out, v1, v3, v4);
                }
            }
        }
    }

    /**
     * Calculate Cartesian coordinates for a point on a sphere given spherical angles.
     *
     * @param r     Radius of the sphere.
     * @param theta Polar angle (latitude).
     * @param phi   Azimuthal angle (longitude).
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
     * Write a triangle facet to the STL file with normal calculation.
     *
     * @param out PrintWriter to write STL.
     * @param a   Vertex A coordinates.
     * @param b   Vertex B coordinates.
     * @param c   Vertex C coordinates.
     */
    private static void writeTriangle(PrintWriter out, float[] a, float[] b, float[] c) {
        writeTriangle(out, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]);
    }

    /**
     * Write a triangle facet to the STL file with normal calculation.
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

        // Calculate normal vector (cross product)
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

        // Write facet to STL file
        out.printf("  facet normal %f %f %f%n", nx, ny, nz);
        out.println("    outer loop");
        out.printf("      vertex %f %f %f%n", ax, ay, az);
        out.printf("      vertex %f %f %f%n", bx, by, bz);
        out.printf("      vertex %f %f %f%n", cx, cy, cz);
        out.println("    endloop");
        out.println("  endfacet");
    }

    /**
     * Load an STL file and print its vertices to the console.
     * This is a simple parser that reads vertices from facets.
     *
     * @param filename STL file to read.
     * @throws IOException if file reading fails.
     */
    public static void loadStl(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int triangleCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();

                // Detect facet start
                if (line.startsWith("facet normal")) {
                    reader.readLine();  // Skip "outer loop"

                    // Read and print 3 vertices
                    for (int i = 0; i < 3; i++) {
                        String vertexLine = reader.readLine().trim();
                        String[] vertexParts = vertexLine.split("\\s+");
                        float x = Float.parseFloat(vertexParts[1]);
                        float y = Float.parseFloat(vertexParts[2]);
                        float z = Float.parseFloat(vertexParts[3]);

                        System.out.printf("Vertex %d: (%.3f, %.3f, %.3f)%n", i + 1, x, y, z);
                    }

                    reader.readLine(); // Skip "endloop"
                    reader.readLine(); // Skip "endfacet"
                    triangleCount++;
                }
            }

            System.out.println("Finished reading STL. Triangles read: " + triangleCount);
        }
    }
}
