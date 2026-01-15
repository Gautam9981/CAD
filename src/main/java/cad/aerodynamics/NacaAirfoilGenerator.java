package cad.aerodynamics;

import cad.core.Sketch;
import java.util.ArrayList;
import java.util.List;

public class NacaAirfoilGenerator {

    public static List<Sketch.PointEntity> generate(String digits, float chord, int numPoints) {
        if (digits == null || digits.length() != 4) {
            throw new IllegalArgumentException("Invalid NACA profile digits: " + digits);
        }

        int mInt = Integer.parseInt(digits.substring(0, 1));
        int pInt = Integer.parseInt(digits.substring(1, 2));
        int tInt = Integer.parseInt(digits.substring(2, 4));

        double m = mInt / 100.0;
        double p = pInt / 10.0;
        double t = tInt / 100.0;

        List<Sketch.PointEntity> points = new ArrayList<>();

        for (int i = 0; i <= numPoints; i++) {
            double beta = Math.PI * i / numPoints;
            double x = (1.0 - Math.cos(beta)) / 2.0;

            double[] coords = calculateCoordinates(x, m, p, t);
            double xu = coords[0];
            double yu = coords[1];

            points.add(new Sketch.PointEntity((float) (xu * chord), (float) (yu * chord)));
        }

        for (int i = numPoints - 1; i >= 0; i--) {
            double beta = Math.PI * i / numPoints;
            double x = (1.0 - Math.cos(beta)) / 2.0;

            double[] coords = calculateCoordinates(x, m, p, t);
            double xl = coords[2];
            double yl = coords[3];

            points.add(new Sketch.PointEntity((float) (xl * chord), (float) (yl * chord)));
        }

        return points;
    }

    private static double[] calculateCoordinates(double x, double m, double p, double t) {
        double yt = 5 * t * (0.2969 * Math.sqrt(x) - 0.1260 * x - 0.3516 * Math.pow(x, 2)
                + 0.2843 * Math.pow(x, 3) - 0.1015 * Math.pow(x, 4));

        double yc = 0;
        double dyc_dx = 0;

        if (m == 0) {
            yc = 0;
            dyc_dx = 0;
        } else {
            if (x < p) {
                yc = (m / (p * p)) * (2 * p * x - x * x);
                dyc_dx = (2 * m / (p * p)) * (p - x);
            } else {
                yc = (m / ((1 - p) * (1 - p))) * ((1 - 2 * p) + 2 * p * x - x * x);
                dyc_dx = (2 * m / ((1 - p) * (1 - p))) * (p - x);
            }
        }

        double theta = Math.atan(dyc_dx);

        double xu = x - yt * Math.sin(theta);
        double yu = yc + yt * Math.cos(theta);

        double xl = x + yt * Math.sin(theta);
        double yl = yc - yt * Math.cos(theta);

        return new double[] { xu, yu, xl, yl };
    }
}
