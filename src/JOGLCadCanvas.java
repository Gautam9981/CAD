package cad.gui;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

// Import your existing Geometry and Sketch classes
import cad.core.Geometry;
import cad.core.Sketch;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.SwingUtilities;
import java.io.IOException;

/**
 * JOGLCadCanvas is a custom OpenGL canvas for rendering 3D objects and 2D sketches.
 * It extends GLJPanel from JOGL, providing an AWT-compatible component that can
 * render OpenGL graphics. It implements GLEventListener to handle OpenGL lifecycle
 * events (initialization, display, reshape, dispose).
 *
 * It supports interactive rotation, zooming, and switching between 2D and 3D views.
 * The 3D model data (cube, sphere, or loaded STL) is managed by the static
 * `cad.core.Geometry` class, making `JOGLCadCanvas` primarily a viewer/renderer.
 */
public class JOGLCadCanvas extends GLJPanel implements GLEventListener {

    private GLU glu; // OpenGL Utility Library for perspective transformations and sphere rendering
    private FPSAnimator animator; // Animator for smooth rendering at a fixed frames per second (FPS)

    // --- Camera/View parameters ---
    // These parameters control the position and orientation of the virtual camera
    // in the 3D scene, allowing user interaction (rotation, zoom).
    private float rotateX = 0.0f; // Rotation angle around the X-axis (for tilting the view up/down)
    private float rotateY = 0.0f; // Rotation angle around the Y-axis (for rotating the view left/right)
    private float zoomZ = -5.0f; // Zoom level (distance from the camera along the Z-axis).
                                 // Negative values move the object further into the screen.
    private int lastMouseX, lastMouseY; // Stores the last mouse position during a drag operation
    private boolean mouseDragging = false; // Flag to track if the mouse is currently being dragged

    // --- Data for rendering ---
    private Sketch sketch; // Reference to the 2D sketch object that this canvas can render.
                           // The Sketch class is assumed to have a `draw(GL2 gl)` method.
    private boolean show3DModel = false; // Flag to determine whether to render the 3D model (true)
                                         // or the 2D sketch (false).

