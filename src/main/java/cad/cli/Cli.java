package cad.cli;

import cad.core.Sketch;
import cad.core.Geometry;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * Command-line interface for the CAD application.
 * Supports commands to create shapes, manage sketches, save/load files, and configure settings.
 */
public class Cli {

    /** Default subdivisions for cubes */
    private static int cubeDivisions = 20;

    /** Latitude subdivisions for spheres */
    private static int sphereLatDiv = 30;

    /** Longitude subdivisions for spheres */
    private static int sphereLonDiv = 30;

    /** Sketch instance to manage 2D drawing entities */
    private static Sketch sketch = new Sketch();

    /**
     * Launch the CLI application.
     * Prints welcome message and enters the command input loop.
     */
    public static void launch() {
        System.out.println("Welcome to CAD CLI v2.5.0");
        System.out.println("Running Cli mode...");
        runCli();
    }

    /**
     * Enum representing supported measurement units and their conversion to millimeters.
     */
    private enum Unit {
        MM(1.0f), CM(10.0f), M(1000.0f), IN(25.4f), FT(304.8f);

        final float toMillimeter;

        Unit(float toMillimeter) {
            this.toMillimeter = toMillimeter;
        }
    }

    /** Current selected unit for measurements (default: millimeters) */
    private static Unit currentUnit = Unit.MM;

    /**
     * Converts a value from the current unit to millimeters.
     * 
     * @param value The value in current unit
     * @return The value converted to millimeters
     */
    private static float convertToMillimeters(float value) {
        return value * currentUnit.toMillimeter;
    }

    /**
     * Main command processing loop.
     * Reads user input, parses commands, and executes corresponding methods.
     * Includes comprehensive error handling for graceful exit.
     */
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
                    if (input.isEmpty()) continue;

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

