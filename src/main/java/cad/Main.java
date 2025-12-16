package cad;

import java.util.Scanner;
import cad.cli.Cli;
import cad.gui.GuiFX;

/**
 * Main entry point for the CAD application.
 * Provides user choice between command-line interface (CLI) and graphical
 * interface (GUI).
 */
public class Main {

    /**
     * Entry point of the application.
     * Prompts the user to select between CLI or GUI mode.
     * Launches the selected interface or shows an error for invalid input.
     */
    public static void main(String[] args) {
        // Add shutdown hook for graceful exit handling
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCAD Application shutting down gracefully...");
        }));

        try {
            // Check for command-line arguments
            if (args.length > 0) {
                // Launch based on command-line argument
                String mode = args[0].toLowerCase();
                if (mode.equals("--cli") || mode.equals("-cli")) {
                    System.out.println("Starting CLI mode...");
                    Cli.launch();
                    return;
                } else if (mode.equals("--gui") || mode.equals("-gui")) {
                    System.out.println("Starting GUI mode...");
                    javafx.application.Application.launch(GuiFX.class, args);
                    return;
                } else {
                    System.out.println("Invalid argument: " + mode);
                    System.out.println("Usage: java -jar SketchApp.jar [--cli|--gui]");
                    System.out.println("  --cli   Launch in command-line interface mode");
                    System.out.println("  --gui   Launch in graphical user interface mode");
                    System.out.println("  (none)  Launch in GUI mode (default)");
                    System.exit(1);
                }
            }

            // No arguments provided - default to GUI mode
            System.out.println("No arguments provided. Starting GUI mode...");
            javafx.application.Application.launch(GuiFX.class, args);

        } catch (Exception e) {
            System.err.println("Error starting CAD application: " + e.getMessage());
            System.exit(1);
        }
    }
}