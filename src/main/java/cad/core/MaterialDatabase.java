package cad.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MaterialDatabase {
        private static MaterialDatabase instance;
        private List<Material> materials;
        private String dataFilePath;
        private Gson gson;

        private MaterialDatabase() {
                this.materials = new ArrayList<>();
                this.gson = new GsonBuilder().setPrettyPrinting().create();

                
                
                this.dataFilePath = Paths.get("materials.json").toAbsolutePath().toString();
                

                load();
        }

        public static MaterialDatabase getInstance() {
                if (instance == null) {
                        instance = new MaterialDatabase();
                }
                return instance;
        }

        public void load() {
                File file = new File(dataFilePath);

                if (!file.exists()) {
                        
                        loadDefaultMaterials();
                        save();
                        return;
                }

                try (Reader reader = new FileReader(file)) {
                        Type listType = new TypeToken<ArrayList<Material>>() {
                        }.getType();
                        List<Material> loaded = gson.fromJson(reader, listType);
                        if (loaded != null) {
                                materials = loaded;
                        } else {
                                loadDefaultMaterials();
                        }
                } catch (IOException e) {
                        System.err.println("Error loading materials: " + e.getMessage());
                        loadDefaultMaterials();
                }
        }

        public void save() {
                try (Writer writer = new FileWriter(dataFilePath)) {
                        gson.toJson(materials, writer);
                } catch (IOException e) {
                        System.err.println("Error saving materials: " + e.getMessage());
                }
        }

        private void loadDefaultMaterials() {
                
                
                
                
                
                

                materials.clear();

                
                materials.add(new Material("Aluminum 1100", 2710.0, "Metals",
                                "99% pure aluminum, excellent corrosion resistance and workability",
                                MaterialColors.Metals.ALUMINUM_AMBIENT, MaterialColors.Metals.ALUMINUM_DIFFUSE,
                                MaterialColors.Metals.ALUMINUM_SPECULAR, MaterialColors.Metals.ALUMINUM_SHININESS));
                materials.add(new Material("Aluminum 2024", 2780.0, "Metals",
                                "High-strength aluminum alloy used in aircraft structures",
                                MaterialColors.Metals.ALUMINUM_AMBIENT, MaterialColors.Metals.ALUMINUM_DIFFUSE,
                                MaterialColors.Metals.ALUMINUM_SPECULAR, MaterialColors.Metals.ALUMINUM_SHININESS));
                materials.add(new Material("Aluminum 6061", 2700.0, "Metals",
                                "Versatile structural aluminum alloy, excellent machinability",
                                MaterialColors.Metals.ALUMINUM_AMBIENT, MaterialColors.Metals.ALUMINUM_DIFFUSE,
                                MaterialColors.Metals.ALUMINUM_SPECULAR, MaterialColors.Metals.ALUMINUM_SHININESS));
                materials.add(new Material("Aluminum 7075", 2810.0, "Metals",
                                "Very high strength aluminum alloy, aerospace applications",
                                MaterialColors.Metals.ALUMINUM_AMBIENT, MaterialColors.Metals.ALUMINUM_DIFFUSE,
                                MaterialColors.Metals.ALUMINUM_SPECULAR, MaterialColors.Metals.ALUMINUM_SHININESS));

                
                materials.add(new Material("Steel 1020", 7850.0, "Metals",
                                "Low carbon steel, general purpose structural applications",
                                MaterialColors.Metals.STEEL_AMBIENT, MaterialColors.Metals.STEEL_DIFFUSE,
                                MaterialColors.Metals.STEEL_SPECULAR, MaterialColors.Metals.STEEL_SHININESS));
                materials.add(new Material("Steel 4140", 7850.0, "Metals",
                                "Chromium-molybdenum alloy steel, high strength and toughness",
                                MaterialColors.Metals.STEEL_AMBIENT, MaterialColors.Metals.STEEL_DIFFUSE,
                                MaterialColors.Metals.STEEL_SPECULAR, MaterialColors.Metals.STEEL_SHININESS));
                materials.add(new Material("Stainless Steel 304", 8000.0, "Metals",
                                "Austenitic stainless steel, excellent corrosion resistance",
                                MaterialColors.Metals.STAINLESS_AMBIENT, MaterialColors.Metals.STAINLESS_DIFFUSE,
                                MaterialColors.Metals.STAINLESS_SPECULAR, MaterialColors.Metals.STAINLESS_SHININESS));
                materials.add(new Material("Stainless Steel 316", 8000.0, "Metals",
                                "Marine grade stainless steel, superior corrosion resistance",
                                MaterialColors.Metals.STAINLESS_AMBIENT, MaterialColors.Metals.STAINLESS_DIFFUSE,
                                MaterialColors.Metals.STAINLESS_SPECULAR, MaterialColors.Metals.STAINLESS_SHININESS));
                materials.add(new Material("Tool Steel A2", 7860.0, "Metals",
                                "Air-hardening tool steel, good toughness and machinability",
                                MaterialColors.Metals.STEEL_AMBIENT, MaterialColors.Metals.STEEL_DIFFUSE,
                                MaterialColors.Metals.STEEL_SPECULAR, MaterialColors.Metals.STEEL_SHININESS));

                
                materials.add(new Material("Titanium Ti-6Al-4V", 4430.0, "Metals",
                                "Grade 5 titanium, aerospace standard, excellent strength-to-weight",
                                MaterialColors.Metals.TITANIUM_AMBIENT, MaterialColors.Metals.TITANIUM_DIFFUSE,
                                MaterialColors.Metals.TITANIUM_SPECULAR, MaterialColors.Metals.TITANIUM_SHININESS));
                materials.add(new Material("Titanium Grade 2", 4510.0, "Metals",
                                "Commercially pure titanium, good corrosion resistance",
                                MaterialColors.Metals.TITANIUM_AMBIENT, MaterialColors.Metals.TITANIUM_DIFFUSE,
                                MaterialColors.Metals.TITANIUM_SPECULAR, MaterialColors.Metals.TITANIUM_SHININESS));

                
                materials.add(new Material("Copper", 8960.0, "Metals",
                                "Pure copper, excellent electrical and thermal conductivity",
                                MaterialColors.Metals.COPPER_AMBIENT, MaterialColors.Metals.COPPER_DIFFUSE,
                                MaterialColors.Metals.COPPER_SPECULAR, MaterialColors.Metals.COPPER_SHININESS));
                materials.add(new Material("Brass (60/40)", 8500.0, "Metals",
                                "60% copper, 40% zinc, good machinability and corrosion resistance",
                                MaterialColors.Metals.BRASS_AMBIENT, MaterialColors.Metals.BRASS_DIFFUSE,
                                MaterialColors.Metals.BRASS_SPECULAR, MaterialColors.Metals.BRASS_SHININESS));
                materials.add(new Material("Bronze", 8800.0, "Metals",
                                "Copper-tin alloy, wear resistant, marine applications",
                                new float[] { 0.28f, 0.18f, 0.1f, 1.0f }, new float[] { 0.65f, 0.45f, 0.3f, 1.0f },
                                new float[] { 0.75f, 0.55f, 0.4f, 1.0f }, 65.0f));
                materials.add(new Material("Magnesium AZ31B", 1770.0, "Metals",
                                "Lightweight structural magnesium alloy",
                                new float[] { 0.28f, 0.28f, 0.28f, 1.0f }, new float[] { 0.68f, 0.68f, 0.68f, 1.0f },
                                new float[] { 0.8f, 0.8f, 0.8f, 1.0f }, 85.0f));
                materials.add(new Material("Zinc", 7140.0, "Metals",
                                "Pure zinc, die casting applications",
                                new float[] { 0.22f, 0.24f, 0.26f, 1.0f }, new float[] { 0.55f, 0.60f, 0.65f, 1.0f },
                                new float[] { 0.7f, 0.75f, 0.8f, 1.0f }, 70.0f));
                materials.add(new Material("Lead", 11340.0, "Metals",
                                "Dense metal, radiation shielding, weights",
                                new float[] { 0.15f, 0.15f, 0.18f, 1.0f }, new float[] { 0.35f, 0.35f, 0.4f, 1.0f },
                                new float[] { 0.25f, 0.25f, 0.3f, 1.0f }, 25.0f));
                materials.add(new Material("Nickel 200", 8890.0, "Metals",
                                "Commercially pure nickel, excellent corrosion resistance",
                                new float[] { 0.25f, 0.25f, 0.25f, 1.0f }, new float[] { 0.62f, 0.62f, 0.62f, 1.0f },
                                new float[] { 0.78f, 0.78f, 0.78f, 1.0f }, 88.0f));

                
                materials.add(new Material("ABS", 1050.0, "Plastics",
                                "Acrylonitrile Butadiene Styrene, tough impact-resistant plastic",
                                MaterialColors.Plastics.ABS_AMBIENT, MaterialColors.Plastics.ABS_DIFFUSE,
                                MaterialColors.Plastics.ABS_SPECULAR, MaterialColors.Plastics.ABS_SHININESS));
                materials.add(new Material("PLA", 1240.0, "Plastics",
                                "Polylactic Acid, biodegradable, common 3D printing filament",
                                MaterialColors.Plastics.PLA_AMBIENT, MaterialColors.Plastics.PLA_DIFFUSE,
                                MaterialColors.Plastics.PLA_SPECULAR, MaterialColors.Plastics.PLA_SHININESS));
                materials.add(new Material("PETG", 1270.0, "Plastics",
                                "Glycol-modified PET, tough, clear, chemical resistant",
                                new float[] { 0.28f, 0.3f, 0.32f, 0.95f }, new float[] { 0.75f, 0.8f, 0.85f, 0.95f },
                                new float[] { 0.5f, 0.5f, 0.5f, 0.95f }, 40.0f));
                materials.add(new Material("Nylon 6", 1140.0, "Plastics",
                                "Polyamide, high strength, wear resistant",
                                new float[] { 0.3f, 0.3f, 0.28f, 1.0f }, new float[] { 0.8f, 0.8f, 0.75f, 1.0f },
                                new float[] { 0.35f, 0.35f, 0.35f, 1.0f }, 28.0f));
                materials.add(new Material("Nylon 66", 1150.0, "Plastics",
                                "Engineering polyamide, higher melting point than Nylon 6",
                                new float[] { 0.3f, 0.3f, 0.28f, 1.0f }, new float[] { 0.8f, 0.8f, 0.75f, 1.0f },
                                new float[] { 0.35f, 0.35f, 0.35f, 1.0f }, 28.0f));
                materials.add(new Material("Polycarbonate", 1200.0, "Plastics",
                                "PC, impact-resistant transparent thermoplastic",
                                MaterialColors.Plastics.POLYCARBONATE_AMBIENT,
                                MaterialColors.Plastics.POLYCARBONATE_DIFFUSE,
                                MaterialColors.Plastics.POLYCARBONATE_SPECULAR,
                                MaterialColors.Plastics.POLYCARBONATE_SHININESS));
                materials.add(new Material("Polypropylene", 900.0, "Plastics",
                                "PP, lightweight, chemical resistant, flexible",
                                new float[] { 0.28f, 0.28f, 0.3f, 1.0f }, new float[] { 0.75f, 0.75f, 0.8f, 1.0f },
                                new float[] { 0.25f, 0.25f, 0.25f, 1.0f }, 18.0f));
                materials.add(new Material("Polyethylene HD", 960.0, "Plastics",
                                "High-density polyethylene, rigid, chemical resistant",
                                new float[] { 0.25f, 0.25f, 0.25f, 1.0f }, new float[] { 0.7f, 0.7f, 0.7f, 1.0f },
                                new float[] { 0.2f, 0.2f, 0.2f, 1.0f }, 15.0f));
                materials.add(new Material("Polyethylene LD", 920.0, "Plastics",
                                "Low-density polyethylene, flexible film applications",
                                new float[] { 0.25f, 0.25f, 0.25f, 1.0f }, new float[] { 0.7f, 0.7f, 0.7f, 1.0f },
                                new float[] { 0.15f, 0.15f, 0.15f, 1.0f }, 10.0f));
                materials.add(new Material("PVC Rigid", 1400.0, "Plastics",
                                "Polyvinyl chloride, pipes, construction applications",
                                new float[] { 0.22f, 0.22f, 0.22f, 1.0f }, new float[] { 0.65f, 0.65f, 0.65f, 1.0f },
                                new float[] { 0.25f, 0.25f, 0.25f, 1.0f }, 22.0f));
                materials.add(new Material("Acrylic (PMMA)", 1180.0, "Plastics",
                                "Polymethyl methacrylate, transparent, weather resistant",
                                new float[] { 0.3f, 0.3f, 0.32f, 0.9f }, new float[] { 0.8f, 0.8f, 0.85f, 0.9f },
                                new float[] { 0.6f, 0.6f, 0.6f, 0.9f }, 60.0f));
                materials.add(new Material("Polystyrene", 1050.0, "Plastics",
                                "PS, rigid transparent plastic, disposable applications",
                                new float[] { 0.28f, 0.28f, 0.3f, 0.95f }, new float[] { 0.75f, 0.75f, 0.8f, 0.95f },
                                new float[] { 0.5f, 0.5f, 0.5f, 0.95f }, 45.0f));
                materials.add(new Material("TPU", 1200.0, "Plastics",
                                "Thermoplastic polyurethane, flexible, abrasion resistant",
                                new float[] { 0.18f, 0.18f, 0.2f, 1.0f }, new float[] { 0.5f, 0.5f, 0.55f, 1.0f },
                                new float[] { 0.2f, 0.2f, 0.2f, 1.0f }, 12.0f));
                materials.add(new Material("PEEK", 1320.0, "Plastics",
                                "Polyether ether ketone, high-performance engineering plastic",
                                new float[] { 0.22f, 0.2f, 0.18f, 1.0f }, new float[] { 0.6f, 0.55f, 0.5f, 1.0f },
                                new float[] { 0.3f, 0.3f, 0.3f, 1.0f }, 35.0f));
                materials.add(new Material("Delrin (Acetal)", 1420.0, "Plastics",
                                "POM, excellent dimensional stability and machinability",
                                new float[] { 0.3f, 0.3f, 0.3f, 1.0f }, new float[] { 0.82f, 0.82f, 0.82f, 1.0f },
                                new float[] { 0.35f, 0.35f, 0.35f, 1.0f }, 30.0f));

                
                materials.add(new Material("Carbon Fiber Composite", 1600.0, "Composites",
                                "Carbon fiber reinforced polymer, very high strength-to-weight",
                                MaterialColors.Composites.CARBON_AMBIENT, MaterialColors.Composites.CARBON_DIFFUSE,
                                MaterialColors.Composites.CARBON_SPECULAR, MaterialColors.Composites.CARBON_SHININESS));
                materials.add(new Material("Fiberglass (GFRP)", 1900.0, "Composites",
                                "Glass fiber reinforced plastic, good strength, corrosion resistant",
                                MaterialColors.Composites.FIBERGLASS_AMBIENT,
                                MaterialColors.Composites.FIBERGLASS_DIFFUSE,
                                MaterialColors.Composites.FIBERGLASS_SPECULAR,
                                MaterialColors.Composites.FIBERGLASS_SHININESS));
                materials.add(new Material("G-10/FR4", 1850.0, "Composites",
                                "Glass-epoxy laminate, electrical insulation, PCB substrate",
                                new float[] { 0.18f, 0.25f, 0.15f, 1.0f }, new float[] { 0.45f, 0.6f, 0.4f, 1.0f },
                                new float[] { 0.2f, 0.2f, 0.2f, 1.0f }, 20.0f));

                
                materials.add(new Material("Oak", 760.0, "Wood",
                                "Hardwood, strong and durable, furniture and construction",
                                MaterialColors.Wood.OAK_AMBIENT, MaterialColors.Wood.OAK_DIFFUSE,
                                MaterialColors.Wood.OAK_SPECULAR, MaterialColors.Wood.OAK_SHININESS));
                materials.add(new Material("Maple", 705.0, "Wood",
                                "Hardwood, fine grain, furniture and flooring",
                                new float[] { 0.3f, 0.25f, 0.18f, 1.0f }, new float[] { 0.7f, 0.6f, 0.45f, 1.0f },
                                new float[] { 0.12f, 0.1f, 0.06f, 1.0f }, 7.0f));
                materials.add(new Material("Pine", 550.0, "Wood",
                                "Softwood, lightweight, construction and furniture",
                                MaterialColors.Wood.PINE_AMBIENT, MaterialColors.Wood.PINE_DIFFUSE,
                                MaterialColors.Wood.PINE_SPECULAR, MaterialColors.Wood.PINE_SHININESS));
                materials.add(new Material("Birch", 670.0, "Wood",
                                "Hardwood, fine grain, plywood and furniture",
                                new float[] { 0.32f, 0.28f, 0.2f, 1.0f }, new float[] { 0.75f, 0.65f, 0.5f, 1.0f },
                                new float[] { 0.1f, 0.08f, 0.05f, 1.0f }, 6.0f));
                materials.add(new Material("Walnut", 660.0, "Wood",
                                "Hardwood, rich color, high-end furniture",
                                MaterialColors.Wood.WALNUT_AMBIENT, MaterialColors.Wood.WALNUT_DIFFUSE,
                                MaterialColors.Wood.WALNUT_SPECULAR, MaterialColors.Wood.WALNUT_SHININESS));
                materials.add(new Material("Plywood", 600.0, "Wood",
                                "Engineered wood product, layered veneers",
                                new float[] { 0.28f, 0.22f, 0.16f, 1.0f }, new float[] { 0.65f, 0.55f, 0.4f, 1.0f },
                                new float[] { 0.08f, 0.07f, 0.05f, 1.0f }, 4.0f));
                materials.add(new Material("MDF", 750.0, "Wood",
                                "Medium-density fiberboard, smooth surface for finishing",
                                new float[] { 0.25f, 0.2f, 0.15f, 1.0f }, new float[] { 0.6f, 0.5f, 0.38f, 1.0f },
                                new float[] { 0.05f, 0.05f, 0.05f, 1.0f }, 3.0f));

                
                materials.add(new Material("Glass (Soda-lime)", 2500.0, "Ceramics",
                                "Standard window glass, bottles, containers",
                                MaterialColors.Ceramics.GLASS_AMBIENT, MaterialColors.Ceramics.GLASS_DIFFUSE,
                                MaterialColors.Ceramics.GLASS_SPECULAR, MaterialColors.Ceramics.GLASS_SHININESS));
                materials.add(new Material("Glass (Borosilicate)", 2230.0, "Ceramics",
                                "Pyrex, laboratory glassware, heat resistant",
                                MaterialColors.Ceramics.GLASS_AMBIENT, MaterialColors.Ceramics.GLASS_DIFFUSE,
                                MaterialColors.Ceramics.GLASS_SPECULAR, MaterialColors.Ceramics.GLASS_SHININESS));
                materials.add(new Material("Alumina Ceramic", 3900.0, "Ceramics",
                                "Aluminum oxide, high hardness, electrical insulation",
                                MaterialColors.Ceramics.CERAMIC_AMBIENT, MaterialColors.Ceramics.CERAMIC_DIFFUSE,
                                MaterialColors.Ceramics.CERAMIC_SPECULAR, MaterialColors.Ceramics.CERAMIC_SHININESS));
                materials.add(new Material("Silicon Carbide", 3100.0, "Ceramics",
                                "Very hard ceramic, abrasives, high temperature applications",
                                new float[] { 0.12f, 0.12f, 0.12f, 1.0f }, new float[] { 0.3f, 0.3f, 0.3f, 1.0f },
                                new float[] { 0.5f, 0.5f, 0.5f, 1.0f }, 80.0f));
                materials.add(new Material("Porcelain", 2400.0, "Ceramics",
                                "Dense ceramic, electrical insulation, decorative",
                                new float[] { 0.32f, 0.32f, 0.3f, 1.0f }, new float[] { 0.9f, 0.9f, 0.85f, 1.0f },
                                new float[] { 0.65f, 0.65f, 0.65f, 1.0f }, 75.0f));

                
                materials.add(new Material("Concrete", 2400.0, "Construction",
                                "Standard concrete mix, structural applications",
                                MaterialColors.Construction.CONCRETE_AMBIENT,
                                MaterialColors.Construction.CONCRETE_DIFFUSE,
                                MaterialColors.Construction.CONCRETE_SPECULAR,
                                MaterialColors.Construction.CONCRETE_SHININESS));
                materials.add(new Material("Concrete (Lightweight)", 1800.0, "Construction",
                                "Aerated or aggregate concrete, non-structural",
                                new float[] { 0.22f, 0.22f, 0.22f, 1.0f }, new float[] { 0.55f, 0.55f, 0.55f, 1.0f },
                                new float[] { 0.03f, 0.03f, 0.03f, 1.0f }, 2.0f));
                materials.add(new Material("Brick", 1920.0, "Construction",
                                "Clay brick, construction and masonry",
                                new float[] { 0.25f, 0.15f, 0.1f, 1.0f }, new float[] { 0.6f, 0.35f, 0.25f, 1.0f },
                                new float[] { 0.08f, 0.05f, 0.05f, 1.0f }, 4.0f));
                materials.add(new Material("Granite", 2750.0, "Construction",
                                "Natural stone, countertops, monuments",
                                MaterialColors.Construction.STONE_AMBIENT, MaterialColors.Construction.STONE_DIFFUSE,
                                MaterialColors.Construction.STONE_SPECULAR,
                                MaterialColors.Construction.STONE_SHININESS));
                materials.add(new Material("Marble", 2700.0, "Construction",
                                "Natural stone, decorative applications",
                                new float[] { 0.28f, 0.28f, 0.3f, 1.0f }, new float[] { 0.85f, 0.85f, 0.9f, 1.0f },
                                new float[] { 0.4f, 0.4f, 0.4f, 1.0f }, 35.0f));

                
                materials.add(new Material("Natural Rubber", 920.0, "Elastomers",
                                "Elastic polymer, seals, tires",
                                MaterialColors.Elastomers.RUBBER_AMBIENT, MaterialColors.Elastomers.RUBBER_DIFFUSE,
                                MaterialColors.Elastomers.RUBBER_SPECULAR, MaterialColors.Elastomers.RUBBER_SHININESS));
                materials.add(new Material("Neoprene", 1250.0, "Elastomers",
                                "Synthetic rubber, oil and weather resistant",
                                new float[] { 0.1f, 0.1f, 0.1f, 1.0f }, new float[] { 0.25f, 0.25f, 0.25f, 1.0f },
                                new float[] { 0.03f, 0.03f, 0.03f, 1.0f }, 1.5f));
                materials.add(new Material("Silicone Rubber", 1100.0, "Elastomers",
                                "High temperature resistant elastomer",
                                new float[] { 0.1f, 0.1f, 0.1f, 1.0f }, new float[] { 0.28f, 0.28f, 0.28f, 1.0f },
                                new float[] { 0.05f, 0.05f, 0.05f, 1.0f }, 2.0f));

                
                materials.add(new Material("Graphite", 2260.0, "Other",
                                "Crystalline carbon, lubricant, electrodes",
                                new float[] { 0.08f, 0.08f, 0.08f, 1.0f }, new float[] { 0.18f, 0.18f, 0.18f, 1.0f },
                                new float[] { 0.25f, 0.25f, 0.25f, 1.0f }, 15.0f));
                materials.add(new Material("Teflon (PTFE)", 2200.0, "Other",
                                "Polytetrafluoroethylene, very low friction, chemical resistant",
                                new float[] { 0.28f, 0.28f, 0.3f, 1.0f }, new float[] { 0.75f, 0.75f, 0.8f, 1.0f },
                                new float[] { 0.15f, 0.15f, 0.15f, 1.0f }, 8.0f));
                materials.add(new Material("Foam (Polyurethane)", 32.0, "Other",
                                "Lightweight cellular foam, cushioning, insulation",
                                new float[] { 0.3f, 0.3f, 0.28f, 1.0f }, new float[] { 0.82f, 0.82f, 0.75f, 1.0f },
                                new float[] { 0.05f, 0.05f, 0.05f, 1.0f }, 1.0f));
        }

        public Material getMaterial(String name) {
                return materials.stream()
                                .filter(m -> m.getName().equalsIgnoreCase(name))
                                .findFirst()
                                .orElse(null);
        }

        public boolean addMaterial(Material material) {
                if (getMaterial(material.getName()) != null) {
                        return false; 
                }
                materials.add(material);
                save();
                return true;
        }

        public boolean removeMaterial(String name) {
                boolean removed = materials.removeIf(m -> m.getName().equalsIgnoreCase(name));
                if (removed) {
                        save();
                }
                return removed;
        }

        public boolean updateMaterial(String name, Material updated) {
                Material existing = getMaterial(name);
                if (existing == null) {
                        return false;
                }

                existing.setName(updated.getName());
                existing.setDensity(updated.getDensity());
                existing.setCategory(updated.getCategory());
                existing.setDescription(updated.getDescription());
                save();
                return true;
        }

        public List<Material> getAllMaterials() {
                return new ArrayList<>(materials);
        }

        public List<Material> getMaterialsByCategory(String category) {
                return materials.stream()
                                .filter(m -> m.getCategory().equalsIgnoreCase(category))
                                .collect(Collectors.toList());
        }

        public List<String> getCategories() {
                return materials.stream()
                                .map(Material::getCategory)
                                .distinct()
                                .sorted()
                                .collect(Collectors.toList());
        }

        public String getDataFilePath() {
                return dataFilePath;
        }
}
