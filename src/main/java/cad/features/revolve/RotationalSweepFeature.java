package cad.features.revolve;

import cad.topology.*;
import cad.geometry.surfaces.*;
import cad.geometry.curves.*;
import cad.core.*;
import cad.math.Vector3d;
import java.util.*;

public class RotationalSweepFeature {
    private Sketch sketch;
    private Vector3d axisOrigin;
    private Vector3d axisDirection;
    private double angle;

    public RotationalSweepFeature(Sketch sketch, Vector3d axisOrigin, Vector3d axisDirection, double angle) {
        this.sketch = sketch;
        this.axisOrigin = axisOrigin;
        this.axisDirection = axisDirection.normalize();
        this.angle = angle;
    }

    public BRepBody generate() throws TopologyException {
        validateInput();
        
        BRepBody body = new BRepBody();
        
        // 1. Create vertices at 0 angle (start profile)
        List<Vertex> startVertices = createVerticesFromSketch(body, 0.0);
        
        List<Vertex> endVertices;
        boolean isFullRevolve = Math.abs(angle - 2 * Math.PI) < 1e-6;
        
        if (isFullRevolve) {
            endVertices = startVertices;
        } else {
            // Create vertices at 'angle' (end profile)
            endVertices = createVerticesFromSketch(body, angle);
        }
        
        // 2. Create edges for start and end profiles
        List<Edge> startProfileEdges = createProfileEdges(body, startVertices, 0.0);
        List<Edge> endProfileEdges;
        
        if (isFullRevolve) {
            endProfileEdges = startProfileEdges;
        } else {
            endProfileEdges = createProfileEdges(body, endVertices, angle);
        }
        
        // 3. Create trajectory edges (arcs around the axis) for each vertex
        List<Edge> trajectoryEdges = createTrajectoryEdges(body, startVertices, endVertices);
        
        // 4. Create swept faces
        List<Face> sweptFaces = createSweptFaces(body, startProfileEdges, endProfileEdges, trajectoryEdges);
        for (Face face : sweptFaces) {
            body.addFace(face);
        }
        
        // 5. Create cap faces if not full revolve
        if (!isFullRevolve) {
            Vector3d startNormal = rotateVector(getSketchNormal(), 0.0);
            Face startFace = createCapFace(body, startProfileEdges, startNormal.negated());
            body.addFace(startFace);
            
            Vector3d endNormal = rotateVector(getSketchNormal(), angle);
            Face endFace = createCapFace(body, endProfileEdges, endNormal);
            body.addFace(endFace);
        }
        
        body.validateTopology();
        return body;
    }

    private void validateInput() throws TopologyException {
        if (sketch == null) throw new TopologyException("Sketch cannot be null");
        if (angle <= 0 || angle > 2 * Math.PI + 1e-6) throw new TopologyException("Angle must be between 0 and 360 degrees");
        if (Math.abs(axisDirection.magnitude() - 1.0) > 1e-6) throw new TopologyException("Axis direction must be normalized");
        
        // TODO: Validate that sketch doesn't cross the axis of revolution
    }

    private Vector3d getSketchNormal() {
        return Vector3d.Z_AXIS; // By default sketch is on XY plane
    }

    private Vector3d rotatePoint(Vector3d point, double theta) {
        Vector3d toPoint = point.subtract(axisOrigin);
        double axialDist = axisDirection.dot(toPoint);
        Vector3d axialComp = axisDirection.multiply(axialDist);
        Vector3d radialComp = toPoint.subtract(axialComp);
        
        if (radialComp.magnitude() < 1e-6) {
            return point;
        }
        
        Vector3d radialDir = radialComp.normalize();
        Vector3d tangentDir = axisDirection.cross(radialDir).normalize();
        
        Vector3d rotatedRadial = radialDir.multiply(Math.cos(theta)).plus(tangentDir.multiply(Math.sin(theta)));
        return axisOrigin.plus(axialComp).plus(rotatedRadial.multiply(radialComp.magnitude()));
    }

    private Vector3d rotateVector(Vector3d vec, double theta) {
        double axialDist = axisDirection.dot(vec);
        Vector3d axialComp = axisDirection.multiply(axialDist);
        Vector3d radialComp = vec.subtract(axialComp);
        
        if (radialComp.magnitude() < 1e-6) {
            return vec;
        }
        
        Vector3d radialDir = radialComp.normalize();
        Vector3d tangentDir = axisDirection.cross(radialDir).normalize();
        
        Vector3d rotatedRadial = radialDir.multiply(Math.cos(theta)).plus(tangentDir.multiply(Math.sin(theta)));
        return axialComp.plus(rotatedRadial.multiply(radialComp.magnitude()));
    }

