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
