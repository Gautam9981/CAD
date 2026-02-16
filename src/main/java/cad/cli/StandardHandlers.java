package cad.cli;

import cad.core.*;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class StandardHandlers {

    public static void registerAll(CommandManager commandManager, Sketch sketch) {

        // --- System ---
        CommandRegistry.register("help", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        System.out.println("Available commands:");
                        CommandRegistry.getHelpMap().forEach((k, v) -> System.out.printf("  %-15s %s%n", k, v));
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Help";
                    }
                };
            }

            public String getUsage() {
                return "help - List commands";
            }
        });
        CommandRegistry.registerAlias("h", "help");

        CommandRegistry.register("exit", new CliHandler() {
            public Command createCommand(String[] args) {
                System.exit(0);
                return null;
            }

            public String getUsage() {
                return "exit - Quit application";
            }
        });
        CommandRegistry.registerAlias("e", "exit");
        CommandRegistry.registerAlias("quit", "exit");

        CommandRegistry.register("version", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        System.out.println("SketchApp, version 2.5 (Beta)");
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Version Info";
                    }
                };
            }

            public String getUsage() {
                return "version - Show version";
            }
        });
        CommandRegistry.registerAlias("v", "version");

        // --- Sketching ---
        CommandRegistry.register("sketch_point", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 3)
                    throw new IllegalArgumentException("Usage: sketch_point <x> <y>");
                float x = Float.parseFloat(args[1]);
                float y = Float.parseFloat(args[2]);
                return new AddEntityCommand(sketch, new Sketch.PointEntity(x, y), "Point");
            }

            public String getUsage() {
                return "sketch_point <x> <y>";
            }
        });

        CommandRegistry.register("sketch_line", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 5)
                    throw new IllegalArgumentException("Usage: sketch_line <x1> <y1> <x2> <y2>");
                float x1 = Float.parseFloat(args[1]);
                float y1 = Float.parseFloat(args[2]);
                float x2 = Float.parseFloat(args[3]);
                float y2 = Float.parseFloat(args[4]);
                return new AddEntityCommand(sketch, new Sketch.Line(x1, y1, x2, y2), "Line");
            }

            public String getUsage() {
                return "sketch_line <x1> <y1> <x2> <y2>";
            }
        });
        CommandRegistry.registerAlias("line", "sketch_line");
        CommandRegistry.registerAlias("l", "sketch_line");

        CommandRegistry.register("sketch_circle", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 4)
                    throw new IllegalArgumentException("Usage: sketch_circle <x> <y> <r>");
                float x = Float.parseFloat(args[1]);
                float y = Float.parseFloat(args[2]);
                float r = Float.parseFloat(args[3]);
                return new AddEntityCommand(sketch, new Sketch.Circle(x, y, r), "Circle");
            }

            public String getUsage() {
                return "sketch_circle <x> <y> <r>";
            }
        });
        CommandRegistry.registerAlias("circle", "sketch_circle");

        CommandRegistry.register("sketch_polygon", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 5)
                    throw new IllegalArgumentException(
                            "Usage: sketch_polygon <x> <y> <radius> <sides> [circumscribed]");
                float x = Float.parseFloat(args[1]);
                float y = Float.parseFloat(args[2]);
                float r = Float.parseFloat(args[3]);
                int sides = Integer.parseInt(args[4]);
                boolean circum = (args.length > 5) && Boolean.parseBoolean(args[5]);
                return new Command() {
                    public void execute() {
                        sketch.addNSidedPolygon(x, y, r, sides, circum);
                        System.out.println("Polygon added.");
                    }

                    public void undo() {
                        // Simple undo could be: remove last entity if we track it.
                        // But for now, standard sketch undo logic applies if we had it.
                    }

                    public String getDescription() {
                        return "Polygon";
                    }
                };
            }

            public String getUsage() {
                return "sketch_polygon <x> <y> <r> <sides> [circum]";
            }
        });
        CommandRegistry.registerAlias("poly", "sketch_polygon");

        CommandRegistry.register("sketch_clear", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        sketch.clearSketch();
                        System.out.println("Sketch cleared.");
                    }

                    public void undo() {
                        /* Undo clear is hard without full state save */ }

                    public String getDescription() {
                        return "Clear Sketch";
                    }
                };
            }

            public String getUsage() {
                return "sketch_clear - Remove all sketch entities";
            }
        });

        // --- 3D Primitive Commands ---
        CommandRegistry.register("cube", new CliHandler() {
            @Override
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: cube <size>");
                float size = Float.parseFloat(args[1]);
                int div = Geometry.getCubeDivisions(); // We might want to pass this or get from global
                return new CreateCubeCommand(size, div);
            }

            @Override
            public String getUsage() {
                return "cube <size> - Create a cube";
            }
        });
        CommandRegistry.registerAlias("c", "cube");

        CommandRegistry.register("sphere", new CliHandler() {
            @Override
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: sphere <radius>");
                float radius = Float.parseFloat(args[1]);
                return new CreateSphereCommand(radius, Geometry.sphereLatDiv, Geometry.sphereLonDiv);
            }

            @Override
            public String getUsage() {
                return "sphere <radius> - Create a sphere";
            }
        });
        CommandRegistry.registerAlias("sp", "sphere");

        // --- 3D Operations ---
        CommandRegistry.register("extrude", new CliHandler() {
            @Override
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: extrude <height>");
                float height = Float.parseFloat(args[1]);
                return new CreateExtrudeCommand(sketch, height);
            }

            @Override
            public String getUsage() {
                return "extrude <height> - Extrude 2D sketch";
            }
        });
        CommandRegistry.registerAlias("ext", "extrude");

        CommandRegistry.register("revolve", new CliHandler() {
            @Override
            public Command createCommand(String[] args) {
                float angle = 360.0f;
                int steps = 60;
                String axis = "Y";
                if (args.length >= 2)
                    angle = Float.parseFloat(args[1]);
                if (args.length >= 3)
                    steps = Integer.parseInt(args[2]);
                return new CreateRevolveCommand(sketch, axis, angle, steps);
            }

            @Override
            public String getUsage() {
                return "revolve [angle] - Revolve sketch around Y axis";
            }
        });

        // --- Inspection ---
        CommandRegistry.register("inspect", new CliHandler() {
            @Override
            public Command createCommand(String[] args) {
                return new InspectStateCommand(sketch);
            }

            @Override
            public String getUsage() {
                return "inspect - Show current state as JSON";
            }
        });

        // --- System / History ---
        CommandRegistry.register("undo", new CliHandler() {
            @Override
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        if (commandManager.undo())
                            System.out.println("Undone.");
                        else
                            System.out.println("Nothing to undo.");
                    }

                    public void undo() {
                        /* No-op or 'Redo' logic? usually history commands are meta */ }

                    public String getDescription() {
                        return "Undo";
                    }
                };
            }

            @Override
            public String getUsage() {
                return "undo - Undo last action";
            }
        });
        CommandRegistry.registerAlias("u", "undo");

        CommandRegistry.register("cut", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: cut <depth>");
                float depth = Float.parseFloat(args[1]);
                return new CreateExtrudeCommand(sketch, depth, Geometry.BooleanOp.DIFFERENCE);
            }

            public String getUsage() {
                return "cut <depth> - Extrude Cut";
            }
        });

        CommandRegistry.register("intersect", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: intersect <depth>");
                float depth = Float.parseFloat(args[1]);
                return new CreateExtrudeCommand(sketch, depth, Geometry.BooleanOp.INTERSECTION);
            }

            public String getUsage() {
                return "intersect <depth> - Extrude Intersect";
            }
        });

        CommandRegistry.register("sweep", new CliHandler() {
            public Command createCommand(String[] args) {
                return new CreateSweepCommand(sketch, Geometry.BooleanOp.UNION);
            }

            public String getUsage() {
                return "sweep - Sweep profile along path";
            }
        });

        CommandRegistry.register("loft", new CliHandler() {
            public Command createCommand(String[] args) {
                return new CreateLoftCommand(sketch, Geometry.BooleanOp.UNION);
            }

            public String getUsage() {
                return "loft - Loft between profiles";
            }
        });

        CommandRegistry.register("kite", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 6)
                    throw new IllegalArgumentException("Usage: kite <cx> <cy> <v> <h> <a>");
                float cx = Float.parseFloat(args[1]);
                float cy = Float.parseFloat(args[2]);
                float v = Float.parseFloat(args[3]);
                float h = Float.parseFloat(args[4]);
                float a = Float.parseFloat(args[5]);
                return new Command() {
                    public void execute() {
                        sketch.addKite(cx, cy, v, h, a);
                        System.out.println("Kite added.");
                    }

                    public void undo() {
                    } // TODO

                    public String getDescription() {
                        return "Kite";
                    }
                };
            }

            public String getUsage() {
                return "kite <cx> <cy> <v> <h> <angle>";
            }
        });

        // --- Constraints ---
        CommandRegistry.register("list_entities", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        List<Sketch.Entity> entities = sketch.getEntities();
                        System.out.println("Entities:");
                        for (int i = 0; i < entities.size(); i++) {
                            System.out.printf("[%d] %s%n", i, entities.get(i).toString());
                        }
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "List Entities with ID";
                    }
                };
            }

            public String getUsage() {
                return "list_entities - List entities with IDs for constraints";
            }
        });

        CommandRegistry.register("constraint_tangent", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 3)
                    throw new IllegalArgumentException("Usage: constraint_tangent <id1> <id2>");
                int id1 = Integer.parseInt(args[1]);
                int id2 = Integer.parseInt(args[2]);
                return new AddConstraintCommand(sketch,
                        new TangentConstraint(sketch.getEntities().get(id1), sketch.getEntities().get(id2)));
            }

            public String getUsage() {
                return "constraint_tangent <id1> <id2>";
            }
        });

        CommandRegistry.register("constraint_concentric", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 3)
                    throw new IllegalArgumentException("Usage: constraint_concentric <id1> <id2>");
                int id1 = Integer.parseInt(args[1]);
                int id2 = Integer.parseInt(args[2]);
                return new AddConstraintCommand(sketch, new ConcentricConstraint(
                        (Sketch.Circle) sketch.getEntities().get(id1), (Sketch.Circle) sketch.getEntities().get(id2)));
            }

            public String getUsage() {
                return "constraint_concentric <circle_id1> <circle_id2>";
            }
        });

        CommandRegistry.register("solve", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        sketch.solveConstraints();
                        System.out.println("Constraints solved.");
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Solve Constraints";
                    }
                };
            }

            public String getUsage() {
                return "solve - Solve constraints";
            }
        });

        CommandRegistry.register("mass_props", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        float vol = Geometry.calculateVolume();
                        float area = Geometry.calculateSurfaceArea();
                        System.out.printf("Volume: %.4f, Surface Area: %.4f%n", vol, area);
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Mass Properties";
                    }
                };
            }

            public String getUsage() {
                return "mass_props - Calculate Volume and Area";
            }
        });

        CommandRegistry.register("constraint_horizontal", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: constraint_horizontal <line_id>");
                int id = Integer.parseInt(args[1]);
                Sketch.Entity e = sketch.getEntities().get(id);
                if (e instanceof Sketch.Line) {
                    Sketch.Line line = (Sketch.Line) e;
                    return new AddConstraintCommand(sketch,
                            new HorizontalConstraint(line.getStartPoint(), line.getEndPoint()));
                }
                throw new IllegalArgumentException("Entity " + id + " must be a Line for horizontal constraint.");
            }

            public String getUsage() {
                return "constraint_horizontal <line_id>";
            }
        });

        CommandRegistry.register("constraint_vertical", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: constraint_vertical <line_id>");
                int id = Integer.parseInt(args[1]);
                Sketch.Entity e = sketch.getEntities().get(id);
                if (e instanceof Sketch.Line) {
                    Sketch.Line line = (Sketch.Line) e;
                    return new AddConstraintCommand(sketch,
                            new VerticalConstraint(line.getStartPoint(), line.getEndPoint()));
                }
                throw new IllegalArgumentException("Entity " + id + " must be a Line for vertical constraint.");
            }

            public String getUsage() {
                return "constraint_vertical <line_id>";
            }
        });

        CommandRegistry.register("constraint_coincident", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 3)
                    throw new IllegalArgumentException("Usage: constraint_coincident <id1> <id2>");
                int id1 = Integer.parseInt(args[1]);
                int id2 = Integer.parseInt(args[2]);
                return new AddConstraintCommand(sketch,
                        new CoincidentConstraint((Sketch.PointEntity) sketch.getEntities().get(id1),
                                (Sketch.PointEntity) sketch.getEntities().get(id2)));
            }

            public String getUsage() {
                return "constraint_coincident <point_id1> <point_id2>";
            }
        });

        CommandRegistry.register("constraint_fixed", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: constraint_fixed <id>");
                int id = Integer.parseInt(args[1]);
                Sketch.Entity e = sketch.getEntities().get(id);
                if (e instanceof Sketch.PointEntity) {
                    return new AddConstraintCommand(sketch, new FixedConstraint(((Sketch.PointEntity) e).getPoint()));
                }
                throw new IllegalArgumentException("Entity " + id + " must be a Point for fixed constraint.");
            }

            public String getUsage() {
                return "constraint_fixed <id>";
            }
        });

        // --- Settings & File I/O ---
        CommandRegistry.register("cube_div", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: cube_div <count>");
                int count = Integer.parseInt(args[1]);
                if (count < 1 || count > 200)
                    throw new IllegalArgumentException("Divisions must be 1-200");
                Geometry.cubeDivisions = count;
                return new Command() {
                    public void execute() {
                        System.out.println("Cube divisions set to " + count);
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Set Cube Divisions";
                    }
                };
            }

            public String getUsage() {
                return "cube_div <count>";
            }
        });

        CommandRegistry.register("sphere_div", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 3)
                    throw new IllegalArgumentException("Usage: sphere_div <lat> <lon>");
                int lat = Integer.parseInt(args[1]);
                int lon = Integer.parseInt(args[2]);
                if (lat < 1 || lat > 200 || lon < 1 || lon > 200)
                    throw new IllegalArgumentException("Divisions must be 1-200");
                Geometry.sphereLatDiv = lat;
                Geometry.sphereLonDiv = lon;
                return new Command() {
                    public void execute() {
                        System.out.printf("Sphere divisions set to %d, %d%n", lat, lon);
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Set Sphere Divisions";
                    }
                };
            }

            public String getUsage() {
                return "sphere_div <lat> <lon>";
            }
        });

        CommandRegistry.register("units", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: units <mm|cm|m|in|ft>");
                String unit = args[1].toLowerCase();
                UnitSystem sys = UnitSystem.MMGS;
                if (unit.equals("mm"))
                    sys = UnitSystem.MMGS;
                else if (unit.equals("cm"))
                    sys = UnitSystem.CGS;
                else if (unit.equals("m"))
                    sys = UnitSystem.MKS;
                else if (unit.equals("in"))
                    sys = UnitSystem.IPS;
                else if (unit.equals("ft"))
                    sys = UnitSystem.IPS; // approximate mapping
                else
                    throw new IllegalArgumentException("Unsupported unit");

                final UnitSystem finalSys = sys;
                return new Command() {
                    public void execute() {
                        sketch.setUnitSystem(finalSys);
                        System.out.println("Units set to " + finalSys.name());
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Set Units";
                    }
                };
            }

            public String getUsage() {
                return "units <mm|cm|m|in|ft>";
            }
        });

        CommandRegistry.register("save", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: save <filename>");
                String filename = args[1];
                return new Command() {
                    public void execute() {
                        try {
                            Geometry.saveStl(filename);
                            System.out.println("Saved " + filename);
                        } catch (Exception e) {
                            System.out.println("Error saving: " + e.getMessage());
                        }
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Save STL";
                    }
                };
            }

            public String getUsage() {
                return "save <filename>";
            }
        });

        CommandRegistry.register("load", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: load <filename>");
                String filename = args[1];
                return new Command() {
                    public void execute() {
                        try {
                            if (filename.toLowerCase().endsWith(".stl")) {
                                Geometry.loadStl(filename);
                                System.out.println("Loaded STL: " + filename);
                            } else if (filename.toLowerCase().endsWith(".dxf")) {
                                sketch.loadDXF(filename);
                                System.out.println("Loaded DXF: " + filename);
                            } else {
                                System.out.println("Unknown format.");
                            }
                        } catch (Exception e) {
                            System.out.println("Error loading: " + e.getMessage());
                        }
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Load File";
                    }
                };
            }

            public String getUsage() {
                return "load <filename>";
            }
        });

        CommandRegistry.register("export_dxf", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: export_dxf <filename>");
                String filename = args[1];
                return new Command() {
                    public void execute() {
                        try {
                            sketch.exportSketchToDXF(filename);
                            System.out.println("Exported " + filename);
                        } catch (Exception e) {
                            System.out.println("Error exporting: " + e.getMessage());
                        }
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Export DXF";
                    }
                };
            }

            public String getUsage() {
                return "export_dxf <filename>";
            }
        });

        CommandRegistry.register("cg", new CliHandler() {
            public Command createCommand(String[] args) {
                return new Command() {
                    public void execute() {
                        float[] c = Geometry.calculateCentroid();
                        System.out.printf("CG: %.4f, %.4f, %.4f%n", c[0], c[1], c[2]);
                    }

                    public void undo() {
                    }

                    public String getDescription() {
                        return "Calculate CG";
                    }
                };
            }

            public String getUsage() {
                return "cg - Calculate Centroid";
            }
        });

        CommandRegistry.register("naca", new CliHandler() {
            public Command createCommand(String[] args) {
                if (args.length < 2)
                    throw new IllegalArgumentException("Usage: naca <digits> [chord]");
                String digits = args[1];
                float chord = (args.length > 2) ? Float.parseFloat(args[2]) : 1.0f;
                return new Command() {
                    public void execute() {
                        int res = sketch.generateNaca4(digits, chord, 50);
                        if (res == 0)
                            System.out.println("Generated NACA " + digits);
                        else
                            System.out.println("Failed to generate NACA.");
                    }

                    public void undo() {
                    } // TODO

                    public String getDescription() {
                        return "NACA Airfoil";
                    }
                };
            }

            public String getUsage() {
                return "naca <digits> [chord]";
            }
        });

        // Add more constraints as needed...
    }
}
