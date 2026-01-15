package cad.features.extrusion;

import cad.topology.*;
import cad.geometry.surfaces.*;
import cad.geometry.curves.*;
import cad.core.*;
import cad.math.Vector3d;
import java.util.*;

public class LinearSweepFeature {
    private Sketch sketch;
    private double distance;
    private Vector3d direction;
    private boolean addDraft;
    private double draftAngle;

    public LinearSweepFeature(Sketch sketch, double distance, Vector3d direction) {
        this(sketch, distance, direction, false, 0.0);
    }

    public LinearSweepFeature(Sketch sketch, double distance, Vector3d direction, boolean addDraft, double draftAngle) {
        this.sketch = sketch;
        this.distance = distance;
        this.direction = direction.normalize();
        this.addDraft = addDraft;
        this.draftAngle = draftAngle;
    }

    public BRepBody generate() throws TopologyException {
        validateInput();
        
        BRepBody body = new BRepBody();
        
        List<Vertex> lowerVertices = createVerticesFromSketch(body, 0.0);
        List<Vertex> upperVertices = createVerticesFromSketch(body, distance);
        
        List<Edge> lowerEdges = createEdgesFromVertices(body, lowerVertices, 0.0);
        List<Edge> upperEdges = createEdgesFromVertices(body, upperVertices, distance);
        List<Edge> sideEdges = createSideEdges(body, lowerVertices, upperVertices);
        
        Face lowerFace = createCapFace(body, lowerEdges, direction.negated());
        Face upperFace = createCapFace(body, upperEdges, direction);
        
        List<Face> sideFaces = createSideFaces(body, lowerEdges, upperEdges, sideEdges);
        
        // Add adjacency information for lower face
        for (Edge edge : lowerEdges) {
            edge.addAdjacentFace(lowerFace);
        }
        body.addFace(lowerFace);
        
        // Add adjacency information for upper face
        for (Edge edge : upperEdges) {
            edge.addAdjacentFace(upperFace);
        }
        body.addFace(upperFace);
        
        // Add adjacency information for side faces
        for (Face sideFace : sideFaces) {
            for (Edge edge : sideFace.getOuterLoop().getEdges()) {
                edge.addAdjacentFace(sideFace);
            }
            body.addFace(sideFace);
        }
        
        body.validateTopology();
        return body;
    }

    private void validateInput() throws TopologyException {
        if (sketch == null) {
            throw new TopologyException("Sketch cannot be null");
        }
        
        if (distance <= 0) {
            throw new TopologyException("Extrusion distance must be positive");
        }
        
        if (!isSketchClosed()) {
            throw new TopologyException("Sketch must be closed for extrusion");
        }
        
        if (Math.abs(direction.magnitude() - 1.0) > 1e-6) {
            throw new TopologyException("Direction vector must be normalized");
        }
    }

    private boolean isSketchClosed() {
        List<Sketch.Entity> entities = sketch.getEntities();
        
        for (Sketch.Entity entity : entities) {
            if (entity instanceof Sketch.Circle || entity instanceof Sketch.Polygon) {
                return true;
            }
        }
        
        // Check if we have a closed loop of lines/arcs
        Map<Vector3d, Integer> vertexCount = new HashMap<>();
        
        for (Sketch.Entity entity : entities) {
            if (entity instanceof Sketch.Line) {
                Sketch.Line line = (Sketch.Line) entity;
                Point start = line.getStartPoint();
                Point end = line.getEndPoint();
                
                Vector3d start3d = new Vector3d(start.x, start.y, 0);
                Vector3d end3d = new Vector3d(end.x, end.y, 0);
                
                vertexCount.put(start3d, vertexCount.getOrDefault(start3d, 0) + 1);
                vertexCount.put(end3d, vertexCount.getOrDefault(end3d, 0) + 1);
            } else if (entity instanceof Sketch.Arc) {
                Sketch.Arc arc = (Sketch.Arc) entity;
                Sketch.PointEntity start = arc.getStartPoint();
                Sketch.PointEntity end = arc.getEndPoint();
                
                Vector3d start3d = new Vector3d(start.getX(), start.getY(), 0);
                Vector3d end3d = new Vector3d(end.getX(), end.getY(), 0);
                
                vertexCount.put(start3d, vertexCount.getOrDefault(start3d, 0) + 1);
                vertexCount.put(end3d, vertexCount.getOrDefault(end3d, 0) + 1);
            }
        }
        
        // All vertices should have exactly 2 connected edges for a closed loop
        for (int count : vertexCount.values()) {
            if (count != 2) {
                return false;
            }
        }
        
        return true;
    }

