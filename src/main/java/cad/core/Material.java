package cad.core;

public class Material {
    private String name;
    private double density; // kg/m³
    private String category;
    private String description;

    // Visual rendering properties for OpenGL
    private float[] ambientColor; // RGBA [0-1]
    private float[] diffuseColor; // RGBA [0-1]
    private float[] specularColor; // RGBA [0-1]
    private float shininess; // [0-128]

    public Material(String name, double density, String category, String description) {
        this.name = name;
        this.density = density;
        this.category = category;
        this.description = description;

        // Set default visual properties based on category
        setDefaultVisualProperties(category);
    }

    public Material(String name, double density, String category, String description,
            float[] ambient, float[] diffuse, float[] specular, float shininess) {
        this.name = name;
        this.density = density;
        this.category = category;
        this.description = description;
        this.ambientColor = ambient;
        this.diffuseColor = diffuse;
        this.specularColor = specular;
        this.shininess = shininess;
    }

    private void setDefaultVisualProperties(String category) {
        // Default gray material
        this.ambientColor = new float[] { 0.2f, 0.2f, 0.2f, 1.0f };
        this.diffuseColor = new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
        this.specularColor = new float[] { 0.3f, 0.3f, 0.3f, 1.0f };
        this.shininess = 32.0f;
    }

    public Material(String name, double density) {
        this(name, density, "Custom", "");
    }

    public String getName() {
        return name;
    }

    public double getDensity() {
        return density;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Visual properties getters
    public float[] getAmbientColor() {
        return ambientColor != null ? ambientColor : new float[] { 0.2f, 0.2f, 0.2f, 1.0f };
    }

    public float[] getDiffuseColor() {
        return diffuseColor != null ? diffuseColor : new float[] { 0.6f, 0.6f, 0.6f, 1.0f };
    }

    public float[] getSpecularColor() {
        return specularColor != null ? specularColor : new float[] { 0.3f, 0.3f, 0.3f, 1.0f };
    }

    public float getShininess() {
        return shininess > 0 ? shininess : 32.0f;
    }

    // Visual properties setters
    public void setVisualProperties(float[] ambient, float[] diffuse, float[] specular, float shininess) {
        this.ambientColor = ambient;
        this.diffuseColor = diffuse;
        this.specularColor = specular;
        this.shininess = shininess;
    }

    @Override
    public String toString() {
        return String.format("%s (%.1f kg/m³) - %s", name, density, category);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Material))
            return false;
        Material other = (Material) obj;
        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
