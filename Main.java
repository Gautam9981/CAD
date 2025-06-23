import java.util.Scanner;
import cli.Cli;
import gui.Gui;

public class Main {

    public static void main(String[] args) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Select mode: (1) CLI or (2) GUI");
            String input = scanner.nextLine();

            if (input.equals("1")) {
                Cli.launch();
            } else if (input.equals("2")) {
                Gui.launch();}
            else {
                System.out.println("Invalid choice. Please try again");
            }
            scanner.close();
        }
    }
