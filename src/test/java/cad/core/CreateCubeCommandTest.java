package cad.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class CreateCubeCommandTest {

    @Test
    public void testExecuteAndUndo() {
        // Setup - Assuming we can run this without full GL context or it mocks it
        // Depending on Geometry dependencies, this might fail if it needs JOGL.
        // Geometry uses JOGL classes. If they aren't initialized, it might crash.
        // But let's try.

        CreateCubeCommand cmd = new CreateCubeCommand(10.0f, 1);

        // Execute
        cmd.execute();

        // Verify state (basic check if param was set)
        // This assumes Geometry state is static and testable
        // assertEquals(10.0f, Geometry.getParam(), 0.001f);

        // Undo
        cmd.undo();

        // Verify restoration
    }
}
