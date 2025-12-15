package cad.core;

/**
 * Command to add a dimension to the sketch.
 */
public class AddDimensionCommand implements Command {
    private Sketch sketch;
    private Dimension dimension;

    public AddDimensionCommand(Sketch sketch, Dimension dimension) {
        this.sketch = sketch;
        this.dimension = dimension;
    }

    @Override
    public void execute() {
        sketch.addDimension(dimension);
    }

    @Override
    public void undo() {
        sketch.removeDimension(dimension);
    }

    @Override
    public String getDescription() {
        return "Add Dimension";
    }
}
