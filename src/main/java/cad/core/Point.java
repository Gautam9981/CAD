package core;

import java.util.Objects;

/**
 * Represents a 2D point with immutable x and y coordinates.
 * Useful for geometric computations and sketch representations.
 */
public class Point {
    public final float x;
    public final float y;

    /**
     * Constructs a Point with specified x and y coordinates.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Checks whether another object is equal to this point.
     * Uses a small epsilon for floating-point comparison.
     *
     * @param obj The object to compare
     * @return true if obj is a Point with approximately equal coordinates
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point)) return false;
        Point other = (Point) obj;
        final float EPSILON = 1e-6f;
        return Math.abs(this.x - other.x) < EPSILON &&
               Math.abs(this.y - other.y) < EPSILON;
    }

    /**
     * Generates a hash code based on the x and y coordinates.
     *
     * @return The hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Returns a string representation of the point in (x, y) format.
     *
     * @return Formatted string of the point
     */
    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
