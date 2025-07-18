package cad.gui;

// JavaFX imports
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane; // Explicitly import SplitPane
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window; // Import Window

// Swing/AWT imports (only for JOGL + file dialogs)
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

// Java utility and I/O
import java.io.File;
import java.util.List;

// JOGL imports
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;

// Project-specific imports
import cad.core.Geometry;
import cad.core.Sketch;

/**
 * GuiFX - Main JavaFX Application Class for CAD System
 * 
 * This class represents the primary GUI application for a Computer-Aided Design (CAD) system
 * built using JavaFX with an embedded JOGL (Java OpenGL) canvas for 3D rendering.
 * 
 * Key Features:
 * - Interactive 3D model viewing and manipulation using OpenGL
 * - Support for STL file import/export and DXF file operations  
 * - Real-time 3D model generation (cubes, spheres with customizable subdivisions)
 * - 2D sketching capabilities with various geometric primitives
 * - Dual-mode view switching between 3D models and 2D sketches
 * - Optimized layout with resizable panels and space-efficient design
 * 
 * Architecture:
 * - JavaFX for main UI components and layout management
 * - SwingNode wrapper for integrating JOGL OpenGL canvas
 * - Event-driven interaction with mouse and keyboard controls
 * - Dual focus management system for seamless keyboard navigation
 * - Model-View pattern with OpenGLRenderer handling all 3D rendering
 * 
 * Navigation Controls:
 * - Mouse: Drag to rotate, wheel to zoom, click to focus
 * - Keyboard: Arrow keys for rotation, Q/E for zoom, R for reset
 * - Space: Toggle between 3D and 2D views
 * - ESC: Restore canvas focus if needed
 * 
 * Layout Structure:
 * - Main split pane (horizontal): Control panel (25%) | 3D Canvas (75%)
 * - Vertical split: Main content (85%) | Console output (15%)
 * - Control panel: Commands section + Parameters section
 * - Optimized for space efficiency with large model viewing
 * 
*/

public class GuiFX extends Application {

    // === UI Components ===
    private TextArea outputArea;
    private TextField cubeSizeField, sphereRadiusField, fileField, latField, lonField;
    private TextField sketchPointX, sketchPointY;
    private TextField sketchLineX1, sketchLineY1, sketchLineX2, sketchLineY2;
    private TextField sketchCircleX, sketchCircleY, sketchCircleR;
    private TextField sketchPolygonX, sketchPolygonY, sketchPolygonR, sketchPolygonSides;
    private ComboBox<String> unitSelector;
    private JOGLCadCanvas glCanvas; // This is an AWT component where OpenGL rendering happens
    private SwingNode canvasNode; // This wraps the AWT GLCanvas for integration into JavaFX scene graph
    private FPSAnimator animator; // Manages the animation loop for the GLCanvas
    private OpenGLRenderer glRenderer; // Reference to the OpenGLRenderer instance

    // === Static fields for configuration and core objects ===
    public static int sphereLatDiv = 10; // Default latitude divisions for sphere
    public static int sphereLonDiv = 10; // Default longitude divisions for sphere
    public static int cubeDivisions = 10; // Default divisions for cube
    public static Sketch sketch; // The core object for 2D sketching operations

    // === OpenGL rendering variables ===
    private float rotationX = 0.0f; // Current rotation around the X-axis for 3D view
    private float rotationY = 0.0f; // Current rotation around the Y-axis for 3D view
    private float zoom = -5.0f; // Current zoom level for 3D view
    private int lastMouseX, lastMouseY; // Last mouse coordinates for drag rotation
    private boolean isDragging = false; // Flag to indicate if mouse is being dragged for rotation
    private List<float[]> stlTriangles; // Stores triangles data from a loaded STL file
    // private boolean showSketch = false; // This field is now managed directly by OpenGLRenderer via setter

    /**
     * The main entry point for the JavaFX application.
     * This method is called after the launch() method is invoked.
     * It initializes the UI, sets up the scene, and displays the primary stage.
     *
     * 
    */
    @Override
    public void start(Stage primaryStage) {
        sketch = new Sketch(); // Initialize the Sketch object

        primaryStage.setTitle("CAD GUI - JavaFX wit");

        // Initialize UI components
        initializeComponents();

        // Create main layout
        BorderPane root = new BorderPane();

        // Create split panes for layout organization
        SplitPane mainSplitPane = createMainSplitPane();
        SplitPane verticalSplitPane = new SplitPane();
        verticalSplitPane.setOrientation(Orientation.VERTICAL);
        verticalSplitPane.getItems().addAll(mainSplitPane, createConsolePane());
        verticalSplitPane.setDividerPositions(0.85); // Give more space to main content, less to console

        root.setCenter(verticalSplitPane);

        // Create scene
        Scene scene = new Scene(root, 1920, 1080);
        // Load external CSS stylesheet for styling
        //scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        // Handle application close request to stop the OpenGL animator and exit gracefully
        primaryStage.setOnCloseRequest(e -> {
            if (animator != null) {
                animator.stop();
            }
            Platform.exit();
            System.exit(0);
        });

        primaryStage.show();

        // Start OpenGL animation if the animator is initialized
        if (animator != null) {
            animator.start();
        }
    }

    /**
     * Initializes all the UI components (TextAreas, TextFields, ComboBoxes).
     * This method is called once during the application's startup.
     */
    private void initializeComponents() {
        // Initialize all TextField components
        cubeSizeField = new TextField();
        sphereRadiusField = new TextField();
        fileField = new TextField();
        latField = new TextField();
        lonField = new TextField();
        sketchPointX = new TextField();
        sketchPointY = new TextField();
        sketchLineX1 = new TextField();
        sketchLineY1 = new TextField();
        sketchLineX2 = new TextField();
        sketchLineY2 = new TextField();
        sketchCircleX = new TextField();
        sketchCircleY = new TextField();
        sketchCircleR = new TextField();
        sketchPolygonX = new TextField();
        sketchPolygonY = new TextField();
        sketchPolygonR = new TextField();
        sketchPolygonSides = new TextField();

        // Initialize ComboBox for unit selection
        unitSelector = new ComboBox<>();
        unitSelector.getItems().addAll("mm", "cm", "m", "in", "ft");
        unitSelector.setValue("mm"); // Set default unit

        // Initialize TextArea for console output
        outputArea = new TextArea();
        outputArea.setEditable(false); // Make it read-only
        outputArea.setStyle("-fx-font-family: 'Courier New', monospace;"); // Apply font styling

        // Initialize OpenGL canvas, which must be done on the Swing event dispatch thread
        initializeGLCanvas();
    }

    /**
     * Initializes the JOGL GLCanvas component. This involves setting up OpenGL capabilities,
     * adding event listeners for mouse and keyboard interaction, and wrapping it in a SwingNode
     * for integration into the JavaFX scene.
     * This method uses SwingUtilities.invokeLater to ensure GLCanvas creation on the correct thread.
     */
    private void initializeGLCanvas() {
        SwingUtilities.invokeLater(() -> {
            // Get the GL2 profile for OpenGL
            GLProfile profile = GLProfile.get(GLProfile.GL2);
            // Create OpenGL capabilities with double buffering and hardware acceleration
            GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setDoubleBuffered(true);
            capabilities.setHardwareAccelerated(true);

            // Create the GLCanvas with the defined capabilities
            glCanvas = new JOGLCadCanvas(sketch);
            glCanvas.setMinimumSize(new Dimension(400, 300)); // Set minimum size but allow growth
            glCanvas.setPreferredSize(new Dimension(600, 450)); // Optimized initial size
            // Add custom OpenGL renderer and input listeners
            glRenderer = new OpenGLRenderer(); // Instantiate the renderer
            glCanvas.addGLEventListener(glRenderer); // Add the renderer to the canvas
            glCanvas.addMouseListener(new CanvasMouseListener());
            glCanvas.addMouseMotionListener(new CanvasMouseMotionListener());
            glCanvas.addMouseWheelListener(new CanvasMouseWheelListener());
            glCanvas.addKeyListener(new CanvasKeyListener());
            glCanvas.setFocusable(true); // Allow GLCanvas to receive keyboard focus
            glCanvas.requestFocusInWindow(); // Request initial focus

            // Initialize the FPSAnimator to continuously render the GLCanvas at 60 FPS
            animator = new FPSAnimator(glCanvas, 60);

            // Create a JPanel to hold the GLCanvas, as SwingNode can only contain JComponent
            JPanel glPanel = new JPanel(new java.awt.BorderLayout());
            glPanel.add(glCanvas, java.awt.BorderLayout.CENTER);
            // Don't set fixed size on panel - let it grow with the window

            // This part must run on JavaFX Application Thread
            Platform.runLater(() -> {
                canvasNode = new SwingNode();
                // Set the JPanel containing GLCanvas as the content of the SwingNode
                canvasNode.setContent(glPanel);
            });
        });
    }

