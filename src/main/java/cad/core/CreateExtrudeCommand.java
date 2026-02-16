package cad.core;

public class CreateExtrudeCommand implements Command {
    private final Sketch sketch;
    private final float height;
    private final Geometry.BooleanOp op;
    private Geometry.State previousState;

    public CreateExtrudeCommand(Sketch sketch, float height) {
        this(sketch, height, Geometry.BooleanOp.NONE);
    }

    public CreateExtrudeCommand(Sketch sketch, float height, Geometry.BooleanOp op) {
        this.sketch = sketch;
        this.height = height;
        this.op = op;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        Geometry.extrude(sketch, height, op);
    }

    @Override
    public void undo() {
        if (previousState != null) {
            Geometry.restoreState(previousState);
        }
    }

    @Override
    public String getDescription() {
        return "Extrude (height=" + height + ")";
    }
}
