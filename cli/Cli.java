package cli;

import core.Sketch;
import core.Geometry;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/**
 * Command-Line Interface (CLI) for the CAD system.
 * Supports creation of shapes, sketching primitives, file export/import, and unit configuration.
 */
public class Cli {

    // Default subdivision settings for geometry
    private static int cubeDivisions = 20;
    private static int sphereLatDiv = 30;
    private static int sphereLonDiv = 30;

    // The active sketch object
    private static final Sketch sketch = new Sketch();

    /**
     * Supported measurement units with millimeter conversion.
     */
    private enum Unit {
        MM(1.0f), CM(10.0f), M(1000.0f), IN(25.4f), FT(304.8f);

        final float toMillimeter;

        Unit(float toMillimeter) {
            this.toMillimeter = toMillimeter;
        }
    }

    private static Unit currentUnit = Unit.MM;

    /**
     * Entry point to start the CLI loop.
     */
    public static void launch() {
        System.out.println("Welcome to CAD CLI v1.0 (BETA)");
        System.out.println("Running CLI mode...");
        runCli();
    }

    /**
     * Main CLI input loop. Reads user input and dispatches commands.
     */
    public static void runCli() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("cad> ");

                if (!scanner.hasNextLine()) {
                    System.out.println("\nEOF detected, exiting.");
                    break;
                }

                String input = scanner.nextLine().trim();
                if (input.isEmpty()) continue;

                String[] argsArray = input.split("\\s+");
                String command = argsArray[0].toLowerCase();

