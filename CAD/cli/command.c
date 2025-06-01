#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "commands.h"
#include "geometry.h"
#include "sketch.h"

// Globals to control subdivisions (default values)
static int cube_divisions = 20;
static int sphere_lat_divisions = 30;
static int sphere_lon_divisions = 30;

int help(int argc, char** argv) {
    printf("Available commands:\n");
    printf("  cube <size>                 - Create a cube with specified size\n");
    printf("  c <size>                    - Alias for cube\n");
    printf("  sphere <radius>             - Create a sphere with specified radius\n");
    printf("  sp <radius>                 - Alias for sphere\n");
    printf("  save <filename>             - Save current shape to an STL file\n");
    printf("  s <filename>                - Alias for save\n");
    printf("  cube_div <count>            - Set cube subdivisions (for smoothness)\n");
    printf("  sphere_div <lat> <lon>      - Set sphere latitude and longitude subdivisions\n");
    printf("  sketch_point <x> <y>        - Add a point to the sketch\n");
    printf("  sketch_line <x1> <y1> <x2> <y2> - Add a line to the sketch\n");
    printf("  sketch_circle <x> <y> <radius>  - Add a circle to the sketch\n");
    printf("  sketch_clear                - Clear all sketch entities\n");
    printf("  export_dxf <filename>       - Export the sketch to a DXF file\n");
    printf("  help (h)                   - Show this help message\n");
    printf("  version (v)                - Show software version\n");
    printf("  exit (e)                   - Exit the program\n");
    return 0;
}


int cmd_exit(int argc, char** argv) {
    printf("Exiting the CLI. Thanks for using it!\n");
    exit(0);
    return 0;
}

int version(int argc, char** argv) {
    printf("CAD, version 0.0 (Beta)\n");
    return 0;
}

int create_geometry(int argc, char** argv) {
    if (argc < 3) {
        printf("Usage: create <cube|sphere> <param>\n");
        return 1;
    }
    if (strcmp(argv[1], "cube") == 0) {
        return create_cube(argc - 1, &argv[1]);
    }
    else if (strcmp(argv[1], "sphere") == 0) {
        return create_sphere(argc - 1, &argv[1]);
    }
    else {
        printf("Unknown geometry '%s'. Supported: cube, sphere.\n", argv[1]);
        return 1;
    }
}

// New commands to set subdivisions

int set_cube_divisions(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: cube_div <count>\n");
        return 1;
    }
    int d = atoi(argv[1]);
    if (d < 1 || d > 100) {
        printf("Cube subdivisions must be between 1 and 100.\n");
        return 1;
    }
    cube_divisions = d;
    printf("Cube subdivisions set to %d\n", cube_divisions);
    return 0;
}

int set_sphere_divisions(int argc, char** argv) {
    if (argc < 3) {
        printf("Usage: sphere_div <lat_divisions> <lon_divisions>\n");
        return 1;
    }
    int lat = atoi(argv[1]);
    int lon = atoi(argv[2]);
    if (lat < 1 || lat > 100 || lon < 1 || lon > 100) {
        printf("Sphere subdivisions must be between 1 and 100 for both latitude and longitude.\n");
        return 1;
    }
    sphere_lat_divisions = lat;
    sphere_lon_divisions = lon;
    printf("Sphere subdivisions set to %d latitude, %d longitude\n", lat, lon);
    return 0;
}

// Updated save_file calls save_stl with argc/argv
int save_file(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: save <filename>\n");
        return 1;
    }
    return save_stl(argc, argv);
}

int cmd_sketch_clear(int argc, char** argv) {
    clear_sketch();
    return 0;
}

int cmd_export_dxf(int argc, char** argv) {
    if (argc < 2) {
        printf("Usage: export_dxf <filename>\n");
        return 1;
    }
    return export_sketch_to_dxf(argv[1]);
}

// Command list including new subdivision commands and aliases
Command commands[] = {
    {"cube", create_cube, "cube <size>, cube_div <divisions>"},
    {"c", create_cube, "Alias for cube"},
    {"sphere", create_sphere, "sphere <radius>, sphere_div <lat_div> <lon_div>"},
    {"sp", create_sphere, "Alias for sphere"},
    {"save", save_stl, "save <filename>"},
    {"s", save_stl, "Alias for save"},
    {"help", help, "Shows this help message"},
    {"h", help, "Alias for help"},
    {"version", version, "Shows software version"},
    {"v", version, "Alias for version"},
    {"exit", cmd_exit, "Exit the program"},
    {"e", cmd_exit, "Alias for exit"},
    
    {"sketch_point", sketch_point, "Add a point: sketch_point <x> <y>"},
    {"sketch_line", sketch_line, "Add a line: sketch_line <x1> <y1> <x2> <y2>"},
    {"sketch_circle", sketch_circle, "Add a circle: sketch_circle <x> <y> <radius>"},
    {"sketch_list", sketch_list, "List sketch entities"},
    {"sketch_clear", cmd_sketch_clear, "Clear the sketch"},
    {"export_dxf", cmd_export_dxf, "Export the sketch to a DXF file: export_dxf <filename>"}
};


int num_commands = sizeof(commands) / sizeof(commands[0]);

/* This is a list of commands that we have currently, can only create spheres and cubes */