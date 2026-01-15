package cad.aerodynamics;

import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import cad.analysis.FluidDynamics;
import cad.analysis.FlowVisualizer;

public class CfdDialog {

    private Stage dialog;
    private Runnable outputCallback;

    public CfdDialog() {
    }

    public void setOutputCallback(Runnable callback) {
        this.outputCallback = callback;
    }

    public void show() {
        dialog = new Stage();
        dialog.setTitle("CFD Analysis Dashboard");
        dialog.initModality(Modality.NONE);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField velocityField = new TextField("50.0");
        TextField aoaField = new TextField("0.0");
        TextField chordField = new TextField("1.0");
        TextField spanField = new TextField("1.0");
        TextField densityField = new TextField(String.valueOf(FluidDynamics.getStandardDensity()));

        grid.add(new Label("Velocity (m/s):"), 0, 0);
        grid.add(velocityField, 1, 0);
        grid.add(new Label("Angle of Attack (deg):"), 0, 1);
        grid.add(aoaField, 1, 1);
        grid.add(new Label("Chord Length (m):"), 0, 2);
        grid.add(chordField, 1, 2);
        grid.add(new Label("Span (m):"), 0, 3);
        grid.add(spanField, 1, 3);
        grid.add(new Label("Air Density (kg/m3):"), 0, 4);
        grid.add(densityField, 1, 4);

        CheckBox showFlowCheckbox = new CheckBox("Show Flow Streamlines");
        showFlowCheckbox.setSelected(true);
        grid.add(showFlowCheckbox, 0, 5, 2, 1);

        TextArea resultsArea = new TextArea();
        resultsArea.setEditable(false);
        resultsArea.setPrefHeight(100);

        Button analyzeBtn = new Button("Run Analysis");
        analyzeBtn.setMaxWidth(Double.MAX_VALUE);
        analyzeBtn.setOnAction(e -> {
            try {
                double v = Double.parseDouble(velocityField.getText());
                double alpha = Double.parseDouble(aoaField.getText());
                double c = Double.parseDouble(chordField.getText());
                double s = Double.parseDouble(spanField.getText());
                double rho = Double.parseDouble(densityField.getText());

                FluidDynamics.AnalysisResult result = FluidDynamics.analyzeAirfoil(v, c, s, alpha, rho);

                StringBuilder sb = new StringBuilder();
                sb.append("--- Analysis Results ---\n");
                sb.append(String.format("Lift Coef (Cl): %.4f\n", result.liftCoef));
                sb.append(String.format("Drag Coef (Cd): %.4f\n", result.dragCoef));
                sb.append(String.format("Lift Force:     %.2f N\n", result.liftForce));
                sb.append(String.format("Drag Force:     %.2f N\n", result.dragForce));
                sb.append(String.format("Reynolds No:    %.2e\n", result.reynoldsNumber));

                resultsArea.setText(sb.toString());

                if (showFlowCheckbox.isSelected()) {
                    FlowVisualizer.showStreamlines(c, alpha, v);
                }

                if (outputCallback != null) {
                    outputCallback.run();
                }
            } catch (Exception ex) {
                resultsArea.setText("Error: " + ex.getMessage());
            }
        });

        grid.add(analyzeBtn, 0, 6, 2, 1);
        grid.add(resultsArea, 0, 7, 2, 1);

        dialog.setScene(new Scene(grid, 350, 450));
        dialog.show();
    }
}
