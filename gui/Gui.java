package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import core.Sketch;
import core.Geometry;
import java.util.List;

public class Gui extends JFrame {
    // === UI Components ===
    private static JTextArea outputArea;
    
    // Input fields for various commands
    private JTextField cubeSizeField, sphereRadiusField, fileField, latField, lonField;
    private JTextField sketchPointX, sketchPointY;
    private JTextField sketchLineX1, sketchLineY1, sketchLineX2, sketchLineY2;
    private JTextField sketchCircleX, sketchCircleY, sketchCircleR;
    private JTextField sketchPolygonX, sketchPolygonY, sketchPolygonR, sketchPolygonSides;

    private SketchCanvas canvas;
    private JComboBox<String> unitSelector = new JComboBox<>(new String[] { "mm", "cm", "m", "in", "ft" });

    // === Static fields for configuration and core objects ===
    public static int sphereLatDiv = 10;
    public static int sphereLonDiv = 10;
    public static int cubeDivisions = 10;
    public static Sketch sketch;

    // === Constructor: builds the GUI ===
    public Gui() {
        sketch = new Sketch();
        canvas = new SketchCanvas(sketch);
        canvas.setPreferredSize(new Dimension(800, 800));

        setTitle("CAD GUI");
        setSize(1920, 1080);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Output area (console-like text area)
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        // Command buttons panel (vertical list of buttons)
        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new GridLayout(0, 1, 5, 5));
        addButtons(commandPanel);

        // Input fields panel (label + input in 2 columns)
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        initializeInputFields();
        addInputFields(inputPanel);

