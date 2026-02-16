package cad.core;

public class CreateCubeCommand implements Command {
    private final float size;
    private final int divisions;
    private Geometry.State previousState;

    public CreateCubeCommand(float size, int divisions) {
        this.size = size;
        this.divisions = divisions;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        Geometry.createCube(size, divisions);
    }

    @Override
    public void undo() {
        if (previousState != null) {
            Geometry.restoreState(previousState);
        }
    }

    @Override
    public String getDescription() {
        return "Create Cube (size=" + size + ")";
    }
}
