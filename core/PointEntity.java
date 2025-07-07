package core;

/**
 * A mutable 2D point entity with public x and y coordinates.
 * Suitable for use cases where the point's position may change over time,
 * such as in simulations, games, or graphical objects.
 */
public class PointEntity {

    /** The x-coordinate of the point. Can be modified directly. */
    public float x;

    /** The y-coordinate of the point. Can be modified directly. */
    public float y;

    /**
     * Constructs a PointEntity with the specified coordinates.
     *
     * @param x the initial x-coordinate
     * @param y the initial y-coordinate
     */
    public PointEntity(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns a string representation of this PointEntity.
     * Format: PointEntity(x, y)
     *
     * @return a string describing the current coordinates of this point
     */
    @Override
    public String toString() {
        return "PointEntity(" + x + ", " + y + ")";
    }
}
