#include <stdio.h>
#include <stdlib.h>
#include "PageTable.h"

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
    int* page_order; // For FIFO
    int* last_access_time; // For LRU
    int* access_count; // For MFU
    int* frames_in_use;
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
    pt->page_order = malloc(sizeof(int) * page_count);
    pt->last_access_time = malloc(sizeof(int) * page_count);
    pt->access_count = malloc(sizeof(int) * page_count);
    pt->frames_in_use = calloc(frame_count, sizeof(int));

    // Initialize the entries
    for (int i = 0; i < page_count; i++) {
        pt->entries[i].frame_number = -1; // Indicate that the frame is free
        pt->entries[i].data = 0; // Clear the valid and dirty bits
        pt->entries[i].access_count = 0; // Reset the access count
        pt->page_order[i] = -1; // Initialize page order
        pt->last_access_time[i] = -1; // Initialize last access time
        pt->access_count[i] = 0; // Initialize access count
    }

    return pt;
}

void page_table_destroy(struct page_table** pt) {
    free((*pt)->entries);
    free((*pt)->page_order);
    free((*pt)->last_access_time);
    free((*pt)->access_count);
    free((*pt)->frames_in_use);
    free(*pt);
    *pt = NULL;
}

void page_table_access_page(struct page_table *pt, int page) {
    // Check if the page is valid
    static int current_time = 0;
    current_time++;
    if (pt->entries[page].data & 1) {
        // The page is valid, so just increase the access count
        pt->entries[page].access_count++;
    } else {
        // The page is not valid, so we have a page fault
        pt->page_faults++;

        // Find the first free frame
        int free_frame = -1;
        for (int i = 0; i < pt->frame_count; i++) {
            if (pt->frames_in_use[i] == 0) {
                free_frame = i;
                break;
            }
        }

        if (free_frame != -1) {
            // Found a free frame
            pt->entries[page].frame_number = free_frame;
            pt->entries[page].data |= 1; // Set the valid bit
            pt->frames_in_use[free_frame] = 1;
            // Add the new page to the end of page_order
            for (int i = 0; i < pt->page_count; i++) {
                if (pt->page_order[i] == -1) {
                    pt->page_order[i] = page;
                    break;
                }
            }
        } else {
            // There are no free frames
            int replace_page = -1;
            int replace_frame = 0;
            switch (pt->algorithm) {
                case FIFO:
                    // Replace the oldest frame
                    replace_page = pt->page_order[0];
                    // Shift the other elements of page_order to the left
                    for (int i = 0; i < pt->page_count - 1; i++) {
                        pt->page_order[i] = pt->page_order[i + 1];
                    }
                    // Add the new page to the end of page_order
                    pt->page_order[pt->page_count - 1] = page;
                    break;
                case LRU:
                    // Replace the least recently used frame
                    replace_page = 0;
                    for (int i = 1; i < pt->page_count; i++) {
                        if (pt->last_access_time[i] < pt->last_access_time[replace_page]) {
                            replace_page = i;
                        }
                    }
                    break;
                case MFU:
                    // Replace the most frequently used frame
                    replace_page = 0;
                    for (int i = 1; i < pt->page_count; i++) {
                        if (pt->access_count[i] > pt->access_count[replace_page]) {
                            replace_page = i;
                        }
                    }        
                    break;
            }

            // Invalidate the old page
            replace_frame = pt->entries[replace_page].frame_number;
            pt->entries[replace_page].data &= ~1; // Clear the valid bit
            pt->entries[replace_page].frame_number = -1; // Update the frame_number
            pt->frames_in_use[replace_frame] = 0; // Mark the frame as not in use

            // Replace the frame
            pt->entries[page].frame_number = replace_frame;
            pt->entries[page].data |= 1; // Set the valid bit
            pt->entries[page].access_count = 1; // Reset the access count
            pt->frames_in_use[replace_frame] = 1;
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