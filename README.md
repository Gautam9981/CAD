# CAD Software Development Journal

A lightweight CAD application with both CLI and GUI interfaces for creating and editing .stl and .dxf files compatible with professional CAD software like Fusion 360 and SolidWorks.

---

## Development Log

### Day 1 (5/31/25) - Project Foundation
Groundbreaking day for the project. Established basic foundation:
- Created terminal application framework
- Added optional GUI support
- Implemented .stl file generation for Fusion/SolidWorks compatibility
- Basic shape generation: cubes and spheres
- Successfully displays as meshes in SolidWorks

**Goal:** Make a lightweight GUI with community assistance

---

### Day 2 (6/1/25) - Sketching Capabilities
**New Features:**
- Basic sketching: lines, circles, and points
- .dxf file export (loads in SolidWorks, though not perfectly clean)

**Key Insight:** Documentation is the biggest challenge, not the code itself. Made repository public for collaboration.

---

### Day 3 (6/2/25) - Compilation Documentation
**Focus:** Created comprehensive compilation instructions
- Tested on Linux and Windows WSL
- Added OS-specific instructions to `Compilation.txt`
- Minimal coding progress today

---

### Day 4 (6/4/25) - Undo/Redo Functionality
**New Feature:** Rudimentary undo/redo system
- ✅ Works perfectly for lines
- ⚠️ Issue: Currently saves to file each time (should reference current file in memory)
- ❌ Geometries undo/redo not functioning correctly

**Created:** "Broken Code" subset
- **WARNING:** DO NOT COMPILE THIS VERSION
- Syntax correct, logic broken
- Main culprits: `history.c` and `history.h`

---

### Day 5 (6/13/25) - Major Architecture Change
**Critical Decision:** Migrated from C to Java
- **Reason:** C was becoming too complicated to maintain functionality and simplicity
- Converted all C files to Java for better maintainability

---

### Day 6 (6/14/25) - File Loading
**New Feature:** Basic load functionality
- .dxf files: Confirms loaded status
- .stl files: Lists all vertices in file

**Next Steps:** Enable editing of loaded files

