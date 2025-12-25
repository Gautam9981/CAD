import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import cad.core.Geometry;
import cad.core.Geometry.BooleanOp;
import cad.core.Geometry.Shape;

/**
 * Test suite to verify JCSG kernel refactoring works correctly.
 */
public class GeometryJCSGTest {

    @Before
    public void setUp() {
        // Tests start fresh each time
    }

    @Test
    public void testCubeCreation() {
        System.out.println("Testing cube creation...");
        Geometry.createCube(2.0f, 1, BooleanOp.NONE);

        // Verify shape was created
        assertEquals("Shape should be CSG_RESULT", Shape.CSG_RESULT, Geometry.getCurrentShape());
        assertEquals("Primitive type should be CUBE", Shape.CUBE, Geometry.getPrimitiveShapeType());

        // Verify triangles were generated
        assertTrue("Should have extruded triangles",
                Geometry.getExtrudedTriangles().size() > 0);

        System.out.println("✓ Cube created: " + Geometry.getExtrudedTriangles().size() + " triangles");
    }

    @Test
    public void testSphereCreation() {
        System.out.println("Testing sphere creation...");
        Geometry.createSphere(1.5f, 20, 20, BooleanOp.NONE);

        assertEquals("Shape should be CSG_RESULT", Shape.CSG_RESULT, Geometry.getCurrentShape());
        assertEquals("Primitive type should be SPHERE", Shape.SPHERE, Geometry.getPrimitiveShapeType());
        assertTrue("Should have extruded triangles",
                Geometry.getExtrudedTriangles().size() > 0);

        System.out.println("✓ Sphere created: " + Geometry.getExtrudedTriangles().size() + " triangles");
    }

    @Test
    public void testBooleanUnion() {
        System.out.println("Testing boolean union...");

        // Create first cube
        Geometry.createCube(2.0f, 1, BooleanOp.NONE);
        int cubeTriangles = Geometry.getExtrudedTriangles().size();
        System.out.println("  First cube: " + cubeTriangles + " triangles");

        // Add second cube with union
        Geometry.createCube(2.0f, 1, BooleanOp.UNION);
        int unionTriangles = Geometry.getExtrudedTriangles().size();
        System.out.println("  After union: " + unionTriangles + " triangles");

        // Union should produce geometry (exact count depends on overlap)
        assertTrue("Union should produce triangles", unionTriangles > 0);

        System.out.println("✓ Boolean union works");
    }

    @Test
    public void testBooleanDifference() {
        System.out.println("Testing boolean difference...");

        // Create base cube
        Geometry.createCube(3.0f, 1, BooleanOp.NONE);
        int cubeTriangles = Geometry.getExtrudedTriangles().size();
        System.out.println("  Base cube: " + cubeTriangles + " triangles");

        // Subtract smaller sphere
        Geometry.createSphere(1.0f, 20, 20, BooleanOp.DIFFERENCE);
        int diffTriangles = Geometry.getExtrudedTriangles().size();
        System.out.println("  After difference: " + diffTriangles + " triangles");

        // Should still have geometry after subtraction
        assertTrue("Difference should produce triangles", diffTriangles > 0);

        System.out.println("✓ Boolean difference works");
    }

    @Test
    public void testBooleanIntersection() {
        System.out.println("Testing boolean intersection...");

        // Create cube
        Geometry.createCube(2.0f, 1, BooleanOp.NONE);

        // Intersect with sphere
        Geometry.createSphere(1.5f, 20, 20, BooleanOp.INTERSECTION);
        int intersectTriangles = Geometry.getExtrudedTriangles().size();
        System.out.println("  After intersection: " + intersectTriangles + " triangles");

        // Intersection should produce geometry
        assertTrue("Intersection should produce triangles", intersectTriangles > 0);

        System.out.println("✓ Boolean intersection works");
    }

    @Test
    public void testSubdividedCube() {
        System.out.println("Testing subdivided cube...");

        // Create cube with multiple divisions
        Geometry.createCube(2.0f, 3, BooleanOp.NONE);
        int triangles = Geometry.getExtrudedTriangles().size();

        // 3 divisions per face = 9 quads per face = 18 triangles per face
        // 6 faces = 108 triangles expected
        System.out.println("  Subdivided cube: " + triangles + " triangles");
        assertTrue("Subdivided cube should have more triangles", triangles >= 100);

        System.out.println("✓ Cube subdivision works");
    }

    @Test
    public void testGeometryBounds() {
        System.out.println("Testing geometry bounds...");

        // Create a known-size cube
        Geometry.createCube(4.0f, 1, BooleanOp.NONE);
        float maxDim = Geometry.getModelMaxDimension();

        System.out.println("  Max dimension: " + maxDim);

        // Should be close to 4.0 (the cube size)
        assertTrue("Max dimension should be reasonable", maxDim > 3.9f && maxDim < 4.1f);

        System.out.println("✓ Geometry bounds calculation works");
    }
}
