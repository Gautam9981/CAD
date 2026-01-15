package cad.core;

import cad.core.Sketch.*;
import java.util.List;

public class MassProperties {
    private double area;
    private Point2D centroid;
    private double mass;
    private Material material;
    private double thickness;
    private double volume; // Store actual volume (not area * thickness)

    private MassProperties() {
    }

    public static MassProperties calculate(Sketch sketch, Material material, double thickness, UnitSystem unitSystem) {
        if (sketch == null || material == null) {
            return null;
        }

        MassProperties props = new MassProperties();
        props.material = material;
        props.thickness = thickness;

        AreaCentroidResult result = calculateAreaAndCentroid(sketch.getEntities());
        if (result.area <= 0) {
            return null;
        }

        props.area = result.area;
        props.centroid = result.centroid;

        double areaInMm2 = convertAreaToMm2(props.area, unitSystem);
        double thicknessInMm = convertLengthToMm(thickness, unitSystem);

        double volumeMm3 = areaInMm2 * thicknessInMm;
        props.volume = volumeMm3; // Store volume for 2D sketches

        double densityGPerMm3 = material.getDensity() / 1_000_000.0;
        props.mass = volumeMm3 * densityGPerMm3;

        return props;
    }

    public static MassProperties calculateFromPrimitive(Geometry.Shape shape, float param, Material material,
            UnitSystem unitSystem) {
        if (shape == null || material == null || param <= 0) {
            return null;
        }

        MassProperties props = new MassProperties();
        props.material = material;
        props.thickness = 0;

        double paramMm = convertLengthToMm(param, unitSystem);
        double volumeMm3;

        switch (shape) {
            case CUBE:

                volumeMm3 = Math.pow(paramMm, 3);

                props.area = paramMm * paramMm;
                props.thickness = paramMm;
                props.volume = volumeMm3; // Store actual volume
                props.centroid = new Point2D(0, 0);
                break;

            case SPHERE:

                volumeMm3 = (4.0 / 3.0) * Math.PI * Math.pow(paramMm, 3);

                props.area = Math.PI * paramMm * paramMm;
                props.thickness = 2 * paramMm;
                props.volume = volumeMm3; // Store actual volume
                props.centroid = new Point2D(0, 0);
                break;

            default:
                return null;
        }

        double densityGPerMm3 = material.getDensity() / 1_000_000.0;
        props.mass = volumeMm3 * densityGPerMm3;

        return props;
    }

    public static MassProperties calculateFrom3DMesh(float volumeMm3, float surfaceAreaMm2, Material material,
            UnitSystem unitSystem) {
        if (material == null || volumeMm3 <= 0) {
            return null;
        }

        MassProperties props = new MassProperties();
        props.material = material;
        props.thickness = 0;
        props.area = surfaceAreaMm2;
        props.volume = volumeMm3; // Store actual volume for 3D meshes
        props.centroid = new Point2D(0, 0);

        double densityGPerMm3 = material.getDensity() / 1_000_000.0;
        props.mass = volumeMm3 * densityGPerMm3;

        return props;
    }

    private static double convertLengthToMm(double length, UnitSystem unitSystem) {
        return switch (unitSystem) {
            case MMGS -> length;
            case CMGS, CGS -> length * 10.0;
            case MKS, MGS -> length * 1000.0;
            case IPS -> length * 25.4;
            case FTLBFS -> length * 304.8;
        };
    }

    private static double convertAreaToMm2(double area, UnitSystem unitSystem) {
        return switch (unitSystem) {
            case MMGS -> area;
            case CMGS, CGS -> area * 100.0;
            case MKS, MGS -> area * 1_000_000.0;
            case IPS -> area * 645.16;
            case FTLBFS -> area * 92903.04;
        };
    }

    private static class AreaCentroidResult {
        double area;
        Point2D centroid;

        AreaCentroidResult(double area, Point2D centroid) {
            this.area = area;
            this.centroid = centroid;
        }
    }

