CAD software (with CLI tools and a GUI tool (eventually) as well) 

Day 1 (5/31/25) 

So far, today was the groundbreaking day for this project. I didn’t get too far, however, I have a decent foundation. I intend to make a terminal application that can run a GUI (at the discretion of the user) and make .stl files that can be read by platforms such as Fusion or Solidworks. So far, it can generate cubes and sphere shapes, which shows up as meshes in Solidworks. I intend to make a gui for this as well, as lightweight as possible, with the assistance of others. 

Day 2 (6/1/25) 

Now I can sketch basic lines and circles and points! However, I can’t necessarily make it look exactly clean in Solidworks, but it exports it as a .dxf file and it loads. I realized that the biggest issue wouldn’t necessarily be the code, but, rather the documentation of the code, so I would need help documenting the code properly. This repo is public, so anyone can see it 

Day 3 (6/2/25) 

Today, I wasn’t really able to get much coding done, however, I will provide the necessary instructions to compile the program in its current state; So far, I have only tested it for Linux systems, or Windows systems with WSL (Windows Subsystem for Linux). Regardless, I will provide instructions for each type of operating system (to the best of my ability) (Instructions are in Compilation.txt) 

Day 4 (6/4/25) 

Today, I started working on a very rudimentary undo/redo functionality for the geometries and the sketches. So far, it works perfectly for lines, however, I would prefer to not have to save it to the file everytime, it should be able to refer to the current file. As for the geometries, it functions even worse, keeping the geometries, despite typing undo and redo. I plan to fix it soon, and I should be able to undo and redo (with improved logic for other functions as well; I have made a separate subset of code called “Broken Code”, so that I can fix it later on, but for now, DO NOT TRY TO COMPILE THIS VERSION; It isn’t syntactically broken, the logic is broken, so it won’t work as expected (For now, the main culprit is history.c and history.h) 

Day 5 (6/13/25) 

Upon a few days break, I have realized that if I continued to go with C, I think it would become too complicated to maintain the current state of functionality and simplicity in the code, so I have converted all of the C files into Java 

Day 6 (6/14/25) 

Made a basic load functionality, in the sense that for dxf, it says it is loaded, while for stl, it lists every vertice in the file. I haven’t been able to edit them as of yet, so that will be my next set of steps. I also made a feedback form, as shown below: [Feedback Form](https://forms.gle/6JeLGzmrWwT5CRcj8) (This form is fully optional, and will only be used to determine what set of features should be included) 

Day 7 (6/16/25) 

I’ve only edited the compilation documentation, by removing slackware (May add it back in the future, once I figure out the packaging instructions properly), and I edited the Nix compilation set of instructions, as there are at least two options of getting the same thing (nix-env and editing /etc/nixos/configuration.nix) 

Day 8 (6/17/25) 

I’ve only revised the documenatation about how to compile the code, as I’ve noticed some strange behavior with Solus OS (As shown below step 3 for Solus compilation instructions), that it may lead to a broken symlink between the binary of java and javac, which may result in a command not found error, as it wouldn’t be able to find the binary files to connect to those commands. I have posted the fix that you can do to fix that issue (Fix is completely optional!). I will revise the compilation instructions further, as I test the software on other operating systems; I may make packages out of this project, so that the package manager of that system can handle installation, rather than having you compile them (not sure right now though)

Day 9 (6/18/25)

I've somewhat rewritten the code a little bit, doing some light refactoring. That's about all I've done today

Day 10 (6/20/25)

I've updated the compilation.txt file to reflect the option for MacOS, if they decide to use Visual Studio Code, rather than doing XCode + Homebrew or Homebrew

Day 11 (6/23/25)

I've heard from my collaborator that he has built a gui form of this application, so I immediately updated the files such that there would a Main class, where it would enable the user to select between the gui and the cli interfaces for their use of the application. I've also updated Compilation.txt for the set of new compilation instructions for multiple classes and separate folders. I have the gui stuff, changed the structure of the repo such that gui and cli are their own thing, but the set of classes they share (such as Geometry and Sketch), remain separate, to support modularity down the line

Day 12 (6/24/25)

I am able to create different types of shapes using the CLI. I will expect my collaborator to incorporate such logic into the GUI, it can make sketches based off of points I made. 

Day 13 (6/25/25)

I just updated the size of the GUI to match 1080p monitors and screens, so it should look better.

Day 14 (6/26/25)

I have update the CLI to show units and scale the sketches appropriately (says units when loaded the .dxf file); Will set it up for GUI for later, with necessary changes

Day 15 (7/1/25)

Now that sketch will show up in the GUI, once it is loaded. It is a very basic pane, I will include more functionality later on. I have also created a .exe file of the application, using launch4j (.jar, then .exe). I also created a .tar.gz file for MacOS and Linux Users, so that they can run the files using a run script (in .sh). Unless you are a developer, please ignore the compilation.txt instructions 

Day 16 (7/7/25)

We finished refactoring and adding comments, as necessary

Day 17 (7/8/25)

I was able to set up the application such that it can also load .stl files as well, and allowing the manipulation of the view using the mouse and the keyboard

Day 18 (7/15/25)

I got JOGL support included in the program (in terms of the .stl files and rendering), and I will provide instructions on how to run it in Windows; For Linux/Mac (The lib files should be there already for Linux/MacOS), there is a script that will run it for you, just run that. 

Instructions for Running the program:

For Windows: 
1. Get the JDK (Download a JDK from Oracle or Adoptium) and install it
2. Download the .exe file
3. Run the .exe file (If you see this message: "Windows protected your PC — Microsoft Defender SmartScreen prevented an unrecognized app from starting…", Click "More info". Then click "Run anyway".)

JOGL Support:
==============================
SketchApp - Windows Instructions
==============================

Requirements:
------------------------------
- Java 8+ must be installed.
  To check: open Command Prompt and type:
    java -version
  If not installed, download from:
    https://adoptium.net or https://www.oracle.com/java/

lib/ folder must include:
------------------------------
- jogl-all.jar
- gluegen-rt.jar
- JOGL native .dll files:
    e.g. jogl_desktop.dll, gluegen-rt.dll, etc.

You can get them from:
https://jogamp.org/deployment/jogamp-current/archive/jogamp-all-platforms.7z  
(Use 7-Zip to extract them)

Folder Structure:
SketchApp/
├── SketchApp.exe
└── lib/
    ├── jogl-all.jar
    ├── gluegen-rt.jar
    ├── jogl_desktop.dll
    ├── gluegen-rt.dll
    └── (other native DLLs)

To Launch the App:
------------------------------
1. Ensure the `lib/` folder contains the required files.
2. Double-click `SketchApp.exe`.

That's it!

Troubleshooting:
------------------------------
- If the app doesn't start:
  → Ensure Java is installed.
  → Check that all required JOGL files are in `lib/`.



For MacOS/Linux/FreeBSD:
1. Download JDK (Oracle or Adoptium), and install it
2. Download the .tar.gz file
3. give the run.sh execution permissions (may do chmod +x run.sh for full execution, or chmod 744 run.sh; For safest use, calculate the number, then do chmod (your number) run.sh)
4. Run it (./run.sh) (Note: for FreeBSD, change the shebang line (#!/bin/bash) to (#!/bin/sh))














































Contributors: Gautam9981 (Me) and AdityaJha25 (Aditya)
