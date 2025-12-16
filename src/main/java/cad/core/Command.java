package cad.core;

/**
 * Command interface for implementing the Command Pattern.
 * Enables undo/redo functionality throughout the application.
 */
public interface Command {
    
    /**
     * Execute the command.
     */
    void execute();
    
    /**
     * Undo the command, reverting to previous state.
     */
    void undo();
    
    /**
     * Get a description of the command for display purposes.
     * @return Human-readable description
     */
    String getDescription();
}
