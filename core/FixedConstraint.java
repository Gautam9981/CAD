package cad.core;

import java.util.UUID;

/**
 * A constraint that fixes a point to a specific location in space.
 * Prevents the point from moving during solve.
 */
public class FixedConstraint extends Constraint {
    private final Point point;
    private final float targetX;
    private final float targetY;

    /**
     * Creates a fixed constraint locking the point to its current location.
     * @param point The point to fix.
     */
    public FixedConstraint(Point point) {
        super(ConstraintType.FIXED);
        this.point = point;
        this.targetX = point.x;
        this.targetY = point.y;
    }

    /**
     * Creates a fixed constraint locking the point to a specific location.
     * @param point The point to fix.
     * @param x Target X coordinate.
     * @param y Target Y coordinate.
     */
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
        // Force the point to the target location
        point.set(targetX, targetY);
    }
}
