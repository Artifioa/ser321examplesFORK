/**
 * Implementation file for functions to simulate a load balancer.
 * 
 * @author Chase Molstad, Acuna
 * @version 1.1
 */

#include "InstanceHost.h"
#include "LoadBalancer.h"
#include <pthread.h>
#include <stdlib.h>

struct balancer {
    pthread_mutex_t mutex;
    struct job_node* jobs;
    int batch_size;
    int job_count;
};

balancer* balancer_create(int batch_size) {
    balancer* lb = malloc(sizeof(balancer));
    pthread_mutex_init(&lb->mutex, NULL);
    lb->jobs = NULL;
    lb->batch_size = batch_size;
    lb->job_count = 0;
    return lb;
}

void balancer_destroy(balancer** lb) {
    pthread_mutex_destroy(&(*lb)->mutex);
    free(*lb);
    *lb = NULL;
}

void balancer_add_job(balancer* lb, int user_id, int data, int* data_return) {
    printf("LoadBalancer: Received new job from user #%d to process data = %d and store it at %p.\n",user_id,data,data_return);
    struct job_node* job = malloc(sizeof(struct job_node));
    job->user_id = user_id;
    job->data = data;
    job->data_result = data_return;
    job->next = lb->jobs;
    lb->jobs = job;
    lb->job_count++;

    if (lb->job_count >= lb->batch_size) {
        host_request_instance(NULL, lb->jobs); // Replace NULL with your host instance
        lb->jobs = NULL;
        lb->job_count = 0;
    }
}