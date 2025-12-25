package cad.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CoincidentConstraint extends Constraint {
    private List<Object> entities;

    public CoincidentConstraint(Object... objects) {
        super(ConstraintType.COINCIDENT);
        this.entities = new ArrayList<>(Arrays.asList(objects));
    }

    // Legacy constructor compatibility if needed (though varargs covers it)
    public CoincidentConstraint(cad.core.Point p1, cad.core.Point p2) {
        super(ConstraintType.COINCIDENT);
        this.entities = new ArrayList<>();
        this.entities.add(p1);
        this.entities.add(p2);
    }

    public CoincidentConstraint(cad.core.Sketch.Entity e1, cad.core.Sketch.Entity e2) {
        super(ConstraintType.COINCIDENT);
        this.entities = new ArrayList<>();
        this.entities.add(e1);
        this.entities.add(e2);
    }

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
        if (entities.size() < 2)
            return 0.0;

        List<cad.core.Point> points = new ArrayList<>();
        for (Object o : entities) {
            cad.core.Point p = asPoint(o);
            if (p != null)
                points.add(p);
        }

        // Case 1: All Points
        if (points.size() == entities.size()) {
            if (points.isEmpty())
                return 0;
            double cx = 0, cy = 0;
            for (cad.core.Point p : points) {
                cx += p.x;
                cy += p.y;
            }
            cx /= points.size();
            cy /= points.size();

            double err = 0;
            for (cad.core.Point p : points) {
                double dx = p.x - cx;
                double dy = p.y - cy;
                err += Math.sqrt(dx * dx + dy * dy);
            }
            return err;
        }

        // Case 2: Standard 2-entity constraints (legacy support + specific types)
        if (entities.size() == 2) {
            return getErrorPair(entities.get(0), entities.get(1));
        }

        // Case 3: Multiple Points constrained to ONE curve (Line/Circle/Arc)
        Object curve = null;
        for (Object o : entities) {
            if (!(o instanceof cad.core.Point) && !(o instanceof cad.core.Sketch.PointEntity)) {
                if (curve == null)
                    curve = o;
                else
                    return 0; // Don't know how to handle multiple curves yet
            }
        }

        if (curve != null) {
            double totalErr = 0;
            for (cad.core.Point p : points) {
                if (curve instanceof cad.core.Sketch.Line)
                    totalErr += getPointLineError(p, (cad.core.Sketch.Line) curve);
                else if (curve instanceof cad.core.Sketch.Circle)
                    totalErr += getPointCircleError(p, (cad.core.Sketch.Circle) curve);
                else if (curve instanceof cad.core.Sketch.Arc)
                    totalErr += getPointArcError(p, (cad.core.Sketch.Arc) curve);
            }
            return totalErr;
        }

        return 0;
    }

    private double getErrorPair(Object o1, Object o2) {
        cad.core.Point p1 = asPoint(o1);
        cad.core.Point p2 = asPoint(o2);

        if (p1 != null && p2 != null) {
            double dx = p1.x - p2.x;
            double dy = p1.y - p2.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        if (p1 != null && o2 instanceof cad.core.Sketch.Line) {
            return getPointLineError(p1, (cad.core.Sketch.Line) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Line) {
            return getPointLineError(asPoint(o2), (cad.core.Sketch.Line) o1);
        }

        if (p1 != null && o2 instanceof cad.core.Sketch.Circle) {
            return getPointCircleError(p1, (cad.core.Sketch.Circle) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Circle) {
            return getPointCircleError(asPoint(o2), (cad.core.Sketch.Circle) o1);
        } else if (p1 != null && o2 instanceof cad.core.Sketch.Arc) {
            return getPointArcError(p1, (cad.core.Sketch.Arc) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Arc) {
            return getPointArcError(asPoint(o2), (cad.core.Sketch.Arc) o1);
        }

        // Logic for Line-Line, Circle-Circle, etc.
        if (o1 instanceof cad.core.Sketch.Line && o2 instanceof cad.core.Sketch.Line) {
            cad.core.Sketch.Line l1 = (cad.core.Sketch.Line) o1;
            cad.core.Sketch.Line l2 = (cad.core.Sketch.Line) o2;

            double e1 = getPointLineError(l1.getStartPoint(), l2);
            double e2 = getPointLineError(l1.getEndPoint(), l2);
            return e1 + e2;
        }

        if (o1 instanceof cad.core.Sketch.Circle && o2 instanceof cad.core.Sketch.Circle) {
            cad.core.Sketch.Circle c1 = (cad.core.Sketch.Circle) o1;
            cad.core.Sketch.Circle c2 = (cad.core.Sketch.Circle) o2;
            double dx = c1.getX() - c2.getX();
            double dy = c1.getY() - c2.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double rDiff = Math.abs(c1.getRadius() - c2.getRadius());
            return dist + rDiff;
        }

        if (o1 instanceof cad.core.Sketch.Arc && o2 instanceof cad.core.Sketch.Arc) {
            cad.core.Sketch.Arc a1 = (cad.core.Sketch.Arc) o1;
            cad.core.Sketch.Arc a2 = (cad.core.Sketch.Arc) o2;
            double dx = a1.getX() - a2.getX();
            double dy = a1.getY() - a2.getY();
            double dist = Math.sqrt(dx * dx + dy * dy);
            double rDiff = Math.abs(a1.getRadius() - a2.getRadius());
            return dist + rDiff;
        }

        return 0;
    }

    private double getPointLineError(cad.core.Point p, cad.core.Sketch.Line line) {
        cad.core.Point l1 = line.getStartPoint();
        cad.core.Point l2 = line.getEndPoint();

        double A = p.x - l1.x;
        double B = p.y - l1.y;
        double C = l2.x - l1.x;
        double D = l2.y - l1.y;

        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        double param = -1;
        if (lenSq != 0)
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

    private double getPointArcError(cad.core.Point p, cad.core.Sketch.Arc a) {
        cad.core.Point center = a.getCenterPoint().getPoint();
        double r = a.getRadius();

        double dx = p.x - center.x;
        double dy = p.y - center.y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        return Math.abs(dist - r);
    }

    @Override
    public void solve() {
        if (entities.size() < 2)
            return;

        List<cad.core.Point> points = new ArrayList<>();
        for (Object o : entities) {
            cad.core.Point p = asPoint(o);
            if (p != null)
                points.add(p);
        }

        // Case 1: All Points
        if (points.size() == entities.size()) {
            double cx = 0, cy = 0;
            for (cad.core.Point p : points) {
                cx += p.x;
                cy += p.y;
            }
            cx /= points.size();
            cy /= points.size();

            for (cad.core.Point p : points) {
                p.move(cx, cy);
            }
            return;
        }

        // Case 2: Pairwise (Legacy 2 items)
        if (entities.size() == 2) {
            solvePair(entities.get(0), entities.get(1));
            return;
        }

        // Case 3: N Points to 1 Curve
        Object curve = null;
        for (Object o : entities) {
            if (!(o instanceof cad.core.Point) && !(o instanceof cad.core.Sketch.PointEntity)) {
                if (curve == null)
                    curve = o;
                else
                    return;
            }
        }

        if (curve != null) {
            for (cad.core.Point p : points) {
                if (curve instanceof cad.core.Sketch.Line)
                    solvePointLine(p, (cad.core.Sketch.Line) curve);
                else if (curve instanceof cad.core.Sketch.Circle)
                    solvePointCircle(p, (cad.core.Sketch.Circle) curve);
                else if (curve instanceof cad.core.Sketch.Arc)
                    solvePointArc(p, (cad.core.Sketch.Arc) curve);
            }
        }
    }

    private void solvePair(Object o1, Object o2) {
        cad.core.Point p1 = asPoint(o1);
        cad.core.Point p2 = asPoint(o2);

        if (p1 != null && p2 != null) {
            double midX = (p1.x + p2.x) / 2.0;
            double midY = (p1.y + p2.y) / 2.0;
            p1.move(midX, midY);
            p2.move(midX, midY);
            return;
        }

        if (p1 != null && o2 instanceof cad.core.Sketch.Line) {
            solvePointLine(p1, (cad.core.Sketch.Line) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Line) {
            solvePointLine(asPoint(o2), (cad.core.Sketch.Line) o1);
        }

        if (p1 != null && o2 instanceof cad.core.Sketch.Circle) {
            solvePointCircle(p1, (cad.core.Sketch.Circle) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Circle) {
            solvePointCircle(asPoint(o2), (cad.core.Sketch.Circle) o1);
        } else if (p1 != null && o2 instanceof cad.core.Sketch.Arc) {
            solvePointArc(p1, (cad.core.Sketch.Arc) o2);
        } else if (asPoint(o2) != null && o1 instanceof cad.core.Sketch.Arc) {
            solvePointArc(asPoint(o2), (cad.core.Sketch.Arc) o1);
        }

        if (o1 instanceof cad.core.Sketch.Line && o2 instanceof cad.core.Sketch.Line) {
            cad.core.Sketch.Line l1 = (cad.core.Sketch.Line) o1;
            cad.core.Sketch.Line l2 = (cad.core.Sketch.Line) o2;

            cad.core.Point l1Start = l1.getStartPoint();
            cad.core.Point l1End = l1.getEndPoint();
            cad.core.Point l2Start = l2.getStartPoint();
            cad.core.Point l2End = l2.getEndPoint();

            double d1 = distSq(l1Start, l2Start);
            double d2 = distSq(l1Start, l2End);
            double d3 = distSq(l1End, l2Start);
            double d4 = distSq(l1End, l2End);

            double minDist = Math.min(Math.min(d1, d2), Math.min(d3, d4));

            if (minDist == d1) {
                double midX = (l1Start.x + l2Start.x) / 2.0;
                double midY = (l1Start.y + l2Start.y) / 2.0;
                l1Start.move(midX, midY);
                l2Start.move(midX, midY);
            } else if (minDist == d2) {
                double midX = (l1Start.x + l2End.x) / 2.0;
                double midY = (l1Start.y + l2End.y) / 2.0;
                l1Start.move(midX, midY);
                l2End.move(midX, midY);
            } else if (minDist == d3) {
                double midX = (l1End.x + l2Start.x) / 2.0;
                double midY = (l1End.y + l2Start.y) / 2.0;
                l1End.move(midX, midY);
                l2Start.move(midX, midY);
            } else {
                double midX = (l1End.x + l2End.x) / 2.0;
                double midY = (l1End.y + l2End.y) / 2.0;
                l1End.move(midX, midY);
                l2End.move(midX, midY);
            }
        }

        if (o1 instanceof cad.core.Sketch.Circle && o2 instanceof cad.core.Sketch.Circle) {
            cad.core.Sketch.Circle c1 = (cad.core.Sketch.Circle) o1;
            cad.core.Sketch.Circle c2 = (cad.core.Sketch.Circle) o2;

            cad.core.Point cp1 = c1.getCenterPoint();
            cad.core.Point cp2 = c2.getCenterPoint();
            double midX = (cp1.x + cp2.x) / 2.0;
            double midY = (cp1.y + cp2.y) / 2.0;
            cp1.move(midX, midY);
            cp2.move(midX, midY);

            float avgR = (c1.getRadius() + c2.getRadius()) / 2.0f;
            c1.setRadius(avgR);
            c2.setRadius(avgR);
        }

        if (o1 instanceof cad.core.Sketch.Arc && o2 instanceof cad.core.Sketch.Arc) {
            cad.core.Sketch.Arc a1 = (cad.core.Sketch.Arc) o1;
            cad.core.Sketch.Arc a2 = (cad.core.Sketch.Arc) o2;

            cad.core.Point cp1 = a1.getCenterPoint().getPoint();
            cad.core.Point cp2 = a2.getCenterPoint().getPoint();
            double midX = (cp1.x + cp2.x) / 2.0;
            double midY = (cp1.y + cp2.y) / 2.0;
            cp1.move(midX, midY);
            cp2.move(midX, midY);

            float avgR = (a1.getRadius() + a2.getRadius()) / 2.0f;
            a1.setRadius(avgR);
            a2.setRadius(avgR);
        }
    }

    private void solvePointLine(cad.core.Point p, cad.core.Sketch.Line line) {
        cad.core.Point l1 = line.getStartPoint();
        cad.core.Point l2 = line.getEndPoint();

        double C = l2.x - l1.x;
        double D = l2.y - l1.y;
        double lenSq = C * C + D * D;
        if (lenSq < 1e-9)
            return;

        double A = p.x - l1.x;
        double B = p.y - l1.y;

        double param = (A * C + B * D) / lenSq;
        double closestX = l1.x + param * C;
        double closestY = l1.y + param * D;

        double dx = closestX - p.x;
        double dy = closestY - p.y;

        p.move(p.x + dx * 0.5, p.y + dy * 0.5);

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
            return;

        double ux = dx / dist;
        double uy = dy / dist;

        double targetX = center.x + r * ux;
        double targetY = center.y + r * uy;

        double ex = targetX - p.x;
        double ey = targetY - p.y;

        p.move(p.x + ex * 0.5, p.y + ey * 0.5);

        center.move(center.x - ex * 0.5, center.y - ey * 0.5);
    }

    private void solvePointArc(cad.core.Point p, cad.core.Sketch.Arc a) {
        cad.core.Point center = a.getCenterPoint().getPoint();
        double r = a.getRadius();

        double dx = p.x - center.x;
        double dy = p.y - center.y;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < 1e-9)
            return;

        double ux = dx / dist;
        double uy = dy / dist;

        double targetX = center.x + r * ux;
        double targetY = center.y + r * uy;

        double ex = targetX - p.x;
        double ey = targetY - p.y;

        p.move(p.x + ex * 0.5, p.y + ey * 0.5);
        center.move(center.x - ex * 0.5, center.y - ey * 0.5);
    }

    private double distSq(cad.core.Point p1, cad.core.Point p2) {
        double dx = p1.x - p2.x;
        double dy = p1.y - p2.y;
        return dx * dx + dy * dy;
    }
}
