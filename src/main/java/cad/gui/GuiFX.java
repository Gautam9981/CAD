package cad.gui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingNode;

import javax.swing.SwingUtilities;
import java.awt.*;
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
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.glu.GLU;

import cad.core.Geometry;
import cad.core.Sketch;

public class GuiFX extends Application {
    
    // === UI Components ===
    private TextArea outputArea;
    private TextField cubeSizeField, sphereRadiusField, fileField, latField, lonField;
    private TextField sketchPointX, sketchPointY;
    private TextField sketchLineX1, sketchLineY1, sketchLineX2, sketchLineY2;
    private TextField sketchCircleX, sketchCircleY, sketchCircleR;
    private TextField sketchPolygonX, sketchPolygonY, sketchPolygonR, sketchPolygonSides;
    private ComboBox<String> unitSelector;
    private GLCanvas glCanvas;
    private SwingNode canvasNode;
    private FPSAnimator animator;
    
    // === Static fields for configuration and core objects ===
    public static int sphereLatDiv = 10;
    public static int sphereLonDiv = 10;
    public static int cubeDivisions = 10;
    public static Sketch sketch;
    
    // === OpenGL rendering variables ===
    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float zoom = -5.0f;
    private int lastMouseX, lastMouseY;
    private boolean isDragging = false;
    private List<float[]> stlTriangles;
    private boolean showSketch = false;
    
