package cad.core;

/**
 * A constraint that forces two points (usually forming a line) to share the same X coordinate.
 */
public class VerticalConstraint extends Constraint {
    private Point p1;
    private Point p2;

    public VerticalConstraint(Point p1, Point p2) {
        super(ConstraintType.VERTICAL);
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public double getError() {
        // Error is the difference in X coordinates
        return Math.abs(p1.x - p2.x);
    }

    @Override
    public void solve() {
        // Set both X coordinates to the average X
        double avgX = (p1.x + p2.x) / 2.0;
        
        p1.move(avgX, p1.y);
        p2.move(avgX, p2.y);
    }
}
