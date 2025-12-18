package cad.core;

import java.util.ArrayList;
import java.util.List;


public class ConstraintSolver {
    private static final int MAX_ITERATIONS = 100;
    private static final double ERROR_TOLERANCE = 1e-4; // 0.0001 units
    
    
    public static boolean solve(List<Constraint> constraints) {
        if (constraints == null || constraints.isEmpty()) {
            return true;
        }

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double totalError = 0;
            double maxError = 0;

            // In each iteration, ask each constraint to solve itself (adjust entities locally)
            for (Constraint c : constraints) {
                if (!c.isActive()) continue;
                
                double error = Math.abs(c.getError());
                if (error > maxError) {
                    maxError = error;
                }
                totalError += error;
                
                // Apply the correction for this constraint
                c.solve();
            }

            // If the maximum error is small enough, we have converged
            if (maxError < ERROR_TOLERANCE) {
                return true; // Converged
            }
        }

        return false; // Did not converge within max iterations
    }
}
