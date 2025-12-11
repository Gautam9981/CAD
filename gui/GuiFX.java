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
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.SplitPane; // Explicitly import SplitPane
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import java.util.Optional;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;

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
import cad.core.Point;
import cad.core.Sketch;

/**
 * GuiFX - Main JavaFX Application Class for CAD System
 * 
 * This class represents the primary GUI application for a Computer-Aided Design
 * (CAD) system
 * built using JavaFX with an embedded JOGL (Java OpenGL) canvas for 3D
 * rendering.
 * 
 * Key Features:
 * - Interactive 3D model viewing and manipulation using OpenGL
 * - Support for STL file import/export and DXF file operations
 * - Real-time 3D model generation (cubes, spheres with customizable
 * subdivisions)
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
    private TextField sketchKiteCenterX, sketchKiteCenterY, sketchKiteDiagV, sketchKiteDiagH, sketchKiteAngle;
    private ComboBox<String> unitSelector;
    private JOGLCadCanvas glCanvas; // This is an AWT component where OpenGL rendering happens
    private SwingNode canvasNode; // This wraps the AWT GLCanvas for integration into JavaFX scene graph
    private FPSAnimator animator; // Manages the animation loop for the GLCanvas
    private OpenGLRenderer glRenderer; // Reference to the OpenGLRenderer instance
    private SketchInteractionManager interactionManager; // Handles interactive sketching logic

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
    // private boolean showSketch = false; // This field is now managed directly by
    // OpenGLRenderer via setter

    /**
     * The main entry point for the JavaFX application.
     * This method is called after the launch() method is invoked.
     * It initializes the UI, sets up the scene, and displays the primary stage.
     *
     * 
     */
    /**
     * The main entry point for the JavaFX application.
     * Initializes the SolidWorks-style layout.
     */
    @Override
    public void start(Stage primaryStage) {
        sketch = new Sketch(); // Initialize the Sketch object
        interactionManager = new SketchInteractionManager(sketch); // Init interaction manager

        primaryStage.setTitle("SketchApp (4.0)");

        // Initialize components (fields, etc.)
        initializeComponents();

        // Main Layout (BorderPane)
        BorderPane root = new BorderPane();

        // 1. Top: Ribbon (CommandManager)
        TabPane ribbon = createRibbon();
        root.setTop(ribbon);

        // 2. Center: 3D Viewport with Heads-Up Toolbar
        // We wrap the canvasNode in a StackPane to overlay the Heads-Up Toolbar
        StackPane viewportStack = new StackPane();
        viewportStack.getStyleClass().add("viewport-stack"); // Use CSS class instead of inline style

        // Heads-Up View Toolbar (Floating in Viewport)
        ToolBar headsUpToolbar = new ToolBar();
        headsUpToolbar.setMaxWidth(180);
        headsUpToolbar.setMaxHeight(30);
        StackPane.setAlignment(headsUpToolbar, Pos.TOP_CENTER);
        StackPane.setMargin(headsUpToolbar, new Insets(10));

        Button btnIso = new Button("[ISO]");
        btnIso.setTooltip(new Tooltip("Isometric View"));
        btnIso.setOnAction(e -> setViewIsometric());

        Button btnFront = new Button("[F]");
        btnFront.setTooltip(new Tooltip("Front View"));
        btnFront.setOnAction(e -> setViewFront());

        Button btnTop = new Button("[T]");
        btnTop.setTooltip(new Tooltip("Top View"));
        btnTop.setOnAction(e -> setViewTop());

        Button btnRight = new Button("[R]");
        btnRight.setTooltip(new Tooltip("Right View"));
        btnRight.setOnAction(e -> setViewRight());

        headsUpToolbar.getItems().addAll(btnIso, btnFront, btnTop, btnRight);
        viewportStack.getChildren().add(headsUpToolbar);

        // Initialize JOGL Canvas (CanvasNode will be added async)
        initializeCanvasAsync(viewportStack);

        root.setCenter(viewportStack);

        // 3. Left: Feature Manager Design Tree (now a TabPane)
        TabPane featureManager = createControlPanel();

        // 4. Right: Property Manager / Task Pane (Placeholder for now)
        TabPane taskPane = createTaskPane();

        // Combine Left, Center, Right using SplitPane for resizability
        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getItems().addAll(featureManager, root.getCenter(), taskPane);
        horizontalSplit.setDividerPositions(0.2, 0.85); // 20% Tree, 65% View, 15% Task Pane

        // We need to re-set the center of BorderPane to this SplitPane
        // But the Ribbon needs to stay at top.
        // So root center is the split pane.
        root.setCenter(horizontalSplit);

        // 5. Bottom: Status Bar / Console
        TitledPane consolePane = createConsolePane();
        root.setBottom(consolePane);

        // Create scene
        Scene scene = new Scene(root, 1600, 900);
        // Load CSS
        try {
            String css = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> cleanupAndExit());
        primaryStage.setMaximized(true); // Start maximized like SW
        primaryStage.show();

        // Start OpenGL animation
        if (animator != null) {
            animator.start();
        }
    }

    // === New SolidWorks-Style Layout Methods ===

    private TabPane createRibbon() {
        TabPane ribbon = new TabPane();
        ribbon.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        // Style is now handled by CSS 'tab-pane' class mostly, but removing inline
        // styles to let CSS take over completely
        ribbon.getStyleClass().add("ribbon-pane");

        // --- SKETCH TAB ---
        Tab sketchTab = new Tab("Sketch");
        ToolBar sketchToolbar = new ToolBar();
        sketchToolbar.getItems().addAll(
                createRibbonButton("Sketch", "Create/Edit Sketch", e -> appendOutput("Sketch Mode Active")),
                createRibbonButton("Load", "Open File", e -> showLoadDialog()),
                new Separator(),
                createRibbonButton("Line", "Line Entity", e -> {
                    interactionManager.setMode(SketchInteractionManager.InteractionMode.SKETCH_LINE);
                    appendOutput("Tool: Line (Click start, click end)");
                }),
                createRibbonButton("Circle", "Circle Entity", e -> {
                    interactionManager.setMode(SketchInteractionManager.InteractionMode.SKETCH_CIRCLE);
                    appendOutput("Tool: Circle (Click center, click radius)");
                }),
                createRibbonButton("Polygon", "Polygon Entity", e -> {
                    interactionManager.setMode(SketchInteractionManager.InteractionMode.SKETCH_POLYGON);
                    appendOutput("Tool: Polygon (Click center, click radius)");
                }),
                createRibbonButton("Point", "Point Entity", e -> sketchPoint()),

                createRibbonButton("Kite", "Kite Entity", e -> sketchKite()),
                createRibbonButton("NACA", "NACA Airfoil", e -> showNacaDialog()), // New Dialog needed
                new Separator(),
                createRibbonButton("Trim", "Trim Entities", e -> appendOutput("Trim not implemented")),
                createRibbonButton("Offset", "Offset Entities", e -> appendOutput("Offset not implemented")));
        sketchTab.setContent(sketchToolbar);

        // --- FEATURES TAB ---
        Tab featuresTab = new Tab("Features");
        ToolBar featuresToolbar = new ToolBar();
        featuresToolbar.getItems().addAll(
                createRibbonButton("Extruded\nBoss/Base", "Extrude Sketch", e -> extrudeSketch()),
                createRibbonButton("Revolved\nBoss/Base", "Revolve Sketch", e -> showRevolveDialog()), // New Dialog
                createRibbonButton("Swept\nBoss/Base", "Sweep Sketch", e -> appendOutput("Sweep not implemented")),
                createRibbonButton("Lofted\nBoss/Base", "Loft Sketches", e -> showLoftDialog()),
                new Separator(),
                createRibbonButton("Extruded\nCut", "Cut Material", e -> appendOutput("Cut not implemented")),
                createRibbonButton("Fillet", "Round Edges", e -> appendOutput("Fillet not implemented")));
        featuresTab.setContent(featuresToolbar);

        // --- EVALUATE TAB ---
        Tab evaluateTab = new Tab("Evaluate");
        ToolBar evaluateToolbar = new ToolBar();
        evaluateToolbar.getItems().addAll(
                createRibbonButton("Measure", "Measure Distance/Area", e -> appendOutput("Measure tool active")),
                createRibbonButton("Mass Properties", "Calculate Mass/CG", e -> calculateMassProperties()));
        evaluateTab.setContent(evaluateToolbar);

        // --- TOOLS TAB (API/Macros) ---
        Tab toolsTab = new Tab("Tools");
        ToolBar toolsToolbar = new ToolBar();
        toolsToolbar.getItems().addAll(
                createRibbonButton("Record", "Record Macro", e -> appendOutput("Macro Recording Started...")),
                createRibbonButton("Stop", "Stop Recording", e -> appendOutput("Macro Recording Stopped.")),
                createRibbonButton("Run", "Run Macro", e -> appendOutput("Select Macro to Run...")),
                new Separator(),
                createRibbonButton("Add-Ins", "Manage Add-Ins", e -> appendOutput("Add-In Manager opened.")));
        toolsTab.setContent(toolsToolbar);

        ribbon.getTabs().addAll(sketchTab, featuresTab, evaluateTab, toolsTab);
        return ribbon;
    }

    private Button createRibbonButton(String text, String tooltipText,
            javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnAction(handler);
        // Remove inline style to let CSS handle it
        btn.setMinWidth(60); // Slightly smaller width
        btn.setMaxHeight(60); // Constrain height
        btn.setPrefHeight(60);
        return btn;
    }

    private VBox createFeatureManagerTree() {
        VBox container = new VBox();
        container.setMinWidth(250);
        container.setStyle("-fx-background-color: white;");

        // Tree View
        TreeItem<String> rootItem = new TreeItem<>("Part1 (Default)");
        rootItem.setExpanded(true);

        TreeItem<String> history = new TreeItem<>("History");
        TreeItem<String> sensors = new TreeItem<>("Sensors");
        TreeItem<String> annotations = new TreeItem<>("Annotations");
        TreeItem<String> material = new TreeItem<>("Material <not specified>");
        TreeItem<String> frontPlane = new TreeItem<>("Front Plane");
        TreeItem<String> topPlane = new TreeItem<>("Top Plane");
        TreeItem<String> rightPlane = new TreeItem<>("Right Plane");
        TreeItem<String> origin = new TreeItem<>("Origin");

        rootItem.getChildren().addAll(history, sensors, annotations, material, frontPlane, topPlane, rightPlane,
                origin);

        TreeView<String> tree = new TreeView<>(rootItem);
        tree.setShowRoot(true);
        VBox.setVgrow(tree, Priority.ALWAYS);

        container.getChildren().add(tree);

        return container;
    }

    private TabPane createTaskPane() {
        TabPane taskPane = new TabPane();
        taskPane.setSide(javafx.geometry.Side.RIGHT); // Tabs on the right side
        taskPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        taskPane.setMinWidth(250); // Expanded width to show content

        // Resources Tab
        Tab resourcesTab = new Tab("Resources");
        VBox resourcesContent = new VBox(10);
        resourcesContent.setPadding(new Insets(10));
        resourcesContent.setStyle("-fx-background-color: white;");

        resourcesContent.getChildren().addAll(
                new Label("SketchApp Tools"),
                new Button("Property Tab Builder"),
                new Button("Rx"),
                new Separator(),
                new Label("Online Resources"),
                new Button("Customer Portal"),
                new Button("MySketchApp"),
                new Button("User Forums"));
        resourcesTab.setContent(resourcesContent);

        // Design Library Tab
        Tab libraryTab = new Tab("Design Library");
        VBox libraryContent = new VBox();
        libraryContent.setPadding(new Insets(5));
        TreeItem<String> libRoot = new TreeItem<>("Design Library");
        libRoot.setExpanded(true);
        libRoot.getChildren().addAll(
                new TreeItem<>("Toolbox"),
                new TreeItem<>("3D Interconnect"),
                new TreeItem<>("Routing"),
                new TreeItem<>("Smart Components"));
        TreeView<String> libTree = new TreeView<>(libRoot);
        libraryContent.getChildren().add(libTree);
        libraryTab.setContent(libraryContent);

        // Appearances Tab
        Tab appearancesTab = new Tab("Appearances");
        appearancesTab.setContent(new Label("  Appearances, Scenes,\n  and Decals"));

        taskPane.getTabs().addAll(resourcesTab, libraryTab, appearancesTab);

        return taskPane;
    }

    // Helper for Async Canvas loading
    private void initializeCanvasAsync(StackPane parent) {
        Task<Void> canvasLoadTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (canvasNode == null) {
                    Thread.sleep(50);
                }
                return null;
            }
        };

        canvasLoadTask.setOnSucceeded(e -> {
            parent.getChildren().add(canvasNode);
            canvasNode.setFocusTraversable(true);
            canvasNode.setOnKeyPressed(keyEvent -> {
                // Forward keys to existing listener logic if possible,
                // or just rely on GLCanvas focus which we requested in initializeGLCanvas
                // But we need to consume arrow keys here to prevent focus traversal out of
                // SwingNode
                switch (keyEvent.getCode()) {
                    case UP:
                    case DOWN:
                    case LEFT:
                    case RIGHT:
                    case SPACE:
                    case Q:
                    case E:
                    case R:
                        keyEvent.consume();
                        break;
                }
            });

            Platform.runLater(() -> {
                canvasNode.requestFocus();
                appendOutput("Ready.");
            });
        });

        Thread canvasThread = new Thread(canvasLoadTask);
        canvasThread.setDaemon(true);
        canvasThread.start();
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

        sketchKiteCenterX = new TextField();
        sketchKiteCenterY = new TextField();
        sketchKiteDiagV = new TextField();
        sketchKiteDiagH = new TextField();
        sketchKiteAngle = new TextField();
        // Initialize ComboBox for unit selection
        // Initialize ComboBox for unit selection with UnitSystem enum
        unitSelector = new ComboBox<>();
        for (cad.core.UnitSystem us : cad.core.UnitSystem.values()) {
            unitSelector.getItems().add(us.name());
        }
        unitSelector.setValue(cad.core.UnitSystem.MMGS.name()); // Default
        unitSelector.setOnAction(e -> {
            cad.core.UnitSystem selected = cad.core.UnitSystem.valueOf(unitSelector.getValue());
            sketch.setUnitSystem(selected);
            appendOutput("Units set to: " + selected.getDescription());
        });

        // Initialize TextArea for console output
        outputArea = new TextArea();
        outputArea.setEditable(false); // Make it read-only
        outputArea.setStyle("-fx-font-family: 'Courier New', monospace;"); // Apply font styling

        // Initialize OpenGL canvas, which must be done on the Swing event dispatch
        // thread
        initializeGLCanvas();
    }

    /**
     * Initializes the JOGL GLCanvas component. This involves setting up OpenGL
     * capabilities,
     * adding event listeners for mouse and keyboard interaction, and wrapping it in
     * a SwingNode
     * for integration into the JavaFX scene.
     * This method uses SwingUtilities.invokeLater to ensure GLCanvas creation on
     * the correct thread.
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
            glRenderer = new OpenGLRenderer(interactionManager); // Pass Interaction Manager
            glCanvas.addGLEventListener(glRenderer); // Add the renderer to the canvas
            glCanvas.addMouseListener(new CanvasMouseListener());
            glCanvas.addMouseMotionListener(new CanvasMouseMotionListener());
            glCanvas.addMouseWheelListener(new CanvasMouseWheelListener());
            glCanvas.addKeyListener(new CanvasKeyListener());
            glCanvas.setFocusable(true); // Allow GLCanvas to receive keyboard focus
            glCanvas.requestFocusInWindow(); // Request initial focus

            // Initialize the FPSAnimator to continuously render the GLCanvas at 60 FPS
            animator = new FPSAnimator(glCanvas, 60);

            // Create a JPanel to hold the GLCanvas, as SwingNode can only contain
            // JComponent
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
        TabPane leftPanel = createControlPanel();

        // Create the right panel (3D canvas)
        VBox rightPanel = new VBox();
        rightPanel.setPadding(new Insets(5.0)); // Reduced padding for more space
        rightPanel.setAlignment(Pos.CENTER);

        // Use a Task to wait for the canvasNode to be initialized from the Swing
        // thread,
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
    /**
     * Creates and configures the control panel (left side of the main split pane).
     * This panel contains TitledPanes for commands and parameters.
     *
     * @return A VBox representing the control panel.
     */
    private TabPane createControlPanel() {
        TabPane controlTabs = new TabPane();
        controlTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        controlTabs.setMinWidth(280);

        // Tab 1: Feature Manager Tree
        Tab treeTab = new Tab("Feature Tree");
        treeTab.setContent(createFeatureManagerTree());

        // Tab 2: Property Manager
        Tab propertyTab = new Tab("Property Manager");
        propertyTab.setContent(createParametersPane());

        // Tab 3: Configuration Manager
        Tab configTab = new Tab("Configuration");
        configTab.setContent(createConfigurationManager());

        controlTabs.getTabs().addAll(treeTab, propertyTab, configTab);
        return controlTabs;
    }

    private VBox createConfigurationManager() {
        VBox container = new VBox();
        container.setSpacing(5);
        container.setPadding(new Insets(5));

        // Toolbar for Config operations
        ToolBar configTools = new ToolBar();
        Button addConfigBtn = new Button("+");
        addConfigBtn.setTooltip(new Tooltip("Add Configuration"));
        Button delConfigBtn = new Button("-");
        delConfigBtn.setTooltip(new Tooltip("Delete Configuration"));
        configTools.getItems().addAll(addConfigBtn, delConfigBtn);

        // Tree View for Configurations
        TreeItem<String> rootConfig = new TreeItem<>("Part1 Configurations");
        rootConfig.setExpanded(true);

        TreeItem<String> defaultConfig = new TreeItem<>("Default [Active]");
        TreeItem<String> description = new TreeItem<>("Description: Default configuration");
        defaultConfig.getChildren().add(description);
        defaultConfig.setExpanded(true);

        rootConfig.getChildren().add(defaultConfig);

        TreeView<String> configTree = new TreeView<>(rootConfig);
        VBox.setVgrow(configTree, Priority.ALWAYS);

        // Configuration Details Pane (Bottom)
        GridPane detailsPane = new GridPane();
        detailsPane.setHgap(5);
        detailsPane.setVgap(5);
        detailsPane.setPadding(new Insets(5));

        detailsPane.add(new Label("Config Name:"), 0, 0);
        detailsPane.add(new TextField("Default"), 1, 0);
        detailsPane.add(new Label("Description:"), 0, 1);
        detailsPane.add(new TextField("Standard Config"), 1, 1);

        TitledPane detailsTitlePane = new TitledPane("Configuration Properties", detailsPane);
        detailsTitlePane.setCollapsible(true);
        detailsTitlePane.setExpanded(true);

        container.getChildren().addAll(configTools, configTree, detailsTitlePane);
        return container;
    }

    /**
     * Creates and populates the commands pane, which contains various buttons
     * categorized into General, File Operations, 3D Model, and 2D Sketching
     * commands.
     *
     * @return A ScrollPane containing the VBox of command buttons.
     */
    // Old createCommandsPane removed (replaced by Ribbon)

    /**
     * Creates and populates the parameters pane, which contains input fields
     * for 3D object parameters, file operations, and 2D sketching parameters.
     *
     * @return A ScrollPane containing the GridPane of parameter input fields.
     */
    private ScrollPane createParametersPane() {
        GridPane parametersGrid = new GridPane();
        parametersGrid.setHgap(8.0); // Reduced horizontal gap
        parametersGrid.setVgap(4.0); // Reduced vertical gap for compactness
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

        // Kite Parameters
        parametersGrid.add(createSectionLabel("Kite Parameters"), 0, row++, 2, 1);
        parametersGrid.add(new Label("Center X:"), 0, row);
        parametersGrid.add(sketchKiteCenterX, 1, row++);
        parametersGrid.add(new Label("Center Y:"), 0, row);
        parametersGrid.add(sketchKiteCenterY, 1, row++);
        parametersGrid.add(new Label("Vertical Diagonal:"), 0, row);
        parametersGrid.add(sketchKiteDiagV, 1, row++);
        parametersGrid.add(new Label("Horizontal Diagonal:"), 0, row);
        parametersGrid.add(sketchKiteDiagH, 1, row++);
        parametersGrid.add(new Label("Angle (deg):"), 0, row);
        parametersGrid.add(sketchKiteAngle, 1, row++);

        // Units Selector
        parametersGrid.add(new Label("Units:"), 0, row);
        parametersGrid.add(unitSelector, 1, row++);

        ScrollPane scrollPane = new ScrollPane(parametersGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400); // Set a preferred height

        return scrollPane;
    }

    // Handler for Sketch Kite button (must be outside of createParametersPane)
    private void sketchKite() {
        try {
            double cx = Double.parseDouble(sketchKiteCenterX.getText());
            double cy = Double.parseDouble(sketchKiteCenterY.getText());
            double diagV = Double.parseDouble(sketchKiteDiagV.getText());
            double diagH = Double.parseDouble(sketchKiteDiagH.getText());
            double angle = Double.parseDouble(sketchKiteAngle.getText());
            sketch.addKite((float) cx, (float) cy, (float) diagV, (float) diagH, (float) angle);
            appendOutput(String.format("Kite sketched at (%.2f, %.2f) with diagonals %.2f, %.2f and angle %.2f", cx, cy,
                    diagV, diagH, angle));
            glCanvas.repaint();
        } catch (NumberFormatException ex) {
            appendOutput("Invalid input for kite parameters.");
        }
    }

    /**
     * Creates and configures the console output pane at the bottom of the window.
     *
     * @return A TitledPane containing the TextArea for console output.
     */
    private TitledPane createConsolePane() { // Create Console Output (Bottom)
        TitledPane consolePane = new TitledPane("Console Output", outputArea);
        consolePane.setCollapsible(true);
        consolePane.setExpanded(true);
        consolePane.setMaxHeight(150); // Reduced height for console
        consolePane.setPrefHeight(100);

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
     * @param text    The text to display on the button.
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
        private boolean showSketch = false; // Flag to switch between rendering 3D models (STL/default cube) and 2D
        private SketchInteractionManager interactionManager;

        public OpenGLRenderer(SketchInteractionManager interactionManager) {
            this.interactionManager = interactionManager;
        }

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
            gl.glEnable(GL2.GL_LIGHTING); // Enable lighting
            gl.glEnable(GL2.GL_LIGHT0); // Enable light source 0
            gl.glEnable(GL2.GL_COLOR_MATERIAL); // Enable material color tracking GL_FRONT_AND_BACK

            // Set up lighting properties (position and diffuse color)
            float[] lightPos = { 1.0f, 1.0f, 1.0f, 0.0f }; // Directional light from top-right-front
            float[] lightColor = { 1.0f, 1.0f, 1.0f, 1.0f }; // White light
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
         * Updates the projection matrix with adaptive clipping planes based on current
         * zoom and model size.
         * This prevents clipping artifacts when rotating large models or when zoomed
         * far out.
         *
         * @param drawable The GLAutoDrawable object to get dimensions from.
         */
        private void updateProjectionMatrix(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            int width = drawable.getSurfaceWidth();
            int height = drawable.getSurfaceHeight();

            if (height == 0)
                height = 1; // Prevent division by zero
            float aspect = (float) width / height;

            // Calculate adaptive clipping planes based on model size and zoom
            float modelSize = Geometry.getModelMaxDimension();
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.005f); // More conservative near plane
            float farPlane = Math.max(1000.0f, Math.abs(zoom) + modelSize * 5.0f); // Increased far plane for large
                                                                                   // models

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
         * @param x        The x-coordinate of the viewport.
         * @param y        The y-coordinate of the viewport.
         * @param width    The width of the viewport.
         * @param height   The height of the viewport.
         */
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();

            if (height == 0)
                height = 1; // Prevent division by zero
            float aspect = (float) width / height;

            gl.glViewport(0, 0, width, height); // Set the viewport to the entire drawable area
            gl.glMatrixMode(GL2.GL_PROJECTION); // Switch to projection matrix mode
            gl.glLoadIdentity(); // Reset the projection matrix

            // Calculate adaptive clipping planes based on model size and zoom
            float modelSize = Geometry.getModelMaxDimension();
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.005f); // More conservative near plane
            float farPlane = Math.max(1000.0f, Math.abs(zoom) + modelSize * 5.0f); // Increased far plane for large
                                                                                   // models

            glu.gluPerspective(45.0, aspect, nearPlane, farPlane); // Set up perspective projection with adaptive
                                                                   // clipping
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
            // ... (keeping existing cube logic if needed, or removing it to be cleaner)
            gl.glColor3f(0.7f, 0.7f, 0.7f); // Set color to grey
            gl.glBegin(GL2.GL_QUADS); // Begin drawing quadrilaterals
            // ... existing vertices ...
            // Simplified for brevity in this replace, effectively just ensuring it exists
            // but user wants planes.
            // Let's actually implement renderPlanes here
        }

        private void renderPlanes(GL2 gl) {
            gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS); // Save all attributes
            try {
                gl.glDisable(GL2.GL_LIGHTING); // Planes are often better seen unlit
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

                float size = 10.0f; // Plane size

                // Front Plane (XY) - Transparent Blue
                gl.glColor4f(0.0f, 0.0f, 0.8f, 0.2f); // Blue tint
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-size, -size, 0);
                gl.glVertex3f(size, -size, 0);
                gl.glVertex3f(size, size, 0);
                gl.glVertex3f(-size, size, 0);
                gl.glEnd();
                // Border
                gl.glColor3f(0.0f, 0.0f, 1.0f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex3f(-size, -size, 0);
                gl.glVertex3f(size, -size, 0);
                gl.glVertex3f(size, size, 0);
                gl.glVertex3f(-size, size, 0);
                gl.glEnd();

                // Top Plane (XZ) - Transparent Green
                gl.glColor4f(0.0f, 0.8f, 0.0f, 0.2f); // Green tint
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-size, 0, -size);
                gl.glVertex3f(size, 0, -size);
                gl.glVertex3f(size, 0, size);
                gl.glVertex3f(-size, 0, size);
                gl.glEnd();
                // Border
                gl.glColor3f(0.0f, 1.0f, 0.0f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex3f(-size, 0, -size);
                gl.glVertex3f(size, 0, -size);
                gl.glVertex3f(size, 0, size);
                gl.glVertex3f(-size, 0, size);
                gl.glEnd();

                // Right Plane (YZ) - Transparent Red
                gl.glColor4f(0.8f, 0.0f, 0.0f, 0.2f); // Red tint
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(0, -size, -size);
                gl.glVertex3f(0, size, -size);
                gl.glVertex3f(0, size, size);
                gl.glVertex3f(0, -size, size);
                gl.glEnd();
                // Border
                gl.glColor3f(1.0f, 0.0f, 0.0f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex3f(0, -size, -size);
                gl.glVertex3f(0, size, -size);
                gl.glVertex3f(0, size, size);
                gl.glVertex3f(0, -size, size);
                gl.glEnd();

            } finally {
                gl.glPopAttrib(); // Restore all attributes
            }
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

            // Draw interactive ghost shape if drawing
            if (interactionManager != null && interactionManager.isDrawing()) {
                renderGhost(gl);
            }

            // Restore matrices and lighting
            gl.glPopMatrix(); // Restore modelview matrix
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPopMatrix(); // Restore projection matrix
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glEnable(GL2.GL_LIGHTING); // Re-enable lighting
        }

        private void renderGhost(GL2 gl) {
            gl.glColor3f(0.5f, 0.5f, 0.5f); // Grey color for ghost
            gl.glLineWidth(1.0f); // Thinner line

            float x1 = interactionManager.getStartX();
            float y1 = interactionManager.getStartY();
            float x2 = interactionManager.getCurrentX();
            float y2 = interactionManager.getCurrentY();

            if (interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_LINE) {
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex2f(x1, y1);
                gl.glVertex2f(x2, y2);
                gl.glEnd();
            } else if (interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_CIRCLE) {
                float radius = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                gl.glBegin(GL2.GL_LINE_LOOP);
                int segments = 50;
                for (int i = 0; i < segments; i++) {
                    double angle = 2.0 * Math.PI * i / segments;
                    gl.glVertex2d(x1 + radius * Math.cos(angle), y1 + radius * Math.sin(angle));
                }
                gl.glEnd();
                // Draw radius line
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex2f(x1, y1);
                gl.glVertex2f(x2, y2);
                gl.glEnd();
            } else if (interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_POLYGON) {
                float radius = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                gl.glBegin(GL2.GL_LINE_LOOP);
                int sides = 6;
                for (int i = 0; i < sides; i++) {
                    double angle = 2.0 * Math.PI * i / sides;
                    gl.glVertex2d(x1 + radius * Math.cos(angle), y1 + radius * Math.sin(angle));
                }
                gl.glEnd();
            }
        }

        /**
         * Sets the list of triangles to be rendered as an STL model.
         * Automatically switches the view to hide the sketch and shows the STL model.
         *
         * @param triangles The list of float arrays, where each array represents an STL
         *                  triangle (normal + 3 vertices).
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
     * MouseListener implementation for the GLCanvas to handle mouse click events
     * and focus management.
     * 
     * This class serves as the primary interface for mouse-based interaction with
     * the 3D canvas,
     * managing the critical focus system that enables keyboard controls to work
     * properly.
     * Due to the JavaFX-Swing integration complexity, this class implements a dual
     * focus
     * strategy to ensure arrow keys are captured by the canvas instead of UI
     * components.
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
            // Check if we are in 2D mode but no tool is selected
            if (glRenderer != null && glRenderer.isShowingSketch() &&
                    interactionManager != null &&
                    (interactionManager.getMode() == SketchInteractionManager.InteractionMode.IDLE ||
                            interactionManager.getMode() == SketchInteractionManager.InteractionMode.VIEW_ROTATE)) {
                appendOutput("Select a tool from the ribbon to draw.");
            }

            // Check if we are in an interactive sketching mode
            if (interactionManager != null &&
                    interactionManager.getMode() != SketchInteractionManager.InteractionMode.IDLE &&
                    interactionManager.getMode() != SketchInteractionManager.InteractionMode.VIEW_ROTATE) {

                // Calculate world coordinates
                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                // Use Press to START drawing
                interactionManager.handleMousePress(worldCoords[0], worldCoords[1]);
                glCanvas.repaint();
                // Do NOT return here, we still want to capture focus, but maybe NOT Set
                // isDragging for VIEW rotation
                // But we DO want 'isDragging' to be true so mouseDragged event fires to update
                // preview
                // isDragging variable in GuiFX seems to control VIEW rotation/pan.
                // We should separate View Dragging from Sketch Dragging.
                isDragging = false; // Disable view dragging when sketching
                return;
            }

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
            isDragging = false; // Clear view dragging flag

            // Handle Sketch Release
            if (interactionManager != null &&
                    interactionManager.getMode() != SketchInteractionManager.InteractionMode.IDLE &&
                    interactionManager.getMode() != SketchInteractionManager.InteractionMode.VIEW_ROTATE) {

                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                interactionManager.handleMouseRelease(worldCoords[0], worldCoords[1]);
                glCanvas.repaint();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        } // Not used

        @Override
        public void mouseExited(MouseEvent e) {
        } // Not used
    }

    /**
     * MouseMotionListener implementation for the GLCanvas to handle mouse dragging.
     * Supports both 3D rotation (for models) and 2D panning (for sketches).
     */
    private class CanvasMouseMotionListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (interactionManager != null && interactionManager.isDrawing()) {
                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                interactionManager.handleMouseMove(worldCoords[0], worldCoords[1]);
                glCanvas.repaint();
                return; // Don't view-pan/rotate if we are drawing
            }

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
        public void mouseMoved(MouseEvent e) {
            if (interactionManager != null && glRenderer != null && glRenderer.isShowingSketch()) {
                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                interactionManager.handleMouseMove(worldCoords[0], worldCoords[1]);
                glCanvas.repaint();
            }
        }
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
                if (sketch2DZoom < 0.1f)
                    sketch2DZoom = 0.1f; // Max zoom out
                if (sketch2DZoom > 20.0f)
                    sketch2DZoom = 20.0f; // Max zoom in
            } else {
                // 3D model mode: adjust 3D zoom
                zoom += e.getWheelRotation() * 0.5f; // Adjust zoom based on wheel rotation
                // Clamp zoom values to a reasonable range for large models
                if (zoom > -1.0f)
                    zoom = -1.0f;
                if (zoom < -800.0f)
                    zoom = -800.0f; // Increased limit for large models
            }
            glCanvas.repaint(); // Request a repaint to show new zoom level
        }
    }

    // === View Control Methods ===

    public void setViewFront() {
        // Front View: Look at XY plane from Z
        rotationX = 0;
        rotationY = 0;
        glCanvas.repaint();
    }

    public void setViewTop() {
        // Top View: Look at XZ plane from Y
        rotationX = 90;
        rotationY = 0;
        glCanvas.repaint();
    }

    public void setViewRight() {
        // Right View: Look at YZ plane from X
        rotationX = 0;
        rotationY = -90;
        glCanvas.repaint();
    }

    public void setViewIsometric() {
        // Isometric View
        rotationX = 35.264f;
        rotationY = -45.0f;
        glCanvas.repaint();
    }

    /**
     * KeyListener implementation for comprehensive keyboard-based 3D navigation and
     * control.
     * 
     * This class provides the primary keyboard interface for 3D model manipulation,
     * offering intuitive controls for rotation, zoom, view management, and mode
     * switching.
     * It implements a performance-optimized approach by only triggering repaints
     * when
     * the view actually changes, reducing unnecessary GPU overhead.
     * 
     * Control Categories:
     * 
     * 1. ROTATION CONTROLS (Arrow Keys):
     * - Up/Down: Rotate around X-axis (pitch) in 3° increments
     * - Left/Right: Rotate around Y-axis (yaw) in 3° increments
     * - Smooth, predictable rotation for precise model positioning
     * 
     * 2. ZOOM CONTROLS (Multiple key options):
     * - Q/+ : Zoom in (move closer to model)
     * - E/- : Zoom out (move away from model)
     * - Clamped between -1.0f (very close) and -50.0f (very far)
     * 
     * 3. VIEW MANAGEMENT:
     * - R: Reset view to default position with auto-scaling zoom
     * - ESC: Restore canvas focus if keyboard controls stop working
     * 
     * 4. MODE SWITCHING:
     * - SPACE: Toggle between 3D model view and 2D sketch view
     * - Automatic mode feedback in console
     * 
     * 5. HELP SYSTEM:
     * - Ctrl+H: Display detailed keyboard controls help
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
                        if (sketch2DZoom > 20.0f)
                            sketch2DZoom = 20.0f; // Clamp max zoom
                    } else {
                        // 3D mode: zoom in
                        zoom += 0.5f;
                        if (zoom > -1.0f)
                            zoom = -1.0f; // Clamp zoom
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_E:
                case KeyEvent.VK_MINUS:
                    if (is2DSketchMode) {
                        // 2D mode: zoom out
                        sketch2DZoom /= 1.2f; // 20% zoom step
                        if (sketch2DZoom < 0.1f)
                            sketch2DZoom = 0.1f; // Clamp min zoom
                    } else {
                        // 3D mode: zoom out
                        zoom -= 0.5f;
                        if (zoom < -800.0f)
                            zoom = -800.0f; // Increased clamp for large models
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
                    toggleSketchView();
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
        public void keyReleased(KeyEvent e) {
        } // Not used

        @Override
        public void keyTyped(KeyEvent e) {
        } // Not used
    }

    // === Helper methods ===

    /**
     * Toggles between 2D sketch view and 3D model view.
     */
    public void toggleSketchView() {
        if (glRenderer != null) {
            boolean currentSketchMode = glRenderer.isShowingSketch();
            glRenderer.setShowSketch(!currentSketchMode);
            if (!currentSketchMode) {
                appendOutput("Switched to 2D sketch view - Use arrow keys to pan, Q/E to zoom");
                // Hint user to select a tool if they haven't already
                if (interactionManager != null &&
                        (interactionManager.getMode() == SketchInteractionManager.InteractionMode.IDLE ||
                                interactionManager.getMode() == SketchInteractionManager.InteractionMode.VIEW_ROTATE)) {
                    appendOutput("Sketch Mode Active. Select a tool (Line, Circle, etc.) from the ribbon to draw.");
                }
            } else {
                appendOutput("Switched to 3D model view - Use arrow keys to rotate, Q/E to zoom");
            }
            if (glCanvas != null) {
                glCanvas.repaint();
            }
        }
    }

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
                zoom = -(maxDimension * 0.8f + (float) Math.log10(maxDimension) * 15.0f);
            }

            // Clamp zoom to reasonable bounds
            if (zoom < -800.0f)
                zoom = -800.0f; // Further increased upper limit for very large models
            if (zoom > -2.0f)
                zoom = -2.0f;

            appendOutput("View reset - zoom auto-adjusted to " + String.format("%.1f", zoom) + " for model size "
                    + String.format("%.1f", maxDimension));
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

    /**
     * Converts screen coordinates to sketch world coordinates.
     * Assumes standard 2D orthographic projection matches current pan/zoom.
     */
    private float[] getSketchWorldCoordinates(int screenX, int screenY) {
        if (glCanvas == null)
            return new float[] { 0, 0 };

        float width = glCanvas.getWidth();
        float height = glCanvas.getHeight();

        // Match the logic in renderSketch:
        // double baseSize = 50.0;
        // double size = baseSize / sketch2DZoom;
        // WorldUnitsPerPixel = (2 * size * aspect) / width = 2 * (50/zoom * w/h) / w =
        // 100 / (zoom * height)
        // Correct logic derived:
        // Ortho Height = 2 * size = 100 / zoom
        // Pixel Height = height
        // Scale = (100 / zoom) / height

        float scale = 100.0f / (sketch2DZoom * height);

        // Center-relative screen coords
        float relX = screenX - width / 2.0f;
        float relY = height / 2.0f - screenY; // Inverted Y for OpenGL

        float worldX = (relX * scale) - sketch2DPanX;
        float worldY = (relY * scale) - sketch2DPanY;

        return new float[] { worldX, worldY };
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
     * Creates a 3D cube with size from input field and current subdivision
     * settings.
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
     * Creates a 3D sphere with radius from input field and current subdivision
     * settings.
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
     * Opens a file chooser dialog to allow the user to select an STL or DXF file to
     * load.
     * Determines file type based on extension and calls appropriate loading methods
     * (`Geometry.loadStl()` or `sketch.loadDXF()`). Updates the GLCanvas display
     * accordingly.
     */
    /**
     * Initiates the file loading process with user-selected file and format
     * detection.
     * 
     * This method provides a comprehensive file loading interface that
     * automatically
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
                new FileChooser.ExtensionFilter("All files", "*.*"));

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
     * This method provides a specialized export interface specifically for DXF
     * format,
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

    private void showLoadDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open CAD File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CAD Files", "*.stl", "*.dxf"),
                new FileChooser.ExtensionFilter("STL Files", "*.stl"),
                new FileChooser.ExtensionFilter("DXF Files", "*.dxf"));
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String path = selectedFile.getAbsolutePath();
            String lowerPath = path.toLowerCase();
            try {
                if (lowerPath.endsWith(".stl")) {
                    List<float[]> triangles = Geometry.loadStl(path);
                    if (glRenderer != null)
                        glRenderer.setStlTriangles(triangles);
                    appendOutput("Loaded STL: " + path);
                } else if (lowerPath.endsWith(".dxf")) {
                    sketch.loadDXF(path);
                    if (glRenderer != null)
                        glRenderer.setShowSketch(true);
                    appendOutput("Loaded DXF: " + path);
                }
            } catch (Exception e) {
                appendOutput("Error loading file: " + e.getMessage());
            }
        }
    }

    /**
     * Shows a dialog to revolve the current sketch. based on X and Y coordinates
     * entered in `sketchPointX` and `sketchPointY` text fields.
     * This method now passes String arguments to the Sketch object.
     */
    /**
     * Creates a new 2D point entity in the sketch from user input coordinates.
     * 
     * This method handles the creation of point entities, which serve as
     * fundamental
     * building blocks for more complex geometry. Points can be used as reference
     * locations, endpoints for lines, centers for circles, or vertices for
     * polygons.
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
     * Reads x1,y1,x2,y2 values, validates them, adds line to sketch, and updates
     * display.
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
     * It parses thttps://www.linkedin.com/in/gautam-mahajan-68848427a/he center
     * coordinates, radius, and number of sides,
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
     * Opens a custom dialog window to get the extrusion height from the user.
     */
    private void extrudeSketch() {
        // Check if sketch has any geometry to extrude
        if (sketch.listSketch().isEmpty()) {
            appendOutput("Error: Sketch is empty. Add some geometry first.");
            appendOutput("Tip: Use sketch commands to create points, lines, circles, or polygons.");
            return;
        }

        // Create a custom dialog window
        Stage extrudeDialog = new Stage();
        extrudeDialog.setTitle("Extrude Sketch");
        extrudeDialog.initModality(Modality.APPLICATION_MODAL);
        extrudeDialog.setResizable(false);

        // Create layout
        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        // Add title label
        Label titleLabel = new Label("Extrude 2D Sketch into 3D");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        // Add instruction label
        Label instructionLabel = new Label("Enter the height for extrusion:");

        // Create text field for height input
        TextField heightField = new TextField("10.0");
        heightField.setPrefWidth(150);
        heightField.setPromptText("Height value");

        // Create buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button extrudeButton = new Button("Extrude");
        Button cancelButton = new Button("Cancel");

        extrudeButton.setPrefWidth(80);
        cancelButton.setPrefWidth(80);

        buttonBox.getChildren().addAll(extrudeButton, cancelButton);

        // Add all components to layout
        layout.getChildren().addAll(titleLabel, instructionLabel, heightField, buttonBox);

        // Set up button actions
        extrudeButton.setOnAction(e -> {
            try {
                double height = Double.parseDouble(heightField.getText());
                if (height <= 0) {
                    appendOutput("Error: Extrusion height must be greater than 0.");
                    return;
                }

                // Perform the extrusion using the sketch's built-in method
                sketch.extrude(height);

                // Get the extruded faces and convert them to triangles for rendering
                List<float[]> extrudedTriangles = sketch.getExtrudedTriangles();

                if (extrudedTriangles != null && !extrudedTriangles.isEmpty()) {
                    // Set the extruded geometry for rendering
                    glRenderer.setStlTriangles(extrudedTriangles);

                    appendOutput("Successfully extruded sketch with height " + height);
                    appendOutput("Generated " + extrudedTriangles.size() + " triangles from extrusion.");
                    appendOutput("Switching to 3D view to show extruded geometry.");

                    // Switch to 3D view to show the result
                    glRenderer.setShowSketch(false);
                    resetView(); // Reset view to fit the new geometry
                    glCanvas.repaint();
                    glCanvas.requestFocusInWindow();
                } else {
                    appendOutput("Warning: No extrudable geometry found in sketch.");
                    appendOutput("Tip: Create polygons or circles to extrude into 3D shapes.");
                }

                extrudeDialog.close();

            } catch (NumberFormatException ex) {
                appendOutput("Error: Invalid height value. Please enter a valid number.");
            } catch (Exception ex) {
                appendOutput("Error during extrusion: " + ex.getMessage());
            }
        });

        cancelButton.setOnAction(e -> extrudeDialog.close());

        // Allow Enter key to trigger extrude
        heightField.setOnAction(e -> extrudeButton.fire());

        // Create scene and show dialog
        Scene scene = new Scene(layout, 300, 200);
        extrudeDialog.setScene(scene);

        // Center the dialog on the main window
        extrudeDialog.initOwner(outputArea.getScene().getWindow());

        // Focus on the text field when dialog opens
        Platform.runLater(() -> heightField.requestFocus());

        extrudeDialog.showAndWait();
    }

    // === New Dialog methods for Features ===

    private void showNacaDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("NACA Airfoil Generator");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        TextField codeField = new TextField("2412");
        TextField chordField = new TextField("1.0");

        layout.getChildren().addAll(
                new Label("NACA 4-Digit Code:"), codeField,
                new Label("Chord Length:"), chordField,
                new Button("Generate") {
                    {
                        setOnAction(e -> {
                            String code = codeField.getText();
                            try {
                                float chord = Float.parseFloat(chordField.getText());
                                int res = sketch.generateNaca4(code, chord, 50);
                                if (res == 0) {
                                    appendOutput("Generated NACA " + code);
                                    glRenderer.setShowSketch(true);
                                    glCanvas.repaint();
                                    dialog.close();
                                } else {
                                    appendOutput("Error generating Airfoil.");
                                }
                            } catch (NumberFormatException nfe) {
                                appendOutput("Invalid number format.");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 250, 200));
        dialog.show();
    }

    private void showRevolveDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Revolve Feature");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        TextField angleField = new TextField("360");

        layout.getChildren().addAll(
                new Label("Angle of Revolution (deg):"), angleField,
                new Button("Revolve") {
                    {
                        setOnAction(e -> {
                            try {
                                float angle = Float.parseFloat(angleField.getText());
                                Geometry.revolve(sketch, angle, 60);
                                appendOutput("Revolved sketch " + angle + " degrees");

                                // Revolve stores data in Geometry.extrudedTriangles
                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());

                                glRenderer.setShowSketch(false);
                                resetView();
                                glCanvas.repaint();
                                dialog.close();
                            } catch (NumberFormatException nfe) {
                                appendOutput("Invalid angle.");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 250, 150));
        dialog.show();
    }

    private void showLoftDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Loft Feature");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        TextField heightField = new TextField("30.0");

        layout.getChildren().addAll(
                new Label("Loft Height:"), heightField,
                new Label("(Lofts between the first two polygons in sketch)"),
                new Button("Loft") {
                    {
                        setOnAction(e -> {
                            try {
                                float height = Float.parseFloat(heightField.getText());
                                Geometry.loft(sketch, height);
                                appendOutput("Loft created with height " + height);

                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());

                                glRenderer.setShowSketch(false);
                                resetView();
                                glCanvas.repaint();
                                dialog.close();
                            } catch (NumberFormatException nfe) {
                                appendOutput("Invalid height.");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 300, 200));
        dialog.show();
    }

    private void calculateMassProperties() {
        float[] cent = Geometry.calculateCentroid();
        appendOutput("Center of Gravity: X=" + cent[0] + ", Y=" + cent[1] + ", Z=" + cent[2]);
        // TODO: Add mass calculation
    }

    /**
     * Main method to launch the JavaFX application.
     *
     * @param args Command line arguments passed to the application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
