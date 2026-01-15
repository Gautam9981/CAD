package cad.cli;

import cad.core.Sketch;
import cad.core.Geometry;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

public class Cli {

    private static int cubeDivisions = 20;

    private static int sphereLatDiv = 30;

    private static int sphereLonDiv = 30;

    private static Sketch sketch = new Sketch();

    private static cad.core.CommandManager commandManager = new cad.core.CommandManager();

    public static void launch() {
        System.out.println("Welcome to CAD CLI v2.5.0");
        System.out.println("Running Cli mode...");
        runCli();
    }

    public static void runCli() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                try {
                    System.out.print("cad> ");
                    if (!scanner.hasNextLine()) {
                        System.out.println("\nEOF detected, exiting CAD CLI gracefully.");
                        System.exit(0);
                    }

                    String input = scanner.nextLine().trim();
                    if (input.isEmpty())
                        continue;

                    String[] argsArray = input.split("\\s+");
                    String command = argsArray[0].toLowerCase();

                    switch (command) {
                        case "help":
                        case "h":
                            help();
                            break;

                        case "version":
                        case "v":
                            version();
                            break;

                        case "exit":
                        case "e":
                            exit();
                            break;

                        case "cube":
                        case "c":
                            createCube(argsArray);
                            break;

                        case "sphere":
                        case "sp":
                            createSphere(argsArray);
                            break;

                        case "cube_div":
                            setCubeDivisions(argsArray);
                            break;

                        case "sphere_div":
                            setSphereDivisions(argsArray);
                            break;

                        case "save":
                        case "s":
                            saveFile(argsArray);
                            break;

                        case "sketch_clear":
                            sketchClear();
                            break;

                        case "export_dxf":
                            exportDxf(argsArray);
                            break;

                        case "sketch_point":
                            sketchPoint(argsArray);
                            break;

                        case "sketch_line":
                            sketchLine(argsArray);
                            break;

                        case "sketch_circle":
                            sketchCircle(argsArray);
                            break;

                        case "sketch_polygon":
                            sketchPolygon(argsArray);
                            break;

                        case "sketch_list":
                            sketchList();
                            break;

                        case "extrude":
                        case "ext":
                            extrudeRedirectToGUI();
                            break;

                        case "load":
                            loadFile(argsArray);
                            break;

                        case "units":
                            setUnits(argsArray);
                            break;

                        case "cg":
                            handleCg();
                            break;

                        case "naca":
                            handleNaca(argsArray);
                            break;

                        case "revolve":
                            handleRevolve(argsArray);
                            break;

                        case "loft":
                            handleLoft(argsArray);
                            break;

                        case "undo":
                        case "u":
                            handleUndo();
                            break;

                        case "redo":
                        case "r":
                            handleRedo();
                            break;

                        default:
                            System.out.println("Unknown command: " + command + ". Type 'help' for a list.");
                    }
                } catch (Exception e) {

                    System.err.println("Error executing command: " + e.getMessage());
                    System.out.println("Type 'help' for available commands or 'exit' to quit.");
                }
            }
        } catch (Exception e) {

            System.err.println("Unexpected error in CLI: " + e.getMessage());
            System.err.println("Exiting CLI due to error.");
            System.exit(1);
        }
    }

    private static void handleLoft(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: loft <height>");
            return;
        }
        try {
            float height = Float.parseFloat(args[1]);
            Geometry.loft(sketch, height);
            System.out.println("Loft created between first two sketch polygons with height " + height);
        } catch (NumberFormatException e) {
            System.out.println("Invalid height.");
        }
    }

    private static void help() {
        System.out.println("Available commands:");
        System.out.println("  cube <size>                 - Create a cube");
        System.out.println("  c <size>                    - Alias for cube");
        System.out.println("  sphere <radius>             - Create a sphere");
        System.out.println("  sp <radius>                 - Alias for sphere");
        System.out.println("  save <filename>             - Save shape to STL");
        System.out.println("  s <filename>                - Alias for save");
        System.out.println("  load <filename>             - Load STL or DXF file");
        System.out.println("  cube_div <count>            - Set cube subdivisions");
        System.out.println("  sphere_div <lat> <lon>      - Set sphere subdivisions");
        System.out.println("  sketch_point <x> <y>        - Add point to sketch");
        System.out.println("  sketch_line <x1> <y1> <x2> <y2> - Add line to sketch");
        System.out.println("  sketch_circle <x> <y> <radius> - Add circle to sketch");
        System.out.println("  sketch_polygon <x> <y> <radius> <sides> - Add polygon (3-25 sides)");
        System.out.println(
                "  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn> - Add polygon from explicit points (3-25 points)");
        System.out.println("  sketch_clear                - Clear sketch");
        System.out.println("  sketch_list                 - List all sketch entities");
        System.out.println("  extrude                     - View extrusion info (use GUI for better experience)");
        System.out.println("  export_dxf <filename>       - Export sketch to DXF");
        System.out.println("  units <mm|cm|m|in|ft>       - Set units");
        System.out.println("  cg                          - Calculate Center of Gravity");
        System.out.println("  naca <digits> [chord]       - Generate NACA 4-digit airfoil (default chord=1.0)");
        System.out.println("  revolve [angle] [steps]     - Revolve sketch 360deg around Y-axis");
        System.out.println("  loft <height>               - Loft between first two polygons in sketch");
        System.out.println("  undo (u)                    - Undo last operation");
        System.out.println("  redo (r)                    - Redo last operation");
        System.out.println("  help (h), version (v), exit (e)");
    }

    private static void version() {
        System.out.println("SketchApp, version 2.5 (Beta)");
    }

    private static void exit() {
        System.out.println("Exiting CAD CLI. Thanks for using it!");
        System.out.flush();
        System.exit(0);
    }

    private static void createCube(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: cube <size>");
            return;
        }
        try {
            float size = Float.parseFloat(args[1]);
            Geometry.createCube(size, cubeDivisions);
        } catch (NumberFormatException e) {
            System.out.println("Invalid size value.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void createSphere(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: sphere <radius>");
            return;
        }
        try {
            float radius = Float.parseFloat(args[1]);
            int maxDiv = Math.max(sphereLatDiv, sphereLonDiv);
            Geometry.createSphere(radius, maxDiv);
        } catch (NumberFormatException e) {
            System.out.println("Invalid radius value.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void setCubeDivisions(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: cube_div <count>");
            return;
        }
        try {
            int count = Integer.parseInt(args[1]);
            if (count < 1 || count > 200) {
                System.out.println("Cube subdivisions must be between 1 and 200.");
            } else {
                cubeDivisions = count;
                System.out.println("Cube subdivisions set to " + cubeDivisions);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid count value.");
        }
    }

    private static void setSphereDivisions(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: sphere_div <lat> <lon>");
            return;
        }
        try {
            int lat = Integer.parseInt(args[1]);
            int lon = Integer.parseInt(args[2]);
            if (lat < 1 || lat > 200 || lon < 1 || lon > 200) {
                System.out.println("Sphere subdivisions must be between 1 and 200.");
            } else {
                sphereLatDiv = lat;
                sphereLonDiv = lon;
                System.out.printf("Sphere subdivisions set to %d lat, %d lon%n", lat, lon);
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid subdivision values.");
        }
    }

    private static void saveFile(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: save <filename>");
            return;
        }
        String filename = args[1];
        try {
            Geometry.saveStl(filename);
        } catch (Exception e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    private static void sketchClear() {
        sketch.clearSketch();
        System.out.println("Sketch cleared.");
    }

    private static void exportDxf(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: export_dxf <filename>");
            return;
        }
        String filename = args[1];
        try {
            sketch.exportSketchToDXF(filename);
            System.out.println("Sketch exported to " + filename);
        } catch (Exception e) {
            System.out.println("Error exporting DXF: " + e.getMessage());
        }
    }

    private static void sketchPoint(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: sketch_point <x> <y>");
            return;
        }
        try {
            float x = Float.parseFloat(args[1]);
            float y = Float.parseFloat(args[2]);
            cad.core.Sketch.PointEntity point = new cad.core.Sketch.PointEntity(x, y);
            commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, point, "Point"));
            System.out.println("Point added to sketch.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinates.");
        }
    }

    private static void sketchLine(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: sketch_line <x1> <y1> <x2> <y2>");
            return;
        }
        try {
            float x1 = Float.parseFloat(args[1]);
            float y1 = Float.parseFloat(args[2]);
            float x2 = Float.parseFloat(args[3]);
            float y2 = Float.parseFloat(args[4]);
            cad.core.Sketch.Line line = new cad.core.Sketch.Line(x1, y1, x2, y2);
            commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, line, "Line"));
            System.out.println("Line added to sketch.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid coordinates.");
        }
    }

    private static void sketchCircle(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        try {
            float x = Float.parseFloat(args[1]);
            float y = Float.parseFloat(args[2]);
            float r = Float.parseFloat(args[3]);
            cad.core.Sketch.Circle circle = new cad.core.Sketch.Circle(x, y, r);
            commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, circle, "Circle"));
            System.out.println("Circle added to sketch.");
        } catch (NumberFormatException e) {
            System.out.println("Invalid parameters.");
        }
    }

    private static void sketchPolygon(String[] args) {

        if (args.length == 5 || args.length == 6) {
            try {
                float x = Float.parseFloat(args[1]);
                float y = Float.parseFloat(args[2]);
                float radius = Float.parseFloat(args[3]);
                int sides = Integer.parseInt(args[4]);

                boolean circumscribed = false;
                if (args.length == 6) {
                    String mode = args[5].toLowerCase();
                    if (mode.equals("c") || mode.equals("circumscribed")) {
                        circumscribed = true;
                    } else if (!mode.equals("i") && !mode.equals("inscribed")) {
                        System.out.println("Invalid mode. Use 'c' for circumscribed or 'i' for inscribed.");
                        return;
                    }
                }

                if (sides < 3 || sides > 25) {
                    System.out.println("Polygon sides must be between 3 and 200.");
                    return;
                }

                sketch.addNSidedPolygon(x, y, radius, sides, circumscribed);
                System.out.println("Polygon added to sketch.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric value in arguments.");
            }
        }

        else if (args.length >= 7 && args.length % 2 == 1) {
            try {
                int pointCount = (args.length - 1) / 2;
                if (pointCount < 3 || pointCount > 25) {
                    System.out.println("Polygon must have between 3 and 200 points.");
                    return;
                }

                List<cad.core.Sketch.PointEntity> points = new ArrayList<>();
                for (int i = 1; i < args.length; i += 2) {
                    float px = Float.parseFloat(args[i]);
                    float py = Float.parseFloat(args[i + 1]);
                    points.add(new cad.core.Sketch.PointEntity(px, py));
                }

                cad.core.Sketch.Polygon poly = new cad.core.Sketch.Polygon(points);
                commandManager.executeCommand(new cad.core.AddEntityCommand(sketch, poly, "Polygon"));
                System.out.println("Polygon added to sketch.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric value in polygon points.");
            }
        } else {
            System.out.println("Usage:");
            System.out.println(
                    "  sketch_polygon <x> <y> <radius> <sides> [c|i]     - Regular polygon (3 to 25 sides), optional c=circumscribed");
            System.out.println(
                    "  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn>   - Polygon from explicit points (3 to 25 points)");
        }
    }

    private static void sketchList() {
        List<String> items = sketch.listSketch();
        if (items.isEmpty()) {
            System.out.println("Sketch is empty.");
        } else {
            System.out.println("Sketch contains:");
            for (String item : items) {
                System.out.println("  " + item);
            }
        }
    }

    private static void extrudeRedirectToGUI() {
        System.out.println("==========================================");
        System.out.println("           EXTRUSION NOTICE");
        System.out.println("==========================================");
        System.out.println();
        System.out.println("Extrusion functionality has been moved to the GUI for a better user experience.");
        System.out.println();
        System.out.println("The GUI provides:");
        System.out.println("  • Real-time 3D visualization of extruded geometry");
        System.out.println("  • Interactive controls for extrusion height");
        System.out.println("  • Support for extruding circles, polygons, and other shapes");
        System.out.println("  • Immediate visual feedback during the extrusion process");
        System.out.println("  • Better handling of complex DXF files with multiple entities");
        System.out.println();
        System.out.println("To use extrusion:");
        System.out.println("  1. Export your sketch to DXF: export_dxf mysketch.dxf");
        System.out.println("  2. Run the GUI application");
        System.out.println("  3. Load your DXF file in the GUI");
        System.out.println("  4. Use the 'Extrude Sketch' button for 3D extrusion");
        System.out.println();
        System.out.println("Thank you for understanding!");
        System.out.println("==========================================");
    }

    private static void loadFile(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: load <filename>");
            return;
        }

        String filename = args[1];
        String lowerFilename = filename.toLowerCase();

        try {
            if (lowerFilename.endsWith(".stl")) {
                Geometry.loadStl(filename);
                System.out.println("STL file loaded: " + filename);
            } else if (lowerFilename.endsWith(".dxf")) {
                sketch.loadDXF(filename);
                System.out.println("DXF file loaded: " + filename);
            } else {
                System.out.println("Unsupported file format provided.");
            }
        } catch (Exception e) {
            System.out.println("Error loading file: " + filename);
        }
    }

    private static void setUnits(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: units <mm|cm|m|in|ft>");
            return;
        }
        String unit = args[1].toLowerCase();
        List<String> validUnits = List.of("mm", "cm", "m", "in", "ft");

        if (!validUnits.contains(unit)) {
            System.out.println("Unsupported unit. Supported units: mm, cm, m, in, ft");
            return;
        }

        cad.core.UnitSystem sys = cad.core.UnitSystem.MMGS;
        switch (unit) {
            case "mm":
                sys = cad.core.UnitSystem.MMGS;
                break;
            case "cm":
                sys = cad.core.UnitSystem.CGS;
                break;
            case "m":
                sys = cad.core.UnitSystem.MKS;
                break;
            case "in":
                sys = cad.core.UnitSystem.IPS;
                break;
            default:
                System.out.println("Unsupported CLI unit. Supported: mm, cm, m, in (mapped to standard systems)");
                return;
        }

        sketch.setUnitSystem(sys);
        System.out.println("Units set to: " + sys.name());
    }

    private static void handleCg() {
        float[] cg = Geometry.calculateCentroid();
        System.out.printf("Center of Gravity (CG): X=%.4f, Y=%.4f, Z=%.4f%n", cg[0], cg[1], cg[2]);
    }

    private static void handleNaca(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: naca <4-digits> [chord]");
            return;
        }
        String digits = args[1];
        float chord = 1.0f;
        if (args.length >= 3) {
            try {
                chord = Float.parseFloat(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid chord length.");
                return;
            }
        }

        int result = sketch.generateNaca4(digits, chord, 50);
        if (result == 0) {
            System.out.println("NACA " + digits + " airfoil generated with chord " + chord + ".");
        } else {
            System.out.println("Failed to generate airfoil.");
        }
    }

    private static void handleRevolve(String[] args) {
        float angle = 360.0f;
        int steps = 60;

        if (args.length >= 2) {
            try {
                angle = Float.parseFloat(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid angle.");
            }
        }
        if (args.length >= 3) {
            try {
                steps = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid steps.");
            }
        }

        Geometry.revolve(sketch, angle, steps);
        System.out.println("Revolve operation completed.");
    }

    private static void handleUndo() {
        if (commandManager.undo()) {
            System.out.println("Undo successful: " + commandManager.getRedoDescription());

        } else {
            System.out.println("Nothing to undo.");
        }
    }

    private static void handleRedo() {
        if (commandManager.redo()) {
            System.out.println("Redo successful: " + commandManager.getUndoDescription());
        } else {
            System.out.println("Nothing to redo.");
        }
    }
}
