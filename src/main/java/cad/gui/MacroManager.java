package cad.gui;

import cad.core.MacroRecorder;
import cad.core.Sketch;
import cad.core.Geometry;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;


public class MacroManager {

    private final MacroRecorder recorder;
    private final Sketch sketch;
    private final Stage primaryStage;
    private Runnable canvasRefresh;
    private java.util.function.Consumer<String> outputCallback;
    private Runnable viewModeCallback;
    private Runnable fitViewCallback;
    private java.util.function.Consumer<List<float[]>> stlUpdateCallback;

    public MacroManager(Sketch sketch, Stage primaryStage) {
        this.recorder = new MacroRecorder();
        this.sketch = sketch;
        this.primaryStage = primaryStage;
        this.canvasRefresh = null; // Will be set by GuiFX
        this.outputCallback = null; // Will be set by GuiFX
        this.viewModeCallback = null; // Will be set by GuiFX
        this.fitViewCallback = null; // Will be set by GuiFX
        this.stlUpdateCallback = null; // Will be set by GuiFX
    }

    
    public void setCanvasRefresh(Runnable canvasRefresh) {
        this.canvasRefresh = canvasRefresh;
    }

    
    public void setOutputCallback(java.util.function.Consumer<String> outputCallback) {
        this.outputCallback = outputCallback;
    }

    
    public void setViewModeCallback(Runnable viewModeCallback) {
        this.viewModeCallback = viewModeCallback;
    }

    
    public void setFitViewCallback(Runnable fitViewCallback) {
        this.fitViewCallback = fitViewCallback;
    }

    
    public void setStlUpdateCallback(java.util.function.Consumer<List<float[]>> stlUpdateCallback) {
        this.stlUpdateCallback = stlUpdateCallback;
    }

    
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

    
    private void executeMacro(List<MacroRecorder.MacroCommand> commands, String macroName) {
        int successCount = 0;
        int failCount = 0;

        // Switch to sketch view mode to show 2D geometry
        if (viewModeCallback != null) {
            viewModeCallback.run();
        }

        log("Executing macro: " + macroName);

        for (int i = 0; i < commands.size(); i++) {
            MacroRecorder.MacroCommand cmd = commands.get(i);
            log("  [" + (i + 1) + "/" + commands.size() + "] " + cmd.action);

            try {
                executeCommand(cmd);
                successCount++;
                log("    ✓ Success");

                // Refresh canvas after each command to show progress
                if (canvasRefresh != null) {
                    canvasRefresh.run();
                }
            } catch (Exception e) {
                log("    ✗ ERROR: " + e.getMessage());
                failCount++;
            }
        }

        // Final canvas refresh
        if (canvasRefresh != null) {
            canvasRefresh.run();
        }

        // Auto-zoom to fit the generated geometry
        if (fitViewCallback != null) {
            fitViewCallback.run();
        }

        // Log results
        if (failCount == 0) {
            log(macroName + " execution completed successfully.");
        } else {
            log(macroName + " completed with " + failCount + " errors.");
        }
    }

    
    private void executeCommand(MacroRecorder.MacroCommand cmd) throws Exception {
        switch (cmd.action) {
            case SET_UNITS:
                String units = (String) cmd.parameters.get("units");
                // Set units in sketch
                break;

            case CLEAR_SKETCH:
                // Only clear 2D entities, keep 3D geometry for multi-step macros
                sketch.clearSketch();
                break;

            case CLEAR_ALL:
                sketch.clearAll();
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

            case EXTRUDE:
                float depth = Float.parseFloat((String) cmd.parameters.get("depth"));
                // Check if cut parameter exists
                boolean cut = false;
                if (cmd.parameters.containsKey("cut")) {
                    cut = Boolean.parseBoolean((String) cmd.parameters.get("cut"));
                }

                sketch.extrude(depth);

                // Get extruded geometry and update renderer
                List<float[]> extrudedTriangles = sketch.getExtrudedTriangles();
                if (stlUpdateCallback != null && extrudedTriangles != null) {
                    stlUpdateCallback.accept(extrudedTriangles);
                }
                break;

            case SAVE_FILE:
                String filename = (String) cmd.parameters.get("filename");
                File file = new File(filename);
                // If relative path, save to user home or current dir
                Geometry.saveStl(file.getAbsolutePath());
                break;

            case SKETCH_NACA:
                String profile = (String) cmd.parameters.get("profile");
                float chord = Float.parseFloat((String) cmd.parameters.get("chord"));
                generateNacaProfile(profile, chord);
                break;

            case REVOLVE:
                String axis = (String) cmd.parameters.get("axis");
                float angle = Float.parseFloat((String) cmd.parameters.get("angle"));

                // Reuse revolve logic from Geometry
                // Geometry.revolve takes (Sketch, angle, segments)
                Geometry.revolve(sketch, angle, 36);

                // Get revolved geometry
                List<float[]> revolvedTriangles = sketch.getExtrudedTriangles(); // Revolve also populates this
                if (stlUpdateCallback != null && revolvedTriangles != null) {
                    stlUpdateCallback.accept(revolvedTriangles);
                }
                break;

            default:
                log("    (Command not implemented: " + cmd.action + ")");
        }
    }

