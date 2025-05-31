#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include "geometry.h"

static enum { None, Cube, Sphere } curr_shape = None;
static float param = 0.0f;
static int cube_divisions = 1;    // default subdivisions for cube
static int sphere_lat_div = 30;   // default latitude divisions for sphere
static int sphere_lon_div = 30;   // default longitude divisions for sphere

// Create cube with given size and optional divisions
int create_cube(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: cube <size> [divisions]\n");
        return 1;
    }
    float size = atof(argv[1]);
    int divisions = cube_divisions;
    if (argc >= 3) {
        int div = atoi(argv[2]);
        if (div < 1 || div > 100) {
            printf("Cube divisions must be between 1 and 100\n");
            return 1;
        }
        divisions = div;
        cube_divisions = divisions;
    }

    curr_shape = Cube;
    param = size;
    printf("Cube created with size %.2f and %d subdivisions\n", size, divisions);
    return 0;
}

// Create sphere with given radius and optional divisions
int create_sphere(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: sphere <radius> [divisions]\n");
        return 1;
    }
    float radius = atof(argv[1]);
    int divisions = sphere_lat_div; // using same divisions for lat and lon
    if (argc >= 3) {
        int div = atoi(argv[2]);
        if (div < 3 || div > 100) {
            printf("Sphere divisions must be between 3 and 100\n");
            return 1;
        }
        divisions = div;
        sphere_lat_div = divisions;
        sphere_lon_div = divisions;
    }

    curr_shape = Sphere;
    param = radius;
    printf("Sphere created with radius %.2f and %d subdivisions\n", radius, divisions);
    return 0;
}

// Helper: Write one triangle facet to file
static void write_triangle(FILE* f, float ax, float ay, float az,
                                    float bx, float by, float bz,
                                    float cx, float cy, float cz) {
    float ux = bx - ax;
    float uy = by - ay;
    float uz = bz - az;

    float vx = cx - ax;
    float vy = cy - ay;
    float vz = cz - az;

    float nx = uy * vz - uz * vy;
    float ny = uz * vx - ux * vz;
    float nz = ux * vy - uy * vx;

    float length = sqrtf(nx*nx + ny*ny + nz*nz);
    if (length != 0.0f) {
        nx /= length;
        ny /= length;
        nz /= length;
    }

    fprintf(f, "  facet normal %f %f %f\n", nx, ny, nz);
    fprintf(f, "    outer loop\n");
    fprintf(f, "      vertex %f %f %f\n", ax, ay, az);
    fprintf(f, "      vertex %f %f %f\n", bx, by, bz);
    fprintf(f, "      vertex %f %f %f\n", cx, cy, cz);
    fprintf(f, "    endloop\n");
    fprintf(f, "  endfacet\n");
}

static void generate_cube_stl(FILE* f, float size, int divisions) {
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
                    case 0: // +X face (x = half)
                        write_triangle(f, half, x0, y0, half, x1, y0, half, x1, y1);
                        write_triangle(f, half, x0, y0, half, x1, y1, half, x0, y1);
                        break;
                    case 1: // -X face (x = -half)
                        write_triangle(f, -half, x0, y0, -half, x1, y1, -half, x1, y0);
                        write_triangle(f, -half, x0, y0, -half, x0, y1, -half, x1, y1);
                        break;
                    case 2: // +Y face (y = half)
                        write_triangle(f, x0, half, y0, x1, half, y0, x1, half, y1);
                        write_triangle(f, x0, half, y0, x1, half, y1, x0, half, y1);
                        break;
                    case 3: // -Y face (y = -half)
                        write_triangle(f, x0, -half, y0, x1, -half, y1, x1, -half, y0);
                        write_triangle(f, x0, -half, y0, x0, -half, y1, x1, -half, y1);
                        break;
                    case 4: // +Z face (z = half)
                        write_triangle(f, x0, y0, half, x1, y0, half, x1, y1, half);
                        write_triangle(f, x0, y0, half, x1, y1, half, x0, y1, half);
                        break;
                    case 5: // -Z face (z = -half)
                        write_triangle(f, x0, y0, -half, x1, y1, -half, x1, y0, -half);
                        write_triangle(f, x0, y0, -half, x0, y1, -half, x1, y1, -half);
                        break;
                }
            }
        }
    }
}

static void generate_sphere_stl(FILE* f, float radius, int lat_div, int lon_div) {
    for (int i = 0; i < lat_div; i++) {
        float theta1 = M_PI * i / lat_div;
        float theta2 = M_PI * (i + 1) / lat_div;

        for (int j = 0; j < lon_div; j++) {
            float phi1 = 2 * M_PI * j / lon_div;
            float phi2 = 2 * M_PI * (j + 1) / lon_div;

            float v1x = radius * sinf(theta1) * cosf(phi1);
            float v1y = radius * cosf(theta1);
            float v1z = radius * sinf(theta1) * sinf(phi1);

            float v2x = radius * sinf(theta2) * cosf(phi1);
            float v2y = radius * cosf(theta2);
            float v2z = radius * sinf(theta2) * sinf(phi1);

            float v3x = radius * sinf(theta2) * cosf(phi2);
            float v3y = radius * cosf(theta2);
            float v3z = radius * sinf(theta2) * sinf(phi2);

            float v4x = radius * sinf(theta1) * cosf(phi2);
            float v4y = radius * cosf(theta1);
            float v4z = radius * sinf(theta1) * sinf(phi2);

            if (i == 0) {
                write_triangle(f, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z);
            } else if (i + 1 == lat_div) {
                write_triangle(f, v1x, v1y, v1z, v2x, v2y, v2z, v4x, v4y, v4z);
            } else {
                write_triangle(f, v1x, v1y, v1z, v2x, v2y, v2z, v3x, v3y, v3z);
                write_triangle(f, v1x, v1y, v1z, v3x, v3y, v3z, v4x, v4y, v4z);
            }
        }
    }
}

int save_stl(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: save <filename>\n");
        return 1;
    }
    const char* filename = argv[1];
    FILE* f = fopen(filename, "w");
    if (!f) {
        perror("Error opening file");
        return 1;
    }

    fprintf(f, "solid shape\n");

    if (curr_shape == Cube) {
        generate_cube_stl(f, param, cube_divisions);
    }
    else if (curr_shape == Sphere) {
        generate_sphere_stl(f, param, sphere_lat_div, sphere_lon_div);
    }
    else {
        printf("No shape created yet\n");
        fclose(f);
        return 1;
    }

    fprintf(f, "endsolid shape\n");
    fclose(f);

    printf("Saved STL file: %s\n", filename);
    return 0;
}
