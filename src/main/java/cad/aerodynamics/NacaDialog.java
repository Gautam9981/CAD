package cad.aerodynamics;

import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import cad.core.Sketch;
import cad.core.CommandManager;
import cad.core.AddPolygonCommand;

import java.util.List;

public class NacaDialog {

    private Sketch sketch;
    private CommandManager commandManager;
    private Stage dialog;
    private Runnable onGenerateCallback;

    public NacaDialog(Sketch sketch, CommandManager commandManager) {
        this.sketch = sketch;
        this.commandManager = commandManager;
    }

    public void setOnGenerateCallback(Runnable callback) {
        this.onGenerateCallback = callback;
    }

    public void show() {
        dialog = new Stage();
        dialog.setTitle("NACA Airfoil Generator");
        dialog.initModality(Modality.APPLICATION_MODAL);

        GridPane layout = new GridPane();
        layout.setHgap(10);
        layout.setVgap(10);
        layout.setPadding(new Insets(15));

        TextField codeField = new TextField("2412");
        TextField chordField = new TextField("1.0");
        TextField depthField = new TextField("5.0");

        layout.add(new Label("NACA 4-Digit Code:"), 0, 0);
        layout.add(codeField, 1, 0);
        layout.add(new Label("Chord Length:"), 0, 1);
        layout.add(chordField, 1, 1);
        layout.add(new Label("Depth (3D):"), 0, 2);
        layout.add(depthField, 1, 2);

        Button generateBtn = new Button("Generate");
        generateBtn.setMaxWidth(Double.MAX_VALUE);
        generateBtn.setOnAction(e -> {
            String code = codeField.getText();
            try {
                float chord = Float.parseFloat(chordField.getText());
                float depth = Float.parseFloat(depthField.getText());

                List<cad.core.Sketch.PointEntity> points = NacaAirfoilGenerator.generate(code, chord, 50);

                AddPolygonCommand cmd = new AddPolygonCommand(sketch, points);
                commandManager.executeCommand(cmd);

                if (depth > 0) {
                    sketch.extrude(depth);
                    sketch.setDirty(true);
                }

                if (onGenerateCallback != null) {
                    onGenerateCallback.run();
                }

                dialog.close();
            } catch (NumberFormatException nfe) {
                showError("Invalid number format.");
            } catch (Exception ex) {
                showError("Error: " + ex.getMessage());
            }
        });

        layout.add(generateBtn, 0, 3, 2, 1);

        dialog.setScene(new Scene(layout, 300, 200));
        dialog.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