    private void generateNacaProfile(String profile, float chord) {
        if (profile == null || profile.length() != 4) {
            log("Invalid NACA profile: " + profile);
            return;
        }

        try {
            int mInt = Integer.parseInt(profile.substring(0, 1));
            int pInt = Integer.parseInt(profile.substring(1, 2));
            int tInt = Integer.parseInt(profile.substring(2, 4));

            double m = mInt / 100.0;
            double p = pInt / 10.0;
            double t = tInt / 100.0;

            int numPoints = 100; // Points per side
            List<Sketch.PointEntity> points = new java.util.ArrayList<>();

            // Generate top and bottom surfaces
            // Using cosine spacing for better distribution at LE/TE
            for (int i = 0; i <= numPoints; i++) {
                double beta = Math.PI * i / numPoints;
                double x = (1.0 - Math.cos(beta)) / 2.0;

                // Thickness distribution
                double yt = 5 * t * (0.2969 * Math.sqrt(x) - 0.1260 * x - 0.3516 * Math.pow(x, 2)
                        + 0.2843 * Math.pow(x, 3) - 0.1015 * Math.pow(x, 4));

                // Camber line and gradient
                double yc = 0;
                double dyc_dx = 0;

                if (m == 0) {
                    // Symmetric
                    yc = 0;
                    dyc_dx = 0;
                } else {
                    if (x < p) {
                        yc = (m / (p * p)) * (2 * p * x - x * x);
                        dyc_dx = (2 * m / (p * p)) * (p - x);
                    } else {
                        yc = (m / ((1 - p) * (1 - p))) * ((1 - 2 * p) + 2 * p * x - x * x);
                        dyc_dx = (2 * m / ((1 - p) * (1 - p))) * (p - x);
                    }
                }

                double theta = Math.atan(dyc_dx);

                double xu = x - yt * Math.sin(theta);
                double yu = yc + yt * Math.cos(theta);

                points.add(new Sketch.PointEntity((float) (xu * chord), (float) (yu * chord)));
            }

            // Bottom surface (backwards from TE to LE)
            for (int i = numPoints - 1; i >= 0; i--) {
                double beta = Math.PI * i / numPoints;
                double x = (1.0 - Math.cos(beta)) / 2.0;

                double yt = 5 * t * (0.2969 * Math.sqrt(x) - 0.1260 * x - 0.3516 * Math.pow(x, 2)
                        + 0.2843 * Math.pow(x, 3) - 0.1015 * Math.pow(x, 4));

                // Camber line and gradient (same as above)
                double yc = 0;
                double dyc_dx = 0;

                if (m != 0) {
                    if (x < p) {
                        yc = (m / (p * p)) * (2 * p * x - x * x);
                        dyc_dx = (2 * m / (p * p)) * (p - x);
                    } else {
                        yc = (m / ((1 - p) * (1 - p))) * ((1 - 2 * p) + 2 * p * x - x * x);
                        dyc_dx = (2 * m / ((1 - p) * (1 - p))) * (p - x);
                    }
                }

                double theta = Math.atan(dyc_dx);

                double xl = x + yt * Math.sin(theta);
                double yl = yc - yt * Math.cos(theta);

                points.add(new Sketch.PointEntity((float) (xl * chord), (float) (yl * chord)));
            }

            // Create polygon
            sketch.addPolygon(points);
            log("    Generated NACA " + profile + " polygon with " + points.size() + " points");

        } catch (Exception e) {
            log("Error generating NACA profile: " + e.getMessage());
        }
    }

    
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

    
    public void recordCommand(MacroRecorder.ActionType action,
            java.util.Map<String, Object> params) {
        recorder.recordCommand(action, params);
    }

    
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

    
    private void log(String message) {
        System.out.println(message);
        if (outputCallback != null) {
            outputCallback.accept(message);
        }
    }
}
