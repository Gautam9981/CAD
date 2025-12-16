package cad.core;

import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Macro recording and playback system for GUI automation and testing.
 * Allows recording user actions and replaying them for testing or batch
 * operations.
 */
public class MacroRecorder {

    private boolean isRecording = false;
    private List<MacroCommand> commands = new ArrayList<>();
    private String currentMacroName = "";
    private static final String MACRO_DIR = "macros/";

    public enum ActionType {
        SKETCH_POINT,
        SKETCH_LINE,
        SKETCH_CIRCLE,
        SKETCH_POLYGON,
        CREATE_CUBE,
        CREATE_SPHERE,
        EXTRUDE,
        REVOLVE,
        ADD_CONSTRAINT,
        SET_UNITS,
        SAVE_FILE,
        CLEAR_SKETCH,
        UNDO,
        REDO
    }

    public static class MacroCommand {
        public ActionType action;
        public Map<String, Object> parameters;
        public String timestamp;

        public MacroCommand(ActionType action, Map<String, Object> params) {
            this.action = action;
            this.parameters = params;
            this.timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        }

        public String toScriptLine() {
            StringBuilder sb = new StringBuilder();
            sb.append(action.name()).append(" ");
            parameters.forEach((key, value) -> sb.append(key).append("=").append(value).append(" "));
            return sb.toString().trim();
        }

        public static MacroCommand fromScriptLine(String line) {
            String[] parts = line.split(" ", 2);
            ActionType action = ActionType.valueOf(parts[0]);
            Map<String, Object> params = new HashMap<>();

            if (parts.length > 1) {
                String[] paramPairs = parts[1].split(" ");
                for (String pair : paramPairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0], kv[1]);
                    }
                }
            }

            MacroCommand cmd = new MacroCommand(action, params);
            return cmd;
        }
    }

    /**
     * Start recording a new macro.
     */
    public void startRecording(String macroName) {
        this.isRecording = true;
        this.currentMacroName = macroName;
        this.commands.clear();
        System.out.println("Macro recording started: " + macroName);
    }

    /**
     * Stop recording and save the macro.
     */
    public void stopRecording() {
        if (!isRecording) {
            System.out.println("No recording in progress");
            return;
        }

        this.isRecording = false;

        try {
            saveMacro(currentMacroName);
            System.out.println("Macro saved: " + currentMacroName + " (" + commands.size() + " commands)");
        } catch (IOException e) {
            System.err.println("Error saving macro: " + e.getMessage());
        }
    }

    /**
     * Record a command during macro recording.
     */
    public void recordCommand(ActionType action, Map<String, Object> parameters) {
        if (!isRecording) {
            return;
        }

        MacroCommand cmd = new MacroCommand(action, parameters);
        commands.add(cmd);
        System.out.println("Recorded: " + cmd.toScriptLine());
    }

    /**
     * Save macro to file.
     */
    private void saveMacro(String name) throws IOException {
        File dir = new File(MACRO_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = MACRO_DIR + name + ".macro";
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Write header
            writer.println("# CAD Macro File");
            writer.println("# Name: " + name);
            writer.println("# Created: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            writer.println("# Commands: " + commands.size());
            writer.println();

            // Write commands
            for (MacroCommand cmd : commands) {
                writer.println(cmd.toScriptLine());
            }
        }
    }

    /**
     * Load and execute a macro file.
     */
    public List<MacroCommand> loadMacro(String name) throws IOException {
        String filename = MACRO_DIR + name + ".macro";
        List<MacroCommand> loadedCommands = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                try {
                    MacroCommand cmd = MacroCommand.fromScriptLine(line);
                    loadedCommands.add(cmd);
                } catch (Exception e) {
                    System.err.println("Error parsing macro line: " + line);
                }
            }
        }

        return loadedCommands;
    }

    /**
     * List all available macros.
     */
    public List<String> listMacros() {
        List<String> macros = new ArrayList<>();
        File dir = new File(MACRO_DIR);

        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".macro"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().replace(".macro", "");
                    macros.add(name);
                }
            }
        }

        return macros;
    }

    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Get current macro name.
     */
    public String getCurrentMacroName() {
        return currentMacroName;
    }

    /**
     * Get recorded commands count.
     */
    public int getCommandCount() {
        return commands.size();
    }
}
