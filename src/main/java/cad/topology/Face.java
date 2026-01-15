package cad.topology;

import cad.geometry.surfaces.Surface;
import cad.math.Vector3d;
import java.util.ArrayList;
import java.util.List;

public class Face {
    private Surface surface;
    private EdgeLoop outerLoop;
    private List<EdgeLoop> innerLoops;
    private Vector3d normal;
    private boolean isNormalValid;

    public Face(Surface surface, EdgeLoop outerLoop) {
        this.surface = surface;
        this.outerLoop = outerLoop;
        this.innerLoops = new ArrayList<>();
        this.isNormalValid = false;
    }

    public void addInnerLoop(EdgeLoop loop) {
        innerLoops.add(loop);
        isNormalValid = false;
    }

    public Surface getSurface() {
        return surface;
    }

    public EdgeLoop getOuterLoop() {
        return outerLoop;
    }

    public List<EdgeLoop> getInnerLoops() {
        return innerLoops;
    }

    public Vector3d getNormal() {
        if (!isNormalValid) {
            calculateNormal();
        }
        return normal;
    }

    private void calculateNormal() {
        if (outerLoop != null && outerLoop.getEdges().size() >= 3) {
            List<Vertex> vertices = getVerticesFromLoop(outerLoop);
            if (vertices.size() >= 3) {
                Vector3d v1 = vertices.get(1).getPoint().subtract(vertices.get(0).getPoint());
                Vector3d v2 = vertices.get(2).getPoint().subtract(vertices.get(0).getPoint());
                normal = v1.cross(v2).normalize();
                isNormalValid = true;
                return;
            }
        }
        
        if (surface != null) {
            Vector3d center = getFaceCenter();
            normal = surface.normal(0.5, 0.5);
            isNormalValid = true;
        } else {
            normal = Vector3d.Z_AXIS;
            isNormalValid = true;
        }
    }

    private List<Vertex> getVerticesFromLoop(EdgeLoop loop) {
        List<Vertex> vertices = new ArrayList<>();
        for (Edge edge : loop.getEdges()) {
            if (!vertices.contains(edge.getStartVertex())) {
                vertices.add(edge.getStartVertex());
            }
        }
        return vertices;
    }

    private Vector3d getFaceCenter() {
        Vector3d center = Vector3d.zero();
        int count = 0;
        
        if (outerLoop != null) {
            for (Vertex vertex : getVerticesFromLoop(outerLoop)) {
                center = center.plus(vertex.getPoint());
                count++;
            }
        }
        
        return count > 0 ? center.multiply(1.0 / count) : Vector3d.zero();
    }

    public double getArea() {
        double area = 0.0;
        
        if (outerLoop != null) {
            area += calculateLoopArea(outerLoop);
        }
        
        for (EdgeLoop innerLoop : innerLoops) {
            area -= calculateLoopArea(innerLoop);
        }
        
        return Math.abs(area);
    }

    private double calculateLoopArea(EdgeLoop loop) {
        List<Vertex> vertices = getVerticesFromLoop(loop);
        if (vertices.size() < 3) return 0.0;
        
        double area = 0.0;
        Vector3d v0 = vertices.get(0).getPoint();
        for (int i = 1; i < vertices.size() - 1; i++) {
            Vector3d v1 = vertices.get(i).getPoint();
            Vector3d v2 = vertices.get(i + 1).getPoint();
            
            Vector3d a = v1.subtract(v0);
            Vector3d b = v2.subtract(v0);
            area += a.cross(b).magnitude() / 2.0;
        }
        return area;
    }

    public boolean isValid() {
        if (outerLoop == null || !outerLoop.isValid() || !outerLoop.isClosed()) {
            return false;
        }
        
        for (EdgeLoop innerLoop : innerLoops) {
            if (!innerLoop.isValid() || !innerLoop.isClosed()) {
                return false;
            }
        }
        
        return true;
    }

    public void invalidateNormal() {
        isNormalValid = false;
    }
}
