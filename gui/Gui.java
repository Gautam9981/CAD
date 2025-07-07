package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import core.Sketch;
import core.Geometry;

/**
 * Main GUI class for the CAD application.
 * Provides user interface elements for creating and manipulating 3D shapes,
 * managing files, and sketching 2D geometry.
 */
public class Gui extends JFrame {
    // Output text area for displaying logs, errors, and status messages
    private static JTextArea outputArea;

    // Input fields for various commands and parameters
    private JTextField cubeSizeField, sphereRadiusField, fileField;
    private JTextField latField, lonField;
    private JTextField sketchPointX, sketchPointY;
    private JTextField sketchLineX1, sketchLineY1, sketchLineX2, sketchLineY2;
    private JTextField sketchCircleX, sketchCircleY, sketchCircleR;
    private JTextField sketchPolygonX, sketchPolygonY, sketchPolygonR, sketchPolygonSides;

    // Canvas for rendering sketches
    private SketchCanvas canvas;

    // Unit selector combo box (e.g. mm, cm, m, in, ft)
    private JComboBox<String> unitSelector = new JComboBox<>(new String[] { "mm", "cm", "m", "in", "ft" });

    // Static fields controlling subdivision counts for sphere and cube
    public static int sphereLatDiv = 10;
    public static int sphereLonDiv = 10;
    public static int cubeDivisions = 10;

    // Sketch object containing geometric entities
    public static Sketch sketch;

    /**
     * Constructs the GUI, initializes components and layouts,
     * and sets up event listeners.
     */
    public Gui() {
        // Initialize sketch and canvas
        sketch = new Sketch();
        canvas = new SketchCanvas(sketch);
        canvas.setPreferredSize(new Dimension(800, 800));

        // JFrame properties
        setTitle("CAD GUI");
        setSize(1920, 1080);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Output area to display messages (non-editable)
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Command panel holding all buttons in a vertical grid layout
        JPanel commandPanel = new JPanel(new GridLayout(0, 1, 5, 5));

        // Add buttons with their corresponding action listeners
        addButton(commandPanel, "Help", e -> help());
        addButton(commandPanel, "Version", e -> appendOutput("CAD System version 1.0"));
        addButton(commandPanel, "Exit", e -> System.exit(0));
        addButton(commandPanel, "Create Cube", e -> createCube(new String[] { cubeSizeField.getText() }));
        addButton(commandPanel, "Create Sphere", e -> createSphere(new String[] { sphereRadiusField.getText() }));
        addButton(commandPanel, "Save File", e -> saveFile(new String[] { fileField.getText() }));
        addButton(commandPanel, "Load File", e -> loadFile(new String[] { fileField.getText() }));
        addButton(commandPanel, "Set Cube Div", e -> setCubeDivisions(new String[] { cubeSizeField.getText() }));
        addButton(commandPanel, "Set Sphere Div", e -> setSphereDivisions(new String[] { latField.getText(), lonField.getText() }));
        addButton(commandPanel, "Export DXF", e -> exportDXF(new String[] { fileField.getText() }));
        addButton(commandPanel, "Sketch Clear", e -> sketchClear());
        addButton(commandPanel, "Sketch List", e -> sketchList());
        addButton(commandPanel, "Sketch Point", e -> sketchPoint(new String[] { sketchPointX.getText(), sketchPointY.getText() }));
        addButton(commandPanel, "Sketch Line", e -> sketchLine(new String[] {
            sketchLineX1.getText(), sketchLineY1.getText(), sketchLineX2.getText(), sketchLineY2.getText()
        }));
        addButton(commandPanel, "Sketch Circle", e -> sketchCircle(new String[] {
            sketchCircleX.getText(), sketchCircleY.getText(), sketchCircleR.getText()
        }));
        addButton(commandPanel, "Sketch Polygon", e -> sketchPolygon(new String[] {
            sketchPolygonX.getText(), sketchPolygonY.getText(), sketchPolygonR.getText(), sketchPolygonSides.getText()
        }));

        // Input panel with labels and corresponding text fields in two-column grid
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        // Initialize input fields
        cubeSizeField = new JTextField();
        sphereRadiusField = new JTextField();
        fileField = new JTextField();
        latField = new JTextField();
        lonField = new JTextField();
        sketchPointX = new JTextField();
        sketchPointY = new JTextField();
        sketchLineX1 = new JTextField();
        sketchLineY1 = new JTextField();
        sketchLineX2 = new JTextField();
        sketchLineY2 = new JTextField();
        sketchCircleX = new JTextField();
        sketchCircleY = new JTextField();
        sketchCircleR = new JTextField();
        sketchPolygonX = new JTextField();
        sketchPolygonY = new JTextField();
        sketchPolygonR = new JTextField();
        sketchPolygonSides = new JTextField();

        // Add labels and fields to the input panel
        inputPanel.add(new JLabel("Cube Size:")); inputPanel.add(cubeSizeField);
        inputPanel.add(new JLabel("Sphere Radius:")); inputPanel.add(sphereRadiusField);
        inputPanel.add(new JLabel("File Name:")); inputPanel.add(fileField);
        inputPanel.add(new JLabel("Sphere Lat Div:")); inputPanel.add(latField);
        inputPanel.add(new JLabel("Sphere Lon Div:")); inputPanel.add(lonField);
        inputPanel.add(new JLabel("Sketch Point X:")); inputPanel.add(sketchPointX);
        inputPanel.add(new JLabel("Sketch Point Y:")); inputPanel.add(sketchPointY);
        inputPanel.add(new JLabel("Line X1:")); inputPanel.add(sketchLineX1);
        inputPanel.add(new JLabel("Line Y1:")); inputPanel.add(sketchLineY1);
        inputPanel.add(new JLabel("Line X2:")); inputPanel.add(sketchLineX2);
        inputPanel.add(new JLabel("Line Y2:")); inputPanel.add(sketchLineY2);
        inputPanel.add(new JLabel("Circle X:")); inputPanel.add(sketchCircleX);
        inputPanel.add(new JLabel("Circle Y:")); inputPanel.add(sketchCircleY);
        inputPanel.add(new JLabel("Circle Radius:")); inputPanel.add(sketchCircleR);
        inputPanel.add(new JLabel("Polygon Center X:")); inputPanel.add(sketchPolygonX);
        inputPanel.add(new JLabel("Polygon Center Y:")); inputPanel.add(sketchPolygonY);
        inputPanel.add(new JLabel("Polygon Radius:")); inputPanel.add(sketchPolygonR);
        inputPanel.add(new JLabel("Polygon Sides (3-25):")); inputPanel.add(sketchPolygonSides);
        inputPanel.add(new JLabel("Units:")); inputPanel.add(unitSelector);

        // Top panel contains commands (buttons) and inputs side by side
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(commandPanel), BorderLayout.WEST);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        // Center panel contains the canvas and output area (scroll pane)
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(canvas, BorderLayout.CENTER);
        centerPanel.add(scrollPane, BorderLayout.SOUTH);

