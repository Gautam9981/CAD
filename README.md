# CAD
CAD software (with CLI tools and a GUI tool (eventually) as well)

Day 1 (5/31/25)

So far, today was the groundbreaking day for this project. I didn't get too far, however, I have a decent foundation. I intend to make a terminal application that can run a GUI (at the discretion of the user) and make .stl files that can be read by platforms such as Fusion or Solidworks. So far, it can generate cubes and sphere shapes, which shows up as meshes in Solidworks. I intend to make a gui for this as well, as lightweight as possible, with the assistance of others.

Day 2 (6/1/25)

Now I can sketch basic lines and circles and points! However, I can't necessarily make it look exactly clean in Solidworks, but it exports it as a .dxf file and it loads. I realized that the biggest issue wouldn't necessarily be the code, but, rather the documentation of the code, so I would need help documenting the code properly. This repo is public, so anyone can see it


Day 3 (6/2/25)

Today, I wasn't really able to get much coding done, however, I will provide the necessary instructions to compile the program in its current state; So far, I have only tested it for Linux systems, or Windows systems with WSL (Windows Subsystem for Linux). Regardless, I will provide instructions for each type of operating system (to the best of my ability) (Instructions are in Compilation.txt)

Day 4 (6/4/25)

Today, I started working on a very rudimentary undo/redo functionality for the geometries and the sketches. So far, it works perfectly for lines, however, I would prefer to not have to save it to the file everytime, it should be able to refer to the current file. As for the geometries, it functions even worse, keeping the geometries, despite typing undo and redo. I plan to fix it soon, and I should be able to undo and redo (with improved logic for other functions as well; I have made a separate subset of code called "Broken Code", so that I can fix it later on, but for now, DO NOT TRY TO COMPILE THIS VERSION; It isn't syntactically broken, the logic is broken, so it won't work as expected (For now, the main culprit is history.c and history.h)

Day 5 (6/13/25)

Upon a few days break, I have realized that if I continued to go with C, I think it would become too complicated to maintain the current state of functionality and simplicity in the code, so I have converted all of the C files into Java

Day 6 (6/14/25)

Made a basic load functionality, in the sense that for dxf, it says it is loaded, while for stl, it lists every vertice in the file. I haven't been able to edit them as of yet, so that will be my next set of steps. I also made a feedback form, as shown below:
[Feedback Form](https://forms.gle/EjwbZkag65qCfGmE8) (This form is fully optional, and will only be used to determine what set of features should be included)
