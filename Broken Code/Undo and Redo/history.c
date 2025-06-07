#include <stdio.h>
#include <string.h>
#include "sketch.h"
#include "history.h"

#define HISTORY 100

typedef struct {
    Entity entities[MAX_SKETCH_ENTITIES];
    int count;
} SketchState;

static SketchState undo_stack[HISTORY];
static int undo_top = -1;

static SketchState redo_stack[HISTORY];
static int redo_top = -1;

void undo_state(void) {
    if (undo_top < HISTORY - 1) {
        undo_top++;
        undo_stack[undo_top].count = get_sketch_count();
        memcpy(undo_stack[undo_top].entities, get_sketch_entities(),
               sizeof(Entity) * undo_stack[undo_top].count);
    } else {
        printf("Undo stack is full.\n");
    }

    // Clear redo stack when new action occurs
    redo_top = -1;
}

void sketch_undo(void) {
    if (undo_top < 0) {
        printf("Nothing to undo.\n");
        return;
    }

    if (redo_top < HISTORY - 1) {
        redo_top++;
        redo_stack[redo_top].count = get_sketch_count();
        memcpy(redo_stack[redo_top].entities, get_sketch_entities(),
               sizeof(Entity) * redo_stack[redo_top].count);
    }

    set_sketch_state(undo_stack[undo_top].entities, undo_stack[undo_top].count);
    undo_top--;

    printf("Undo successful.\n");
}

void sketch_redo(void) {
    if (redo_top < 0) {
        printf("Nothing to redo.\n");
        return;
    }

    if (undo_top < HISTORY - 1) {
        undo_top++;
        undo_stack[undo_top].count = get_sketch_count();
        memcpy(undo_stack[undo_top].entities, get_sketch_entities(),
               sizeof(Entity) * undo_stack[undo_top].count);
    }

    set_sketch_state(redo_stack[redo_top].entities, redo_stack[redo_top].count);
    redo_top--;

    printf("Redo successful.\n");
}

void clear_history(void) {
    undo_top = -1;
    redo_top = -1;
}
