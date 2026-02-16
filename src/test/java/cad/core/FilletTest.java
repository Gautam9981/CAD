package cad.core;
import org.junit.Test;
import static org.junit.Assert.*;
import eu.mihosoft.jcsg.CSG;
import eu.mihosoft.jcsg.Cube;
import java.util.List;
public class FilletTest {
    @Test
    public void testPickEdge() {
        Geometry.createCube(10.0f, 1, Geometry.BooleanOp.NONE);
        float[] rayOrigin = { 0, 10, 5 };
        float[] rayDir = { 0, -1, 0 };
        rayOrigin = new float[] { 0, 10, 4 };
        rayDir = new float[] { 0, -1, 0 };
        float[] edge = Geometry.pickEdge(rayOrigin, rayDir);
        assertNotNull("Edge should be picked", edge);
        boolean match1 = (Math.abs(edge[1] - 5) < 0.01 && Math.abs(edge[4] - 5) < 0.01);
        assertTrue("Y coordinate should be 5", match1);
    }
    @Test
    public void testFilletEdge() {
        Geometry.createCube(10.0f, 1, Geometry.BooleanOp.NONE);
        int initialTris = Geometry.getExtrudedTriangles().size();
        float[] edge = { -5, 5, 5, 5, 5, 5 };
        Geometry.filletEdge(edge, 1.0f);
        int finalTris = Geometry.getExtrudedTriangles().size();
        assertNotEquals("Triangle count should change after fillet", initialTris, finalTris);
        assertTrue("Triangle count should increase", finalTris > initialTris);
    }
}
