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
    private static List<float[]> extrudedTriangles = new ArrayList<>(); // Extruded geometry storage

    /**
     * Stores triangles loaded from the last STL file. Each float[] contains:
     * [normalX, normalY, normalZ, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z]
     * This format includes the facet normal directly with the vertices for easier rendering.
     */
    private static List<float[]> loadedStlTriangles = new ArrayList<>();

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
     *
     * @param filename STL file to read.
     * @throws IOException if file reading fails or file is malformed.
     */
    public static void loadStl(String filename) throws IOException {
        loadedStlTriangles.clear(); // Clear any previously loaded data
        currShape = Shape.NONE;     // Reset shape type while loading

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
                    for (int i = 0; i < 3; i++) {
                        String vertexLine = reader.readLine().trim();
                        String[] vertexParts = vertexLine.split("\\s+");
                        if (vertexParts.length >= 4) {
                            currentVertices[i * 3] = Float.parseFloat(vertexParts[1]);
                            currentVertices[i * 3 + 1] = Float.parseFloat(vertexParts[2]);
                            currentVertices[i * 3 + 2] = Float.parseFloat(vertexParts[3]);
                        } else {
                            System.err.println("Warning: Malformed vertex line, skipping facet: " + vertexLine);
                            // If a vertex is malformed, we can't draw this triangle, so break and skip facet
                            // More robust error handling might clear the currentNormal/currentVertices
                            break; // Exit vertex reading loop
                        }
                    }

                    // Combine normal and vertices into one array for storage
                    float[] fullTriangleData = new float[12]; // 3 normal + 9 vertices
                    System.arraycopy(currentNormal, 0, fullTriangleData, 0, 3);
                    System.arraycopy(currentVertices, 0, fullTriangleData, 3, 9);
                    loadedStlTriangles.add(fullTriangleData);

                    reader.readLine(); // Skip "endloop"
                    reader.readLine(); // Skip "endfacet"
                }
            }
            System.out.println("Finished reading STL. Triangles loaded: " + loadedStlTriangles.size());
            currShape = Shape.STL_LOADED; // Set the current shape to STL_LOADED
        } catch (NumberFormatException e) {
            throw new IOException("Error parsing numeric data in STL file: " + e.getMessage(), e);
        }
    }

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
     * Save the currently created shape (cube, sphere) or extruded geometry to an STL file.
     * This method delegates the actual triangle generation to private helper methods.
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
            } else if (currShape == Shape.STL_LOADED && !loadedStlTriangles.isEmpty()) {
                // If a loaded STL is the current shape, just write its triangles back out
                writeLoadedStlTriangles(out);
            }
            // else if (!extrudedTriangles.isEmpty()) { // For when extrusion is enabled
            //     // TODO: Implement extrusion export once fixed
            //     System.out.println("Skipping export of extruded geometry (not implemented).");
            // }

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
     * @param out     PrintWriter to write STL.
     * @param radius  Sphere radius.
     * @param latDiv  Latitude subdivisions.
     * @param lonDiv  Longitude subdivisions.
     */
    private static void generateSphereStl(PrintWriter out, float radius, int latDiv, int lonDiv) {
        // This method generates sphere geometry. For robust results, it might be better
        // to pre-calculate normals based on the sphere's surface (normalized vertex position)
        // rather than using cross product of triangle vertices if the triangulation is poor.
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
     * Calculate Cartesian coordinates for a point on a sphere given spherical angles.
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
     * Write a triangle facet to the STL file, computing its normal based on vertex winding.
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
     *
     * @param gl The GL2 object (OpenGL context) used for drawing.
     */
    // This method is already public static, which is good.
    public static void drawCurrentShape(GL2 gl) { // Renamed from drawLoadedStl to reflect general purpose
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

    /**
     * Draws a cube using OpenGL. Vertices and normals are calculated on the fly.
     * For high performance with many cubes, consider using VBOs.
     *
     * @param gl        The GL2 object.
     * @param size      Edge length of the cube.
     * @param divisions Number of subdivisions per edge (for finer detail, though simple cube just draws 6 faces).
     */
    // CHANGE: Made private to public to allow JOGLCadCanvas to call it directly if desired.
    // However, the preferred method is to call drawCurrentShape.
    // Making this public is technically a valid fix, but calling drawCurrentShape is better design.
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
     * @param gl        The GL2 object.
     * @param radius    Radius of the sphere.
     * @param latDiv    Number of latitude subdivisions.
     * @param lonDiv    Number of longitude subdivisions.
     */
    // CHANGE: Made private to public to allow JOGLCadCanvas to call it directly if desired.
    // However, the preferred method is to call drawCurrentShape.
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
     * `loadedStlTriangles` list and renders each triangle with its associated normal.
     * This uses immediate mode (`glBegin`/`glEnd`), which is simple but less performant
     * for very large models compared to Vertex Buffer Objects (VBOs).
     *
     * @param gl The GL2 object.
     */
    // CHANGE: Made private to public to allow JOGLCadCanvas to call it directly if desired.
    // However, the preferred method is to call drawCurrentShape.
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