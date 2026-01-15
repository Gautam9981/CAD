package cad.gui;

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
import javafx.scene.control.Spinner;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import javafx.scene.control.SplitPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.Optional;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;

import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

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

import java.io.File;
import java.util.List;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.TextRenderer;

import cad.core.Geometry;

import cad.core.Sketch;
import cad.core.UnitSystem;
import cad.core.CommandManager;
import cad.core.AddConstraintCommand;

import cad.core.Constraint;
import cad.core.CoincidentConstraint;
import cad.core.HorizontalConstraint;
import cad.core.VerticalConstraint;
import cad.core.FixedConstraint;
import cad.core.MassProperties;
import cad.core.Sketch.Line;
import cad.core.Sketch.PointEntity;
import cad.core.Sketch.Entity;

import cad.core.Sketch.Entity;
import cad.core.ViewChangeCommand;
import cad.aerodynamics.NacaDialog;
import cad.aerodynamics.CfdDialog;
import cad.analysis.FlowVisualizer;

public class GuiFX extends Application {

    private TextArea outputArea;
    private TextArea helpText;
    private ScrollPane helpScrollPane;
    private JOGLCadCanvas glCanvas;
    private SwingNode canvasNode;
    private FPSAnimator animator;
    private OpenGLRenderer glRenderer;
    private SketchInteractionManager interactionManager;
    private MacroManager macroManager;

    public static int sphereLatDiv = 10;
    public static int sphereLonDiv = 10;
    public static int cubeDivisions = 10;
    public static Sketch sketch;
    private CommandManager commandManager;
    private UnitSystem currentUnits = UnitSystem.MMGS;

    private float rotationX = 35.264f;
    private float rotationY = -45.0f;
    private float zoom = -30.0f;
    private int lastMouseX, lastMouseY;
    private boolean isDragging = false;
    private List<float[]> stlTriangles;

    private float sketch2DPanX = 0.0f;
    private float sketch2DPanY = 0.0f;
    private float sketch2DZoom = 1.0f;

    @Override
    public void start(Stage primaryStage) {
        sketch = new Sketch();
        commandManager = new CommandManager();

        showUnitSelectionWindow(primaryStage);
    }

    private void showUnitSelectionWindow(Stage primaryStage) {
        Stage splashStage = new Stage();
        splashStage.setTitle("Select Units");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);

        Label lblPrompt = new Label("Select Unit System for this session:");
        lblPrompt.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        ComboBox<UnitSystem> unitCombo = new ComboBox<>();
        unitCombo.getItems().addAll(UnitSystem.values());
        unitCombo.setValue(UnitSystem.MMGS);
        unitCombo.setMaxWidth(Double.MAX_VALUE);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(e -> {
            splashStage.close();
            Platform.exit();
            System.exit(0);
        });

        Button btnOK = new Button("Start SketchApp");
        btnOK.setDefaultButton(true);
        btnOK.setOnAction(e -> {
            currentUnits = unitCombo.getValue();
            sketch.setUnitSystem(currentUnits);
            System.out.println("Selected Unit System: " + currentUnits.toString());

            splashStage.close();
            initializeMainUI(primaryStage);
        });

        buttonBox.getChildren().addAll(btnCancel, btnOK);
        root.getChildren().addAll(lblPrompt, unitCombo, buttonBox);

