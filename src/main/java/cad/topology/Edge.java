package cad.topology;

import cad.geometry.curves.Curve;
import cad.math.Vector3d;
import java.util.ArrayList;
import java.util.List;

public class Edge {
    private Vertex startVertex;
    private Vertex endVertex;
    private Curve curve;
    private List<Face> adjacentFaces;
    private boolean isOriented;

    public Edge(Vertex start, Vertex end, Curve curve) {
        this.startVertex = start;
        this.endVertex = end;
        this.curve = curve;
        this.adjacentFaces = new ArrayList<>();
        this.isOriented = false;
        
        startVertex.addIncidentEdge(this);
        endVertex.addIncidentEdge(this);
    }

    public Vertex getStartVertex() {
        return startVertex;
    }

    public Vertex getEndVertex() {
        return endVertex;
    }

    public Curve getCurve() {
        return curve;
    }

    public List<Face> getAdjacentFaces() {
        return adjacentFaces;
    }

    public void addAdjacentFace(Face face) {
        if (!adjacentFaces.contains(face)) {
            adjacentFaces.add(face);
        }
    }

    public double getLength() {
        if (curve != null) {
            double start = curve.startParam();
            double end = curve.endParam();
            Vector3d startPoint = curve.value(start);
            Vector3d endPoint = curve.value(end);
            return startPoint.distance(endPoint);
        }
        return startVertex.getPoint().distance(endVertex.getPoint());
    }

    public Vector3d getTangentAtStart() {
        if (curve != null) {
            return curve.tangent(curve.startParam()).normalize();
        }
        return endVertex.getPoint().subtract(startVertex.getPoint()).normalize();
    }

    public Vector3d getTangentAtEnd() {
        if (curve != null) {
            return curve.tangent(curve.endParam()).normalize();
        }
        return endVertex.getPoint().subtract(startVertex.getPoint()).normalize();
    }

    public boolean isLinear() {
        if (curve == null) return true;
        
        Vector3d directVector = endVertex.getPoint().subtract(startVertex.getPoint());
        double expectedLength = directVector.magnitude();
        
        if (Math.abs(getLength() - expectedLength) > 1e-6) {
            return false;
        }
        
        return true;
    }

    public Vector3d getMidPoint() {
        if (curve != null) {
            double midParam = (curve.startParam() + curve.endParam()) / 2.0;
            return curve.value(midParam);
        }
        return startVertex.getPoint().plus(endVertex.getPoint()).multiply(0.5);
    }

    public boolean isBorderEdge() {
        return adjacentFaces.size() == 1;
    }

    public boolean isManifoldEdge() {
        return adjacentFaces.size() == 2;
    }

    public boolean isValid() {
        if (startVertex == null || endVertex == null) {
            return false;
        }
        
        if (startVertex.equals(endVertex)) {
            return false;
        }
        
        return true;
    }

    public void reverse() {
        Vertex temp = startVertex;
        startVertex = endVertex;
        endVertex = temp;
        isOriented = !isOriented;
    }

    public boolean isOriented() {
        return isOriented;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Edge edge = (Edge) obj;
        return startVertex.equals(edge.startVertex) && endVertex.equals(edge.endVertex);
    }

    @Override
    public int hashCode() {
        return startVertex.hashCode() * 31 + endVertex.hashCode();
    }
}
