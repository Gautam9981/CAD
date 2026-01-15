package cad;

import java.util.Scanner;
import cad.cli.Cli;
import cad.gui.GuiFX;

public class Main {

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nCAD Application shutting down gracefully...");
        }));

        try {

            if (args.length > 0) {
                String mode = args[0].toLowerCase();
                if (mode.equals("--cli") || mode.equals("-cli")) {
                    Cli.launch();
                } else if (mode.equals("--gui") || mode.equals("-gui")) {
                    javafx.application.Application.launch(GuiFX.class, args);
                } else {
                    printUsage();
                }
            } else {
                System.out.println("No arguments provided. Starting GUI mode...");
                javafx.application.Application.launch(GuiFX.class, args);
            }
        } catch (Exception e) {
            System.err.println("Error starting CAD application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar SketchApp.jar [--cli|--gui]");
        System.out.println("  --cli     Launch in command-line interface mode");
        System.out.println("  --gui     Launch in graphical user interface mode");
    }
}