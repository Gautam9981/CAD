package gui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import core.Sketch;

/**
 * A JPanel subclass that renders a Sketch object.
 * Provides a simple viewer for DXF sketches loaded from files.
 */
public class SketchCanvas extends JPanel {
    private Sketch sketch;

    /**
     * Constructor initializes the canvas with a Sketch instance.
     * Sets preferred size and background color.
     * 
     * @param sketch The Sketch object to render.
     */
    public SketchCanvas(Sketch sketch) {
        this.sketch = sketch;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
    }

    /**
     * Override paintComponent to draw the sketch on the canvas.
     * 
     * @param g The Graphics context.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Delegate drawing to the Sketch object
        sketch.draw(g);
    }

    /**
     * Refreshes the canvas by scheduling a repaint.
     */
    public void refresh() {
        repaint();
    }

    /**
     * Main method to launch the Sketch viewer application.
     * Opens a file chooser dialog to load a DXF file, then displays it.
     */
    public static void main(String[] args) {
        Sketch sketch = new Sketch();

        // Open file chooser dialog to select a DXF file
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                // Load the selected DXF file into the sketch
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                sketch.loadDXF(filePath);
            } catch (IOException e) {
                // Show error dialog if loading fails, then exit
                System.err.println("Failed to load DXF: " + e.getMessage());
                JOptionPane.showMessageDialog(null, "Failed to load DXF: " + e.getMessage(), 
                                              "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            // No file selected; exit application
            System.out.println("No file selected, exiting.");
            System.exit(0);
        }

        // Create the main application window
        JFrame frame = new JFrame("Sketch Viewer");
        SketchCanvas canvas = new SketchCanvas(sketch);

        // Setup JFrame
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);  // Center on screen
        frame.setVisible(true);
    }
}
