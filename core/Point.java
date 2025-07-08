package core;
import java.util.Objects;

public class Point {
    public final float x;
    public final float y;

    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point)) return false;
        Point other = (Point) obj;
        final float EPSILON = 1e-6f;
        return Math.abs(this.x - other.x) < EPSILON && Math.abs(this.y - other.y) < EPSILON;
    }

    public int hashCode() {
        return Objects.hash(x, y);
    }

    public String toString() {
        return String.format("(%.3f, %.3f)", x, y);
    }
}
