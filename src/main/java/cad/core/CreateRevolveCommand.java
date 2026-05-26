package cad.core;

public class CreateRevolveCommand implements Command {
    private final Sketch sketch;
    private final String axisName;
    private float angle;
    private final int steps;
    private Geometry.State previousState;

    public CreateRevolveCommand(Sketch sketch, String axisName, float angle, int steps) {
        this.sketch = sketch;
        this.axisName = axisName;
        this.angle = angle;
        this.steps = steps;
    }

    public float getAngle() {
        return angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    @Override
    public void execute() {
        this.previousState = Geometry.captureState();
        
        cad.math.Vector3d axisOrigin = cad.math.Vector3d.zero();
        cad.math.Vector3d axisDir = axisName.equals("X") ? cad.math.Vector3d.X_AXIS : cad.math.Vector3d.Y_AXIS;
        
        cad.features.revolve.RotationalSweepFeature feature = cad.features.revolve.RotationalSweepFeature
                .builder()
                .sketch(sketch)
                .axisOrigin(axisOrigin)
                .axisDirection(axisDir)
                .angle(Math.toRadians(angle))
                .build();
                
        try {
            cad.topology.BRepBody body = feature.generate();
            java.util.List<float[]> tris = cad.core.Geometry.convertBodyToTriangles(body);
            cad.core.Geometry.setExtrudedTriangles(tris);
        } catch (cad.topology.TopologyException e) {
            System.err.println("Revolve failed: " + e.getMessage());
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
        return "Revolve (axis=" + axisName + ", angle=" + angle + ")";
    }
}
