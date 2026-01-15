package cad.geometry.surfaces;

import cad.math.Vector3d;

public class SphericalSurface extends Surface {
    private Vector3d center;
    private double radius;

    public SphericalSurface(Vector3d center, double radius) {
        this.center = center;
        this.radius = Math.abs(radius);
    }

    @Override
    public Vector3d value(double u, double v) {
        double theta = u * 2 * Math.PI; // Azimuthal angle
        double phi = v * Math.PI;       // Polar angle

        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);
        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);

        double x = radius * sinPhi * cosTheta;
        double y = radius * sinPhi * sinTheta;
        double z = radius * cosPhi;

        return center.plus(new Vector3d(x, y, z));
    }

    @Override
    public Vector3d normal(double u, double v) {
        Vector3d point = value(u, v);
        return point.subtract(center).normalize();
    }

    @Override
    public Vector3d derivativeU(double u, double v) {
        double theta = u * 2 * Math.PI;
        double phi = v * Math.PI;
        
        double sinPhi = Math.sin(phi);
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double x = -radius * sinPhi * Math.sin(theta) * 2 * Math.PI;
        double y = radius * sinPhi * Math.cos(theta) * 2 * Math.PI;
        double z = 0;

        return new Vector3d(x, y, z);
    }

    @Override
    public Vector3d derivativeV(double u, double v) {
        double theta = u * 2 * Math.PI;
        double phi = v * Math.PI;
        
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);
        double cosTheta = Math.cos(theta);
        double sinTheta = Math.sin(theta);

        double x = radius * cosPhi * cosTheta * Math.PI;
        double y = radius * cosPhi * sinTheta * Math.PI;
        double z = -radius * sinPhi * Math.PI;

        return new Vector3d(x, y, z);
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
        Vector3d min = center.minus(new Vector3d(radius, radius, radius));
        Vector3d max = center.plus(new Vector3d(radius, radius, radius));
        return new BoundingBox(min, max);
    }

    public double distanceToPoint(Vector3d point) {
        double distanceToCenter = point.distance(center);
        return Math.abs(distanceToCenter - radius);
    }

    public Vector3d closestPoint(Vector3d point) {
        Vector3d direction = point.subtract(center).normalize();
        return center.plus(direction.multiply(radius));
    }

    public boolean containsPoint(Vector3d point, double tolerance) {
        return distanceToPoint(point) <= tolerance;
    }

    public Vector3d getCenter() { return center; }
    public double getRadius() { return radius; }

    public boolean isPointInside(Vector3d point) {
        return point.distance(center) < radius;
    }

    public Vector3d getPointAtAngles(double theta, double phi) {
        double sinPhi = Math.sin(phi);
        double cosPhi = Math.cos(phi);
        double sinTheta = Math.sin(theta);
        double cosTheta = Math.cos(theta);

        double x = radius * sinPhi * cosTheta;
        double y = radius * sinPhi * sinTheta;
        double z = radius * cosPhi;

        return center.plus(new Vector3d(x, y, z));
    }

    public double[] getAnglesForPoint(Vector3d point) {
        Vector3d relative = point.subtract(center);
        double r = relative.magnitude();
        
        if (r == 0) {
            return new double[]{0, 0};
        }
        
        double theta = Math.atan2(relative.y(), relative.x());
        double phi = Math.acos(relative.z() / r);
        
        theta = theta < 0 ? theta + 2 * Math.PI : theta;
        
        return new double[]{theta, phi};
    }
}