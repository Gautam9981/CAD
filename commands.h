#ifndef COMMANDS_H
#define COMMANDS_H

int help(int argc, char** argv);
int create_geom(int argc, char** argv);
int save_file(int argc, char** argv);
int cmd_exit(int argc, char** argv);
int version(int argc, char** argv);

typedef struct {
	const char* name;
	int (*func)(int argc, char**argv);
	const char* description;
} Command;

extern Command commands[];
extern int num_commands;

#endif

/* Setup of the commands to be used in this software */
