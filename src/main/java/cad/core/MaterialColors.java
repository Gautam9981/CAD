package cad.core;


public class MaterialColors {

    // METALS - High shininess, metallic appearance
    public static class Metals {
        // Aluminum - Light gray metallic
        public static final float[] ALUMINUM_AMBIENT = { 0.25f, 0.25f, 0.25f, 1.0f };
        public static final float[] ALUMINUM_DIFFUSE = { 0.65f, 0.65f, 0.65f, 1.0f };
        public static final float[] ALUMINUM_SPECULAR = { 0.8f, 0.8f, 0.8f, 1.0f };
        public static final float ALUMINUM_SHININESS = 90.0f;

        // Steel - Medium gray metallic
        public static final float[] STEEL_AMBIENT = { 0.2f, 0.2f, 0.2f, 1.0f };
        public static final float[] STEEL_DIFFUSE = { 0.5f, 0.5f, 0.5f, 1.0f };
        public static final float[] STEEL_SPECULAR = { 0.75f, 0.75f, 0.75f, 1.0f };
        public static final float STEEL_SHININESS = 85.0f;

        // Stainless Steel - Bright metallic
        public static final float[] STAINLESS_AMBIENT = { 0.28f, 0.28f, 0.28f, 1.0f };
        public static final float[] STAINLESS_DIFFUSE = { 0.7f, 0.7f, 0.7f, 1.0f };
        public static final float[] STAINLESS_SPECULAR = { 0.85f, 0.85f, 0.85f, 1.0f };
        public static final float STAINLESS_SHININESS = 95.0f;

        // Titanium - Dark gray metallic
        public static final float[] TITANIUM_AMBIENT = { 0.18f, 0.18f, 0.2f, 1.0f };
        public static final float[] TITANIUM_DIFFUSE = { 0.45f, 0.45f, 0.5f, 1.0f };
        public static final float[] TITANIUM_SPECULAR = { 0.7f, 0.7f, 0.75f, 1.0f };
        public static final float TITANIUM_SHININESS = 80.0f;

        // Copper - Reddish metallic
        public static final float[] COPPER_AMBIENT = { 0.3f, 0.15f, 0.1f, 1.0f };
        public static final float[] COPPER_DIFFUSE = { 0.7f, 0.4f, 0.25f, 1.0f };
        public static final float[] COPPER_SPECULAR = { 0.8f, 0.6f, 0.5f, 1.0f };
        public static final float COPPER_SHININESS = 75.0f;

        // Brass - Yellowish metallic
        public static final float[] BRASS_AMBIENT = { 0.3f, 0.25f, 0.1f, 1.0f };
        public static final float[] BRASS_DIFFUSE = { 0.7f, 0.6f, 0.3f, 1.0f };
        public static final float[] BRASS_SPECULAR = { 0.85f, 0.75f, 0.5f, 1.0f };
        public static final float BRASS_SHININESS = 70.0f;
    }

    // PLASTICS - Medium shininess, varied colors
    public static class Plastics {
        // ABS - Beige/tan
        public static final float[] ABS_AMBIENT = { 0.25f, 0.22f, 0.18f, 1.0f };
        public static final float[] ABS_DIFFUSE = { 0.75f, 0.7f, 0.6f, 1.0f };
        public static final float[] ABS_SPECULAR = { 0.35f, 0.35f, 0.35f, 1.0f };
        public static final float ABS_SHININESS = 25.0f;

        // PLA - White/translucent
        public static final float[] PLA_AMBIENT = { 0.3f, 0.3f, 0.3f, 1.0f };
        public static final float[] PLA_DIFFUSE = { 0.85f, 0.85f, 0.85f, 1.0f };
        public static final float[] PLA_SPECULAR = { 0.4f, 0.4f, 0.4f, 1.0f };
        public static final float PLA_SHININESS = 30.0f;

        // Polycarbonate - Clear/slightly blue
        public static final float[] POLYCARBONATE_AMBIENT = { 0.25f, 0.27f, 0.3f, 0.9f };
        public static final float[] POLYCARBONATE_DIFFUSE = { 0.7f, 0.75f, 0.8f, 0.9f };
        public static final float[] POLYCARBONATE_SPECULAR = { 0.6f, 0.6f, 0.6f, 0.9f };
        public static final float POLYCARBONATE_SHININESS = 50.0f;

        // Generic plastic
        public static final float[] PLASTIC_AMBIENT = { 0.2f, 0.2f, 0.2f, 1.0f };
        public static final float[] PLASTIC_DIFFUSE = { 0.7f, 0.7f, 0.7f, 1.0f };
        public static final float[] PLASTIC_SPECULAR = { 0.3f, 0.3f, 0.3f, 1.0f };
        public static final float PLASTIC_SHININESS = 20.0f;
    }