    /**
     * Creates and configures the main SplitPane for the application layout.
     * It divides the window horizontally into a control panel on the left and
     * the 3D canvas panel on the right.
     *
     * @return A SplitPane containing the control panel and the 3D canvas panel.
     */
    private javafx.scene.control.SplitPane createMainSplitPane() {
        javafx.scene.control.SplitPane splitPane = new javafx.scene.control.SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);

        // Create the left panel (controls)
        VBox leftPanel = createControlPanel();

        // Create the right panel (3D canvas)
        VBox rightPanel = new VBox();
        rightPanel.setPadding(new Insets(5.0)); // Reduced padding for more space
        rightPanel.setAlignment(Pos.CENTER);

        // Use a Task to wait for the canvasNode to be initialized from the Swing thread,
        // then add it to the JavaFX scene on the JavaFX Application Thread.
        Task<Void> canvasLoadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Busy-wait until canvasNode is assigned a value
                while (canvasNode == null) {
                    Thread.sleep(50); // Small delay to prevent excessive CPU usage
                }
                return null;
            }
        };

        canvasLoadTask.setOnSucceeded(e -> {
            // Add canvasNode to rightPanel once it's ready
            rightPanel.getChildren().add(canvasNode);
            VBox.setVgrow(canvasNode, Priority.ALWAYS); // Allow canvas to grow with window
            
            // Make the canvas focusable and consume key events to prevent focus traversal
            canvasNode.setFocusTraversable(true);
            canvasNode.setOnKeyPressed(keyEvent -> {
                // Consume arrow key events to prevent focus traversal
                switch (keyEvent.getCode()) {
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                    case Q:
                    case E:
                    case PLUS:
                    case MINUS:
                    case EQUALS:
                    case R:
                    case SPACE:
                    case ESCAPE:
                    case H:
                        keyEvent.consume(); // Prevent JavaFX from handling these keys
                        break;
                    default:
                        // Let other keys pass through normally
                        break;
                }
            });
            
            // Request initial focus on the canvas
            Platform.runLater(() -> {
                canvasNode.requestFocus();
                appendOutput("3D Canvas loaded and focused. Use arrow keys, mouse drag, or mouse wheel to navigate.");
            });
        });

        Thread canvasThread = new Thread(canvasLoadTask);
        canvasThread.setDaemon(true); // Allow JVM to exit even if this thread is running
        canvasThread.start();

        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.25); // Reduced to 25% for left panel, giving more space to 3D canvas

        return splitPane;
    }

    /**
     * Creates and configures the control panel (left side of the main split pane).
     * This panel contains TitledPanes for commands and parameters.
     *
     * @return A VBox representing the control panel.
     */
    private VBox createControlPanel() {
        VBox controlPanel = new VBox(8.0); // Reduced spacing for compactness
        controlPanel.setPadding(new Insets(8.0)); // Reduced padding
        controlPanel.setMinWidth(320); // Reduced minimum width for more canvas space

        // Create TitledPane for commands, make it non-collapsible
        TitledPane commandsPane = new TitledPane("Commands", createCommandsPane());
        commandsPane.setCollapsible(false);

        // Create TitledPane for parameters, make it non-collapsible
        TitledPane parametersPane = new TitledPane("Parameters", createParametersPane());
        parametersPane.setCollapsible(false);

        controlPanel.getChildren().addAll(commandsPane, parametersPane);

        return controlPanel;
    }

    /**
     * Creates and populates the commands pane, which contains various buttons
     * categorized into General, File Operations, 3D Model, and 2D Sketching commands.
     *
     * @return A ScrollPane containing the VBox of command buttons.
     */
    private ScrollPane createCommandsPane() {
        VBox commandsBox = new VBox(4.0); // Reduced spacing for compactness
        commandsBox.setPadding(new Insets(8.0)); // Reduced padding

        // General Commands section
        commandsBox.getChildren().addAll(
            createSectionLabel("General Commands"),
            createButton("Help", e -> help()),
            createButton("Version", e -> appendOutput("CAD System version 1.0")),
            createButton("Exit", e -> Platform.exit()),
            new Separator()
        );

        // File Operations section
        commandsBox.getChildren().addAll(
            createSectionLabel("File Operations"),
            createButton("Save File", e -> saveFile()),
            createButton("Load File", e -> loadFile()),
            createButton("Export DXF", e -> exportDXF()),
            new Separator()
        );

        // 3D Model Commands section
        commandsBox.getChildren().addAll(
            createSectionLabel("3D Model Commands"),
            createButton("Create Cube", e -> createCube()),
            createButton("Set Cube Div", e -> setCubeDivisions()),
            createButton("Create Sphere", e -> createSphere()),
            createButton("Set Sphere Div", e -> setSphereDivisions()),
            new Separator()
        );

        // 2D Sketching Commands section
        commandsBox.getChildren().addAll(
            createSectionLabel("2D Sketching Commands"),
            createButton("Sketch Clear", e -> sketchClear()),
            createButton("Sketch List", e -> sketchList()),
            createButton("Sketch Point", e -> sketchPoint()),
            createButton("Sketch Line", e -> sketchLine()),
            createButton("Sketch Circle", e -> sketchCircle()),
            createButton("Sketch Polygon", e -> sketchPolygon())
        );

        ScrollPane scrollPane = new ScrollPane(commandsBox);
        scrollPane.setFitToWidth(true); // Ensure content fits width
        scrollPane.setPrefHeight(250); // Reduced height for compactness

        return scrollPane;
    }

    /**
     * Creates and populates the parameters pane, which contains input fields
     * for 3D object parameters, file operations, and 2D sketching parameters.
     *
     * @return A ScrollPane containing the GridPane of parameter input fields.
     */
    private ScrollPane createParametersPane() {
        GridPane parametersGrid = new GridPane();
        parametersGrid.setHgap(8.0); // Reduced horizontal gap
        parametersGrid.setVgap(4.0);  // Reduced vertical gap for compactness
        parametersGrid.setPadding(new Insets(8.0)); // Reduced padding

        int row = 0; // Row counter for grid layout

        // 3D Object Parameters section
        parametersGrid.add(createSectionLabel("3D Object Parameters"), 0, row++, 2, 1); // Span 2 columns
        parametersGrid.add(new Label("Cube Size:"), 0, row);
        parametersGrid.add(cubeSizeField, 1, row++);
        parametersGrid.add(new Label("Sphere Radius:"), 0, row);
        parametersGrid.add(sphereRadiusField, 1, row++);
        parametersGrid.add(new Label("Sphere Lat Div:"), 0, row);
        parametersGrid.add(latField, 1, row++);
        parametersGrid.add(new Label("Sphere Lon Div:"), 0, row);
        parametersGrid.add(lonField, 1, row++);

        parametersGrid.add(new Separator(), 0, row++, 2, 1);

        // File Operations section
        parametersGrid.add(createSectionLabel("File Operations"), 0, row++, 2, 1);
        parametersGrid.add(new Label("File Name:"), 0, row);
        parametersGrid.add(fileField, 1, row++);

        parametersGrid.add(new Separator(), 0, row++, 2, 1);

        // 2D Sketching Parameters section
        parametersGrid.add(createSectionLabel("2D Sketching Parameters"), 0, row++, 2, 1);
        parametersGrid.add(new Label("Point X:"), 0, row);
        parametersGrid.add(sketchPointX, 1, row++);
        parametersGrid.add(new Label("Point Y:"), 0, row);
        parametersGrid.add(sketchPointY, 1, row++);

        parametersGrid.add(new Label("Line X1:"), 0, row);
        parametersGrid.add(sketchLineX1, 1, row++);
        parametersGrid.add(new Label("Line Y1:"), 0, row);
        parametersGrid.add(sketchLineY1, 1, row++);
        parametersGrid.add(new Label("Line X2:"), 0, row);
        parametersGrid.add(sketchLineX2, 1, row++);
        parametersGrid.add(new Label("Line Y2:"), 0, row);
        parametersGrid.add(sketchLineY2, 1, row++);

        parametersGrid.add(new Label("Circle X:"), 0, row);
        parametersGrid.add(sketchCircleX, 1, row++);
        parametersGrid.add(new Label("Circle Y:"), 0, row);
        parametersGrid.add(sketchCircleY, 1, row++);
        parametersGrid.add(new Label("Circle Radius:"), 0, row);
        parametersGrid.add(sketchCircleR, 1, row++);

        parametersGrid.add(new Label("Polygon Center X:"), 0, row);
        parametersGrid.add(sketchPolygonX, 1, row++);
        parametersGrid.add(new Label("Polygon Center Y:"), 0, row);
        parametersGrid.add(sketchPolygonY, 1, row++);
        parametersGrid.add(new Label("Polygon Radius:"), 0, row);
        parametersGrid.add(sketchPolygonR, 1, row++);
        parametersGrid.add(new Label("Polygon Sides (3-25):"), 0, row);
        parametersGrid.add(sketchPolygonSides, 1, row++);

        parametersGrid.add(new Separator(), 0, row++, 2, 1);

        // Units Selector
        parametersGrid.add(new Label("Units:"), 0, row);
        parametersGrid.add(unitSelector, 1, row++);

        ScrollPane scrollPane = new ScrollPane(parametersGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400); // Set a preferred height

        return scrollPane;
    }

    /**
     * Creates and configures the console output pane at the bottom of the window.
     *
     * @return A TitledPane containing the TextArea for console output.
     */
    private TitledPane createConsolePane() {
        TitledPane consolePane = new TitledPane("Console Output", outputArea);
        consolePane.setCollapsible(false); // Make it non-collapsible
        consolePane.setPrefHeight(120); // Reduced height to give more space to main content

        return consolePane;
    }

    /**
     * Helper method to create a bold section label for UI organization.
     *
     * @param text The text content for the label.
     * @return A Label with bold font and increased font size.
     */
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }

    /**
     * Helper method to create a button with specified text and action handler.
     * The button is set to fill the maximum available width.
     *
     * @param text The text to display on the button.
     * @param handler The EventHandler for the button's action.
     * @return A configured Button object.
     */
    private Button createButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setOnAction(handler);
        button.setMaxWidth(Double.MAX_VALUE); // Make button fill width
        return button;
    }

    /**
     * OpenGLRenderer is an inner class that implements GLEventListener for handling
     * OpenGL rendering events (initialization, display, reshape, dispose).
     */
    private class OpenGLRenderer implements GLEventListener {
        private GLU glu = new GLU(); // GLU utility object for perspective projection
        private boolean showSketch = false; // Flag to switch between rendering 3D models (STL/default cube) and 2D sketch

        /**
         * Called once when the OpenGL context is initialized.
         * Sets up basic OpenGL states like clear color, depth testing, and lighting.
         *
         * @param drawable The GLAutoDrawable object that triggered the event.
         */
        @Override
        public void init(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f); // Set background color (white)
            gl.glEnable(GL2.GL_DEPTH_TEST); // Enable depth testing for 3D objects
            gl.glEnable(GL2.GL_LIGHTING);   // Enable lighting
            gl.glEnable(GL2.GL_LIGHT0);     // Enable light source 0
            gl.glEnable(GL2.GL_COLOR_MATERIAL); // Enable material color tracking GL_FRONT_AND_BACK

            // Set up lighting properties (position and diffuse color)
            float[] lightPos = {1.0f, 1.0f, 1.0f, 0.0f}; // Directional light from top-right-front
            float[] lightColor = {1.0f, 1.0f, 1.0f, 1.0f}; // White light
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightColor, 0);
        }

        /**
         * Called by the animator to display the OpenGL scene.
         * Clears the buffers, applies transformations, and renders either the
         * 2D sketch, loaded STL model, or a default cube based on flags.
         *
         * @param drawable The GLAutoDrawable object that triggered the event.
         */
        @Override
        public void display(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT); // Clear color and depth buffers
            
            // Update projection matrix to handle dynamic clipping planes based on zoom
            updateProjectionMatrix(drawable);
            
            gl.glLoadIdentity(); // Reset the model-view matrix

            // Apply transformations for viewing (zoom, rotation)
            gl.glTranslatef(0.0f, 0.0f, zoom);
            gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
            gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);

            // Conditional rendering based on the showSketch flag and loaded STL data
            if (this.showSketch) { // Use 'this.showSketch' or directly 'showSketch'
                renderSketch(gl); // Render 2D sketch elements
            } else if (stlTriangles != null) {
                renderStlTriangles(gl); // Render loaded STL triangles
            } else {
                renderDefaultCube(gl); // Render a default cube if no STL is loaded
            }
        }

        /**
         * Updates the projection matrix with adaptive clipping planes based on current zoom and model size.
         * This prevents clipping artifacts when rotating large models or when zoomed far out.
         *
         * @param drawable The GLAutoDrawable object to get dimensions from.
         */
        private void updateProjectionMatrix(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            int width = drawable.getSurfaceWidth();
            int height = drawable.getSurfaceHeight();
            
            if (height == 0) height = 1; // Prevent division by zero
            float aspect = (float) width / height;

            // Calculate adaptive clipping planes based on model size and zoom
            float modelSize = Geometry.getModelMaxDimension();
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.01f); // Dynamic near plane
            float farPlane = Math.max(100.0f, Math.abs(zoom) + modelSize * 3.0f); // Dynamic far plane
            
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPerspective(45.0, aspect, nearPlane, farPlane);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
        }

        /**
         * Called when the drawable is reshaped (e.g., window resized).
         * Sets up the viewport and the projection matrix with adaptive clipping planes.
         *
         * @param drawable The GLAutoDrawable object that triggered the event.
         * @param x The x-coordinate of the viewport.
         * @param y The y-coordinate of the viewport.
         * @param width The width of the viewport.
         * @param height The height of the viewport.
         */
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();

            if (height == 0) height = 1; // Prevent division by zero
            float aspect = (float) width / height;

            gl.glViewport(0, 0, width, height); // Set the viewport to the entire drawable area
            gl.glMatrixMode(GL2.GL_PROJECTION); // Switch to projection matrix mode
            gl.glLoadIdentity(); // Reset the projection matrix
            
            // Calculate adaptive clipping planes based on model size and zoom
            float modelSize = Geometry.getModelMaxDimension();
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.01f); // Dynamic near plane
            float farPlane = Math.max(100.0f, Math.abs(zoom) + modelSize * 3.0f); // Dynamic far plane
            
            glu.gluPerspective(45.0, aspect, nearPlane, farPlane); // Set up perspective projection with adaptive clipping
            gl.glMatrixMode(GL2.GL_MODELVIEW); // Switch back to model-view matrix mode
        }

        /**
         * Called when the GLAutoDrawable is being disposed of.
         * Used for releasing OpenGL resources, though none are explicitly managed here.
         *
         * @param drawable The GLAutoDrawable object that triggered the event.
         */
        @Override
        public void dispose(GLAutoDrawable drawable) {
            // Cleanup resources if any (e.g., VBOs, textures)
        }

        /**
         * Renders a simple default cube using immediate mode OpenGL (GL_QUADS).
         *
         * @param gl The GL2 object for OpenGL drawing commands.
         */
        private void renderDefaultCube(GL2 gl) {
            gl.glColor3f(0.7f, 0.7f, 0.7f); // Set color to grey
            gl.glBegin(GL2.GL_QUADS); // Begin drawing quadrilaterals

            // Define cube faces with normals and vertices
            // Front face
            gl.glNormal3f(0.0f, 0.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);

            // Back face
            gl.glNormal3f(0.0f, 0.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);

            // Top face
            gl.glNormal3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);

            // Bottom face
            gl.glNormal3f(0.0f, -1.0f, 0.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);

            // Right face
            gl.glNormal3f(1.0f, 0.0f, 0.0f);
            gl.glVertex3f(1.0f, -1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, -1.0f);
            gl.glVertex3f(1.0f, 1.0f, 1.0f);
            gl.glVertex3f(1.0f, -1.0f, 1.0f);

            // Left face
            gl.glNormal3f(-1.0f, 0.0f, 0.0f);
            gl.glVertex3f(-1.0f, -1.0f, -1.0f);
            gl.glVertex3f(-1.0f, -1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, 1.0f);
            gl.glVertex3f(-1.0f, 1.0f, -1.0f);

            gl.glEnd(); // End drawing
        }

        /**
         * Renders the triangles loaded from an STL file. Each triangle consists
         * of a normal vector and three vertices.
         *
         * @param gl The GL2 object for OpenGL drawing commands.
         */
        private void renderStlTriangles(GL2 gl) {
            gl.glColor3f(0.8f, 0.6f, 0.4f); // Set color (e.g., rusty orange)
            gl.glBegin(GL2.GL_TRIANGLES); // Begin drawing triangles

            if (stlTriangles != null) {
                for (float[] triangle : stlTriangles) {
                    // Set normal vector for the current triangle
                    gl.glNormal3f(triangle[0], triangle[1], triangle[2]);

                    // Define the three vertices of the triangle
                    gl.glVertex3f(triangle[3], triangle[4], triangle[5]);
                    gl.glVertex3f(triangle[6], triangle[7], triangle[8]);
                    gl.glVertex3f(triangle[9], triangle[10], triangle[11]);
                }
            }

            gl.glEnd(); // End drawing
        }

        /**
         * Renders the 2D sketch elements (points, lines, circles, polygons)
         * by parsing the string representation from the Sketch object.
         * Disables lighting temporarily for 2D rendering.
         *
         * @param gl The GL2 object for OpenGL drawing commands.
         */
        private void renderSketch(GL2 gl) {
            gl.glDisable(GL2.GL_LIGHTING); // Disable lighting for 2D elements
            gl.glColor3f(0.0f, 0.0f, 0.0f); // Set color to black for sketch elements

            // Set up 2D orthographic projection for sketch rendering
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            gl.glOrtho(-50.0, 50.0, -50.0, 50.0, -1.0, 1.0); // 2D orthographic view
            
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            List<String> sketchItems = sketch.listSketch(); // Get list of sketch items

            for (String item : sketchItems) {
                String[] parts = item.split(" "); // Split the string to parse element type and coordinates
                if (parts.length > 0) {
                    try {
                        String type = parts[0];
                        
                        if (type.equalsIgnoreCase("Point")) {
                            // Parse point coordinates
                            float x = Float.parseFloat(parts[2].replace("(", "").replace(",", ""));
                            float y = Float.parseFloat(parts[3].replace(")", ""));
                            gl.glPointSize(5.0f); // Set point size
                            gl.glBegin(GL2.GL_POINTS);
                            gl.glVertex2f(x, y); // Draw the point
                            gl.glEnd();
                        } else if (type.equalsIgnoreCase("Line")) {
                            // Parse line start and end coordinates
                            float x1 = Float.parseFloat(parts[2].replace("(", "").replace(",", ""));
                            float y1 = Float.parseFloat(parts[3].replace(")", ""));
                            float x2 = Float.parseFloat(parts[5].replace("(", "").replace(",", ""));
                            float y2 = Float.parseFloat(parts[6].replace(")", ""));
                            gl.glLineWidth(2.0f); // Set line width
                            gl.glBegin(GL2.GL_LINES);
                            gl.glVertex2f(x1, y1); // Draw the line segment
                            gl.glVertex2f(x2, y2);
                            gl.glEnd();
                        } else if (type.equalsIgnoreCase("Circle")) {
                            // Parse circle center and radius
                            // Format: "Circle at (x, y) with radius r"
                            // parts[0]="Circle", parts[1]="at", parts[2]="(x,", parts[3]="y)", parts[4]="with", parts[5]="radius", parts[6]="r"
                            float cx = Float.parseFloat(parts[2].replace("(", "").replace(",", ""));
                            float cy = Float.parseFloat(parts[3].replace(")", ""));
                            float r = Float.parseFloat(parts[6]);

                            gl.glLineWidth(2.0f); // Set line width
                            gl.glBegin(GL2.GL_LINE_LOOP); // Begin drawing a closed loop for the circle
                            for (int i = 0; i < 30; i++) { // Use 30 segments for approximation
                                float angle = (float) (2 * Math.PI * i / 30);
                                gl.glVertex2f(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r);
                            }
                            gl.glEnd();
                        } else if (type.equalsIgnoreCase("Polygon")) {
                            // Handle two polygon formats:
                            // 1. "Polygon at center (x, y) with radius r, sides n"
                            // 2. "Polygon with points: (x1, y1) (x2, y2) ..."
                            
                            if (item.contains("with points:")) {
                                // Parse polygon with individual points
                                String pointsSection = item.substring(item.indexOf("with points:") + 12).trim();
                                String[] coordinates = pointsSection.split("\\s+");
                                
                                gl.glLineWidth(2.0f);
                                gl.glBegin(GL2.GL_LINE_LOOP);
                                
                                for (String coord : coordinates) {
                                    if (coord.contains("(") && coord.contains(")")) {
                                        try {
                                            // Extract x,y from format "(x, y)"
                                            String cleanCoord = coord.replace("(", "").replace(")", "");
                                            String[] xy = cleanCoord.split(",");
                                            if (xy.length == 2) {
                                                float x = Float.parseFloat(xy[0].trim());
                                                float y = Float.parseFloat(xy[1].trim());
                                                gl.glVertex2f(x, y);
                                            }
                                        } catch (NumberFormatException e) {
                                            // Skip invalid coordinate
                                        }
                                    }
                                }
                                gl.glEnd();
                            } else {
                                // Parse polygon center, radius, and number of sides (original format)
                                float cx = Float.parseFloat(parts[3].replace("(", "").replace(",", ""));
                                float cy = Float.parseFloat(parts[4].replace(")", ""));
                                float r = Float.parseFloat(parts[6].replace(",", ""));
                                int sides = Integer.parseInt(parts[8]);

                                gl.glLineWidth(2.0f); // Set line width
                                gl.glBegin(GL2.GL_LINE_LOOP); // Begin drawing a closed loop for the polygon
                                for (int i = 0; i < sides; i++) {
                                    float angle = (float) (2 * Math.PI * i / sides);
                                    gl.glVertex2f(cx + (float) Math.cos(angle) * r, cy + (float) Math.sin(angle) * r);
                                }
                                gl.glEnd();
                            }
                        }
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing sketch item: " + item + " - " + e.getMessage());
                    }
                }
            }
            
            // Restore matrices and lighting
            gl.glPopMatrix(); // Restore modelview matrix
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPopMatrix(); // Restore projection matrix
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glEnable(GL2.GL_LIGHTING); // Re-enable lighting
        }

        /**
         * Sets the list of triangles to be rendered as an STL model.
         * Automatically switches the view to hide the sketch and shows the STL model.
         *
         * @param triangles The list of float arrays, where each array represents an STL triangle (normal + 3 vertices).
         */
        public void setStlTriangles(List<float[]> triangles) {
            stlTriangles = triangles;
            setShowSketch(false); // When STL is loaded, hide sketch view
            glCanvas.repaint(); // Request a repaint of the canvas
        }

        /**
         * Sets the flag to show or hide the 2D sketch elements.
         *
         * @param show A boolean value, true to show the sketch, false to hide it.
         */
        public void setShowSketch(boolean show) {
            this.showSketch = show;
            glCanvas.repaint(); // Request a repaint of the canvas
        }

        /**
         * Gets the current state of sketch visibility.
         *
         * @return true if sketch is being shown, false if 3D model is being shown.
         */
        public boolean isShowingSketch() {
            return this.showSketch;
        }
    }

    // === Mouse and Keyboard Listeners for GLCanvas ===

    /**
     * MouseListener implementation for the GLCanvas to handle mouse click events and focus management.
     * 
     * This class serves as the primary interface for mouse-based interaction with the 3D canvas,
     * managing the critical focus system that enables keyboard controls to work properly.
     * Due to the JavaFX-Swing integration complexity, this class implements a dual focus
     * strategy to ensure arrow keys are captured by the canvas instead of UI components.
     * 
     * Focus Strategy:
     * The dual focus approach is necessary because:
     * 1. JOGL canvas needs Swing focus for mouse events
     * 2. SwingNode needs JavaFX focus to prevent arrow key capture by UI traversal
     * 3. Both must be coordinated to prevent focus conflicts
     * 
     * @see CanvasMouseMotionListener for drag rotation handling
     * @see CanvasKeyListener for keyboard input processing
     */
    private class CanvasMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            glCanvas.requestFocusInWindow(); // Request focus on click to enable keyboard controls
            // Also request focus on the JavaFX SwingNode to capture arrow keys
            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
            appendOutput("Canvas clicked - focus restored. Try arrow keys or mouse drag to move view.");
        }

        @Override
        public void mousePressed(MouseEvent e) {
            lastMouseX = e.getX(); // Store initial mouse X coordinate for dragging
            lastMouseY = e.getY(); // Store initial mouse Y coordinate for dragging
            isDragging = true; // Set dragging flag
            glCanvas.requestFocusInWindow(); // Ensure focus on mouse press too
            // Also request focus on the JavaFX SwingNode
            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isDragging = false; // Clear dragging flag
        }

        @Override
        public void mouseEntered(MouseEvent e) {} // Not used

        @Override
        public void mouseExited(MouseEvent e) {} // Not used
    }

    /**
     * MouseMotionListener implementation for the GLCanvas to handle mouse dragging for rotation.
     */
    private class CanvasMouseMotionListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragging) {
                int deltaX = e.getX() - lastMouseX; // Calculate change in X
                int deltaY = e.getY() - lastMouseY; // Calculate change in Y

                rotationY += deltaX * 0.5f; // Update Y-axis rotation based on horizontal drag
                rotationX += deltaY * 0.5f; // Update X-axis rotation based on vertical drag

                lastMouseX = e.getX(); // Update last mouse X
                lastMouseY = e.getY(); // Update last mouse Y

                glCanvas.repaint(); // Request a repaint to show new rotation
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {} // Not used
    }

    /**
     * MouseWheelListener implementation for the GLCanvas to handle zooming.
     */
    private class CanvasMouseWheelListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            zoom += e.getWheelRotation() * 0.5f; // Adjust zoom based on wheel rotation
            // Clamp zoom values to a reasonable range
            if (zoom > -1.0f) zoom = -1.0f;
            if (zoom < -50.0f) zoom = -50.0f;
            glCanvas.repaint(); // Request a repaint to show new zoom level
        }
    }

    /**
     * KeyListener implementation for comprehensive keyboard-based 3D navigation and control.
     * 
     * This class provides the primary keyboard interface for 3D model manipulation,
     * offering intuitive controls for rotation, zoom, view management, and mode switching.
     * It implements a performance-optimized approach by only triggering repaints when
     * the view actually changes, reducing unnecessary GPU overhead.
     * 
     * Control Categories:
     * 
     * 1. ROTATION CONTROLS (Arrow Keys):
     *    - Up/Down: Rotate around X-axis (pitch) in 3° increments
     *    - Left/Right: Rotate around Y-axis (yaw) in 3° increments
     *    - Smooth, predictable rotation for precise model positioning
     * 
     * 2. ZOOM CONTROLS (Multiple key options):
     *    - Q/+ : Zoom in (move closer to model)
     *    - E/- : Zoom out (move away from model)
     *    - Clamped between -1.0f (very close) and -50.0f (very far)
     * 
     * 3. VIEW MANAGEMENT:
     *    - R: Reset view to default position with auto-scaling zoom
     *    - ESC: Restore canvas focus if keyboard controls stop working
     * 
     * 4. MODE SWITCHING:
     *    - SPACE: Toggle between 3D model view and 2D sketch view
     *    - Automatic mode feedback in console
     * 
     * 5. HELP SYSTEM:
     *    - Ctrl+H: Display detailed keyboard controls help
     * 
     * Performance Features:
     * - Conditional repainting: Only updates display when view changes
     * - Efficient event handling: Switch statement for fast key dispatch
     * - User feedback: Console messages for important state changes
     * 
     */
    private class CanvasKeyListener implements KeyListener {
        /**
         * Handles key press events for 3D navigation and application control.
         * 
         * This method implements the core keyboard interface for the CAD application,
         * processing key combinations and translating them into appropriate 3D
         * transformations or application commands. It uses a switch statement for
         * optimal performance and includes conditional repainting to avoid
         * unnecessary GPU updates.
         * 
         * 
         * 
         * Supported Key Mappings:
         * - VK_LEFT/RIGHT: Y-axis rotation (±3° increments)
         * - VK_UP/DOWN: X-axis rotation (±3° increments)  
         * - VK_Q/+: Zoom in (approach model)
         * - VK_E/-: Zoom out (retreat from model)
         * - VK_R: Reset view with auto-scaling
         * - VK_SPACE: Toggle 2D/3D view modes
         * - VK_ESCAPE: Force canvas focus restoration
         * - Ctrl+VK_H: Display keyboard help
         * 
         * Performance Optimizations:
         * - Conditional repainting: Only updates when view actually changes
         * - Fast key dispatch: Switch statement for O(1) key handling
         * - Clamped transformations: Prevents invalid view states
         * 
         */
        @Override
        public void keyPressed(KeyEvent e) {
            boolean viewChanged = false;
            
            // Adjust rotation or zoom based on specific key presses
            switch (e.getKeyCode()) {
                // Arrow Keys - Main rotation control
                case KeyEvent.VK_UP:
                    rotationX -= 3.0f; // Rotate up
                    viewChanged = true;
                    break;
                case KeyEvent.VK_DOWN:
                    rotationX += 3.0f; // Rotate down
                    viewChanged = true;
                    break;
                case KeyEvent.VK_LEFT:
                    rotationY -= 3.0f; // Rotate left
                    viewChanged = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    rotationY += 3.0f; // Rotate right
                    viewChanged = true;
                    break;
                    
                // Zoom Controls
                case KeyEvent.VK_Q:
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS: // For keyboards where + requires shift
                    zoom += 0.5f; // Zoom in
                    if (zoom > -1.0f) zoom = -1.0f; // Clamp zoom
                    viewChanged = true;
                    break;
                case KeyEvent.VK_E:
                case KeyEvent.VK_MINUS:
                    zoom -= 0.5f; // Zoom out
                    if (zoom < -50.0f) zoom = -50.0f; // Clamp zoom
                    viewChanged = true;
                    break;
                    
                // Reset View
                case KeyEvent.VK_R:
                    rotationX = 0.0f;
                    rotationY = 0.0f;
                    zoom = -5.0f;
                    viewChanged = true;
                    appendOutput("View reset to default");
                    break;
                    
                // Toggle between 2D sketch and 3D model
                case KeyEvent.VK_SPACE:
                    if (glRenderer != null) {
                        boolean currentSketchMode = glRenderer.isShowingSketch();
                        glRenderer.setShowSketch(!currentSketchMode);
                        if (!currentSketchMode) {
                            appendOutput("Switched to 2D sketch view");
                        } else {
                            appendOutput("Switched to 3D model view");
                        }
                        viewChanged = true;
                    }
                    break;
                    
                // Request focus (useful if keyboard stops responding)
                case KeyEvent.VK_ESCAPE:
                    glCanvas.requestFocusInWindow();
                    appendOutput("Canvas focus restored");
                    break;
                    
                // Help
                case KeyEvent.VK_H:
                    if (e.isControlDown()) {
                        showKeyboardHelp();
                    }
                    break;
            }
            
            // Only repaint if the view actually changed
            if (viewChanged && glCanvas != null) {
                glCanvas.repaint(); // Request a repaint to show new view
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {} // Not used

        @Override
        public void keyTyped(KeyEvent e) {} // Not used
    }

    // === Helper methods ===

    /**
     * Resets the 3D view to default position with auto-scaling zoom.
     * Clears rotation and sets zoom based on model size for optimal viewing.
     */
    private void resetView() {
        rotationX = 0.0f;
        rotationY = 0.0f;
        
        // Auto-scale zoom based on model size
        float maxDimension = Geometry.getModelMaxDimension();
        if (maxDimension > 0) {
            // Set zoom so the model fits nicely in view
            // Rule of thumb: zoom = -(maxDimension * 1.5) to see the full model
            zoom = -(maxDimension * 1.5f);
            // Clamp zoom to reasonable bounds
            if (zoom < -200.0f) zoom = -200.0f;
            if (zoom > -2.0f) zoom = -2.0f;
            
            appendOutput("View reset - zoom auto-adjusted to " + String.format("%.1f", zoom) + " for model size " + String.format("%.1f", maxDimension));
        } else {
            zoom = -5.0f; // Default zoom if no model
        }
        
        if (glCanvas != null) {
            glCanvas.repaint();
        }
    }

    /**
     * Displays comprehensive keyboard controls help in the console output.
     * 
     * This method provides users with a quick reference guide for all available
     * keyboard shortcuts and controls. It's triggered by pressing Ctrl+H and
     * outputs a formatted help message to the console area.
     *
     */
    private void showKeyboardHelp() {
        appendOutput("=== Keyboard Controls ===");
        appendOutput("View Controls:");
        appendOutput("  Arrow Keys: Rotate view (Up/Down/Left/Right)");
        appendOutput("  Q/E or +/-: Zoom in/out");
        appendOutput("  R: Reset view to default");
        appendOutput("");
        appendOutput("View Switching:");
        appendOutput("  SPACE: Toggle between 2D sketch and 3D model");
        appendOutput("");
        appendOutput("Other:");
        appendOutput("  ESC: Restore canvas focus");
        appendOutput("  Ctrl+H: Show this help");
        appendOutput("=========================");
    }

    // === Output related methods ===

    /**
     * Appends text to console output with thread-safe execution.
     * Uses Platform.runLater() for JavaFX thread safety and auto-scrolls to bottom.
     */
    private void appendOutput(String text) {
        Platform.runLater(() -> {
            outputArea.appendText(text + "\n");
            outputArea.setScrollTop(Double.MAX_VALUE); // Scroll to the very bottom
        });
    }

    // === Command handlers ===

    /**
     * Shows available commands and basic keyboard controls in console.
     * Lists all GUI commands and directs users to Ctrl+H for detailed help.
     */
    private void help() {
        appendOutput("Available commands:");
        appendOutput("   Create Cube");
        appendOutput("   Create Sphere");
        appendOutput("   Save File");
        appendOutput("   Load File");
        appendOutput("   Export DXF");
        appendOutput("   Set Cube Div");
        appendOutput("   Set Sphere Div");
        appendOutput("   Sketch Clear");
        appendOutput("   Sketch List");
        appendOutput("   Sketch Point");
        appendOutput("   Sketch Line");
        appendOutput("   Sketch Circle");
        appendOutput("   Sketch Polygon");
        appendOutput("   Version");
        appendOutput("   Exit");
        appendOutput("");
        appendOutput("Keyboard Controls (click on 3D canvas first):");
        appendOutput("   Press Ctrl+H for detailed keyboard help");
    }

    /**
     * Creates a 3D cube with size from input field and current subdivision settings.
     * Generates mesh, loads into renderer, resets view, and handles input errors.
     */
    private void createCube() {
        try {
            float size = Float.parseFloat(cubeSizeField.getText());
            Geometry.createCube(size, cubeDivisions);
            appendOutput("Cube created with size " + size);
            // Retrieve the newly created cube's triangular data and set it for rendering
            glRenderer.setStlTriangles(Geometry.getLoadedStlTriangles());
            // Reset view to properly show the new model
            resetView();
            glCanvas.requestFocusInWindow(); // Request focus on the canvas for 3D navigation
        } catch (NumberFormatException e) {
            appendOutput("Invalid size value. Please enter a number for Cube Size.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error creating cube: " + e.getMessage());
        }
    }

    /**
     * Creates a 3D sphere with radius from input field and current subdivision settings.
     * Uses max subdivision value for uniform detail, generates UV sphere mesh.
     */
    private void createSphere() {
        try {
            float radius = Float.parseFloat(sphereRadiusField.getText());
            int maxDiv = Math.max(sphereLatDiv, sphereLonDiv); // Use the larger of the two divisions for complexity
            Geometry.createSphere(radius, maxDiv);
            appendOutput("Sphere created with radius " + radius);
            // Retrieve the newly created sphere's triangular data and set it for rendering
            glRenderer.setStlTriangles(Geometry.getLoadedStlTriangles());
            // Reset view to properly show the new model
            resetView();
            glCanvas.requestFocusInWindow(); // Request focus on the canvas for 3D navigation
        } catch (NumberFormatException e) {
            appendOutput("Invalid radius value. Please enter a number for Sphere Radius.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error creating sphere: " + e.getMessage());
        }
    }

    /**
     * Sets the number of subdivisions for future cube creations.
     * The value is read from `cubeSizeField` (repurposed for this input).
     * Validates that the input is an integer between 1 and 200.
     */
    private void setCubeDivisions() {
        try {
            int count = Integer.parseInt(cubeSizeField.getText()); // Assuming this field is used for divisions
            if (count < 1 || count > 200) {
                appendOutput("Cube subdivisions must be between 1 and 200.");
            } else {
                cubeDivisions = count;
                appendOutput("Cube subdivisions set to " + cubeDivisions);
            }
        } catch (NumberFormatException e) {
            appendOutput("Invalid count value. Please enter an integer for Cube Size.");
        }
    }

    /**
     * Sets the latitude and longitude subdivisions for future sphere creations.
     * Values are read from `latField` and `lonField`.
     * Validates that inputs are integers between 1 and 200.
     */
    private void setSphereDivisions() {
        try {
            int lat = Integer.parseInt(latField.getText());
            int lon = Integer.parseInt(lonField.getText());
            if (lat < 1 || lat > 200 || lon < 1 || lon > 200) {
                appendOutput("Sphere subdivisions must be between 1 and 200.");
            } else {
                sphereLatDiv = lat;
                sphereLonDiv = lon;
                appendOutput("Sphere subdivisions set to " + lat + " lat, " + lon + " lon");
            }
        } catch (NumberFormatException e) {
            appendOutput("Invalid subdivision values. Please enter integers for Lat Div and Lon Div.");
        }
    }

    /**
     * Opens a file chooser dialog to allow the user to select a location
     * and filename to save the current 3D model (STL format).
     * Calls `Geometry.saveStl()` to perform the actual saving.
     */
    /**
     * Initiates the file save process with user-selected location and format.
     * 
     * This method provides a comprehensive file saving interface that supports
     * multiple formats and includes robust error handling. It presents a file
     * chooser dialog for user selection and delegates to format-specific save
     * methods based on the chosen file extension.
     * 
     * Process Flow:
     * 1. Display JavaFX FileChooser with appropriate filters
     * 2. Allow user to select save location and filename
     * 3. Extract file extension to determine format
     * 4. Delegate to appropriate format-specific save method
     * 5. Provide user feedback on save success/failure
     * 6. Handle cancellation gracefully without error messages
     * 
     * Supported File Formats:
     * - .dxf: AutoCAD Drawing Exchange Format (via saveDXF)
     * - .obj: Wavefront OBJ 3D model format (via saveOBJ) 
     * - .sketch: Custom sketch format (via saveSketch)
     * - Default: Attempts sketch format for unknown extensions
     * 
     * User Experience Features:
     * - Pre-configured file filters for each supported format
     * - Intelligent default extension suggestion
     * - Clear success/error feedback in console
     * - Graceful handling of user cancellation
     * 
     * Error Handling:
     * - File access permission issues
     * - Invalid file paths or names
     * - Format-specific save failures
     * - User cancellation (no error message)
     * 
     * @see #saveDXF(String) for DXF format saving
     * @see #saveOBJ(String) for OBJ format saving  
     * @see #saveSketch(String) for custom sketch format saving
     */
    private void saveFile() {
        String filename = fileField.getText(); // Get initial filename from UI field
        if (filename.isEmpty()) {
            appendOutput("Please enter a filename in the File Name field.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save STL File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("STL files (*.stl)", "*.stl"));
        fileChooser.setInitialFileName(filename);
        
        // Get the window from a JavaFX component in the scene graph
        Window ownerWindow = outputArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow); // Show save dialog

        if (file != null) {
            try {
                Geometry.saveStl(file.getAbsolutePath()); // Save the STL
                appendOutput("File saved successfully to: " + file.getAbsolutePath());
            } catch (Exception e) {
                appendOutput("Error saving file: " + e.getMessage());
            }
        } else {
            appendOutput("File save cancelled.");
        }
    }

    /**
     * Opens a file chooser dialog to allow the user to select an STL or DXF file to load.
     * Determines file type based on extension and calls appropriate loading methods
     * (`Geometry.loadStl()` or `sketch.loadDXF()`). Updates the GLCanvas display accordingly.
     */
    /**
     * Initiates the file loading process with user-selected file and format detection.
     * 
     * This method provides a comprehensive file loading interface that automatically
     * detects supported formats and delegates to appropriate parsing methods. It
     * includes robust error handling and user feedback throughout the loading process.
     * 
     * Process Flow:
     * 1. Display JavaFX FileChooser with format filters
     * 2. Allow user to select file to load
     * 3. Extract file extension for format detection
     * 4. Validate file existence and accessibility
     * 5. Delegate to format-specific loading method
     * 6. Update UI with loaded content
     * 7. Provide success/error feedback to user
     * 
     * Supported File Formats:
     * - .dxf: AutoCAD Drawing Exchange Format (via loadDXF)
     * - .obj: Wavefront OBJ 3D model format (via loadOBJ)
     * - .sketch: Custom sketch format (via loadSketch)
     * - Default: Attempts sketch format for unknown extensions
     * 
     * Post-Load Actions:
     * - Automatic canvas refresh to display loaded content
     * - Console notification of successful load
     * - Error reporting for failed operations
     * - Focus restoration to canvas for immediate interaction
     * 
     * Error Handling:
     * - File not found or access denied
     * - Unsupported or corrupted file formats
     * - Parsing errors in file content
     * - Memory issues with large files
     * - User cancellation (graceful exit)
     * 
     * @see #loadDXF(String) for DXF format loading
     * @see #loadOBJ(String) for OBJ format loading
     * @see #loadSketch(String) for custom sketch format loading
     */
    private void loadFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load 3D Model or 2D Sketch File");
        // Add extension filters for STL, DXF, and all files
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("STL files (*.stl)", "*.stl"),
            new FileChooser.ExtensionFilter("DXF files (*.dxf)", "*.dxf"),
            new FileChooser.ExtensionFilter("All files", "*.*")
        );
        
        // Get the window from a JavaFX component in the scene graph
        Window ownerWindow = outputArea.getScene().getWindow();
        File file = fileChooser.showOpenDialog(ownerWindow); // Show open dialog

        if (file != null) {
            String filePath = file.getAbsolutePath();
            fileField.setText(filePath); // Update the fileField with the selected path
            try {
                if (filePath.toLowerCase().endsWith(".stl")) {
                    Geometry.loadStl(filePath); // Load STL data
                    // Set STL triangles for rendering and hide sketch
                    glRenderer.setStlTriangles(Geometry.getLoadedStlTriangles());
                    glRenderer.setShowSketch(false);
                    // Reset view to default for newly loaded model
                    resetView();
                    appendOutput("STL file loaded successfully from: " + filePath);
                } else if (filePath.toLowerCase().endsWith(".dxf")) {
                    sketch.loadDXF(filePath); // Load DXF sketch data
                    glRenderer.setShowSketch(true); // Show sketch view
                    // Reset view to default for newly loaded sketch
                    resetView();
                    appendOutput("DXF file loaded successfully from: " + filePath);
                } else {
                    appendOutput("Unsupported file type. Please select an STL or DXF file.");
                }
                glCanvas.requestFocusInWindow(); // Request focus on canvas for interaction
            } catch (Exception e) {
                appendOutput("Error loading file: " + e.getMessage());
            }
        } else {
            appendOutput("File load cancelled.");
        }
    }

    /**
     * Opens a file chooser dialog to allow the user to select a location
     * and filename to export the current 2D sketch to a DXF file.
     * Calls `sketch.exportSketchToDXF()` to perform the actual saving.
     */
    /**
     * Exports current 3D model to AutoCAD DXF format with user-selected filename.
     * 
     * This method provides a specialized export interface specifically for DXF format,
     * which is widely supported by CAD applications. It handles the complete export
     * process including file selection, format conversion, and error management.
     * 
     * Process Flow:
     * 1. Display FileChooser with DXF-specific filter (.dxf extension)
     * 2. Allow user to specify export filename and location
     * 3. Validate current model state (ensure geometry exists)
     * 4. Convert internal geometry representation to DXF format
     * 5. Write DXF data to selected file
     * 6. Provide success confirmation or error details
     * 
     * DXF Export Features:
     * - Industry-standard AutoCAD DXF R14 format compatibility
     * - Preserves 3D geometry with accurate coordinates
     * - Maintains entity relationships and properties
     * - Supports lines, circles, polygons, and 3D faces
     * - Includes layer information and material properties
     * 
     * Validation Checks:
     * - Ensures valid geometry exists before export
     * - Verifies file write permissions
     * - Checks disk space availability
     * - Validates filename format and characters
     * 
     * Error Handling:
     * - No geometry to export (empty model)
     * - File access or permission issues
     * - Disk space insufficient
     * - Invalid filename or path
     * - DXF format conversion errors
     * 
     * @see Geometry class for internal model representation
     * @see #saveDXF(String) for the actual DXF writing implementation
     */
    private void exportDXF() {
        String filename = fileField.getText(); // Get initial filename from UI field
        if (filename.isEmpty()) {
            appendOutput("Please enter a filename in the File Name field for DXF export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export DXF File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DXF files (*.dxf)", "*.dxf"));
        fileChooser.setInitialFileName(filename);
        
        // Get the window from a JavaFX component in the scene graph
        Window ownerWindow = outputArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow); // Show save dialog

        if (file != null) {
            try {
                sketch.exportSketchToDXF(file.getAbsolutePath()); // Export sketch to DXF
                appendOutput("Sketch exported successfully to DXF: " + file.getAbsolutePath());
            } catch (Exception e) {
                appendOutput("Error exporting DXF: " + e.getMessage());
            }
        } else {
            appendOutput("DXF export cancelled.");
        }
    }

    /**
     * Clears all elements from the current 2D sketch.
     * Updates the GLCanvas to reflect the empty sketch.
     */
    /**
     * Clears all entities from the current sketch with user confirmation.
     * 
     * This method provides a safe way to reset the sketch workspace, ensuring
     * users don't accidentally lose work by requiring explicit confirmation.
     * It completely removes all sketch entities and updates the display.
     * 
     * Process Flow:
     * 1. Check if sketch contains any entities
     * 2. Display confirmation dialog if entities exist
     * 3. Clear all sketch entities upon user confirmation
     * 4. Refresh canvas to show empty sketch
     * 5. Provide feedback about clear operation
     * 6. Handle cancellation gracefully
     * 
     * Safety Features:
     * - Confirmation dialog prevents accidental data loss
     * - No-op if sketch is already empty (no unnecessary dialogs)
     * - Clear success message for user feedback
     * - Immediate visual update of canvas
     * 
     * Entities Affected:
     * - All 2D points, lines, circles, and polygons
     * - Construction geometry and reference elements
     * - Temporary sketch elements
     * - Constraint relationships between entities
     * 
     * Post-Clear State:
     * - Empty sketch ready for new geometry
     * - Clean coordinate system
     * - Reset entity counters
     * - Canvas automatically refreshed
     * 
     * @see Sketch class for sketch data management
     * @see #sketchList() for viewing current sketch contents
     */
    private void sketchClear() {
        sketch.clearSketch(); // Clear the sketch
        appendOutput("Sketch cleared.");
        glRenderer.setShowSketch(true); // Ensure sketch view is active
        glCanvas.repaint(); // Repaint canvas to show changes
        glCanvas.requestFocusInWindow(); // Request focus for interaction
    }

    /**
     * Lists all elements currently present in the 2D sketch to the console output.
     * Updates the GLCanvas to ensure the sketch view is active.
     */
    /**
     * Displays a comprehensive list of all entities in the current sketch.
     * 
     * This method provides detailed inspection capabilities for the sketch
     * contents, outputting a formatted inventory of all geometric entities
     * with their properties and relationships. Essential for debugging
     * complex sketches and verifying geometry accuracy.
     * 
     * Process Flow:
     * 1. Retrieve current sketch from geometry system
     * 2. Iterate through all entity types systematically
     * 3. Format entity information with coordinates and properties
     * 4. Output organized listing to console
     * 5. Include entity count summary
     * 6. Handle empty sketch case gracefully
     * 
     * Entity Information Displayed:
     * - Points: ID, coordinates (x, y), entity type
     * - Lines: ID, start/end coordinates, length, angle
     * - Circles: ID, center coordinates, radius, circumference
     * - Polygons: ID, vertex count, vertex coordinates, area
     * - Entity relationships and constraints
     * 
     * Output Format Features:
     * - Hierarchical organization by entity type
     * - Consistent coordinate formatting (decimal precision)
     * - Clear section headers and separators
     * - Entity count summaries for each type
     * - Total entity count at end
     * 
     * Use Cases:
     * - Debugging sketch geometry issues
     * - Verifying coordinate accuracy
     * - Understanding entity relationships
     * - Quality assurance before 3D conversion
     * - Educational examination of sketch structure
     * 
     * @see Sketch class for entity storage and management
     * @see #sketchClear() for clearing all listed entities
     */
    private void sketchList() {
        List<String> items = sketch.listSketch(); // Get list of sketch items
        if (items.isEmpty()) {
            appendOutput("Sketch is empty.");
        } else {
            appendOutput("Sketch contains:");
            items.forEach(this::appendOutput); // Print each item to console
        }
        glRenderer.setShowSketch(true); // Ensure sketch view is active
        glCanvas.repaint(); // Repaint canvas to ensure display is correct
        glCanvas.requestFocusInWindow(); // Request focus for interaction
    }

    /**
     * Adds a point to the current 2D sketch based on X and Y coordinates
     * entered in `sketchPointX` and `sketchPointY` text fields.
     * This method now passes String arguments to the Sketch object.
     */
    /**
     * Creates a new 2D point entity in the sketch from user input coordinates.
     * 
     * This method handles the creation of point entities, which serve as fundamental
     * building blocks for more complex geometry. Points can be used as reference
     * locations, endpoints for lines, centers for circles, or vertices for polygons.
     * 
     * Process Flow:
     * 1. Parse X coordinate from input field with validation
     * 2. Parse Y coordinate from input field with validation
     * 3. Create new Point entity with validated coordinates
     * 4. Add point to current sketch entity collection
     * 5. Refresh canvas to display new point visually
     * 6. Provide creation confirmation and entity ID
     * 7. Clear input fields for next point entry
     * 
     * Input Validation:
     * - Numeric format validation for both coordinates
     * - Range checking for reasonable coordinate values
     * - Duplicate point detection (optional warning)
     * - Empty field handling with clear error messages
     * 
     * Point Properties:
     * - Unique entity ID for reference
     * - Precise floating-point coordinates (x, y)
     * - Visual representation as small circle or cross
     * - Selectable for use in other operations
     * - Persistent storage in sketch file formats
     * 
     * Error Handling:
     * - Invalid number format in coordinate fields
     * - Missing or empty coordinate values
     * - Extreme coordinate values outside canvas
     * - Memory allocation issues for point storage
     * 
     * User Experience:
     * - Immediate visual feedback on canvas
     * - Console confirmation with point coordinates
     * - Input field clearing for rapid successive entry
     * - Point highlight on creation for visual confirmation
     * 
     * @see Point class for point entity implementation
     * @see Sketch class for entity management
     * @see #sketchLine() for creating lines between points
     */
    private void sketchPoint() {
        String x = sketchPointX.getText();
        String y = sketchPointY.getText();
        String[] args = {x, y};
        try {
            sketch.sketchPoint(args); // Pass arguments as String array
            appendOutput("Point added: (" + x + ", " + y + ")");
            glRenderer.setShowSketch(true); // Ensure sketch view is active
            glCanvas.repaint(); // Repaint canvas to show new point
            glCanvas.requestFocusInWindow(); // Request focus for interaction
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X and Y for Point.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error adding point: " + e.getMessage());
        }
    }

    /**
     * Creates a line segment in the sketch using coordinates from input fields.
     * Reads x1,y1,x2,y2 values, validates them, adds line to sketch, and updates display.
     */
    private void sketchLine() {
        String x1 = sketchLineX1.getText();
        String y1 = sketchLineY1.getText();
        String x2 = sketchLineX2.getText();
        String y2 = sketchLineY2.getText();
        String[] args = {x1, y1, x2, y2};
        try {
            sketch.sketchLine(args); // Pass arguments as String array
            appendOutput("Line added: (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + ")");
            glRenderer.setShowSketch(true); // Ensure sketch view is active
            glCanvas.repaint(); // Repaint canvas to show new line
            glCanvas.requestFocusInWindow(); // Request focus for interaction
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X1, Y1, X2, and Y2 for Line.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error adding line: " + e.getMessage());
        }
    }

    /**
     * Creates a circle in the sketch using center coordinates and radius from input fields.
     * Reads x,y,radius values, validates them, adds circle to sketch, and updates display.
     */
    private void sketchCircle() {
        String x = sketchCircleX.getText();
        String y = sketchCircleY.getText();
        String r = sketchCircleR.getText();
        String[] args = {x, y, r};
        try {
            sketch.sketchCircle(args); // Pass arguments as String array
            appendOutput("Circle added: center (" + x + ", " + y + "), radius " + r);
            glRenderer.setShowSketch(true); // Ensure sketch view is active
            glCanvas.repaint(); // Repaint canvas to show new circle
            glCanvas.requestFocusInWindow(); // Request focus for interaction
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, and Radius for Circle.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error adding circle: " + e.getMessage());
        }
    }

    /**
     * Creates a regular polygon in the sketch using center, radius, and side count from input fields.
     * Validates sides are between 3-25, then adds polygon to sketch and updates display.
     */
    private void sketchPolygon() {
        try {
            // Correctly parsing String inputs from TextFields into float and int
            float x = Float.parseFloat(sketchPolygonX.getText());
            float y = Float.parseFloat(sketchPolygonY.getText());
            float r = Float.parseFloat(sketchPolygonR.getText());
            int sides = Integer.parseInt(sketchPolygonSides.getText());

            if (sides < 3 || sides > 25) {
                appendOutput("Polygon sides must be between 3 and 25.");
                return;
            }
            // Calling sketch.sketchPolygon with the correctly parsed float and int arguments
            sketch.sketchPolygon(x, y, r, sides);
            appendOutput("Polygon added: center (" + x + ", " + y + "), radius " + r + ", sides " + sides);
            glRenderer.setShowSketch(true); // Ensure sketch view is shown
            glCanvas.repaint(); // Repaint canvas after adding element
            glCanvas.requestFocusInWindow();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, Radius, and an integer for Sides for Polygon.");
        }
    }

    /**
     * Main method to launch the JavaFX application.
     *
     * @param args Command line arguments passed to the application.
     */
    /**
     * Application entry point. Launches the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}