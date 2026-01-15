package cad.geometry.surfaces;

import cad.math.Vector3d;

public class NurbsSurface extends Surface {
    private int uDegree;
    private int vDegree;
    private int uControlPoints;
    private int vControlPoints;
    private Vector3d[][] controlPoints;
    private double[][] weights;
    private double[] uKnots;
    private double[] vKnots;
    
    public NurbsSurface(int uDegree, int vDegree, Vector3d[][] controlPoints, double[][] weights, 
                       double[] uKnots, double[] vKnots) {
        this.uDegree = uDegree;
        this.vDegree = vDegree;
        this.controlPoints = controlPoints;
        this.weights = weights;
        this.uKnots = uKnots;
        this.vKnots = vKnots;
        this.uControlPoints = controlPoints.length;
        this.vControlPoints = controlPoints[0].length;
    }
    
    @Override
    public Vector3d value(double u, double v) {
        double scaledU = scaleParameter(u, uKnots);
        double scaledV = scaleParameter(v, vKnots);
        
        Vector3d numerator = Vector3d.zero();
        double denominator = 0.0;
        
        for (int i = 0; i < uControlPoints; i++) {
            for (int j = 0; j < vControlPoints; j++) {
                double basisU = calculateBasis(i, uDegree, scaledU, uKnots);
                double basisV = calculateBasis(j, vDegree, scaledV, vKnots);
                double weight = weights[i][j];
                
                double blendedWeight = basisU * basisV * weight;
                numerator = numerator.plus(controlPoints[i][j].multiply(blendedWeight));
                denominator += blendedWeight;
            }
        }
        
        return denominator > 0 ? numerator.multiply(1.0 / denominator) : Vector3d.zero();
    }
    
    @Override
    public Vector3d normal(double u, double v) {
        Vector3d dU = derivativeU(u, v);
        Vector3d dV = derivativeV(u, v);
        return dU.cross(dV).normalize();
    }
    
    @Override
    public Vector3d derivativeU(double u, double v) {
        double scaledU = scaleParameter(u, uKnots);
        double scaledV = scaleParameter(v, vKnots);
        
        Vector3d dNumerator = Vector3d.zero();
        double dDenominator = 0.0;
        double denominator = 0.0;
        
        for (int i = 0; i < uControlPoints; i++) {
            for (int j = 0; j < vControlPoints; j++) {
                double basisU = calculateBasis(i, uDegree, scaledU, uKnots);
                double dBasisU = calculateBasisDerivative(i, uDegree, scaledU, uKnots);
                double basisV = calculateBasis(j, vDegree, scaledV, vKnots);
                double weight = weights[i][j];
                
                double blendedWeight = basisU * basisV * weight;
                double dBlendedWeight = dBasisU * basisV * weight;
                
                dNumerator = dNumerator.plus(controlPoints[i][j].multiply(dBlendedWeight));
                dDenominator += dBlendedWeight;
                denominator += blendedWeight;
            }
        }
        
        if (denominator == 0) return Vector3d.zero();
        
        Vector3d term1 = dNumerator.multiply(1.0 / denominator);
        Vector3d term2 = value(u, v).multiply(dDenominator / (denominator * denominator));
        return term1.minus(term2);
    }
    
    @Override
    public Vector3d derivativeV(double u, double v) {
        double scaledU = scaleParameter(u, uKnots);
        double scaledV = scaleParameter(v, vKnots);
        
        Vector3d dNumerator = Vector3d.zero();
        double dDenominator = 0.0;
        double denominator = 0.0;
        
        for (int i = 0; i < uControlPoints; i++) {
            for (int j = 0; j < vControlPoints; j++) {
                double basisU = calculateBasis(i, uDegree, scaledU, uKnots);
                double basisV = calculateBasis(j, vDegree, scaledV, vKnots);
                double dBasisV = calculateBasisDerivative(j, vDegree, scaledV, vKnots);
                double weight = weights[i][j];
                
                double blendedWeight = basisU * basisV * weight;
                double dBlendedWeight = basisU * dBasisV * weight;
                
                dNumerator = dNumerator.plus(controlPoints[i][j].multiply(dBlendedWeight));
                dDenominator += dBlendedWeight;
                denominator += blendedWeight;
            }
        }
        
        if (denominator == 0) return Vector3d.zero();
        
        Vector3d term1 = dNumerator.multiply(1.0 / denominator);
        Vector3d term2 = value(u, v).multiply(dDenominator / (denominator * denominator));
        return term1.minus(term2);
    }
    
    private double scaleParameter(double t, double[] knots) {
        double tMin = knots[uDegree];
        double tMax = knots[knots.length - uDegree - 1];
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
    
    @Override
    public BoundingBox getBounds() {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        
        for (int i = 0; i < uControlPoints; i++) {
            for (int j = 0; j < vControlPoints; j++) {
                Vector3d cp = controlPoints[i][j];
                minX = Math.min(minX, cp.x());
                maxX = Math.max(maxX, cp.x());
                minY = Math.min(minY, cp.y());
                maxY = Math.max(maxY, cp.y());
                minZ = Math.min(minZ, cp.z());
                maxZ = Math.max(maxZ, cp.z());
            }
        }
        
        Vector3d min = new Vector3d(minX, minY, minZ);
        Vector3d max = new Vector3d(maxX, maxY, maxZ);
        return new BoundingBox(min, max);
    }
    
    public static NurbsSurface createCylindricalSurface(Vector3d origin, Vector3d axis, double radius, double height) {
        int uDegree = 2, vDegree = 1;
        int uPoints = 5, vPoints = 3;
        
        Vector3d[][] controlPoints = new Vector3d[uPoints][vPoints];
        double[][] weights = new double[uPoints][vPoints];
        double[] uKnots = {0, 0, 0, 0.5, 1, 1, 1};
        double[] vKnots = {0, 0, 1, 1};
        
        Vector3d radialX = Math.abs(axis.dot(Vector3d.Z_AXIS)) < 0.9 ? axis.cross(Vector3d.Z_AXIS).normalize() : axis.cross(Vector3d.X_AXIS).normalize();
        Vector3d radialY = axis.cross(radialX);
        
        for (int i = 0; i < uPoints; i++) {
            double angle = 2 * Math.PI * i / (uPoints - 1);
            Vector3d radial = radialX.multiply(Math.cos(angle)).plus(radialY.multiply(Math.sin(angle))).multiply(radius);
            
            for (int j = 0; j < vPoints; j++) {
                double h = height * j / (vPoints - 1) - height / 2.0;
                controlPoints[i][j] = origin.plus(radial).plus(axis.multiply(h));
                weights[i][j] = 1.0;
            }
        }
        
        return new NurbsSurface(uDegree, vDegree, controlPoints, weights, uKnots, vKnots);
    }
    
    public int getUDegree() { return uDegree; }
    public int getVDegree() { return vDegree; }
    public int getUControlPoints() { return uControlPoints; }
    public int getVControlPoints() { return vControlPoints; }
    public Vector3d[][] getControlPoints() { return controlPoints; }
    public double[][] getWeights() { return weights; }
    public double[] getUKnots() { return uKnots; }
    public double[] getVKnots() { return vKnots; }
}