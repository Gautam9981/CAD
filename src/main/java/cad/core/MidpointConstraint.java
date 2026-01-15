package cad.core;

public class MidpointConstraint extends Constraint {
    private Point point;
    private Point startPoint;
    private Point endPoint;

    public MidpointConstraint(Point point, Point startPoint, Point endPoint) {
        super(ConstraintType.MIDPOINT);
        this.point = point;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    @Override
    public double getError() {
        double midX = (startPoint.x + endPoint.x) / 2.0;
        double midY = (startPoint.y + endPoint.y) / 2.0;
        
        double dx = point.x - midX;
        double dy = point.y - midY;
        
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void solve() {
        double midX = (startPoint.x + endPoint.x) / 2.0;
        double midY = (startPoint.y + endPoint.y) / 2.0;
        
        // Move the point to the midpoint
        point.move(midX, midY);
    }

    public Point getPoint() { return point; }
    public Point getStartPoint() { return startPoint; }
    public Point getEndPoint() { return endPoint; }
}