**Created:** [Feedback Form](https://forms.gle/6JeLGzmrWwT5CRcj8) (optional)
- Used to determine priority features

---

### Day 7 (6/16/25) - Documentation Refinement
**Updates:**
- Removed Slackware instructions (may re-add after proper packaging research)
- Updated Nix compilation with two installation options:
  - `nix-env` method
  - Editing `/etc/nixos/configuration.nix`

---

### Day 8 (6/17/25) - OS-Specific Bug Fixes
**Documentation Updates:**
- Revised compilation instructions for Solus OS
- Identified broken symlink issue between Java and javac binaries
- Added optional fix for "command not found" errors

**Future Plans:** Consider creating OS-specific packages for automatic installation

---

### Day 9 (6/18/25) - Code Refactoring
Light refactoring day - cleaned up code structure and improved readability.

---

### Day 10 (6/20/25) - macOS Support
Updated `Compilation.txt` with macOS options:
- XCode + Homebrew method
- Homebrew standalone
- Visual Studio Code alternative

---

### Day 11 (6/23/25) - GUI Integration
**Major Update:** Collaborator built GUI interface
- Created `Main` class for interface selection (CLI or GUI)
- Restructured repository:
  - `cli/` folder for terminal interface
  - `gui/` folder for graphical interface
  - Shared classes (`Geometry`, `Sketch`) remain separate for modularity
- Updated compilation instructions for multi-class/multi-folder structure

---

### Day 12 (6/24/25) - Shape Creation
**Progress:**
- CLI can create different types of shapes
- Sketches can be generated from user-defined points
- Collaborator will incorporate logic into GUI

---

### Day 13 (6/25/25) - UI Improvements
Updated GUI dimensions to match 1080p monitors and screens for better display quality.

---

### Day 14 (6/26/25) - Units and Scaling
**CLI Updates:**
- Shows units when loading .dxf files
- Scales sketches appropriately
- GUI implementation planned

---

### Day 15 (7/1/25) - Binary Distribution
**Major Milestone:** Created distributable binaries
- Sketches now display in GUI after loading
- Created `.exe` file using launch4j (.jar → .exe conversion)
- Created `.tar.gz` file for macOS/Linux with run script (`.sh`)

**Note:** Unless you're a developer, ignore `Compilation.txt` instructions

---

### Day 16 (7/7/25) - Code Cleanup
Completed refactoring and added comprehensive comments throughout codebase.

---

### Day 17 (7/8/25) - 3D Viewer Enhancement
**New Features:**
- .stl file loading support
- Mouse and keyboard controls for view manipulation

---

### Day 18 (7/15/25) - OpenGL Integration
**Major Feature:** JOGL (Java OpenGL) support
- Improved .stl rendering
- Added Windows-specific run instructions
- Linux/Mac: Run script handles JOGL libraries automatically

---

### Days 19-20 (7/16/25 - 7/17/25) - Binary Testing
**Testing Phase:** OpenGL/JOGL support verification
- ✅ Linux binaries working
- ✅ Windows binaries working
- ❓ macOS binaries untested

**Windows Distribution:** Included both .jar and .exe (`.exe` is a wrapper for `.jar`)

---

### Day 21 (7/18/25) - Architecture Overhaul
**Major Change:** Restructured `Geometry.java`
- Added support for 3D shapes beyond cubes and spheres
- Need to update CLI and GUI implementations

---

### Day 22 (7/20/25) - Extrusion Feature
**New Feature:** Extrusion functionality
- Currently works but feels crude (basic implementation)
- Updating all binaries to include extrusion

---

### Day 23 (7/22/25) - Extrusion Refinement
Got extrusion working properly. Considering stylistic improvements.

---

### Day 24 (9/16/25) - Kite Shapes
Implemented kite creation functionality (sketching + extruding). Feature in interesting/experimental state.

---

### Day 25 (12/11/25) - UI Modernization
**Current Progress:**
- UI now resembles production CAD software
- Features behind UI not yet implemented
- ⚠️ Click and drag support for sketchpad broken (investigating)
- Planning color scheme improvements for better aesthetics

---

### Day 26 (12/14/25) - Undo/Redo Extensions
**Major Update:** Comprehensive Undo/Redo support for constraints and dimensions
- Implemented `AddConstraintCommand` and `AddDimensionCommand`
- Integrated with `GuiFX` and `SketchInteractionManager`
- Updated CLI to support new undoable actions
- Added necessary removal methods to `Sketch` class

---

### Day 27 (12/15/25) - Build Automation & Distribution
**Major Update:** Cross-platform build automation and package distribution
- Implemented GitHub Actions workflow for automated builds
  - Windows `.exe` installers
  - macOS `.dmg` packages
  - Linux `.deb` packages (Debian/Ubuntu)
  - Linux `.rpm` packages (Fedora/RHEL/CentOS)
- Created comprehensive build scripts:
  - `build-all.sh` - Auto-detects platform and builds appropriate installer
  - `build-linux.sh` - Builds both .deb and .rpm packages
  - `build-windows.sh` - Windows installer generation
  - `build-macos.sh` - macOS DMG creation
- Added `.gitignore` for proper build artifact management
- Automatic GitHub Releases on version tags


---

### Day 28 (12/16/25) - Rendering Overhaul & macOS Fixes
**Critical Startup Fix:**
- **Issue:** Resolved startup hang on macOS caused by blocking `ChoiceDialog` in `start()`.
- **Solution:** implemented "Splash Stage" for Unit Selection to prevent event loop deadlocks.
- **Verification:** Fix implemented but **pending final tests on macOS hardware**.

**Rendering Engine Upgrade:**
- ✅ **MSAA (Multisample Anti-Aliasing):** Enabled 4x MSAA for smooth, high-quality edges in the 3D viewer.
- ✅ **Normal Calculation:** Fixed lighting issues on flat surfaces (gears, flanges) by respecting face flatness (no unwanted smoothing filters).
- ✅ **Tessellation:** Fixed rendering of complex polygons (like gear teeth) to prevent "garbage" geometry.

**Macro System & Testing:**
- Validated and fixed complex macros (`gear-design.macro`, `flange.macro`).
- Created automated test scripts (`test-gear.sh`, `test-flange.sh`) to verify geometry generation.

**API Documentation (v2):**
- **Automated Generation:** Pipeline now fully parses Java source code to generate JSON data.
- **Smart Inference:** Replaced weak/missing Javadoc with intelligent, context-aware descriptions (e.g., `handleLoft` → "Executes the logic for the 'loft' command").
- **Cleanups:** Fixed regex bugs to remove "garbage" classes and standardized description formatting.
- **Live Site:** [https://gautam9981.github.io/CAD/](https://gautam9981.github.io/CAD/)

---

## Installation & Running

### Windows

1. **Install JDK 21+** from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
2. **Download** the `.exe` installer
3. **Run** the `.exe` file
   - If Windows SmartScreen warning appears:
     - Click "More info"
     - Click "Run anyway"
4. **Follow** the installation wizard
5. **Launch** from Start Menu or Desktop shortcut


---

### Linux

**Requirements:** JDK 21+ from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)

**For Debian/Ubuntu (.deb):**
```bash
sudo dpkg -i sketchapp_4.0.0-1_amd64.deb
sudo apt-get install -f  # Install any missing dependencies
```

**For Fedora/RHEL/CentOS (.rpm):**
```bash
sudo dnf install sketchapp-4.0.0-1.x86_64.rpm
# Or: sudo rpm -i sketchapp-4.0.0-1.x86_64.rpm
```

**Running the Application:**
```bash
/opt/sketchapp/bin/sketchapp
```

The installer creates:
- Desktop launcher in application menu (under Graphics)
- Installation in `/opt/sketchapp`

---

### macOS

1. **Install JDK 21+** from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Adoptium](https://adoptium.net/)
2. **Download** the `.dmg` installer
3. **Double-click** the `.dmg` file
4. **Drag** SketchApp.app to the Applications folder
5. **Launch** from Launchpad or Applications folder


---

## Troubleshooting

### Application Won't Start
- Ensure Java is installed: `java -version`
- Must have Java 21 or higher
- Check execution permissions on run script (macOS/Linux/FreeBSD)

### JOGL/OpenGL Issues
- Verify JOGL libraries are in `lib/` folder
- Windows: Use `.bat` file instead of `.exe`
- Linux/macOS: Run script handles library paths automatically

---

## Feedback

Help shape this project! [Feedback Form](https://forms.gle/6JeLGzmrWwT5CRcj8) (optional)

---

## Contributors

- **Gautam9981** (Me) - Project Lead
- **AdityaJha25** (Aditya) - GUI Development & Collaboration

---

## Current Features

✅ **Implemented:**
- Dual interface (CLI and GUI)
- .stl and .dxf file export
- File loading (.stl and .dxf)
- 3D shapes (cubes, spheres, custom geometries, kites)
- 2D sketching (lines, circles, points)
- **Sketch Constraints:**
  - Horizontal constraint
  - Vertical constraint
  - Fixed (lock) constraint
  - Coincident constraint
- **Dimensions:**
  - Linear dimensions
  - Radial dimensions
- Extrusion functionality
- OpenGL rendering (JOGL)
- Mouse/keyboard view controls
- Units and scaling support
- Advanced Undo/Redo (Constraints & Dimensions)
- **Cross-platform installers:**
  - Windows `.exe`
  - macOS `.dmg`
  - Linux `.deb` and `.rpm`
  - Automated builds via GitHub Actions
- **Advanced Rendering:**
  - 4x MSAA (Anti-Aliasing)
  - Accurate lighting/shading for mechanical parts
- **Documentation:**
  - [Auto-generated API Reference](https://gautam9981.github.io/CAD/)

⚠️ **In Progress:**
- Click and drag in sketchpad
- Feature implementation behind modern UI
- File editing capabilities

---

## License

This repository is public and open for collaboration.
