#ifndef SKETCH_H
#define SKETCH_H

#define MAX_SKETCH_ENTITIES 1000
typedef enum { point, line, circle } TypeSketch;

typedef struct {
    TypeSketch type;
    union {
        struct { float x, y; } point;
        struct { float x1, y1, x2, y2; } line;
        struct { float x, y, r; } circle;
    };
} Entity;

typedef struct {
    Entity entities[MAX_SKETCH_ENTITIES];
    int count;
} SketchSnapshot;

Entity* get_sketch_entities(void);
int get_sketch_count(void);

int sketch_point(int argc, char** argv);
int sketch_line(int argc, char** argv);
int sketch_circle(int argc, char** argv);
int sketch_list(int argc, char** argv);
void clear_sketch(void);
int cmd_sketch_clear(int argc, char** argv);
int export_sketch_to_dxf(const char *filename);
void set_sketch_state(const Entity* entities, int count);

#endif 

/* Defines the functions and variables needed for basic sketching (line, circle, exporting to dxf, and so forth) */
