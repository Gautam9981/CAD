package cad.geometry.surfaces;

import cad.geometry.curves.Curve;
import cad.math.Vector3d;

public class SurfaceOfRevolution extends Surface {
    private Curve baseCurve;
    private Vector3d axisOrigin;
    private Vector3d axisDirection;
    private double startAngle;
    private double endAngle;

    public SurfaceOfRevolution(Curve baseCurve, Vector3d axisOrigin, Vector3d axisDirection) {
        this(baseCurve, axisOrigin, axisDirection, 0.0, 2.0 * Math.PI);
    }

    public SurfaceOfRevolution(Curve baseCurve, Vector3d axisOrigin, Vector3d axisDirection, double startAngle, double endAngle) {
        this.baseCurve = baseCurve;
        this.axisOrigin = axisOrigin;
        this.axisDirection = axisDirection.normalize();
        this.startAngle = startAngle;
        this.endAngle = endAngle;
    }

    @Override
    public Vector3d value(double u, double v) {
        // v parametrizes the curve (e.g. from startParam to endParam)
        double t = baseCurve.startParam() + v * (baseCurve.endParam() - baseCurve.startParam());
        Vector3d curvePoint = baseCurve.value(t);
        
        // u parametrizes the angle
        double angle = startAngle + u * (endAngle - startAngle);
        
        // Rotate curvePoint around the axis
        Vector3d toPoint = curvePoint.subtract(axisOrigin);
        double axialDist = axisDirection.dot(toPoint);
        Vector3d axialComp = axisDirection.multiply(axialDist);
        Vector3d radialComp = toPoint.subtract(axialComp);
        
        Vector3d radialDir = radialComp;
        double radius = radialDir.magnitude();
        if (radius > 1e-6) {
            radialDir = radialDir.normalize();
        } else {
            radialDir = getPerpendicularX();
        }
        
        Vector3d tangentDir = axisDirection.cross(radialDir).normalize();
        
        Vector3d rotatedRadial = radialDir.multiply(Math.cos(angle)).plus(tangentDir.multiply(Math.sin(angle)));
        
        return axisOrigin.plus(axialComp).plus(rotatedRadial.multiply(radius));
    }

    @Override
    public Vector3d normal(double u, double v) {
        Vector3d dU = derivativeU(u, v);
        Vector3d dV = derivativeV(u, v);
        return dU.cross(dV).normalize();
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
    public boolean isClosedInU() {
        return Math.abs((endAngle - startAngle) - 2 * Math.PI) < 1e-6;
    }

    @Override
    public BoundingBox getBounds() {
        // Simple bounding box sampling
        int uSteps = 16;
        int vSteps = 16;
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (int i = 0; i <= uSteps; i++) {
            for (int j = 0; j <= vSteps; j++) {
                double u = (double) i / uSteps;
                double v = (double) j / vSteps;
                Vector3d p = value(u, v);
                minX = Math.min(minX, p.x()); maxX = Math.max(maxX, p.x());
                minY = Math.min(minY, p.y()); maxY = Math.max(maxY, p.y());
                minZ = Math.min(minZ, p.z()); maxZ = Math.max(maxZ, p.z());
            }
        }
        return new BoundingBox(new Vector3d(minX, minY, minZ), new Vector3d(maxX, maxY, maxZ));
    }
    
    private Vector3d getPerpendicularX() {
        Vector3d test = Math.abs(axisDirection.dot(Vector3d.Z_AXIS)) < 0.9 ? Vector3d.Z_AXIS : Vector3d.X_AXIS;
        return axisDirection.cross(test).normalize();
    }
}