        Scene scene = new Scene(root, 300, 150);
        splashStage.setScene(scene);
        splashStage.setResizable(false);
        splashStage.centerOnScreen();
        splashStage.show();
    }

    private void initializeMainUI(Stage primaryStage) {

        commandManager = new CommandManager();
        interactionManager = new SketchInteractionManager(sketch, commandManager);
        macroManager = new MacroManager(sketch, primaryStage);

        macroManager.setCanvasRefresh(() -> {
            if (glCanvas != null) {
                javax.swing.SwingUtilities.invokeLater(() -> glCanvas.repaint());
            }
        });

        macroManager.setOutputCallback(this::appendOutput);

        macroManager.setViewModeCallback(() -> {
            if (glRenderer != null) {
                glRenderer.setShowSketch(true);
                appendOutput("Switched to sketch view for macro execution");
            }
        });

        macroManager.setFitViewCallback(this::fitSketchView);

        macroManager.setStlUpdateCallback(triangles -> {
            if (glRenderer != null) {
                glRenderer.setStlTriangles(triangles);
                glRenderer.setShowSketch(false);
                appendOutput("Macro: Extrusion/Revolve complete - Switched to 3D view");

                resetView();
            }
        });

        primaryStage.setTitle("SketchApp (4.5)");

        primaryStage.setOnCloseRequest(event -> {
            if (sketch.isModified()) {
                event.consume();
                showUnsavedChangesDialog(primaryStage);
            }
        });

        initializeComponents();

        BorderPane root = new BorderPane();

        TabPane ribbon = createRibbon();
        root.setTop(ribbon);

        StackPane viewportStack = new StackPane();
        viewportStack.getStyleClass().add("viewport-stack");

        ToolBar headsUpToolbar = new ToolBar();
        headsUpToolbar.getStyleClass().add("heads-up-toolbar");
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

        initializeCanvasAsync(viewportStack);

        root.setCenter(viewportStack);

        TabPane featureManager = createControlPanel();

        TabPane taskPane = createTaskPane();

        SplitPane horizontalSplit = new SplitPane();
        horizontalSplit.getItems().addAll(featureManager, root.getCenter(), taskPane);
        horizontalSplit.setDividerPositions(0.2, 0.85);

        root.setCenter(horizontalSplit);

        VBox bottomPane = new VBox();
        bottomPane.getChildren().addAll(createStatusBar(), createConsolePane());
        root.setBottom(bottomPane);

        Scene scene = new Scene(root, 1600, 900);

        try {
            String css = getClass().getResource("styles.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            System.err.println("Could not load CSS: " + e.getMessage());
        }
        primaryStage.setScene(scene);

        primaryStage.setMaximized(true);
        primaryStage.show();

        if (animator != null) {
            animator.start();
        }
    }

    private TabPane createRibbon() {
        TabPane ribbon = new TabPane();
        ribbon.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        ribbon.getStyleClass().add("ribbon-pane");

        Tab editTab = new Tab("Edit");
        ToolBar editToolbar = new ToolBar();
        editToolbar.getItems().addAll(
                createRibbonButton("Undo", "Undo (Ctrl+Z)", e -> performUndo()),
                createRibbonButton("Redo", "Redo (Ctrl+Shift+Z)", e -> performRedo()));
        editTab.setContent(editToolbar);

        Tab sketchTab = new Tab("Sketch");
        ToolBar sketchToolbar = new ToolBar();
        sketchToolbar.getItems().addAll(
                createRibbonButton("Sketch", "Create/Edit Sketch", e -> appendOutput("Sketch Mode Active")),
                createRibbonButton("Load", "Open File", e -> showLoadDialog()),
                createRibbonButton("Save", "Save STL", e -> showSaveDialog()),
                new Separator(),
                createRibbonButton("2D/3D View", "Toggle 2D/3D View", e -> toggleSketchView()),
                new Separator(),
                createRibbonButton("Line", "Line Entity",
                        e -> activateSketchTool(SketchInteractionManager.InteractionMode.SKETCH_LINE,
                                "Line: Click and drag to draw a line")),
                createRibbonButton("Circle", "Circle Entity",
                        e -> activateSketchTool(SketchInteractionManager.InteractionMode.SKETCH_CIRCLE,
                                "Circle: Click center, drag to set radius")),
                createRibbonButton("Arc", "Arc Entity",
                        e -> activateSketchTool(SketchInteractionManager.InteractionMode.SKETCH_ARC,
                                "Arc: Click center, start point, then end point")),
                createRibbonButton("Polygon", "Polygon Entity", e -> showPolygonParametersDialog()),
                createRibbonButton("Point", "Point Entity",
                        e -> activateSketchTool(SketchInteractionManager.InteractionMode.SKETCH_POINT,
                                "Point: Click to place points")),
                createRibbonButton("Spline", "Spline Entity",
                        e -> activateSketchTool(SketchInteractionManager.InteractionMode.SKETCH_SPLINE,
                                "Spline: Click to add points, double-click to finish")),

                createRibbonButton("Kite", "Kite Entity", e -> showKiteDialog()),
                createRibbonButton("NACA", "NACA Airfoil", e -> showNacaDialog()),
                new Separator(),
                createRibbonButton("Trim", "Trim Entities", e -> showNotImplemented("Trim")),
                createRibbonButton("Offset", "Offset Entities", e -> showNotImplemented("Offset")));
        sketchTab.setContent(sketchToolbar);

        Tab featuresTab = new Tab("Features");
        ToolBar featuresToolbar = new ToolBar();
        featuresToolbar.getItems().addAll(
                createRibbonButton("Cube", "Create Cube", e -> showCubeDialog()),
                createRibbonButton("Sphere", "Create Sphere", e -> showSphereDialog()),
                new Separator(),
                createRibbonButton("Extruded\nBoss/Base", "Extrude Sketch", e -> extrudeSketch()),
                createRibbonButton("Revolved\nBoss/Base", "Revolve Sketch", e -> showRevolveDialog()),
                createRibbonButton("Swept\nBoss/Base", "Sweep Sketch", e -> showSweepDialog()),
                createRibbonButton("Lofted\nBoss/Base", "Loft Sketches", e -> showLoftDialog()),
                new Separator(),
                createRibbonButton("Extruded\nCut", "Cut Material", e -> performExtrudeCut()),
                createRibbonButton("Intersect", "Boolean Intersect", e -> performIntersect()),
                createRibbonButton("Fillet", "Round Edges", e -> showNotImplemented("Fillet")),
                createRibbonButton("Shell", "Shell Feature", e -> showNotImplemented("Shell")),
                createRibbonButton("Wrap", "Wrap Feature", e -> showNotImplemented("Wrap")),
                createRibbonButton("Draft", "Draft Feature", e -> showNotImplemented("Draft")));
        featuresTab.setContent(featuresToolbar);

        Tab evaluateTab = new Tab("Evaluate");
        ToolBar evaluateToolbar = new ToolBar();
        evaluateToolbar.getItems().addAll(
                createRibbonButton("Dimension", "Measure Distance/Radius", e -> activateDimensionTool()),
                createRibbonButton("Mass Props", "Calculate Mass Properties", e -> showMassPropertiesDialog()),
                createRibbonButton("Centroid", "Toggle Centroid Display", e -> toggleCentroid()),
                new Separator(),
                createRibbonButton("Materials", "Manage Material Database", e -> showMaterialDatabaseDialog()));
        evaluateTab.setContent(evaluateToolbar);

        Tab constraintsTab = new Tab("Constraints");
        ToolBar constraintsToolbar = new ToolBar();
        constraintsToolbar.getItems().addAll(
                createRibbonButton("Select", "Select Entities", e -> activateSelectionTool()),
                new Separator(),
                createRibbonButton("Horizontal", "Make Horizontal", e -> applyHorizontalConstraint()),
                createRibbonButton("Vertical", "Make Vertical", e -> applyVerticalConstraint()),
                createRibbonButton("Coincident", "Make Coincident", e -> applyCoincidentConstraint()),
                createRibbonButton("Fixed", "Fix Position", e -> applyFixedConstraint()),
                createRibbonButton("Coincident", "Make Coincident", e -> applyCoincidentConstraint()),
                createRibbonButton("Fixed", "Fix Position", e -> applyFixedConstraint()),
                new Separator(),
                createRibbonButton("Tangent", "Make Tangent", e -> applyTangentConstraint()),
                createRibbonButton("Concentric", "Make Concentric", e -> applyConcentricConstraint()),
                createRibbonButton("Parallel", "Make Parallel", e -> applyParallelConstraint()),
                createRibbonButton("Perpendicular", "Make Perpendicular", e -> applyPerpendicularConstraint()),
                createRibbonButton("Midpoint", "Make Midpoint", e -> applyMidpointConstraint()),
                createRibbonButton("Equal", "Make Equal", e -> applyEqualConstraint()),
                createRibbonButton("Collinear", "Make Collinear", e -> applyCollinearConstraint()),
                createRibbonButton("Symmetric", "Make Symmetric", e -> applySymmetricConstraint()),
                new Separator(),
                createRibbonButton("Solve", "Solve Constraints", e -> {
                    sketch.solveConstraints();
                    glCanvas.repaint();
                    appendOutput("Constraints Solved.");
                }));
        constraintsTab.setContent(constraintsToolbar);

        Tab toolsTab = new Tab("Tools");
        ToolBar toolsToolbar = new ToolBar();
        toolsToolbar.getItems().addAll(
                createRibbonButton("Record", "Record Macro", e -> macroManager.startRecording()),
                createRibbonButton("Stop", "Stop Recording", e -> macroManager.stopRecording()),
                createRibbonButton("Run", "Run Macro", e -> showRunMacroMenu()),
                new Separator(),
                createRibbonButton("Add-Ins", "Manage Add-Ins", e -> appendOutput("Add-In Manager opened.")));
        toolsTab.setContent(toolsToolbar);

        Tab aeroTab = new Tab("Aerodynamics");
        ToolBar aeroToolbar = new ToolBar();
        aeroToolbar.getItems().addAll(
                createRibbonButton("NACA", "NACA Airfoil Generator", e -> showNacaDialog()),
                createRibbonButton("Analyze", "CFD Analysis", e -> showCfdDialog()));
        aeroTab.setContent(aeroToolbar);

        ribbon.getTabs().addAll(editTab, sketchTab, featuresTab, constraintsTab, evaluateTab, aeroTab, toolsTab);
        return ribbon;
    }

    private Button createRibbonButton(String text, String tooltipText,
            javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setOnAction(handler);

        btn.setMinWidth(60);
        btn.setMaxHeight(60);
        btn.setPrefHeight(60);
        return btn;
    }

    private VBox createFeatureManagerTree() {
        VBox container = new VBox();
        container.setMinWidth(250);
        container.setStyle("-fx-background-color: white;");

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

        rootItem.getChildren().addAll(List.of(history, sensors, annotations, material, frontPlane, topPlane, rightPlane,
                origin));

        TreeView<String> tree = new TreeView<>(rootItem);
        tree.setShowRoot(true);
        VBox.setVgrow(tree, Priority.ALWAYS);

        tree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<String> selectedItem = tree.getSelectionModel().getSelectedItem();
                if (selectedItem != null) {
                    String itemName = selectedItem.getValue();
                    orientViewToPlane(itemName);
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem createPlaneItem = new MenuItem("Create New Plane...");
        createPlaneItem.setOnAction(e -> showCreatePlaneDialog(rootItem));
        contextMenu.getItems().add(createPlaneItem);
        tree.setContextMenu(contextMenu);

        container.getChildren().add(tree);

        return container;
    }

    private TabPane createTaskPane() {
        TabPane taskPane = new TabPane();
        taskPane.setSide(javafx.geometry.Side.RIGHT);
        taskPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        taskPane.setMinWidth(250);

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

        Tab libraryTab = new Tab("Design Library");
        VBox libraryContent = new VBox();
        libraryContent.setPadding(new Insets(5));
        TreeItem<String> libRoot = new TreeItem<>("Design Library");
        libRoot.setExpanded(true);
        libRoot.getChildren().addAll(List.of(
                new TreeItem<>("Toolbox"),
                new TreeItem<>("3D Interconnect"),
                new TreeItem<>("Routing"),
                new TreeItem<>("Smart Components")));
        TreeView<String> libTree = new TreeView<>(libRoot);
        libraryContent.getChildren().add(libTree);
        libraryTab.setContent(libraryContent);

        Tab appearancesTab = new Tab("Appearances");
        appearancesTab.setContent(new Label("  Appearances, Scenes,\n  and Decals"));

        Tab helpTab = new Tab("Help");
        VBox helpContent = new VBox();
        helpContent.setFillWidth(true);
        helpContent.getChildren().add(createHelpPane());
        helpTab.setContent(helpContent);

        taskPane.getTabs().addAll(resourcesTab, libraryTab, appearancesTab, helpTab);

        return taskPane;
    }

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
                    default:
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

    @Override
    public void stop() throws Exception {
        cleanupAndExit();
        super.stop();
    }

    private void cleanupAndExit() {
        try {
            if (animator != null) {
                if (animator.isAnimating()) {
                    animator.stop();
                }
                Thread.sleep(50);
            }
            if (glCanvas != null) {
                glCanvas.destroy();
            }
        } catch (Exception e) {
            System.err.println("Warning during cleanup: " + e.getMessage());
        }
    }

    private void initializeComponents() {

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-font-family: 'Courier New', monospace;");

        initializeGLCanvas();
    }

    private void initializeGLCanvas() {
        SwingUtilities.invokeLater(() -> {

            GLProfile profile = GLProfile.get(GLProfile.GL2);

            GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setDoubleBuffered(true);
            capabilities.setHardwareAccelerated(true);

            glCanvas = new JOGLCadCanvas(sketch);
            glCanvas.setMinimumSize(new Dimension(600, 450));
            glCanvas.setPreferredSize(new Dimension(1200, 900));

            glRenderer = new OpenGLRenderer(interactionManager);
            glCanvas.addGLEventListener(glRenderer);
            glCanvas.addMouseListener(new CanvasMouseListener());
            glCanvas.addMouseMotionListener(new CanvasMouseMotionListener());
            glCanvas.addMouseWheelListener(new CanvasMouseWheelListener());
            glCanvas.addKeyListener(new CanvasKeyListener());
            glCanvas.setFocusable(true);
            glCanvas.requestFocusInWindow();

            animator = new FPSAnimator(glCanvas, 60);

            JPanel glPanel = new JPanel(new java.awt.BorderLayout());
            glPanel.add(glCanvas, java.awt.BorderLayout.CENTER);

            Platform.runLater(() -> {
                canvasNode = new SwingNode();

                canvasNode.setContent(glPanel);
            });
        });
    }

    private TabPane createControlPanel() {
        TabPane controlTabs = new TabPane();
        controlTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        controlTabs.setMinWidth(280);

        Tab treeTab = new Tab("Feature Tree");
        treeTab.setContent(createFeatureManagerTree());

        Tab configTab = new Tab("Configuration");
        configTab.setContent(createConfigurationManager());

        controlTabs.getTabs().addAll(treeTab, configTab);
        return controlTabs;
    }

    private VBox createConfigurationManager() {
        VBox container = new VBox();
        container.setSpacing(5);
        container.setPadding(new Insets(5));

        ToolBar configTools = new ToolBar();
        Button addConfigBtn = new Button("+");
        addConfigBtn.setTooltip(new Tooltip("Add Configuration"));
        Button delConfigBtn = new Button("-");
        delConfigBtn.setTooltip(new Tooltip("Delete Configuration"));
        configTools.getItems().addAll(addConfigBtn, delConfigBtn);

        TreeItem<String> rootConfig = new TreeItem<>("Part1 Configurations");
        rootConfig.setExpanded(true);

        TreeItem<String> defaultConfig = new TreeItem<>("Default [Active]");
        TreeItem<String> description = new TreeItem<>("Description: Default configuration");
        defaultConfig.getChildren().add(description);
        defaultConfig.setExpanded(true);

        rootConfig.getChildren().add(defaultConfig);

        TreeView<String> configTree = new TreeView<>(rootConfig);
        VBox.setVgrow(configTree, Priority.ALWAYS);

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

    public void setViewMode(boolean showSketch) {
        if (glRenderer != null) {
            glRenderer.setShowSketch(showSketch);
            glCanvas.repaint();
        }
    }

    private void requestViewChange(boolean showSketch) {
        if (glRenderer != null) {
            boolean currentMode = glRenderer.isShowSketch();
            if (currentMode != showSketch) {

                ViewChangeCommand cmd = new ViewChangeCommand(this, showSketch, currentMode);
                commandManager.executeCommand(cmd);
                appendOutput("View changed to " + (showSketch ? "2D Sketch" : "3D View"));
            }
        }
    }

    private void showNotImplemented(String featureName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Feature Not Implemented");
        alert.setHeaderText(null);
        alert.setContentText(featureName + " is not yet implemented in this version.");
        alert.showAndWait();
    }

    private void applyTangentConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        if (selected.size() == 2) {
            Entity e1 = selected.get(0);
            Entity e2 = selected.get(1);

            Object obj1 = e1 instanceof Sketch.PointEntity ? ((Sketch.PointEntity) e1).getPoint() : e1;
            Object obj2 = e2 instanceof Sketch.PointEntity ? ((Sketch.PointEntity) e2).getPoint() : e2;

            // TangentConstraint handles Line-Circle, Circle-Circle, Arc-Line, etc.
            // We need to pass the raw geometric objects usually, but let's see what
            // TangentConstraint expects.
            // It expects: Sketch.Circle/Arc/Line objects. Entity subclasses match this
            // mostly.

            Constraint c = new cad.core.TangentConstraint(e1, e2);
            sketch.addConstraint(c);
            appendOutput("Added Tangent Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 2 entities for Tangent");
        }
    }

    private void applyConcentricConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        if (selected.size() == 2 &&
                selected.get(0) instanceof Sketch.Circle &&
                selected.get(1) instanceof Sketch.Circle) {

            Constraint c = new cad.core.ConcentricConstraint((Sketch.Circle) selected.get(0),
                    (Sketch.Circle) selected.get(1));
            sketch.addConstraint(c);
            appendOutput("Added Concentric Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 2 circles for Concentric");
        }
    }

    private void applyParallelConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        if (selected.size() == 2 &&
                selected.get(0) instanceof Sketch.Line &&
                selected.get(1) instanceof Sketch.Line) {

            Constraint c = new cad.core.ParallelConstraint((Sketch.Line) selected.get(0),
                    (Sketch.Line) selected.get(1));
            sketch.addConstraint(c);
            appendOutput("Added Parallel Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 2 lines for Parallel");
        }
    }

    private void applyPerpendicularConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        if (selected.size() == 2 &&
                selected.get(0) instanceof Sketch.Line &&
                selected.get(1) instanceof Sketch.Line) {

            Constraint c = new cad.core.PerpendicularConstraint((Sketch.Line) selected.get(0),
                    (Sketch.Line) selected.get(1));
            sketch.addConstraint(c);
            appendOutput("Added Perpendicular Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 2 lines for Perpendicular");
        }
    }

    private void applyMidpointConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        // Try to find one point and one line
        Sketch.PointEntity p = null;
        Sketch.Line l = null;

        for (Entity e : selected) {
            if (e instanceof Sketch.PointEntity)
                p = (Sketch.PointEntity) e;
            if (e instanceof Sketch.Line)
                l = (Sketch.Line) e;
        }

        if (p != null && l != null && selected.size() == 2) {
            Constraint c = new cad.core.MidpointConstraint(p.getPoint(), l.getStartPoint(), l.getEndPoint());
            sketch.addConstraint(c);
            appendOutput("Added Midpoint Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 1 point and 1 line for Midpoint");
        }
    }

    private void applyEqualConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        if (selected.size() == 2) {
            Constraint c = new cad.core.EqualConstraint(selected.get(0), selected.get(1));
            sketch.addConstraint(c);
            appendOutput("Added Equal Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 2 compatible entities for Equal");
        }
    }

    private void applyCollinearConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        // Need 3 points? Or 2 lines?
        // CollinearConstraint implementation expects 3 Points
        // Standard CAD "Collinear" usually means two lines are collinear.
        // Our implementation checks area of triangle of 3 points.
        // Let's implement Collinear for 2 lines using the logic:
        // Line 1 is (p1, p2), Line 2 is (p3, p4).
        // 3 points (p1, p2, p3) collinear AND (p1, p2, p4) collinear implies lines are
        // collinear.

        if (selected.size() == 2 && selected.get(0) instanceof Sketch.Line && selected.get(1) instanceof Sketch.Line) {
            Sketch.Line l1 = (Sketch.Line) selected.get(0);
            Sketch.Line l2 = (Sketch.Line) selected.get(1);

            // Constrain l2's start point to be collinear with l1
            Constraint c1 = new cad.core.CollinearConstraint(l1.getStartPoint(), l1.getEndPoint(), l2.getStartPoint());
            // Constrain l2's end point to be collinear with l1
            Constraint c2 = new cad.core.CollinearConstraint(l1.getStartPoint(), l1.getEndPoint(), l2.getEndPoint());

            sketch.addConstraint(c1);
            sketch.addConstraint(c2);
            appendOutput("Added Collinear Constraint (Line-Line)");
            glCanvas.repaint();

        } else if (selected.size() == 3) {
            // Check if all are points
            // ... helper for points if needed
            appendOutput("Select 2 lines for Collinear");
        } else {
            appendOutput("Select 2 lines for Collinear");
        }
    }

    private void applySymmetricConstraint() {
        List<Entity> selected = interactionManager.getSelectedEntities();
        // Need 2 points and 1 center line
        Sketch.PointEntity p1 = null;
        Sketch.PointEntity p2 = null;
        Sketch.Line centerLine = null;

        for (Entity e : selected) {
            if (e instanceof Sketch.PointEntity) {
                if (p1 == null)
                    p1 = (Sketch.PointEntity) e;
                else if (p2 == null)
                    p2 = (Sketch.PointEntity) e;
            }
            if (e instanceof Sketch.Line) {
                centerLine = (Sketch.Line) e; // Use last selected line as symmetry axis?
                // Ideally user selects 2 points then a line, or we infer center line is
                // construction line?
                // For now, assume 1 line selected.
            }
        }

        if (p1 != null && p2 != null && centerLine != null && selected.size() == 3) {
            Constraint c = new cad.core.SymmetricConstraint(p1.getPoint(), p2.getPoint(), centerLine);
            sketch.addConstraint(c);
            appendOutput("Added Symmetric Constraint");
            glCanvas.repaint();
        } else {
            appendOutput("Select 2 points and 1 symmetry line");
        }
    }

    private Label statusLabel;

    private BorderPane createStatusBar() {
        BorderPane statusBar = new BorderPane();
        statusBar.getStyleClass().add("status-bar");

        statusLabel = new Label("Ready");
        statusBar.setLeft(statusLabel);

        return statusBar;
    }

    private TitledPane createConsolePane() {
        TitledPane consolePane = new TitledPane("Console Output", outputArea);
        consolePane.setCollapsible(true);
        consolePane.setExpanded(true);
        consolePane.setMaxHeight(150);
        consolePane.setPrefHeight(100);

        return consolePane;
    }

    private class OpenGLRenderer implements GLEventListener {
        private GLU glu = new GLU();

        private double[] modelviewMatrix = new double[16];
        private double[] projectionMatrix = new double[16];
        private int[] viewport = new int[4];

        private boolean showSketch = false;
        private boolean showCentroid = false;
        private float[] modelCentroid = null;
        private SketchInteractionManager interactionManager;
        private TextRenderer textRenderer;

        public OpenGLRenderer(SketchInteractionManager interactionManager) {
            this.interactionManager = interactionManager;
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_LIGHT0);

            float[] globalAmbient = { 0.3f, 0.3f, 0.3f, 1.0f };
            gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, globalAmbient, 0);

            float[] lightPos = { 1.0f, 1.0f, 1.0f, 0.0f };
            float[] lightDiffuse = { 1.0f, 1.0f, 1.0f, 1.0f };
            float[] lightSpecular = { 1.0f, 1.0f, 1.0f, 1.0f };
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightDiffuse, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, lightSpecular, 0);

            textRenderer = new TextRenderer(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 24));
        }

        @Override
        public void display(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

            if (this.showSketch) {

                renderSketch(gl, drawable);
            } else {

                updateProjectionMatrix(drawable);

                gl.glLoadIdentity();

                gl.glTranslatef(0.0f, 0.0f, zoom);
                gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
                gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);

                if (stlTriangles != null) {
                    renderStlTriangles(gl);
                    renderModelAxes(gl, drawable);
                } else {
                    renderAxes(drawable);
                    renderPlanes(gl);
                }
            }

            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelviewMatrix, 0);
            gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projectionMatrix, 0);
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
        }

        private void updateProjectionMatrix(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            int width = drawable.getSurfaceWidth();
            int height = drawable.getSurfaceHeight();

            if (height == 0)
                height = 1;
            float aspect = (float) width / height;

            float modelSize = Geometry.getModelMaxDimension();
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.005f);
            float farPlane = Math.max(1000.0f, Math.abs(zoom) + modelSize * 5.0f);

            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPerspective(45.0, aspect, nearPlane, farPlane);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
        }

        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();

            if (height == 0)
                height = 1;
            float aspect = (float) width / height;

            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();

            float modelSize = Geometry.getModelMaxDimension();
            float nearPlane = Math.max(0.01f, Math.abs(zoom) * 0.005f);
            float farPlane = Math.max(1000.0f, Math.abs(zoom) + modelSize * 5.0f);

            glu.gluPerspective(45.0, aspect, nearPlane, farPlane);

            gl.glMatrixMode(GL2.GL_MODELVIEW);
        }

        @Override
        public void dispose(GLAutoDrawable drawable) {

        }

        private void renderAxes(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glLineWidth(3.0f);

            float axisLength = 10.0f;
            float arrowLen = 2.0f;
            float arrowBase = 0.8f;

            gl.glBegin(GL2.GL_LINES);

            gl.glColor3f(1.0f, 0.0f, 0.0f);
            gl.glVertex3f(0.0f, 0.0f, 0.0f);
            gl.glVertex3f(axisLength, 0.0f, 0.0f);

            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glVertex3f(0.0f, 0.0f, 0.0f);
            gl.glVertex3f(0.0f, axisLength, 0.0f);

            gl.glColor3f(0.0f, 0.0f, 1.0f);
            gl.glVertex3f(0.0f, 0.0f, 0.0f);
            gl.glVertex3f(0.0f, 0.0f, axisLength);

            gl.glEnd();

            gl.glColor3f(1.0f, 0.0f, 0.0f);
            gl.glPushMatrix();
            gl.glTranslatef(axisLength, 0.0f, 0.0f);
            gl.glRotatef(90.0f, 0.0f, 1.0f, 0.0f);
            drawCone(gl, arrowBase, arrowLen, 16);
            gl.glPopMatrix();

            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glPushMatrix();
            gl.glTranslatef(0.0f, axisLength, 0.0f);
            gl.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);
            drawCone(gl, arrowBase, arrowLen, 16);
            gl.glPopMatrix();

            gl.glColor3f(0.0f, 0.0f, 1.0f);
            gl.glPushMatrix();
            gl.glTranslatef(0.0f, 0.0f, axisLength);

            drawCone(gl, arrowBase, arrowLen, 16);
            gl.glPopMatrix();

            if (textRenderer != null) {
                renderAxisLabels(drawable, axisLength + arrowLen, 0, 0, 0);
            }

            gl.glEnable(GL2.GL_LIGHTING);
        }

        private void drawCone(GL2 gl, float radius, float height, int slices) {
            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex3f(0.0f, 0.0f, height);
            for (int i = 0; i <= slices; i++) {
                double angle = 2 * Math.PI * i / slices;
                float x = (float) (radius * Math.cos(angle));
                float y = (float) (radius * Math.sin(angle));
                gl.glVertex3f(x, y, 0.0f);
            }
            gl.glEnd();

            gl.glBegin(GL2.GL_TRIANGLE_FAN);
            gl.glVertex3f(0.0f, 0.0f, 0.0f);
            for (int i = 0; i <= slices; i++) {
                double angle = 2 * Math.PI * i / slices;
                float x = (float) (radius * Math.cos(angle));
                float y = (float) (radius * Math.sin(angle));
                gl.glVertex3f(x, -y, 0.0f);
            }
            gl.glEnd();
        }

        private void renderAxisLabels(GLAutoDrawable drawable, float axisLength, float originX, float originY,
                float originZ) {
            GL2 gl = drawable.getGL().getGL2();

            double[] modelview = new double[16];
            double[] projection = new double[16];
            int[] viewport = new int[4];

            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
            gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

            double[] winCoords = new double[3];

            float labelOffset = 2.0f;

            textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

            int charWidth = 18;
            int charHeight = 24;

            boolean xProjected = glu.gluProject(originX + axisLength + labelOffset, originY, originZ,
                    modelview, 0, projection, 0, viewport, 0,
                    winCoords, 0);
            if (xProjected && winCoords[2] >= 0 && winCoords[2] <= 1) {
                textRenderer.setColor(1.0f, 0.0f, 0.0f, 1.0f);
                int x = (int) winCoords[0] - charWidth / 2;
                int y = (int) winCoords[1] - charHeight / 2;
                textRenderer.draw("X", x, y);
            }

            boolean yProjected = glu.gluProject(originX, originY + axisLength + labelOffset, originZ,
                    modelview, 0, projection, 0, viewport, 0,
                    winCoords, 0);
            if (yProjected && winCoords[2] >= 0 && winCoords[2] <= 1) {
                textRenderer.setColor(0.0f, 0.8f, 0.0f, 1.0f);
                int x = (int) winCoords[0] - charWidth / 2;
                int y = (int) winCoords[1] - charHeight / 2;
                textRenderer.draw("Y", x, y);
            }

            boolean zProjected = glu.gluProject(originX, originY, originZ + axisLength + labelOffset,
                    modelview, 0, projection, 0, viewport, 0,
                    winCoords, 0);
            if (zProjected && winCoords[2] >= 0 && winCoords[2] <= 1) {
                textRenderer.setColor(0.0f, 0.4f, 1.0f, 1.0f);
                int x = (int) winCoords[0] - charWidth / 2;
                int y = (int) winCoords[1] - charHeight / 2;
                textRenderer.draw("Z", x, y);
            }

            textRenderer.endRendering();
        }

        private void renderModelAxes(GL2 gl, GLAutoDrawable drawable) {
            if (modelCentroid == null) {
                return;
            }

            float modelSize = Geometry.getModelMaxDimension();
            float axisLength = Math.max(modelSize * 0.75f, 5.0f);

            float cx = modelCentroid[0];
            float cy = modelCentroid[1];
            float cz = modelCentroid[2];

            gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
            try {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glDisable(GL2.GL_DEPTH_TEST);
                gl.glLineWidth(3.0f);

                gl.glBegin(GL2.GL_LINES);

                gl.glColor3f(1.0f, 0.0f, 0.0f);
                gl.glVertex3f(cx, cy, cz);
                gl.glVertex3f(cx + axisLength, cy, cz);

                gl.glColor3f(0.0f, 1.0f, 0.0f);
                gl.glVertex3f(cx, cy, cz);
                gl.glVertex3f(cx, cy + axisLength, cz);

                gl.glColor3f(0.0f, 0.0f, 1.0f);
                gl.glVertex3f(cx, cy, cz);
                gl.glVertex3f(cx, cy, cz + axisLength);
                gl.glEnd();

                gl.glColor3f(1.0f, 1.0f, 0.0f);
                gl.glPointSize(10.0f);
                gl.glBegin(GL2.GL_POINTS);
                gl.glVertex3f(cx, cy, cz);
                gl.glEnd();
            } finally {
                gl.glPopAttrib();
            }

            if (textRenderer != null) {
                renderAxisLabels(drawable, axisLength, cx, cy, cz);
            }
        }

        private void renderPlanes(GL2 gl) {
            gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS);
            try {
                gl.glDisable(GL2.GL_LIGHTING);
                gl.glEnable(GL2.GL_BLEND);
                gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

                float size = 10.0f;

                gl.glColor4f(0.0f, 0.0f, 0.8f, 0.2f);
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-size, -size, 0);
                gl.glVertex3f(size, -size, 0);
                gl.glVertex3f(size, size, 0);
                gl.glVertex3f(-size, size, 0);
                gl.glEnd();

                gl.glColor3f(0.0f, 0.0f, 1.0f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex3f(-size, -size, 0);
                gl.glVertex3f(size, -size, 0);
                gl.glVertex3f(size, size, 0);
                gl.glVertex3f(-size, size, 0);
                gl.glEnd();

                gl.glColor4f(0.0f, 0.8f, 0.0f, 0.2f);
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(-size, 0, -size);
                gl.glVertex3f(size, 0, -size);
                gl.glVertex3f(size, 0, size);
                gl.glVertex3f(-size, 0, size);
                gl.glEnd();

                gl.glColor3f(0.0f, 1.0f, 0.0f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex3f(-size, 0, -size);
                gl.glVertex3f(size, 0, -size);
                gl.glVertex3f(size, 0, size);
                gl.glVertex3f(-size, 0, size);
                gl.glEnd();

                gl.glColor4f(0.8f, 0.0f, 0.0f, 0.2f);
                gl.glBegin(GL2.GL_QUADS);
                gl.glVertex3f(0, -size, -size);
                gl.glVertex3f(0, size, -size);
                gl.glVertex3f(0, size, size);
                gl.glVertex3f(0, -size, size);
                gl.glEnd();

                gl.glColor3f(1.0f, 0.0f, 0.0f);
                gl.glBegin(GL2.GL_LINE_LOOP);
                gl.glVertex3f(0, -size, -size);
                gl.glVertex3f(0, size, -size);
                gl.glVertex3f(0, size, size);
                gl.glVertex3f(0, -size, size);
                gl.glEnd();

            } finally {
                gl.glPopAttrib();
            }
        }

        private void renderStlTriangles(GL2 gl) {

            cad.core.Material material = sketch != null ? sketch.getMaterial() : null;

            if (material != null) {

                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, material.getAmbientColor(), 0);
                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, material.getDiffuseColor(), 0);
                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, material.getSpecularColor(), 0);
                gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, material.getShininess());
            } else {

                float[] defaultAmbient = { 0.25f, 0.15f, 0.1f, 1.0f };
                float[] defaultDiffuse = { 0.8f, 0.6f, 0.4f, 1.0f };
                float[] defaultSpecular = { 0.3f, 0.3f, 0.3f, 1.0f };
                float defaultShininess = 30.0f;

                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT, defaultAmbient, 0);
                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_DIFFUSE, defaultDiffuse, 0);
                gl.glMaterialfv(GL2.GL_FRONT_AND_BACK, GL2.GL_SPECULAR, defaultSpecular, 0);
                gl.glMaterialf(GL2.GL_FRONT_AND_BACK, GL2.GL_SHININESS, defaultShininess);
            }

            gl.glBegin(GL2.GL_TRIANGLES);

            if (stlTriangles != null) {
                for (float[] triangle : stlTriangles) {

                    gl.glNormal3f(triangle[0], triangle[1], triangle[2]);

                    gl.glVertex3f(triangle[3], triangle[4], triangle[5]);
                    gl.glVertex3f(triangle[6], triangle[7], triangle[8]);
                    gl.glVertex3f(triangle[9], triangle[10], triangle[11]);
                }
            }

            gl.glEnd();
        }

        public void renderSketch(GL2 gl, GLAutoDrawable drawable) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3f(0.0f, 0.0f, 0.0f);

            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            int[] viewport = new int[4];
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
            int width = viewport[2];
            int height = viewport[3];

            double aspectRatio = (double) width / height;
            double baseSize = 50.0;
            double size = baseSize / sketch2DZoom;

            if (aspectRatio >= 1.0) {

                gl.glOrtho(-size * aspectRatio, size * aspectRatio, -size, size, -1.0, 1.0);
            } else {

                gl.glOrtho(-size, size, -size / aspectRatio, size / aspectRatio, -1.0, 1.0);
            }

            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPushMatrix();
            gl.glLoadIdentity();

            gl.glTranslatef(sketch2DPanX, sketch2DPanY, 0.0f);

            sketch.draw(gl);

            if (interactionManager != null && interactionManager.isDrawing()) {
                renderGhost(gl, drawable);
            }

            if (textRenderer != null && sketch != null) {
                renderDimensionText(gl, drawable);
            }

            renderSelectionHighlights(gl);

            gl.glPopMatrix();
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glPopMatrix();
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glEnable(GL2.GL_LIGHTING);
        }

        private void renderDimensionText(GL2 gl, GLAutoDrawable drawable) {
            List<cad.core.Dimension> dims = sketch.getDimensions();
            if (dims.isEmpty())
                return;

            double[] modelview = new double[16];
            double[] projection = new double[16];
            int[] viewport = new int[4];

            gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
            gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);
            gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

            double[] winCoords = new double[3];

            textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            textRenderer.setColor(0.3f, 0.3f, 0.3f, 1.0f);

            for (cad.core.Dimension dim : dims) {
                if (!dim.isVisible())
                    continue;

                if (glu.gluProject(dim.getTextX(), dim.getTextY(), dim.getTextZ(),
                        modelview, 0, projection, 0, viewport, 0,
                        winCoords, 0)) {

                    textRenderer.draw(dim.getLabel(), (int) winCoords[0], (int) winCoords[1]);
                }
            }

            textRenderer.endRendering();
        }

        private void renderGhost(GL2 gl, GLAutoDrawable drawable) {

            gl.glColor3f(0.2f, 0.5f, 1.0f);
            gl.glLineWidth(2.0f);

            float x1 = interactionManager.getStartX();
            float y1 = interactionManager.getStartY();
            float x2 = interactionManager.getCurrentX();
            float y2 = interactionManager.getCurrentY();

            String dimensionText = "";

            if (interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_LINE) {
                gl.glBegin(GL2.GL_LINES);
                gl.glVertex2f(x1, y1);
                gl.glVertex2f(x2, y2);
                gl.glEnd();

                drawPoint(gl, x1, y1, 3.0f);
                drawPoint(gl, x2, y2, 3.0f);

                float length = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                dimensionText = String.format("%.2f %s", length, currentUnits.getAbbreviation());
            } else if (interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_CIRCLE) {
                float radius = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                gl.glBegin(GL2.GL_LINE_LOOP);
                int segments = 50;
                for (int i = 0; i < segments; i++) {
                    double angle = 2.0 * Math.PI * i / segments;
                    gl.glVertex2d(x1 + radius * Math.cos(angle), y1 + radius * Math.sin(angle));
                }
                gl.glEnd();

                drawPoint(gl, x1, y1, 4.0f);

                dimensionText = String.format("R%.2f %s", radius, currentUnits.getAbbreviation());
            } else if (interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_POLYGON) {
                float radius = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
                gl.glBegin(GL2.GL_LINE_LOOP);
                int sides = interactionManager.getPolygonSides();
                for (int i = 0; i < sides; i++) {
                    double angle = 2.0 * Math.PI * i / sides;
                    gl.glVertex2d(x1 + radius * Math.cos(angle), y1 + radius * Math.sin(angle));
                }
                gl.glEnd();

                drawPoint(gl, x1, y1, 4.0f);

                dimensionText = String.format("R%.2f %s", radius, currentUnits.getAbbreviation());
            }

            gl.glLineWidth(1.0f);

            if (!dimensionText.isEmpty() && textRenderer != null) {

                double[] modelview = new double[16];
                double[] projection = new double[16];
                int[] viewport = new int[4];

                gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, modelview, 0);
                gl.glGetDoublev(GL2.GL_PROJECTION_MATRIX, projection, 0);
                gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);

                double[] winCoords = new double[3];

                if (glu.gluProject(x2, y2, 0, modelview, 0, projection, 0, viewport, 0, winCoords, 0)) {
                    textRenderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                    textRenderer.setColor(0.2f, 0.5f, 1.0f, 1.0f);

                    int textX = (int) winCoords[0] + 15;
                    int textY = (int) winCoords[1] + 15;

                    textRenderer.draw(dimensionText, textX, textY);
                    textRenderer.endRendering();
                }
            }
        }

        private void renderSelectionHighlights(GL2 gl) {
            List<Entity> selected = interactionManager.getSelectedEntities();
            if (selected.isEmpty())
                return;

            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3f(1.0f, 0.5f, 0.0f);
            gl.glLineWidth(3.0f);

            for (Entity e : selected) {
                if (e instanceof PointEntity) {
                    PointEntity p = (PointEntity) e;
                    gl.glPointSize(8.0f);
                    gl.glBegin(GL2.GL_POINTS);
                    gl.glVertex2f(p.getX(), p.getY());
                    gl.glEnd();
                    gl.glPointSize(1.0f);
                } else if (e instanceof Line) {
                    Line l = (Line) e;
                    gl.glBegin(GL2.GL_LINES);
                    gl.glVertex2f(l.getX1(), l.getY1());
                    gl.glVertex2f(l.getX2(), l.getY2());
                    gl.glEnd();
                } else if (e instanceof Sketch.Circle) {
                    Sketch.Circle c = (Sketch.Circle) e;
                    gl.glBegin(GL2.GL_LINE_LOOP);
                    int segments = 50;
                    for (int i = 0; i < segments; i++) {
                        double angle = 2.0 * Math.PI * i / segments;
                        float x = c.getX() + c.getRadius() * (float) Math.cos(angle);
                        float y = c.getY() + c.getRadius() * (float) Math.sin(angle);
                        gl.glVertex2f(x, y);
                    }
                    gl.glEnd();
                }
            }
            gl.glLineWidth(1.0f);
        }

        private void drawPoint(GL2 gl, float x, float y, float size) {
            gl.glPointSize(size);
            gl.glBegin(GL2.GL_POINTS);
            gl.glVertex2f(x, y);
            gl.glEnd();
            gl.glPointSize(1.0f);
        }

        public void handle3DDimensionClick(int mouseX, int mouseY) {

            int glY = viewport[3] - mouseY;

            double[] nearPoint = new double[3];
            double[] farPoint = new double[3];

            glu.gluUnProject(mouseX, glY, 0.0, modelviewMatrix, 0, projectionMatrix, 0, viewport, 0, nearPoint, 0);

            glu.gluUnProject(mouseX, glY, 1.0, modelviewMatrix, 0, projectionMatrix, 0, viewport, 0, farPoint, 0);

            float[] rayOrigin = { (float) nearPoint[0], (float) nearPoint[1], (float) nearPoint[2] };
            float[] rayDir = {
                    (float) (farPoint[0] - nearPoint[0]),
                    (float) (farPoint[1] - nearPoint[1]),
                    (float) (farPoint[2] - nearPoint[2])
            };

            float len = (float) Math.sqrt(rayDir[0] * rayDir[0] + rayDir[1] * rayDir[1] + rayDir[2] * rayDir[2]);
            if (len > 0) {
                rayDir[0] /= len;
                rayDir[1] /= len;
                rayDir[2] /= len;
            }

            float[] triangle = Geometry.pickFace(rayOrigin, rayDir);

            if (triangle != null) {

                float area = Geometry.calculateTriangleArea(triangle);
                appendOutput(String.format("Selected Face Area: %.4f sq units", area));
            } else {
                appendOutput("No geometry selected.");
            }
        }

        public void setStlTriangles(List<float[]> triangles) {
            stlTriangles = triangles;
            modelCentroid = calculateStlCentroid(triangles);
            setShowSketch(false);
            glCanvas.repaint();
        }

        public boolean isShowSketch() {
            return showSketch;
        }

        private float[] calculateStlCentroid(List<float[]> triangles) {
            if (triangles == null || triangles.isEmpty()) {
                return null;
            }

            float sumX = 0, sumY = 0, sumZ = 0;
            int vertexCount = 0;

            for (float[] triangle : triangles) {

                for (int i = 3; i < 12; i += 3) {
                    sumX += triangle[i];
                    sumY += triangle[i + 1];
                    sumZ += triangle[i + 2];
                    vertexCount++;
                }
            }

            if (vertexCount > 0) {
                return new float[] {
                        sumX / vertexCount,
                        sumY / vertexCount,
                        sumZ / vertexCount
                };
            }

            return null;
        }

        public void setShowSketch(boolean show) {
            this.showSketch = show;
            glCanvas.repaint();
        }

        public boolean isShowingSketch() {
            return this.showSketch;
        }

        public boolean isShowingCentroid() {
            return this.showCentroid;
        }

        public void setShowCentroid(boolean show) {
            this.showCentroid = show;
        }
    }

    private class CanvasMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {

            if (interactionManager != null &&
                    interactionManager.getMode() == SketchInteractionManager.InteractionMode.DIMENSION_TOOL &&
                    glRenderer != null && glRenderer.isShowingSketch()) {

                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                handleDimensionClick(worldCoords[0], worldCoords[1]);
                return;
            }

            if (interactionManager != null &&
                    interactionManager.getMode() == SketchInteractionManager.InteractionMode.DIMENSION_TOOL &&
                    glRenderer != null && !glRenderer.isShowingSketch()) {

                glRenderer.handle3DDimensionClick(e.getX(), e.getY());
                return;
            }

            if (interactionManager != null &&
                    interactionManager.getMode() == SketchInteractionManager.InteractionMode.SKETCH_SPLINE &&
                    glRenderer != null && glRenderer.isShowingSketch()) {
                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                interactionManager.handleMouseClick(e, worldCoords[0], worldCoords[1]);

                if (glCanvas != null)
                    glCanvas.repaint();
                return;
            }

            glCanvas.requestFocusInWindow();

            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
            appendOutput("Canvas clicked - focus restored. Try arrow keys or mouse drag to move view.");
        }

        @Override
        public void mousePressed(MouseEvent e) {

            if (glRenderer != null && glRenderer.isShowingSketch() &&
                    interactionManager != null &&
                    (interactionManager.getMode() == SketchInteractionManager.InteractionMode.IDLE ||
                            interactionManager.getMode() == SketchInteractionManager.InteractionMode.VIEW_ROTATE)) {
                appendOutput("Select a tool from the ribbon to draw.");
            }

            if (interactionManager != null &&
                    interactionManager.getMode() != SketchInteractionManager.InteractionMode.IDLE &&
                    interactionManager.getMode() != SketchInteractionManager.InteractionMode.VIEW_ROTATE) {

                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());

                interactionManager.handleMousePress(worldCoords[0], worldCoords[1]);
                glCanvas.repaint();

                isDragging = false;
                return;
            }

            lastMouseX = e.getX();
            lastMouseY = e.getY();
            isDragging = true;
            glCanvas.requestFocusInWindow();

            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isDragging = false;

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
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }

    private class CanvasMouseMotionListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (interactionManager != null && interactionManager.isDrawing()) {
                float[] worldCoords = getSketchWorldCoordinates(e.getX(), e.getY());
                interactionManager.handleMouseMove(worldCoords[0], worldCoords[1]);
                glCanvas.repaint();
                return;
            }

            if (isDragging) {
                int deltaX = e.getX() - lastMouseX;
                int deltaY = e.getY() - lastMouseY;

                if (glRenderer != null && glRenderer.isShowingSketch()) {

                    float panSpeed = 0.1f / sketch2DZoom;
                    sketch2DPanX += deltaX * panSpeed;
                    sketch2DPanY -= deltaY * panSpeed;
                } else {

                    rotationY += deltaX * 0.5f;
                    rotationX += deltaY * 0.5f;
                }

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                glCanvas.repaint();
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

    private class CanvasMouseWheelListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (glRenderer != null && glRenderer.isShowingSketch()) {

                float zoomFactor = 1.1f;
                if (e.getWheelRotation() < 0) {

                    sketch2DZoom *= zoomFactor;
                } else {

                    sketch2DZoom /= zoomFactor;
                }

                if (sketch2DZoom < 0.1f)
                    sketch2DZoom = 0.1f;
                if (sketch2DZoom > 20.0f)
                    sketch2DZoom = 20.0f;
            } else {

                zoom += e.getWheelRotation() * 0.5f;

                if (zoom > -1.0f)
                    zoom = -1.0f;
                if (zoom < -800.0f)
                    zoom = -800.0f;
            }
            glCanvas.repaint();
        }

    }

    public void setViewFront() {

        if (glRenderer != null) {
            glRenderer.setShowSketch(false);
        }
        rotationX = 0;
        rotationY = 0;
        glCanvas.repaint();
        glCanvas.requestFocusInWindow();
        appendOutput("View: Front (XY Plane)");
    }

    public void setViewTop() {

        if (glRenderer != null) {
            glRenderer.setShowSketch(false);
        }
        rotationX = 90;
        rotationY = 0;
        glCanvas.repaint();
        glCanvas.requestFocusInWindow();
        appendOutput("View: Top (XZ Plane)");
    }

    public void setViewRight() {

        if (glRenderer != null) {
            glRenderer.setShowSketch(false);
        }
        rotationX = 0;
        rotationY = -90;
        glCanvas.repaint();
        glCanvas.requestFocusInWindow();
        appendOutput("View: Right (YZ Plane)");
    }

    public void setViewIsometric() {

        if (glRenderer != null) {
            glRenderer.setShowSketch(false);
        }
        rotationX = 35.264f;
        rotationY = -45.0f;
        glCanvas.repaint();
        glCanvas.requestFocusInWindow();
        appendOutput("View: Isometric (3D)");
    }

    private class CanvasKeyListener implements KeyListener {

        @Override
        public void keyPressed(KeyEvent e) {
            boolean viewChanged = false;
            boolean is2DSketchMode = (glRenderer != null && glRenderer.isShowingSketch());

            switch (e.getKeyCode()) {

                case KeyEvent.VK_UP:
                    if (is2DSketchMode) {

                        sketch2DPanY += 2.0f / sketch2DZoom;
                    } else {

                        rotationX -= 3.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_DOWN:
                    if (is2DSketchMode) {

                        sketch2DPanY -= 2.0f / sketch2DZoom;
                    } else {

                        rotationX += 3.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_LEFT:
                    if (is2DSketchMode) {

                        sketch2DPanX -= 2.0f / sketch2DZoom;
                    } else {

                        rotationY -= 3.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_RIGHT:
                    if (is2DSketchMode) {

                        sketch2DPanX += 2.0f / sketch2DZoom;
                    } else {

                        rotationY += 3.0f;
                    }
                    viewChanged = true;
                    break;

                case KeyEvent.VK_Q:
                case KeyEvent.VK_PLUS:
                case KeyEvent.VK_EQUALS:
                    if (is2DSketchMode) {

                        sketch2DZoom *= 1.2f;
                        if (sketch2DZoom > 20.0f)
                            sketch2DZoom = 20.0f;
                    } else {

                        zoom += 0.5f;
                        if (zoom > -1.0f)
                            zoom = -1.0f;
                    }
                    viewChanged = true;
                    break;
                case KeyEvent.VK_E:
                case KeyEvent.VK_MINUS:
                    if (is2DSketchMode) {

                        sketch2DZoom /= 1.2f;
                        if (sketch2DZoom < 0.1f)
                            sketch2DZoom = 0.1f;
                    } else {

                        zoom -= 0.5f;
                        if (zoom < -800.0f)
                            zoom = -800.0f;
                    }
                    viewChanged = true;
                    break;

                case KeyEvent.VK_R:
                    if (is2DSketchMode) {

                        sketch2DPanX = 0.0f;
                        sketch2DPanY = 0.0f;
                        sketch2DZoom = 1.0f;
                        appendOutput("2D sketch view reset");
                    } else {

                        resetView();
                        appendOutput("3D model view reset with auto-scaling");
                    }
                    viewChanged = true;
                    break;

                case KeyEvent.VK_SPACE:
                    toggleSketchView();
                    break;

                case KeyEvent.VK_ESCAPE:

                    if (interactionManager != null &&
                            interactionManager.getMode() != SketchInteractionManager.InteractionMode.IDLE &&
                            interactionManager.getMode() != SketchInteractionManager.InteractionMode.VIEW_ROTATE) {
                        interactionManager.setMode(SketchInteractionManager.InteractionMode.VIEW_ROTATE);
                        appendOutput("Drawing cancelled. Select a tool to draw again.");
                        viewChanged = true;
                    } else {

                        glCanvas.requestFocusInWindow();
                        appendOutput("Canvas focus restored");
                    }
                    break;

                case KeyEvent.VK_H:
                    if (e.isControlDown()) {
                        showKeyboardHelp();
                    }
                    break;
            }

            if (viewChanged && glCanvas != null) {
                glCanvas.repaint();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent e) {
        }
    }

    private void activateSketchTool(SketchInteractionManager.InteractionMode mode, String message) {

        if (interactionManager != null) {
            interactionManager.setMode(mode);
        }

        if (glRenderer != null) {
            requestViewChange(true);
            appendOutput("Switched to 2D sketch mode");
            if (statusLabel != null)
                statusLabel.setText(message);
        }

        appendOutput(message);
        appendOutput("Press ESC to cancel drawing");

        Platform.runLater(() -> {
            if (canvasNode != null) {
                canvasNode.requestFocus();
            }
        });

        if (glCanvas != null) {
            glCanvas.repaint();
        }
    }

    private void performUndo() {
        if (commandManager != null && commandManager.canUndo()) {
            commandManager.undo();
            appendOutput(commandManager.getUndoDescription());
            if (glCanvas != null) {
                glCanvas.repaint();
            }
        } else {
            appendOutput("Nothing to undo");
        }
    }

    private void performRedo() {
        if (commandManager != null && commandManager.canRedo()) {
            commandManager.redo();
            appendOutput(commandManager.getRedoDescription());
            if (glCanvas != null) {
                glCanvas.repaint();
            }
        } else {
            appendOutput("Nothing to redo");
        }
    }

    private void activateDimensionTool() {

        if (interactionManager != null) {
            interactionManager.setMode(SketchInteractionManager.InteractionMode.DIMENSION_TOOL);
        }

        if (statusLabel != null)
            statusLabel.setText("Dimension Tool: Select Line/Circle/2 Points to measure");

        if (glRenderer != null && !glRenderer.isShowingSketch()) {
            glRenderer.setShowSketch(true);
            appendOutput("Switched to 2D sketch mode for dimensioning");
        }

        appendOutput("Dimension Tool: Click an entity to measure it");
        appendOutput("  - Line: shows length");
        appendOutput("  - Circle: shows radius");
        appendOutput("  - Polygon: shows edge length");
        appendOutput("Press ESC to deactivate tool");

        Platform.runLater(() -> {
            if (canvasNode != null) {
                canvasNode.requestFocus();
            }
        });

        if (glCanvas != null) {
            glCanvas.repaint();
        }
    }

    private void handleDimensionClick(float worldX, float worldY) {
        if (sketch == null) {
            appendOutput("No sketch to dimension");
            return;
        }

        Sketch.Entity entity = sketch.findClosestEntity(worldX, worldY, 2.0f);

        if (entity != null) {

            cad.core.Dimension dim = sketch.createDimensionFor(entity, worldX, worldY);
            if (dim != null) {
                sketch.addDimension(dim);
                appendOutput("Dimension added: " + dim.getLabel());
                glCanvas.repaint();
            } else {
                appendOutput("Could not create dimension for this entity type");
            }
        } else {
            appendOutput("No entity found at click location (click closer to an entity)");
        }
    }

    private void orientViewToPlane(String planeName) {
        if (glRenderer == null)
            return;

        if (glRenderer.isShowingSketch()) {
            glRenderer.setShowSketch(false);
        }

        if (planeName.contains("Front")) {

            rotationX = 0.0f;
            rotationY = 0.0f;
            appendOutput("View oriented to Front Plane (XY)");
        } else if (planeName.contains("Top")) {

            rotationX = -90.0f;
            rotationY = 0.0f;
            appendOutput("View oriented to Top Plane (XZ)");
        } else if (planeName.contains("Right")) {

            rotationX = 0.0f;
            rotationY = -90.0f;
            appendOutput("View oriented to Right Plane (YZ)");
        } else {

            rotationX = 35.264f;
            rotationY = 45.0f;
            appendOutput("View oriented to Isometric");
        }

        zoom = -50.0f;

        if (glCanvas != null) {
            glCanvas.repaint();
        }
    }

    public void toggleSketchView() {
        if (glRenderer != null) {
            boolean currentSketchMode = glRenderer.isShowingSketch();
            glRenderer.setShowSketch(!currentSketchMode);
            if (!currentSketchMode) {
                appendOutput("Switched to 2D sketch view - Use arrow keys to pan, Q/E to zoom");

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

    private void resetView() {
        rotationX = 0.0f;
        rotationY = 0.0f;

        float maxDimension = Geometry.getModelMaxDimension();
        if (maxDimension > 0) {

            if (maxDimension <= 10.0f) {

                zoom = -(maxDimension * 1.8f);
            } else if (maxDimension <= 50.0f) {

                zoom = -(maxDimension * 1.5f);
            } else if (maxDimension <= 150.0f) {

                zoom = -(maxDimension * 1.2f);
            } else {

                zoom = -(maxDimension * 0.8f + (float) Math.log10(maxDimension) * 15.0f);
            }

            if (zoom < -800.0f)
                zoom = -800.0f;
            if (zoom > -2.0f)
                zoom = -2.0f;

            appendOutput("View reset - zoom auto-adjusted to " + String.format("%.1f", zoom) + " for model size "
                    + String.format("%.1f", maxDimension));
        } else {
            zoom = -5.0f;
        }

        if (glCanvas != null) {
            glCanvas.repaint();
        }
    }

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

    private void appendOutput(String text) {
        Platform.runLater(() -> {
            outputArea.appendText(text + "\n");
            outputArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    private float[] getSketchWorldCoordinates(int screenX, int screenY) {
        if (glCanvas == null)
            return new float[] { 0, 0 };

        float width = glCanvas.getWidth();
        float height = glCanvas.getHeight();

        float scale = 100.0f / (sketch2DZoom * height);

        float relX = screenX - width / 2.0f;
        float relY = height / 2.0f - screenY;

        float worldX = (relX * scale) - sketch2DPanX;
        float worldY = (relY * scale) - sketch2DPanY;

        return new float[] { worldX, worldY };
    }

    private TitledPane createHelpPane() {
        helpText = new TextArea();
        helpText.setEditable(false);
        helpText.setWrapText(true);
        helpText.setText("Shortcuts:\n" +
                "Rotate: Arrow Keys\n" +
                "Zoom: Mouse Scroll\n" +
                "Reset: Double-click plane\n" +
                "Undo: Ctrl+Z");

        helpScrollPane = new ScrollPane(helpText);
        helpScrollPane.setFitToWidth(true);

        TitledPane helpPane = new TitledPane("Help / Shortcuts", helpScrollPane);
        helpPane.setCollapsible(true);
        helpPane.setExpanded(false);
        return helpPane;
    }

    private void showHelp() {
        if (helpText == null)
            return;
        helpText.setText(
                "Keyboard Shortcuts:\n" +
                        "-------------------\n" +
                        "Rotate View:   Arrow Keys\n" +
                        "Zoom View:     Mouse Scroll\n" +
                        "Reset View:    Double-click Planes in Tree\n" +
                        "Undo:          Ctrl + Z\n" +
                        "Redo:          Ctrl + Shift + Z\n" +
                        "Help:          H\n\n" +
                        "Features:\n" +
                        "---------\n" +
                        "Sketches:      Create 2D profiles on Z=0 plane\n" +
                        "Constraints:   Apply Geometric relations (Horiz, Vert)\n" +
                        "Dimensions:    Context-aware 2D and 3D measurements\n" +
                        "Extrude:       Convert closed sketch to 3D solid\n" +
                        "Revolve:       Revolve sketch (requires CL centerline)\n" +
                        "Tree View:     Manage history and view planes\n\n" +
                        "Tip: Switch to 'Sketch' tab to start drawing!");
    }

    private void showSaveDialog() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");

        FileChooser.ExtensionFilter stlFilter = new FileChooser.ExtensionFilter("STL files (*.stl)", "*.stl");
        FileChooser.ExtensionFilter dxfFilter = new FileChooser.ExtensionFilter("DXF files (*.dxf)", "*.dxf");

        fileChooser.getExtensionFilters().addAll(stlFilter, dxfFilter);

        fileChooser.setSelectedExtensionFilter(stlFilter);
        fileChooser.setInitialFileName("model.stl");

        Window ownerWindow = outputArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow);

        if (file != null) {
            String filepath = file.getAbsolutePath();
            String extension = "";

            FileChooser.ExtensionFilter selectedFilter = fileChooser.getSelectedExtensionFilter();
            if (selectedFilter == stlFilter) {
                extension = "stl";
            } else if (selectedFilter == dxfFilter) {
                extension = "dxf";
            } else {

                if (filepath.toLowerCase().endsWith(".dxf")) {
                    extension = "dxf";
                } else {
                    extension = "stl";
                }
            }

            if (extension.equals("stl") && !filepath.toLowerCase().endsWith(".stl")) {
                filepath += ".stl";
            } else if (extension.equals("dxf") && !filepath.toLowerCase().endsWith(".dxf")) {
                filepath += ".dxf";
            }

            try {
                if (extension.equals("stl")) {
                    Geometry.saveStl(filepath);
                    appendOutput("3D Model saved to: " + filepath);
                } else if (extension.equals("dxf")) {
                    sketch.exportSketchToDXF(filepath);
                    appendOutput("2D Sketch exported to: " + filepath);
                }
            } catch (Exception e) {
                appendOutput("Error saving file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            appendOutput("File save cancelled.");
        }
    }

    private void exportDXF() {
        String filename = "sketch.dxf";
        if (filename.isEmpty()) {
            appendOutput("Please enter a filename in the File Name field for DXF export.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export DXF File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DXF files (*.dxf)", "*.dxf"));
        fileChooser.setInitialFileName(filename);

        Window ownerWindow = outputArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow);

        if (file != null) {
            try {
                sketch.exportSketchToDXF(file.getAbsolutePath());
                appendOutput("Sketch exported successfully to DXF: " + file.getAbsolutePath());
            } catch (Exception e) {
                appendOutput("Error exporting DXF: " + e.getMessage());
            }
        } else {
            appendOutput("DXF export cancelled.");
        }
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

                    resetView();
                    setViewIsometric();
                } else if (lowerPath.endsWith(".dxf")) {
                    sketch.loadDXF(path);
                    if (glRenderer != null)
                        glRenderer.setShowSketch(true);
                    appendOutput("Loaded DXF: " + path);

                    sketch2DPanX = 0.0f;
                    sketch2DPanY = 0.0f;
                    sketch2DZoom = 1.0f;
                    glCanvas.repaint();
                }
            } catch (Exception e) {
                appendOutput("Error loading file: " + e.getMessage());
            }
        }
    }

    private void performExtrudeCut() {
        if (sketch == null) {
            appendOutput("No sketch available to cut.");
            return;
        }

        Stage dialog = new Stage();
        dialog.setTitle("Extruded Cut");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        TextField heightField = new TextField("10.0");

        layout.getChildren().addAll(
                new Label("Cut Depth:"), heightField,
                new Button("Cut") {
                    {
                        setOnAction(e -> {
                            try {
                                float depth = Float.parseFloat(heightField.getText());

                                Geometry.extrude(sketch, depth, Geometry.BooleanOp.DIFFERENCE);

                                sketch.setDirty(true);

                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                glRenderer.setShowSketch(false);
                                requestViewChange(false);
                                glCanvas.repaint();

                                appendOutput("Extruded Cut performed: depth=" + depth);
                                dialog.close();
                            } catch (NumberFormatException ex) {
                                appendOutput("Invalid depth.");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 250, 150));
        dialog.show();
    }

    private void performIntersect() {
        if (sketch == null) {
            appendOutput("No sketch available to intersect.");
            return;
        }

        Stage dialog = new Stage();
        dialog.setTitle("Intersect");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        TextField heightField = new TextField("10.0");

        layout.getChildren().addAll(
                new Label("Extrusion Depth for Intersection:"), heightField,
                new Button("Intersect") {
                    {
                        setOnAction(e -> {
                            try {
                                float depth = Float.parseFloat(heightField.getText());

                                Geometry.extrude(sketch, depth, Geometry.BooleanOp.INTERSECTION);

                                sketch.setDirty(true);

                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                glRenderer.setShowSketch(false);
                                requestViewChange(false);
                                glCanvas.repaint();

                                appendOutput("Intersect performed: depth=" + depth);
                                dialog.close();
                            } catch (NumberFormatException ex) {
                                appendOutput("Invalid depth.");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 300, 150));
        dialog.show();
    }

    private void extrudeSketch() {

        if (sketch.listSketch().isEmpty()) {
            appendOutput("Error: Sketch is empty. Add some geometry first.");
            appendOutput("Tip: Use sketch commands to create points, lines, circles, or polygons.");
            return;
        }

        Stage extrudeDialog = new Stage();
        extrudeDialog.setTitle("Extrude Sketch");
        extrudeDialog.initModality(Modality.APPLICATION_MODAL);
        extrudeDialog.setResizable(false);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label titleLabel = new Label("Extrude 2D Sketch into 3D");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label instructionLabel = new Label("Enter the height for extrusion:");

        TextField heightField = new TextField("10.0");
        heightField.setPrefWidth(150);
        heightField.setPromptText("Height value");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button extrudeButton = new Button("Extrude");
        Button cancelButton = new Button("Cancel");

        extrudeButton.setPrefWidth(80);
        cancelButton.setPrefWidth(80);

        buttonBox.getChildren().addAll(extrudeButton, cancelButton);

        layout.getChildren().addAll(titleLabel, instructionLabel, heightField, buttonBox);

        extrudeButton.setOnAction(e -> {
            try {
                double height = Double.parseDouble(heightField.getText());
                if (height <= 0) {
                    appendOutput("Error: Extrusion height must be greater than 0.");
                    return;
                }

                sketch.extrude(height);
                sketch.setDirty(true);

                List<float[]> extrudedTriangles = sketch.getExtrudedTriangles();

                if (extrudedTriangles != null && !extrudedTriangles.isEmpty()) {

                    glRenderer.setStlTriangles(extrudedTriangles);

                    appendOutput("Successfully extruded sketch with height " + height);
                    appendOutput("Generated " + extrudedTriangles.size() + " triangles from extrusion.");
                    appendOutput("Switching to 3D view to show extruded geometry.");

                    requestViewChange(false);
                    resetView();
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

        heightField.setOnAction(e -> extrudeButton.fire());

        Scene scene = new Scene(layout, 300, 200);
        extrudeDialog.setScene(scene);

        extrudeDialog.initOwner(outputArea.getScene().getWindow());

        Platform.runLater(() -> heightField.requestFocus());

        extrudeDialog.showAndWait();
    }

    private void showKiteDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Create Kite");
        dialog.initModality(Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField cxField = new TextField("0");
        TextField cyField = new TextField("0");
        TextField vField = new TextField("10");
        TextField hField = new TextField("6");
        TextField angleField = new TextField("0");

        grid.add(new Label("Center X:"), 0, 0);
        grid.add(cxField, 1, 0);
        grid.add(new Label("Center Y:"), 0, 1);
        grid.add(cyField, 1, 1);
        grid.add(new Label("Vertical Diag:"), 0, 2);
        grid.add(vField, 1, 2);
        grid.add(new Label("Horizontal Diag:"), 0, 3);
        grid.add(hField, 1, 3);
        grid.add(new Label("Angle (deg):"), 0, 4);
        grid.add(angleField, 1, 4);

        Button btnCreate = new Button("Create");
        btnCreate.setOnAction(e -> {
            try {
                float cx = Float.parseFloat(cxField.getText());
                float cy = Float.parseFloat(cyField.getText());
                float v = Float.parseFloat(vField.getText());
                float h = Float.parseFloat(hField.getText());
                float a = Float.parseFloat(angleField.getText());
                sketch.addKite(cx, cy, v, h, a);
                appendOutput("Kite created at (" + cx + "," + cy + ")");
                glCanvas.repaint();
                dialog.close();
            } catch (NumberFormatException ex) {
                appendOutput("Invalid number format for Kite");
            }
        });

        grid.add(btnCreate, 1, 5);
        Scene scene = new Scene(grid);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showCubeDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Create Cube");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        TextField sizeField = new TextField("10.0");
        TextField divField = new TextField(String.valueOf(cubeDivisions));
        ComboBox<String> operationBox = new ComboBox<>();
        operationBox.getItems().addAll("New Body", "Merge (Union)", "Cut (Differ...)", "Intersect");
        operationBox.setValue("New Body");

        layout.getChildren().addAll(
                new Label("Size:"), sizeField,
                new Label("Divisions:"), divField,
                new Label("Operation:"), operationBox,
                new Button("Create") {
                    {
                        setOnAction(e -> {
                            try {
                                float size = Float.parseFloat(sizeField.getText());
                                int divs = Integer.parseInt(divField.getText());
                                cubeDivisions = divs;

                                String op = operationBox.getValue();
                                Geometry.BooleanOp boolOp = Geometry.BooleanOp.NONE;
                                if (op.startsWith("Merge"))
                                    boolOp = Geometry.BooleanOp.UNION;
                                else if (op.startsWith("Cut"))
                                    boolOp = Geometry.BooleanOp.DIFFERENCE;
                                else if (op.startsWith("Intersect"))
                                    boolOp = Geometry.BooleanOp.INTERSECTION;

                                Geometry.createCube(size, divs, boolOp);

                                if (sketch != null)
                                    sketch.setDirty(true);

                                appendOutput("Cube created: size=" + size + ", op=" + op);

                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                requestViewChange(false);
                                resetView();
                                glCanvas.repaint();
                                dialog.close();
                            } catch (Exception ex) {
                                appendOutput("Invalid input for Cube");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 300, 350));
        dialog.show();
    }

    private void showSphereDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Create Sphere");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));

        TextField radiusField = new TextField("5.0");
        TextField latField = new TextField(String.valueOf(sphereLatDiv));
        TextField lonField = new TextField(String.valueOf(sphereLonDiv));

        layout.getChildren().addAll(
                new Label("Radius:"), radiusField,
                new Label("Lat. Divisions:"), latField,
                new Label("Lon. Divisions:"), lonField,
                new Button("Create") {
                    {
                        setOnAction(e -> {
                            try {
                                float r = Float.parseFloat(radiusField.getText());
                                int lat = Integer.parseInt(latField.getText());
                                int lon = Integer.parseInt(lonField.getText());
                                sphereLatDiv = lat;
                                sphereLonDiv = lon;
                                Geometry.createSphere(r, lat, lon);

                                if (sketch != null)
                                    sketch.setDirty(true);

                                appendOutput("Sphere created: r=" + r + ", lat=" + lat + ", lon=" + lon);

                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                requestViewChange(false);
                                resetView();
                                glCanvas.repaint();
                                dialog.close();
                            } catch (Exception ex) {
                                appendOutput("Invalid input for Sphere");
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 300, 350));
        dialog.show();
    }

    private void showPolygonParametersDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Polygon Parameters");
        dialog.initModality(Modality.APPLICATION_MODAL);

        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setPadding(new Insets(15));

        Label sidesLabel = new Label("Number of Sides:");
        javafx.scene.control.Spinner<Integer> sidesSpinner = new javafx.scene.control.Spinner<>(3, 20, 6);
        sidesSpinner.setEditable(true);
        sidesSpinner.setPrefWidth(100);

        layout.add(sidesLabel, 0, 0);
        layout.add(sidesSpinner, 1, 0);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button okButton = new Button("OK");
        Button cancelButton = new Button("Cancel");
        buttons.getChildren().addAll(okButton, cancelButton);

        layout.add(buttons, 0, 1, 2, 1);

        okButton.setOnAction(e -> {
            int sides = sidesSpinner.getValue();
            if (interactionManager != null) {
                interactionManager.setPolygonSides(sides);
            }
            activateSketchTool(
                    SketchInteractionManager.InteractionMode.SKETCH_POLYGON,
                    sides + "-sided Polygon: Click center, drag to set radius");
            dialog.close();
        });

        cancelButton.setOnAction(e -> dialog.close());

        Scene scene = new Scene(layout, 280, 120);
        dialog.setScene(scene);
        dialog.initOwner(outputArea.getScene().getWindow());

        Platform.runLater(() -> sidesSpinner.requestFocus());

        dialog.showAndWait();
    }

    private void showCreatePlaneDialog(TreeItem<String> rootItem) {
        Stage dialog = new Stage();
        dialog.setTitle("Create Custom Plane");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setResizable(false);

        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setPadding(new Insets(20));

        Label nameLabel = new Label("Plane Name:");
        TextField nameField = new TextField("Custom Plane 1");
        nameField.setPrefWidth(200);
        layout.add(nameLabel, 0, 0);
        layout.add(nameField, 1, 0);

        Label refLabel = new Label("Parallel To:");
        javafx.scene.control.ComboBox<String> refCombo = new javafx.scene.control.ComboBox<>();
        refCombo.getItems().addAll("Front Plane", "Top Plane", "Right Plane");
        refCombo.setValue("Front Plane");
        layout.add(refLabel, 0, 1);
        layout.add(refCombo, 1, 1);

        Label offsetLabel = new Label("Offset Distance:");
        Spinner<Double> offsetSpinner = new Spinner<>(-1000.0, 1000.0, 0.0, 1.0);
        offsetSpinner.setEditable(true);
        offsetSpinner.setPrefWidth(120);
        Label unitLabel = new Label(currentUnits.getAbbreviation());
        HBox offsetBox = new HBox(5, offsetSpinner, unitLabel);
        layout.add(offsetLabel, 0, 2);
        layout.add(offsetBox, 1, 2);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        Button okButton = new Button("Create");
        okButton.setDefaultButton(true);
        okButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        Button cancelButton = new Button("Cancel");
        buttons.getChildren().addAll(okButton, cancelButton);
        layout.add(buttons, 0, 3, 2, 1);

        okButton.setOnAction(e -> {
            String planeName = nameField.getText().trim();
            if (!planeName.isEmpty()) {

                TreeItem<String> newPlane = new TreeItem<>(planeName + " Plane");
                rootItem.getChildren().add(newPlane);

                String refPlane = refCombo.getValue();
                double offset = offsetSpinner.getValue();

                appendOutput("Created custom plane: " + planeName +
                        " (parallel to " + refPlane + ", offset " + offset + " " + currentUnits.getAbbreviation()
                        + ")");

                dialog.close();
            }
        });

        cancelButton.setOnAction(e -> dialog.close());

        Scene scene = new Scene(layout, 350, 180);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void setApplicationUnits(UnitSystem units) {
        this.currentUnits = units;
        if (sketch != null) {
            sketch.setUnitSystem(units);
        }
        appendOutput("Units set to: " + units.getAbbreviation() + " (" + units.name() + ")");
    }

    private void showNacaDialog() {
        NacaDialog nacaDialog = new NacaDialog(sketch, commandManager);
        nacaDialog.setOnGenerateCallback(() -> {
            List<float[]> extrudedTriangles = sketch.getExtrudedTriangles();
            if (extrudedTriangles != null && !extrudedTriangles.isEmpty()) {
                glRenderer.setStlTriangles(extrudedTriangles);
                appendOutput("Auto-extruded NACA airfoil to 3D");
                requestViewChange(false);
                resetView();
            } else {
                requestViewChange(true);
                resetView();
            }
            glCanvas.repaint();
        });
        nacaDialog.show();
    }

    private void showCfdDialog() {
        CfdDialog cfdDialog = new CfdDialog();
        cfdDialog.setOutputCallback(() -> {
            appendOutput("CFD Analysis complete.");
        });
        cfdDialog.show();
    }

    private void showRevolveDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Revolve Feature");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        ComboBox<String> axisBox = new ComboBox<>();
        axisBox.getItems().addAll("X-Axis", "Y-Axis");
        axisBox.setValue("Y-Axis");

        TextField angleField = new TextField("360");

        layout.getChildren().addAll(
                new Label("Axis of Revolution:"), axisBox,
                new Label("Angle (deg):"), angleField,
                new Button("Revolve") {
                    {
                        setOnAction(e -> {
                            try {
                                float angle = Float.parseFloat(angleField.getText());

                                String axisName = axisBox.getValue().substring(0, 1);

                                Geometry.revolve(sketch, axisName, angle, Geometry.BooleanOp.UNION);

                                if (sketch != null)
                                    sketch.setDirty(true);

                                appendOutput("Revolved sketch " + angle + " degrees around " + axisName + "-Axis");

                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                glRenderer.setShowSketch(false);
                                requestViewChange(false);
                                glCanvas.repaint();
                                dialog.close();
                            } catch (Exception ex) {
                                appendOutput("Error revolving: " + ex.getMessage());
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 250, 200));
        dialog.show();
    }

    private void showSweepDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Sweep Feature");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        layout.getChildren().addAll(
                new Label("Sweep Profile along Path"),
                new Label("(Requires 1 Profile and 1 Path in Sketch)"),
                new Button("Sweep") {
                    {
                        setOnAction(e -> {
                            try {

                                Geometry.sweep(sketch, Geometry.BooleanOp.UNION);
                                if (sketch != null)
                                    sketch.setDirty(true);

                                appendOutput("Sweep operation initiated.");
                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                glRenderer.setShowSketch(false);
                                resetView();
                                glCanvas.repaint();
                                dialog.close();
                            } catch (Exception ex) {
                                appendOutput("Error sweeping: " + ex.getMessage());
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 300, 200));
        dialog.show();
    }

    private void showLoftDialog() {
        Stage dialog = new Stage();
        dialog.setTitle("Loft Feature");
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        layout.getChildren().addAll(
                new Label("Loft Profiles"),
                new Label("(Lofts all sections in Sketch order)"),
                new Button("Loft") {
                    {
                        setOnAction(e -> {
                            try {
                                Geometry.loft(sketch, Geometry.BooleanOp.UNION);
                                if (sketch != null)
                                    sketch.setDirty(true);

                                appendOutput("Loft operation initiated.");
                                glRenderer.setStlTriangles(Geometry.getExtrudedTriangles());
                                glRenderer.setShowSketch(false);
                                resetView();
                                glCanvas.repaint();
                                dialog.close();
                            } catch (Exception ex) {
                                appendOutput("Error lofting: " + ex.getMessage());
                            }
                        });
                    }
                });

        dialog.setScene(new Scene(layout, 300, 200));
        dialog.show();
    }

    private void showMassPropertiesDialog() {

        Geometry.Shape currentShape = Geometry.getCurrentShape();

        if (currentShape == Geometry.Shape.CSG_RESULT || currentShape == Geometry.Shape.EXTRUDED) {

            if (sketch.getMaterial() == null) {
                appendOutput("Error: No material assigned. Please set a material first.");
                return;
            }

            float volume = cad.core.Geometry.calculateVolume();
            float surfaceArea = cad.core.Geometry.calculateSurfaceArea();

            if (volume <= 0 || surfaceArea <= 0) {
                appendOutput("Error: Could not calculate mass properties for 3D model.");
                return;
            }

            MassProperties props = MassProperties.calculateFrom3DMesh(
                    volume,
                    surfaceArea,
                    sketch.getMaterial(),
                    sketch.getUnitSystem());

            MassPropertiesDialog dialog = new MassPropertiesDialog(sketch, props);
            dialog.showAndWait();
        } else {

            MassPropertiesDialog dialog = new MassPropertiesDialog(sketch);
            dialog.showAndWait();
        }
    }

    private void toggleCentroid() {
        if (glRenderer != null) {
            boolean newState = !glRenderer.isShowingCentroid();
            glRenderer.setShowCentroid(newState);
            glCanvas.repaint();
            appendOutput(newState ? "Centroid display enabled" : "Centroid display disabled");
        }
    }

    private void showMaterialDatabaseDialog() {
        MaterialDatabaseDialog dialog = new MaterialDatabaseDialog(sketch, () -> {

            if (glCanvas != null) {
                glCanvas.repaint();
            }
        }, this::appendOutput);
        dialog.showAndWait();
    }

    private void activateSelectionTool() {
        interactionManager.setMode(SketchInteractionManager.InteractionMode.SELECT);
        interactionManager.clearSelection();
        appendOutput("Selection Mode Active. Click to select entities.");
        if (statusLabel != null)
            statusLabel.setText("Select entities. Click to toggle selection.");
    }

    private void applyHorizontalConstraint() {

        List<Entity> selected = interactionManager.getSelectedEntities();

        if (selected.size() == 1 && selected.get(0) instanceof Line) {

            Line line = (Line) selected.get(0);
            Constraint c = new HorizontalConstraint(line.getStartPoint(), line.getEndPoint());
            commandManager.executeCommand(new AddConstraintCommand(sketch, c));
            appendOutput("Applied Horizontal Constraint to Line.");
            interactionManager.clearSelection();
            glCanvas.repaint();
        } else if (selected.size() == 2 && selected.get(0) instanceof PointEntity
                && selected.get(1) instanceof PointEntity) {

            PointEntity p1 = (PointEntity) selected.get(0);
            PointEntity p2 = (PointEntity) selected.get(1);
            Constraint c = new HorizontalConstraint(p1.getPoint(), p2.getPoint());
            commandManager.executeCommand(new AddConstraintCommand(sketch, c));
            appendOutput("Applied Horizontal Constraint to Points.");
            interactionManager.clearSelection();
            glCanvas.repaint();
        } else {

            interactionManager.clearSelection();
            interactionManager.setMode(SketchInteractionManager.InteractionMode.SELECT);

            if (glRenderer != null && !glRenderer.isShowingSketch()) {
                glRenderer.setShowSketch(true);
                appendOutput("Switched to 2D sketch mode");
            }

            appendOutput("Horizontal Constraint: Click to select 1 Line or 2 Points, then click this button again.");
            if (statusLabel != null)
                statusLabel.setText("Select 1 Line or 2 Points for Horizontal Constraint");

            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
        }
    }

    private void applyVerticalConstraint() {

        List<Entity> selected = interactionManager.getSelectedEntities();

        if (selected.size() == 1 && selected.get(0) instanceof Line) {

            Line line = (Line) selected.get(0);
            Constraint c = new VerticalConstraint(line.getStartPoint(), line.getEndPoint());
            commandManager.executeCommand(new AddConstraintCommand(sketch, c));
            appendOutput("Applied Vertical Constraint to Line.");
            interactionManager.clearSelection();
            glCanvas.repaint();
        } else if (selected.size() == 2 && selected.get(0) instanceof PointEntity
                && selected.get(1) instanceof PointEntity) {

            PointEntity p1 = (PointEntity) selected.get(0);
            PointEntity p2 = (PointEntity) selected.get(1);
            Constraint c = new VerticalConstraint(p1.getPoint(), p2.getPoint());
            commandManager.executeCommand(new AddConstraintCommand(sketch, c));
            appendOutput("Applied Vertical Constraint to Points.");
            interactionManager.clearSelection();
            glCanvas.repaint();
        } else {

            interactionManager.clearSelection();
            interactionManager.setMode(SketchInteractionManager.InteractionMode.SELECT);

            if (glRenderer != null && !glRenderer.isShowingSketch()) {
                glRenderer.setShowSketch(true);
                appendOutput("Switched to 2D sketch mode");
            }

            appendOutput("Vertical Constraint: Click to select 1 Line or 2 Points, then click this button again.");
            if (statusLabel != null)
                statusLabel.setText("Select 1 Line or 2 Points for Vertical Constraint");

            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
        }
    }

    private void applyCoincidentConstraint() {

        List<Entity> selected = interactionManager.getSelectedEntities();

        if (selected.size() >= 2) {

            for (int i = 0; i < selected.size() - 1; i++) {
                Entity e1 = selected.get(i);
                Entity e2 = selected.get(i + 1);
                Constraint c = new CoincidentConstraint(e1, e2);
                commandManager.executeCommand(new AddConstraintCommand(sketch, c));
            }

            if (selected.size() == 2) {
                appendOutput("Applied Coincident Constraint to 2 entities.");
            } else {
                appendOutput("Applied Coincident Constraint to " + selected.size() + " entities.");
            }
            interactionManager.clearSelection();
            glCanvas.repaint();
        } else {

            interactionManager.clearSelection();
            interactionManager.setMode(SketchInteractionManager.InteractionMode.SELECT);

            if (glRenderer != null && !glRenderer.isShowingSketch()) {
                glRenderer.setShowSketch(true);
                appendOutput("Switched to 2D sketch mode");
            }

            appendOutput(
                    "Coincident Constraint: Click to select 2+ entities (Points, Lines, Circles), then click this button again.");
            if (statusLabel != null)
                statusLabel.setText("Select 2+ Entities for Coincident Constraint");

            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
        }
    }

    private void applyFixedConstraint() {

        List<Entity> selected = interactionManager.getSelectedEntities();

        if (!selected.isEmpty()) {
            int count = 0;
            for (Entity e : selected) {
                if (e instanceof PointEntity) {
                    PointEntity p = (PointEntity) e;
                    Constraint c = new FixedConstraint(p.getPoint());
                    commandManager.executeCommand(new AddConstraintCommand(sketch, c));
                    count++;
                }
            }
            if (count > 0) {
                appendOutput("Fixed " + count + " points.");
                interactionManager.clearSelection();
                glCanvas.repaint();
            } else {

                interactionManager.clearSelection();
                interactionManager.setMode(SketchInteractionManager.InteractionMode.SELECT);

                if (glRenderer != null && !glRenderer.isShowingSketch()) {
                    glRenderer.setShowSketch(true);
                    appendOutput("Switched to 2D sketch mode");
                }

                appendOutput("Fixed Constraint: Click to select Points, then click this button again.");
                if (statusLabel != null)
                    statusLabel.setText("Select Points to Fix in Place");

                Platform.runLater(() -> {
                    if (canvasNode != null) {
                        canvasNode.requestFocus();
                    }
                });
            }
        } else {

            interactionManager.setMode(SketchInteractionManager.InteractionMode.SELECT);

            if (glRenderer != null && !glRenderer.isShowingSketch()) {
                glRenderer.setShowSketch(true);
                appendOutput("Switched to 2D sketch mode");
            }

            appendOutput("Fixed Constraint: Click to select Points, then click this button again.");
            if (statusLabel != null)
                statusLabel.setText("Select Points to Fix in Place");

            Platform.runLater(() -> {
                if (canvasNode != null) {
                    canvasNode.requestFocus();
                }
            });
        }
    }

    private void showUnsavedChangesDialog(Stage owner) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes in your sketch.");
        alert.setContentText("Do you want to save your changes before exiting?");

        ButtonType buttonTypeSave = new ButtonType("Save");
        ButtonType buttonTypeDontSave = new ButtonType("Don't Save");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeSave, buttonTypeDontSave, buttonTypeCancel);

        alert.initOwner(owner);
        java.util.Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == buttonTypeSave) {
                // Open native OS file chooser to let user choose save location
                boolean saved = saveBeforeExit();
                if (saved) {
                    // Only exit if save was successful or user completed the operation
                    Platform.exit();
                    System.exit(0);
                }
                // If save was cancelled, don't exit - return to the application
            } else if (result.get() == buttonTypeDontSave) {
                Platform.exit();
                System.exit(0);
            }
            // If Cancel was clicked, do nothing - dialog closes and app stays open
        }
    }

    /**
     * Opens a native file chooser to save the current work before exiting.
     * Uses the OS-native file manager (Nautilus/Dolphin on Linux, File Explorer on
     * Windows, Finder on macOS).
     * 
     * @return true if file was saved successfully, false if cancelled
     */
    private boolean saveBeforeExit() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Your Work");

        // Determine what type of content we have
        boolean has3DModel = (Geometry.getCurrentShape() != Geometry.Shape.NONE &&
                !Geometry.getExtrudedTriangles().isEmpty()) ||
                !Geometry.getLoadedStlTriangles().isEmpty();
        boolean hasSketch = sketch != null && !sketch.getEntities().isEmpty();

        // Set up file filters based on what content exists
        if (has3DModel) {
            FileChooser.ExtensionFilter stlFilter = new FileChooser.ExtensionFilter("3D Model (*.stl)", "*.stl");
            fileChooser.getExtensionFilters().add(stlFilter);
            fileChooser.setSelectedExtensionFilter(stlFilter);
            fileChooser.setInitialFileName("model.stl");
        }

        if (hasSketch) {
            FileChooser.ExtensionFilter dxfFilter = new FileChooser.ExtensionFilter("2D Sketch (*.dxf)", "*.dxf");
            fileChooser.getExtensionFilters().add(dxfFilter);
            if (!has3DModel) {
                fileChooser.setSelectedExtensionFilter(dxfFilter);
                fileChooser.setInitialFileName("sketch.dxf");
            }
        }

        // If no content, default to DXF
        if (!has3DModel && !hasSketch) {
            FileChooser.ExtensionFilter dxfFilter = new FileChooser.ExtensionFilter("2D Sketch (*.dxf)", "*.dxf");
            fileChooser.getExtensionFilters().add(dxfFilter);
            fileChooser.setInitialFileName("sketch.dxf");
        }

        // Open the native OS file chooser dialog
        Window ownerWindow = outputArea.getScene().getWindow();
        File file = fileChooser.showSaveDialog(ownerWindow);

        if (file != null) {
            String filepath = file.getAbsolutePath();

            try {
                // Determine file type and save accordingly
                if (filepath.toLowerCase().endsWith(".stl")) {
                    if (!filepath.toLowerCase().endsWith(".stl")) {
                        filepath += ".stl";
                    }
                    Geometry.saveStl(filepath);
                    appendOutput("3D Model saved to: " + filepath);
                    return true;
                } else {
                    // Default to DXF for sketches
                    if (!filepath.toLowerCase().endsWith(".dxf")) {
                        filepath += ".dxf";
                    }
                    sketch.exportSketchToDXF(filepath);
                    appendOutput("2D Sketch saved to: " + filepath);
                    return true;
                }
            } catch (Exception e) {
                // Show error dialog
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Save Error");
                errorAlert.setHeaderText("Failed to save file");
                errorAlert.setContentText("Error: " + e.getMessage());
                errorAlert.showAndWait();
                appendOutput("Error saving file: " + e.getMessage());
                return false;
            }
        } else {
            // User cancelled the save dialog
            appendOutput("Save cancelled by user.");
            return false;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    private void showRunMacroMenu() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Run Macro");
        alert.setHeaderText("Choose macro source");
        alert.setContentText("Upload a macro file or select from your macro library.");

        ButtonType btnLibrary = new ButtonType("Select from Library");
        ButtonType btnUpload = new ButtonType("Upload Macro File");
        ButtonType btnCancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnLibrary, btnUpload, btnCancel);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == btnLibrary) {
                macroManager.selectAndRunMacro();
            } else if (result.get() == btnUpload) {
                macroManager.uploadAndRunMacro();
            }
        }
    }

    public void fitSketchView() {
        if (sketch == null)
            return;
        java.util.List<cad.core.Sketch.Entity> entities = sketch.getEntities();
        if (entities.isEmpty()) {
            sketch2DPanX = 0;
            sketch2DPanY = 0;
            sketch2DZoom = 1.0f;
            if (glCanvas != null)
                glCanvas.repaint();
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (cad.core.Sketch.Entity entity : entities) {
            if (entity instanceof cad.core.Sketch.PointEntity) {
                cad.core.Sketch.PointEntity p = (cad.core.Sketch.PointEntity) entity;
                minX = Math.min(minX, p.getX());
                minY = Math.min(minY, p.getY());
                maxX = Math.max(maxX, p.getX());
                maxY = Math.max(maxY, p.getY());
            } else if (entity instanceof cad.core.Sketch.Line) {
                cad.core.Sketch.Line l = (cad.core.Sketch.Line) entity;
                minX = Math.min(minX, Math.min(l.getX1(), l.getX2()));
                minY = Math.min(minY, Math.min(l.getY1(), l.getY2()));
                maxX = Math.max(maxX, Math.max(l.getX1(), l.getX2()));
                maxY = Math.max(maxY, Math.max(l.getY1(), l.getY2()));
            } else if (entity instanceof cad.core.Sketch.Circle) {
                cad.core.Sketch.Circle c = (cad.core.Sketch.Circle) entity;
                minX = Math.min(minX, c.getX() - c.getRadius());
                minY = Math.min(minY, c.getY() - c.getRadius());
                maxX = Math.max(maxX, c.getX() + c.getRadius());
                maxY = Math.max(maxY, c.getY() + c.getRadius());
            } else if (entity instanceof cad.core.Sketch.Polygon) {
                cad.core.Sketch.Polygon poly = (cad.core.Sketch.Polygon) entity;
                for (cad.core.Sketch.Point2D p : poly.getPoints()) {
                    minX = Math.min(minX, p.getX());
                    minY = Math.min(minY, p.getY());
                    maxX = Math.max(maxX, p.getX());
                    maxY = Math.max(maxY, p.getY());
                }
            }
        }

        if (minX == Float.MAX_VALUE)
            return;

        float width = maxX - minX;
        float height = maxY - minY;

        width *= 1.2f;
        height *= 1.2f;

        if (width < 1.0f)
            width = 1.0f;
        if (height < 1.0f)
            height = 1.0f;

        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;

        sketch2DPanX = -centerX;
        sketch2DPanY = -centerY;

        float zoomX = 600.0f / width;
        float zoomY = 400.0f / height;
        sketch2DZoom = Math.min(zoomX, zoomY);

        if (sketch2DZoom < 0.1f)
            sketch2DZoom = 0.1f;
        if (sketch2DZoom > 50.0f)
            sketch2DZoom = 50.0f;

        if (glCanvas != null) {
            javax.swing.SwingUtilities.invokeLater(() -> glCanvas.repaint());
        }
        appendOutput(String.format("Fit view to sketch bounds: [%.1f, %.1f] x [%.1f, %.1f]", minX, maxX, minY, maxY));
    }
}
