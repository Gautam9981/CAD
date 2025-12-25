package cad.gui;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class VBOManager {
    private int vboHandle = 0;
    private int normalVboHandle = 0;
    private int vertexCount = 0;

    public void uploadFaces(GL2 gl, GLU glu, List<? extends Object> faces) {
        List<Float> verts = new ArrayList<>();
        List<Float> norms = new ArrayList<>();

        GLUtessellator tess = glu.gluNewTess();
        TessCallback callback = new TessCallback(verts, norms);
        glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, callback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX_DATA, callback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_END, callback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, callback);

        for (Object faceObj : faces) {
            List<?> vertices;
            List<?> normals = null;
            try {
                vertices = (List<?>) faceObj.getClass().getMethod("getVertices").invoke(faceObj);
                try {
                    normals = (List<?>) faceObj.getClass().getMethod("getVertexNormals").invoke(faceObj);
                } catch (Exception e) {
                }
            } catch (Exception e) {
                continue;
            }

            if (vertices.size() < 3)
                continue;

            if (vertices.size() == 3) {
                
                for (int i = 0; i < 3; i++) {
                    addVertexData(verts, norms, vertices.get(i), (normals != null) ? normals.get(i) : null);
                }
            } else if (vertices.size() == 4) {
                
                
                addVertexData(verts, norms, vertices.get(0), (normals != null) ? normals.get(0) : null);
                addVertexData(verts, norms, vertices.get(1), (normals != null) ? normals.get(1) : null);
                addVertexData(verts, norms, vertices.get(2), (normals != null) ? normals.get(2) : null);
                
                addVertexData(verts, norms, vertices.get(0), (normals != null) ? normals.get(0) : null);
                addVertexData(verts, norms, vertices.get(2), (normals != null) ? normals.get(2) : null);
                addVertexData(verts, norms, vertices.get(3), (normals != null) ? normals.get(3) : null);
            } else {
                
                callback.currentNormals = (List<float[]>) normals;
                callback.currentVertices = (List<Object>) vertices;

                glu.gluTessBeginPolygon(tess, null);
                glu.gluTessBeginContour(tess);

                for (int i = 0; i < vertices.size(); i++) {
                    Object v = vertices.get(i);
                    double[] coords = getCoords(v);
                    
                    glu.gluTessVertex(tess, coords, 0, Integer.valueOf(i));
                }

                glu.gluTessEndContour(tess);
                glu.gluTessEndPolygon(tess);
            }
        }
        glu.gluDeleteTess(tess);

        vertexCount = verts.size() / 3;
        
        FloatBuffer buffer = FloatBuffer.allocate(verts.size());
        for (Float f : verts)
            buffer.put(f);
        buffer.rewind();
        if (vboHandle == 0) {
            int[] handles = new int[1];
            gl.glGenBuffers(1, handles, 0);
            vboHandle = handles[0];
        }
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboHandle);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.capacity() * 4, buffer, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        
        FloatBuffer normalBuffer = FloatBuffer.allocate(norms.size());
        for (Float n : norms)
            normalBuffer.put(n);
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

    private void addVertexData(List<Float> verts, List<Float> norms, Object v, Object nObj) {
        double[] c = getCoords(v);
        verts.add((float) c[0]);
        verts.add((float) c[1]);
        verts.add((float) c[2]);
        if (nObj != null) {
            float[] n = (float[]) nObj;
            norms.add(n[0]);
            norms.add(n[1]);
            norms.add(n[2]);
        } else {
            norms.add(0f);
            norms.add(0f);
            norms.add(1f);
        }
    }

    private double[] getCoords(Object v) {
        try {
            float x = (float) v.getClass().getMethod("getX").invoke(v);
            float y = (float) v.getClass().getMethod("getY").invoke(v);
            float z = (float) v.getClass().getMethod("getZ").invoke(v);
            return new double[] { x, y, z };
        } catch (Exception e) {
            return new double[] { 0, 0, 0 };
        }
    }

    private class TessCallback extends com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter {
        private List<Float> verts;
        private List<Float> norms;
        public List<float[]> currentNormals;
        public List<Object> currentVertices;

        public TessCallback(List<Float> verts, List<Float> norms) {
            this.verts = verts;
            this.norms = norms;
        }

        @Override
        public void vertexData(Object vertexData, Object polygonData) {
            if (vertexData instanceof Integer) {
                int index = (Integer) vertexData;
                Object v = currentVertices.get(index);
                Object n = (currentNormals != null && index < currentNormals.size()) ? currentNormals.get(index) : null;
                addVertexData(verts, norms, v, n);
            }
        }

        @Override
        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
            
            outData[0] = data[0];
        }
    }

    public void draw(GL2 gl) {
        if (vboHandle == 0 || normalVboHandle == 0 || vertexCount == 0)
            return;
        
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboHandle);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        
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
            int[] handles = { vboHandle };
            gl.glDeleteBuffers(1, handles, 0);
            vboHandle = 0;
        }
        if (normalVboHandle != 0) {
            int[] handles = { normalVboHandle };
            gl.glDeleteBuffers(1, handles, 0);
            normalVboHandle = 0;
        }
    }
}
