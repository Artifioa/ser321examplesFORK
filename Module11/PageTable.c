#include <stdio.h>
#include <stdlib.h>
#include "PageTable.h"
#include "DataLoader.h"

struct page_table_entry {
    unsigned int data; // rightmost bit is valid/invalid bit, second bit from right is dirty bit
    int frame_number;
    int access_count; // for LRU and MFU
};

struct page_table {
    struct page_table_entry* entries;
    int page_count;
    int frame_count;
    enum replacement_algorithm algorithm;
    int verbose;
    int page_faults;
};

struct page_table* page_table_create(int page_count, int frame_count, enum replacement_algorithm algorithm, int verbose) {
    // Allocate memory for the page table
    struct page_table* pt = malloc(sizeof(struct page_table));
    pt->entries = malloc(sizeof(struct page_table_entry) * page_count);
    pt->page_count = page_count;
    pt->frame_count = frame_count;
    pt->algorithm = algorithm;
    pt->verbose = verbose;
    pt->page_faults = 0;
    return pt;
}

void page_table_destroy(struct page_table** pt) {
    free((*pt)->entries);
    free(*pt);
    *pt = NULL;
}

void page_table_access_page(struct page_table *pt, int page) {
    if (pt->entries[page].data & 1) {
        pt->entries[page].access_count++;
    } else {
        pt->page_faults++;

        int free_frame = -1;
        for (int i = 0; i < pt->page_count; i++) {
            if (!(pt->entries[i].data & 1)) {
                free_frame = i;
                break;
            }
        }

        if (free_frame != -1) {
            pt->entries[free_frame].frame_number = page;
            pt->entries[free_frame].data |= 1;
            pt->entries[free_frame].access_count = 1;
        } else {
            int replace_frame = 0;
            switch (pt->algorithm) {
                case FIFO:
                    replace_frame = 0;
                    break;
                case LRU:
                    for (int i = 1; i < pt->page_count; i++) {
                        if (pt->entries[i].access_count < pt->entries[replace_frame].access_count) {
                            replace_frame = i;
                        }
                    }
                    break;
                case MFU:
                    for (int i = 1; i < pt->page_count; i++) {
                        if (pt->entries[i].access_count > pt->entries[replace_frame].access_count) {
                            replace_frame = i;
                        }
                    }
                    break;
            }

            pt->entries[replace_frame].data &= ~1;
            pt->entries[replace_frame].frame_number = page;
            pt->entries[replace_frame].data |= 1;
            pt->entries[replace_frame].access_count = 1;
        }
    }
}

void page_table_display(struct page_table* pt) {
    printf("==== Page Table ====\n");
    printf("Mode : %s\n", pt->algorithm == FIFO ? "FIFO" : pt->algorithm == LRU ? "LRU" : "MFU");
    printf("Page Faults : %d\n", pt->page_faults);
    page_table_display_contents(pt);
}

void page_table_display_contents(struct page_table *pt) {
    printf("page frame | dirty valid\n");
    for (int i = 0; i < pt->page_count; i++) {
        printf("   %d     %d |     %d     %d\n", i, pt->entries[i].frame_number, (pt->entries[i].data >> 1) & 1, pt->entries[i].data & 1);
    }
}

int main(int argc, char *argv[]) {
    if (argc != 2) {
        printf("Usage: %s <filename>\n", argv[0]);
        return 1;
    }

    struct test_scenario* scenario = load_test_data(argv[1]);
    if (scenario == NULL) {
        printf("Could not load test data\n");
        return 1;
    }

    enum replacement_algorithm algorithms[] = {FIFO, LRU, MFU};
    const char* algorithm_names[] = {"FIFO", "LRU", "MFU"};

    for (int a = 0; a < 3; a++) {
        struct page_table* pt = page_table_create(scenario->page_count, scenario->frame_count, algorithms[a], 1);
        if (pt == NULL) {
            printf("Could not create page table\n");
            free(scenario);
            return 1;
        }

        for (int i = 0; i < scenario->refstr_len; i++) {
            page_table_access_page(pt, scenario->refstr[i]);
        }
        
        page_table_display(pt);
        page_table_destroy(&pt);
    }

    free(scenario);
    return 0;
}