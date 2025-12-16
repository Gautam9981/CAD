package cad.core;

/**
 * A constraint that forces two points to be at the same location.
 */
public class CoincidentConstraint extends Constraint {
    private Point p1;
    private Point p2;

    public CoincidentConstraint(Point p1, Point p2) {
        super(ConstraintType.COINCIDENT);
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public double getError() {
        // Error is the distance squared between the two points
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void solve() {
        // Move both points to their average position
        double midX = (p1.x + p2.x) / 2.0;
        double midY = (p1.y + p2.y) / 2.0;

        p1.move(midX, midY); // Assuming Point has a move/set method, or direct access
        p2.move(midX, midY);
    }
}
