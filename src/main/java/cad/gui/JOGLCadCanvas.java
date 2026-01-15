package cad.gui;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;

import cad.core.Geometry;
import cad.core.Sketch;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.SwingUtilities;
import java.io.IOException;

public class JOGLCadCanvas extends GLJPanel implements GLEventListener {

    private GLU glu;

    private float rotateX = 0.0f;
    private float rotateY = 0.0f;
    private float zoomZ = -600.0f;

    private int lastMouseX, lastMouseY;
    private boolean mouseDragging = false;

    private Sketch sketch;
    private boolean show3DModel = false;

    private VBOManager vboManager = new VBOManager();
    private boolean vboDirty = true;

    public JOGLCadCanvas(Sketch sketch) {

        super(createCapabilities());
        this.sketch = sketch;

        addGLEventListener(this);

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                mouseDragging = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDragging = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDragging) {
                    float dx = e.getX() - lastMouseX;
                    float dy = e.getY() - lastMouseY;

                    if (SwingUtilities.isLeftMouseButton(e)) {
                        rotateY += dx * 0.5f;
                        rotateX += dy * 0.5f;
                    } else if (SwingUtilities.isRightMouseButton(e)) {

                        zoomZ += dy * 0.05f;

                    }
                    lastMouseX = e.getX();
                    lastMouseY = e.getY();
                    repaint();
                }
            }
        });

        addMouseWheelListener(e -> {

            float notches = e.getWheelRotation();
            zoomZ += notches * 0.5f;

            repaint();
        });

        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_R:

                        rotateX = 0.0f;
                        rotateY = 0.0f;
                        zoomZ = -600.0f;
                        repaint();
                        break;
                    case KeyEvent.VK_2:

                        showSketch();
                        break;
                    case KeyEvent.VK_3:

                        show3DModel();
                        break;
                }
                repaint();
            }
        });

        setFocusable(true);
        requestFocusInWindow();
    }

    private static GLCapabilities createCapabilities() {
        GLCapabilities caps = new GLCapabilities(GLProfile.get("GL2"));
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        return caps;
    }

    public void setCube(float size, int divisions) {

        Geometry.createCube(size, divisions);

        show3DModel();
    }

    public void setSphere(float radius, int latDiv, int lonDiv) {

        Geometry.createSphere(radius, latDiv);

        show3DModel();
    }

    public void loadSTL(String filePath) {
        try {

            Geometry.loadStl(filePath);
            System.out.println("Loaded STL triangles: " + Geometry.getLoadedStlTriangles().size());
            System.out.println("Current shape after loading: " + Geometry.getCurrentShape());
            System.out.println("Show3DModel flag: " + show3DModel);

            show3DModel();

            repaint();
        } catch (IOException e) {

            System.err.println("Error loading STL file: " + e.getMessage());
            e.printStackTrace();

        }
    }

    public void showSketch() {
        show3DModel = false;
        repaint();
    }

    public void show3DModel() {
        show3DModel = true;
        vboDirty = true;
        repaint();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        glu = new GLU();
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
        gl.glClearDepth(1.0f);
        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glDepthFunc(GL2.GL_LEQUAL);
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
        gl.glEnable(GL2.GL_NORMALIZE);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);
        float[] lightPos = { 1.0f, 1.0f, 1.0f, 0.0f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
        float[] ambientLight = { 0.3f, 0.3f, 0.3f, 1.0f };
        float[] diffuseLight = { 0.7f, 0.7f, 0.7f, 1.0f };
        float[] specularLight = { 1.0f, 1.0f, 1.0f, 1.0f };
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularLight, 0);
        float[] materialAmbient = { 0.1f, 0.1f, 0.1f, 1.0f };
        float[] materialDiffuse = { 0.8f, 0.8f, 0.8f, 1.0f };
        float[] materialSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };
        float shininess = 100.0f;
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, materialAmbient, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, materialDiffuse, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, materialSpecular, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, shininess);
        vboDirty = true;
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height == 0)
            height = 1;
        float h = (float) width / (float) height;

        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        if (show3DModel) {

            glu.gluPerspective(45.0f, h, 0.1f, 2000.0f);
        } else {

            float orthoScale = 100.0f;
            glu.gluOrtho2D(-width / orthoScale, width / orthoScale, -height / orthoScale, height / orthoScale);
        }

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        if (show3DModel) {
            float distance = -zoomZ;
            if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
                float geometrySize = calculateGeometrySize();
                float minDistance = geometrySize * 3.0f;
                if (distance < minDistance)
                    distance = minDistance;
            }
            float[] geometryCenter = calculateGeometryCenter();
            float centerX = geometryCenter[0];
            float centerY = geometryCenter[1];
            float centerZ = geometryCenter[2];
            float radX = (float) Math.toRadians(rotateX);
            float radY = (float) Math.toRadians(rotateY);
            float currentEyeX = 0;
            float currentEyeY = 0;
            float currentEyeZ = distance;
            float tempX = (float) (currentEyeX * Math.cos(radY) + currentEyeZ * Math.sin(radY));
            float tempZ = (float) (-currentEyeX * Math.sin(radY) + currentEyeZ * Math.cos(radY));
            currentEyeX = tempX;
            currentEyeZ = tempZ;
            float tempY = (float) (currentEyeY * Math.cos(radX) - currentEyeZ * Math.sin(radX));
            tempZ = (float) (currentEyeY * Math.sin(radX) + currentEyeZ * Math.cos(radX));
            currentEyeY = tempY;
            currentEyeZ = tempZ;
            currentEyeX += centerX;
            currentEyeY += centerY;
            currentEyeZ += centerZ;
            if (glu != null) {
                glu.gluLookAt(currentEyeX, currentEyeY, currentEyeZ, centerX, centerY, centerZ, 0.0f, 1.0f, 0.0f);
            } else {
                System.err.println("GLU object not initialized!");
            }
            float[] objectColor = { 0.6f, 0.7f, 0.9f, 1.0f };
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, objectColor, 0);

            if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
                if (vboDirty) {

                    sketch.computePerVertexNormals();
                    vboManager.uploadFaces(gl, glu, sketch.extrudedFaces);
                    vboDirty = false;
                }
                vboManager.draw(gl);
            } else {
                Geometry.drawCurrentShape(gl);
            }
        } else {

            gl.glPushMatrix();
            gl.glTranslatef(0.0f, 0.0f, zoomZ);

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3f(0.2f, 0.2f, 0.8f);
            gl.glLineWidth(2.0f);

            if (sketch != null) {

                sketch.draw(gl);
            }
            gl.glEnable(GL2.GL_LIGHTING);

            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glPopMatrix();
        }

        gl.glFlush();

    }

    private float[] calculateGeometryCenter() {

        if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
            float totalX = 0.0f;
            float totalY = 0.0f;
            float totalZ = 0.0f;
            int vertexCount = 0;

            for (Sketch.Face3D face : sketch.extrudedFaces) {
                for (Sketch.Point3D vertex : face.getVertices()) {
                    totalX += vertex.getX();
                    totalY += vertex.getY();
                    totalZ += vertex.getZ();
                    vertexCount++;
                }
            }

            if (vertexCount > 0) {
                return new float[] {
                        totalX / vertexCount,
                        totalY / vertexCount,
                        totalZ / vertexCount
                };
            }
        }

        return new float[] { 0.0f, 0.0f, 0.0f };
    }

    private float calculateGeometrySize() {
        if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
            float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

            for (Sketch.Face3D face : sketch.extrudedFaces) {
                for (Sketch.Point3D vertex : face.getVertices()) {
                    float x = vertex.getX();
                    float y = vertex.getY();
                    float z = vertex.getZ();

                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z);
                    maxZ = Math.max(maxZ, z);
                }
            }

            float width = maxX - minX;
            float height = maxY - minY;
            float depth = maxZ - minZ;
            return Math.max(Math.max(width, height), depth);
        }

        return 50.0f;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        vboManager.dispose(gl);
    }
}