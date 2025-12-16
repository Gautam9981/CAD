package cad.core;

import cad.core.Sketch.Entity;

/**
 * Command for adding an entity to a sketch.
 * Supports undo by removing the entity.
 */
public class AddEntityCommand implements Command {
    
    private Sketch sketch;
    private Entity entity;
    private String entityType;
    
    public AddEntityCommand(Sketch sketch, Entity entity, String entityType) {
        this.sketch = sketch;
        this.entity = entity;
        this.entityType = entityType;
    }
    
    @Override
    public void execute() {
        if (sketch != null && entity != null) {
            sketch.addEntity(entity);
        }
    }
    
    @Override
    public void undo() {
        if (sketch != null && entity != null) {
            sketch.removeEntity(entity);
        }
    }
    
    @Override
    public String getDescription() {
        return "Add " + entityType;
    }
}
