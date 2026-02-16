package cad.core;

public class CreateSweepCommand implements Command {
    private final Sketch sketch;
    private final Geometry.BooleanOp op;
    private Geometry.State previousState;

    public CreateSweepCommand(Sketch sketch, Geometry.BooleanOp op) {
        this.sketch = sketch;
        this.op = op;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        try {
            Geometry.sweep(sketch, op);
        } catch (Exception e) {
            // If it fails, we might want to throw or log, but here we just print
            // In a real command pattern, we might want better error handling
            System.out.println("Sweep Error: " + e.getMessage());
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
        return "Sweep (op=" + op + ")";
    }
}
