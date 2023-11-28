#include <stdio.h>
#include <stdlib.h>
#include "DataLoader.h"

struct test_scenario* load_test_data(char* filename) {
    FILE* file = fopen(filename, "r");
    if (file == NULL) {
        printf("Could not open file %s\n", filename);
        return NULL;
    }

    struct test_scenario* scenario = malloc(sizeof(struct test_scenario));
    if (scenario == NULL) {
        printf("Could not allocate memory for test scenario\n");
        fclose(file);
        return NULL;
    }

    // Read the number of pages, frames, and entries from the file
    fscanf(file, "%d", &(scenario->page_count));
    fscanf(file, "%d", &(scenario->frame_count));
    fscanf(file, "%d", &(scenario->refstr_len));

    // Ensure that refstr_len does not exceed the size of the array
    if (scenario->refstr_len > 512) {
        printf("Reference string length exceeds size of the array\n");
        free(scenario);
        fclose(file);
        return NULL;
    }

    // Read the entries into the array
    for (int i = 0; i < scenario->refstr_len; i++) {
        fscanf(file, "%d", &(scenario->refstr[i]));
    }

    fclose(file);
    return scenario;
}