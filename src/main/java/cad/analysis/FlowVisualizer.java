package cad.analysis;

import org.jzy3d.chart.Chart;
import org.jzy3d.chart.factories.AWTChartFactory;
import org.jzy3d.colors.Color;
import org.jzy3d.maths.Coord3d;
import org.jzy3d.plot3d.primitives.LineStrip;
import org.jzy3d.plot3d.primitives.Point;

import java.util.ArrayList;
import java.util.List;

public class FlowVisualizer {

    public static void showStreamlines(double chord, double alpha, double velocity) {

        AWTChartFactory factory = new AWTChartFactory();
        Chart chart = factory.newChart();
        chart.getView().setBackgroundColor(Color.WHITE);

        int numStreamlines = 30;
        double alphaRad = Math.toRadians(alpha);
        double vinf = velocity;

        for (int i = 0; i < numStreamlines; i++) {
            double t = i / (double) (numStreamlines - 1);
            double yInit = (t - 0.5) * chord * 2.0;

            List<Coord3d> points = new ArrayList<>();

            double x = -chord * 2.0;
            double y = yInit;
            double z = 0.0;

            for (int step = 0; step < 400; step++) {
                double r = Math.sqrt(x * x + y * y);

                if (r < chord * 0.03) {
                    break;
                }

                points.add(new Coord3d(x, y, z));

                double theta = Math.atan2(y, x);
                double gamma = 5.0 * Math.PI * vinf * chord * Math.sin(alphaRad);
                double a = chord * 0.10;

                double vr = vinf * Math.cos(alphaRad - theta) * (1.0 - (a * a) / (r * r));
                double vtheta = -vinf * Math.sin(alphaRad - theta) * (1.0 + (a * a) / (r * r))
                        + gamma / (2.0 * Math.PI * r);

                double vx = vr * Math.cos(theta) - vtheta * Math.sin(theta);
                double vy = vr * Math.sin(theta) + vtheta * Math.cos(theta);

                double speed = Math.sqrt(vx * vx + vy * vy);
                double dt = chord * 0.01 / Math.max(speed, vinf * 0.1);

                x += vx * dt;
                y += vy * dt;

                if (x > chord * 4.0 || Math.abs(y) > chord * 3.0) {
                    break;
                }
            }

            if (points.size() > 15) {
                LineStrip line = new LineStrip();
                for (Coord3d point : points) {
                    Point p = new Point(point, Color.CYAN, 2.0f);
                    line.add(p);
                }
                line.setWireframeColor(Color.CYAN);
                line.setWireframeDisplayed(true);
                line.setWidth(2);
                chart.add(line);
            }
        }

        chart.getView().setViewPoint(new Coord3d(0, -5 * chord, 2 * chord));
        chart.open("Flow Visualization - AoA: " + alpha + "°", 800, 600);
    }
}