    // WOOD - Low shininess, brown tones
    public static class Wood {
        // Oak - Medium brown
        public static final float[] OAK_AMBIENT = { 0.25f, 0.15f, 0.08f, 1.0f };
        public static final float[] OAK_DIFFUSE = { 0.6f, 0.4f, 0.2f, 1.0f };
        public static final float[] OAK_SPECULAR = { 0.15f, 0.1f, 0.05f, 1.0f };
        public static final float OAK_SHININESS = 8.0f;

        // Pine - Light tan
        public static final float[] PINE_AMBIENT = { 0.3f, 0.25f, 0.15f, 1.0f };
        public static final float[] PINE_DIFFUSE = { 0.7f, 0.6f, 0.4f, 1.0f };
        public static final float[] PINE_SPECULAR = { 0.12f, 0.1f, 0.05f, 1.0f };
        public static final float PINE_SHININESS = 6.0f;

        // Walnut - Dark brown
        public static final float[] WALNUT_AMBIENT = { 0.2f, 0.12f, 0.08f, 1.0f };
        public static final float[] WALNUT_DIFFUSE = { 0.5f, 0.3f, 0.2f, 1.0f };
        public static final float[] WALNUT_SPECULAR = { 0.1f, 0.08f, 0.05f, 1.0f };
        public static final float WALNUT_SHININESS = 5.0f;
    }

    // COMPOSITES
    public static class Composites {
        // Carbon Fiber - Black with slight sheen
        public static final float[] CARBON_AMBIENT = { 0.05f, 0.05f, 0.05f, 1.0f };
        public static final float[] CARBON_DIFFUSE = { 0.15f, 0.15f, 0.15f, 1.0f };
        public static final float[] CARBON_SPECULAR = { 0.4f, 0.4f, 0.4f, 1.0f };
        public static final float CARBON_SHININESS = 40.0f;

        // Fiberglass - Off-white
        public static final float[] FIBERGLASS_AMBIENT = { 0.3f, 0.3f, 0.28f, 1.0f };
        public static final float[] FIBERGLASS_DIFFUSE = { 0.8f, 0.8f, 0.75f, 1.0f };
        public static final float[] FIBERGLASS_SPECULAR = { 0.25f, 0.25f, 0.25f, 1.0f };
        public static final float FIBERGLASS_SHININESS = 15.0f;
    }

    // CERAMICS - High shininess, smooth
    public static class Ceramics {
        // Glass - Transparent
        public static final float[] GLASS_AMBIENT = { 0.25f, 0.28f, 0.3f, 0.7f };
        public static final float[] GLASS_DIFFUSE = { 0.7f, 0.75f, 0.8f, 0.7f };
        public static final float[] GLASS_SPECULAR = { 0.9f, 0.9f, 0.9f, 0.7f };
        public static final float GLASS_SHININESS = 100.0f;

        // Ceramic - White/beige
        public static final float[] CERAMIC_AMBIENT = { 0.3f, 0.3f, 0.28f, 1.0f };
        public static final float[] CERAMIC_DIFFUSE = { 0.85f, 0.85f, 0.8f, 1.0f };
        public static final float[] CERAMIC_SPECULAR = { 0.6f, 0.6f, 0.6f, 1.0f };
        public static final float CERAMIC_SHININESS = 70.0f;
    }

    // CONSTRUCTION MATERIALS
    public static class Construction {
        // Concrete - Gray matte
        public static final float[] CONCRETE_AMBIENT = { 0.2f, 0.2f, 0.2f, 1.0f };
        public static final float[] CONCRETE_DIFFUSE = { 0.5f, 0.5f, 0.5f, 1.0f };
        public static final float[] CONCRETE_SPECULAR = { 0.05f, 0.05f, 0.05f, 1.0f };
        public static final float CONCRETE_SHININESS = 3.0f;

        // Stone - Natural gray/tan
        public static final float[] STONE_AMBIENT = { 0.22f, 0.2f, 0.18f, 1.0f };
        public static final float[] STONE_DIFFUSE = { 0.55f, 0.5f, 0.45f, 1.0f };
        public static final float[] STONE_SPECULAR = { 0.1f, 0.1f, 0.1f, 1.0f };
        public static final float STONE_SHININESS = 5.0f;
    }

    // ELASTOMERS - Low shininess, dark
    public static class Elastomers {
        // Rubber - Black matte
        public static final float[] RUBBER_AMBIENT = { 0.08f, 0.08f, 0.08f, 1.0f };
        public static final float[] RUBBER_DIFFUSE = { 0.2f, 0.2f, 0.2f, 1.0f };
        public static final float[] RUBBER_SPECULAR = { 0.05f, 0.05f, 0.05f, 1.0f };
        public static final float RUBBER_SHININESS = 2.0f;
    }

    // DEFAULT - Generic gray
    public static final float[] DEFAULT_AMBIENT = { 0.2f, 0.2f, 0.2f, 1.0f };
    public static final float[] DEFAULT_DIFFUSE = { 0.6f, 0.6f, 0.6f, 1.0f };
    public static final float[] DEFAULT_SPECULAR = { 0.3f, 0.3f, 0.3f, 1.0f };
    public static final float DEFAULT_SHININESS = 32.0f;
}
