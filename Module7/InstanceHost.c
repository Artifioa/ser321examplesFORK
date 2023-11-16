#include "InstanceHost.h"
#include "InstanceHost.h"
#include <pthread.h>
#include <stdlib.h>

//struct for representing the host
struct host {
    pthread_t* instances; //array of active instances
    int num_instances; //number of active instances
    pthread_mutex_t lock; //mutex for thread safety
};

//forward declarations for (private) functions
void* instance_thread(void* arg);

/**
* Initializes the host environment.
*/
host* host_create() {
    host* h = (host*)malloc(sizeof(host));
    h->instances = NULL;
    h->num_instances = 0;
    pthread_mutex_init(&h->lock, NULL);
    return h;
}

/**
* Shuts down the host environment. Ensures any outstanding batches have
* completed.
*/
void host_destroy(host** h) {
    pthread_mutex_lock(&(*h)->lock);
    //wait for all instances to complete
    for (int i = 0; i < (*h)->num_instances; i++) {
        pthread_join((*h)->instances[i], NULL);
    }
    free((*h)->instances);
    pthread_mutex_unlock(&(*h)->lock);
    pthread_mutex_destroy(&(*h)->lock);
    free(*h);
    *h = NULL;
}

/**
* Creates a new server instance (i.e., thread) to handle processing the items
* contained in a batch (i.e., a listed list of job_node). InstanceHost will
* maintain a list of active instances, and if the host is requested to
* shutdown, ensures that all jobs are completed.
*
* @param job_batch_list A list containing the jobs in a batch to process.
*/
void host_request_instance(host* h, struct job_node* batch) {
    printf("LoadBalancer:Received batch and spinning up new instance.\n")
    pthread_mutex_lock(&h->lock);
    //create new instance
    pthread_t instance;
    pthread_create(&instance, NULL, instance_thread, (void*)batch);
    //add instance to array
    h->num_instances++;
    h->instances = (pthread_t*)realloc(h->instances, h->num_instances * sizeof(pthread_t));
    h->instances[h->num_instances - 1] = instance;
    pthread_mutex_unlock(&h->lock);
}

/**
* Private function for processing a batch of jobs in a separate thread.
*/
void* instance_thread(void* arg) {
    struct job_node* batch = (struct job_node*)arg;
    //process jobs in batch
    while (batch != NULL) {
        process_job(batch->job);
        batch = batch->next;
    }
    return NULL;
}
