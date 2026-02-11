package cad.cli;

import cad.core.Sketch;
import cad.core.CommandManager;
import java.util.Scanner;

public class Cli {

    private static final Sketch sketch = new Sketch();
    private static final CommandManager commandManager = new CommandManager();

    public static void launch() {
        System.out.println("Welcome to CAD CLI v3.0.0 (AI Ready)");
        System.out.println("Running Cli mode...");

        // Register all standard commands
        StandardHandlers.registerAll(commandManager, sketch);

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
                    String commandName = argsArray[0].toLowerCase();

                    CliHandler handler = CommandRegistry.getHandler(commandName);

                    if (handler != null) {
                        try {
                            cad.core.Command cmd = handler.createCommand(argsArray);
                            if (cmd != null) {
                                commandManager.executeCommand(cmd);
                            }
                        } catch (IllegalArgumentException e) {
                            System.out.println("Error: " + e.getMessage());
                        } catch (Exception e) {
                            System.out.println("Execution Error: " + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Unknown command: " + commandName);
                        System.out.println("Type 'help' for available commands.");
                    }
                } catch (Exception e) {
                    System.err.println("Error processing input: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Critical error in CLI loop: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
