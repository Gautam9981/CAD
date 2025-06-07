#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "sketch.h"
#include "history.h"

Entity sketch_entities[MAX_SKETCH_ENTITIES];
int sketch_count = 0;

Entity* get_sketch_entities(void) {
    return sketch_entities;
}

int get_sketch_count(void) {
    return sketch_count;
}

void set_sketch_state(const Entity* entities, int count) {
    if (count > MAX_SKETCH_ENTITIES)
        count = MAX_SKETCH_ENTITIES;

    memcpy(sketch_entities, entities, sizeof(Entity) * count);
    sketch_count = count;
}

static int add_sketch_point(float x, float y) {
    if (sketch_count >= MAX_SKETCH_ENTITIES) {
        printf("Sketch buffer full\n");
        return 1;
    }

    undo_state();
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

    undo_state();
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

    //undo_state();
    sketch_entities[sketch_count].type = circle;
    sketch_entities[sketch_count].circle.x = x;
    sketch_entities[sketch_count].circle.y = y;
    sketch_entities[sketch_count].circle.r = r;
    sketch_count++;
    return 0;
}

void clear_sketch(void) {
    undo_state();
    sketch_count = 0;
    printf("Sketch cleared.\n");
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
        Entity e = sketch_entities[i];
        switch (e.type) {
            case point:
                printf("Point at (%.2f, %.2f)\n", e.point.x, e.point.y);
                break;
            case line:
                printf("Line from (%.2f, %.2f) to (%.2f, %.2f)\n", e.line.x1, e.line.y1, e.line.x2, e.line.y2);
                break;
            case circle:
                printf("Circle at (%.2f, %.2f) with radius %.2f\n", e.circle.x, e.circle.y, e.circle.r);
                break;
        }
    }
    return 0;
}



int load_dxf(const char* filename) {
    FILE* file = fopen(filename, "r");
    if (!file) {
        printf("Failed to open DXF file: %s\n", filename);
        return 1;
    }

    char line[256];
    int reading_entity = 0;
    char entity_type[32] = {0};
    float x = 0, y = 0, x2 = 0, y2 = 0;
    int point_read = 0, line_read = 0;

    while (fgets(line, sizeof(line), file)) {
        // Trim newline
        line[strcspn(line, "\r\n")] = 0;

        if (strcmp(line, "ENDSEC") == 0) {
            // End of entities section, stop reading
            break;
        }

        if (strcmp(line, "0") == 0) {
            // Start of new entity or section
            if (reading_entity) {
                // Process previously read entity
                if (strcmp(entity_type, "POINT") == 0) {
                    add_sketch_point(x, y);
                } else if (strcmp(entity_type, "LINE") == 0) {
                    add_sketch_line(x, y, x2, y2);
                }
            }
            // Prepare for new entity
            reading_entity = 1;
            fgets(line, sizeof(line), file);  // next line contains entity type
            line[strcspn(line, "\r\n")] = 0;
            strncpy(entity_type, line, sizeof(entity_type) - 1);

            // Reset entity data
            x = y = x2 = y2 = 0;
            point_read = line_read = 0;
            continue;
        }

        if (!reading_entity) continue;

        // DXF group codes for points and lines (simplified)
        if (strcmp(entity_type, "POINT") == 0) {
            if (strcmp(line, "10") == 0) {  // X coordinate
                fgets(line, sizeof(line), file);
                x = atof(line);
            } else if (strcmp(line, "20") == 0) {  // Y coordinate
                fgets(line, sizeof(line), file);
                y = atof(line);
            }
        } else if (strcmp(entity_type, "LINE") == 0) {
            if (strcmp(line, "10") == 0) {  // start X
                fgets(line, sizeof(line), file);
                x = atof(line);
            } else if (strcmp(line, "20") == 0) {  // start Y
                fgets(line, sizeof(line), file);
                y = atof(line);
            } else if (strcmp(line, "11") == 0) {  // end X
                fgets(line, sizeof(line), file);
                x2 = atof(line);
            } else if (strcmp(line, "21") == 0) {  // end Y
                fgets(line, sizeof(line), file);
                y2 = atof(line);
            }
        }
    }

    // Process last entity if needed
    if (reading_entity) {
        if (strcmp(entity_type, "POINT") == 0) {
            add_sketch_point(x, y);
        } else if (strcmp(entity_type, "LINE") == 0) {
            add_sketch_line(x, y, x2, y2);
        }
    }

    fclose(file);
    printf("DXF file loaded.\n");
    return 0;
}