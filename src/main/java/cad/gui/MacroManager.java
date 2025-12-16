package cad.gui;

import cad.core.MacroRecorder;
import cad.core.Sketch;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Macro manager for GUI - handles loading, executing, and managing macros.
 */
public class MacroManager {

    private final MacroRecorder recorder;
    private final Sketch sketch;
    private final Stage primaryStage;

    public MacroManager(Sketch sketch, Stage primaryStage) {
        this.recorder = new MacroRecorder();
        this.sketch = sketch;
        this.primaryStage = primaryStage;
    }

    /**
     * Show file chooser to upload and run a macro file.
     */
    public void uploadAndRunMacro() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Upload Macro File");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        // Set file extension filters
        FileChooser.ExtensionFilter macroFilter = new FileChooser.ExtensionFilter(
                "Macro Files (*.macro)", "*.macro");
        FileChooser.ExtensionFilter allFilter = new FileChooser.ExtensionFilter(
                "All Files (*.*)", "*.*");
        fileChooser.getExtensionFilters().addAll(macroFilter, allFilter);
        fileChooser.setSelectedExtensionFilter(macroFilter);

        // Show open dialog
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {
            // Copy file to macros directory if not already there
            File macrosDir = new File("macros");
            if (!macrosDir.exists()) {
                macrosDir.mkdirs();
            }

            try {
                String macroName = selectedFile.getName().replace(".macro", "");
                File targetFile = new File(macrosDir, selectedFile.getName());

                // If file is not in macros directory, copy it
                if (!selectedFile.getParentFile().equals(macrosDir)) {
                    java.nio.file.Files.copy(
                            selectedFile.toPath(),
                            targetFile.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    showInfo("Macro Uploaded",
                            "Macro '" + macroName + "' uploaded successfully.\nRunning now...");
                }

                // Run the macro
                runMacro(macroName);

            } catch (IOException e) {
                showError("Upload Failed", "Could not upload macro file: " + e.getMessage());
            }
        }
    }

    /**
     * Show dialog to select and run an existing macro.
     */
    public void selectAndRunMacro() {
        List<String> macros = recorder.listMacros();

        if (macros.isEmpty()) {
            showInfo("No Macros", "No macros found in the macros/ directory.\n\n" +
                    "You can:\n" +
                    "1. Record a new macro using the Record button\n" +
                    "2. Upload a macro file using the Upload Macro option");
            return;
        }

        // Create selection dialog
        javafx.scene.control.ChoiceDialog<String> dialog = new javafx.scene.control.ChoiceDialog<>(macros.get(0),
                macros);
        dialog.setTitle("Run Macro");
        dialog.setHeaderText("Select a macro to run");
        dialog.setContentText("Available macros:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(this::runMacro);
    }

    /**
     * Execute a macro by name.
     */
    public void runMacro(String macroName) {
        try {
            List<MacroRecorder.MacroCommand> commands = recorder.loadMacro(macroName);

            if (commands.isEmpty()) {
                showWarning("Empty Macro", "The macro '" + macroName + "' contains no commands.");
                return;
            }

            // Confirm execution
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Run Macro");
            confirm.setHeaderText("Execute macro: " + macroName);
            confirm.setContentText("This macro contains " + commands.size() + " command(s).\n\n" +
                    "The macro will execute automatically.\nContinue?");

            Optional<ButtonType> confirmResult = confirm.showAndWait();
            if (confirmResult.isPresent() && confirmResult.get() == ButtonType.OK) {
                executeMacro(commands, macroName);
            }

        } catch (IOException e) {
            showError("Macro Error", "Could not load macro '" + macroName + "':\n" + e.getMessage());
        }
    }

    /**
     * Execute macro commands.
     */
    private void executeMacro(List<MacroRecorder.MacroCommand> commands, String macroName) {
        int successCount = 0;
        int failCount = 0;

        System.out.println("Executing macro: " + macroName);

        for (int i = 0; i < commands.size(); i++) {
            MacroRecorder.MacroCommand cmd = commands.get(i);
            System.out.println("  [" + (i + 1) + "/" + commands.size() + "] " + cmd.action);

            try {
                executeCommand(cmd);
                successCount++;
            } catch (Exception e) {
                System.err.println("  ERROR: " + e.getMessage());
                failCount++;
            }
        }

        // Show results
        String message = String.format(
                "Macro execution complete:\n\n" +
                        "✓ Successful: %d\n" +
                        "✗ Failed: %d\n" +
                        "Total: %d",
                successCount, failCount, commands.size());

        if (failCount == 0) {
            showInfo("Macro Complete", message);
        } else {
            showWarning("Macro Complete (with errors)", message);
        }
    }

    /**
     * Execute a single macro command.
     */
    private void executeCommand(MacroRecorder.MacroCommand cmd) throws Exception {
        switch (cmd.action) {
            case SET_UNITS:
                String units = (String) cmd.parameters.get("units");
                // Set units in sketch
                break;

            case CLEAR_SKETCH:
                sketch.clearSketch();
                break;

            case SKETCH_POINT:
                float px = Float.parseFloat((String) cmd.parameters.get("x"));
                float py = Float.parseFloat((String) cmd.parameters.get("y"));
                sketch.addPoint(px, py);
                break;

            case SKETCH_LINE:
                float x1 = Float.parseFloat((String) cmd.parameters.get("x1"));
                float y1 = Float.parseFloat((String) cmd.parameters.get("y1"));
                float x2 = Float.parseFloat((String) cmd.parameters.get("x2"));
                float y2 = Float.parseFloat((String) cmd.parameters.get("y2"));
                sketch.addLine(x1, y1, x2, y2);
                break;

            case SKETCH_CIRCLE:
                float cx = Float.parseFloat((String) cmd.parameters.get("x"));
                float cy = Float.parseFloat((String) cmd.parameters.get("y"));
                float radius = Float.parseFloat((String) cmd.parameters.get("radius"));
                sketch.addCircle(cx, cy, radius);
                break;

            case SKETCH_POLYGON:
                float pcx = Float.parseFloat((String) cmd.parameters.get("centerX"));
                float pcy = Float.parseFloat((String) cmd.parameters.get("centerY"));
                float prad = Float.parseFloat((String) cmd.parameters.get("radius"));
                int sides = Integer.parseInt((String) cmd.parameters.get("sides"));
                sketch.addNSidedPolygon(pcx, pcy, prad, sides);
                break;

            // Add more command handlers as needed

            default:
                System.out.println("  (Command not implemented: " + cmd.action + ")");
        }
    }

    /**
     * Start recording a new macro.
     */
    public void startRecording() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Record Macro");
        dialog.setHeaderText("Start Recording");
        dialog.setContentText("Enter macro name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                recorder.startRecording(name.trim());
                showInfo("Recording Started", "Recording macro: " + name);
            }
        });
    }

    /**
     * Stop recording current macro.
     */
    public void stopRecording() {
        if (!recorder.isRecording()) {
            showWarning("Not Recording", "No macro recording is in progress.");
            return;
        }

        String macroName = recorder.getCurrentMacroName();
        int commandCount = recorder.getCommandCount();

        recorder.stopRecording();

        showInfo("Recording Stopped",
                "Macro '" + macroName + "' saved with " + commandCount + " command(s).");
    }

    /**
     * Record a command during macro recording.
     */
    public void recordCommand(MacroRecorder.ActionType action,
            java.util.Map<String, Object> params) {
        recorder.recordCommand(action, params);
    }

    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return recorder.isRecording();
    }

    // Helper methods for dialogs

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
