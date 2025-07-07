import java.util.Scanner;
import cli.Cli;
import gui.Gui;

public class Main {

    /**
     * Entry point of the application.
     * Prompts the user to select between CLI or GUI mode.
     * Launches the selected interface or shows an error for invalid input.
     */
    public static void main(String[] args) {
        // Create a Scanner object to read user input from the console
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Select mode: (1) CLI or (2) GUI");

            // Read the user's input
            String input = scanner.nextLine();

            // Determine which mode to launch based on input
            if (input.equals("1")) {
                Cli.launch();
            } else if (input.equals("2")) {
                Gui.launch();
            } else {
                System.out.println("Invalid choice. Please try again.");
            }
        } // Scanner is automatically closed here due to try-with-resources
    }
}
