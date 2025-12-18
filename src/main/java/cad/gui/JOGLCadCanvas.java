package cad.gui;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;

// Import your existing Geometry and Sketch classes
import cad.core.Geometry;
import cad.core.Sketch;
import cad.gui.VBOManager;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.SwingUtilities;
import java.io.IOException;
import cad.core.MassProperties;

public class JOGLCadCanvas extends GLJPanel implements GLEventListener {

    private GLU glu; // OpenGL Utility Library for perspective transformations and sphere rendering

    // --- Camera/View parameters ---
    // These parameters control the position and orientation of the virtual camera
    // in the 3D scene, allowing user interaction (rotation, zoom).
    private float rotateX = 0.0f; // Rotation angle around the X-axis (for tilting the view up/down)
    private float rotateY = 0.0f; // Rotation angle around the Y-axis (for rotating the view left/right)
    private float zoomZ = -600.0f; // Zoom level (distance from the camera along the Z-axis).
                                   // Negative values move the object further into the screen.
    private int lastMouseX, lastMouseY; // Stores the last mouse position during a drag operation
    private boolean mouseDragging = false; // Flag to track if the mouse is currently being dragged

    // --- Data for rendering ---
    private Sketch sketch; // Reference to the 2D sketch object that this canvas can render.
    private boolean show3DModel = false; // Flag to determine whether to render the 3D model (true) or the 2D sketch
                                         // (false).
    private VBOManager vboManager = new VBOManager();
    private boolean vboDirty = true; // Track if VBO needs update

