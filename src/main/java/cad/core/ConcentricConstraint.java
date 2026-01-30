package cad.core;

public class ConcentricConstraint extends Constraint {
    private Sketch.Circle circle1;
    private Sketch.Circle circle2;

    public ConcentricConstraint(Sketch.Circle circle1, Sketch.Circle circle2) {
        super(ConstraintType.CONCENTRIC);
        this.circle1 = circle1;
        this.circle2 = circle2;
    }

    @Override
    public double getError() {
        Point center1 = new Point(circle1.getX(), circle1.getY());
        Point center2 = new Point(circle2.getX(), circle2.getY());
        
        double dx = center2.x - center1.x;
        double dy = center2.y - center1.y;
        
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public void solve() {
        Point center1 = new Point(circle1.getX(), circle1.getY());
        Point center2 = new Point(circle2.getX(), circle2.getY());
        
        double avgX = (center1.x + center2.x) / 2.0;
        double avgY = (center1.y + center2.y) / 2.0;
        
        center1.move(avgX, avgY);
        center2.move(avgX, avgY);
        
        circle1.setCenter((float) avgX, (float) avgY);
        circle2.setCenter((float) avgX, (float) avgY);
    }

    public Sketch.Circle getCircle1() { return circle1; }
    public Sketch.Circle getCircle2() { return circle2; }
}