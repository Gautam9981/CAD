package cad.geometry.curves;

import cad.math.Vector3d;

public class LineCurve extends Curve {
    private Vector3d startPoint;
    private Vector3d endPoint;

    public LineCurve(Vector3d startPoint, Vector3d endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    @Override
    public Vector3d value(double t) {
        return startPoint.plus(endPoint.subtract(startPoint).multiply(t));
    }

    @Override
    public Vector3d tangent(double t) {
        Vector3d direction = endPoint.subtract(startPoint);
        return direction.normalize();
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
    public Vector3d derivative(double t) {
        return endPoint.subtract(startPoint);
    }

    public Vector3d getStartPoint() {
        return startPoint;
    }

    public Vector3d getEndPoint() {
        return endPoint;
    }

    public double getLength() {
        return startPoint.distance(endPoint);
    }

    public boolean isPoint() {
        return startPoint.distance(endPoint) < 1e-10;
    }
}