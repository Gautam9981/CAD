package cad.core;

public class TangentConstraint extends Constraint {
    private Object entity1;
    private Object entity2;

    public TangentConstraint(Object entity1, Object entity2) {
        super(ConstraintType.TANGENT);
        this.entity1 = entity1;
        this.entity2 = entity2;
    }

    @Override
    public double getError() {
        if (entity1 instanceof Sketch.Circle && entity2 instanceof Sketch.Line) {
            return getCircleLineTangentError((Sketch.Circle) entity1, (Sketch.Line) entity2);
        } else if (entity1 instanceof Sketch.Line && entity2 instanceof Sketch.Circle) {
            return getCircleLineTangentError((Sketch.Circle) entity2, (Sketch.Line) entity1);
        } else if (entity1 instanceof Sketch.Circle && entity2 instanceof Sketch.Circle) {
            return getCircleCircleTangentError((Sketch.Circle) entity1, (Sketch.Circle) entity2);
        } else if (entity1 instanceof Sketch.Arc && entity2 instanceof Sketch.Line) {
            return getArcLineTangentError((Sketch.Arc) entity1, (Sketch.Line) entity2);
        } else if (entity1 instanceof Sketch.Line && entity2 instanceof Sketch.Arc) {
            return getArcLineTangentError((Sketch.Arc) entity2, (Sketch.Line) entity1);
        } else if (entity1 instanceof Sketch.Arc && entity2 instanceof Sketch.Arc) {
            return getArcArcTangentError((Sketch.Arc) entity1, (Sketch.Arc) entity2);
        }
        return 0.0;
    }

    private double getCircleLineTangentError(Sketch.Circle circle, Sketch.Line line) {
        Point center = new Point(circle.getX(), circle.getY());
        double radius = circle.getRadius();
        
        Point l1 = line.getStartPoint();
        Point l2 = line.getEndPoint();
        
        // Distance from circle center to line
        double A = center.x - l1.x;
        double B = center.y - l1.y;
        double C = l2.x - l1.x;
        double D = l2.y - l1.y;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        
        if (lenSq < 1e-9) return 0.0;
        
        double param = dot / lenSq;
        param = Math.max(0, Math.min(1, param));
        
        double xx = l1.x + param * C;
        double yy = l1.y + param * D;
        
        double dx = center.x - xx;
        double dy = center.y - yy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return Math.abs(distance - radius);
    }

    private double getCircleCircleTangentError(Sketch.Circle c1, Sketch.Circle c2) {
        Point center1 = new Point(c1.getX(), c1.getY());
        Point center2 = new Point(c2.getX(), c2.getY());
        
        double dx = center2.x - center1.x;
        double dy = center2.y - center1.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double r1 = c1.getRadius();
        double r2 = c2.getRadius();
        
        // For external tangency: distance = r1 + r2
        return Math.abs(distance - (r1 + r2));
    }

    private double getArcLineTangentError(Sketch.Arc arc, Sketch.Line line) {
        Point center = arc.getCenterPoint().getPoint();
        double radius = arc.getRadius();
        
        Point l1 = line.getStartPoint();
        Point l2 = line.getEndPoint();
        
        // Distance from arc center to line
        double A = center.x - l1.x;
        double B = center.y - l1.y;
        double C = l2.x - l1.x;
        double D = l2.y - l1.y;
        
        double dot = A * C + B * D;
        double lenSq = C * C + D * D;
        
        if (lenSq < 1e-9) return 0.0;
        
        double param = dot / lenSq;
        param = Math.max(0, Math.min(1, param));
        
        double xx = l1.x + param * C;
        double yy = l1.y + param * D;
        
        double dx = center.x - xx;
        double dy = center.y - yy;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        return Math.abs(distance - radius);
    }

    private double getArcArcTangentError(Sketch.Arc arc1, Sketch.Arc arc2) {
        Point center1 = arc1.getCenterPoint().getPoint();
        Point center2 = arc2.getCenterPoint().getPoint();
        
        double dx = center2.x - center1.x;
        double dy = center2.y - center1.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        double r1 = arc1.getRadius();
        double r2 = arc2.getRadius();
        
        return Math.abs(distance - (r1 + r2));
    }

    @Override
    public void solve() {
        if (entity1 instanceof Sketch.Circle && entity2 instanceof Sketch.Line) {
            solveCircleLineTangent((Sketch.Circle) entity1, (Sketch.Line) entity2);
        } else if (entity1 instanceof Sketch.Line && entity2 instanceof Sketch.Circle) {
            solveCircleLineTangent((Sketch.Circle) entity2, (Sketch.Line) entity1);
        } else if (entity1 instanceof Sketch.Circle && entity2 instanceof Sketch.Circle) {
            solveCircleCircleTangent((Sketch.Circle) entity1, (Sketch.Circle) entity2);
        }
    }

    private void solveCircleLineTangent(Sketch.Circle circle, Sketch.Line line) {
        Point center = new Point(circle.getX(), circle.getY());
        double radius = circle.getRadius();
        Point l1 = line.getStartPoint();
        Point l2 = line.getEndPoint();
        
        // Move line to be tangent to circle
        double dx = l2.x - l1.x;
        double dy = l2.y - l1.y;
        double len = Math.sqrt(dx * dx + dy * dy);
        
        if (len < 1e-9) return;
        
        double nx = -dy / len;
        double ny = dx / len;
        
        // Move line to correct distance from circle center
        double targetDistance = radius;
        double currentDistance = getCircleLineTangentError(circle, line);
        
        double correction = (targetDistance - currentDistance) * 0.5;
        l1.move(l1.x + nx * correction, l1.y + ny * correction);
        l2.move(l2.x + nx * correction, l2.y + ny * correction);
    }

    private void solveCircleCircleTangent(Sketch.Circle c1, Sketch.Circle c2) {
        Point center1 = new Point(c1.getX(), c1.getY());
        Point center2 = new Point(c2.getX(), c2.getY());
        
        double dx = center2.x - center1.x;
        double dy = center2.y - center1.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < 1e-9) return;
        
        double r1 = c1.getRadius();
        double r2 = c2.getRadius();
        double targetDistance = r1 + r2;
        
        // Move circles to achieve tangency
        double correction = (targetDistance - distance) * 0.5;
        double ux = dx / distance;
        double uy = dy / distance;
        
        center2.move(center2.x + ux * correction, center2.y + uy * correction);
    }

    public Object getEntity1() { return entity1; }
    public Object getEntity2() { return entity2; }
}