    public JOGLCadCanvas(Sketch sketch) {
        // Call the superclass constructor using static helper to configure capabilities
        super(createCapabilities());
        this.sketch = sketch; // Assign the provided sketch object

        // Register this class as an OpenGL event listener. This enables the `init`,
        // `display`, `reshape`, and `dispose` methods to be called by JOGL.
        addGLEventListener(this);

        // --- Setup Mouse Listeners for Interaction ---
        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                mouseDragging = true; // Start tracking drag
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDragging = false; // Stop tracking drag
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (mouseDragging) {
                    float dx = e.getX() - lastMouseX; // Change in X position
                    float dy = e.getY() - lastMouseY; // Change in Y position

                    if (SwingUtilities.isLeftMouseButton(e)) { // Rotate with Left Mouse Button
                        rotateY += dx * 0.5f; // Accumulate rotation around Y-axis
                        rotateX += dy * 0.5f; // Accumulate rotation around X-axis
                    } else if (SwingUtilities.isRightMouseButton(e)) { // Zoom/Pan with Right Mouse Button
                        // Simple vertical drag affects zoom (moves camera closer/further)
                        zoomZ += dy * 0.05f;
                        // For a proper pan, you'd translate the model based on dx, dy, and current
                        // zoom.
                    }
                    lastMouseX = e.getX(); // Update last mouse position
                    lastMouseY = e.getY();
                    repaint(); // Request a repaint to update the view with new transformations
                }
            }
        });

        // --- Setup Mouse Wheel Listener for Zooming ---
        addMouseWheelListener(e -> {

            float notches = e.getWheelRotation(); // Get number of "notches" the wheel was rotated
            zoomZ += notches * 0.5f; // Adjust zoom based on wheel rotation (positive moves away, negative moves
                                     // closer)
            repaint(); // Request repaint
        });

        // --- Setup Keyboard Listener for View Control ---
        addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_R: // 'R' key to Reset View
                        // Resets rotation and zoom to their default initial values.
                        rotateX = 0.0f;
                        rotateY = 0.0f;
                        zoomZ = -600.0f; // Reset zoom to initial distance
                        repaint();
                        break;
                    case KeyEvent.VK_2: // '2' key to Switch to 2D sketch view
                        // Calls the method to set the canvas to render the 2D sketch.
                        showSketch();
                        break;
                    case KeyEvent.VK_3: // '3' key to Switch to 3D model view
                        // Calls the method to set the canvas to render the 3D model.
                        show3DModel();
                        break;
                }
                repaint(); // Request repaint after any key-triggered change to update the display.
            }
        });

        setFocusable(true); // Makes the GLJPanel component able to receive keyboard focus.
        requestFocusInWindow(); // Requests that this component gain the keyboard focus when it's visible.
    }

    private static GLCapabilities createCapabilities() {
        GLCapabilities caps = new GLCapabilities(GLProfile.get("GL2"));
        caps.setSampleBuffers(true);
        caps.setNumSamples(4); // 4x MSAA
        return caps;
    }

    public void setCube(float size, int divisions) {
        // Calls the static method in Geometry to define a cube with the given size and
        // divisions.
        // Geometry will internally set itself to render a CUBE and store these
        // parameters.
        Geometry.createCube(size, divisions);
        // Switches the canvas to 3D model display mode to show the newly created cube.
        show3DModel();
    }

    public void setSphere(float radius, int latDiv, int lonDiv) {
        // Calls the static method in Geometry to define a sphere with the given radius
        // and divisions.
        // Geometry will internally set itself to render a SPHERE and store these
        // parameters.
        // Note: `createSphere` in `Geometry` might simplify latDiv and lonDiv into one
        // 'divisions' param.
        Geometry.createSphere(radius, latDiv); // Assuming createSphere takes radius and one division param for
                                               // simplicity based on your comment. Adjust if Geometry.createSphere
                                               // needs both latDiv and lonDiv explicitly.
        // Switches the canvas to 3D model display mode to show the newly created
        // sphere.
        show3DModel();
    }

    public void loadSTL(String filePath) {
        try {
            // Calls the static method in Geometry to load the STL file data.
            // Geometry will parse the file and store the triangle data internally,
            // also setting its current shape type to STL_LOADED.
            Geometry.loadStl(filePath);
            System.out.println("Loaded STL triangles: " + Geometry.getLoadedStlTriangles().size()); // Debug statement
            System.out.println("Current shape after loading: " + Geometry.getCurrentShape()); // Debug current shape
            System.out.println("Show3DModel flag: " + show3DModel); // Debug 3D flag

            // If loading succeeds, switch the canvas to 3D model display mode.
            show3DModel();
            // Request a repaint to immediately display the loaded STL model.
            repaint();
        } catch (IOException e) {
            // Catches any I/O errors that occur during file loading (e.g., file not found,
            // read errors).
            System.err.println("Error loading STL file: " + e.getMessage());
            e.printStackTrace(); // Prints the stack trace for debugging.
            // Optionally, you could reset Geometry.currShape here if the loading failure
            // should result in nothing being displayed or reverting to a previous state.
        }
    }

    public void showSketch() {
        show3DModel = false; // Set flag to render 2D sketch
        repaint(); // Request the canvas to redraw itself
    }

    public void show3DModel() {
        show3DModel = true; // Set flag to render 3D model
        vboDirty = true; // Mark VBO as dirty (geometry may have changed)
        repaint(); // Request the canvas to redraw itself
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
            height = 1; // Prevent division by zero if height is 0
        float h = (float) width / (float) height; // Calculate the aspect ratio

        // Set the OpenGL viewport to cover the entire canvas area.
        gl.glViewport(0, 0, width, height);

        // Switch to the projection matrix mode. This matrix defines how 3D coordinates
        // are projected onto the 2D screen.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity(); // Reset the projection matrix to an identity matrix

        if (show3DModel) {
            // For 3D models, set up a perspective projection.
            // This simulates how a camera sees objects, with distant objects appearing
            // smaller.
            // Parameters: field of view (45 deg), aspect ratio, near clipping plane (0.1f),
            // far clipping plane (2000.0f).
            glu.gluPerspective(45.0f, h, 0.1f, 2000.0f); // Increased far clipping for large models
        } else {
            // For 2D sketches, set up an orthographic projection.
            // This projects objects without perspective distortion, suitable for CAD 2D
            // views.
            // It maps a 2D coordinate system directly to the screen pixels.
            float orthoScale = 100.0f; // A scaling factor for the orthographic view
            glu.gluOrtho2D(-width / orthoScale, width / orthoScale, -height / orthoScale, height / orthoScale);
        }
        // Switch back to the modelview matrix mode. This matrix is used for
        // transforming objects in the 3D scene (e.g., rotation, translation).
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity(); // Reset the modelview matrix
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        // Clear the color buffer (screen pixels) and the depth buffer (for depth
        // testing).
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity(); // Reset the current modelview matrix to identity

        if (show3DModel) { // If in 3D view mode
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
            // --- VBO-based rendering for extruded geometry ---
            if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
                if (vboDirty) {
                    // Compute per-vertex normals for smooth shading before uploading
                    sketch.computePerVertexNormals();
                    vboManager.uploadFaces(gl, glu, sketch.extrudedFaces);
                    vboDirty = false;
                }
                vboManager.draw(gl);
            } else {
                Geometry.drawCurrentShape(gl);
            }
        } else { // If in 2D sketch view mode
            // --- Set up for 2D Sketch Rendering ---
            gl.glPushMatrix(); // Save the current modelview matrix before applying 2D-specific transforms.
            gl.glTranslatef(0.0f, 0.0f, zoomZ); // Apply zoom to the 2D sketch (moves it in/out of the screen plane).

            gl.glDisable(GL2.GL_LIGHTING); // Disable lighting for 2D sketches to ensure flat, unlit appearance.
            gl.glColor3f(0.2f, 0.2f, 0.8f);
            gl.glLineWidth(2.0f); // Set the line width for sketch elements.

            // --- Render the 2D Sketch ---
            if (sketch != null) {
                // CORRECTED: Added the missing method name 'draw'
                sketch.draw(gl); // Call the `draw` method of the `Sketch` object to render all its elements
            }
            gl.glEnable(GL2.GL_LIGHTING); // Re-enable lighting. Important so 3D objects are lit correctly
                                          // if the view switches back to 3D.
            gl.glEnable(GL2.GL_DEPTH_TEST); // Ensure depth testing is enabled for 3D rendering.
            gl.glPopMatrix(); // Restore the previous modelview matrix.
        }

        gl.glFlush(); // Ensures all OpenGL commands are executed immediately, sending them to the
                      // graphics hardware.
    }

    private float[] calculateGeometryCenter() {
        // If we have extruded geometry, calculate its center
        if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
            float totalX = 0.0f;
            float totalY = 0.0f;
            float totalZ = 0.0f;
            int vertexCount = 0;

            // Sum all vertex coordinates
            for (Sketch.Face3D face : sketch.extrudedFaces) {
                for (Sketch.Point3D vertex : face.getVertices()) {
                    totalX += vertex.getX();
                    totalY += vertex.getY();
                    totalZ += vertex.getZ();
                    vertexCount++;
                }
            }

            // Calculate average (centroid)
            if (vertexCount > 0) {
                return new float[] {
                        totalX / vertexCount,
                        totalY / vertexCount,
                        totalZ / vertexCount
                };
            }
        }

        // Default to origin for built-in shapes (cube, sphere, STL)
        return new float[] { 0.0f, 0.0f, 0.0f };
    }

    private float calculateGeometrySize() {
        if (sketch != null && !sketch.extrudedFaces.isEmpty()) {
            float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
            float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

            // Find bounding box
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

            // Return the maximum dimension for zoom calculation
            float width = maxX - minX;
            float height = maxY - minY;
            float depth = maxZ - minZ;
            return Math.max(Math.max(width, height), depth);
        }

        return 50.0f; // Default size for built-in shapes
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        vboManager.dispose(gl);
    }
}