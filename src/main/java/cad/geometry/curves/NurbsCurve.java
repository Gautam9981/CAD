package cad.geometry.curves;

import cad.math.Vector3d;

public class NurbsCurve extends Curve {
    private int degree;
    private int controlPoints;
    private Vector3d[] controlPointArray;
    private double[] weights;
    private double[] knots;

    public NurbsCurve(int degree, Vector3d[] controlPoints, double[] weights, double[] knots) {
        this.degree = degree;
        this.controlPointArray = controlPoints;
        this.weights = weights;
        this.knots = knots;
        this.controlPoints = controlPoints.length;
    }

    @Override
    public Vector3d value(double t) {
        double scaledT = scaleParameter(t);
        
        Vector3d numerator = Vector3d.zero();
        double denominator = 0.0;
        
        for (int i = 0; i < controlPoints; i++) {
            double basis = calculateBasis(i, degree, scaledT, knots);
            double weight = weights[i];
            
            double blendedWeight = basis * weight;
            numerator = numerator.plus(controlPointArray[i].multiply(blendedWeight));
            denominator += blendedWeight;
        }
        
        return denominator > 0 ? numerator.multiply(1.0 / denominator) : Vector3d.zero();
    }

    @Override
    public Vector3d tangent(double t) {
        Vector3d derivative = derivative(t);
        return derivative.normalize();
    }

    @Override
    public Vector3d derivative(double t) {
        double scaledT = scaleParameter(t);
        
        Vector3d dNumerator = Vector3d.zero();
        double dDenominator = 0.0;
        double denominator = 0.0;
        
        for (int i = 0; i < controlPoints; i++) {
            double basis = calculateBasis(i, degree, scaledT, knots);
            double dBasis = calculateBasisDerivative(i, degree, scaledT, knots);
            double weight = weights[i];
            
            double blendedWeight = basis * weight;
            double dBlendedWeight = dBasis * weight;
            
            dNumerator = dNumerator.plus(controlPointArray[i].multiply(dBlendedWeight));
            dDenominator += dBlendedWeight;
            denominator += blendedWeight;
        }
        
        if (denominator == 0) return Vector3d.zero();
        
        Vector3d term1 = dNumerator.multiply(1.0 / denominator);
        Vector3d term2 = value(t).multiply(dDenominator / (denominator * denominator));
        return term1.minus(term2);
    }

    @Override
    public double startParam() {
        return 0.0;
    }

    @Override
    public double endParam() {
        return 1.0;
    }

    private double scaleParameter(double t) {
        double tMin = knots[degree];
        double tMax = knots[knots.length - degree - 1];
        return tMin + t * (tMax - tMin);
    }

    private double calculateBasis(int i, int degree, double t, double[] knots) {
        if (degree == 0) {
            return (t >= knots[i] && t < knots[i + 1]) ? 1.0 : 0.0;
        }
        
        double left = 0.0, right = 0.0;
        
        double denominator1 = knots[i + degree] - knots[i];
        if (denominator1 != 0) {
            left = ((t - knots[i]) / denominator1) * calculateBasis(i, degree - 1, t, knots);
        }
        
        double denominator2 = knots[i + degree + 1] - knots[i + 1];
        if (denominator2 != 0) {
            right = ((knots[i + degree + 1] - t) / denominator2) * calculateBasis(i + 1, degree - 1, t, knots);
        }
        
        return left + right;
    }

    private double calculateBasisDerivative(int i, int degree, double t, double[] knots) {
        if (degree == 0) {
            return 0.0; // Constant basis function
        }
        
        double leftDerivative = 0.0, rightDerivative = 0.0;
        
        double denominator1 = knots[i + degree] - knots[i];
        if (denominator1 != 0) {
            double basis1 = calculateBasis(i, degree - 1, t, knots);
            double derivative1 = calculateBasisDerivative(i, degree - 1, t, knots);
            leftDerivative = (basis1 + (t - knots[i]) * derivative1) / denominator1;
        }
        
        double denominator2 = knots[i + degree + 1] - knots[i + 1];
        if (denominator2 != 0) {
            double basis2 = calculateBasis(i + 1, degree - 1, t, knots);
            double derivative2 = calculateBasisDerivative(i + 1, degree - 1, t, knots);
            rightDerivative = (-basis2 + (knots[i + degree + 1] - t) * derivative2) / denominator2;
        }
        
        return leftDerivative + rightDerivative;
    }

    public static NurbsCurve createCircle(Vector3d center, double radius, Vector3d normal) {
        int degree = 2;
        int numPoints = 7;
        
        Vector3d[] controlPoints = new Vector3d[numPoints];
        double[] weights = new double[numPoints];
        double[] knots = {0, 0, 0, 0.25, 0.5, 0.75, 1, 1, 1};
        
        Vector3d radialX = Math.abs(normal.dot(Vector3d.Z_AXIS)) < 0.9 ? normal.cross(Vector3d.Z_AXIS).normalize() : normal.cross(Vector3d.X_AXIS).normalize();
        Vector3d radialY = normal.cross(radialX);
        
        for (int i = 0; i < numPoints; i++) {
            double angle = 2 * Math.PI * i / 6.0;
            if (i % 2 == 0) {
                controlPoints[i] = center.plus(radialX.multiply(radius * Math.cos(angle)).plus(radialY.multiply(radius * Math.sin(angle))));
                weights[i] = 1.0;
            } else {
                double scale = 1.0 / Math.cos(Math.PI / 6.0);
                controlPoints[i] = center.plus(radialX.multiply(radius * scale * Math.cos(angle)).plus(radialY.multiply(radius * scale * Math.sin(angle))));
                weights[i] = Math.cos(Math.PI / 6.0);
            }
        }
        
        return new NurbsCurve(degree, controlPoints, weights, knots);
    }

    public static NurbsCurve createLine(Vector3d start, Vector3d end) {
        return new NurbsCurve(1, new Vector3d[]{start, end}, new double[]{1.0, 1.0}, new double[]{0, 0, 1, 1});
    }

    public int getDegree() { return degree; }
    public int getControlPointCount() { return controlPoints; }
    public Vector3d[] getControlPoints() { return controlPointArray; }
    public double[] getWeights() { return weights; }
    public double[] getKnots() { return knots; }
}