package cad.geometry.curves;

import cad.math.Vector3d;

public class ArcCurve extends Curve {
    private Vector3d center;
    private Vector3d start;
    private Vector3d end;
    private double radius;
    private double startAngle;
    private double endAngle;
    private Vector3d normal;

    public ArcCurve(Vector3d center, Vector3d start, Vector3d end, Vector3d normal) {
        this.center = center;
        this.start = start;
        this.end = end;
        this.normal = normal.normalize();
        this.radius = center.distance(start);
        this.startAngle = calculateAngle(center, start, normal);
        this.endAngle = calculateAngle(center, end, normal);
    }

    public ArcCurve(Vector3d center, double radius, double startAngle, double endAngle, Vector3d normal) {
        this.center = center;
        this.radius = Math.abs(radius);
        this.startAngle = startAngle;
        this.endAngle = endAngle;
        this.normal = normal.normalize();
        
        Vector3d radialX = getRadialX();
        Vector3d radialY = normal.cross(radialX);
        
        this.start = center.plus(
            radialX.multiply(radius * Math.cos(startAngle))
                .plus(radialY.multiply(radius * Math.sin(startAngle)))
        );
        this.end = center.plus(
            radialX.multiply(radius * Math.cos(endAngle))
                .plus(radialY.multiply(radius * Math.sin(endAngle)))
        );
    }

    @Override
    public Vector3d value(double t) {
        double angle = startAngle + t * (endAngle - startAngle);
        return getPointAtAngle(angle);
    }

    @Override
    public Vector3d tangent(double t) {
        double angle = startAngle + t * (endAngle - startAngle);
        Vector3d radialX = getRadialX();
        Vector3d radialY = normal.cross(radialX);
        
        Vector3d tangent = radialX.multiply(-Math.sin(angle))
            .plus(radialY.multiply(Math.cos(angle)));
        
        if ((endAngle - startAngle) < 0) {
            tangent = tangent.negated();
        }
        
        return tangent.normalize();
    }

    @Override
    public Vector3d derivative(double t) {
        double angleRange = endAngle - startAngle;
        Vector3d tangent = tangent(t);
        return tangent.multiply(Math.abs(angleRange) * radius);
    }

    @Override
    public double startParam() {
        return 0.0;
    }

    @Override
    public double endParam() {
        return 1.0;
    }

    @Override
    public double length() {
        double angleRange = Math.abs(endAngle - startAngle);
        return radius * angleRange;
    }

    private Vector3d getPointAtAngle(double angle) {
        Vector3d radialX = getRadialX();
        Vector3d radialY = normal.cross(radialX);
        
        return center.plus(
            radialX.multiply(radius * Math.cos(angle))
                .plus(radialY.multiply(radius * Math.sin(angle)))
        );
    }

    private Vector3d getRadialX() {
        Vector3d toStart = start.minus(center);
        if (toStart.magnitude() > 1e-10) {
            return toStart.normalize();
        }
        
        Vector3d test = Math.abs(normal.dot(Vector3d.Z_AXIS)) < 0.9 ? Vector3d.Z_AXIS : Vector3d.X_AXIS;
        return normal.cross(test).normalize();
    }

    private static double calculateAngle(Vector3d center, Vector3d point, Vector3d normal) {
        Vector3d toPoint = point.minus(center);
        if (toPoint.magnitude() < 1e-10) return 0.0;
        
        Vector3d radialX = getRadialX(center, normal);
        Vector3d radialY = normal.cross(radialX);
        
        double x = toPoint.dot(radialX);
        double y = toPoint.dot(radialY);
        
        return Math.atan2(y, x);
    }

    private static Vector3d getRadialX(Vector3d center, Vector3d normal) {
        Vector3d test = Math.abs(normal.dot(Vector3d.Z_AXIS)) < 0.9 ? Vector3d.Z_AXIS : Vector3d.X_AXIS;
        return normal.cross(test).normalize();
    }

    public Vector3d getCenter() { return center; }
    public double getRadius() { return radius; }
    public double getStartAngle() { return startAngle; }
    public double getEndAngle() { return endAngle; }
    public Vector3d getNormal() { return normal; }
    public Vector3d getStart() { return start; }
    public Vector3d getEnd() { return end; }

    public boolean isFullCircle() {
        double angleDiff = Math.abs(endAngle - startAngle);
        return Math.abs(angleDiff - 2 * Math.PI) < 1e-6;
    }
}