    private List<Vertex> createVerticesFromSketch(BRepBody body, double offset) throws TopologyException {
        List<Vertex> vertices = new ArrayList<>();
        Set<Vector3d> uniquePoints = new HashSet<>();
        
        for (Sketch.Entity entity : sketch.getEntities()) {
            if (entity instanceof Sketch.Line) {
                Sketch.Line line = (Sketch.Line) entity;
                Point start = line.getStartPoint();
                Point end = line.getEndPoint();
                
                Vector3d start3d = new Vector3d(start.x, start.y, offset);
                Vector3d end3d = new Vector3d(end.x, end.y, offset);
                
                if (!uniquePoints.contains(start3d)) {
                    Vertex vertex = new Vertex(start3d);
                    body.addVertex(vertex);
                    vertices.add(vertex);
                    uniquePoints.add(start3d);
                }
                
                if (!uniquePoints.contains(end3d)) {
                    Vertex vertex = new Vertex(end3d);
                    body.addVertex(vertex);
                    vertices.add(vertex);
                    uniquePoints.add(end3d);
                }
            } else if (entity instanceof Sketch.Arc) {
                Sketch.Arc arc = (Sketch.Arc) entity;
                int segments = 12;
                for (int i = 0; i <= segments; i++) {
                    double t = (double) i / segments;
                    Vector3d point = getArcPointAt(arc, t);
                    point = new Vector3d(point.x(), point.y(), offset);
                    
                    if (!uniquePoints.contains(point)) {
                        Vertex vertex = new Vertex(point);
                        body.addVertex(vertex);
                        vertices.add(vertex);
                        uniquePoints.add(point);
                    }
                }
            } else if (entity instanceof Sketch.Circle) {
                Sketch.Circle circle = (Sketch.Circle) entity;
                int segments = 24;
                for (int i = 0; i <= segments; i++) {
                    double t = (double) i / segments;
                    double angle = 2 * Math.PI * t;
                    double x = circle.getX() + circle.getRadius() * Math.cos(angle);
                    double y = circle.getY() + circle.getRadius() * Math.sin(angle);
                    Vector3d point = new Vector3d(x, y, offset);
                    
                    if (!uniquePoints.contains(point)) {
                        Vertex vertex = new Vertex(point);
                        body.addVertex(vertex);
                        vertices.add(vertex);
                        uniquePoints.add(point);
                    }
                }
            } else if (entity instanceof Sketch.Polygon) {
                Sketch.Polygon polygon = (Sketch.Polygon) entity;
                for (Sketch.PointEntity pointEntity : polygon.getSketchPoints()) {
                    Vector3d point = new Vector3d(pointEntity.getX(), pointEntity.getY(), offset);
                    
                    if (!uniquePoints.contains(point)) {
                        Vertex vertex = new Vertex(point);
                        body.addVertex(vertex);
                        vertices.add(vertex);
                        uniquePoints.add(point);
                    }
                }
            }
        }
        
        return vertices;
    }

    private Vector3d getArcPointAt(Sketch.Arc arc, double t) {
        double startAngle = Math.toRadians(arc.getStartAngle());
        double endAngle = Math.toRadians(arc.getEndAngle());
        double angle = startAngle + t * (endAngle - startAngle);
        
        double x = arc.getX() + arc.getRadius() * Math.cos(angle);
        double y = arc.getY() + arc.getRadius() * Math.sin(angle);
        
        return new Vector3d(x, y, 0);
    }

