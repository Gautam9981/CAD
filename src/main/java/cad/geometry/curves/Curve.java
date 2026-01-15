package cad.geometry.curves;

import cad.math.Vector3d;

public abstract class Curve {
    public abstract Vector3d value(double t);

    public abstract Vector3d tangent(double t);

    public abstract double startParam();

    public abstract double endParam();
    
    public Vector3d derivative(double t) {
        double delta = 1e-6;
        Vector3d p1 = value(t - delta);
        Vector3d p2 = value(t + delta);
        return p2.subtract(p1).multiply(1.0 / (2.0 * delta));
    }
    
    public double length() {
        return length(startParam(), endParam(), 100);
    }
    
    public double length(double t1, double t2, int segments) {
        if (segments <= 0) return 0.0;
        
        double dt = (t2 - t1) / segments;
        double totalLength = 0.0;
        
        Vector3d prevPoint = value(t1);
        
        for (int i = 1; i <= segments; i++) {
            double t = t1 + i * dt;
            Vector3d currPoint = value(t);
            totalLength += prevPoint.distance(currPoint);
            prevPoint = currPoint;
        }
        
        return totalLength;
    }
    
    public Vector3d getMidPoint() {
        return value((startParam() + endParam()) / 2.0);
    }
}
