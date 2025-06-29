package cli;

import core.Point;
import core.Sketch;
import core.Geometry;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;


public class Cli {
    private static int cubeDivisions = 20;
    private static int sphereLatDiv = 30;
    private static int sphereLonDiv = 30;

    private static Sketch sketch = new Sketch();

    public static void launch() {
        System.out.println("Welcome to CAD CLI v1.0 (BETA)");
        System.out.println("Running Cli mode...");
        runCli();
    }

    private enum Unit {
        MM(1.0f), CM(10.0f), M(1000.0f), IN(25.4f), FT(304.8f);

        final float toMillimeter;

        Unit(float toMillimeter) {
            this.toMillimeter = toMillimeter;
        }
    }

    private static Unit currentUnit = Unit.MM;

    private static float convertToMillimeters(float value) {
        return value * currentUnit.toMillimeter;
    }


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

                    case "load":
                        loadFile(argsArray);
                        break;

                    case "units":
                        setUnits(argsArray);
                        break;

                    default:
                        System.out.println("Unknown command: " + command + ". Type 'help' for a list.");
                }
            }
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
        System.out.println("  sketch_polygon <x1> <y1> <x2> <y2> ... <xn> <yn> - Add polygon from explicit points (at least 3 points)");
        System.out.println("  sketch_clear                - Clear sketch");
        System.out.println("  sketch_list                 - List all sketch entities");
        System.out.println("  export_dxf <filename>       - Export sketch to DXF");
        System.out.println("  units <mm|cm|m|in|ft>       - Set units");
        System.out.println("  help (h), version (v), exit (e)");
    }

    private static void version() {
        System.out.println("CAD, version 1.0 (Beta)");
    }

    private static void exit() {
        System.out.println("Exiting the CLI. Thanks for using it!");
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
        String[] params = {
            args[1],
            args[2]
        };
        int result = sketch.sketchPoint(params);
        if (result == 0) System.out.println("Point added to sketch.");
        else System.out.println("Failed to add point.");
    }

    private static void sketchLine(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: sketch_line <x1> <y1> <x2> <y2>");
            return;
        }
        String[] params = {
            args[1],
            args[2],
            args[3],
            args[4]
        };
        int result = sketch.sketchLine(params);
        if (result == 0) System.out.println("Line added to sketch.");
        else System.out.println("Failed to add line.");
    }

    private static void sketchCircle(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: sketch_circle <x> <y> <radius>");
            return;
        }
        String[] params = {
            args[1],
            args[2],
            args[3]
        };
        int result = sketch.sketchCircle(params);
        if (result == 0) System.out.println("Circle added to sketch.");
        else System.out.println("Failed to add circle.");
    }

    private static void sketchPolygon(String[] args) {
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
        } else if (args.length >= 7 && args.length % 2 == 1) {
            try {
                int pointCount = (args.length - 1) / 2;
                if (pointCount < 3 || pointCount > 25) {
                    System.out.println("Polygon must have between 3 and 25 points.");
                    return;
                }

                List < Sketch.PointEntity > points = new ArrayList < > ();
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


    private static void sketchList() {
        sketch.listSketch();
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
        List < String > validUnits = List.of("mm", "cm", "m", "in", "ft");

        if (!validUnits.contains(unit)) {
            System.out.println("Unsupported unit. Supported units: mm, cm, m, in, ft");
            return;
        }

        sketch.setUnits(unit);
        System.out.println("Units set to: " + unit);
    }


}
