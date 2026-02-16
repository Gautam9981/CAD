package cad.core;

public class CreateRevolveCommand implements Command {
    private final Sketch sketch;
    private final String axisName;
    private final float angle;
    private final int steps;
    private Geometry.State previousState;

    public CreateRevolveCommand(Sketch sketch, String axisName, float angle, int steps) {
        this.sketch = sketch;
        this.axisName = axisName;
        this.angle = angle;
        this.steps = steps;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        Geometry.revolve(sketch, axisName, angle, steps, Geometry.BooleanOp.NONE);
    }

    @Override
    public void undo() {
        if (previousState != null) {
            Geometry.restoreState(previousState);
        }
    }

    @Override
    public String getDescription() {
        return "Revolve (axis=" + axisName + ", angle=" + angle + ")";
    }
}
