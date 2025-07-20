package cad;
import java.util.Scanner;
import cad.cli.Cli;
import cad.gui.GuiFX;

/**
 * Main entry point for the CAD application.
 * Provides user choice between command-line interface (CLI) and graphical interface (GUI).
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
        
        // Create a Scanner object to read user input from the console
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Select mode: (1) CLI or (2) GUI");
            System.out.println("Press Ctrl+C to exit at any time.");

            // Read the user's input
            String input = scanner.nextLine();

            // Determine which mode to launch based on input
            if (input.equals("1")) {
                System.out.println("Starting CLI mode...");
                Cli.launch();
            } else if (input.equals("2")) {
                System.out.println("Starting GUI mode...");
                // Properly launch JavaFX application - this will block until the GUI exits
                javafx.application.Application.launch(GuiFX.class, args);
            } else {
                System.out.println("Invalid choice. Please enter 1 for CLI or 2 for GUI.");
                System.exit(1); // Exit with error code for invalid choice
            }
        } catch (Exception e) {
            System.err.println("Error starting CAD application: " + e.getMessage());
            System.exit(1);
        }
        // Scanner is automatically closed here due to try-with-resources
    }
} 