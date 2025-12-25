package cad.core;

public class FixedConstraint extends Constraint {
    private final Point point;
    private final float targetX;
    private final float targetY;

    public FixedConstraint(Point point) {
        super(ConstraintType.FIXED);
        this.point = point;
        this.targetX = point.x;
        this.targetY = point.y;
    }

    public FixedConstraint(Point point, float x, float y) {
        super(ConstraintType.FIXED);
        this.point = point;
        this.targetX = x;
        this.targetY = y;
    }

    @Override
    public double getError() {
        double dx = point.x - targetX;
        double dy = point.y - targetY;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void solve() {
        
        point.set(targetX, targetY);
    }
}
