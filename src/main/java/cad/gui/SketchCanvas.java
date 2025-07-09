package cad.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;
import cad.core.Sketch;

/**
 * A JPanel subclass that renders a Sketch object or STL geometry.
 * Provides a simple viewer for DXF sketches and STL files loaded from files.
 */
public class SketchCanvas extends JPanel {
    private Sketch sketch;
    // STL geometry: each triangle is float[9] (3 vertices x 3 coords)
    private List<float[]> stlTriangles = new ArrayList<>();
    // View transform
    private double offsetX = 0, offsetY = 0, zoom = 1.0;
    private double rotateX = 0, rotateY = 0; // For 3D rotation
    private boolean showStl = false;

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
        // Mouse drag for pan
        MouseAdapter ma = new MouseAdapter() {
            private int lastX, lastY;
            private boolean dragging = false;
            @Override
            public void mousePressed(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
                dragging = true;
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragging = false;
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging) {
                    offsetX += (e.getX() - lastX);
                    offsetY += (e.getY() - lastY);
                    lastX = e.getX();
                    lastY = e.getY();
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        // Mouse wheel for zoom
        addMouseWheelListener(e -> {
            double factor = (e.getPreciseWheelRotation() < 0) ? 1.1 : 0.9;
            zoom *= factor;
            repaint();
        });
        // Keyboard for rotation
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> { rotateY -= 0.1; repaint(); }
                    case KeyEvent.VK_RIGHT -> { rotateY += 0.1; repaint(); }
                    case KeyEvent.VK_UP -> { rotateX -= 0.1; repaint(); }
                    case KeyEvent.VK_DOWN -> { rotateX += 0.1; repaint(); }
                    case KeyEvent.VK_R -> { resetView(); repaint(); }
                }
            }
        });
    }

    // Call this to set STL triangles and switch to STL view
    public void setStlTriangles(List<float[]> triangles) {
        this.stlTriangles = triangles;
        this.showStl = true;
        resetView();
        repaint();
    }
    // Call this to switch back to sketch view
    public void showSketch() {
        this.showStl = false;
        repaint();
    }
    private void resetView() {
        offsetX = getWidth() / 2.0;
        offsetY = getHeight() / 2.0;
        zoom = 1.0;
        rotateX = 0;
        rotateY = 0;
    }

    /**
     * Override paintComponent to draw the sketch on the canvas.
     * 
     * @param g The Graphics context.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (showStl && !stlTriangles.isEmpty()) {
            drawStl((Graphics2D) g);
        } else {
            sketch.draw(g);
        }
    }

    // Simple 3D to 2D projection and drawing for STL triangles
    private void drawStl(Graphics2D g) {
        g.setColor(Color.BLUE);
        for (float[] tri : stlTriangles) {
            int[] x = new int[3];
            int[] y = new int[3];
            for (int i = 0; i < 3; i++) {
                double[] p = project(tri[i * 3], tri[i * 3 + 1], tri[i * 3 + 2]);
                x[i] = (int) (p[0] * zoom + offsetX);
                y[i] = (int) (p[1] * zoom + offsetY);
            }
            g.drawPolygon(x, y, 3);
        }
    }
    // Simple 3D rotation and orthographic projection
    private double[] project(float x, float y, float z) {
        // Rotate around X axis
        double sinX = Math.sin(rotateX), cosX = Math.cos(rotateX);
        double sinY = Math.sin(rotateY), cosY = Math.cos(rotateY);
        double y1 = y * cosX - z * sinX;
        double z1 = y * sinX + z * cosX;
        // Rotate around Y axis
        double x2 = x * cosY + z1 * sinY;
        double z2 = -x * sinY + z1 * cosY;
        // Orthographic projection (ignore z2)
        return new double[] { x2, y1 };
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
    /*public static void main(String[] args) {
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
*/
}
