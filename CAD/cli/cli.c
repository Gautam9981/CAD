#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <ctype.h>  // for tolower()
#include "commands.h"

#define MAX_INPUT_LEN 256
#define MAX_ARGS 10

int run_cli() {
    char input[MAX_INPUT_LEN];

    while (1) {
        printf("cad> ");
        if (!fgets(input, sizeof(input), stdin)) {
            printf("\nEOF detected, exiting.\n");
            break;
        }

        size_t len = strlen(input);
        if (len > 0 && input[len - 1] == '\n') {
            input[len - 1] = '\0';
        }

        if (len == 1) continue;

        char* args[MAX_ARGS];
        int arg_count = 0;

        char* token = strtok(input, " ");
        while (token && arg_count < MAX_ARGS) {
            args[arg_count++] = token;
            token = strtok(NULL, " ");
        }

        if (arg_count == 0) continue;

        // Convert command name to lowercase
        for (char* p = args[0]; *p; ++p) {
            *p = tolower(*p);
        }

        int found = 0;
        for (int i = 0; i < num_commands; i++) {
            if (strcmp(args[0], commands[i].name) == 0) {
                commands[i].func(arg_count, args);
                found = 1;
                break;
            }
        }

        if (!found) {
            printf("Unknown command: %s. Type 'help' for a list.\n", args[0]);
        }
    }

    return 0;
}

int main() {
    printf("Welcome to CAD CLI v0.0 (BETA)\n");
    return run_cli();
}

/* This is the main file that sets up the cli environment */
