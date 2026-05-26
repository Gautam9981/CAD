package cad.geometry.surfaces;

import cad.math.Vector3d;

public class ConicalSurface extends Surface {
    private Vector3d axisOrigin;
    private Vector3d axisDirection;
    private double baseRadius;
    private double halfAngle; // radians
    private double minV;
    private double maxV;

    public ConicalSurface(Vector3d axisOrigin, Vector3d axisDirection, double baseRadius, double halfAngle, double minV, double maxV) {
        this.axisOrigin = axisOrigin;
        this.axisDirection = axisDirection.normalize();
        this.baseRadius = baseRadius;
        this.halfAngle = halfAngle;
        this.minV = minV;
        this.maxV = maxV;
    }

    @Override
    public Vector3d value(double u, double v) {
        double angle = u * 2 * Math.PI;
        double heightPos = minV + v * (maxV - minV);
        double currentRadius = baseRadius + heightPos * Math.tan(halfAngle);
        
        Vector3d radialX = getPerpendicularX();
        Vector3d radialY = axisDirection.cross(radialX).normalize();
        
        Vector3d radial = radialX.multiply(Math.cos(angle)).plus(radialY.multiply(Math.sin(angle)));
        return axisOrigin.plus(axisDirection.multiply(heightPos)).plus(radial.multiply(currentRadius));
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
    public boolean isClosedInU() { return true; }

    @Override
    public BoundingBox getBounds() {
        int uSteps = 8;
        int vSteps = 2; // only minV and maxV strictly necessary for a straight cone
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
