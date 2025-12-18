package cad.gui;

import cad.core.Material;
import cad.core.MaterialDatabase;
import cad.core.MassProperties;
import cad.core.Sketch;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.application.Platform;

public class MassPropertiesDialog extends Dialog<ButtonType> {

    private Sketch sketch;
    private ComboBox<String> materialCombo;
    private TextField thicknessField;
    private TextArea resultsArea;
    private MassProperties precomputedProps; // For primitives
    private boolean isPrimitive; // Flag to indicate if using precomputed props

    public MassPropertiesDialog(Sketch sketch) {
        this.sketch = sketch;

        setTitle("Mass Properties");
        setHeaderText("Calculate Mass, Area, Volume, and Centroid");

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        // Material selection
        HBox materialBox = new HBox(10);
        materialBox.setAlignment(Pos.CENTER_LEFT);
        Label materialLabel = new Label("Material:");
        materialLabel.setMinWidth(80);

        materialCombo = new ComboBox<>();
        MaterialDatabase db = MaterialDatabase.getInstance();
        for (Material mat : db.getAllMaterials()) {
            materialCombo.getItems().add(mat.getName());
        }

        // Set current material if assigned
        if (sketch.getMaterial() != null) {
            materialCombo.setValue(sketch.getMaterial().getName());
        } else {
            materialCombo.setValue("Aluminum 6061");
        }

        materialCombo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(materialCombo, Priority.ALWAYS);
        materialBox.getChildren().addAll(materialLabel, materialCombo);

        // Thickness input
        HBox thicknessBox = new HBox(10);
        thicknessBox.setAlignment(Pos.CENTER_LEFT);
        Label thicknessLabel = new Label("Thickness:");
        thicknessLabel.setMinWidth(80);

        thicknessField = new TextField(String.valueOf(sketch.getThickness()));
        thicknessField.setPromptText("Enter thickness");
        thicknessField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(thicknessField, Priority.ALWAYS);

        Label unitLabel = new Label(sketch.getUnitSystem().getAbbreviation());
        thicknessBox.getChildren().addAll(thicknessLabel, thicknessField, unitLabel);

        // Calculate button
        Button calculateButton = new Button("Calculate Properties");
        calculateButton.setDefaultButton(true);
        calculateButton.setOnAction(e -> calculate());
        calculateButton.setMaxWidth(Double.MAX_VALUE);

        // Results area
        Label resultsLabel = new Label("Results:");
        resultsArea = new TextArea();
        resultsArea.setEditable(false);
        resultsArea.setPrefRowCount(10);
        resultsArea.setStyle("-fx-font-family: 'Courier New', monospace;");
        VBox.setVgrow(resultsArea, Priority.ALWAYS);

        content.getChildren().addAll(
                materialBox,
                thicknessBox,
                calculateButton,
                new Separator(),
                resultsLabel,
                resultsArea);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Auto-calculate on first show
        Platform.runLater(this::calculate);
    }

    
    public MassPropertiesDialog(Sketch sketch, MassProperties props) {
        this.sketch = sketch;
        this.precomputedProps = props;
        this.isPrimitive = true;

        setTitle("Mass Properties (Primitive Shape)");
        setHeaderText("Mass Properties for " + cad.core.Geometry.getCurrentShape());

        // Create simplified content - just show results
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefWidth(450);

        // Results area
        Label resultsLabel = new Label("Results:");
        resultsArea = new TextArea();
        resultsArea.setEditable(false);
        resultsArea.setPrefRowCount(12);
        resultsArea.setStyle("-fx-font-family: 'Courier New', monospace;");
        VBox.setVgrow(resultsArea, Priority.ALWAYS);

        content.getChildren().addAll(
                resultsLabel,
                resultsArea);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Display precomputed results immediately
        Platform.runLater(this::displayPrimitiveResults);
    }

