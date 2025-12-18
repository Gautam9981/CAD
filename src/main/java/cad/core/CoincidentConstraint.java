package cad.core;


public class CoincidentConstraint extends Constraint {
    private Object o1;
    private Object o2;

    public CoincidentConstraint(cad.core.Point p1, cad.core.Point p2) {
        super(ConstraintType.COINCIDENT);
        this.o1 = p1;
        this.o2 = p2;
    }

    public CoincidentConstraint(cad.core.Sketch.Entity e1, cad.core.Sketch.Entity e2) {
        super(ConstraintType.COINCIDENT);
        this.o1 = e1;
        this.o2 = e2;
    }

    // Helper to extract a Point from an object if possible
    private cad.core.Point asPoint(Object o) {
        if (o instanceof cad.core.Point) {
            return (cad.core.Point) o;
        } else if (o instanceof cad.core.Sketch.PointEntity) {
            return ((cad.core.Sketch.PointEntity) o).getPoint();
        }
        return null;
    }

    @Override
    public double getError() {
        cad.core.Point p1 = asPoint(o1);
        cad.core.Point p2 = asPoint(o2);

        // Case 1: Point-Point
        if (p1 != null && p2 != null) {
            double dx = p1.x - p2.x;
            double dy = p1.y - p2.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        // Case 2: Point-Line (or Line-Point)
        if (p1 != null && o2 instanceof cad.core.Sketch.Line) {
            return getPointLineError(p1, (cad.core.Sketch.Line) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Line) {
            return getPointLineError(asPoint(o2), (cad.core.Sketch.Line) o1);
        }

        // Case 3: Point-Circle (or Circle-Point)
        if (p1 != null && o2 instanceof cad.core.Sketch.Circle) {
            return getPointCircleError(p1, (cad.core.Sketch.Circle) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Circle) {
            return getPointCircleError(asPoint(o2), (cad.core.Sketch.Circle) o1);
        }

        // Case 4: Line-Line (Collinear)
        if (o1 instanceof cad.core.Sketch.Line && o2 instanceof cad.core.Sketch.Line) {
            cad.core.Sketch.Line l1 = (cad.core.Sketch.Line) o1;
            cad.core.Sketch.Line l2 = (cad.core.Sketch.Line) o2;
            // Check distance of l1 endpoints to l2
            double e1 = getPointLineError(l1.getStartPoint(), l2);
            double e2 = getPointLineError(l1.getEndPoint(), l2);
            return e1 + e2;
        }

        // Case 5: Circle-Circle (Concentric/Equal)
        if (o1 instanceof cad.core.Sketch.Circle && o2 instanceof cad.core.Sketch.Circle) {
            cad.core.Sketch.Circle c1 = (cad.core.Sketch.Circle) o1;
            cad.core.Sketch.Circle c2 = (cad.core.Sketch.Circle) o2;
            double dx = c1.getX() - c2.getX();
            double dy = c1.getY() - c2.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double rDiff = Math.abs(c1.getRadius() - c2.getRadius());
            return dist + rDiff;
        }

        return 0; // Unknown or incompatible types
    }

    private double getPointLineError(cad.core.Point p, cad.core.Sketch.Line line) {
        // Distance from point p to infinite line defined by line.start and line.end
        cad.core.Point l1 = line.getStartPoint();
        cad.core.Point l2 = line.getEndPoint();

        double A = p.x - l1.x;
        double B = p.y - l1.y;
        double C = l2.x - l1.x;
        double D = l2.y - l1.y;

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = -1;
        if (lenSq != 0) // Avoid division by 0
            param = dot / lenSq;

        double xx = l1.x + param * C;
        double yy = l1.y + param * D;

        double dx = p.x - xx;
        double dy = p.y - yy;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double getPointCircleError(cad.core.Point p, cad.core.Sketch.Circle c) {
        double dx = p.x - c.getCenterPoint().x;
        double dy = p.y - c.getCenterPoint().y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return Math.abs(dist - c.getRadius());
    }

    @Override
    public void solve() {
        cad.core.Point p1 = asPoint(o1);
        cad.core.Point p2 = asPoint(o2);

        // Case 1: Point-Point
        if (p1 != null && p2 != null) {
            double midX = (p1.x + p2.x) / 2.0;
            double midY = (p1.y + p2.y) / 2.0;
            p1.move(midX, midY);
            p2.move(midX, midY);
            return;
        }

        // Case 2: Point-Line
        if (p1 != null && o2 instanceof cad.core.Sketch.Line) {
            solvePointLine(p1, (cad.core.Sketch.Line) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Line) {
            solvePointLine(asPoint(o2), (cad.core.Sketch.Line) o1);
        }

        // Case 3: Point-Circle
        if (p1 != null && o2 instanceof cad.core.Sketch.Circle) {
            solvePointCircle(p1, (cad.core.Sketch.Circle) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Circle) {
            solvePointCircle(asPoint(o2), (cad.core.Sketch.Circle) o1);
        }

        // Case 4: Line-Line
        if (o1 instanceof cad.core.Sketch.Line && o2 instanceof cad.core.Sketch.Line) {
            cad.core.Sketch.Line l1 = (cad.core.Sketch.Line) o1;
            cad.core.Sketch.Line l2 = (cad.core.Sketch.Line) o2;
            // Move l1 endpoints towards l2
            solvePointLine(l1.getStartPoint(), l2);
            solvePointLine(l1.getEndPoint(), l2);
            // Move also l2 endpoints to l1 for symmetry?
            // Doing both might be overkill per step but more stable.
            solvePointLine(l2.getStartPoint(), l1);
            solvePointLine(l2.getEndPoint(), l1);
        }

        // Case 5: Circle-Circle
        if (o1 instanceof cad.core.Sketch.Circle && o2 instanceof cad.core.Sketch.Circle) {
            cad.core.Sketch.Circle c1 = (cad.core.Sketch.Circle) o1;
            cad.core.Sketch.Circle c2 = (cad.core.Sketch.Circle) o2;

            // Average centers
            cad.core.Point cp1 = c1.getCenterPoint();
            cad.core.Point cp2 = c2.getCenterPoint();
            double midX = (cp1.x + cp2.x) / 2.0;
            double midY = (cp1.y + cp2.y) / 2.0;
            cp1.move(midX, midY);
            cp2.move(midX, midY);

            // Average radii
            float avgR = (c1.getRadius() + c2.getRadius()) / 2.0f;
            c1.setRadius(avgR);
            c2.setRadius(avgR);
        }
    }

    private void solvePointLine(cad.core.Point p, cad.core.Sketch.Line line) {
        cad.core.Point l1 = line.getStartPoint();
        cad.core.Point l2 = line.getEndPoint();

        double C = l2.x - l1.x;
        double D = l2.y - l1.y;
        double lenSq = C * C + D * D;
        if (lenSq < 1e-9)
            return; // Degenerate line

        double A = p.x - l1.x;
        double B = p.y - l1.y;

        // Projection calculation
        double param = (A * C + B * D) / lenSq;
        double closestX = l1.x + param * C;
        double closestY = l1.y + param * D; // Point on infinite line

        // Vector from P to closest point
        double dx = closestX - p.x;
        double dy = closestY - p.y;

        // Move P halfway
        p.move(p.x + dx * 0.5, p.y + dy * 0.5);

        // Move Line halfway (translate entire line in opposite direction)
        double moveX = -dx * 0.5;
        double moveY = -dy * 0.5;
        l1.move(l1.x + moveX, l1.y + moveY);
        l2.move(l2.x + moveX, l2.y + moveY);
    }

    private void solvePointCircle(cad.core.Point p, cad.core.Sketch.Circle c) {
        cad.core.Point center = c.getCenterPoint();
        double r = c.getRadius();

        double dx = p.x - center.x;
        double dy = p.y - center.y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 1e-9)
            return; // Point at center

        // Vector form Center to P
        double ux = dx / dist;
        double uy = dy / dist;

        // Target position for P is Center + R * U
        double targetX = center.x + r * ux;
        double targetY = center.y + r * uy;

        // Error vector from P to Target
        double ex = targetX - p.x;
        double ey = targetY - p.y;

        // Move P halfway to target
        p.move(p.x + ex * 0.5, p.y + ey * 0.5);

        // Move Center halfway towards "P - R*U"
        // Or simpler: Push center in opposite direction of error
        center.move(center.x - ex * 0.5, center.y - ey * 0.5);
    }
}