    /**
     * Constructor for JOGLCadCanvas.
     * Initializes the OpenGL canvas and sets up event listeners for user interaction
     * (mouse for rotation/zoom, keyboard for view mode switching).
     *
     * @param sketch The 2D sketch object to render when in 2D view mode.
     */
    public JOGLCadCanvas(Sketch sketch) {
        // Call the superclass constructor, requesting an OpenGL 2.0 profile.
        // GL2 is used for compatibility with fixed-function pipeline features.
        // This MUST be the first statement in the constructor.
        super(new GLCapabilities(GLProfile.get("GL2")));
        this.sketch = sketch; // Assign the provided sketch object

        // Register this class as an OpenGL event listener. This enables the `init`,
        // `display`, `reshape`, and `dispose` methods to be called by JOGL.
        addGLEventListener(this);

        // --- Setup Mouse Listeners for Interaction ---
        addMouseListener(new MouseAdapter() {
            /**
             * Captures the initial mouse position when a mouse button is pressed
             * and sets the dragging flag.
             */
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                mouseDragging = true; // Start tracking drag
            }

            /**
             * Resets the dragging flag when the mouse button is released.
             */
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDragging = false; // Stop tracking drag
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            /**
             * Handles mouse dragging for rotation (left-click drag) and
             * basic zoom/pan (right-click drag).
             */
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
                        // For a proper pan, you'd translate the model based on dx, dy, and current zoom.
                    }
                    lastMouseX = e.getX(); // Update last mouse position
                    lastMouseY = e.getY();
                    repaint(); // Request a repaint to update the view with new transformations
                }
            }
        });

        // --- Setup Mouse Wheel Listener for Zooming ---
        addMouseWheelListener(e -> {
            /**
             * Adjusts the zoom level based on mouse wheel rotation.
             * Scrolling up zooms in, scrolling down zooms out.
             */
            float notches = e.getWheelRotation(); // Get number of "notches" the wheel was rotated
            zoomZ += notches * 0.5f; // Adjust zoom based on wheel rotation (positive moves away, negative moves closer)
            repaint(); // Request repaint
        });

        // --- Setup Keyboard Listener for View Control ---
        addKeyListener(new KeyAdapter() {
            /**
             * Handles key presses for resetting view and switching between 2D/3D modes.
             */
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_R: // 'R' key to Reset View
                        // Resets rotation and zoom to their default initial values.
                        rotateX = 0.0f;
                        rotateY = 0.0f;
                        zoomZ = -5.0f;
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

    /**
     * Sets the current active shape to a cube and updates its size and subdivisions.
     * This method delegates the actual setting of parameters to the static
     * `Geometry.createCube()` method, which manages the application's central
     * 3D model state.
     *
     * @param size      The edge length of the cube.
     * @param divisions The number of subdivisions per edge (affects geometry, not rendering directly here).
     */
    public void setCube(float size, int divisions) {
        // Calls the static method in Geometry to define a cube with the given size and divisions.
        // Geometry will internally set itself to render a CUBE and store these parameters.
        Geometry.createCube(size, divisions);
        // Switches the canvas to 3D model display mode to show the newly created cube.
        show3DModel();
    }

    /**
     * Sets the current active shape to a sphere and updates its radius and subdivisions.
     * This method delegates the actual setting of parameters to the static
     * `Geometry.createSphere()` method.
     *
     * @param radius The radius of the sphere.
     * @param latDiv The number of latitude subdivisions (for sphere generation).
     * @param lonDiv The number of longitude subdivisions (for sphere generation).
     */
    public void setSphere(float radius, int latDiv, int lonDiv) {
        // Calls the static method in Geometry to define a sphere with the given radius and divisions.
        // Geometry will internally set itself to render a SPHERE and store these parameters.
        // Note: `createSphere` in `Geometry` might simplify latDiv and lonDiv into one 'divisions' param.
        Geometry.createSphere(radius, latDiv); // Assuming createSphere takes radius and one division param for simplicity based on your comment. Adjust if Geometry.createSphere needs both latDiv and lonDiv explicitly.
        // Switches the canvas to 3D model display mode to show the newly created sphere.
        show3DModel();
    }

    /**
     * Loads an STL (Stereolithography) file using the static `Geometry.loadStl()` method.
     * This method attempts to read 3D model data from the specified file.
     * If the loading is successful, the view is automatically switched to 3D mode.
     * Includes basic error handling for file input/output operations.
     *
     * @param filePath The path to the STL file to load.
     */
    public void loadSTL(String filePath) {
        try {
            // Calls the static method in Geometry to load the STL file data.
            // Geometry will parse the file and store the triangle data internally,
            // also setting its current shape type to STL_LOADED.
            Geometry.loadStl(filePath);
            // If loading succeeds, switch the canvas to 3D model display mode.
            show3DModel();
            // Request a repaint to immediately display the loaded STL model.
            repaint();
        } catch (IOException e) {
            // Catches any I/O errors that occur during file loading (e.g., file not found, read errors).
            System.err.println("Error loading STL file: " + e.getMessage());
            e.printStackTrace(); // Prints the stack trace for debugging.
            // Optionally, you could reset Geometry.currShape here if the loading failure
            // should result in nothing being displayed or reverting to a previous state.
        }
    }

    /**
     * Switches the canvas display mode to show the 2D sketch.
     * Sets the internal flag `show3DModel` to `false` and requests a repaint.
     */
    public void showSketch() {
        show3DModel = false; // Set flag to render 2D sketch
        repaint();           // Request the canvas to redraw itself
    }

    /**
     * Switches the canvas display mode to show the 3D model (cube, sphere, or loaded STL).
     * Sets the internal flag `show3DModel` to `true` and requests a repaint.
     */
    public void show3DModel() {
        show3DModel = true;  // Set flag to render 3D model
        repaint();           // Request the canvas to redraw itself
    }

    /**
     * This method is called exactly once when the OpenGL context is first created.
     * It's used to perform all necessary OpenGL initializations, such as setting
     * background color, enabling depth testing, configuring lighting, and setting
     * material properties.
     *
     * @param drawable The GLAutoDrawable object, providing access to the OpenGL context.
     */
    @Override
    public void init(GLAutoDrawable drawable) {
        // Obtain the GL2 object, which provides the OpenGL 2.0 API.
        GL2 gl = drawable.getGL().getGL2();
        // Initialize GLU (OpenGL Utility Library) for functions like perspective setup and sphere drawing.
        glu = new GLU();

        // Set the clear color for the color buffer (the background color of the canvas).
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f); // Dark gray background (RGB A)
        // Set the clear value for the depth buffer. 1.0f means furthest away.
        gl.glClearDepth(1.0f);
        // Enable depth testing. This ensures that objects closer to the camera obscure
        // objects further away, providing correct 3D perception.
        gl.glEnable(GL2.GL_DEPTH_TEST);
        // Set the depth function. GL_LEQUAL means a fragment passes the depth test if
        // its depth value is less than or equal to the stored depth value.
        gl.glDepthFunc(GL2.GL_LEQUAL);
        // Hint to OpenGL for perspective correction (for better visual quality).
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
        // Set the shading model to smooth. This interpolates colors across faces for a smoother appearance.
        gl.glShadeModel(GL2.GL_SMOOTH);

        // --- Important Rendering Mode Setting ---
        // Set the polygon mode for both front and back faces to GL_FILL.
        // This makes OpenGL render polygons as solid (filled) surfaces, rather than just wireframes.
        gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);

        // Enable face culling. This optimizes rendering by not drawing faces that are
        // facing away from the camera, which are typically hidden anyway.
        gl.glEnable(GL2.GL_CULL_FACE);
        // Specify that back-facing polygons should be culled. The "back" face is determined
        // by the winding order of its vertices (clockwise or counter-clockwise).
        gl.glCullFace(GL2.GL_BACK);

        // --- Lighting Setup ---
        // Enable OpenGL's lighting engine. Without this, objects will appear flat and unlit.
        gl.glEnable(GL2.GL_LIGHTING);
        // Enable a specific light source (Light 0). OpenGL supports multiple lights.
        gl.glEnable(GL2.GL_LIGHT0);
        // Define the position of Light 0. The last component (0.0f) indicates a directional light.
        // If it were 1.0f, it would be a positional light at (1,1,1).
        float[] lightPos = {1.0f, 1.0f, 1.0f, 0.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
        // Define the ambient, diffuse, and specular components of Light 0.
        // Ambient: Light that has been scattered so much it comes from all directions.
        float[] ambientLight = {0.3f, 0.3f, 0.3f, 1.0f};
        // Diffuse: Light that reflects equally in all directions (main component for color).
        float[] diffuseLight = {0.7f, 0.7f, 0.7f, 1.0f};
        // Specular: Highlight from the light source, dependent on observer position and shininess.
        float[] specularLight = {1.0f, 1.0f, 1.0f, 1.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, ambientLight, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, diffuseLight, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, specularLight, 0);

        // --- Material Properties Setup ---
        // These define how the surface of the object interacts with light.
        // Set the ambient, diffuse, specular, and shininess properties for the front face of objects.
        float[] materialAmbient = {0.1f, 0.1f, 0.1f, 1.0f}; // Base ambient color
        float[] materialDiffuse = {0.8f, 0.8f, 0.8f, 1.0f}; // Base diffuse color (main object color)
        float[] materialSpecular = {1.0f, 1.0f, 1.0f, 1.0f}; // Color of the highlight
        float shininess = 100.0f; // How "shiny" the material is (higher values = smaller, more intense highlight)
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, materialAmbient, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, materialDiffuse, 0);
        gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR, materialSpecular, 0);
        gl.glMaterialf(GL2.GL_FRONT, GL2.GL_SHININESS, shininess);

        // Initialize and start the FPSAnimator to continuously redraw the canvas
        // at 60 frames per second. This provides smooth animation and interactivity.
        animator = new FPSAnimator(drawable, 60, true);
        animator.start();
    }

    /**
     * This method is called whenever the OpenGL canvas is resized.
     * It adjusts the viewport (the area on the screen where OpenGL draws)
     * and reconfigures the projection matrix to maintain correct aspect ratio
     * and perspective for either 3D or 2D rendering.
     *
     * @param drawable The GLAutoDrawable object.
     * @param x        The x-coordinate of the viewport origin.
     * @param y        The y-coordinate of the viewport origin.
     * @param width    The new width of the viewport.
     * @param height   The new height of the viewport.
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        if (height == 0) height = 1; // Prevent division by zero if height is 0
        float h = (float) width / (float) height; // Calculate the aspect ratio

        // Set the OpenGL viewport to cover the entire canvas area.
        gl.glViewport(0, 0, width, height);

        // Switch to the projection matrix mode. This matrix defines how 3D coordinates
        // are projected onto the 2D screen.
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity(); // Reset the projection matrix to an identity matrix

        if (show3DModel) {
            // For 3D models, set up a perspective projection.
            // This simulates how a camera sees objects, with distant objects appearing smaller.
            // Parameters: field of view (45 deg), aspect ratio, near clipping plane (0.1f), far clipping plane (500.0f).
            glu.gluPerspective(45.0f, h, 0.1f, 500.0f);
        } else {
            // For 2D sketches, set up an orthographic projection.
            // This projects objects without perspective distortion, suitable for CAD 2D views.
            // It maps a 2D coordinate system directly to the screen pixels.
            float orthoScale = 100.0f; // A scaling factor for the orthographic view
            glu.gluOrtho2D(-width / orthoScale, width / orthoScale, -height / orthoScale, height / orthoScale);
        }
        // Switch back to the modelview matrix mode. This matrix is used for
        // transforming objects in the 3D scene (e.g., rotation, translation).
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity(); // Reset the modelview matrix
    }

    /**
     * This method is called repeatedly by the animator to render the scene.
     * It clears the buffers, applies camera transformations, and then draws
     * either the 3D model (cube, sphere, or STL) or the 2D sketch based on
     * the current display mode (`show3DModel` flag).
     *
     * @param drawable The GLAutoDrawable object.
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        // Clear the color buffer (screen pixels) and the depth buffer (for depth testing).
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity(); // Reset the current modelview matrix to identity

        if (show3DModel) { // If in 3D view mode
            // --- Apply 3D Camera Transformations ---
            // Translate the entire scene based on the zoom level.
            gl.glTranslatef(0.0f, 0.0f, zoomZ);
            // Rotate the scene around the X-axis based on `rotateX`.
            gl.glRotatef(rotateX, 1.0f, 0.0f, 0.0f);
            // Rotate the scene around the Y-axis based on `rotateY`.
            gl.glRotatef(rotateY, 0.0f, 1.0f, 0.0f);

            // --- Set Default Material Color for 3D Objects ---
            // Set a default color (light blue) for objects. This applies to primitives
            // and STL models if they don't define their own material colors.
            float[] objectColor = {0.6f, 0.7f, 0.9f, 1.0f};
            // Apply this color to both ambient and diffuse material properties.
            gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT_AND_DIFFUSE, objectColor, 0);

            // --- Render the Currently Selected 3D Object ---
            // This switch statement queries the `Geometry` class to determine
            // which 3D primitive or loaded model should be drawn.
            switch (Geometry.getCurrentShape()) { // Get the current active shape type from Geometry
                case CUBE:
                    // If a cube is selected, check if its size is valid (greater than 0).
                    if (Geometry.getParam() > 0) {
                        // Call the static `drawCube` method in `Geometry` to render the cube.
                        // It uses `Geometry.getParam()` for size and `Geometry.cubeDivisions` for detail.
                        Geometry.drawCube(gl, Geometry.getParam(), Geometry.cubeDivisions);
                    }
                    break;
                case SPHERE:
                    // If a sphere is selected, check if its radius is valid.
                    if (Geometry.getParam() > 0) {
                        // Call the static `drawSphere` method in `Geometry` to render the sphere.
                        // It uses `Geometry.getParam()` for radius and `Geometry.sphereLatDiv`/`sphereLonDiv` for detail.
                        Geometry.drawSphere(gl, Geometry.getParam(), Geometry.sphereLatDiv, Geometry.sphereLonDiv);
                    }
                    break;
                case STL_LOADED:
                    // If an STL model has been loaded and is the active shape, draw it.
                    // This method iterates through the stored STL triangles and renders them.
                    Geometry.drawLoadedStl(gl);
                    break;
                case NONE:
                default:
                    // If no specific primitive or STL is selected, nothing is drawn in 3D.
                    // This represents an empty 3D scene or no active model.
                    break;
            }
        } else { // If in 2D sketch view mode
            // --- Set up for 2D Sketch Rendering ---
            gl.glPushMatrix(); // Save the current modelview matrix before applying 2D-specific transforms.
            gl.glTranslatef(0.0f, 0.0f, zoomZ); // Apply zoom to the 2D sketch (moves it in/out of the screen plane).

            gl.glDisable(GL2.GL_LIGHTING); // Disable lighting for 2D sketches to ensure flat, unlit appearance.
            gl.glColor3f(0.8f, 0.8f, 0.8f); // Set the drawing color for sketch elements (light gray).
            gl.glLineWidth(2.0f); // Set the line width for sketch elements.

            // --- Render the 2D Sketch ---
            if (sketch != null) {
                // CORRECTED: Added the missing method name 'draw'
                sketch.draw(gl); // Call the `draw` method of the `Sketch` object to render all its elements
            }
            gl.glEnable(GL2.GL_LIGHTING); // Re-enable lighting. Important so 3D objects are lit correctly
                                          // if the view switches back to 3D.
            gl.glPopMatrix(); // Restore the previous modelview matrix.
        }

        gl.glFlush(); // Ensures all OpenGL commands are executed immediately, sending them to the graphics hardware.
    }

    /**
     * This method is called when the OpenGL context is destroyed (e.g., when the
     * GLJPanel is removed from its parent container or the application exits).
     * It's used to release any OpenGL-related resources.
     *
     * @param drawable The GLAutoDrawable object.
     */
    @Override
    public void dispose(GLAutoDrawable drawable) {
        // If the animator is running, stop it to release its thread and resources.
        if (animator != null && animator.isStarted()) {
            animator.stop();
        }
        // No explicit cleanup for GLU object is usually needed as it's managed by JOGL's context.
    }
}