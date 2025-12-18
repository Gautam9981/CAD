package cad.core;

import cad.gui.GuiFX;


public class ViewChangeCommand implements Command {
    private final GuiFX gui;
    private final boolean newState; // true for Sketch, false for 3D
    private final boolean oldState;

    public ViewChangeCommand(GuiFX gui, boolean newState, boolean oldState) {
        this.gui = gui;
        this.newState = newState;
        this.oldState = oldState;
    }

    @Override
    public void execute() {
        gui.setViewMode(newState);
    }

    @Override
    public void undo() {
        gui.setViewMode(oldState);
    }

    @Override
    public String getDescription() {
        return newState ? "Switch to Sketch Mode" : "Switch to 3D Mode";
    }
}
