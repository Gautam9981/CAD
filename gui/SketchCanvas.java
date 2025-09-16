package cad.gui;

import com.jogamp.graph.geom.Triangle;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;


/**
 * OpenGL canvas supporting both 2D sketch rendering and smooth-shaded 3D STL display.
 * Provides mouse/keyboard controls for pan, zoom, and rotation with averaged normals for STL.
 */
public class SketchCanvas extends GLJPanel implements GLEventListener {
    private final cad.core.Sketch sketch;

    // Each triangle is float[9] (3 vertices)
    private List<float[]> stlTriangles = new ArrayList<>();

    // Map of vertex position string to its averaged normal
    private Map<String, float[]> vertexNormals = new HashMap<>();

    private boolean showStl = false;
    private double offsetX = 0, offsetY = 0;
    private double zoom = 1.0;
    private double rotateX = 0, rotateY = 0;

    /**
     * Creates sketch canvas with mouse/keyboard controls for 2D/3D interaction.
     * @param sketch 2D sketch object for rendering
     */
    public SketchCanvas(cad.core.Sketch sketch) {
        super(new GLCapabilities(GLProfile.getDefault()));
        this.sketch = sketch;

        setPreferredSize(new Dimension(800, 600));
        addGLEventListener(this);

        // --- Mouse-based Pan ---
        MouseAdapter mouse = new MouseAdapter() {
            private int lastX, lastY;
            @Override public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
            @Override public void mouseDragged(MouseEvent e) {
                offsetX += (e.getX() - lastX);
                offsetY += (e.getY() - lastY);
                lastX = e.getX(); lastY = e.getY();
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        // --- Scroll Zoom ---
        addMouseWheelListener(e -> {
            zoom *= (e.getPreciseWheelRotation() < 0) ? 1.1 : 0.9;
            repaint();
        });

        // --- Arrow Keys for Rotate, R to Reset ---
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> { rotateY -= 0.1; repaint(); }
                    case KeyEvent.VK_RIGHT -> { rotateY += 0.1; repaint(); }
                    case KeyEvent.VK_UP -> { rotateX -= 0.1; repaint(); }
                    case KeyEvent.VK_DOWN -> { rotateX += 0.1; repaint(); }
                    case KeyEvent.VK_R -> { resetView(); repaint(); }
                }
            }
        });
    }

    /**
     * Accepts a list of STL triangles and computes averaged normals for smooth shading.
     */
    public void setStlTriangles(List<float[]> triangles) {
        stlTriangles.clear();
        vertexNormals.clear();

        if (triangles == null || triangles.isEmpty()) {
            showStl = false;
            repaint();
            return;
        }

        // --- Normalize geometry ---
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (float[] tri : triangles) {
            for (int i = 0; i < 3; i++) {
                float x = tri[i * 3], y = tri[i * 3 + 1], z = tri[i * 3 + 2];
                minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
            }
        }

        float cx = (minX + maxX) / 2f;
        float cy = (minY + maxY) / 2f;
        float cz = (minZ + maxZ) / 2f;
        float scale = 2.0f / Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);

        // --- Normalize vertices and compute normals ---
        for (float[] tri : triangles) {
            float[] norm = new float[9];
            float[] v1 = new float[3], v2 = new float[3], v3 = new float[3];

            for (int i = 0; i < 3; i++) {
                float x = (tri[i * 3]     - cx) * scale;
                float y = (tri[i * 3 + 1] - cy) * scale;
                float z = (tri[i * 3 + 2] - cz) * scale;
                norm[i * 3] = x;
                norm[i * 3 + 1] = y;
                norm[i * 3 + 2] = z;
                if (i == 0) { v1[0] = x; v1[1] = y; v1[2] = z; }
                if (i == 1) { v2[0] = x; v2[1] = y; v2[2] = z; }
                if (i == 2) { v3[0] = x; v3[1] = y; v3[2] = z; }
            }
            stlTriangles.add(norm);

            float[] normal = computeNormal(v1, v2, v3);
            for (float[] v : new float[][]{v1, v2, v3}) {
                String key = v[0] + "," + v[1] + "," + v[2];
                vertexNormals.merge(key, normal.clone(), (a, b) -> {
                    a[0] += b[0]; a[1] += b[1]; a[2] += b[2]; return a;
                });
            }
        }

        // --- Normalize summed normals ---
        for (Map.Entry<String, float[]> entry : vertexNormals.entrySet()) {
            float[] n = entry.getValue();
            float len = (float) Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
            if (len != 0) {
                n[0] /= len;
                n[1] /= len;
                n[2] /= len;
            }
        }

        showStl = true;
        resetView();
        repaint();
    }

    /** Switches back to 2D sketch mode */
    public void showSketch() {
        this.showStl = false;
        repaint();
    }

    /** Reset camera/transform */
    private void resetView() {
        offsetX = getWidth() / 2.0;
        offsetY = getHeight() / 2.0;
        zoom = 1.0;
        rotateX = 0;
        rotateY = 0;
    }

    // ========== JOGL RENDERING ==========

    /** JOGL: Called once when GL context is initialized */
    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        float[] lightPos = { 0.0f, 0.0f, 10.0f, 1.0f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);

        gl.glEnable(GL2.GL_COLOR_MATERIAL);
        gl.glColorMaterial(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE);
        gl.glShadeModel(GL2.GL_SMOOTH);
    }

    /** JOGL: Called when window is resized */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        GLU glu = new GLU();
        if (height == 0) height = 1;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0, (float) width / height, 0.1, 100.0);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    /** JOGL: Main draw function */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        if (showStl) {
            gl.glTranslated(0, 0, -5.0);
            gl.glTranslated(offsetX / getWidth(), offsetY / getHeight(), 0);
            gl.glRotated(Math.toDegrees(rotateX), 1, 0, 0);
            gl.glRotated(Math.toDegrees(rotateY), 0, 1, 0);
            gl.glScaled(zoom, zoom, zoom);
            drawStl(gl);
        } else {
            sketch.draw(gl);
        }

        gl.glFlush();
    }

    /** JOGL: Cleanup */
    @Override
    public void dispose(GLAutoDrawable drawable) {}

    /** Draw STL triangles with smooth shading */
    private void drawStl(GL2 gl) {
        gl.glColor3f(0.2f, 0.4f, 1.0f);
        for (float[] tri : stlTriangles) {
            gl.glBegin(GL2.GL_TRIANGLES);
            for (int i = 0; i < 3; i++) {
                float x = tri[i * 3], y = tri[i * 3 + 1], z = tri[i * 3 + 2];
                float[] normal = vertexNormals.getOrDefault(x + "," + y + "," + z, new float[]{0, 0, 1});
                gl.glNormal3f(normal[0], normal[1], normal[2]);
                gl.glVertex3f(x, y, z);
            }
            gl.glEnd();
        }
    }

    /** Computes the normal vector of a triangle (right-hand rule) */
    private float[] computeNormal(float[] v1, float[] v2, float[] v3) {
        float[] u = new float[]{v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};
        float[] v = new float[]{v3[0] - v1[0], v3[1] - v1[1], v3[2] - v1[2]};
        float[] normal = new float[]{
            u[1]*v[2] - u[2]*v[1],
            u[2]*v[0] - u[0]*v[2],
            u[0]*v[1] - u[1]*v[0]
        };
        float len = (float)Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (len != 0) {
            normal[0] /= len; normal[1] /= len; normal[2] /= len;
        }
        return normal;
    }

    /** Forces a redraw */
    public void refresh() {
        repaint();
    }
}