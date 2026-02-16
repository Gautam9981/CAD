package cad.cli;

import cad.core.Command;

public interface CliHandler {
    /**
     * Creates a command from the given arguments.
     * 
     * @param args The arguments passed to the command (including the command name
     *             itself at index 0).
     * @return The Command to execute.
     * @throws IllegalArgumentException if arguments are invalid.
     */
    Command createCommand(String[] args) throws IllegalArgumentException;

    /**
     * Returns a short description of usage for help text.
     */
    String getUsage();
}
