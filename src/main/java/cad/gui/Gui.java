package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import core.Geometry;
import core.Sketch;

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
    public static Sketch sketch; // Declared static as per original code

    // === Constructor: builds the GUI ===
    public Gui() {
        // Set the Look and Feel FIRST for a modern appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Failed to initialize Look and Feel: " + ex);
        }

        sketch = new Sketch();
        canvas = new SketchCanvas(sketch);

        setTitle("CAD GUI");
        setSize(1920, 1080); // Full HD size
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        // --- Output area (console-like text area) ---
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Use a monospaced font for console output
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Console Output")); // Add a titled border

        // --- Command buttons panel (vertical list of buttons) ---
        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new GridLayout(0, 1, 5, 5)); // Still good for vertical buttons
        commandPanel.setBorder(BorderFactory.createTitledBorder("Commands")); // Add a titled border
        addButtons(commandPanel); // Populate buttons

        // Wrap the commandPanel in a JScrollPane to make it scrollable
        JScrollPane commandScrollPane = new JScrollPane(commandPanel);
        commandScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        commandScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // No horizontal scroll for vertical buttons
        commandScrollPane.setPreferredSize(new Dimension(200, 0)); // Give it a preferred width, height managed by layout

        // --- Input fields panel (label + input) ---
        // Use GridBagLayout for more controlled form layout and better grouping
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Parameters")); // Add a titled border
        initializeInputFields(); // Initialize all JTextFields
        addInputFields(inputPanel); // Add labeled inputs using GridBagLayout

        // --- Left Control Panel (Commands + Inputs) ---
        // Combines command buttons and input fields side by side
        JPanel leftControlPanel = new JPanel(new BorderLayout(10, 0)); // Add horizontal gap between components
        leftControlPanel.add(commandScrollPane, BorderLayout.WEST); // Add the scrollable command panel
        leftControlPanel.add(inputPanel, BorderLayout.CENTER);
        leftControlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Add padding around the control panel

        // --- Main Content Horizontal Split Pane ---
        // Left side will have the combined command/input panels
        // Right side will have the canvas
        JSplitPane mainHorizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftControlPanel, canvas);
        mainHorizontalSplitPane.setResizeWeight(0.7); // Give leftControlPanel more space initially (e.g., 70% for controls, 30% for canvas)
        mainHorizontalSplitPane.setOneTouchExpandable(true); // Adds a quick expand/collapse button
        // Set initial divider location on component visibility
        SwingUtilities.invokeLater(() -> {
            mainHorizontalSplitPane.setDividerLocation(mainHorizontalSplitPane.getWidth() * 2 / 3);
        });

        // --- Vertical Split Pane for Main Content and Console ---
        // Top component is the mainHorizontalSplitPane
        // Bottom component is the console output scrollPane
        JSplitPane mainVerticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainHorizontalSplitPane, scrollPane);
        mainVerticalSplitPane.setResizeWeight(0.8); // Give mainHorizontalSplitPane more space (e.g., 80% for drawing/controls, 20% for console)
        mainVerticalSplitPane.setOneTouchExpandable(true); // Adds a quick expand/collapse button
        // Set initial divider location on component visibility
        SwingUtilities.invokeLater(() -> {
            mainVerticalSplitPane.setDividerLocation(mainVerticalSplitPane.getHeight() - 150); // Set console to initial 150px height
        });

        // --- Add the main vertical split pane to the frame ---
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(mainVerticalSplitPane, BorderLayout.CENTER);
    }

    // ... (rest of your Gui class methods remain unchanged) ...

    // === Helper method: Add buttons and their actions ===
    private void addButtons(JPanel panel) {
        // General Commands
        addButton(panel, "Help", e -> help());
        addButton(panel, "Version", e -> appendOutput("CAD System version 1.0"));
        addButton(panel, "Exit", e -> System.exit(0));
        panel.add(new JSeparator()); // Visual separator

        // File Operations
        addButton(panel, "Save File", e -> saveFile(new String[] { fileField.getText() }));
        addButton(panel, "Load File", e -> loadFile(new String[] { fileField.getText() }));
        addButton(panel, "Export DXF", e -> exportDXF(new String[] { fileField.getText() }));
        panel.add(new JSeparator());

        // 3D Model Commands
        addButton(panel, "Create Cube", e -> createCube(new String[] { cubeSizeField.getText() }));
        addButton(panel, "Set Cube Div", e -> setCubeDivisions(new String[] { cubeSizeField.getText() }));
        panel.add(new JSeparator());
        addButton(panel, "Create Sphere", e -> createSphere(new String[] { sphereRadiusField.getText() }));
        addButton(panel, "Set Sphere Div", e -> setSphereDivisions(new String[] { latField.getText(), lonField.getText() }));
        panel.add(new JSeparator());

        // 2D Sketching Commands
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

    // Initialize all input text fields with a preferred column width
    private void initializeInputFields() {
        cubeSizeField = new JTextField(10);
        sphereRadiusField = new JTextField(10);
        fileField = new JTextField(15);
        latField = new JTextField(5);
        lonField = new JTextField(5);
        sketchPointX = new JTextField(5);
        sketchPointY = new JTextField(5);
        sketchLineX1 = new JTextField(5);
        sketchLineY1 = new JTextField(5);
        sketchLineX2 = new JTextField(5);
        sketchLineY2 = new JTextField(5);
        sketchCircleX = new JTextField(5);
        sketchCircleY = new JTextField(5);
        sketchCircleR = new JTextField(5);
        sketchPolygonX = new JTextField(5);
        sketchPolygonY = new JTextField(5);
        sketchPolygonR = new JTextField(5);
        sketchPolygonSides = new JTextField(5);
        // unitSelector is already initialized as an instance variable
    }

    // Add labeled inputs to the input panel using GridBagLayout for better control and grouping
    private void addInputFields(JPanel inputPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4); // Padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL; // Stretch components horizontally

        int row = 0;

        // Group 3D Object Parameters
        addSectionHeader(inputPanel, "3D Object Parameters", row++);

        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Cube Size:"), gbc);
        gbc.gridx = 1; inputPanel.add(cubeSizeField, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Sphere Radius:"), gbc);
        gbc.gridx = 1; inputPanel.add(sphereRadiusField, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Sphere Lat Div:"), gbc);
        gbc.gridx = 1; inputPanel.add(latField, gbc);

        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Sphere Lon Div:"), gbc);
        gbc.gridx = 1; inputPanel.add(lonField, gbc);

        // Separator
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2; // Span across two columns
        inputPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1; // Reset gridwidth

        // Group File Operations
        addSectionHeader(inputPanel, "File Operations", row++);

        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("File Name:"), gbc);
        gbc.gridx = 1; inputPanel.add(fileField, gbc);

        // Separator
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        inputPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Group Sketching Parameters
        addSectionHeader(inputPanel, "2D Sketching Parameters", row++);

        // Sketch Point
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Point X:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchPointX, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Point Y:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchPointY, gbc);

        // Sketch Line
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Line X1:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchLineX1, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Line Y1:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchLineY1, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Line X2:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchLineX2, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Line Y2:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchLineY2, gbc);

        // Sketch Circle
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Circle X:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchCircleX, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Circle Y:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchCircleY, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Circle Radius:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchCircleR, gbc);

        // Sketch Polygon
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Polygon Center X:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchPolygonX, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Polygon Center Y:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchPolygonY, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Polygon Radius:"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchPolygonR, gbc);
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Polygon Sides (3-25):"), gbc);
        gbc.gridx = 1; inputPanel.add(sketchPolygonSides, gbc);

        // Separator
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        inputPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        // Units Selector
        gbc.gridy = row++;
        gbc.gridx = 0; inputPanel.add(new JLabel("Units: "), gbc);
        gbc.gridx = 1; inputPanel.add(unitSelector, gbc);

        // Add some vertical glue at the bottom to push components to the top
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0; // This makes the last row absorb extra vertical space
        inputPanel.add(Box.createVerticalGlue(), gbc);
    }

    // Helper method to add section headers in GridBagLayout
    private void addSectionHeader(JPanel panel, String title, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2; // Span across two columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 5, 0); // Padding around header

        JLabel headerLabel = new JLabel(title);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD, headerLabel.getFont().getSize() + 2));
        panel.add(headerLabel, gbc);
    }

    // === Output related methods ===
    private static void appendOutput(String text) {
        outputArea.append(text + "\n");
        outputArea.setCaretPosition(outputArea.getDocument().getLength()); // Auto-scroll to bottom
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

    private static void createCube(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: cube <size>");
            return;
        }
        try {
            float size = Float.parseFloat(args[0]);
            Geometry.createCube(size, cubeDivisions);
            appendOutput("Cube created with size " + size);
        } catch (NumberFormatException e) {
            appendOutput("Invalid size value. Please enter a number.");
        } catch (IllegalArgumentException e) {
            appendOutput("Error creating cube: " + e.getMessage());
        }
    }

    private static void createSphere(String[] args) {
        if (args.length < 1 || args[0].trim().isEmpty()) {
            appendOutput("Usage: sphere <radius>");
            return;
        }
        try {
            float radius = Float.parseFloat(args[0]);
            int maxDiv = Math.max(sphereLatDiv, sphereLonDiv); // Using class static fields
            Geometry.createSphere(radius, maxDiv);
            appendOutput("Sphere created with radius " + radius);
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

    private void loadFile(String[] args) {
        String filename = null;

        if (args.length < 1 || args[0].trim().isEmpty()) {
            // No filename provided — open file picker dialog
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
            // Filename provided in args
            filename = args[0].trim();
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
                appendOutput("Unsupported file format. Please use .stl or .dxf.");
                return;
            }
            canvas.repaint(); // Repaint canvas after loading new geometry/sketch
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
            canvas.repaint();
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
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid coordinates. Please enter numbers for all coordinates.");
        }
    }

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
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, and Radius.");
        }
    }

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
            canvas.repaint();
        } catch (NumberFormatException e) {
            appendOutput("Invalid input. Please enter numbers for X, Y, Radius, and an integer for Sides.");
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