    private static AreaCentroidResult calculateAreaAndCentroid(List<Entity> entities) {
        double totalArea = 0.0;
        double weightedCx = 0.0;
        double weightedCy = 0.0;

        for (Entity entity : entities) {
            if (entity instanceof Polygon) {
                Polygon poly = (Polygon) entity;
                double polyArea = calculatePolygonArea(poly);
                Point2D polyCentroid = calculatePolygonCentroid(poly);

                totalArea += polyArea;
                weightedCx += polyArea * polyCentroid.getX();
                weightedCy += polyArea * polyCentroid.getY();

            } else if (entity instanceof Circle) {
                Circle circle = (Circle) entity;
                double circleArea = Math.PI * circle.getRadius() * circle.getRadius();
                totalArea += circleArea;
                weightedCx += circleArea * circle.getX();
                weightedCy += circleArea * circle.getY();
            }
        }

        if (totalArea == 0) {
            return new AreaCentroidResult(0, new Point2D(0, 0));
        }

        Point2D centroid = new Point2D(
                (float) (weightedCx / totalArea),
                (float) (weightedCy / totalArea));

        return new AreaCentroidResult(totalArea, centroid);
    }

    private static double calculatePolygonArea(Polygon polygon) {
        List<Sketch.PointEntity> points = polygon.getSketchPoints();
        if (points.size() < 3) {
            return 0;
        }

        double area = 0.0;
        int n = points.size();

        for (int i = 0; i < n; i++) {
            Sketch.PointEntity p1 = points.get(i);
            Sketch.PointEntity p2 = points.get((i + 1) % n);
            area += p1.getX() * p2.getY() - p2.getX() * p1.getY();
        }

        return Math.abs(area / 2.0);
    }

    private static Point2D calculatePolygonCentroid(Polygon polygon) {
        List<Sketch.PointEntity> points = polygon.getSketchPoints();
        if (points.size() < 3) {
            return new Point2D(0, 0);
        }

        double cx = 0.0;
        double cy = 0.0;
        double signedArea = 0.0;
        int n = points.size();

        for (int i = 0; i < n; i++) {
            Sketch.PointEntity p1 = points.get(i);
            Sketch.PointEntity p2 = points.get((i + 1) % n);

            double cross = p1.getX() * p2.getY() - p2.getX() * p1.getY();
            signedArea += cross;
            cx += (p1.getX() + p2.getX()) * cross;
            cy += (p1.getY() + p2.getY()) * cross;
        }

        signedArea *= 0.5;
        cx /= (6.0 * signedArea);
        cy /= (6.0 * signedArea);

        return new Point2D((float) cx, (float) cy);
    }

    public double getArea() {
        return area;
    }

    public Point2D getCentroid() {
        return centroid;
    }

    public double getMass() {
        return mass;
    }

    public Material getMaterial() {
        return material;
    }

    public double getThickness() {
        return thickness;
    }

    public double getVolume() {
        // Return stored volume if available (for primitives and 3D meshes)
        // Otherwise calculate from area * thickness (for 2D extruded sketches)
        return volume > 0 ? volume : area * thickness;
    }

    @Override
    public String toString() {
        return String.format(
                "=== MASS PROPERTIES ===\n" +
                        "Material: %s (%.1f kg/m³)\n" +
                        "Thickness: %.2f mm\n" +
                        "Area: %.2f mm²\n" +
                        "Volume: %.2f mm³\n" +
                        "Mass: %.2f g\n" +
                        "Centroid: (%.2f, %.2f) mm",
                material.getName(),
                material.getDensity(),
                thickness,
                area,
                getVolume(),
                mass,
                centroid.getX(),
                centroid.getY());
    }

    public String toDisplayString() {
        return String.format(
                "Material: %s\n" +
                        "Density: %.1f kg/m³\n" +
                        "Thickness: %.2f mm\n\n" +
                        "Area: %.2f mm²\n" +
                        "Volume: %.2f mm³\n" +
                        "Mass: %.2f g\n\n" +
                        "Centroid:\n" +
                        "  X: %.2f mm\n" +
                        "  Y: %.2f mm",
                material.getName(),
                material.getDensity(),
                thickness,
                area,
                getVolume(),
                mass,
                centroid.getX(),
                centroid.getY());
    }
}
