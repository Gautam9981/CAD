package cad.core;

import java.util.UUID;
import java.util.List;

/**
 * Abstract base class for all geometric constraints.
 * A constraint defines a relationship between one or more sketch entities
 * (or their sub-elements like points) that must be satisfied by the solver.
 */
public abstract class Constraint {
    protected String id;
    protected ConstraintType type;
    protected boolean active;

    public Constraint(ConstraintType type) {
        this.id = UUID.randomUUID().toString();
        this.type = type;
        this.active = true;
    }

    public String getId() {
        return id;
    }

    public ConstraintType getType() {
        return type;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Calculates the current error (deviation) from satisfying this constraint.
     * The solver tries to minimize this error to 0.
     * @return The error value (distance, angle difference, etc.)
     */
    public abstract double getError();
    
    /**
     * Solving method used by the iterative solver.
     * Adjusts the involved entities to reduce the error.
     */
    public abstract void solve();
}
