package cad.core;

import cad.core.Sketch.Line;

public class PerpendicularConstraint extends Constraint {
    private Line line1;
    private Line line2;

    public PerpendicularConstraint(Line line1, Line line2) {
        super(ConstraintType.PERPENDICULAR);
        this.line1 = line1;
        this.line2 = line2;
    }

    @Override
    public double getError() {
        // Dot product of direction vectors should be 0
        Point p1s = line1.getStartPoint();
        Point p1e = line1.getEndPoint();
        Point p2s = line2.getStartPoint();
        Point p2e = line2.getEndPoint();

        double dx1 = p1e.x - p1s.x;
        double dy1 = p1e.y - p1s.y;
        double dx2 = p2e.x - p2s.x;
        double dy2 = p2e.y - p2s.y;

        return dx1 * dx2 + dy1 * dy2;
    }

    @Override
    public void solve() {
        Point p1s = line1.getStartPoint();
        Point p1e = line1.getEndPoint();
        Point p2s = line2.getStartPoint();
        Point p2e = line2.getEndPoint();

        double dx1 = p1e.x - p1s.x;
        double dy1 = p1e.y - p1s.y;
        double dx2 = p2e.x - p2s.x;
        double dy2 = p2e.y - p2s.y;

        double dot = dx1 * dx2 + dy1 * dy2;
        if (Math.abs(dot) < 1e-9)
            return;

        // Rotate line2 to be perpendicular to line1
        // Target angle for line2 is angle1 + 90 or -90
        double angle1 = Math.atan2(dy1, dx1);
        double targetAngle = angle1 + Math.PI / 2;

        adjustLineAngle(line2, targetAngle);
    }

    private void adjustLineAngle(Line line, double targetAngle) {
        Point start = line.getStartPoint();
        Point end = line.getEndPoint();
        double cx = (start.x + end.x) / 2.0;
        double cy = (start.y + end.y) / 2.0;
        double len = Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));

        double dx = Math.cos(targetAngle) * len / 2.0;
        double dy = Math.sin(targetAngle) * len / 2.0;

        start.x = (float) (cx - dx);
        start.y = (float) (cy - dy);
        end.x = (float) (cx + dx);
        end.y = (float) (cy + dy);
    }
}
