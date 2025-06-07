#ifndef HISTORY_H
#define HISTORY_H
#include "sketch.h"

void undo_state(void);
void sketch_undo(void);
void sketch_redo(void);
void clear_history(void);

#endif

/* Defines procedures needed for undo/redo functionality */ 
