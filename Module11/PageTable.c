#include <stdio.h>
#include <stdlib.h>
#include "PageTable.h"

enum replacement_algorithm {
    FIFO,
    LRU,
    MFU
};

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
    // Check if the page is valid
    if (pt->entries[page].data & 1) {
        // The page is valid, so just increase the access count
        pt->entries[page].access_count++;
    } else {
        // The page is not valid, so we have a page fault
        pt->page_faults++;

        // Find the first free frame
        int free_frame = -1;
        for (int i = 0; i < pt->frame_count; i++) {
            if (!(pt->entries[i].data & 1)) {
                free_frame = i;
                break;
            }
        }

        if (free_frame != -1) {
            // Found a free frame
            pt->entries[free_frame].frame_number = page;
            pt->entries[free_frame].data |= 1; // Set the valid bit
        } else {
            // There are no free frames
            int replace_frame = 0;
            switch (pt->algorithm) {
                case FIFO:
                    // Replace the first frame
                    replace_frame = 0;
                    break;
                case LRU:
                    // Replace the least recently used frame
                    for (int i = 1; i < pt->frame_count; i++) {
                        if (pt->entries[i].access_count < pt->entries[replace_frame].access_count) {
                            replace_frame = i;
                        }
                    }
                    break;
                case MFU:
                    // Replace the most frequently used frame
                    for (int i = 1; i < pt->frame_count; i++) {
                        if (pt->entries[i].access_count > pt->entries[replace_frame].access_count) {
                            replace_frame = i;
                        }
                    }
                    break;
            }

            // Should replace the frame
            pt->entries[replace_frame].frame_number = page;
            pt->entries[replace_frame].data |= 1; // Set the valid bit
            pt->entries[replace_frame].access_count = 1; // Reset the access count
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
        printf("%d %d | %d %d\n", i, pt->entries[i].frame_number, (pt->entries[i].data >> 1) & 1, pt->entries[i].data & 1);
    }
}