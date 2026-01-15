package cad.core;

import cad.core.Sketch.Line;
import cad.core.Sketch.Circle;

public class EqualConstraint extends Constraint {
    private Object entity1;
    private Object entity2;

    public EqualConstraint(Object entity1, Object entity2) {
        super(ConstraintType.EQUAL_LENGTH);
        this.entity1 = entity1;
        this.entity2 = entity2;
    }

    @Override
    public double getError() {
        if (entity1 instanceof Line && entity2 instanceof Line) {
            return getLineLengthError((Line) entity1, (Line) entity2);
        } else if (entity1 instanceof Circle && entity2 instanceof Circle) {
            return getCircleRadiusError((Circle) entity1, (Circle) entity2);
        }
        return 0;
    }

    private double getLineLengthError(Line l1, Line l2) {
        double len1 = Math.sqrt(Math.pow(l1.getX2() - l1.getX1(), 2) + Math.pow(l1.getY2() - l1.getY1(), 2));
        double len2 = Math.sqrt(Math.pow(l2.getX2() - l2.getX1(), 2) + Math.pow(l2.getY2() - l2.getY1(), 2));
        return Math.abs(len1 - len2);
    }

    private double getCircleRadiusError(Circle c1, Circle c2) {
        return Math.abs(c1.getRadius() - c2.getRadius());
    }

    @Override
    public void solve() {
        if (entity1 instanceof Line && entity2 instanceof Line) {
            solveLineLength((Line) entity1, (Line) entity2);
        } else if (entity1 instanceof Circle && entity2 instanceof Circle) {
            solveCircleRadius((Circle) entity1, (Circle) entity2);
        }
    }

    private void solveLineLength(Line l1, Line l2) {
        double len1 = Math.sqrt(Math.pow(l1.getX2() - l1.getX1(), 2) + Math.pow(l1.getY2() - l1.getY1(), 2));
        double len2 = Math.sqrt(Math.pow(l2.getX2() - l2.getX1(), 2) + Math.pow(l2.getY2() - l2.getY1(), 2));
        double avgLen = (len1 + len2) / 2.0;

        if (Math.abs(len1 - avgLen) > 1e-9) {
            adjustLineLength(l1, avgLen, len1);
        }
        if (Math.abs(len2 - avgLen) > 1e-9) {
            adjustLineLength(l2, avgLen, len2);
        }
    }

    private void adjustLineLength(Line line, double targetLen, double currentLen) {
        if (currentLen < 1e-9)
            return;
        double cx = (line.getX1() + line.getX2()) / 2.0;
        double cy = (line.getY1() + line.getY2()) / 2.0;

        double scale = targetLen / currentLen;
        double dx = (line.getX2() - line.getX1()) * 0.5 * scale;
        double dy = (line.getY2() - line.getY1()) * 0.5 * scale;

        line.setStart((float) (cx - dx), (float) (cy - dy));
        line.setEnd((float) (cx + dx), (float) (cy + dy));
    }

    private void solveCircleRadius(Circle c1, Circle c2) {
        double avgRadius = (c1.getRadius() + c2.getRadius()) / 2.0;
        c1.setRadius((float) avgRadius);
        c2.setRadius((float) avgRadius);
    }
}
