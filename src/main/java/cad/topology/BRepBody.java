package cad.topology;

import cad.math.Vector3d;
import java.util.ArrayList;
import java.util.List;

public class BRepBody {
    private List<Face> faces;
    private List<Edge> edges;
    private List<Vertex> vertices;

    public BRepBody() {
        this.faces = new ArrayList<>();
        this.edges = new ArrayList<>();
        this.vertices = new ArrayList<>();
    }

    public void addFace(Face face) {
        if (!faces.contains(face)) {
            faces.add(face);
        }
    }

    public void addEdge(Edge edge) {
        if (!edges.contains(edge)) {
            edges.add(edge);
        }
    }

    public void addVertex(Vertex vertex) {
        if (!vertices.contains(vertex)) {
            vertices.add(vertex);
        }
    }

    public List<Face> getFaces() {
        return faces;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public boolean validateEulerCharacteristic() {
        int V = vertices.size();
        int E = edges.size();
        int F = faces.size();
        
        int chi = V - E + F;
        
        return chi == 2;
    }
    
    public double getVolume() {
        double volume = 0.0;
        for (Face face : faces) {
            List<Vertex> faceVertices = getFaceVertices(face);
            if (faceVertices.size() >= 3) {
                Vector3d v0 = faceVertices.get(0).getPoint();
                for (int i = 1; i < faceVertices.size() - 1; i++) {
                    Vector3d v1 = faceVertices.get(i).getPoint();
                    Vector3d v2 = faceVertices.get(i + 1).getPoint();
                    
                    Vector3d a = v1.subtract(v0);
                    Vector3d b = v2.subtract(v0);
                    
                    volume += v0.dot(a.cross(b)) / 6.0;
                }
            }
        }
        return Math.abs(volume);
    }
    
    public double getSurfaceArea() {
        double area = 0.0;
        for (Face face : faces) {
            area += calculateFaceArea(face);
        }
        return area;
    }
    
    private double calculateFaceArea(Face face) {
        List<Vertex> faceVertices = getFaceVertices(face);
        if (faceVertices.size() < 3) return 0.0;
        
        double area = 0.0;
        Vector3d v0 = faceVertices.get(0).getPoint();
        for (int i = 1; i < faceVertices.size() - 1; i++) {
            Vector3d v1 = faceVertices.get(i).getPoint();
            Vector3d v2 = faceVertices.get(i + 1).getPoint();
            
            Vector3d a = v1.subtract(v0);
            Vector3d b = v2.subtract(v0);
            area += a.cross(b).magnitude() / 2.0;
        }
        return area;
    }
    
    private List<Vertex> getFaceVertices(Face face) {
        List<Vertex> vertices = new ArrayList<>();
        EdgeLoop outerLoop = face.getOuterLoop();
        if (outerLoop != null) {
            for (Edge edge : outerLoop.getEdges()) {
                if (!vertices.contains(edge.getStartVertex())) {
                    vertices.add(edge.getStartVertex());
                }
            }
        }
        return vertices;
    }
    
    public void validateTopology() throws TopologyException {
        if (!validateEulerCharacteristic()) {
            throw new TopologyException("Invalid Euler characteristic: V - E + F != 2");
        }
        
        for (Edge edge : edges) {
            if (edge.getAdjacentFaces().size() < 1 || edge.getAdjacentFaces().size() > 2) {
                throw new TopologyException("Edge has invalid number of adjacent faces: " + edge.getAdjacentFaces().size());
            }
        }
        
        for (Vertex vertex : vertices) {
            if (vertex.getIncidentEdges().isEmpty()) {
                throw new TopologyException("Vertex has no incident edges");
            }
        }
    }
}
