#include <stdio.h>
#include <stdlib.h>

struct test_scenario {
    int* reference_string;
    int reference_string_length;
    int num_pages;
    int num_frames;
};

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
    fscanf(file, "%d", &(scenario->num_pages));
    fscanf(file, "%d", &(scenario->num_frames));
    fscanf(file, "%d", &(scenario->reference_string_length));

    scenario->reference_string = malloc(sizeof(int) * scenario->reference_string_length);
    if (scenario->reference_string == NULL) {
        printf("Could not allocate memory for reference string\n");
        free(scenario);
        fclose(file);
        return NULL;
    }

    for (int i = 0; i < scenario->reference_string_length; i++) {
        fscanf(file, "%d", &(scenario->reference_string[i]));
    }

    fclose(file);
    return scenario;
}