package cad.core;


public class HorizontalConstraint extends Constraint {
    private Point p1;
    private Point p2;

    public HorizontalConstraint(Point p1, Point p2) {
        super(ConstraintType.HORIZONTAL);
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public double getError() {
        // Error is the difference in Y coordinates
        return Math.abs(p1.y - p2.y);
    }

    @Override
    public void solve() {
        // Set both Y coordinates to the average Y
        double avgY = (p1.y + p2.y) / 2.0;
        
        p1.move(p1.x, avgY);
        p2.move(p2.x, avgY);
    }
}
