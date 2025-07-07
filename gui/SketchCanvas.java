package gui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import core.Sketch;

/**
 * A canvas panel that renders a Sketch object.
 */
public class SketchCanvas extends JPanel {
    private final Sketch sketch;

    public SketchCanvas(Sketch sketch) {
        this.sketch = sketch;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sketch != null) {
            sketch.draw(g);
        }
    }

    /**
     * Triggers a repaint of the canvas.
     */
    public void refresh() {
        repaint();
    }

    /**
     * Standalone launcher for viewing a DXF file in a window.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Sketch sketch = new Sketch();

            JFileChooser fileChooser = new JFileChooser();
            int result = fileChooser.showOpenDialog(null);

            if (result != JFileChooser.APPROVE_OPTION) {
                System.out.println("No file selected, exiting.");
                System.exit(0);
                return;
            }

            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            try {
                sketch.loadDXF(filePath);
            } catch (IOException e) {
                String message = "Failed to load DXF: " + e.getMessage();
                System.err.println(message);
                JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
                return;
            }

            JFrame frame = new JFrame("Sketch Viewer");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.getContentPane().add(new SketchCanvas(sketch));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
