package cad.gui;

import cad.core.Material;
import cad.core.MaterialDatabase;
import cad.core.Sketch;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class MaterialDatabaseDialog extends Dialog<ButtonType> {

    private TableView<Material> materialTable;
    private MaterialDatabase database;
    private ComboBox<String> categoryFilter;
    private Sketch sketch;
    private Runnable onMaterialChanged; // Callback to refresh 3D view
    private java.util.function.Consumer<String> consoleLogger; // Callback to log to in-app console

    public MaterialDatabaseDialog(Sketch sketch, Runnable onMaterialChanged,
            java.util.function.Consumer<String> consoleLogger) {
        this.database = MaterialDatabase.getInstance();
        this.sketch = sketch;
        this.onMaterialChanged = onMaterialChanged;
        this.consoleLogger = consoleLogger;

        setTitle("Material Database Manager");
        setHeaderText("Manage Engineering Materials");

        // Create the dialog content
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setPrefSize(700, 500);

        // Filter controls
        HBox filterBox = new HBox(10);
        filterBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label filterLabel = new Label("Filter by Category:");
        categoryFilter = new ComboBox<>();
        categoryFilter.getItems().addAll("All", "Metals", "Plastics", "Composites",
                "Wood", "Ceramics", "Construction", "Elastomers", "Other");
        categoryFilter.setValue("All");
        categoryFilter.setOnAction(e -> refreshTable());
        filterBox.getChildren().addAll(filterLabel, categoryFilter);

        // Table
        materialTable = createMaterialTable();
        VBox.setVgrow(materialTable, Priority.ALWAYS);

        // Set Material button (to assign to sketch)
        Button setMaterialButton = new Button("Set as Active Material");
        setMaterialButton.setMaxWidth(Double.MAX_VALUE);
        setMaterialButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        setMaterialButton.setOnAction(e -> setMaterial());
        setMaterialButton.disableProperty().bind(materialTable.getSelectionModel().selectedItemProperty().isNull());

        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button addButton = new Button("Add Material");
        addButton.setOnAction(e -> addMaterial());

        Button editButton = new Button("Edit");
        editButton.setOnAction(e -> editMaterial());
        editButton.disableProperty().bind(materialTable.getSelectionModel().selectedItemProperty().isNull());

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(e -> deleteMaterial());
        deleteButton.disableProperty().bind(materialTable.getSelectionModel().selectedItemProperty().isNull());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton);

        content.getChildren().addAll(filterBox, materialTable, setMaterialButton, buttonBox);

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Initialize table
        refreshTable();
    }

    private TableView<Material> createMaterialTable() {
        TableView<Material> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Name column
        TableColumn<Material, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));
        nameCol.setPrefWidth(200);

        // Density column
        TableColumn<Material, String> densityCol = new TableColumn<>("Density (kg/m³)");
        densityCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.format("%.1f", data.getValue().getDensity())));
        densityCol.setPrefWidth(150);

        // Category column
        TableColumn<Material, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory()));
        categoryCol.setPrefWidth(120);

        // Description column
        TableColumn<Material, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        descCol.setPrefWidth(230);

        table.getColumns().addAll(nameCol, densityCol, categoryCol, descCol);

        return table;
    }

    private void refreshTable() {
        String filter = categoryFilter.getValue();
        ObservableList<Material> materials;

        if ("All".equals(filter)) {
            materials = FXCollections.observableArrayList(database.getAllMaterials());
        } else {
            materials = FXCollections.observableArrayList(database.getMaterialsByCategory(filter));
        }

        materialTable.setItems(materials);
    }

    private void addMaterial() {
        MaterialEditorDialog editor = new MaterialEditorDialog(null);
        editor.showAndWait().ifPresent(material -> {
            if (database.addMaterial(material)) {
                refreshTable();
            } else {
                showError("Material with name '" + material.getName() + "' already exists.");
            }
        });
    }

    private void editMaterial() {
        Material selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        MaterialEditorDialog editor = new MaterialEditorDialog(selected);
        editor.showAndWait().ifPresent(material -> {
            database.updateMaterial(selected.getName(), material);
            refreshTable();
        });
    }

    private void deleteMaterial() {
        Material selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Material");
        confirm.setContentText("Are you sure you want to delete '" + selected.getName() + "'?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                database.removeMaterial(selected.getName());
                refreshTable();
            }
        });
    }

    private void setMaterial() {
        Material selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null || sketch == null)
            return;

        sketch.setMaterial(selected);

        // Trigger 3D view refresh immediately
        if (onMaterialChanged != null) {
            onMaterialChanged.run();
        }

        // Log to in-app console
        if (consoleLogger != null) {
            consoleLogger.accept(
                    "Material set to: " + selected.getName() + " (Density: " + selected.getDensity() + " kg/m³)");
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class MaterialEditorDialog extends Dialog<Material> {
        private TextField nameField;
        private TextField densityField;
        private ComboBox<String> categoryCombo;
        private TextArea descriptionArea;

        public MaterialEditorDialog(Material material) {
            setTitle(material == null ? "Add Material" : "Edit Material");
            setHeaderText(material == null ? "Create New Material" : "Edit Material Properties");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            nameField = new TextField();
            nameField.setPromptText("e.g., Steel 1020");
            if (material != null)
                nameField.setText(material.getName());

            densityField = new TextField();
            densityField.setPromptText("e.g., 7850");
            if (material != null)
                densityField.setText(String.valueOf(material.getDensity()));

            categoryCombo = new ComboBox<>();
            categoryCombo.getItems().addAll("Metals", "Plastics", "Wood", "Other");
            categoryCombo.setValue(material != null ? material.getCategory() : "Metals");

            descriptionArea = new TextArea();
            descriptionArea.setPromptText("Optional description");
            descriptionArea.setPrefRowCount(3);
            if (material != null)
                descriptionArea.setText(material.getDescription());

            grid.add(new Label("Name:"), 0, 0);
            grid.add(nameField, 1, 0);
            grid.add(new Label("Density (kg/m³):"), 0, 1);
            grid.add(densityField, 1, 1);
            grid.add(new Label("Category:"), 0, 2);
            grid.add(categoryCombo, 1, 2);
            grid.add(new Label("Description:"), 0, 3);
            grid.add(descriptionArea, 1, 3);

            getDialogPane().setContent(grid);
            getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Enable/disable OK button
            Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
            okButton.setDisable(true);

            nameField.textProperty().addListener((obs, old, newVal) -> okButton
                    .setDisable(newVal.trim().isEmpty() || densityField.getText().trim().isEmpty()));
            densityField.textProperty().addListener((obs, old, newVal) -> okButton
                    .setDisable(newVal.trim().isEmpty() || nameField.getText().trim().isEmpty()));

            // Convert result
            setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    try {
                        String name = nameField.getText().trim();
                        double density = Double.parseDouble(densityField.getText().trim());
                        String category = categoryCombo.getValue();
                        String description = descriptionArea.getText().trim();

                        return new Material(name, density, category, description);
                    } catch (NumberFormatException e) {
                        showError("Invalid density value. Please enter a number.");
                        return null;
                    }
                }
                return null;
            });
        }

        private void showError(String message) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
}
