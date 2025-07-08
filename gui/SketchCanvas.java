package gui;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import core.Sketch;

public class SketchCanvas extends JPanel {
    private Sketch sketch;

    public SketchCanvas(Sketch sketch) {
        this.sketch = sketch;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        sketch.draw(g);
    }

    public void refresh() {
        repaint();
    }

    public static void main(String[] args) {
        Sketch sketch = new Sketch();

        // Create and show a file chooser dialog to pick a DXF file
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                sketch.loadDXF(filePath);
            } catch (IOException e) {
                System.err.println("Failed to load DXF: " + e.getMessage());
                JOptionPane.showMessageDialog(null, "Failed to load DXF: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } else {
            System.out.println("No file selected, exiting.");
            System.exit(0);
        }

        JFrame frame = new JFrame("Sketch Viewer");
        SketchCanvas canvas = new SketchCanvas(sketch);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
