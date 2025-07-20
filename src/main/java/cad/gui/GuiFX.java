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
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane; // Explicitly import SplitPane
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window; // Import Window

import java.util.Optional;

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
 * 
 * Navigation Controls:
 * - Mouse: Drag to rotate, wheel to zoom, click to focus
 * - Keyboard: Arrow keys for rotation, Q/E for zoom, R for reset
 * - Space: Toggle between 3D and 2D views
 * - ESC: Restore canvas focus if needed
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
    
    // === 2D Sketch view variables ===
    private float sketch2DPanX = 0.0f; // Pan offset in X direction for 2D sketch view
    private float sketch2DPanY = 0.0f; // Pan offset in Y direction for 2D sketch view
    private float sketch2DZoom = 1.0f; // Zoom level for 2D sketch view (1.0 = default, >1.0 = zoomed in)
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
            cleanupAndExit();
        });

        primaryStage.show();

        // Start OpenGL animation if the animator is initialized
        if (animator != null) {
            animator.start();
        }
    }

    /**
     * Properly cleans up resources and exits the application.
     * This method should be called whenever the application needs to terminate,
     * whether through window close, exit button, or programmatic exit.
     * It ensures all resources are properly released before termination.
     */
    private void cleanupAndExit() {
        try {
            // Stop the OpenGL animator immediately
            if (animator != null) {
                if (animator.isAnimating()) {
                    animator.stop();
                }
                // Give a very short time for cleanup, but don't wait too long
                Thread.sleep(50);
            }
            
            // Dispose of the OpenGL context if possible
            if (glCanvas != null) {
                glCanvas.destroy();
            }
            
        } catch (Exception e) {
            // Ignore cleanup errors and force exit
            System.err.println("Warning during cleanup: " + e.getMessage());
        }
        
        // Force immediate exit - don't wait for JavaFX platform
        Platform.runLater(() -> {
            Platform.exit();
            // Use a separate thread to force exit after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    System.exit(0);
                } catch (InterruptedException ignored) {
                    System.exit(0);
                }
            }).start();
        });
        
        // Also try to exit from this thread as backup
        new Thread(() -> {
            try {
                Thread.sleep(200);
                System.exit(0);
            } catch (InterruptedException ignored) {
                System.exit(0);
            }
        }).start();
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
            capabilities.setHardwareAccelerated(false);

            // Create the GLCanvas with the defined capabilities
            glCanvas = new JOGLCadCanvas(sketch);
            glCanvas.setMinimumSize(new Dimension(600, 450)); // Increased minimum size for larger models
            glCanvas.setPreferredSize(new Dimension(1200, 900)); // Much larger initial size for better viewing
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
        splitPane.setDividerPositions(0.20); // Reduced to 20% for left panel, giving even more space to 3D canvas

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
        controlPanel.setMinWidth(280); // Further reduced minimum width for more canvas space

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
            createButton("Exit", e -> cleanupAndExit()),
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
            // createButton("Extrude Sketch", e -> extrudeSketch())
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
            
            // Conditional rendering based on the showSketch flag and loaded STL data
            if (this.showSketch) {
                // For 2D sketch rendering, don't apply 3D transformations
                renderSketch(gl); // Render 2D sketch elements
            } else {
                // For 3D rendering, apply transformations and update projection matrix
                updateProjectionMatrix(drawable);
                
                gl.glLoadIdentity(); // Reset the model-view matrix

                // Apply transformations for viewing (zoom, rotation)
                gl.glTranslatef(0.0f, 0.0f, zoom);
                gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
                gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);

                if (stlTriangles != null) {
                    renderStlTriangles(gl); // Render loaded STL triangles
                } else {
                    renderDefaultCube(gl); // Render a default cube if no STL is loaded
                }
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
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.005f); // More conservative near plane
            float farPlane = Math.max(1000.0f, Math.abs(zoom) + modelSize * 5.0f); // Increased far plane for large models
            
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
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.005f); // More conservative near plane
            float farPlane = Math.max(1000.0f, Math.abs(zoom) + modelSize * 5.0f); // Increased far plane for large models
            
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
         * Renders the 2D sketch elements using the Sketch class's built-in draw method.
         * This is much more efficient than parsing string representations.
         * Uses proper aspect ratio to ensure circles appear round, not oval.
         * Completely independent of 3D view transformations.
         * Supports 2D view manipulation (pan and zoom).
         *
         * @param gl The GL2 object for OpenGL drawing commands.
         */
        private void renderSketch(GL2 gl) {
            gl.glDisable(GL2.GL_LIGHTING); // Disable lighting for 2D elements
            gl.glColor3f(0.0f, 0.0f, 0.0f); // Set color to black for sketch elements

            // Completely reset and set up 2D orthographic projection
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            
            // Get current viewport dimensions to calculate aspect ratio
            int[] viewport = new int[4];
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
            int width = viewport[2];
            int height = viewport[3];
            
            // Calculate aspect ratio and set up orthographic projection with zoom
            double aspectRatio = (double) width / height;
            double baseSize = 50.0; // Base coordinate system size
            double size = baseSize / sketch2DZoom; // Apply zoom (smaller size = more zoomed in)
            
            if (aspectRatio >= 1.0) {
                // Wide screen - extend horizontal range
                gl.glOrtho(-size * aspectRatio, size * aspectRatio, -size, size, -1.0, 1.0);
            } else {
                // Tall screen - extend vertical range
                gl.glOrtho(-size, size, -size / aspectRatio, size / aspectRatio, -1.0, 1.0);
            }
            
            // Set up modelview matrix with 2D transformations
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();
            
            // Apply 2D pan transformations
            gl.glTranslatef(sketch2DPanX, sketch2DPanY, 0.0f);

            // Use the sketch's built-in draw method instead of parsing strings
            sketch.draw(gl);
            
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
     * MouseMotionListener implementation for the GLCanvas to handle mouse dragging.
     * Supports both 3D rotation (for models) and 2D panning (for sketches).
     */
    private class CanvasMouseMotionListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragging) {
                int deltaX = e.getX() - lastMouseX; // Calculate change in X
                int deltaY = e.getY() - lastMouseY; // Calculate change in Y

                if (glRenderer != null && glRenderer.isShowingSketch()) {
                    // 2D sketch mode: pan the view
                    // Convert screen delta to world coordinates based on current zoom
                    float panSpeed = 0.1f / sketch2DZoom; // Scale pan speed with zoom level
                    sketch2DPanX += deltaX * panSpeed;
                    sketch2DPanY -= deltaY * panSpeed; // Flip Y because screen Y is inverted
                } else {
                    // 3D model mode: rotate the view
                    rotationY += deltaX * 0.5f; // Update Y-axis rotation based on horizontal drag
                    rotationX += deltaY * 0.5f; // Update X-axis rotation based on vertical drag
                }

                lastMouseX = e.getX(); // Update last mouse X
                lastMouseY = e.getY(); // Update last mouse Y

                glCanvas.repaint(); // Request a repaint to show new transformation
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {} // Not used
    }

    /**
     * MouseWheelListener implementation for the GLCanvas to handle zooming.
     * Supports both 3D zoom (for models) and 2D zoom (for sketches).
     */
    private class CanvasMouseWheelListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (glRenderer != null && glRenderer.isShowingSketch()) {
                // 2D sketch mode: adjust 2D zoom
                float zoomFactor = 1.1f; // 10% zoom step
                if (e.getWheelRotation() < 0) {
                    // Scroll up: zoom in
                    sketch2DZoom *= zoomFactor;
                } else {
                    // Scroll down: zoom out
                    sketch2DZoom /= zoomFactor;
                }
                // Clamp zoom to reasonable range
                if (sketch2DZoom < 0.1f) sketch2DZoom = 0.1f; // Max zoom out
                if (sketch2DZoom > 20.0f) sketch2DZoom = 20.0f; // Max zoom in
            } else {
                // 3D model mode: adjust 3D zoom
                zoom += e.getWheelRotation() * 0.5f; // Adjust zoom based on wheel rotation
                // Clamp zoom values to a reasonable range for large models
                if (zoom > -1.0f) zoom = -1.0f;
                if (zoom < -800.0f) zoom = -800.0f; // Increased limit for large models
            }
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
     * 
     */
    private class CanvasKeyListener implements KeyListener {
        /**
         * Handles key press events for navigation and application control.
         * Supports both 3D model manipulation and 2D sketch view manipulation.
         * 
         * This method implements the core keyboard interface for the CAD application,
         * processing key combinations and translating them into appropriate view
         * transformations or application commands. It dynamically switches behavior
         * based on whether the user is in 2D sketch mode or 3D model mode.
         * 
         * 2D Sketch Mode Controls:
         * - Arrow Keys: Pan view (move sketch content)
         * - Q/E or +/-: Zoom in/out
         * - R: Reset 2D view to default
         * 
         * 3D Model Mode Controls:
         * - Arrow Keys: Rotate model
         * - Q/E or +/-: Zoom in/out
         * - R: Reset 3D view with auto-scaling
         * 
         * Universal Controls:
         * - SPACE: Toggle between 2D/3D view modes
         * - ESC: Force canvas focus restoration
         * - Ctrl+H: Display keyboard help
         * 
         */
        @Override
        public void keyPressed(KeyEvent e) {
            boolean viewChanged = false;
            boolean is2DSketchMode = (glRenderer != null && glRenderer.isShowingSketch());
            
            // Adjust view based on current mode and key presses
            switch (e.getKeyCode()) {
                // Arrow Keys - Context-sensitive controls
                case KeyEvent.VK_UP:
                    if (is2DSketchMode) {
                        // 2D mode: pan up
                        sketch2DPanY += 2.0f / sketch2DZoom; // Scale pan with zoom
                    } else {
                        // 3D mode: rotate up
                        rotationX -= 3.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_DOWN:
                    if (is2DSketchMode) {
                        // 2D mode: pan down
                        sketch2DPanY -= 2.0f / sketch2DZoom; // Scale pan with zoom
                    } else {
                        // 3D mode: rotate down
                        rotationX += 3.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_LEFT:
                    if (is2DSketchMode) {
                        // 2D mode: pan left
                        sketch2DPanX -= 2.0f / sketch2DZoom; // Scale pan with zoom
                    } else {
                        // 3D mode: rotate left
                        rotationY -= 3.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    if (is2DSketchMode) {
                        // 2D mode: pan right
                        sketch2DPanX += 2.0f / sketch2DZoom; // Scale pan with zoom
                    } else {
                        // 3D mode: rotate right
                        rotationY += 3.0f;
                    }
                    viewChanged = true;
                    break;
                    
                // Zoom Controls - Context-sensitive
                case KeyEvent.VK_Q:
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS: // For keyboards where + requires shift
                    if (is2DSketchMode) {
                        // 2D mode: zoom in
                        sketch2DZoom *= 1.2f; // 20% zoom step
                        if (sketch2DZoom > 20.0f) sketch2DZoom = 20.0f; // Clamp max zoom
                    } else {
                        // 3D mode: zoom in
                        zoom += 0.5f;
                        if (zoom > -1.0f) zoom = -1.0f; // Clamp zoom
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_E:
                case KeyEvent.VK_MINUS:
                    if (is2DSketchMode) {
                        // 2D mode: zoom out
                        sketch2DZoom /= 1.2f; // 20% zoom step
                        if (sketch2DZoom < 0.1f) sketch2DZoom = 0.1f; // Clamp min zoom
                    } else {
                        // 3D mode: zoom out
                        zoom -= 0.5f;
                        if (zoom < -800.0f) zoom = -800.0f; // Increased clamp for large models
                    }
                    viewChanged = true;
                    break;
                    
                // Reset View - Context-sensitive
                case KeyEvent.VK_R:
                    if (is2DSketchMode) {
                        // Reset 2D view
                        sketch2DPanX = 0.0f;
                        sketch2DPanY = 0.0f;
                        sketch2DZoom = 1.0f;
                        appendOutput("2D sketch view reset");
                    } else {
                        // Reset 3D view
                        resetView(); // Use the auto-scaling reset method
                        appendOutput("3D model view reset with auto-scaling");
                    }
                    viewChanged = true;
                    break;
                    
                // Toggle between 2D sketch and 3D model
                case KeyEvent.VK_SPACE:
                    if (glRenderer != null) {
                        boolean currentSketchMode = glRenderer.isShowingSketch();
                        glRenderer.setShowSketch(!currentSketchMode);
                        if (!currentSketchMode) {
                            appendOutput("Switched to 2D sketch view - Use arrow keys to pan, Q/E to zoom");
                        } else {
                            appendOutput("Switched to 3D model view - Use arrow keys to rotate, Q/E to zoom");
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
            // For very large models, use a logarithmic scaling approach
            if (maxDimension <= 10.0f) {
                // Small models: use simple scaling
                zoom = -(maxDimension * 1.8f);
            } else if (maxDimension <= 50.0f) {
                // Medium models: moderate scaling
                zoom = -(maxDimension * 1.5f);
            } else if (maxDimension <= 150.0f) {
                // Large models (like radius 60 sphere = 120 diameter): optimized scaling
                zoom = -(maxDimension * 1.2f);
            } else {
                // Very large models: use more conservative scaling with logarithmic component
                zoom = -(maxDimension * 0.8f + (float)Math.log10(maxDimension) * 15.0f);
            }
            
            // Clamp zoom to reasonable bounds
            if (zoom < -800.0f) zoom = -800.0f;  // Further increased upper limit for very large models
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
     * outputs a formatted help message to the console area. Updated to include
     * both 2D sketch manipulation and 3D model controls.
     *
     */
    private void showKeyboardHelp() {
        appendOutput("=== Keyboard Controls ===");
        appendOutput("3D Model View Controls:");
        appendOutput("  Arrow Keys: Rotate model (Up/Down/Left/Right)");
        appendOutput("  Q/E or +/-: Zoom in/out");
        appendOutput("  R: Reset view to default with auto-scaling");
        appendOutput("");
        appendOutput("2D Sketch View Controls:");
        appendOutput("  Arrow Keys: Pan sketch view (Up/Down/Left/Right)");
        appendOutput("  Q/E or +/-: Zoom in/out");
        appendOutput("  R: Reset view to center with default zoom");
        appendOutput("");
        appendOutput("Universal Controls:");
        appendOutput("  SPACE: Toggle between 2D sketch and 3D model");
        appendOutput("  ESC: Restore canvas focus");
        appendOutput("  Ctrl+H: Show this help");
        appendOutput("");
        appendOutput("Mouse Controls:");
        appendOutput("  3D Mode: Drag to rotate, wheel to zoom");
        appendOutput("  2D Mode: Drag to pan, wheel to zoom");
        appendOutput("  Click: Restore focus for keyboard controls");
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
     * 
     * @see #saveDXF(String) for DXF format saving
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
     * detects supported formats and delegates to appropriate parsing methods. 
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
     * 
     * @see Point class for point entity implementation
     * @see Sketch class for entity management
     * @see #sketchLine() for creating lines between points
     */
    private void sketchPoint() {
        try {
            float x = Float.parseFloat(sketchPointX.getText());
            float y = Float.parseFloat(sketchPointY.getText());
            sketch.addPoint(x, y);
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
        try {
            float x1 = Float.parseFloat(sketchLineX1.getText());
            float y1 = Float.parseFloat(sketchLineY1.getText());
            float x2 = Float.parseFloat(sketchLineX2.getText());
            float y2 = Float.parseFloat(sketchLineY2.getText());
            sketch.addLine(x1, y1, x2, y2);
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
     * Adds a circle to the sketch from user input fields.
     * It parses the center coordinates (x, y) and the radius,
     * then adds the circle to the sketch and updates the view.
     */
    private void sketchCircle() {
        try {
            float x = Float.parseFloat(sketchCircleX.getText());
            float y = Float.parseFloat(sketchCircleY.getText());
            float r = Float.parseFloat(sketchCircleR.getText());
            sketch.addCircle(x, y, r);
            appendOutput("Circle added at (" + x + ", " + y + ") with radius " + r);
            glRenderer.setShowSketch(true); // Ensure sketch view is shown
            glCanvas.repaint(); // Repaint canvas after adding element
            glCanvas.requestFocusInWindow();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, and Radius for Circle.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error adding circle: " + e.getMessage());
        }
    }

    /**
     * Adds a regular polygon to the sketch from user input fields.
     * It parses the center coordinates, radius, and number of sides,
     * then adds the polygon to the sketch and updates the view.
     */
    private void sketchPolygon() {
        try {
            float x = Float.parseFloat(sketchPolygonX.getText());
            float y = Float.parseFloat(sketchPolygonY.getText());
            float r = Float.parseFloat(sketchPolygonR.getText());
            int sides = Integer.parseInt(sketchPolygonSides.getText());
            sketch.addNSidedPolygon(x, y, r, sides);
            appendOutput("Polygon added at (" + x + ", " + y + ") with radius " + r + " and " + sides + " sides.");
            glRenderer.setShowSketch(true); // Ensure sketch view is shown
            glCanvas.repaint(); // Repaint canvas after adding element
            glCanvas.requestFocusInWindow();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, Radius, and an integer for Sides for Polygon.");
        }
    }

    /**
     * Extrudes the current 2D sketch into a 3D shape.
     * Opens a dialog to get the extrusion height from the user.
     */
    // private void extrudeSketch() {
    //     // Check if sketch has any closed loops (polygons)
    //     if (!sketch.isClosedLoop()) {
    //         appendOutput("Error: Sketch must contain at least one polygon to extrude.");
    //         appendOutput("Tip: Use 'Sketch Polygon' button to create polygons first.");
    //         return;
    //     }

    //     // Create a dialog to get the extrusion height
    //     TextInputDialog dialog = new TextInputDialog("10.0");
    //     dialog.setTitle("Extrude Sketch");
    //     dialog.setHeaderText("Extrude 2D Sketch into 3D");
    //     dialog.setContentText("Enter extrusion height:");

    //     // Show dialog and process result
    //     Optional<String> result = dialog.showAndWait();
    //     if (result.isPresent()) {
    //         try {
    //             float height = Float.parseFloat(result.get());
    //             if (height <= 0) {
    //                 appendOutput("Error: Extrusion height must be greater than 0.");
    //                 return;
    //             }

    //             // Perform the extrusion
    //             Geometry.extrude(sketch, height);
                
    //             appendOutput("Successfully extruded sketch with height " + height);
    //             appendOutput("Switching to 3D view to show extruded geometry.");
                
    //             // Switch to 3D view to show the result
    //             glRenderer.setShowSketch(false);
    //             glCanvas.repaint();
    //             glCanvas.requestFocusInWindow();
                
    //         } catch (NumberFormatException e) {
    //             appendOutput("Error: Invalid height value. Please provide a numeric value.");
    //         } catch (Exception e) {
    //             appendOutput("Error during extrusion: " + e.getMessage());
    //         }
    //     }
    // }

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
