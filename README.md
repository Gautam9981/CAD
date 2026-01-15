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
- **macOS .dmg note:** The provided `.dmg` is not signed, so macOS Gatekeeper will block it. To run the app, open **System Settings → Privacy & Security**, scroll down to the **Security** section, locate the message about the blocked app, click **Open Anyway**, and enter your password to allow it.
- **Note:** Unless you're a developer, ignore `Compilation.txt` instructions

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
  - Linux `.tar.gz` archives (All other Linux Distros)
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
- Validated and fixed complex macros (`gear-design.macro`).

**API Documentation (v2):**
- **Automated Generation:** Pipeline now fully parses Java source code to generate JSON data.
- **Smart Inference:** Replaced weak/missing Javadoc with intelligent, context-aware descriptions (e.g., `handleLoft` → "Executes the logic for the 'loft' command").
- **Cleanups:** Fixed regex bugs to remove "garbage" classes and standardized description formatting.

---

### Day 29 (12/18/25) - Dimension Rendering Improvements
**Visual Fixes:**
- **Linear Dimensions:** Implemented "smart arrows" for small dimensions. Arrows now point inwards from the outside to prevent overlap.
- **Text Placement:** Added logic to laterally shift dimension text for small values to avoid overlapping with extension lines.
- **Radial Dimensions:** Investigated duplicate text issues (likely user workflow related).

**Features:**
- **Materials Database:** Implemented initial material system. Selected materials are now reflected in the application (currently rudimentary, development continuing).

---

### Day 30 (12/21/25) - 3D Feature Fixes & CSG Support
**Critical Fixes:**
- **Sphere & Cube Rendering:** Fixed non-functional "Create Sphere" and "Create Cube" features:
  - Updated to use correct mesh data source (`getExtrudedTriangles()` instead of `getLoadedStlTriangles()`)
  - Added automatic camera reset (`resetView()`) to ensure newly created shapes are visible
  - Increased dialog window sizes so "Create" buttons are fully visible and clickable
- **Mass Properties:** Fixed calculation for 3D models (Cubes, Spheres, CSG results):
  - Updated `Geometry.getActiveTriangles()` to handle CSG_RESULT shapes
  - Added `MassProperties.calculateFrom3DMesh()` for mesh-based property calculation
  - Implemented primitive shape type tracking to display "CUBE" or "SPHERE" instead of "CSG_RESULT" in dialogs

**New Features:**
- **CSG (Constructive Solid Geometry) Support:** Full implementation of boolean operations:
  - Union, Difference, and Intersection operations on 3D solids
  - Integration with Cube, Sphere, Extrude, Revolve, Loft, and Sweep features
  - Supports combining multiple operations to create complex geometries
- **Macro System Enhancements:** Fixed `REVOLVE` command to use correct axis parameters

---

### Day 31 (1/14/26) - Aerodynamics & Flow Visualization
**Major Features:**
- **Aerodynamics Tab:** New ribbon tab for aerodynamic analysis
  - NACA 4-digit airfoil generator with auto-extrusion
  - Centralized `NacaAirfoilGenerator` class for profile generation
- **CFD Analysis Dashboard:**
  - Lift/Drag coefficient calculations (thin airfoil theory)
  - Reynolds number computation
  - `FluidDynamics` analysis engine
- **3D Flow Visualization (Jzy3d):**
  - Integrated Jzy3d library for professional streamline rendering
  - Potential flow model for airfoil streamlines
  - Interactive 3D rotation/zoom in separate window
  - Cross-platform support (Windows x64, macOS, Linux arm64/x64)

**Code Improvements:**
- Fixed `Main.java` to launch GUI by default
- Cleaned up unused imports and dead code
- Added Jzy3d Maven repository for dependency resolution

**Dependencies Added:**
- `org.jzy3d:jzy3d-native-jogl-awt:2.1.0`

---

## Installation & Running

### Windows

1. **Download** the `.exe` installer
2. **Run** the `.exe` file
   - **Note:** The installer is not code-signed, so Windows SmartScreen may show a warning. This is expected and not a cause for concern.
   - If the warning appears:
     - Click **"More info"**
     - Click **"Run anyway"**
3. **Follow** the installation wizard
4. **Launch** from Start Menu or Desktop shortcut


---

### Linux

**Note:** The packages are not signed, so your package manager may warn you. This is expected and not a cause for concern.

**For Debian/Ubuntu (.deb):**
```bash
sudo dpkg -i sketchapp_4.5.0-1_amd64.deb
sudo apt-get install -f  # Install any missing dependencies
```

**For Fedora/RHEL/CentOS (.rpm):**
```bash
sudo dnf install sketchapp-4.5.0-1.x86_64.rpm
# Or: sudo rpm -i sketchapp-4.5.0-1.x86_64.rpm
```

**For Arch Linux (.pkg.tar.zst):**
1. Download the packages from the github
2. Install, using pacman:
   ```bash
   sudo pacman -U SketchApp-4.5.0-1-x86_64.pkg.tar.zst
   sudo pacman -U SketchApp-debug-4.5.0-1-x86_64.pkg.tar.zst
   ```

**For Void Linux (.xbps):**
1. Download the `.xbps` package (e.g., `SketchApp-4.5.0_1.x86_64.xbps`) from releases.
2. Index the package in the current directory:
   ```bash
   xbps-rindex -a *.xbps
   ```
3. Install the package:
   ```bash
   sudo xbps-install --repository=$PWD SketchApp
   ```



---

### macOS

