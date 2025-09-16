package cad.core;

/**
 * Represents a 2D point entity with x and y coordinates.
 */
public class PointEntity {
    /** The x-coordinate of the point */
    public float x;

    /** The y-coordinate of the point */
    public float y;

    /**
     * Constructs a PointEntity with specified x and y coordinates.
     * 
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public PointEntity(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns a string representation of the PointEntity.
     * 
     * @return A string in the format PointEntity(x, y)
     */
    @Override
    public String toString() {
        return "PointEntity(" + x + ", " + y + ")";
    }
}
