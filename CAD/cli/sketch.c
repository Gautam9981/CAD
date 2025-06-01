#include <stdio.h>
#include <stdlib.h>
#include "sketch.h"

#define MAX_SKETCH_ENTITIES 1000

Entity sketch_entities[MAX_SKETCH_ENTITIES];
int sketch_count = 0;

static int add_sketch_point(float x, float y) {
    if (sketch_count >= MAX_SKETCH_ENTITIES) {
        printf("Sketch buffer full\n");
        return 1;
    }
    sketch_entities[sketch_count].type = point;
    sketch_entities[sketch_count].point.x = x;
    sketch_entities[sketch_count].point.y = y;
    sketch_count++;
    return 0;
}

static int add_sketch_line(float x1, float y1, float x2, float y2) {
    if (sketch_count >= MAX_SKETCH_ENTITIES) {
        printf("Sketch buffer full\n");
        return 1;
    }
    sketch_entities[sketch_count].type = line;
    sketch_entities[sketch_count].line.x1 = x1;
    sketch_entities[sketch_count].line.y1 = y1;
    sketch_entities[sketch_count].line.x2 = x2;
    sketch_entities[sketch_count].line.y2 = y2;
    sketch_count++;
    return 0;
}

static int add_sketch_circle(float x, float y, float r) {
    if (sketch_count >= MAX_SKETCH_ENTITIES) {
        printf("Sketch buffer full\n");
        return 1;
    }
    sketch_entities[sketch_count].type = circle;
    sketch_entities[sketch_count].circle.x = x;
    sketch_entities[sketch_count].circle.y = y;
    sketch_entities[sketch_count].circle.r = r;
    sketch_count++;
    return 0;
}

void clear_sketch(void) {
    sketch_count = 0;
    printf("Sketch cleared.\n");
}

Entity* get_sketch_entities(void) {
    return sketch_entities;
}

int get_sketch_count(void) {
    return sketch_count;
}

int sketch_point(int argc, char** argv) {
    if (argc != 3) {
        printf("Usage: sketch_point <x> <y>\n");
        return 1;
    }
    float x = atof(argv[1]);
    float y = atof(argv[2]);
    return add_sketch_point(x, y);
}

int sketch_line(int argc, char** argv) {
    if (argc != 5) {
        printf("Usage: sketch_line <x1> <y1> <x2> <y2>\n");
        return 1;
    }
    float x1 = atof(argv[1]);
    float y1 = atof(argv[2]);
    float x2 = atof(argv[3]);
    float y2 = atof(argv[4]);
    return add_sketch_line(x1, y1, x2, y2);
}

int sketch_circle(int argc, char** argv) {
    if (argc != 4) {
        printf("Usage: sketch_circle <x> <y> <radius>\n");
        return 1;
    }
    float x = atof(argv[1]);
    float y = atof(argv[2]);
    float r = atof(argv[3]);
    return add_sketch_circle(x, y, r);
}

int sketch_list(int argc, char** argv) {
    for (int i = 0; i < sketch_count; i++) {
        switch (sketch_entities[i].type) {
            case point:
                printf("Point at (%f, %f)\n", sketch_entities[i].point.x, sketch_entities[i].point.y);
                break;
            case line:
                printf("Line from (%f, %f) to (%f, %f)\n",
                       sketch_entities[i].line.x1, sketch_entities[i].line.y1,
                       sketch_entities[i].line.x2, sketch_entities[i].line.y2);
                break;
            case circle:
                printf("Circle at (%f, %f) with radius %f\n",
                       sketch_entities[i].circle.x, sketch_entities[i].circle.y,
                       sketch_entities[i].circle.r);
                break;
        }
    }
    return 0;
}