1. **Download** the `.dmg` installer
2. **Double-click** the `.dmg` file
3. **Drag** SketchApp.app to the Applications folder
4. **Launch** from Launchpad or Applications folder
   - **Note:** The app is not code-signed, so macOS Gatekeeper will block it on first launch. This is expected and not a cause for concern.
   - To allow the app:
     - Open **System Settings → Privacy & Security**
     - Scroll down to the **Security** section
     - Locate the message about the blocked app
     - Click **"Open Anyway"**
     - Enter your password when prompted



> [!NOTE]
> The macOS x64 binary has been tested and confirmed working on macOS 14.8.3 and newer. The ARM variant is untested. Both builds are generated on macOS 15 (Sequoia).

## Verifying Downloads

To ensure the integrity of your download, you can verify the SHA256 checksum. You can find the `SHA256SUMS` file in the releases or the `Binaries/` directory.

### Verifying GPG Signatures

To ensure the release files have not been tampered with, you can verify the GPG signature.

1.  **Import the Public Key**
    Download `public.gpg` from the release and import it:
    ```bash
    gpg --import public.gpg
    ```
    **Key ID:** `8EB08A8C4A7723F4D74013DA7224C931D9B24AD1`
    **Fingerprint:** `97618304F1FB877217E8B8516261D7A47482CB80`

2.  **Verify the Signature**
    Download `SHA256SUMS` and `SHA256SUMS.asc`. Run:
    ```bash
    gpg --verify SHA256SUMS.asc SHA256SUMS
    ```
    Confirm that the output reports a **Good signature**.

### Verifying Checksums
Run the following command in your terminal where you downloaded the file and `SHA256SUMS`:

```bash
# Verify the downloaded file
shasum -a 256 -c SHA256SUMS --ignore-missing
```
*Note: If `shasum` is not available, you can use `sha256sum -c SHA256SUMS --ignore-missing`.*

### Windows
Open PowerShell and run the following command to generate the hash for your downloaded file:

```powershell
Get-FileHash .\SketchApp-Windows-x64.zip -Algorithm SHA256
```
Compare the output hash with the corresponding entry in the `SHA256SUMS` file.


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
  - Coincident constraint (General: Point-Point, Point-Line, Point-Circle, Line-Line, Circle-Circle)
- **Dimensions:**
  - Linear dimensions
  - Radial dimensions
- **Advanced Sketching:**
  - Circumscribed/Inscribed Polygons
- Extrusion functionality
- **CSG Operations (Constructive Solid Geometry):**
  - Union (combine solids)
  - Difference (subtract solids)
  - Intersection (keep overlapping volume)
  - Support for Cube, Sphere, Extrude, Revolve, Loft, and Sweep
- **Mass Properties Calculation:**
  - Volume and surface area calculation
  - Mass calculation with material density
  - Support for sketch-based and 3D mesh geometries
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


⚠️ **In Progress / To Be Fixed:**
- **CRITICAL:** Dimensioning rendering MUST be fixed (ensure zero overlap for all scale factors).
- Feature implementation behind modern UI

---

## API Documentation

Explore the full API reference: [https://gautam9981.github.io/CAD/](https://gautam9981.github.io/CAD/)

---

**Contributing**

I welcome contributions! To contribute:

1. Fork the repository

2. Create a new branch for your feature or bugfix

3. Submit a pull request describing your changes. Please follow existing code style and write clear commit messages. Consider documenting new features and updating tests when applicable, by using the docs/ files

Optional: Fill out our Feedback Form to suggest features or report issues.

Help shape this project! [Feedback Form](https://forms.gle/pbKVRUrLYv9ob9nG6) (optional)

## License

SketchApp is licensed under the **Apache License 2.0**.  
You can find the full license in the [`LICENSE`](LICENSE) file included with this project.

## Third-Party Dependencies

This project uses several third-party libraries. All are included in the release and are listed in the [`NOTICE.txt`](NOTICE.txt) file.

### Key Libraries

| Library | Version | License |
|---------|--------|---------|
| antlr:antlr | 2.7.7 | BSD License |
| com.formdev:flatlaf | 3.6 | Apache 2.0 |
| com.formdev:flatlaf-intellij-themes | 3.6 | Apache 2.0 |
| com.google.code.gson:gson | 2.10.1 | Apache 2.0 |
| eu.mihosoft.vrl.jcsg:jcsg | 0.5.7 | BSD 2-Clause |
| eu.mihosoft.vvecmath:vvecmath | 0.3.8 | BSD 2-Clause |
| junit:junit | 4.13.1 | Eclipse Public License 1.0 |
| org.hamcrest:hamcrest-core | 1.3 | New BSD License |
| org.jogamp.gluegen:gluegen-rt | 2.6.0 | BSD-2, BSD-3, BSD-4 |
| org.jogamp.jogl:jogl-all | 2.6.0 | Apache 1.1, Apache 2.0, BSD-2, BSD-3, SGI Free Software License B, Ubuntu Font Licence 1.0 |
| org.jzy3d:jzy3d-native-jogl-awt | 2.1.0 | BSD 3-Clause |
| org.openjfx:javafx | 23.0.1 | GPLv2+CE |
| org.slf4j:slf4j-api / slf4j-simple | 1.6.1 | MIT License |
| org.jzy3d:jzy3d-native-jogl-awt | 2.1.0 | BSD 3-Clause |


All third-party libraries are distributed under their respective licenses, and you can see full details in [`NOTICE.txt`](NOTICE.txt).

---

**All contributions to this project are licensed under the Apache License 2.0.**
