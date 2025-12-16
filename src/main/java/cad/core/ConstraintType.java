package cad.core;

/**
 * Enumeration of supported geometric constraint types.
 */
public enum ConstraintType {
    // Unary constraints (1 entity)
    HORIZONTAL,
    VERTICAL,
    FIXED, // Locks a point in space
    
    // Binary constraints (2 entities)
    COINCIDENT, // Two points are at the same location
    PARALLEL,
    PERPENDICULAR,
    DISTANCE, // Distance between two points or point-line
    EQUAL_LENGTH,
    
    // N-ary constraints
    SYMMETRIC
}
