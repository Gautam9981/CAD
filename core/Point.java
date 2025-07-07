package core;

import java.util.Objects;

/**
 * Represents a 2D point with immutable x and y coordinates.
 * Useful for geometric calculations, graphics, simulations, etc.
 */
public class Point {

    /** The x-coordinate of the point (immutable). */
    public final float x;

    /** The y-coordinate of the point (immutable). */
    public final float y;

    /**
     * Constructs a Point object with the specified x and y coordinates.
     *
     * @param x the x-coordinate of the point
     * @param y the y-coordinate of the point
     */
    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Checks whether this Point is equal to another object.
     * Two points are considered equal if their x and y coordinates
     * are within a small tolerance (EPSILON), to account for floating-point precision.
     *
     * @param obj the object to compare to
     * @return true if the other object is a Point with coordinates close to this one; false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        // Check for object identity
        if (this == obj) return true;

        // Check for null or incorrect type
        if (!(obj instanceof Point)) return false;

        // Cast and compare coordinates
        Point other = (Point) obj;
        final float EPSILON = 1e-6f;

        return Math.abs(this.x - other.x) < EPSILON &&
               Math.abs(this.y - other.y) < EPSILON;
    }

    /**
     * Returns a hash code value for the point.
     * Required to be consistent with equals() when using in hash-based collections.
     *
     * @return the hash code based on x and y
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Returns a string representation of the point.
     * The format is "(x.xxx, y.yyy)" with 3 decimal places of precision.
     *
     * @return the string representation of this Point
     */
    @Override
    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
