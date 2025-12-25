package cad.core;

import java.util.UUID;
import java.util.List;

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

    
    public abstract double getError();
    
    
    public abstract void solve();
}
