package cad.core;

public class RadiusConstraint extends Constraint {
    private Sketch.Circle circle;
    private double targetRadius;

    public RadiusConstraint(Sketch.Circle circle, double targetRadius) {
        super(ConstraintType.RADIUS);
        this.circle = circle;
        this.targetRadius = targetRadius;
    }

    @Override
    public double getError() {
        return Math.abs(circle.getRadius() - targetRadius);
    }

    @Override
    public void solve() {
        circle.setRadius((float) targetRadius);
    }

    public Sketch.Circle getCircle() { return circle; }
    public double getTargetRadius() { return targetRadius; }
}