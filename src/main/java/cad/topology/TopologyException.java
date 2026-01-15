package cad.topology;

public class TopologyException extends Exception {
    public TopologyException(String message) {
        super(message);
    }
    
    public TopologyException(String message, Throwable cause) {
        super(message, cause);
    }
}