package cad.geometry.surfaces;

import cad.math.Vector3d;

public abstract class Surface {
    public abstract Vector3d value(double u, double v);
    public abstract Vector3d normal(double u, double v);
    
    public Vector3d derivativeU(double u, double v) {
        double delta = 1e-6;
        Vector3d p1 = value(u - delta, v);
        Vector3d p2 = value(u + delta, v);
        return p2.subtract(p1).multiply(1.0 / (2.0 * delta));
    }
    
    public Vector3d derivativeV(double u, double v) {
        double delta = 1e-6;
        Vector3d p1 = value(u, v - delta);
        Vector3d p2 = value(u, v + delta);
        return p2.subtract(p1).multiply(1.0 / (2.0 * delta));
    }
    
    public double getUMin() { return 0.0; }
    public double getUMax() { return 1.0; }
    public double getVMin() { return 0.0; }
    public double getVMax() { return 1.0; }
    
    public boolean isClosedInU() { return false; }
    public boolean isClosedInV() { return false; }
    
    public Surface reverseU() { return this; }
    public Surface reverseV() { return this; }
    public Surface swapUV() { return this; }
    
    public abstract BoundingBox getBounds();
    
    public static class BoundingBox {
        public final Vector3d min;
        public final Vector3d max;
        
        public BoundingBox(Vector3d min, Vector3d max) {
            this.min = min;
            this.max = max;
        }
        
        public double getVolume() {
            Vector3d size = max.subtract(min);
            return size.x() * size.y() * size.z();
        }
    }
}
