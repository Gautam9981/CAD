package cad.core;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

/**
 * CommandManager - Manages command history for undo/redo operations.
 * Implements the Command Pattern for full history tracking.
 */
public class CommandManager {
    
    private static final int MAX_HISTORY_SIZE = 100;
    
    private Stack<Command> undoStack;
    private Stack<Command> redoStack;
    private List<CommandListener> listeners;
    
    /**
     * Listener interface for command state changes.
     */
    public interface CommandListener {
        void onCommandExecuted(Command cmd);
        void onUndo(Command cmd);
        void onRedo(Command cmd);
        void onHistoryChanged();
    }
    
    public CommandManager() {
        undoStack = new Stack<>();
        redoStack = new Stack<>();
        listeners = new ArrayList<>();
    }
    
    /**
     * Execute a command and add it to the undo history.
     * @param cmd The command to execute
     */
    public void executeCommand(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        
        // Clear redo stack when new command is executed
        redoStack.clear();
        
        // Limit history size
        while (undoStack.size() > MAX_HISTORY_SIZE) {
            undoStack.remove(0);
        }
        
        notifyCommandExecuted(cmd);
        notifyHistoryChanged();
    }
    
    /**
     * Undo the last command.
     * @return true if undo was successful, false if nothing to undo
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        
        Command cmd = undoStack.pop();
        cmd.undo();
        redoStack.push(cmd);
        
        notifyUndo(cmd);
        notifyHistoryChanged();
        return true;
    }
    
    /**
     * Redo the last undone command.
     * @return true if redo was successful, false if nothing to redo
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        
        Command cmd = redoStack.pop();
        cmd.execute();
        undoStack.push(cmd);
        
        notifyRedo(cmd);
        notifyHistoryChanged();
        return true;
    }
    
    /**
     * Check if undo is available.
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    /**
     * Check if redo is available.
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    /**
     * Get the description of the next undo command.
     */
    public String getUndoDescription() {
        if (undoStack.isEmpty()) return "Undo";
        return "Undo " + undoStack.peek().getDescription();
    }
    
    /**
     * Get the description of the next redo command.
     */
    public String getRedoDescription() {
        if (redoStack.isEmpty()) return "Redo";
        return "Redo " + redoStack.peek().getDescription();
    }
    
    /**
     * Clear all history.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        notifyHistoryChanged();
    }
    
    /**
     * Get the number of commands in undo history.
     */
    public int getUndoCount() {
        return undoStack.size();
    }
    
    /**
     * Get the number of commands in redo history.
     */
    public int getRedoCount() {
        return redoStack.size();
    }
    
    // Listener management
    public void addListener(CommandListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(CommandListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyCommandExecuted(Command cmd) {
        for (CommandListener l : listeners) {
            l.onCommandExecuted(cmd);
        }
    }
    
    private void notifyUndo(Command cmd) {
        for (CommandListener l : listeners) {
            l.onUndo(cmd);
        }
    }
    
    private void notifyRedo(Command cmd) {
        for (CommandListener l : listeners) {
            l.onRedo(cmd);
        }
    }
    
    private void notifyHistoryChanged() {
        for (CommandListener l : listeners) {
            l.onHistoryChanged();
        }
    }
}
