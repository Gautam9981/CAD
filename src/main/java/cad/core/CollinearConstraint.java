package cad.core;

public class CollinearConstraint extends Constraint {
    private Point point1;
    private Point point2;
    private Point point3;

    public CollinearConstraint(Point point1, Point point2, Point point3) {
        super(ConstraintType.COLLINEAR);
        this.point1 = point1;
        this.point2 = point2;
        this.point3 = point3;
    }

    @Override
    public double getError() {
        double area = Math.abs(
                (point2.x - point1.x) * (point3.y - point1.y) -
                        (point3.x - point1.x) * (point2.y - point1.y))
                / 2.0;

        return area;
    }

    @Override
    public void solve() {
        double dx = point2.x - point1.x;
        double dy = point2.y - point1.y;

        double len = Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-9)
            return;

        double ux = dx / len;
        double uy = dy / len;

        double dx3 = point3.x - point1.x;
        double dy3 = point3.y - point1.y;
        double dot = dx3 * ux + dy3 * uy;

        double targetX = point1.x + ux * dot;
        double targetY = point1.y + uy * dot;

        point3.move(point3.x + (targetX - point3.x) * 0.5,
                point3.y + (targetY - point3.y) * 0.5);
    }

    public Point getPoint1() {
        return point1;
    }

    public Point getPoint2() {
        return point2;
    }

    public Point getPoint3() {
        return point3;
    }
}