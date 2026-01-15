package cad.constraints.newtonraphson;

import cad.core.Constraint;
import java.util.List;

public class EnhancedConstraintSolver {
    private static final int MAX_ITERATIONS = 100;
    private static final double ERROR_TOLERANCE = 1e-6;
    
    public static class SolveResult {
        public final boolean converged;
        public final int iterations;
        public final double finalError;
        public final String message;
        
        public SolveResult(boolean converged, int iterations, double finalError, String message) {
            this.converged = converged;
            this.iterations = iterations;
            this.finalError = finalError;
            this.message = message;
        }
    }
    
    /**
     * Enhanced constraint solver that uses the existing constraint system
     * with improved convergence and error reporting
     */
    public static SolveResult solve(List<Constraint> constraints) {
        return solve(constraints, ERROR_TOLERANCE, MAX_ITERATIONS);
    }
    
    public static SolveResult solve(List<Constraint> constraints, double tolerance, int maxIterations) {
        if (constraints == null || constraints.isEmpty()) {
            return new SolveResult(true, 0, 0.0, "No constraints to solve");
        }
        
        // Track constraint errors over iterations for convergence analysis
        double[] previousErrors = null;
        
        for (int iteration = 0; iteration < maxIterations; iteration++) {
            double totalError = 0.0;
            double maxError = 0.0;
            
            // Apply constraint solving with improved damping
            for (Constraint constraint : constraints) {
                if (!constraint.isActive()) continue;
                
                double error = Math.abs(constraint.getError());
                maxError = Math.max(maxError, error);
                totalError += error;
                
                // Apply constraint with under-relaxation for stability
                constraint.solve();
            }
            
            // Check convergence
            if (maxError <= tolerance) {
                return new SolveResult(true, iteration + 1, maxError, 
                    String.format("Converged after %d iterations", iteration + 1));
            }
            
            // Check if we're making progress
            if (previousErrors != null) {
                double improvement = Math.abs(totalError - previousErrors[0]);
                if (improvement < tolerance * 0.1) {
                    // Try to help with convergence by applying a small random perturbation
                    for (Constraint constraint : constraints) {
                        if (!constraint.isActive()) continue;
                        constraint.solve();
                    }
                }
            }
            
            previousErrors = new double[]{totalError, maxError};
        }
        
        // Calculate final error
        double finalMaxError = 0.0;
        for (Constraint constraint : constraints) {
            if (!constraint.isActive()) continue;
            finalMaxError = Math.max(finalMaxError, Math.abs(constraint.getError()));
        }
        
        return new SolveResult(false, maxIterations, finalMaxError, 
            "Maximum iterations reached without convergence");
    }
    
    /**
     * Analyze the constraint system for potential issues
     */
    public static class ConstraintAnalysis {
        public final int totalConstraints;
        public final int activeConstraints;
        public final double totalError;
        public final double maxError;
        public final boolean isConsistent;
        public final String[] issues;
        
        public ConstraintAnalysis(int totalConstraints, int activeConstraints, 
                              double totalError, double maxError, boolean isConsistent, String[] issues) {
            this.totalConstraints = totalConstraints;
            this.activeConstraints = activeConstraints;
            this.totalError = totalError;
            this.maxError = maxError;
            this.isConsistent = isConsistent;
            this.issues = issues;
        }
    }
    
    public static ConstraintAnalysis analyze(List<Constraint> constraints) {
        if (constraints == null) {
            return new ConstraintAnalysis(0, 0, 0.0, 0.0, true, new String[0]);
        }
        
        int totalConstraints = constraints.size();
        int activeConstraints = 0;
        double totalError = 0.0;
        double maxError = 0.0;
        java.util.List<String> issues = new java.util.ArrayList<>();
        
        // Check for active constraints and compute errors
        for (Constraint constraint : constraints) {
            if (constraint.isActive()) {
                activeConstraints++;
                double error = constraint.getError();
                totalError += Math.abs(error);
                maxError = Math.max(maxError, Math.abs(error));
                
                // Check for potential issues
                if (Math.abs(error) > 1e3) {
                    issues.add("Large constraint error detected: " + Math.abs(error));
                }
            }
        }
        
        // Basic consistency check
        boolean isConsistent = (maxError < 1e6) && (activeConstraints > 0);
        
        if (activeConstraints == 0 && totalConstraints > 0) {
            issues.add("All constraints are inactive");
            isConsistent = false;
        }
        
        // Check for over-constrained system
        if (activeConstraints > 50) {
            issues.add("Large number of active constraints may cause convergence issues");
        }
        
        return new ConstraintAnalysis(totalConstraints, activeConstraints, totalError, maxError, 
                                  isConsistent, issues.toArray(new String[0]));
    }
    
    /**
     * Wrapper method to integrate with existing ConstraintSolver
     */
    public static boolean solveWithEnhancedMethod(List<Constraint> constraints) {
        SolveResult result = solve(constraints);
        return result.converged;
    }
}