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
    private int normalVboHandle = 0;
    private int vertexCount = 0;

    public void uploadFaces(GL2 gl, List<? extends Object> faces) {
        // Flatten all Face3D vertices and normals into float arrays
        List<Float> verts = new ArrayList<>();
        List<Float> norms = new ArrayList<>();
        for (Object faceObj : faces) {
            List<?> vertices;
            List<?> normals = null;
            try {
                vertices = (List<?>) faceObj.getClass().getMethod("getVertices").invoke(faceObj);
                // Try to get normals if available
                try {
                    normals = (List<?>) faceObj.getClass().getMethod("getVertexNormals").invoke(faceObj);
                } catch (Exception e) {
                    // Normals not present, fallback to (0,0,1)
                }
            } catch (Exception e) {
                continue;
            }
            for (int i = 0; i < vertices.size(); i++) {
                Object v = vertices.get(i);
                float x, y, z;
                try {
                    x = (float) v.getClass().getMethod("getX").invoke(v);
                    y = (float) v.getClass().getMethod("getY").invoke(v);
                    z = (float) v.getClass().getMethod("getZ").invoke(v);
                } catch (Exception e) {
                    continue;
                }
                verts.add(x); verts.add(y); verts.add(z);
                // Normals
                if (normals != null && normals.size() == vertices.size()) {
                    float[] n = (float[]) normals.get(i);
                    norms.add(n[0]); norms.add(n[1]); norms.add(n[2]);
                } else {
                    norms.add(0f); norms.add(0f); norms.add(1f);
                }
            }
        }
        vertexCount = verts.size() / 3;
        // Vertex buffer
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

        // Normal buffer
        FloatBuffer normalBuffer = FloatBuffer.allocate(norms.size());
        for (Float n : norms) normalBuffer.put(n);
        normalBuffer.rewind();
        if (normalVboHandle == 0) {
            int[] handles = new int[1];
            gl.glGenBuffers(1, handles, 0);
            normalVboHandle = handles[0];
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, normalVboHandle);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, normalBuffer.capacity() * 4, normalBuffer, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void draw(GL2 gl) {
        if (vboHandle == 0 || normalVboHandle == 0 || vertexCount == 0) return;
        // Vertex array
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboHandle);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        // Normal array
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, normalVboHandle);
        gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glNormalPointer(GL.GL_FLOAT, 0, 0);

        gl.glDrawArrays(GL2.GL_TRIANGLES, 0, vertexCount);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    public void dispose(GL2 gl) {
        if (vboHandle != 0) {
            int[] handles = {vboHandle};
            gl.glDeleteBuffers(1, handles, 0);
            vboHandle = 0;
        }
        if (normalVboHandle != 0) {
            int[] handles = {normalVboHandle};
            gl.glDeleteBuffers(1, handles, 0);
            normalVboHandle = 0;
        }
    }
}
