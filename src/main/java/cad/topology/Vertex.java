package cad.topology;

import java.util.ArrayList;
import java.util.List;
import cad.math.Vector3d;

public class Vertex {
    private Vector3d point;
    private List<Edge> incidentEdges;

    public Vertex(Vector3d point) {
        this.point = point;
        this.incidentEdges = new ArrayList<>();
    }

    public Vertex(double x, double y, double z) {
        this(new Vector3d(x, y, z));
    }

    public Vector3d getPoint() {
        return point;
    }

    public void setPoint(Vector3d point) {
        this.point = point;
    }

    public List<Edge> getIncidentEdges() {
        return incidentEdges;
    }

    public void addIncidentEdge(Edge edge) {
        if (!incidentEdges.contains(edge)) {
            incidentEdges.add(edge);
        }
    }

    public void removeIncidentEdge(Edge edge) {
        incidentEdges.remove(edge);
    }

    public List<Vertex> getAdjacentVertices() {
        List<Vertex> adjacent = new ArrayList<>();
        for (Edge edge : incidentEdges) {
            Vertex other = edge.getStartVertex().equals(this) ? edge.getEndVertex() : edge.getStartVertex();
            if (!adjacent.contains(other)) {
                adjacent.add(other);
            }
        }
        return adjacent;
    }

    public List<Face> getAdjacentFaces() {
        List<Face> faces = new ArrayList<>();
        for (Edge edge : incidentEdges) {
            for (Face face : edge.getAdjacentFaces()) {
                if (!faces.contains(face)) {
                    faces.add(face);
                }
            }
        }
        return faces;
    }

    public boolean isBoundaryVertex() {
        for (Edge edge : incidentEdges) {
            if (edge.isBorderEdge()) {
                return true;
            }
        }
        return false;
    }

    public boolean isManifoldVertex() {
        List<Face> faces = getAdjacentFaces();
        if (faces.size() < 2) return false;
        
        Vector3d normalSum = Vector3d.zero();
        for (Face face : faces) {
            normalSum = normalSum.plus(face.getNormal());
        }
        
        return normalSum.magnitude() > 1e-6;
    }

    public Vector3d calculateNormal() {
        List<Face> faces = getAdjacentFaces();
        if (faces.isEmpty()) {
            return Vector3d.Z_AXIS;
        }
        
        Vector3d normalSum = Vector3d.zero();
        for (Face face : faces) {
            normalSum = normalSum.plus(face.getNormal());
        }
        
        return normalSum.normalize();
    }

    public int getValence() {
        return incidentEdges.size();
    }

    public boolean isValid() {
        if (point == null) return false;
        if (Double.isNaN(point.x()) || Double.isNaN(point.y()) || Double.isNaN(point.z())) return false;
        if (Double.isInfinite(point.x()) || Double.isInfinite(point.y()) || Double.isInfinite(point.z())) return false;
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vertex vertex = (Vertex) obj;
        return point.equals(vertex.point);
    }

    @Override
    public int hashCode() {
        return point.hashCode();
    }

    @Override
    public String toString() {
        return "Vertex{" + point + "}";
    }
}