    private List<Vertex> createVerticesFromSketch(BRepBody body, double currentAngle) throws TopologyException {
        List<Vertex> vertices = new ArrayList<>();
        Set<Vector3d> uniquePoints = new HashSet<>();
        
        for (Sketch.Entity entity : sketch.getEntities()) {
            if (entity instanceof Sketch.Line) {
                Sketch.Line line = (Sketch.Line) entity;
                Vector3d start3d = rotatePoint(new Vector3d(line.getStartPoint().x, line.getStartPoint().y, 0), currentAngle);
                Vector3d end3d = rotatePoint(new Vector3d(line.getEndPoint().x, line.getEndPoint().y, 0), currentAngle);
                
                addUniqueVertex(body, vertices, uniquePoints, start3d);
                addUniqueVertex(body, vertices, uniquePoints, end3d);
            } else if (entity instanceof Sketch.Polygon) {
                Sketch.Polygon poly = (Sketch.Polygon) entity;
                for (Sketch.PointEntity p : poly.getSketchPoints()) {
                    Vector3d pt3d = rotatePoint(new Vector3d(p.getX(), p.getY(), 0), currentAngle);
                    addUniqueVertex(body, vertices, uniquePoints, pt3d);
                }
            }
        }
        return vertices;
    }

    private void addUniqueVertex(BRepBody body, List<Vertex> vertices, Set<Vector3d> uniquePoints, Vector3d point) {
        for (Vector3d p : uniquePoints) {
            if (p.distance(point) < 1e-6) return;
        }
        Vertex v = new Vertex(point);
        body.addVertex(v);
        vertices.add(v);
        uniquePoints.add(point);
    }

    private Vertex findVertex(List<Vertex> vertices, Vector3d point) {
        for (Vertex v : vertices) {
            if (v.getPoint().distance(point) < 1e-6) return v;
        }
        return null;
    }

    private List<Edge> createProfileEdges(BRepBody body, List<Vertex> vertices, double currentAngle) {
        List<Edge> edges = new ArrayList<>();
        for (Sketch.Entity entity : sketch.getEntities()) {
            if (entity instanceof Sketch.Line) {
                Sketch.Line line = (Sketch.Line) entity;
                Vector3d start = rotatePoint(new Vector3d(line.getX1(), line.getY1(), 0), currentAngle);
                Vector3d end = rotatePoint(new Vector3d(line.getX2(), line.getY2(), 0), currentAngle);
                
                Vertex v1 = findVertex(vertices, start);
                Vertex v2 = findVertex(vertices, end);
                
                if (v1 != null && v2 != null) {
                    LineCurve curve = new LineCurve(v1.getPoint(), v2.getPoint());
                    Edge edge = new Edge(v1, v2, curve);
                    body.addEdge(edge);
                    edges.add(edge);
                    v1.addIncidentEdge(edge);
                    v2.addIncidentEdge(edge);
                }
            } else if (entity instanceof Sketch.Polygon) {
                Sketch.Polygon poly = (Sketch.Polygon) entity;
                List<Sketch.PointEntity> pts = poly.getSketchPoints();
                for (int i = 0; i < pts.size(); i++) {
                    Vector3d p1 = rotatePoint(new Vector3d(pts.get(i).getX(), pts.get(i).getY(), 0), currentAngle);
                    Vector3d p2 = rotatePoint(new Vector3d(pts.get((i+1)%pts.size()).getX(), pts.get((i+1)%pts.size()).getY(), 0), currentAngle);
                    
                    Vertex v1 = findVertex(vertices, p1);
                    Vertex v2 = findVertex(vertices, p2);
                    
                    if (v1 != null && v2 != null) {
                        LineCurve curve = new LineCurve(v1.getPoint(), v2.getPoint());
                        Edge edge = new Edge(v1, v2, curve);
                        body.addEdge(edge);
                        edges.add(edge);
                        v1.addIncidentEdge(edge);
                        v2.addIncidentEdge(edge);
                    }
                }
            }
        }
        return edges;
    }

