package cad.geometry.surfaces;

import cad.math.Vector3d;

public class PlaneSurface extends Surface {
    private Vector3d origin;
    private Vector3d normal;
    private Vector3d uDirection;
    private Vector3d vDirection;
    private double uSize;
    private double vSize;

    public PlaneSurface(Vector3d origin, Vector3d normal, double uSize, double vSize) {
        this.origin = origin;
        this.normal = normal.normalize();
        this.uSize = uSize;
        this.vSize = vSize;
        
        calculateDirectionVectors();
    }

    public PlaneSurface(Vector3d origin, Vector3d normal) {
        this(origin, normal, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public PlaneSurface(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d v1 = p2.subtract(p1);
        Vector3d v2 = p3.subtract(p1);
        this.origin = p1;
        this.normal = v1.cross(v2).normalize();
        this.uSize = Double.MAX_VALUE;
        this.vSize = Double.MAX_VALUE;
        
        calculateDirectionVectors();
    }

    private void calculateDirectionVectors() {
        if (Math.abs(normal.dot(Vector3d.Z_AXIS)) < 0.9) {
            vDirection = normal.cross(Vector3d.Z_AXIS).normalize();
        } else {
            vDirection = normal.cross(Vector3d.X_AXIS).normalize();
        }
        uDirection = vDirection.cross(normal).normalize();
    }

    @Override
    public Vector3d value(double u, double v) {
        return origin
            .plus(uDirection.multiply(u * uSize))
            .plus(vDirection.multiply(v * vSize));
    }

    @Override
    public Vector3d normal(double u, double v) {
        return normal;
    }

    @Override
    public Vector3d derivativeU(double u, double v) {
        return uDirection.multiply(uSize);
    }

    @Override
    public Vector3d derivativeV(double u, double v) {
        return vDirection.multiply(vSize);
    }

    @Override
    public double getUMin() { return uSize == Double.MAX_VALUE ? -Double.MAX_VALUE / 2 : 0.0; }
    @Override
    public double getUMax() { return uSize == Double.MAX_VALUE ? Double.MAX_VALUE / 2 : 1.0; }
    @Override
    public double getVMin() { return vSize == Double.MAX_VALUE ? -Double.MAX_VALUE / 2 : 0.0; }
    @Override
    public double getVMax() { return vSize == Double.MAX_VALUE ? Double.MAX_VALUE / 2 : 1.0; }

    @Override
    public BoundingBox getBounds() {
        if (uSize == Double.MAX_VALUE || vSize == Double.MAX_VALUE) {
            Vector3d inf = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            return new BoundingBox(inf.negated(), inf);
        }

        Vector3d[] corners = {
            value(0.0, 0.0),
            value(1.0, 0.0),
            value(0.0, 1.0),
            value(1.0, 1.0)
        };

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (Vector3d corner : corners) {
            minX = Math.min(minX, corner.x());
            maxX = Math.max(maxX, corner.x());
            minY = Math.min(minY, corner.y());
            maxY = Math.max(maxY, corner.y());
            minZ = Math.min(minZ, corner.z());
            maxZ = Math.max(maxZ, corner.z());
        }

        return new BoundingBox(
            new Vector3d(minX, minY, minZ),
            new Vector3d(maxX, maxY, maxZ)
        );
    }

    public Vector3d getOrigin() { return origin; }
    public Vector3d getNormal() { return normal; }
    public Vector3d getUDirection() { return uDirection; }
    public Vector3d getVDirection() { return vDirection; }
    public double getUSize() { return uSize; }
    public double getVSize() { return vSize; }

    public double distanceToPoint(Vector3d point) {
        return Math.abs(normal.dot(point.subtract(origin)));
    }

    public Vector3d closestPoint(Vector3d point) {
        Vector3d toPoint = point.subtract(origin);
        double distance = normal.dot(toPoint);
        return point.subtract(normal.multiply(distance));
    }

    public boolean containsPoint(Vector3d point, double tolerance) {
        return distanceToPoint(point) <= tolerance;
    }
}