package cad.core;

import cad.core.Sketch.Line;

public class SymmetricConstraint extends Constraint {
    private Point point1;
    private Point point2;
    private Line symmetryLine;

    public SymmetricConstraint(Point point1, Point point2, Line symmetryLine) {
        super(ConstraintType.SYMMETRIC);
        this.point1 = point1;
        this.point2 = point2;
        this.symmetryLine = symmetryLine;
    }

    @Override
    public double getError() {
        // 1. Midpoint of p1-p2 should be on symmetryLine
        // 2. Line p1-p2 should be perpendicular to symmetryLine

        double midX = (point1.x + point2.x) / 2.0;
        double midY = (point1.y + point2.y) / 2.0;

        Point sStart = symmetryLine.getStartPoint();
        Point sEnd = symmetryLine.getEndPoint();

        double A = sEnd.y - sStart.y;
        double B = sStart.x - sEnd.x;
        double C = -A * sStart.x - B * sStart.y;

        // Distance from midpoint to line
        double dist = Math.abs(A * midX + B * midY + C) / Math.sqrt(A * A + B * B);

        // Perpendicularity check: dot product of p1-p2 and symmetry line direction
        double dx = point2.x - point1.x;
        double dy = point2.y - point1.y;

        double sx = sEnd.x - sStart.x;
        double sy = sEnd.y - sStart.y;

        // Normalize
        double lenP = Math.sqrt(dx * dx + dy * dy);
        double lenS = Math.sqrt(sx * sx + sy * sy);

        double dot = 0;
        if (lenP > 1e-9 && lenS > 1e-9) {
            dot = (dx * sx + dy * sy) / (lenP * lenS);
        }

        return dist + Math.abs(dot) * 10; // Weight perpendicularity
    }

    @Override
    public void solve() {
        Point sStart = symmetryLine.getStartPoint();
        Point sEnd = symmetryLine.getEndPoint();

        // Project midpoint onto symmetry line
        double midX = (point1.x + point2.x) / 2.0;
        double midY = (point1.y + point2.y) / 2.0;

        double dx = sEnd.x - sStart.x;
        double dy = sEnd.y - sStart.y;
        double lenSq = dx * dx + dy * dy;

        if (lenSq < 1e-9)
            return;

        double u = ((midX - sStart.x) * dx + (midY - sStart.y) * dy) / lenSq;
        double projX = sStart.x + u * dx;
        double projY = sStart.y + u * dy;

        // Make p1 and p2 equidistant from projected point and perpendicular
        double p1Val = (point1.x - projX) * dy - (point1.y - projY) * dx; // vector rejection-ish
        double p2Val = (point2.x - projX) * dy - (point2.y - projY) * dx;

        // Not perfectly rigorous, let's simplify:
        // Reflect p1 across line to get target p2'
        // Reflect p2 across line to get target p1'
        // Average them?

        // Better:
        // 1. Move midpoint to projX, projY
        double shiftX = projX - midX;
        double shiftY = projY - midY;
        point1.move(point1.x + shiftX, point1.y + shiftY);
        point2.move(point2.x + shiftX, point2.y + shiftY);

        // 2. Rotate p1-p2 to be perpendicular to symmetry line
        // Current angle of p1-p2
        double angleP = Math.atan2(point2.y - point1.y, point2.x - point1.x);
        // Angle of symmetry line
        double angleS = Math.atan2(dy, dx);

        // Target angle should be angleS +/- 90
        double targetAngle = angleS + Math.PI / 2.0;
        // Verify which direction is closer
        double angleDiff = angleP - targetAngle;
        while (angleDiff <= -Math.PI)
            angleDiff += 2 * Math.PI;
        while (angleDiff > Math.PI)
            angleDiff -= 2 * Math.PI;

        if (Math.abs(angleDiff) > Math.PI / 2) {
            targetAngle = angleS - Math.PI / 2.0;
        }

        double currentLen = Math.sqrt(Math.pow(point2.x - point1.x, 2) + Math.pow(point2.y - point1.y, 2));
        double newDx = Math.cos(targetAngle) * currentLen / 2.0;
        double newDy = Math.sin(targetAngle) * currentLen / 2.0;

        point1.move(projX - newDx, projY - newDy);
        point2.move(projX + newDx, projY + newDy);
    }
}