                        default:
                            System.out.println("Unknown command: " + command + ". Type 'help' for a list.");
                    }
                } catch (Exception e) {
                    // Handle any command execution errors gracefully
                    System.err.println("Error executing command: " + e.getMessage());
                    System.out.println("Type 'help' for available commands or 'exit' to quit.");
                }
            }
        } catch (Exception e) {
            // Handle any unexpected errors in the main loop
            System.err.println("Unexpected error in CLI: " + e.getMessage());
            System.err.println("Exiting CLI due to error.");
            System.exit(1);
        }
    }

    /**
     * Displays a help message listing all available commands and their usage.
     */
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
        System.out.println("  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn> - Add polygon from explicit points (3-25 points)");
        System.out.println("  sketch_clear                - Clear sketch");
        System.out.println("  sketch_list                 - List all sketch entities");
        System.out.println("  extrude                     - View extrusion info (use GUI for better experience)");
        System.out.println("  export_dxf <filename>       - Export sketch to DXF");
        System.out.println("  units <mm|cm|m|in|ft>       - Set units");
        System.out.println("  help (h), version (v), exit (e)");
    }

    /**
     * Prints the version information.
     */
    private static void version() {
        System.out.println("CAD, version 1.0 (Beta)");
    }

    /**
     * Exits the application gracefully.
     * Provides user feedback and ensures clean termination.
     */
    private static void exit() {
        System.out.println("Exiting CAD CLI. Thanks for using it!");
        System.out.flush(); // Ensure output is written before exit
        System.exit(0);
    }

    /**
     * Creates a cube with the specified size and configured subdivisions.
     * 
     * @param args Command arguments, expects size as second argument
     */
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

    /**
     * Creates a sphere with the specified radius and configured subdivisions.
     * 
     * @param args Command arguments, expects radius as second argument
     */
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

    /**
     * Sets the number of subdivisions for cubes.
     * 
     * @param args Command arguments, expects count as second argument
     */
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

    /**
     * Sets latitude and longitude subdivisions for spheres.
     * 
     * @param args Command arguments, expects lat and lon as second and third arguments
     */
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

    /**
     * Saves the current geometry to an STL file.
     * 
     * @param args Command arguments, expects filename as second argument
     */
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

    /**
     * Clears all entities from the current sketch.
     */
    private static void sketchClear() {
        sketch.clearSketch();
        System.out.println("Sketch cleared.");
    }

    /**
     * Exports the current sketch to a DXF file.
     * 
     * @param args Command arguments, expects filename as second argument
     */
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

    /**
     * Adds a point entity to the sketch.
     * 
     * @param args Command arguments, expects x and y as second and third arguments
     */
    private static void sketchPoint(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: sketch_point <x> <y>");
            return;
        }
        String[] params = { args[1], args[2] };
        int result = sketch.sketchPoint(params);
        if (result == 0) System.out.println("Point added to sketch.");
        else System.out.println("Failed to add point.");
    }

    /**
     * Adds a line entity to the sketch.
     * 
     * @param args Command arguments, expects x1, y1, x2, y2 as arguments 2-5
     */
    private static void sketchLine(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: sketch_line <x1> <y1> <x2> <y2>");
            return;
        }
        String[] params = { args[1], args[2], args[3], args[4] };
        int result = sketch.sketchLine(params);
        if (result == 0) System.out.println("Line added to sketch.");
        else System.out.println("Failed to add line.");
    }

    /**
     * Adds a circle entity to the sketch.
     * 
     * @param args Command arguments, expects x, y, radius as arguments 2-4
     */
    private static void sketchCircle(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        String[] params = { args[1], args[2], args[3] };
        int result = sketch.sketchCircle(params);
        if (result == 0) System.out.println("Circle added to sketch.");
        else System.out.println("Failed to add circle.");
    }

    /**
     * Adds a polygon to the sketch.
     * Supports two modes:
     *  - Regular polygon defined by center (x,y), radius, and number of sides
     *  - Polygon defined by explicit points (at least 3)
     * 
     * @param args Command arguments for polygon creation
     */
    private static void sketchPolygon(String[] args) {
        // Regular polygon mode: sketch_polygon <x> <y> <radius> <sides>
        if (args.length == 5) {
            try {
                float x = Float.parseFloat(args[1]);
                float y = Float.parseFloat(args[2]);
                float radius = Float.parseFloat(args[3]);
                int sides = Integer.parseInt(args[4]);

                if (sides < 3 || sides > 25) {
                    System.out.println("Polygon sides must be between 3 and 25.");
                    return;
                }

                int result = sketch.addNSidedPolygon(x, y, radius, sides);
                System.out.println(result == 0 ? "Polygon added to sketch." : "Failed to add polygon to sketch.");
            } catch (NumberFormatException e) {
                System.out.println("Invalid numeric value in arguments.");
            }
        } 
        // Polygon from explicit points: sketch_polygon <x1> <y1> ... <xn> <yn>
        else if (args.length >= 7 && args.length % 2 == 1) {
            try {
                int pointCount = (args.length - 1) / 2;
                if (pointCount < 3 || pointCount > 25) {
                    System.out.println("Polygon must have between 3 and 25 points.");
                    return;
                }

                List<Sketch.PointEntity> points = new ArrayList<>();
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
            System.out.println("Usage:");
            System.out.println("  sketch_polygon <x> <y> <radius> <sides>           - Regular polygon (3 to 25 sides)");
            System.out.println("  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn>   - Polygon from explicit points (3 to 25 points)");
        }
    }

    /**
     * Lists all entities currently in the sketch.
     */
    private static void sketchList() {
        sketch.listSketch();
    }

    /**
     * Displays information about extrusion and redirects users to the GUI for better experience.
     * The CLI no longer supports extrusion functionality as it's better suited for the GUI.
     */
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

    /**
     * Loads an STL or DXF file.
     * 
     * @param args Command arguments, expects filename as second argument
     */
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

    /**
     * Sets the current measurement units.
     * Supported units: mm, cm, m, in, ft
     * 
     * @param args Command arguments, expects unit as second argument
     */
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

        sketch.setUnits(unit);
        System.out.println("Units set to: " + unit);
    }
}
