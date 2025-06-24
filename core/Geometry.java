package core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class Geometry {
    public enum Shape { NONE, CUBE, SPHERE }

    private static Shape currShape = Shape.NONE;
    private static float param = 0.0f;
    public static int cubeDivisions = 1;
    public static int sphereLatDiv = 30;
    public static int sphereLonDiv = 30;
    private static List<float[]> extrudedTriangles = new ArrayList<>();

    public static void createCube(float size, int divisions) {
        if (divisions < 1 || divisions > 100) {
            throw new IllegalArgumentException("Cube divisions must be between 1 and 100");
        }
        cubeDivisions = divisions;
        param = size;
        currShape = Shape.CUBE;
        System.out.printf("Cube created with size %.2f and %d subdivisions%n", size, divisions);
    }

    public static void createSphere(float radius, int divisions) {
        if (divisions < 3 || divisions > 100) {
            throw new IllegalArgumentException("Sphere divisions must be between 3 and 100");
        }
        sphereLatDiv = sphereLonDiv = divisions;
        param = radius;
        currShape = Shape.SPHERE;
        System.out.printf("Sphere created with radius %.2f and %d subdivisions%n", radius, divisions);
    }

    public static void extrude(Sketch sketch, float height) {
        if (!sketch.isClosedLoop()) {
            System.out.println("Sketch must be a closed loop to extrude.");
            return;
        }

        extrudedTriangles.clear();
        var entities = sketch.getEntities();
        for (int i = 0; i < entities.size(); i++) {
            Sketch.LineEntity line = (Sketch.LineEntity) entities.get(i);
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

        currShape = Shape.NONE;
        System.out.println("Extruded sketch stored in memory.");
    }

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
                for (int i = 0; i < extrudedTriangles.size(); i += 3) {
                    float[] a = extrudedTriangles.get(i);
                    float[] b = extrudedTriangles.get(i + 1);
                    float[] c = extrudedTriangles.get(i + 2);
                    writeTriangle(out, a, b, c);
                }
            }
            out.println("endsolid shape");
        }

        System.out.println("Saved STL file: " + filename);
    }

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
                        case 0 -> {
                            writeTriangle(out, half, x0, y0, half, x1, y0, half, x1, y1);
                            writeTriangle(out, half, x0, y0, half, x1, y1, half, x0, y1);
                        }
                        case 1 -> {
                            writeTriangle(out, -half, x0, y0, -half, x1, y1, -half, x1, y0);
                            writeTriangle(out, -half, x0, y0, -half, x0, y1, -half, x1, y1);
                        }
                        case 2 -> {
                            writeTriangle(out, x0, half, y0, x1, half, y0, x1, half, y1);
                            writeTriangle(out, x0, half, y0, x1, half, y1, x0, half, y1);
                        }
                        case 3 -> {
                            writeTriangle(out, x0, -half, y0, x1, -half, y1, x1, -half, y0);
                            writeTriangle(out, x0, -half, y0, x0, -half, y1, x1, -half, y1);
                        }
                        case 4 -> {
                            writeTriangle(out, x0, y0, half, x1, y0, half, x1, y1, half);
                            writeTriangle(out, x0, y0, half, x1, y1, half, x0, y1, half);
                        }
                        case 5 -> {
                            writeTriangle(out, x0, y0, -half, x1, y1, -half, x1, y0, -half);
                            writeTriangle(out, x0, y0, -half, x0, y1, -half, x1, y1, -half);
                        }
                    }
                }
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
        return new float[]{
            r * (float) Math.sin(theta) * (float) Math.cos(phi),
            r * (float) Math.cos(theta),
            r * (float) Math.sin(theta) * (float) Math.sin(phi)
        };
    }

    private static void writeTriangle(PrintWriter out, float[] a, float[] b, float[] c) {
        writeTriangle(out, a[0], a[1], a[2], b[0], b[1], b[2], c[0], c[1], c[2]);
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

        out.printf("  facet normal %f %f %f%n", nx, ny, nz);
        out.println("    outer loop");
        out.printf("      vertex %f %f %f%n", ax, ay, az);
        out.printf("      vertex %f %f %f%n", bx, by, bz);
        out.printf("      vertex %f %f %f%n", cx, cy, cz);
        out.println("    endloop");
        out.println("  endfacet");
    }

    public static void loadStl(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int triangleCount = 0;

            while ((line = reader.readLine()) != null) {
                line = line.trim().toLowerCase();
                if (line.startsWith("facet normal")) {
                    reader.readLine();

                    for (int i = 0; i < 3; i++) {
                        String vertexLine = reader.readLine().trim();
                        String[] vertexParts = vertexLine.split("\\s+");
                        float x = Float.parseFloat(vertexParts[1]);
                        float y = Float.parseFloat(vertexParts[2]);
                        float z = Float.parseFloat(vertexParts[3]);

                        System.out.printf("Vertex %d: (%.3f, %.3f, %.3f)%n", i + 1, x, y, z);
                    }

                    reader.readLine();
                    reader.readLine();
                    triangleCount++;
                }
            }

            System.out.println("Finished reading STL. Triangles read: " + triangleCount);
        }
    }
}
