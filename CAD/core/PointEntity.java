package core;

public class PointEntity {
    public float x;
    public float y;

    public PointEntity(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "PointEntity(" + x + ", " + y + ")";
    }
}