        // Add main panels to the JFrame content pane
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Displays help information in the output area.
     */
    private static void help() {
        appendOutput("Available commands:");
        appendOutput("  cube <size>");
        appendOutput("  sphere <radius>");
        appendOutput("  save <filename>");
        appendOutput("  load <filename>");
        appendOutput("  cube_div <count>");
        appendOutput("  sphere_div <lat> <lon>");
        appendOutput("  sketch_point <x> <y>");
        appendOutput("  sketch_line <x1> <y1> <x2> <y2>");
        appendOutput("  sketch_circle <x> <y> <r>");
        appendOutput("  sketch_polygon <x> <y> <radius> <sides>");
        appendOutput("  export_dxf <filename>");
    }

    /**
     * Helper method to create and add a button with a click listener to a panel.
     */
    private static void addButton(JPanel panel, String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        panel.add(button);
    }

    /**
     * Append text to the output text area with a newline.
     */
    private static void appendOutput(String text) {
        outputArea.append(text + "\n");
    }

    /**
     * Creates a cube of specified size and subdivision.
     */
    private static void createCube(String[] args) {
        if (args.length < 1) {
            appendOutput("Usage: cube <size>");
            return;
        }
        try {
            float size = Float.parseFloat(args[0]);
            Geometry.createCube(size, cubeDivisions);
            appendOutput("Cube created with size " + size);
        } catch (NumberFormatException e) {
            appendOutput("Invalid size value.");
        } catch (IllegalArgumentException e) {
            appendOutput(e.getMessage());
        }
    }

    /**
     * Creates a sphere of specified radius and subdivision.
     */
    private static void createSphere(String[] args) {
        if (args.length < 1) {
            appendOutput("Usage: sphere <radius>");
            return;
        }
        try {
            float radius = Float.parseFloat(args[0]);
            int maxDiv = Math.max(sphereLatDiv, sphereLonDiv);
            Geometry.createSphere(radius, maxDiv);
            appendOutput("Sphere created with radius " + radius);
        } catch (NumberFormatException e) {
            appendOutput("Invalid radius value.");
        } catch (IllegalArgumentException e) {
            appendOutput(e.getMessage());
        }
    }