                // Command dispatch
                switch (command) {
                    case "help":
                    case "h":         help(); break;
                    case "version":
                    case "v":         version(); break;
                    case "exit":
                    case "e":         exit(); break;
                    case "cube":
                    case "c":         createCube(argsArray); break;
                    case "sphere":
                    case "sp":        createSphere(argsArray); break;
                    case "cube_div":  setCubeDivisions(argsArray); break;
                    case "sphere_div":setSphereDivisions(argsArray); break;
                    case "save":
                    case "s":         saveFile(argsArray); break;
                    case "sketch_clear": sketchClear(); break;
                    case "export_dxf": exportDxf(argsArray); break;
                    case "sketch_point": sketchPoint(argsArray); break;
                    case "sketch_line": sketchLine(argsArray); break;
                    case "sketch_circle": sketchCircle(argsArray); break;
                    case "sketch_polygon": sketchPolygon(argsArray); break;
                    case "sketch_list": sketchList(); break;
                    case "load":      loadFile(argsArray); break;
                    case "units":     setUnits(argsArray); break;
                    default:
                        System.out.println("Unknown command: " + command + ". Type 'help' for a list.");
                }
            }
        }
    }

    /**
     * Converts a value to millimeters using the current unit setting.
     * @param value input value in current units
     * @return equivalent value in millimeters
     */
    private static float convertToMillimeters(float value) {
        return value * currentUnit.toMillimeter;
    }

    /**
     * Prints a list of available CLI commands and their usage.
     */
    private static void help() {
        System.out.println("Available commands:");
        System.out.println("  cube <size>                 - Create a cube");
        System.out.println("  sphere <radius>             - Create a sphere");
        System.out.println("  save <filename>             - Save geometry to STL");
        System.out.println("  load <filename>             - Load STL or DXF file");
        System.out.println("  cube_div <count>            - Set cube subdivisions");
        System.out.println("  sphere_div <lat> <lon>      - Set sphere subdivisions");
        System.out.println("  sketch_point <x> <y>        - Add point to sketch");
        System.out.println("  sketch_line <x1> <y1> <x2> <y2> - Add line to sketch");
        System.out.println("  sketch_circle <x> <y> <radius> - Add circle to sketch");
        System.out.println("  sketch_polygon <x> <y> <r> <sides> - Add regular polygon");
        System.out.println("  sketch_polygon <x1> <y1> ...     - Add polygon from points");
        System.out.println("  sketch_clear                - Clear all sketch entities");
        System.out.println("  sketch_list                 - List all sketch entities");
        System.out.println("  export_dxf <filename>       - Export sketch to DXF");
        System.out.println("  units <mm|cm|m|in|ft>       - Set units");
        System.out.println("  help (h), version (v), exit (e)");
    }

    /**
     * Prints the current application version.
     */
    private static void version() {
        System.out.println("CAD, version 1.0 (Beta)");
    }

    /**
     * Exits the CLI application.
     */
    private static void exit() {
        System.out.println("Exiting the CLI. Thanks for using it!");
        System.exit(0);
    }

    /**
     * Creates a cube of the given size.
     * @param args [0]=cube, [1]=size
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
     * Creates a sphere of the given radius.
     * @param args [0]=sphere, [1]=radius
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
     * Sets cube subdivisions (detail level).
     * @param args [0]=cube_div, [1]=count
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
     * Sets latitude and longitude subdivisions for sphere creation.
     * @param args [0]=sphere_div, [1]=lat, [2]=lon
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
     * @param args [0]=save, [1]=filename
     */
    private static void saveFile(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: save <filename>");
            return;
        }
        try {
            Geometry.saveStl(args[1]);
        } catch (Exception e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    /**
     * Clears all entities in the current sketch.
     */
    private static void sketchClear() {
        sketch.clearSketch();
        System.out.println("Sketch cleared.");
    }

    /*
     * Exports the current sketch to a DXF
    file.
    * @param args [0]=export_dxf, [1]=filename
    */
    private static void exportDxf(String[] args) {
        if (args.length < 2) {
        System.out.println("Usage: export_dxf <filename>");
        return;
    }
    try {
        sketch.exportSketchToDXF(args[1]);
        System.out.println("Sketch exported to " + args[1]);
        } catch (Exception e) {
        System.out.println("Error exporting DXF: " + e.getMessage());
    }
    }

/**
 * Adds a point to the current sketch.
 * @param args [0]=sketch_point, [1]=x, [2]=y
 */
private static void sketchPoint(String[] args) {
    if (args.length < 3) {
        System.out.println("Usage: sketch_point <x> <y>");
        return;
    }
    int result = sketch.sketchPoint(new String[] {args[1], args[2]});
    if (result == 0) System.out.println("Point added to sketch.");
    else System.out.println("Failed to add point.");
}

/**
 * Adds a line segment to the current sketch.
 * @param args [0]=sketch_line, [1]=x1, [2]=y1, [3]=x2, [4]=y2
 */
private static void sketchLine(String[] args) {
    if (args.length < 5) {
        System.out.println("Usage: sketch_line <x1> <y1> <x2> <y2>");
        return;
    }
    int result = sketch.sketchLine(new String[] {args[1], args[2], args[3], args[4]});
    if (result == 0) System.out.println("Line added to sketch.");
    else System.out.println("Failed to add line.");
}

/**
 * Adds a circle to the current sketch.
 * @param args [0]=sketch_circle, [1]=x, [2]=y, [3]=radius
 */
private static void sketchCircle(String[] args) {
    if (args.length < 4) {
        System.out.println("Usage: sketch_circle <x> <y> <radius>");
        return;
    }
    int result = sketch.sketchCircle(new String[] {args[1], args[2], args[3]});
    if (result == 0) System.out.println("Circle added to sketch.");
    else System.out.println("Failed to add circle.");
}

/**
 * Adds a polygon to the current sketch. Supports either regular polygons (defined by center, radius, and sides)
 * or explicit polygons defined by a list of points.
 * @param args CLI arguments, either:
 *             - <x> <y> <radius> <sides>
 *             - <x1> <y1> <x2> <y2> ... <xn> <yn> (at least 3 points)
 */
private static void sketchPolygon(String[] args) {
    if (args.length == 5) {
        // Regular polygon (center + radius + sides)
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
    } else if (args.length >= 7 && args.length % 2 == 1) {
        // Polygon defined by explicit points
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
        // Invalid usage - print help message
        System.out.println("Usage:");
        System.out.println("  sketch_polygon <x> <y> <radius> <sides>           - Regular polygon (3 to 25 sides)");
        System.out.println("  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn> - Polygon from explicit points (3 to 25 points)");
    }
}

/**
 * Lists all sketch entities in the current sketch.
 */
private static void sketchList() {
    sketch.listSketch();
}

/**
 * Loads geometry or sketch data from a file.
 * Supports STL and DXF formats.
 * @param args [0]=load, [1]=filename
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
        System.out.println("Error loading file: " + e.getMessage());
    }
}

/**
 * Sets the current measurement units used for interpreting input values.
 * @param args [0]=units, [1]=unit string ("mm", "cm", "m", "in", "ft")
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
    currentUnit = Unit.valueOf(unit.toUpperCase());
    System.out.println("Units set to: " + unit);
}

}