        // Top panel contains commands and inputs side by side
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(commandPanel), BorderLayout.WEST);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        // Center panel contains canvas and output area
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(canvas, BorderLayout.CENTER);
        centerPanel.add(scrollPane, BorderLayout.SOUTH);

        // Add top and center panels to frame
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(centerPanel, BorderLayout.CENTER);
    }

    // === Helper method: Add buttons and their actions ===
    private void addButtons(JPanel panel) {
        addButton(panel, "Help", e -> help());
        addButton(panel, "Version", e -> appendOutput("CAD System version 1.0"));
        addButton(panel, "Exit", e -> System.exit(0));
        addButton(panel, "Create Cube", e -> createCube(new String[] { cubeSizeField.getText() }));
        addButton(panel, "Create Sphere", e -> createSphere(new String[] { sphereRadiusField.getText() }));
        addButton(panel, "Save File", e -> saveFile(new String[] { fileField.getText() }));
        addButton(panel, "Load File", e -> loadFile(new String[] { fileField.getText() }));
        addButton(panel, "Set Cube Div", e -> setCubeDivisions(new String[] { cubeSizeField.getText() }));
        addButton(panel, "Set Sphere Div", e -> setSphereDivisions(new String[] { latField.getText(), lonField.getText() }));
        addButton(panel, "Export DXF", e -> exportDXF(new String[] { fileField.getText() }));
        addButton(panel, "Sketch Clear", e -> sketchClear());
        addButton(panel, "Sketch List", e -> sketchList());
        addButton(panel, "Sketch Point", e -> sketchPoint(new String[] { sketchPointX.getText(), sketchPointY.getText() }));
        addButton(panel, "Sketch Line", e -> sketchLine(new String[] { sketchLineX1.getText(), sketchLineY1.getText(), sketchLineX2.getText(), sketchLineY2.getText() }));
        addButton(panel, "Sketch Circle", e -> sketchCircle(new String[] { sketchCircleX.getText(), sketchCircleY.getText(), sketchCircleR.getText() }));
        addButton(panel, "Sketch Polygon", e -> sketchPolygon(new String[] { sketchPolygonX.getText(), sketchPolygonY.getText(), sketchPolygonR.getText(), sketchPolygonSides.getText() }));
    }

    // Adds a single button to a panel with given label and listener
    private static void addButton(JPanel panel, String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        panel.add(button);
    }

    // Initialize all input text fields
    private void initializeInputFields() {
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
    }

    // Add labeled inputs to the input panel
    private void addInputFields(JPanel inputPanel) {
        inputPanel.add(new JLabel("Cube Size:"));
        inputPanel.add(cubeSizeField);
        inputPanel.add(new JLabel("Sphere Radius:"));
        inputPanel.add(sphereRadiusField);
        inputPanel.add(new JLabel("File Name:"));
        inputPanel.add(fileField);
        inputPanel.add(new JLabel("Sphere Lat Div:"));
        inputPanel.add(latField);
        inputPanel.add(new JLabel("Sphere Lon Div:"));
        inputPanel.add(lonField);
        inputPanel.add(new JLabel("Sketch Point X:"));
        inputPanel.add(sketchPointX);
        inputPanel.add(new JLabel("Sketch Point Y:"));
        inputPanel.add(sketchPointY);
        inputPanel.add(new JLabel("Line X1:"));
        inputPanel.add(sketchLineX1);
        inputPanel.add(new JLabel("Line Y1:"));
        inputPanel.add(sketchLineY1);
        inputPanel.add(new JLabel("Line X2:"));
        inputPanel.add(sketchLineX2);
        inputPanel.add(new JLabel("Line Y2:"));
        inputPanel.add(sketchLineY2);
        inputPanel.add(new JLabel("Circle X:"));
        inputPanel.add(sketchCircleX);
        inputPanel.add(new JLabel("Circle Y:"));
        inputPanel.add(sketchCircleY);
        inputPanel.add(new JLabel("Circle Radius:"));
        inputPanel.add(sketchCircleR);
        inputPanel.add(new JLabel("Polygon Center X:"));
        inputPanel.add(sketchPolygonX);
        inputPanel.add(new JLabel("Polygon Center Y:"));
        inputPanel.add(sketchPolygonY);
        inputPanel.add(new JLabel("Polygon Radius:"));
        inputPanel.add(sketchPolygonR);
        inputPanel.add(new JLabel("Polygon Sides (3-25):"));
        inputPanel.add(sketchPolygonSides);
        inputPanel.add(new JLabel("Units: "));
        inputPanel.add(unitSelector);
    }

    // === Output related methods ===
    private static void appendOutput(String text) {
        outputArea.append(text + "\n");
    }

    // === Command handlers ===

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

    private void loadFile(String[] args) {
        String filename = null;

        if (args.length < 1 || args[0].trim().isEmpty()) {
            // No filename provided — open file picker dialog
            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                filename = fileChooser.getSelectedFile().getAbsolutePath();
            } else {
                appendOutput("File loading canceled.");
                return;
            }
        } else {
            // Filename provided in args
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

            canvas.repaint();
        } catch (Exception e) {
            appendOutput("Error loading file: " + e.getMessage());
        }
    }

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
            appendOutput("DXF exported to " + filename + " with unit " + unit);
        } catch (Exception e) {
            appendOutput("Error exporting DXF: " + e.getMessage());
        }
    }

    // Sketch commands
    private void sketchClear() {
        sketch.clearSketch();
        appendOutput("Sketch cleared");
        canvas.repaint();
    }

    private void sketchList() {
        List<String> items = sketch.listSketch();
        if (items.isEmpty()) {
            appendOutput("Sketch is empty");
        } else {
            appendOutput("Sketch contents:");
            for (String item : items) {
                appendOutput(item);
            }
        }
    }

    private void sketchPoint(String[] args) {
        if (args.length < 2) {
            appendOutput("Usage: sketch_point <x> <y>");
            return;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            sketch.addPoint(x, y);
            appendOutput("Point added to sketch: (" + x + ", " + y + ")");
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid coordinates.");
        }
    }

    private void sketchLine(String[] args) {
        if (args.length < 4) {
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
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid coordinates.");
        }
    }

    private void sketchCircle(String[] args) {
        if (args.length < 3) {
            appendOutput("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        try {
            float x = Float.parseFloat(args[0]);
            float y = Float.parseFloat(args[1]);
            float r = Float.parseFloat(args[2]);
            sketch.addCircle(x, y, r);
            appendOutput("Circle added to sketch: center (" + x + ", " + y + "), radius " + r);
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input.");
        }
    }

    private void sketchPolygon(String[] args) {
        if (args.length < 4) {
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
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input.");
        }
    }

    // === Main method to start the app ===
    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui();
            gui.setVisible(true);
        });
    }
}