    /**
     * Sets the number of subdivisions for the cube.
     */
    private static void setCubeDivisions(String[] args) {
        if (args.length < 1) {
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
            appendOutput("Invalid count value.");
        }
    }

    /**
     * Sets the latitude and longitude subdivisions for the sphere.
     */
    private static void setSphereDivisions(String[] args) {
        if (args.length < 2) {
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
            appendOutput("Invalid subdivision values.");
        }
    }

    /**
     * Saves the current geometry to a file.
     */
    private static void saveFile(String[] args) {
        if (args.length < 1) {
            appendOutput("Usage: save <filename>");
            return;
        }
        String filename = args[0];
        try {
            Geometry.saveStl(filename);
            appendOutput("File saved: " + filename);
        } catch (Exception e) {
            appendOutput("Error saving file: " + e.getMessage());
        }
    }

    /**
     * Loads geometry or sketch data from a file, or opens a file chooser dialog if no filename is provided.
     */
    private void loadFile(String[] args) {
        String filename = null;

        // If no filename provided, open a file chooser dialog
        if (args.length < 1 || args[0].trim().isEmpty()) {
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                filename = fileChooser.getSelectedFile().getAbsolutePath();
            } else {
                appendOutput("File loading canceled.");
                return;
            }
        } else {
            filename = args[0];
        }

        String lowerFilename = filename.toLowerCase();
        try {
            if (lowerFilename.endsWith(".stl")) {
                Geometry.loadStl(filename);
                appendOutput("STL file loaded: " + filename);
            } else if (lowerFilename.endsWith(".dxf")) {
                sketch.loadDXF(filename);
                appendOutput("DXF file loaded: " + filename);
            } else {
                appendOutput("Unsupported file format.");
                return;
            }
            // Repaint canvas after loading new geometry
            canvas.repaint();
        } catch (Exception e) {
            appendOutput("Error loading file: " + e.getMessage());
        }
    }

    /**
     * Exports the current sketch to a DXF file with selected units.
     */
    private void exportDXF(String[] args) {
        if (args.length < 1) {
            appendOutput("Usage: export_dxf <filename>");
            return;
        }
        String filename = args[0];
        String unit = (String) unitSelector.getSelectedItem();

        sketch.setUnits(unit);

        try {
            sketch.exportSketchToDXF(filename);
            appendOutput("Sketch exported to " + filename + " with units: " + unit);
        } catch (Exception e) {
            appendOutput("Error exporting DXF: " + e.getMessage());
        }
    }

    /**
     * Clears the current sketch.
     */
    private static void sketchClear() {
        sketch.clearSketch();
        appendOutput("Sketch cleared.");
    }

    /**
     * Lists the current sketch entities (likely logs to console or output).
     */
    private static void sketchList() {
        sketch.listSketch();
    }

    /**
     * Adds a point to the sketch using input arguments.
     */
    private static void sketchPoint(String[] args) {
        if (args.length < 2) {
            appendOutput("Usage: sketch_point <x> <y>");
            return;
        }
        if (sketch.sketchPoint(args) == 0) {
            appendOutput("Point added to sketch.");
        }
    }

    /**
     * Adds a line to the sketch.
     */
    private static void sketchLine(String[] args) {
        if (args.length < 4) {
            appendOutput("Usage: sketch_line <x1> <y1> <x2> <y2>");
            return;
        }
        if (sketch.sketchLine(args) == 0) {
            appendOutput("Line added to sketch.");
        }
    }

    /**
     * Adds a circle to the sketch.
     */
    private static void sketchCircle(String[] args) {
        if (args.length < 3) {
            appendOutput("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        if (sketch.sketchCircle(args) == 0) {
            appendOutput("Circle added to sketch.");
        }
    }

    /**
     * Adds a polygon to the sketch.
     */
    private static void sketchPolygon(String[] args) {
        if (args.length < 4) {
            appendOutput("Usage: sketch_polygon <x> <y> <radius> <sides>");
            return;
        }
        if (sketch.sketchPolygon(Float.parseFloat((args[0])), Float.parseFloat(args[1]), Float.parseFloat(args[2]), Integer.parseInt(args[3])) == 0) {
            appendOutput("Polygon added to sketch.");
        }
    }

    /**
     * Main method to run the GUI.
     */
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui();
            gui.setVisible(true);
        });
    }
}