    @Override
    public void start(Stage primaryStage) {
        sketch = new Sketch();
        
        primaryStage.setTitle("CAD GUI - JavaFX with JOGL");
        
        // Initialize UI components
        initializeComponents();
        
        // Create main layout
        BorderPane root = new BorderPane();
        
        // Create split panes
        SplitPane mainSplitPane = createMainSplitPane();
        SplitPane verticalSplitPane = new SplitPane();
        verticalSplitPane.setOrientation(Orientation.VERTICAL);
        verticalSplitPane.getItems().addAll(mainSplitPane, createConsolePane());
        verticalSplitPane.setDividerPositions(0.8);
        
        root.setCenter(verticalSplitPane);
        
        // Create scene
        Scene scene = new Scene(root, 1920, 1080);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (animator != null) {
                animator.stop();
            }
            Platform.exit();
            System.exit(0);
        });
        
        primaryStage.show();
        
        // Start OpenGL animation
        if (animator != null) {
            animator.start();
        }
    }
    
    private void initializeComponents() {
        // Initialize text fields
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
        
        // Initialize combo box
        unitSelector = new ComboBox<>();
        unitSelector.getItems().addAll("mm", "cm", "m", "in", "ft");
        unitSelector.setValue("mm");
        
        // Initialize output area
        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setStyle("-fx-font-family: 'Courier New', monospace;");
        
        // Initialize OpenGL canvas
        initializeGLCanvas();
    }
    
    private void initializeGLCanvas() {
        SwingUtilities.invokeLater(() -> {
            GLProfile profile = GLProfile.get(GLProfile.GL2);
            GLCapabilities capabilities = new GLCapabilities(profile);
            capabilities.setDoubleBuffered(true);
            capabilities.setHardwareAccelerated(true);
            
            glCanvas = new GLCanvas(capabilities);
            glCanvas.setPreferredSize(new Dimension(800, 600));
            glCanvas.addGLEventListener(new OpenGLRenderer());
            glCanvas.addMouseListener(new CanvasMouseListener());
            glCanvas.addMouseMotionListener(new CanvasMouseMotionListener());
            glCanvas.addMouseWheelListener(new CanvasMouseWheelListener());
            glCanvas.addKeyListener(new CanvasKeyListener());
            glCanvas.setFocusable(true);
            
            animator = new FPSAnimator(glCanvas, 60);
            
            Platform.runLater(() -> {
                canvasNode = new SwingNode();
                canvasNode.setContent(glCanvas);
            });
        });
    }
    
    private SplitPane createMainSplitPane() {
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        
        // Left panel with controls
        VBox leftPanel = createControlPanel();
        
        // Right panel with 3D canvas
        VBox rightPanel = new VBox();
        rightPanel.setPadding(new Insets(10));
        
        // Wait for canvas to be initialized
        Task<Void> canvasTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while (canvasNode == null) {
                    Thread.sleep(50);
                }
                return null;
            }
        };
        
        canvasTask.setOnSucceeded(e -> {
            rightPanel.getChildren().add(canvasNode);
            VBox.setVgrow(canvasNode, Priority.ALWAYS);
        });
        
        Thread canvasThread = new Thread(canvasTask);
        canvasThread.setDaemon(true);
        canvasThread.start();
        
        splitPane.getItems().addAll(leftPanel, rightPanel);
        splitPane.setDividerPositions(0.7);
        
        return splitPane;
    }
    
    private VBox createControlPanel() {
        VBox controlPanel = new VBox(10);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setMinWidth(400);
        
        // Commands section
        TitledPane commandsPane = new TitledPane("Commands", createCommandsPane());
        commandsPane.setCollapsible(false);
        
        // Parameters section
        TitledPane parametersPane = new TitledPane("Parameters", createParametersPane());
        parametersPane.setCollapsible(false);
        
        controlPanel.getChildren().addAll(commandsPane, parametersPane);
        
        return controlPanel;
    }
    
    private ScrollPane createCommandsPane() {
        VBox commandsBox = new VBox(5);
        commandsBox.setPadding(new Insets(10));
        
        // General Commands
        commandsBox.getChildren().addAll(
            createSectionLabel("General Commands"),
            createButton("Help", e -> help()),
            createButton("Version", e -> appendOutput("CAD System version 1.0")),
            createButton("Exit", e -> Platform.exit()),
            new Separator()
        );
        
        // File Operations
        commandsBox.getChildren().addAll(
            createSectionLabel("File Operations"),
            createButton("Save File", e -> saveFile()),
            createButton("Load File", e -> loadFile()),
            createButton("Export DXF", e -> exportDXF()),
            new Separator()
        );
        
        // 3D Model Commands
        commandsBox.getChildren().addAll(
            createSectionLabel("3D Model Commands"),
            createButton("Create Cube", e -> createCube()),
            createButton("Set Cube Div", e -> setCubeDivisions()),
            createButton("Create Sphere", e -> createSphere()),
            createButton("Set Sphere Div", e -> setSphereDivisions()),
            new Separator()
        );
        
        // 2D Sketching Commands
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
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300);
        
        return scrollPane;
    }
    
    private ScrollPane createParametersPane() {
        GridPane parametersGrid = new GridPane();
        parametersGrid.setHgap(10);
        parametersGrid.setVgap(5);
        parametersGrid.setPadding(new Insets(10));
        
        int row = 0;
        
        // 3D Object Parameters
        parametersGrid.add(createSectionLabel("3D Object Parameters"), 0, row++, 2, 1);
        parametersGrid.add(new Label("Cube Size:"), 0, row);
        parametersGrid.add(cubeSizeField, 1, row++);
        parametersGrid.add(new Label("Sphere Radius:"), 0, row);
        parametersGrid.add(sphereRadiusField, 1, row++);
        parametersGrid.add(new Label("Sphere Lat Div:"), 0, row);
        parametersGrid.add(latField, 1, row++);
        parametersGrid.add(new Label("Sphere Lon Div:"), 0, row);
        parametersGrid.add(lonField, 1, row++);
        
        parametersGrid.add(new Separator(), 0, row++, 2, 1);
        
        // File Operations
        parametersGrid.add(createSectionLabel("File Operations"), 0, row++, 2, 1);
        parametersGrid.add(new Label("File Name:"), 0, row);
        parametersGrid.add(fileField, 1, row++);
        
        parametersGrid.add(new Separator(), 0, row++, 2, 1);
        
        // 2D Sketching Parameters
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
        scrollPane.setPrefHeight(400);
        
        return scrollPane;
    }
    
    private TitledPane createConsolePane() {
        TitledPane consolePane = new TitledPane("Console Output", outputArea);
        consolePane.setCollapsible(false);
        consolePane.setPrefHeight(150);
        
        return consolePane;
    }
    
    private Label createSectionLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }
    
    private Button createButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button button = new Button(text);
        button.setOnAction(handler);
        button.setMaxWidth(Double.MAX_VALUE);
        return button;
    }
    
    // === OpenGL Renderer ===
    private class OpenGLRenderer implements GLEventListener {
        private GLU glu = new GLU();
        
        @Override
        public void init(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            
            gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnable(GL2.GL_LIGHT0);
            gl.glEnable(GL2.GL_COLOR_MATERIAL);
            
            // Set up lighting
            float[] lightPos = {1.0f, 1.0f, 1.0f, 0.0f};
            float[] lightColor = {1.0f, 1.0f, 1.0f, 1.0f};
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, lightPos, 0);
            gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, lightColor, 0);
        }
        
        @Override
        public void display(GLAutoDrawable drawable) {
            GL2 gl = drawable.getGL().getGL2();
            
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();
            
            // Apply transformations
            gl.glTranslatef(0.0f, 0.0f, zoom);
            gl.glRotatef(rotationX, 1.0f, 0.0f, 0.0f);
            gl.glRotatef(rotationY, 0.0f, 1.0f, 0.0f);
            
            if (showSketch) {
                renderSketch(gl);
            } else if (stlTriangles != null) {
                renderStlTriangles(gl);
            } else {
                renderDefaultCube(gl);
            }
        }
        
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            GL2 gl = drawable.getGL().getGL2();
            
            if (height == 0) height = 1;
            float aspect = (float) width / height;
            
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            glu.gluPerspective(45.0, aspect, 0.1, 100.0);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
        }
        
        @Override
        public void dispose(GLAutoDrawable drawable) {
            // Cleanup resources
        }
        
        private void renderDefaultCube(GL2 gl) {
            gl.glColor3f(0.7f, 0.7f, 0.7f);
            gl.glBegin(GL2.GL_QUADS);
            
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
            
            gl.glEnd();
        }
        
        private void renderStlTriangles(GL2 gl) {
            gl.glColor3f(0.8f, 0.6f, 0.4f);
            gl.glBegin(GL2.GL_TRIANGLES);
            
            for (float[] triangle : stlTriangles) {
                // Normal vector
                gl.glNormal3f(triangle[0], triangle[1], triangle[2]);
                
                // Vertices
                gl.glVertex3f(triangle[3], triangle[4], triangle[5]);
                gl.glVertex3f(triangle[6], triangle[7], triangle[8]);
                gl.glVertex3f(triangle[9], triangle[10], triangle[11]);
            }
            
            gl.glEnd();
        }
        
        private void renderSketch(GL2 gl) {
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glColor3f(0.0f, 1.0f, 0.0f);
            gl.glLineWidth(2.0f);
            
            // Render sketch elements here
            // This would need to be implemented based on the sketch data structure
            
            gl.glEnable(GL2.GL_LIGHTING);
        }
    }
    
    // === Mouse and Keyboard Listeners ===
    private class CanvasMouseListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {}
        
        @Override
        public void mousePressed(MouseEvent e) {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            isDragging = true;
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            isDragging = false;
        }
        
        @Override
        public void mouseEntered(MouseEvent e) {}
        
        @Override
        public void mouseExited(MouseEvent e) {}
    }
    
    private class CanvasMouseMotionListener implements MouseMotionListener {
        @Override
        public void mouseDragged(MouseEvent e) {
            if (isDragging) {
                int deltaX = e.getX() - lastMouseX;
                int deltaY = e.getY() - lastMouseY;
                
                rotationY += deltaX * 0.5f;
                rotationX += deltaY * 0.5f;
                
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                
                glCanvas.repaint();
            }
        }
        
        @Override
        public void mouseMoved(MouseEvent e) {}
    }
    
    private class CanvasMouseWheelListener implements MouseWheelListener {
        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            zoom += e.getWheelRotation() * 0.5f;
            if (zoom > -1.0f) zoom = -1.0f;
            if (zoom < -20.0f) zoom = -20.0f;
            glCanvas.repaint();
        }
    }
    
    private class CanvasKeyListener implements KeyListener {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_W:
                    rotationX -= 5.0f;
                    break;
                case KeyEvent.VK_S:
                    rotationX += 5.0f;
                    break;
                case KeyEvent.VK_A:
                    rotationY -= 5.0f;
                    break;
                case KeyEvent.VK_D:
                    rotationY += 5.0f;
                    break;
                case KeyEvent.VK_Q:
                    zoom += 0.5f;
                    break;
                case KeyEvent.VK_E:
                    zoom -= 0.5f;
                    break;
            }
            glCanvas.repaint();
        }
        
        @Override
        public void keyReleased(KeyEvent e) {}
        
        @Override
        public void keyTyped(KeyEvent e) {}
    }
    
    // === Output related methods ===
    private void appendOutput(String text) {
        Platform.runLater(() -> {
            outputArea.appendText(text + "\n");
            outputArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    // === Command handlers ===

    private static void help() {
        appendOutput("Available commands:");
        appendOutput("   cube <size>");
        appendOutput("   sphere <radius>");
        appendOutput("   save <filename>");
        appendOutput("   load <filename>");
        appendOutput("   cube_div <count>");
        appendOutput("   sphere_div <lat> <lon>");
        appendOutput("   sketch_point <x> <y>");
        appendOutput("   sketch_line <x1> <y1> <x2> <y2>");
        appendOutput("   sketch_circle <x> <y> <r>");
        appendOutput("   sketch_polygon <x> <y> <radius> <sides>");
        appendOutput("   export_dxf <filename>");
    }

    private void createCube(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: cube <size>");
            return;
        }
        try {
            float size = Float.parseFloat(args[0]);
            Geometry.createCube(size, cubeDivisions);
            appendOutput("Cube created with size " + size);
            // New change: After creating the cube, retrieve its triangular data
            // from the Geometry class and pass it to the SketchCanvas for 3D rendering.
            canvas.setStlTriangles(Geometry.getLoadedStlTriangles());
            // Request focus on the canvas to enable immediate 3D navigation (mouse/keyboard).
            canvas.requestFocusInWindow();
        } catch (NumberFormatException e) {
            appendOutput("Invalid size value. Please enter a number.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error creating cube: " + e.getMessage());
        }
    }

    private void createSphere(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: sphere <radius>");
            return;
        }
        try {
            float radius = Float.parseFloat(args[0]);
            int maxDiv = Math.max(sphereLatDiv, sphereLonDiv); // Using class static fields
            Geometry.createSphere(radius, maxDiv);
            appendOutput("Sphere created with radius " + radius);
            // New change: After creating the sphere, retrieve its triangular data
            // from the Geometry class and pass it to the SketchCanvas for 3D rendering.
            canvas.setStlTriangles(Geometry.getLoadedStlTriangles());
            // Request focus on the canvas to enable immediate 3D navigation (mouse/keyboard).
            canvas.requestFocusInWindow();
        } catch (NumberFormatException e) {
            appendOutput("Invalid radius value. Please enter a number.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error creating sphere: " + e.getMessage());
        }
    }

    private static void setCubeDivisions(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: cube_div <count>");
            return;
        }
        try {
            int count = Integer.parseInt(args[0]);
            if (count < 1 || count > 200) {
                appendOutput("Cube subdivisions must be between 1 and 200.");
            } else {
                cubeDivisions = count;
                appendOutput("Cube subdivisions set to " + cubeDivisions);
            }
        } catch (NumberFormatException e) {
            appendOutput("Invalid count value. Please enter an integer.");
        }
    }

    private static void setSphereDivisions(String[] args) {
        if (args.length < 2 || args[0].trim().isEmpty() || args[1].trim().isEmpty()) {
            appendOutput("Usage: sphere_div <lat> <lon>");
            return;
        }
        try {
            int lat = Integer.parseInt(args[0]);
            int lon = Integer.parseInt(args[1]);
            if (lat < 1 || lat > 200 || lon < 1 || lon > 200) {
                appendOutput("Sphere subdivisions must be between 1 and 200.");
            } else {
                sphereLatDiv = lat;
                sphereLonDiv = lon;
                appendOutput("Sphere subdivisions set to " + lat + " lat, " + lon + " lon");
            }
        } catch (NumberFormatException e) {
            appendOutput("Invalid subdivision values. Please enter integers.");
        }
    }

    private static void saveFile(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: save <filename>");
            return;
        }
        String filename = args[0].trim();
        try {
            Geometry.saveStl(filename);
            appendOutput("File saved: " + filename);
        } catch (Exception e) {
            appendOutput("Error saving file: " + e.getMessage());
        }
    }

    // Updated loadFile method for better integration with GUI and SketchCanvas
    private void loadFile(String[] args) {
        String filename = null;

        // If no filename is provided via arguments (e.g., text field is empty), open file picker
        if (args.length < 1 || args[0].trim().isEmpty()) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load CAD File");
            int result = fileChooser.showOpenDialog(this); // Use 'this' for parent component
            if (result == JFileChooser.APPROVE_OPTION) {
                filename = fileChooser.getSelectedFile().getAbsolutePath();
            } else {
                appendOutput("File loading canceled.");
                return;
            }
        } else {
            // Filename provided in args (from text field)
            filename = args[0].trim();
        }

        String lowerFilename = filename.toLowerCase();
        try {
            if (lowerFilename.endsWith(".stl")) {
                // Load STL file and store triangles in Geometry
                Geometry.loadStl(filename);
                appendOutput("STL file loaded: " + filename);
                // Show STL geometry in the canvas (enables 3D navigation)
                canvas.setStlTriangles(Geometry.getLoadedStlTriangles());
                // Request focus so keyboard navigation works immediately
                canvas.requestFocusInWindow();
            } else if (lowerFilename.endsWith(".dxf")) {
                // Load DXF file into the sketch object
                sketch.loadDXF(filename);
                appendOutput("DXF file loaded: " + filename);
                // Switch canvas to sketch (2D) view
                canvas.showSketch();
            } else {
                appendOutput("Unsupported file format. Please use .stl or .dxf.");
                return;
            }
            // Repaint the canvas to show the loaded data (STL or sketch)
            canvas.repaint();
        } catch (Exception e) {
            appendOutput("Error loading file: " + e.getMessage());
        }
    }

    private void exportDXF(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: export_dxf <filename>");
            return;
        }
        String filename = args[0].trim();
        String unit = (String) unitSelector.getSelectedItem();

        sketch.setUnits(unit);
        try {
            sketch.exportSketchToDXF(filename);
            appendOutput("DXF exported to " + filename + " with unit " + unit);
        } catch (Exception e) {
            appendOutput("Error exporting DXF: " + e.getMessage());
        }
    }

    // Sketch commands
    private void sketchClear() {
        sketch.clearSketch();
        appendOutput("Sketch cleared");
        canvas.repaint(); // Repaint canvas after clearing
    }

    private void sketchList() {
        List<String> items = sketch.listSketch();
        if (items.isEmpty()) {
            appendOutput("Sketch is empty");
        } else {
            appendOutput("Sketch contents:");
            for (String item : items) {
                appendOutput("   " + item); // Indent list items for readability
            }
        }
    }

    private void sketchPoint(String[] args) {
        if (args.length < 2 || args[0].trim().isEmpty() || args[1].trim().isEmpty()) {
            appendOutput("Usage: sketch_point <x> <y>");
            return;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            sketch.addPoint(x, y);
            appendOutput("Point added to sketch: (" + x + ", " + y + ")");
            canvas.repaint(); // Repaint canvas after adding element
        } catch (NumberFormatException e) {
            appendOutput("Invalid coordinates. Please enter numbers for X and Y.");
        }
    }

    private void sketchLine(String[] args) {
        if (args.length < 4 || args[0].trim().isEmpty() || args[1].trim().isEmpty() || args[2].trim().isEmpty() || args[3].trim().isEmpty()) {
            appendOutput("Usage: sketch_line <x1> <y1> <x2> <y2>");
            return;
        }
        try {
            float x1 = Float.parseFloat(args[0]);
            float y1 = Float.parseFloat(args[1]);
            float x2 = Float.parseFloat(args[2]);
            float y2 = Float.parseFloat(args[3]);
            sketch.addLine(x1, y1, x2, y2);
            appendOutput("Line added to sketch: (" + x1 + ", " + y1 + ") -> (" + x2 + ", " + y2 + ")");
            canvas.repaint(); // Repaint canvas after adding element
        } catch (NumberFormatException e) {
            appendOutput("Invalid coordinates. Please enter numbers for all coordinates.");
        }
    }

    /**
     * Adds a circle to the current sketch using the provided arguments.
     * Expects args: [x, y, radius].
     * Repaints the canvas after adding.
     */
    private void sketchCircle(String[] args) {
        if (args.length < 3 || args[0].trim().isEmpty() || args[1].trim().isEmpty() || args[2].trim().isEmpty()) {
            appendOutput("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            float r = Float.parseFloat(args[2]);
            sketch.addCircle(x, y, r);
            appendOutput("Circle added to sketch: center (" + x + ", " + y + "), radius " + r);
            canvas.repaint(); // Repaint canvas after adding element
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, and Radius.");
        }
    }

    /**
     * Adds a regular polygon to the current sketch using the provided arguments.
     * Expects args: [x, y, radius, sides].
     * Sides must be between 3 and 25. Repaints the canvas after adding.
     */
    private void sketchPolygon(String[] args) {
        if (args.length < 4 || args[0].trim().isEmpty() || args[1].trim().isEmpty() || args[2].trim().isEmpty() || args[3].trim().isEmpty()) {
            appendOutput("Usage: sketch_polygon <x> <y> <radius> <sides>");
            return;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            float r = Float.parseFloat(args[2]);
            int sides = Integer.parseInt(args[3]);
            if (sides < 3 || sides > 25) {
                appendOutput("Polygon sides must be between 3 and 25.");
                return;
            }
            sketch.sketchPolygon(x, y, r, sides);
            appendOutput("Polygon added: center (" + x + ", " + y + "), radius " + r + ", sides " + sides);
            canvas.repaint(); // Repaint canvas after adding element
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, Radius, and an integer for Sides.");
        }
    }

    /**
     * Launches the CAD GUI application on the Swing event dispatch thread.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui();
            gui.setVisible(true);
        });
    }
}