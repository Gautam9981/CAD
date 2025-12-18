package cad.core;


public class AddConstraintCommand implements Command {
    private Sketch sketch;
    private Constraint constraint;

    public AddConstraintCommand(Sketch sketch, Constraint constraint) {
        this.sketch = sketch;
        this.constraint = constraint;
    }

    @Override
    public void execute() {
        // Note: Sketch.addConstraint also solves, but we rely on the caller 
        // to have created the constraint. Re-adding it is safe.
        // If the constraint was already added before creating the command (which is typical),
        // we might not want to add it again in execute() the first time?
        // Standard Command pattern: create command -> execute() -> pushes to stack.
        // So the constraint should NOT be added before executing this command.
        sketch.addConstraint(constraint);
    }

    @Override
    public void undo() {
        sketch.removeConstraint(constraint);
    }

    @Override
    public String getDescription() {
        return "Add Constraint (" + constraint.getType() + ")";
    }
}
