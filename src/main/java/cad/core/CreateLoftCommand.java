package cad.core;

public class CreateLoftCommand implements Command {
    private final Sketch sketch;
    private final Geometry.BooleanOp op;
    private Geometry.State previousState;

    public CreateLoftCommand(Sketch sketch, Geometry.BooleanOp op) {
        this.sketch = sketch;
        this.op = op;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        try {
            Geometry.loft(sketch, op);
        } catch (Exception e) {
            System.out.println("Loft Error: " + e.getMessage());
        }
    }

    @Override
    public void undo() {
        if (previousState != null) {
            Geometry.restoreState(previousState);
        }
    }

    @Override
    public String getDescription() {
        return "Loft (op=" + op + ")";
    }
}
