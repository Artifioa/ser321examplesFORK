/**
 * Implementation file for functions to simulate a cloud-like server instance
 * host.
 * 
 * @author Chase Molstad, Acuna
 * @version 1.1
 */

#include "InstanceHost.h"
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

// Structure to represent a server instance (thread)
struct instance {
    pthread_t thread;           // Thread representing the server instance
    struct job_node* current_job; // Current job being processed by the instance
    struct instance* next;      // Pointer to the next instance in the list
};

// Structure to represent the host
struct host {
    pthread_mutex_t mutex;       // Mutex to protect the list of active instances
    struct instance* instances;  // List of active instances
    int instance_count;          // Number of active instances
};

// Function to process a job (data value squared)
void process_job(struct job_node* job) {
    if (job != NULL && job->data_result != NULL) {
        *(job->data_result) = job->data * job->data;
    }
}

// Thread function to handle processing of jobs for an instance
void* instance_thread(void* arg) {
    struct instance* inst = (struct instance*)arg;
    
    // Process jobs until there are no more
    while (inst->current_job != NULL) {
        process_job(inst->current_job);
        inst->current_job = inst->current_job->next;
    }

    return NULL;
}

host* host_create() {
    host* h = (host*)malloc(sizeof(host));
    if (h == NULL) {
        perror("Error creating host");
        exit(EXIT_FAILURE);
    }

    pthread_mutex_init(&h->mutex, NULL);
    h->instances = NULL;
    h->instance_count = 0;

    return h;
}

void host_destroy(host** h) {
    if (h == NULL || *h == NULL) {
        return;
    }

    // Wait for all instances to finish
    pthread_mutex_lock(&(*h)->mutex);
    struct instance* current_instance = (*h)->instances;
    while (current_instance != NULL) {
        pthread_join(current_instance->thread, NULL);
        current_instance = current_instance->next;
    }
    pthread_mutex_unlock(&(*h)->mutex);

    // Clean up
    pthread_mutex_destroy(&(*h)->mutex);
    free(*h);
    *h = NULL;
}

void host_request_instance(host* h, struct job_node* batch) {
    printf("LoadBalancer:Received batch and spinning up new instance.\n")
    pthread_mutex_lock(&h->mutex); 

    // Create a new instance
    struct instance* new_instance = (struct instance*)malloc(sizeof(struct instance));
    if (new_instance == NULL) {
        perror("Error creating instance");
        exit(EXIT_FAILURE);
    }

    new_instance->current_job = batch;
    pthread_create(&new_instance->thread, NULL, &instance_thread, (void*)new_instance);

    // Add the new instance to the list
    new_instance->next = h->instances;
    h->instances = new_instance;
    h->instance_count++;

    pthread_mutex_unlock(&h->mutex);
}