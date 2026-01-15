package cad.analysis;

public class FluidDynamics {

    private static final double RHO_SEA_LEVEL = 1.225;

    public static class AnalysisResult {
        public double liftCoef;
        public double dragCoef;
        public double liftForce;
        public double dragForce;
        public double reynoldsNumber;

        @Override
        public String toString() {
            return String.format("Cl: %.4f, Cd: %.4f, L: %.2f N, D: %.2f N, Re: %.2e",
                    liftCoef, dragCoef, liftForce, dragForce, reynoldsNumber);
        }
    }

    public static AnalysisResult analyzeAirfoil(double velocity, double chord, double span, double alpha,
            double density) {
        AnalysisResult result = new AnalysisResult();

        double alphaRad = Math.toRadians(alpha);

        if (Math.abs(alpha) < 15) {
            result.liftCoef = 2 * Math.PI * alphaRad;
        } else {
            result.liftCoef = 2 * Math.PI * Math.toRadians(15) * Math.cos(alphaRad);
        }

        double cd0 = 0.005;
        double inducedDragFactor = 0.05;
        result.dragCoef = cd0 + (inducedDragFactor * result.liftCoef * result.liftCoef);

        if (Math.abs(alpha) > 15) {
            result.dragCoef += 0.5 * Math.sin(alphaRad);
        }

        double q = 0.5 * density * velocity * velocity;
        double area = chord * span;

        result.liftForce = q * area * result.liftCoef;
        result.dragForce = q * area * result.dragCoef;

        double mu = 1.81e-5;
        result.reynoldsNumber = (density * velocity * chord) / mu;

        return result;
    }

    public static double getStandardDensity() {
        return RHO_SEA_LEVEL;
    }
}
