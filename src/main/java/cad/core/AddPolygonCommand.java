package cad.core;

import cad.core.Sketch.Entity;
import java.util.List;

public class AddPolygonCommand implements Command {

    private Sketch sketch;
    private List<cad.core.Sketch.PointEntity> points;
    private cad.core.Sketch.Polygon polygon;

    public AddPolygonCommand(Sketch sketch, List<cad.core.Sketch.PointEntity> points) {
        this.sketch = sketch;
        this.points = points;
    }

    @Override
    public void execute() {
        if (sketch != null && points != null) {
            sketch.addPolygon(points);
            polygon = (cad.core.Sketch.Polygon) sketch.getEntities().get(sketch.getEntities().size() - 1);
        }
    }

    @Override
    public void undo() {
        if (sketch != null && polygon != null) {
            sketch.removeEntity(polygon);
        }
    }

    @Override
    public String getDescription() {
        return "Add Polygon";
    }
}