    private List<Edge> createEdgesFromVertices(BRepBody body, List<Vertex> vertices, double z) {
        List<Edge> edges = new ArrayList<>();
        
        for (Sketch.Entity entity : sketch.getEntities()) {
            if (entity instanceof Sketch.Line) {
                Sketch.Line line = (Sketch.Line) entity;
                Vector3d start = new Vector3d(line.getX1(), line.getY1(), z);
                Vector3d end = new Vector3d(line.getX2(), line.getY2(), z);
                
                Vertex startVertex = findVertex(vertices, start);
                Vertex endVertex = findVertex(vertices, end);
                
                if (startVertex != null && endVertex != null) {
                    LineCurve curve = new LineCurve(startVertex.getPoint(), endVertex.getPoint());
                    Edge edge = new Edge(startVertex, endVertex, curve);
                    body.addEdge(edge);
                    edges.add(edge);
                    
                    // Add adjacency information
                    startVertex.addIncidentEdge(edge);
                    endVertex.addIncidentEdge(edge);
                }
            } else if (entity instanceof Sketch.Arc) {
                Sketch.Arc arc = (Sketch.Arc) entity;
                int segments = 12;
                
                for (int i = 0; i < segments; i++) {
                    double t1 = (double) i / segments;
                    double t2 = (double) (i + 1) / segments;
                    
                    Vector3d p1 = getArcPointAt(arc, t1);
                    Vector3d p2 = getArcPointAt(arc, t2);
                    p1 = new Vector3d(p1.x(), p1.y(), z);
                    p2 = new Vector3d(p2.x(), p2.y(), z);
                    
                    Vertex v1 = findVertex(vertices, p1);
                    Vertex v2 = findVertex(vertices, p2);
                    
                    if (v1 != null && v2 != null) {
                        Vector3d center = new Vector3d(arc.getX(), arc.getY(), z);
                        LineCurve curve = new LineCurve(v1.getPoint(), v2.getPoint());
                        Edge edge = new Edge(v1, v2, curve);
                        body.addEdge(edge);
                        edges.add(edge);
                    }
                }
            } else if (entity instanceof Sketch.Circle) {
                Sketch.Circle circle = (Sketch.Circle) entity;
                int segments = 24;
                
                for (int i = 0; i < segments; i++) {
                    double angle1 = 2 * Math.PI * i / segments;
                    double angle2 = 2 * Math.PI * (i + 1) / segments;
                    
                    Vector3d p1 = new Vector3d(circle.getX() + circle.getRadius() * Math.cos(angle1),
                                             circle.getY() + circle.getRadius() * Math.sin(angle1), z);
                    Vector3d p2 = new Vector3d(circle.getX() + circle.getRadius() * Math.cos(angle2),
                                             circle.getY() + circle.getRadius() * Math.sin(angle2), z);
                    
                    Vertex v1 = findVertex(vertices, p1);
                    Vertex v2 = findVertex(vertices, p2);
                    
                    if (v1 != null && v2 != null) {
                        LineCurve curve = new LineCurve(v1.getPoint(), v2.getPoint());
                        Edge edge = new Edge(v1, v2, curve);
                        body.addEdge(edge);
                        edges.add(edge);
                    }
                }
            } else if (entity instanceof Sketch.Polygon) {
                Sketch.Polygon polygon = (Sketch.Polygon) entity;
                List<Sketch.PointEntity> points = polygon.getSketchPoints();
                
                for (int i = 0; i < points.size(); i++) {
                    Sketch.PointEntity p1 = points.get(i);
                    Sketch.PointEntity p2 = points.get((i + 1) % points.size());
                    
                    Vector3d v1_3d = new Vector3d(p1.getX(), p1.getY(), z);
                    Vector3d v2_3d = new Vector3d(p2.getX(), p2.getY(), z);
                    
                    Vertex v1 = findVertex(vertices, v1_3d);
                    Vertex v2 = findVertex(vertices, v2_3d);
                    
                    if (v1 != null && v2 != null) {
                        LineCurve curve = new LineCurve(v1.getPoint(), v2.getPoint());
                        Edge edge = new Edge(v1, v2, curve);
                        body.addEdge(edge);
                        edges.add(edge);
                    }
                }
            }
        }
        
        return edges;
    }

    private List<Edge> createSideEdges(BRepBody body, List<Vertex> lowerVertices, List<Vertex> upperVertices) {
        List<Edge> sideEdges = new ArrayList<>();
        
        for (int i = 0; i < lowerVertices.size(); i++) {
            Vertex lower = lowerVertices.get(i);
            Vector3d upperPoint = lower.getPoint().plus(direction.multiply(distance));
            Vertex upper = findVertex(upperVertices, upperPoint);
            
            if (upper != null) {
                if (addDraft && Math.abs(draftAngle) > 1e-6) {
                    Vector3d offset = upper.getPoint().minus(lower.getPoint());
                    Vector3d center = lower.getPoint().plus(upper.getPoint()).multiply(0.5);
                    double draftRadius = Math.max(0.1, offset.magnitude() * Math.tan(draftAngle));
                    
                    Vector3d radial = offset.cross(direction);
                    if (radial.magnitude() < 1e-6) {
                        radial = Vector3d.X_AXIS;
                    } else {
                        radial = radial.normalize();
                    }
                    
                    LineCurve curve = new LineCurve(lower.getPoint(), upper.getPoint());
                    Edge edge = new Edge(lower, upper, curve);
                    body.addEdge(edge);
                    sideEdges.add(edge);
                } else {
                    LineCurve curve = new LineCurve(lower.getPoint(), upper.getPoint());
                    Edge edge = new Edge(lower, upper, curve);
                    body.addEdge(edge);
                    sideEdges.add(edge);
                }
            }
        }
        
        return sideEdges;
    }

