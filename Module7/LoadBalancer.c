/**
 * Implementation file for functions to simulate a load balancer.
 * 
 * @author Chase Molstad, Acuna
 * @version 1.1
 */

#include "LoadBalancer.h"
#include "InstanceHost.h"
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>

// Structure to represent the load balancer
struct balancer {
    int batch_size;              // Batch size for the load balancer
    struct job_node* job_list;   // List of jobs
    int job_count;               // Number of jobs in the list
    pthread_mutex_t mutex;       // Mutex to protect the job list
    host* instance_host;         // Instance host associated with the load balancer
};

balancer* balancer_create(int batch_size) {
    balancer* lb = (balancer*)malloc(sizeof(balancer));
    if (lb == NULL) {
        perror("Error creating load balancer");
        exit(EXIT_FAILURE);
    }

    lb->batch_size = batch_size;
    lb->job_list = NULL;
    lb->job_count = 0;
    pthread_mutex_init(&lb->mutex, NULL);
    lb->instance_host = host_create();

    return lb;
}

void balancer_destroy(balancer** lb) {
    if (lb == NULL || *lb == NULL) {
        return;
    }

    // Ensure any outstanding batches have completed
    pthread_mutex_lock(&(*lb)->mutex);
    if ((*lb)->job_count > 0) {
        // If there are leftover jobs, create an instance host to handle them
        host_request_instance((*lb)->instance_host, (*lb)->job_list);
        (*lb)->job_list = NULL;
        (*lb)->job_count = 0;
    }
    pthread_mutex_unlock(&(*lb)->mutex);

    // Clean up
    host_destroy(&(*lb)->instance_host);
    pthread_mutex_destroy(&(*lb)->mutex);
    free(*lb);
    *lb = NULL;
}

void balancer_add_job(balancer* lb, int user_id, int data, int* data_return) {
    printf("LoadBalancer: Received new job from user# %d to process data= %d and store it at %p.\n",user_id,data,data_return);
    pthread_mutex_lock(&lb->mutex);

    // Create a new job node
    struct job_node* new_job = (struct job_node*)malloc(sizeof(struct job_node));
    if (new_job == NULL) {
        perror("Error creating job");
        exit(EXIT_FAILURE);
    }

    new_job->user_id = user_id;
    new_job->data = data;
    new_job->data_result = data_return;
    new_job->next = NULL;

    // Add the job to the list
    if (lb->job_list == NULL) {
        lb->job_list = new_job;
    } else {
        struct job_node* current_job = lb->job_list;
        while (current_job->next != NULL) {
            current_job = current_job->next;
        }
        current_job->next = new_job;
    }

    lb->job_count++;

    // If enough jobs have been added to fill a batch, request a new instance
    if (lb->job_count >= lb->batch_size) {
        host_request_instance(lb->instance_host, lb->job_list);
        lb->job_list = NULL;
        lb->job_count = 0;
    }

    pthread_mutex_unlock(&lb->mutex);
}