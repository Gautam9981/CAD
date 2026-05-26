package cad.core;

import org.junit.Test;
import static org.junit.Assert.*;

import cad.topology.*;
import cad.features.extrusion.LinearSweepFeature;
import cad.math.Vector3d;
import java.util.List;

public class KernelIntegrationTest {

    @Test
    public void testSketchClosure() {
        Sketch sketch = new Sketch();
        
        // Add lines to form a square: (0,0) -> (10,0) -> (10,10) -> (0,10) -> (0,0)
        sketch.addLine(0, 0, 10, 0);
        sketch.addLine(10, 0, 10, 10);
        sketch.addLine(10, 10, 0, 10);
        
        // At this point, it is open
        assertFalse("Sketch should be detected as open", sketch.isClosedLoop());
        
        sketch.addLine(0, 10, 0, 0);
        
        // Now it is closed
        assertTrue("Sketch should be detected as closed loop", sketch.isClosedLoop());
    }

    @Test
    public void testBRepEulerCharacteristic() throws Exception {
        Sketch sketch = new Sketch();
        
        // Form a square: (0,0) -> (10,0) -> (10,10) -> (0,10) -> (0,0)
        sketch.addLine(0, 0, 10, 0);
        sketch.addLine(10, 0, 10, 10);
        sketch.addLine(10, 10, 0, 10);
        sketch.addLine(0, 10, 0, 0);
        
        LinearSweepFeature extrude = new LinearSweepFeature(sketch, 10.0, Vector3d.Z_AXIS);
        BRepBody body = extrude.generate();
        
        // Should not throw TopologyException
        body.validateTopology();
        
        // Extruded square has 8 vertices, 12 edges, 6 faces
        assertEquals("Vertices should be 8", 8, body.getVertices().size());
        assertEquals("Edges should be 12", 12, body.getEdges().size());
        assertEquals("Faces should be 6", 6, body.getFaces().size());
        
        // Chi = V - E + F = 8 - 12 + 6 = 2
        assertTrue("Euler Characteristic must be valid", body.validateEulerCharacteristic());
    }
    
    @Test
    public void testErrorHandling() {
        Sketch sketch = new Sketch();
        
        // Open sketch
        sketch.addLine(0, 0, 10, 0);
        sketch.addLine(10, 0, 10, 10);
        
        LinearSweepFeature extrude = new LinearSweepFeature(sketch, 10.0, Vector3d.Z_AXIS);
        
        try {
            extrude.generate();
            fail("Expected TopologyException due to open sketch");
        } catch (TopologyException e) {
            // Success
            assertEquals("Sketch must be closed for extrusion", e.getMessage());
        }
    }
    
    @Test(timeout = 500)
    public void testPerformanceExtrusion() throws Exception {
        Sketch sketch = new Sketch();
        
        // Create a highly segmented circle (100 sides)
        int segments = 100;
        double radius = 10.0;
        for (int i = 0; i < segments; i++) {
            double angle1 = 2 * Math.PI * i / segments;
            double angle2 = 2 * Math.PI * (i + 1) / segments;
            
            float x1 = (float)(radius * Math.cos(angle1));
            float y1 = (float)(radius * Math.sin(angle1));
            float x2 = (float)(radius * Math.cos(angle2));
            float y2 = (float)(radius * Math.sin(angle2));
            
            sketch.addLine(x1, y1, x2, y2);
        }
        
        LinearSweepFeature extrude = new LinearSweepFeature(sketch, 50.0, Vector3d.Z_AXIS);
        BRepBody body = extrude.generate();
        
        body.validateTopology();
        
        // V = 200, E = 300, F = 102 (100 sides + 2 caps)
        assertEquals(segments * 2, body.getVertices().size());
        assertEquals(segments * 3, body.getEdges().size());
        assertEquals(segments + 2, body.getFaces().size());
    }
}