    private Face createCapFace(BRepBody body, List<Edge> edges, Vector3d normal) throws TopologyException {
        if (edges.isEmpty()) {
            throw new TopologyException("Cannot create face from empty edge list");
        }
        EdgeLoop loop = new EdgeLoop(edges);
        PlaneSurface surface = new PlaneSurface(edges.get(0).getStartVertex().getPoint(), normal);
        return new Face(surface, loop);
    }

    private List<Face> createSideFaces(BRepBody body, List<Edge> lowerEdges, List<Edge> upperEdges, List<Edge> sideEdges) {
        List<Face> sideFaces = new ArrayList<>();
        
        Map<Vertex, List<Edge>> vertexToEdges = new HashMap<>();
        
        for (Edge edge : lowerEdges) {
            vertexToEdges.computeIfAbsent(edge.getStartVertex(), k -> new ArrayList<>()).add(edge);
            vertexToEdges.computeIfAbsent(edge.getEndVertex(), k -> new ArrayList<>()).add(edge);
        }
        
        for (Edge edge : upperEdges) {
            vertexToEdges.computeIfAbsent(edge.getStartVertex(), k -> new ArrayList<>()).add(edge);
            vertexToEdges.computeIfAbsent(edge.getEndVertex(), k -> new ArrayList<>()).add(edge);
        }
        
        for (int i = 0; i < lowerEdges.size(); i++) {
            Edge lowerEdge = lowerEdges.get(i);
            Vertex v1 = lowerEdge.getStartVertex();
            Vertex v2 = lowerEdge.getEndVertex();
            
            Edge side1 = findSideEdge(sideEdges, v1, v1.getPoint().plus(direction.multiply(distance)));
            Edge side2 = findSideEdge(sideEdges, v2, v2.getPoint().plus(direction.multiply(distance)));
            
            if (side1 != null && side2 != null) {
                List<Edge> faceEdges = Arrays.asList(lowerEdge, side2, side1);
                EdgeLoop loop = new EdgeLoop(faceEdges);
                
                Surface surface;
                if (addDraft && Math.abs(draftAngle) > 1e-6) {
                    surface = new CylindricalSurface(v1.getPoint(), direction, distance / Math.cos(draftAngle));
                } else {
                    Vector3d p1 = v1.getPoint();
                    Vector3d p2 = v2.getPoint();
                    Vector3d p3 = p1.plus(direction.multiply(distance));
                    surface = new PlaneSurface(p1, p2, p3);
                }
                
                Face face = new Face(surface, loop);
                sideFaces.add(face);
            }
        }
        
        return sideFaces;
    }

    private Vertex findVertex(List<Vertex> vertices, Vector3d point) {
        for (Vertex vertex : vertices) {
            if (vertex.getPoint().distance(point) < 1e-6) {
                return vertex;
            }
        }
        return null;
    }

    private Edge findSideEdge(List<Edge> sideEdges, Vertex start, Vector3d end) {
        for (Edge edge : sideEdges) {
            if (edge.getStartVertex().equals(start)) {
                if (edge.getEndVertex().getPoint().distance(end) < 1e-6) {
                    return edge;
                }
            }
        }
        return null;
    }

    public static LinearSweepBuilder builder() {
        return new LinearSweepBuilder();
    }

    public static class LinearSweepBuilder {
        private Sketch sketch;
        private double distance;
        private Vector3d direction = Vector3d.Z_AXIS;
        private boolean addDraft = false;
        private double draftAngle = 0.0;

        public LinearSweepBuilder sketch(Sketch sketch) {
            this.sketch = sketch;
            return this;
        }

        public LinearSweepBuilder distance(double distance) {
            this.distance = distance;
            return this;
        }

        public LinearSweepBuilder direction(Vector3d direction) {
            this.direction = direction.normalize();
            return this;
        }

        public LinearSweepBuilder draft(double draftAngle) {
            this.addDraft = true;
            this.draftAngle = draftAngle;
            return this;
        }

        public LinearSweepFeature build() {
            return new LinearSweepFeature(sketch, distance, direction, addDraft, draftAngle);
        }
    }
}