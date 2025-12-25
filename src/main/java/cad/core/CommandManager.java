package cad.core;

import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

public class CommandManager {
    
    private static final int MAX_HISTORY_SIZE = 100;
    
    private Stack<Command> undoStack;
    private Stack<Command> redoStack;
    private List<CommandListener> listeners;
    
    
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
    
    
    public void executeCommand(Command cmd) {
        cmd.execute();
        undoStack.push(cmd);
        
        
        redoStack.clear();
        
        
        while (undoStack.size() > MAX_HISTORY_SIZE) {
            undoStack.remove(0);
        }
        
        notifyCommandExecuted(cmd);
        notifyHistoryChanged();
    }
    
    
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
    
    
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }
    
    
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
    
    
    public String getUndoDescription() {
        if (undoStack.isEmpty()) return "Undo";
        return "Undo " + undoStack.peek().getDescription();
    }
    
    
    public String getRedoDescription() {
        if (redoStack.isEmpty()) return "Redo";
        return "Redo " + redoStack.peek().getDescription();
    }
    
    
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        notifyHistoryChanged();
    }
    
    
    public int getUndoCount() {
        return undoStack.size();
    }
    
    
    public int getRedoCount() {
        return redoStack.size();
    }
    
    
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