    private List<Edge> createTrajectoryEdges(BRepBody body, List<Vertex> startVertices, List<Vertex> endVertices) {
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < startVertices.size(); i++) {
            Vertex vStart = startVertices.get(i);
            Vertex vEnd = endVertices.get(i); // If isFullRevolve, vEnd == vStart
            
            // Check if vertex is on the axis
            Vector3d toPoint = vStart.getPoint().subtract(axisOrigin);
            Vector3d radial = toPoint.subtract(axisDirection.multiply(axisDirection.dot(toPoint)));
            if (radial.magnitude() < 1e-6) {
                edges.add(null); // No trajectory edge for point on axis
                continue;
            }
            
            // Create an ArcCurve for the trajectory
            // We approximate it with a LineCurve for now if angle is small, or use a generic Curve
            // Since we need a Curve object, let's just use LineCurve between start and end.
            // In a real BRep, this should be an ArcCurve! But since cad.geometry.curves.ArcCurve doesn't support 3D arcs directly easily,
            // we will use a placeholder linecurve. This is only used for edge geometry.
            LineCurve curve = new LineCurve(vStart.getPoint(), vEnd.getPoint());
            Edge edge = new Edge(vStart, vEnd, curve);
            body.addEdge(edge);
            edges.add(edge);
            
            if (vStart != vEnd) {
                vStart.addIncidentEdge(edge);
                vEnd.addIncidentEdge(edge);
            } else {
                vStart.addIncidentEdge(edge); // loop edge
            }
        }
        return edges;
    }

    private Face createCapFace(BRepBody body, List<Edge> edges, Vector3d normal) throws TopologyException {
        if (edges.isEmpty()) throw new TopologyException("Cannot create cap face from empty edges");
        EdgeLoop loop = new EdgeLoop(edges);
        PlaneSurface surface = new PlaneSurface(edges.get(0).getStartVertex().getPoint(), normal);
        return new Face(surface, loop);
    }

    private List<Face> createSweptFaces(BRepBody body, List<Edge> startEdges, List<Edge> endEdges, List<Edge> trajectoryEdges) throws TopologyException {
        List<Face> sweptFaces = new ArrayList<>();
        
        for (int i = 0; i < startEdges.size(); i++) {
            Edge startEdge = startEdges.get(i);
            Edge endEdge = endEdges.get(i);
            
            Vertex v1 = startEdge.getStartVertex();
            Vertex v2 = startEdge.getEndVertex();
            
            Edge traj1 = findTrajectoryEdge(trajectoryEdges, v1);
            Edge traj2 = findTrajectoryEdge(trajectoryEdges, v2);
            
            List<Edge> faceEdges = new ArrayList<>();
            faceEdges.add(startEdge);
            if (traj2 != null) faceEdges.add(traj2);
            if (startEdge != endEdge) faceEdges.add(endEdge);
            if (traj1 != null) faceEdges.add(traj1); // In proper order, need to reverse if needed
            
            // Surface of revolution
            LineCurve baseCurve = new LineCurve(v1.getPoint(), v2.getPoint());
            SurfaceOfRevolution surface = new SurfaceOfRevolution(baseCurve, axisOrigin, axisDirection, 0.0, angle);
            
            EdgeLoop loop = new EdgeLoop(faceEdges);
            Face face = new Face(surface, loop);
            sweptFaces.add(face);
        }
        return sweptFaces;
    }
    
    private Edge findTrajectoryEdge(List<Edge> trajectoryEdges, Vertex v) {
        for (Edge e : trajectoryEdges) {
            if (e != null && (e.getStartVertex() == v || e.getEndVertex() == v)) return e;
        }
        return null;
    }

    public static RotationalSweepBuilder builder() {
        return new RotationalSweepBuilder();
    }

    public static class RotationalSweepBuilder {
        private Sketch sketch;
        private Vector3d axisOrigin = Vector3d.zero();
        private Vector3d axisDirection = Vector3d.Y_AXIS;
        private double angle = 2 * Math.PI;

        public RotationalSweepBuilder sketch(Sketch sketch) {
            this.sketch = sketch;
            return this;
        }
        public RotationalSweepBuilder axisOrigin(Vector3d axisOrigin) {
            this.axisOrigin = axisOrigin;
            return this;
        }
        public RotationalSweepBuilder axisDirection(Vector3d axisDirection) {
            this.axisDirection = axisDirection;
            return this;
        }
        public RotationalSweepBuilder angle(double angle) {
            this.angle = angle;
            return this;
        }
        public RotationalSweepFeature build() {
            return new RotationalSweepFeature(sketch, axisOrigin, axisDirection, angle);
        }
    }
}