    private void calculate() {
        try {
            // Get material
            String materialName = materialCombo.getValue();
            Material material = MaterialDatabase.getInstance().getMaterial(materialName);

            if (material == null) {
                showError("Material not found: " + materialName);
                return;
            }

            // Get thickness
            double thickness = Double.parseDouble(thicknessField.getText().trim());
            if (thickness <= 0) {
                showError("Thickness must be positive.");
                return;
            }

            // Update sketch thickness only (NOT material - that's managed separately)
            // sketch.setMaterial(material); // REMOVED - Material should only be set via
            // Material Database Manager
            sketch.setThickness(thickness);

            // Calculate
            MassProperties props = sketch.calculateMassProperties();

            if (props == null) {
                resultsArea.setText("No closed shapes found in sketch.\nCreate a polygon or circle first.");
                return;
            }

            // Display results
            StringBuilder sb = new StringBuilder();
            String unit = sketch.getUnitSystem().getAbbreviation();

            sb.append("=".repeat(40)).append("\n");
            sb.append("MASS PROPERTIES\n");
            sb.append("=".repeat(40)).append("\n\n");

            sb.append("Material\n");
            sb.append("  Name: ").append(material.getName()).append("\n");
            sb.append("  Density: ").append(String.format("%.1f", material.getDensity())).append(" kg/m³\n");
            sb.append("  Category: ").append(material.getCategory()).append("\n\n");

            sb.append("Geometry\n");
            sb.append("  Thickness: ").append(String.format("%.2f", thickness)).append(" ").append(unit).append("\n");
            sb.append("  Area: ").append(String.format("%.2f", props.getArea())).append(" ").append(unit).append("²\n");
            sb.append("  Volume: ").append(String.format("%.2f", props.getVolume())).append(" mm³\n\n");

            sb.append("Mass\n");
            sb.append("  Mass: ").append(String.format("%.3f", props.getMass())).append(" g\n\n");

            sb.append("Centroid\n");
            sb.append("  X: ").append(String.format("%.3f", props.getCentroid().getX())).append(" ").append(unit)
                    .append("\n");
            sb.append("  Y: ").append(String.format("%.3f", props.getCentroid().getY())).append(" ").append(unit)
                    .append("\n");
            // Z centroid is at the midpoint of the extrusion thickness
            double zCentroid = thickness / 2.0;
            sb.append("  Z: ").append(String.format("%.3f", zCentroid)).append(" ").append(unit)
                    .append(" (at mid-thickness)\n");

            sb.append("\n").append("=".repeat(40));

            resultsArea.setText(sb.toString());

        } catch (NumberFormatException ex) {
            showError("Invalid thickness value. Please enter a number.");
        }
    }

    private void displayPrimitiveResults() {
        if (precomputedProps == null) {
            resultsArea.setText("Error: No mass properties available.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        String unit = sketch.getUnitSystem().getAbbreviation();

        sb.append("=".repeat(40)).append("\n");
        sb.append("MASS PROPERTIES (PRIMITIVE)\n");
        sb.append("=".repeat(40)).append("\n\n");

        sb.append("Shape Type: ").append(cad.core.Geometry.getCurrentShape()).append("\n");
        sb.append("Parameter: ").append(String.format("%.2f", cad.core.Geometry.getParam()))
                .append(" ").append(unit).append("\n\n");

        sb.append("Material\n");
        sb.append("  Name: ").append(precomputedProps.getMaterial().getName()).append("\n");
        sb.append("  Density: ").append(String.format("%.1f", precomputedProps.getMaterial().getDensity()))
                .append(" kg/m³\n");
        sb.append("  Category: ").append(precomputedProps.getMaterial().getCategory()).append("\n\n");

        sb.append("Geometry\n");
        sb.append("  Volume: ").append(String.format("%.2f", precomputedProps.getVolume())).append(" mm³\n\n");

        sb.append("Mass\n");
        sb.append("  Mass: ").append(String.format("%.3f", precomputedProps.getMass())).append(" g\n\n");

        sb.append("Centroid: Origin (0, 0, 0)\n");

        sb.append("\n").append("=".repeat(40));

        resultsArea.setText(sb.toString());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
