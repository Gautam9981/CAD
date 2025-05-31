#include <stdio.h>
#include <string.h>
#include "cli/cli.h"
#include "gui/gui.h"

int main(int argc, char** argv){
    if (argc > 1) {
	    return run_cli(argc, argv);
    }
    else {
	    if(!startgui("CAD app", 1920, 1080)) {
		    fprintf(stderr, "Failed to start GUI component.\n");
		    return 1;
            }
    }
    return 0;
}

/* This is the main code that will launch in CLI-mode if any arguments are provided, otherwise, it will start in a GUI mode, or it may not start at all; Very dependent on the operating system in use. */
