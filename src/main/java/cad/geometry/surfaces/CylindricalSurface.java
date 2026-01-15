package cad.geometry.surfaces;

import cad.math.Vector3d;

public class CylindricalSurface extends Surface {
    private Vector3d axisOrigin;
    private Vector3d axisDirection;
    private double radius;
    private double height;
    private boolean isInfinite;

    public CylindricalSurface(Vector3d axisOrigin, Vector3d axisDirection, double radius) {
        this(axisOrigin, axisDirection, radius, Double.MAX_VALUE, true);
    }

    public CylindricalSurface(Vector3d axisOrigin, Vector3d axisDirection, double radius, double height) {
        this(axisOrigin, axisDirection, radius, height, false);
    }

    private CylindricalSurface(Vector3d axisOrigin, Vector3d axisDirection, double radius, double height, boolean isInfinite) {
        this.axisOrigin = axisOrigin;
        this.axisDirection = axisDirection.normalize();
        this.radius = Math.abs(radius);
        this.height = Math.abs(height);
        this.isInfinite = isInfinite;
    }

    @Override
    public Vector3d value(double u, double v) {
        double angle = u * 2 * Math.PI;
        double h = v * height - height / 2.0;

        Vector3d radialX = getPerpendicularX();
        Vector3d radialY = axisDirection.cross(radialX);

        Vector3d radial = radialX.multiply(Math.cos(angle)).plus(radialY.multiply(Math.sin(angle)));
        return axisOrigin
            .plus(radial.multiply(radius))
            .plus(axisDirection.multiply(h));
    }

    @Override
    public Vector3d normal(double u, double v) {
        double angle = u * 2 * Math.PI;
        Vector3d radialX = getPerpendicularX();
        Vector3d radialY = axisDirection.cross(radialX);
        
        return radialX.multiply(Math.cos(angle)).plus(radialY.multiply(Math.sin(angle))).normalize();
    }

    @Override
    public Vector3d derivativeU(double u, double v) {
        double angle = u * 2 * Math.PI;
        Vector3d radialX = getPerpendicularX();
        Vector3d radialY = axisDirection.cross(radialX);
        
        Vector3d tangent = radialX.multiply(-Math.sin(angle)).plus(radialY.multiply(Math.cos(angle)));
        return tangent.multiply(2 * Math.PI * radius);
    }

    @Override
    public Vector3d derivativeV(double u, double v) {
        return axisDirection.multiply(height);
    }

    @Override
    public double getUMin() { return 0.0; }
    @Override
    public double getUMax() { return 1.0; }
    @Override
    public double getVMin() { return 0.0; }
    @Override
    public double getVMax() { return 1.0; }

    @Override
    public boolean isClosedInU() { return true; }

    @Override
    public BoundingBox getBounds() {
        if (isInfinite) {
            Vector3d inf = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
            return new BoundingBox(inf.negated(), inf);
        }

        Vector3d radialX = getPerpendicularX();
        Vector3d radialY = axisDirection.cross(radialX);

        Vector3d bottom = axisOrigin.minus(axisDirection.multiply(height / 2.0));
        Vector3d top = axisOrigin.plus(axisDirection.multiply(height / 2.0));

        Vector3d[] samplePoints = {
            bottom.plus(radialX.multiply(radius)),
            bottom.minus(radialX.multiply(radius)),
            bottom.plus(radialY.multiply(radius)),
            bottom.minus(radialY.multiply(radius)),
            top.plus(radialX.multiply(radius)),
            top.minus(radialX.multiply(radius)),
            top.plus(radialY.multiply(radius)),
            top.minus(radialY.multiply(radius))
        };

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (Vector3d point : samplePoints) {
            minX = Math.min(minX, point.x());
            maxX = Math.max(maxX, point.x());
            minY = Math.min(minY, point.y());
            maxY = Math.max(maxY, point.y());
            minZ = Math.min(minZ, point.z());
            maxZ = Math.max(maxZ, point.z());
        }

        return new BoundingBox(
            new Vector3d(minX, minY, minZ),
            new Vector3d(maxX, maxY, maxZ)
        );
    }

    private Vector3d getPerpendicularX() {
        Vector3d test = Math.abs(axisDirection.dot(Vector3d.Z_AXIS)) < 0.9 ? Vector3d.Z_AXIS : Vector3d.X_AXIS;
        return axisDirection.cross(test).normalize();
    }

    public double distanceToPoint(Vector3d point) {
        Vector3d toPoint = point.subtract(axisOrigin);
        double axialDistance = axisDirection.dot(toPoint);
        Vector3d closestPointOnAxis = axisOrigin.plus(axisDirection.multiply(axialDistance));
        double radialDistance = toPoint.subtract(axisDirection.multiply(axialDistance)).magnitude();
        
        return Math.abs(radialDistance - radius);
    }

    public Vector3d closestPoint(Vector3d point) {
        Vector3d toPoint = point.subtract(axisOrigin);
        double axialDistance = axisDirection.dot(toPoint);
        
        if (!isInfinite) {
            axialDistance = Math.max(-height/2.0, Math.min(height/2.0, axialDistance));
        }
        
        Vector3d closestPointOnAxis = axisOrigin.plus(axisDirection.multiply(axialDistance));
        Vector3d radial = point.subtract(closestPointOnAxis);
        
        if (radial.magnitude() > 0) {
            radial = radial.normalize().multiply(radius);
        } else {
            Vector3d perpX = getPerpendicularX();
            radial = perpX.multiply(radius);
        }
        
        return closestPointOnAxis.plus(radial);
    }

    public Vector3d getAxisOrigin() { return axisOrigin; }
    public Vector3d getAxisDirection() { return axisDirection; }
    public double getRadius() { return radius; }
    public double getHeight() { return height; }
    public boolean isInfinite() { return isInfinite; }

    public boolean containsPoint(Vector3d point, double tolerance) {
        return distanceToPoint(point) <= tolerance;
    }
}