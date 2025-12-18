package cad.core;

import cad.core.Sketch.*;
import java.util.List;

public class MassProperties {
    private double area; // mm²
    private Point2D centroid; // Geometric center
    private double mass; // grams
    private Material material;
    private double thickness; // mm

    private MassProperties() {
    }

    public static MassProperties calculate(Sketch sketch, Material material, double thickness, UnitSystem unitSystem) {
        if (sketch == null || material == null) {
            return null;
        }

        MassProperties props = new MassProperties();
        props.material = material;
        props.thickness = thickness;

        // Calculate area and centroid in sketch units
        AreaCentroidResult result = calculateAreaAndCentroid(sketch.getEntities());
        if (result.area <= 0) {
            return null; // No closed shapes
        }

        props.area = result.area;
        props.centroid = result.centroid;

        // Convert to metric (mm) for mass calculation
        // Material density is always in kg/m³
        double areaInMm2 = convertAreaToMm2(props.area, unitSystem);
        double thicknessInMm = convertLengthToMm(thickness, unitSystem);

        // Calculate volume in mm³
        double volumeMm3 = areaInMm2 * thicknessInMm;

        // Convert to mass
        // Density in kg/m³ = g/mm³ * 1,000,000
        // densityGPerMm3 = kg/m³ / 1,000,000
        double densityGPerMm3 = material.getDensity() / 1_000_000.0;
        props.mass = volumeMm3 * densityGPerMm3;

        return props;
    }

    /**
     * Calculate mass properties for primitive shapes (cube, sphere) using
     * analytical formulas.
     * 
     * @param shape      The primitive shape type
     * @param param      Size (for cube) or radius (for sphere) in current units
     * @param material   Material definition
     * @param unitSystem Unit system for parameter conversion
     * @return MassProperties object or null if invalid
     */
    public static MassProperties calculateFromPrimitive(Geometry.Shape shape, float param, Material material,
            UnitSystem unitSystem) {
        if (shape == null || material == null || param <= 0) {
            return null;
        }

        MassProperties props = new MassProperties();
        props.material = material;
        props.thickness = 0; // Not applicable for 3D primitives

        // Convert parameter to mm
        double paramMm = convertLengthToMm(param, unitSystem);
        double volumeMm3;

        switch (shape) {
            case CUBE:
                // Volume = side³
                volumeMm3 = Math.pow(paramMm, 3);
                // For display purposes, treat as area × thickness
                props.area = paramMm * paramMm; // One face area
                props.thickness = paramMm; // Side length as "thickness"
                props.centroid = new Point2D(0, 0); // Centered at origin
                break;

            case SPHERE:
                // Volume = (4/3) × π × r³
                volumeMm3 = (4.0 / 3.0) * Math.PI * Math.pow(paramMm, 3);
                // For display, approximate as cross-sectional area × diameter
                props.area = Math.PI * paramMm * paramMm; // Cross-section
                props.thickness = 2 * paramMm; // Diameter
                props.centroid = new Point2D(0, 0); // Centered at origin
                break;

            default:
                return null; // Unsupported shape
        }

        // Calculate mass: volume (mm³) × density (g/mm³)
        double densityGPerMm3 = material.getDensity() / 1_000_000.0;
        props.mass = volumeMm3 * densityGPerMm3;

        return props;
    }

    private static double convertLengthToMm(double length, UnitSystem unitSystem) {
        return switch (unitSystem) {
            case MMGS -> length; // Already in mm
            case CMGS, CGS -> length * 10.0; // cm to mm
            case MKS, MGS -> length * 1000.0; // m to mm
            case IPS -> length * 25.4; // inches to mm
            case FTLBFS -> length * 304.8; // feet to mm
        };
    }

    private static double convertAreaToMm2(double area, UnitSystem unitSystem) {
        return switch (unitSystem) {
            case MMGS -> area; // Already in mm²
            case CMGS, CGS -> area * 100.0; // cm² to mm² (10²)
            case MKS, MGS -> area * 1_000_000.0; // m² to mm² (1000²)
            case IPS -> area * 645.16; // in² to mm² (25.4²)
            case FTLBFS -> area * 92903.04; // ft² to mm² (304.8²)
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

    // Getters

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
        return area * thickness;
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
