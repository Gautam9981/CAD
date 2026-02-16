package cad.core;

public class CreateSphereCommand implements Command {
    private final float radius;
    private final int latDiv;
    private final int lonDiv;
    private Geometry.State previousState;

    public CreateSphereCommand(float radius, int latDiv, int lonDiv) {
        this.radius = radius;
        this.latDiv = latDiv;
        this.lonDiv = lonDiv;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        Geometry.createSphere(radius, latDiv, lonDiv);
    }

    @Override
    public void undo() {
        if (previousState != null) {
            Geometry.restoreState(previousState);
        }
    }

    @Override
    public String getDescription() {
        return "Create Sphere (r=" + radius + ")";
    }
}
