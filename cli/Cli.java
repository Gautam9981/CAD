import java.util.Scanner;

public class Cli {
    private static int cubeDivisions = 20;
    private static int sphereLatDiv = 30;
    private static int sphereLonDiv = 30;

    // Added static Sketch instance to manage sketch data
    private static Sketch sketch = new Sketch();

    public static void main(String[] args) {
        System.out.println("Welcome to CAD CLI v0.0 (BETA)");
        runCli();
    }

    public static void runCli() {
        Scanner scanner = new Scanner(System.in);

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

                default:
                    System.out.println("Unknown command: " + command + ". Type 'help' for a list.");
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
        System.out.println("  cube_div <count>            - Set cube subdivisions");
        System.out.println("  sphere_div <lat> <lon>      - Set sphere subdivisions");
        System.out.println("  sketch_clear                - Clear sketch");
        System.out.println("  export_dxf <filename>       - Export to DXF");
        System.out.println("  help (h), version (v), exit (e)");
    }

    private static void version() {
        System.out.println("CAD, version 0.0 (Beta)");
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
            if (count < 1 || count > 100) {
                System.out.println("Cube subdivisions must be between 1 and 100.");
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
            if (lat < 1 || lat > 100 || lon < 1 || lon > 100) {
                System.out.println("Sphere subdivisions must be between 1 and 100.");
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
}
