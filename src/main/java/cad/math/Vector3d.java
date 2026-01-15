package cad.math;

public class Vector3d {
    private double x, y, z;

    public static final Vector3d ZERO = new Vector3d(0, 0, 0);
    public static final Vector3d X_AXIS = new Vector3d(1, 0, 0);
    public static final Vector3d Y_AXIS = new Vector3d(0, 1, 0);
    public static final Vector3d Z_AXIS = new Vector3d(0, 0, 1);

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }

    public Vector3d add(Vector3d v) {
        return new Vector3d(x + v.x, y + v.y, z + v.z);
    }

    public Vector3d subtract(Vector3d v) {
        return new Vector3d(x - v.x, y - v.y, z - v.z);
    }
    
    public Vector3d minus(Vector3d v) {
        return subtract(v);
    }
    
    public Vector3d plus(Vector3d v) {
        return add(v);
    }

    public Vector3d multiply(double scalar) {
        return new Vector3d(x * scalar, y * scalar, z * scalar);
    }
    
    public Vector3d times(double scalar) {
        return multiply(scalar);
    }
    
    public Vector3d negated() {
        return new Vector3d(-x, -y, -z);
    }

    public double dot(Vector3d v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public Vector3d cross(Vector3d v) {
        return new Vector3d(
            y * v.z - z * v.y,
            z * v.x - x * v.z,
            x * v.y - y * v.x
        );
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public Vector3d normalize() {
        double m = magnitude();
        if (m == 0) return ZERO;
        return new Vector3d(x / m, y / m, z / m);
    }
    
    public double distance(Vector3d v) {
        return Math.sqrt(Math.pow(x - v.x, 2) + Math.pow(y - v.y, 2) + Math.pow(z - v.z, 2));
    }
    
    public static Vector3d xyz(double x, double y, double z) {
        return new Vector3d(x, y, z);
    }
    
    public static Vector3d zero() {
        return ZERO;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector3d other = (Vector3d) obj;
        final double EPSILON = 1e-9;
        return Math.abs(this.x - other.x) < EPSILON &&
               Math.abs(this.y - other.y) < EPSILON &&
               Math.abs(this.z - other.z) < EPSILON;
    }

    @Override
    public int hashCode() {
        final double EPSILON = 1e-9;
        long xBits = Double.doubleToLongBits(Math.round(x / EPSILON));
        long yBits = Double.doubleToLongBits(Math.round(y / EPSILON));
        long zBits = Double.doubleToLongBits(Math.round(z / EPSILON));
        return (int) (xBits ^ (xBits >>> 32) ^ yBits ^ (yBits >>> 32) ^ zBits ^ (zBits >>> 32));
    }

    public String toString() {
        return String.format("(%.3f, %.3f, %.3f)", x, y, z);
    }
}
