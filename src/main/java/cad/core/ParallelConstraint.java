package cad.core;

import cad.core.Sketch.Line;

public class ParallelConstraint extends Constraint {
    private Line line1;
    private Line line2;

    public ParallelConstraint(Line line1, Line line2) {
        super(ConstraintType.PARALLEL);
        this.line1 = line1;
        this.line2 = line2;
    }

    @Override
    public double getError() {
        // Cross product of direction vectors
        Point p1s = line1.getStartPoint();
        Point p1e = line1.getEndPoint();
        Point p2s = line2.getStartPoint();
        Point p2e = line2.getEndPoint();

        double dx1 = p1e.x - p1s.x;
        double dy1 = p1e.y - p1s.y;
        double dx2 = p2e.x - p2s.x;
        double dy2 = p2e.y - p2s.y;

        // Normalize vectors to avoid scaling issues?
        // Or just cross product: x1*y2 - x2*y1
        return dx1 * dy2 - dx2 * dy1;
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

        double currentCross = dx1 * dy2 - dx2 * dy1;
        if (Math.abs(currentCross) < 1e-9)
            return;

        // Rotate line2 to match line1's slope (or average slope)
        // For stability, let's rotate both towards the average angle
        double angle1 = Math.atan2(dy1, dx1);
        double angle2 = Math.atan2(dy2, dx2);

        // Handle 180 degree ambiguity
        double diff = angle1 - angle2;
        while (diff <= -Math.PI)
            diff += 2 * Math.PI;
        while (diff > Math.PI)
            diff -= 2 * Math.PI;

        if (Math.abs(diff) > Math.PI / 2) {
            // Vectors are opposing, but lines are parallel.
            // We want angle2 to be angle1 + PI or angle1 - PI
            // Constraint equation dx1*dy2 - dx2*dy1 = 0 handles parallel and anti-parallel.
        }

        double avgAngle = angle1 - diff / 2.0;

        // Adjust line1
        adjustLineAngle(line1, avgAngle);
        // Adjust line2
        adjustLineAngle(line2, avgAngle);
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
