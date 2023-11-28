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
    struct Queue* fifo_queue;
    struct list* lru_list;
    struct priority_queue* mfu_queue;

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
    pt->fifo_queue = createQueue(frame_count);
    pt->lru_list = list_create(frame_count);
    pt->mfu_queue = priority_queue_create(frame_count);

    // Initialize the entries
    for (int i = 0; i < page_count; i++) {
        pt->entries[i].frame_number = -1; // Indicate that the frame is free
        pt->entries[i].data = 0; // Clear the valid and dirty bits
        pt->entries[i].access_count = 0; // Reset the access count
    }

    return pt;
}

void page_table_destroy(struct page_table** pt) {
    free((*pt)->fifo_queue->array);
    free((*pt)->fifo_queue);
    free((*pt)->lru_list->data);
    free((*pt)->lru_list);
    free((*pt)->mfu_queue->data);
    free((*pt)->mfu_queue->priorities);
    free((*pt)->mfu_queue);
    free((*pt)->entries);
    free(*pt);
    *pt = NULL;
}


































typedef struct Queue {
    int front, rear, size;
    unsigned capacity;
    int* array;
} Queue;

Queue* createQueue(unsigned capacity) {
    Queue* queue = (Queue*) malloc(sizeof(Queue));
    queue->capacity = capacity;
    queue->front = queue->size = 0; 
    queue->rear = capacity - 1;
    queue->array = (int*) malloc(queue->capacity * sizeof(int));
    return queue;
}

int isFull(Queue* queue) {
    return (queue->size == queue->capacity);
}

int isEmpty(Queue* queue) {
    return (queue->size == 0);
}

void enqueue(Queue* queue, int item) {
    queue->rear = (queue->rear + 1) % queue->capacity;
    queue->array[queue->rear] = item;
    queue->size = queue->size + 1;
}

int queue_pop(Queue* queue) {
    int item = queue->array[queue->front];
    queue->front = (queue->front + 1) % queue->capacity;
    queue->size = queue->size - 1;
    return item;
}

void queue_push(Queue* queue, int item) {
    queue->rear = (queue->rear + 1) % queue->capacity;
    queue->array[queue->rear] = item;
    queue->size = queue->size + 1;
}

int dequeue(Queue* queue) {
    int item = queue->array[queue->front];
    queue->front = (queue->front + 1) % queue->capacity;
    queue->size = queue->size - 1;
    return item;
}

int front(Queue* queue) {
    return queue->array[queue->front];
}

int rear(Queue* queue) {
    return queue->array[queue->rear];
}

// List structure
struct list {
    int* data;
    int size;
    int capacity;
};

struct list* list_create(int capacity) {
    struct list* l = malloc(sizeof(struct list));
    l->data = malloc(sizeof(int) * capacity);
    l->size = 0;
    l->capacity = capacity;
    return l;
}

void list_push_back(struct list* l, int item) {
    l->data[l->size++] = item;
}

void list_remove(struct list* l, int item) {
    int i;
    for (i = 0; i < l->size; i++) {
        if (l->data[i] == item) break;
    }
    for (; i < l->size - 1; i++) {
        l->data[i] = l->data[i + 1];
    }
    l->size--;
}

int list_pop_front(struct list* l) {
    int item = l->data[0];
    list_remove(l, item);
    return item;
}

// Priority queue structure
struct priority_queue {
    int* data;
    int* priorities;
    int size;
    int capacity;
};

struct priority_queue* priority_queue_create(int capacity) {
    struct priority_queue* pq = malloc(sizeof(struct priority_queue));
    pq->data = malloc(sizeof(int) * capacity);
    pq->priorities = malloc(sizeof(int) * capacity);
    pq->size = 0;
    pq->capacity = capacity;
    return pq;
}

void priority_queue_push(struct priority_queue* pq, int item, int priority) {
    pq->data[pq->size] = item;
    pq->priorities[pq->size] = priority;
    pq->size++;
}

void priority_queue_increase(struct priority_queue* pq, int item) {
    for (int i = 0; i < pq->size; i++) {
        if (pq->data[i] == item) {
            pq->priorities[i]++;
            break;
        }
    }
}

int priority_queue_pop(struct priority_queue* pq) {
    int max_priority_index = 0;
    for (int i = 1; i < pq->size; i++) {
        if (pq->priorities[i] > pq->priorities[max_priority_index]) {
            max_priority_index = i;
        }
    }
    int item = pq->data[max_priority_index];
    for (int i = max_priority_index; i < pq->size - 1; i++) {
        pq->data[i] = pq->data[i + 1];
        pq->priorities[i] = pq->priorities[i + 1];
    }
    pq->size--;
    return item;
}













































void page_table_access_page(struct page_table *pt, int page) {
    // Check if the page is valid
    if (pt->entries[page].data & 1) {
        // The page is valid, so just increase the access count
        pt->entries[page].access_count++;

        // Update the LRU list
        list_remove(pt->lru_list, page);
        list_push_back(pt->lru_list, page);

        // Update the MFU priority queue
        priority_queue_increase(pt->mfu_queue, page);
    } else {
        // The page is not valid, so we have a page fault
        pt->page_faults++;

        // Find the first free frame
        int free_frame = -1;
        for (int i = 0; i < pt->page_count; i++) {
            if (!(pt->entries[i].data & 1)) {
                free_frame = i;
                break;
            }
        }

        if (free_frame != -1) {
            // Found a free frame
            pt->entries[free_frame].frame_number = page;
            pt->entries[free_frame].data |= 1; // Set the valid bit
            pt->entries[free_frame].access_count = 1; // Reset the access count
        } else {
            // There are no free frames
            int replace_page;
            switch (pt->algorithm) {
                case FIFO:
                    replace_page = queue_pop(pt->fifo_queue);
                    break;
                case LRU:
                    replace_page = list_pop_front(pt->lru_list);
                    break;
                case MFU:
                    replace_page = priority_queue_pop(pt->mfu_queue);
                    break;
            }

            // Invalidate the old page
            for (int i = 0; i < pt->page_count; i++) {
                if (pt->entries[i].frame_number == replace_page) {
                    pt->entries[i].data &= ~1; // Clear the valid bit
                    break;
                }
            }

            // Replace the frame
            pt->entries[replace_page].frame_number = page;
            pt->entries[replace_page].data |= 1; // Set the valid bit
            pt->entries[replace_page].access_count = 1; // Reset the access count
        }

        // Add the page to the data structures for each algorithm
        queue_push(pt->fifo_queue, page);
        list_push_back(pt->lru_list, page);
        priority_queue_push(pt->mfu_queue, page, pt->entries[page].access_count);
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











