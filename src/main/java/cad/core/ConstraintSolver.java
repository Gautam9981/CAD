package cad.core;

import java.util.List;

public class ConstraintSolver {
    private static final int MAX_ITERATIONS = 100;
    private static final double ERROR_TOLERANCE = 1e-4;

    public static boolean solve(List<Constraint> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return true;
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double maxError = 0;

            for (Constraint c : constraints) {
                if (!c.isActive())
                    continue;

                double error = Math.abs(c.getError());
                if (error > maxError) {
                    maxError = error;
                }

                c.solve();
            }

            if (maxError < ERROR_TOLERANCE) {
                return true;
            }
        }

        return false;
    }
}
