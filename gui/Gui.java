package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import core.Sketch;
import core.Geometry;
import java.util.List;
import java.util.ArrayList;

public class Gui extends JFrame {
    private static JTextArea outputArea;
    private JTextField cubeSizeField, sphereRadiusField, fileField, latField, lonField;
    private JTextField sketchPointX, sketchPointY;
    private JTextField sketchLineX1, sketchLineY1, sketchLineX2, sketchLineY2;
    private JTextField sketchCircleX, sketchCircleY, sketchCircleR;
    private JTextField sketchPolygonX, sketchPolygonY, sketchPolygonR, sketchPolygonSides;

    public static int sphereLatDiv = 10;
    public static int sphereLonDiv = 10;
    public static int cubeDivisions = 10;
    public static Sketch sketch;

    public Gui() {
        sketch = new Sketch();
        setTitle("CAD GUI");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);

        JPanel commandPanel = new JPanel();
        commandPanel.setLayout(new GridLayout(0, 1, 5, 5));

        addButton(commandPanel, "Help", e -> help());
        addButton(commandPanel, "Version", e -> appendOutput("CAD System version 1.0"));
        addButton(commandPanel, "Exit", e -> System.exit(0));
        addButton(commandPanel, "Create Cube", e -> createCube(new String[] {
            cubeSizeField.getText()
        }));
        addButton(commandPanel, "Create Sphere", e -> createSphere(new String[] {
            sphereRadiusField.getText()
        }));
        addButton(commandPanel, "Save File", e -> saveFile(new String[] {
            fileField.getText()
        }));
        addButton(commandPanel, "Load File", e -> loadFile(new String[] {
            fileField.getText()
        }));
        addButton(commandPanel, "Set Cube Div", e -> setCubeDivisions(new String[] {
            cubeSizeField.getText()
        }));
        addButton(commandPanel, "Set Sphere Div", e -> setSphereDivisions(new String[] {
            latField.getText(), lonField.getText()
        }));
        addButton(commandPanel, "Export DXF", e -> exportDXF(new String[] {
            fileField.getText()
        }));
        addButton(commandPanel, "Sketch Clear", e -> sketchClear());
        addButton(commandPanel, "Sketch List", e -> sketchList());
        addButton(commandPanel, "Sketch Point", e -> sketchPoint(new String[] {
            sketchPointX.getText(), sketchPointY.getText()
        }));
        addButton(commandPanel, "Sketch Line", e -> sketchLine(new String[] {
            sketchLineX1.getText(), sketchLineY1.getText(), sketchLineX2.getText(), sketchLineY2.getText()
        }));
        addButton(commandPanel, "Sketch Circle", e -> sketchCircle(new String[] {
            sketchCircleX.getText(), sketchCircleY.getText(), sketchCircleR.getText()
        }));
        addButton(commandPanel, "Sketch Polygon", e -> sketchPolygon(new String[] {
            sketchPolygonX.getText(), sketchPolygonY.getText(), sketchPolygonR.getText(), sketchPolygonSides.getText()
        }));

        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 5, 5));
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

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JScrollPane(commandPanel), BorderLayout.WEST);
        topPanel.add(inputPanel, BorderLayout.CENTER);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topPanel, BorderLayout.NORTH);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

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

    private static void addButton(JPanel panel, String label, ActionListener listener) {
        JButton button = new JButton(label);
        button.addActionListener(listener);
        panel.add(button);
    }

    private static void appendOutput(String text) {
        outputArea.append(text + "\n");
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

    private static void loadFile(String[] args) {
        if (args.length < 1) {
            appendOutput("Usage: load <filename>");
            return;
        }
        String filename = args[0];
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
            }
        } catch (Exception e) {
            appendOutput("Error loading file: " + e.getMessage());
        }
    }

    private static void exportDXF(String[] args) {
        if (args.length < 1) {
            appendOutput("Usage: export_dxf <filename>");
            return;
        }
        try {
            sketch.exportSketchToDXF(args[0]);
            appendOutput("Sketch exported to " + args[0]);
        } catch (Exception e) {
            appendOutput("Error exporting DXF: " + e.getMessage());
        }
    }

    private static void sketchClear() {
        sketch.clearSketch();
        appendOutput("Sketch cleared.");
    }

    private static void sketchList() {
        sketch.listSketch();
    }

    private static void sketchPoint(String[] args) {
        if (args.length < 2) {
            appendOutput("Usage: sketch_point <x> <y>");
            return;
        }
        if (sketch.sketchPoint(args) == 0) appendOutput("Point added to sketch.");
    }

    private static void sketchLine(String[] args) {
        if (args.length < 4) {
            appendOutput("Usage: sketch_line <x1> <y1> <x2> <y2>");
            return;
        }
        if (sketch.sketchLine(args) == 0) appendOutput("Line added to sketch.");
    }

    private static void sketchCircle(String[] args) {
        if (args.length < 3) {
            appendOutput("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        if (sketch.sketchCircle(args) == 0) appendOutput("Circle added to sketch.");
    }

    private static void sketchPolygon(String[] args) {
        if (args.length != 5) {
            try {
                float x = Float.parseFloat(args[0]);
                float y = Float.parseFloat(args[1]);
                float radius = Float.parseFloat(args[2]);
                int sides = Integer.parseInt(args[3]);

                if (sides < 3 || sides > 25) {
                    System.out.println("Polygon sides must be between 3 and 25.");
                    return;
                }

                int result = sketch.addNSidedPolygon(x, y, radius, sides);
                System.out.println(result == 0 ? "Polygon added to sketch." : "Failed to add polygon to sketch.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric value in arguments.");
            }
        } else if (args.length >= 7 && args.length % 2 == 1) {
            try {
                int pointCount = (args.length - 1) / 2;
                if (pointCount < 3 || pointCount > 25) {
                    System.out.println("Polygon must have between 3 and 25 points.");
                    return;
                }

                List <Sketch.PointEntity> points = new ArrayList <> ();
                for (int i = 1; i < args.length; i += 2) {
                    float px = Float.parseFloat(args[i]);
                    float py = Float.parseFloat(args[i + 1]);
                    points.add(new Sketch.PointEntity(px, py));
                }

                int result = sketch.addPolygon(points);
                System.out.println(result == 0 ? "Polygon added to sketch." : "Failed to add polygon to sketch.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric value in polygon points.");
            }
        } else {
            // Help message
            System.out.println("Usage:");
            System.out.println("  sketch_polygon <x> <y> <radius> <sides>           - Regular polygon (3 to 25 sides)");
            System.out.println("  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn>   - Polygon from explicit points (3 to 25 points)");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Gui().setVisible(true));
    }

    public static void launch() {
        System.out.println("Launching GUI...");
        SwingUtilities.invokeLater(() -> {
            Gui gui = new Gui();
            gui.setVisible(true);
        });
    }
}
