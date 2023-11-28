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
    free(*pt);
    *pt = NULL;
}



void mfu(struct page_table *pt, int page) {
    int frames[10];
    int pages[30];
    int count[10];
    int flag_1;
    int flag_2;
    for(int i = 0; i < pt->frame_count; i++) {
        frames[i] = -1;
        count[i] = 0;
    }
    pt->page_faults = 0;
    for(int i = 0; i < pt->page_count; i++) {
        flag_1 = 0;
        flag_2 = 0;
        for(int j = 0; j < pt->frame_count; j++) {
            if(frames[j] == pages[i]) {
                count[j]++;
                flag_1 = 1;
                flag_2 = 1;
                break;
            }
        }
        if(flag_1 == 0) {
            for(int j = 0; j < pt->frame_count; j++) {
                if(frames[j] == -1) {
                    pt->page_faults++;
                    frames[j] = pages[i];
                    count[j] = 1;
                    flag_2 = 1;
                    break;
                }
            }
        }
        if(flag_2 == 0) {
            int max = count[0];
            int pos = 0;
            for(int j = 0; j < pt->frame_count; j++) {
                if(count[j] > max) {
                    max = count[j];
                    pos = j;
                }
            }
            pt->page_faults++;
            frames[pos] = pages[i];
            count[pos] = 1;
        }
    }
}

int findLRU(int time[], int n) {
    int i, minimum = time[0], pos = 0;
    for(i = 1; i < n; ++i) {
        if(time[i] < minimum) {
            minimum = time[i];
            pos = i;
        }
    }
    return pos;
}

void lru(struct page_table *pt, int page) {
    int frames[10];
    int pages[30];
    int counter[10];
    int time[10];
    int flag_1;
    int flag_2;
    int pos;
    int minimum;
    for(int i = 0; i < pt->frame_count; i++) {
        frames[i] = -1;
        counter[i] = 0;
        time[i] = 0;
    }
    pt->page_faults = 0;
    for(int i = 0; i < pt->page_count; i++) {
        flag_1 = 0;
        flag_2 = 0;
        for(int j = 0; j < pt->frame_count; j++) {
            if(frames[j] == pages[i]) {
                counter[j]++;
                time[j] = i;
                flag_1 = 1;
                flag_2 = 1;
                break;
            }
        }
        if(flag_1 == 0) {
            for(int j = 0; j < pt->frame_count; j++) {
                if(frames[j] == -1) {
                    pt->page_faults++;
                    frames[j] = pages[i];
                    counter[j] = 1;
                    time[j] = i;
                    flag_2 = 1;
                    break;
                }
            }
        }
        if(flag_2 == 0) {
            pos = findLRU(time, pt->frame_count);
            pt->page_faults++;
            frames[pos] = pages[i];
            counter[pos] = 1;
            time[pos] = i;
        }
    }
}

void fifo(struct page_table *pt, int page) {
    int frames[10];
    int pages[30];
    int flag_1;
    int flag_2;
    for(int i = 0; i < pt->frame_count; i++) {
        frames[i] = -1;
    }
    pt->page_faults = 0;
    for(int i = 0; i < pt->page_count; i++) {
        flag_1 = 0;
        flag_2 = 0;
        for(int j = 0; j < pt->frame_count; j++) {
            if(frames[j] == pages[i]) {
                flag_1 = 1;
                flag_2 = 1;
                break;
            }
        }
        if(flag_1 == 0) {
            for(int j = 0; j < pt->frame_count; j++) {
                if(frames[j] == -1) {
                    pt->page_faults++;
                    frames[j] = pages[i];
                    flag_2 = 1;
                    break;
                }
            }
        }
        if(flag_2 == 0) {
            for(int j = 0; j < pt->frame_count - 1; j++) {
                frames[j] = frames[j + 1];
            }
            frames[pt->frame_count - 1] = pages[i];
            pt->page_faults++;
        }
    }
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
            if (pt->entries[i].frame_number == -1) {
                free_frame = i;
                break;
            }
        }

        if (free_frame != -1) {
            // Found a free frame
            pt->entries[page].frame_number = free_frame;
            pt->entries[free_frame].data |= 1; // Set the valid bit
            pt->entries[free_frame].access_count = 1; // Reset the access count
        } else {
            // There are no free frames
            int replace_frame = 0;
            switch (pt->algorithm) {
                case FIFO:
                    fifo(pt, page);
                    break;
                case LRU:
                    lru(pt, page);
                    break;
                case MFU:
                    mfu(pt, page);
                    break;
                default:
                    printf("Invalid page replacement algorithm\n");
                    break;
            }

            // Invalidate the old page
            for (int i = 0; i < pt->page_count; i++) {
                if (pt->entries[i].frame_number == replace_frame) {
                    pt->entries[i].data &= ~1; // Clear the valid bit
                    pt->entries[i].frame_number = -1; // Mark the frame as free
                    break;
                }
            }

            // Replace the frame
            pt->entries[page].frame_number = replace_frame;
            pt->entries[replace_frame].data |= 1; // Set the valid bit
            pt->entries[replace_frame].access_count = 1; // Reset the access count

            // Invalidate the replaced page
            for (int i = 0; i < pt->page_count; i++) {
                if (pt->entries[i].frame_number == replace_frame && i != page) {
                    pt->entries[i].data &= ~1; // Clear the valid bit
                    pt->entries[i].frame_number = -1; // Mark the frame as free
                    break;
                }
            }
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