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
    struct queue* fifo_queue; // for FIFO
    struct list* lru_list; // for LRU
    int* mfu_counts; // for MFU
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

    // Initialize the entries
    for (int i = 0; i < page_count; i++) {
        pt->entries[i].frame_number = -1; // Indicate that the frame is free
        pt->entries[i].data = 0; // Clear the valid and dirty bits
        pt->entries[i].access_count = 0; // Reset the access count
    }

    return pt;
}

void page_table_destroy(struct page_table** pt) {
    free((*pt)->entries);
    free(*pt);
    *pt = NULL;
}

void page_table_access_page(struct page_table *pt, int page, int write) {
    // Check if the page is valid
    if (pt->entries[page].data & 1) {
        // The page is valid, so just increase the access count
        pt->entries[page].access_count++;
        if (pt->algorithm == LRU) {
            // Move the page to the back of the LRU list
            move_to_back(pt->lru_list, page);
        }
        if (write) {
            // Set the dirty bit
            pt->entries[page].data |= 2;
        }
    } else {
        // The page is not valid, so we have a page fault
        pt->page_faults++;

        // Find the first free frame
        int free_frame = -1;
        for (int i = 0; i < pt->frame_count; i++) {
            if (pt->entries[i].frame_number == -1) {
                free_frame = i;
                break;
            }
        }

        if (free_frame == -1) {
            // No free frame, need to replace a page
            int replace_page;
            if (pt->algorithm == FIFO) {
                replace_page = dequeue(pt->fifo_queue);
            } else if (pt->algorithm == LRU) {
                replace_page = remove_front(pt->lru_list);
            } else if (pt->algorithm == MFU) {
                replace_page = find_max(pt->mfu_counts);
            }
            pt->entries[replace_page].data &= ~1; // Set valid bit to 0
            pt->entries[replace_page].frame_number = -1;
            free_frame = replace_page;
        }

        // Load the page into the free frame
        pt->entries[page].frame_number = free_frame;
        pt->entries[page].data |= 1; // Set the valid bit

        if (pt->algorithm == FIFO) {
            // Add the page to the FIFO queue
            enqueue(pt->fifo_queue, page);
        } else if (pt->algorithm == LRU) {
            // Add the page to the LRU list
            add_to_back(pt->lru_list, page);
        }

        if (write) {
            // Set the dirty bit
            pt->entries[page].data |= 2;
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