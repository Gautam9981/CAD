package cad.topology;

import cad.math.Vector3d;
import java.util.ArrayList;
import java.util.List;

public class EdgeLoop {
    private List<Edge> edges;

    public EdgeLoop(List<Edge> edges) {
        this.edges = new ArrayList<>(edges);
    }

    public EdgeLoop() {
        this.edges = new ArrayList<>();
    }

    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public boolean isClosed() {
        if (edges.isEmpty()) {
            return false;
        }
        Vertex start = edges.get(0).getStartVertex();
        Vertex end = edges.get(edges.size() - 1).getEndVertex();
        return start.equals(end);
    }

    public boolean isValid() {
        if (edges.size() < 3) return false;
        
        for (int i = 0; i < edges.size(); i++) {
            Edge current = edges.get(i);
            Edge next = edges.get((i + 1) % edges.size());
            
            if (!current.getEndVertex().equals(next.getStartVertex())) {
                return false;
            }
        }
        return true;
    }

    public double getLength() {
        double length = 0.0;
        for (Edge edge : edges) {
            Vector3d start = edge.getStartVertex().getPoint();
            Vector3d end = edge.getEndVertex().getPoint();
            length += start.distance(end);
        }
        return length;
    }
}
