package cad.gui;

import com.jogamp.opengl.*;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for managing VBOs for Face3D geometry.
 */
public class VBOManager {
    private int vboHandle = 0;
    private int vertexCount = 0;

    public void uploadFaces(GL2 gl, List<? extends Object> faces) {
        // Flatten all Face3D vertices into a float array
        List<Float> verts = new ArrayList<>();
        for (Object faceObj : faces) {
            // Face3D is an inner class of Sketch
            List<?> vertices;
            try {
                vertices = (List<?>) faceObj.getClass().getMethod("getVertices").invoke(faceObj);
            } catch (Exception e) {
                continue;
            }
            for (Object v : vertices) {
                float x, y, z;
                try {
                    x = (float) v.getClass().getMethod("getX").invoke(v);
                    y = (float) v.getClass().getMethod("getY").invoke(v);
                    z = (float) v.getClass().getMethod("getZ").invoke(v);
                } catch (Exception e) {
                    continue;
                }
                verts.add(x); verts.add(y); verts.add(z);
            }
        }
        vertexCount = verts.size() / 3;
        FloatBuffer buffer = FloatBuffer.allocate(verts.size());
        for (Float f : verts) buffer.put(f);
        buffer.rewind();
        if (vboHandle == 0) {
            int[] handles = new int[1];
            gl.glGenBuffers(1, handles, 0);
            vboHandle = handles[0];
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboHandle);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void draw(GL2 gl) {
        if (vboHandle == 0 || vertexCount == 0) return;
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboHandle);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertexCount);
        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void dispose(GL2 gl) {
        if (vboHandle != 0) {
            int[] handles = {vboHandle};
            gl.glDeleteBuffers(1, handles, 0);
            vboHandle = 0;
        }
    }
}
