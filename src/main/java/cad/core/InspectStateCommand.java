package cad.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class InspectStateCommand implements Command {

    private final Sketch sketch;

    public InspectStateCommand(Sketch sketch) {
        this.sketch = sketch;
    }

    @Override
    public void execute() {
        Map<String, Object> state = new HashMap<>();

        // Sketch State
        Map<String, Object> sketchData = new HashMap<>();
        List<Map<String, Object>> entities = new ArrayList<>();

        for (Sketch.Entity e : sketch.getEntities()) {
            Map<String, Object> entityMap = new HashMap<>();
            entityMap.put("type", e.getClass().getSimpleName());
            entityMap.put("data", e.toString());
            entities.add(entityMap);
        }
        sketchData.put("entities", entities);
        sketchData.put("isClosed", sketch.isClosedLoop());
        sketchData.put("units", sketch.getUnitSystem());
        state.put("sketch", sketchData);

        // 3D Geometry State
        Map<String, Object> geometryData = new HashMap<>();
        geometryData.put("currentShape", Geometry.getCurrentShape());
        geometryData.put("primitiveType", Geometry.getPrimitiveShapeType());
        geometryData.put("param", Geometry.getParam());
        geometryData.put("maxDimension", Geometry.getModelMaxDimension());

        state.put("geometry", geometryData);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonOutput = gson.toJson(state);

        System.out.println("=== CAD STATE INSPECTION ===");
        System.out.println(jsonOutput);
        System.out.println("============================");
    }

    @Override
    public void undo() {
        // Inspection is read-only, no undo needed.
    }

    @Override
    public String getDescription() {
        return "Inspect State";
    }
}
