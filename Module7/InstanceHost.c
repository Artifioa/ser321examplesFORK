/**
 * Implementation file for functions to simulate a cloud-like server instance
 * host.
 * 
 * @author Chase Molstad

// Rest of the code goes here

 * @version 1.1
 */

#include "InstanceHost.h"
#include <pthread.h>
#include <stdlib.h>
#include <stdio.h>
struct host {
    pthread_mutex_t mutex;
    struct job_node* jobs;
};

host* host_create() {
    host* h = malloc(sizeof(host));
    pthread_mutex_init(&h->mutex, NULL);
    h->jobs = NULL;
    return h;
}

void host_destroy(host** h) {
    pthread_mutex_destroy(&(*h)->mutex);
    free(*h);
    *h = NULL;
}

void* process_job(void* arg) {
    struct job_node* job = (struct job_node*) arg;
    *(job->data_result) = job->data * job->data;
    return NULL;
}

void host_request_instance(host* h, struct job_node* batch) {
    printf("LoadBalancer: Received batch and spinning up new instance.\n");
    pthread_t thread;
    pthread_create(&thread, NULL, process_job, (void*) batch);
    pthread_detach(thread);
}