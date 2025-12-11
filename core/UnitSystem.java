package cad.core;

/**
 * Defines standard unit systems for the CAD application.
 * Mirrors common CAD standards like SolidWorks.
 */
public enum UnitSystem {
    MKS("MKS (meter, kilogram, second)"),
    CGS("CGS (centimeter, gram, second)"),
    MMGS("MMGS (millimeter, gram, second)"),
    IPS("IPS (inch, pound, second)");

    private final String description;

    UnitSystem(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the Length conversion factor to Meters (SI base).
     * Useful for inter-unit calculations.
     */
    public double toMeters(double value) {
        switch (this) {
            case MKS:
                return value;
            case CGS:
                return value / 100.0;
            case MMGS:
                return value / 1000.0;
            case IPS:
                return value * 0.0254;
            default:
                return value;
        }
    }

    /**
     * Returns the DXF unit code for this system.
     */
    public int getDXFCode() {
        switch (this) {
            case IPS:
                return 1; // Inches
            case MKS:
                return 6; // Meters
            case MMGS:
                return 4; // Millimeters
            case CGS:
                return 5; // Centimeters
            default:
                return 0; // Unitless
        }
    }

    /**
     * Resolves a UnitSystem from a DXF integer code.
     */
    public static UnitSystem fromDXFCode(int code) {
        switch (code) {
            case 1:
                return IPS;
            case 6:
                return MKS;
            case 4:
                return MMGS;
            case 5:
                return CGS;
            default:
                return MMGS; // Default fallbacks
        }
    }
}
