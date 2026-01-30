package cad.core;

public class AngleConstraint extends Constraint {
    private Point vertex;
    private Point point1;
    private Point point2;
    private double targetAngle; 
    private boolean useRadians;

    public AngleConstraint(Point vertex, Point point1, Point point2, double targetAngle) {
        this(vertex, point1, point2, targetAngle, false);
    }

    public AngleConstraint(Point vertex, Point point1, Point point2, double targetAngle, boolean useRadians) {
        super(ConstraintType.ANGLE);
        this.vertex = vertex;
        this.point1 = point1;
        this.point2 = point2;
        this.targetAngle = targetAngle;
        this.useRadians = useRadians;
    }

    @Override
    public double getError() {
        double angle = calculateAngle();
        double target = useRadians ? targetAngle : Math.toRadians(targetAngle);
        
        return Math.abs(angle - target);
    }

    private double calculateAngle() {
        double v1x = point1.x - vertex.x;
        double v1y = point1.y - vertex.y;
        double v2x = point2.x - vertex.x;
        double v2y = point2.y - vertex.y;
        
        double len1 = Math.sqrt(v1x * v1x + v1y * v1y);
        double len2 = Math.sqrt(v2x * v2x + v2y * v2y);
        
        if (len1 < 1e-9 || len2 < 1e-9) return 0.0;
        
        v1x /= len1;
        v1y /= len1;
        v2x /= len2;
        v2y /= len2;
        
        double dot = v1x * v2x + v1y * v2y;
        
        dot = Math.max(-1.0, Math.min(1.0, dot));
        
        double angle = Math.acos(dot);
        
        double cross = v1x * v2y - v1y * v2x;
        if (cross < 0) {
            angle = 2 * Math.PI - angle;
        }
        
        return angle;
    }

    @Override
    public void solve() {
        double currentAngle = calculateAngle();
        double target = useRadians ? targetAngle : Math.toRadians(targetAngle);
        double error = target - currentAngle;
        
        double rotationAmount = error * 0.5; 
        
        double dx = point2.x - vertex.x;
        double dy = point2.y - vertex.y;
        
        double cosAngle = Math.cos(rotationAmount);
        double sinAngle = Math.sin(rotationAmount);
        
        double newX = vertex.x + (dx * cosAngle - dy * sinAngle);
        double newY = vertex.y + (dx * sinAngle + dy * cosAngle);
        
        point2.move(newX, newY);
    }

    public Point getVertex() { return vertex; }
    public Point getPoint1() { return point1; }
    public Point getPoint2() { return point2; }
    public double getTargetAngle() { return targetAngle; }
    public boolean isUseRadians() { return useRadians; }
}