#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "cli.h"

typedef int (*command_func)(int argc, char**argv);

typedef struct {
	const char* name;
	const char* description;
	command_func func;
} Command;


int run_cli(int argc, char** argv) {
	printf("Running in CLI mode with %d arguments.\n", argc - 1);
	for (int i = 1; i < argc; i++) {
		printf("Arg $d: %s\n", i, argv[i]);
	}
}
