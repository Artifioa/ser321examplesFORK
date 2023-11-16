#include "InstanceHost.h"
#include "LoadBalancer.h"
#include <stdlib.h>

//structure to represent the load balancer
struct balancer {
    int batch_size;         //size of a batch
    int num_jobs;           //number of jobs in the current batch
    struct job_node* jobs;  //pointer to the first job in the current batch
    struct balancer* next;  //pointer to the next load balancer in a list of load balancers
};

//helper function to create a new job node
struct job_node* job_node_create(int user_id, int data, int* data_return) {
    struct job_node* node = (struct job_node*) malloc(sizeof(struct job_node));
    node->user_id = user_id;
    node->data = data;
    node->data_result = data_return;
    node->next = NULL;
    return node;
}

//helper function to free a job node
void job_node_destroy(struct job_node* node) {
    free(node);
}

//helper function to process a batch of jobs
void process_batch(struct balancer* lb, struct job_node* jobs) {
    //create an array to hold the data for the batch
    int* batch_data = (int*) malloc(sizeof(int) * lb->batch_size);
    int i = 0;
    //copy the data from the job nodes into the batch data array
    while (jobs != NULL) {
        batch_data[i] = jobs->data;
        jobs = jobs->next;
        i++;
    }
    //get an instance from the instance host and process the batch
    int* batch_result = instance_process_batch(instance_host_get_instance(), batch_data, lb->batch_size);
    
    //copy the results back into the job nodes
    i = 0;
    while (lb->jobs != NULL) {
        lb->jobs->data_result = &batch_result[i];
        lb->jobs = lb->jobs->next;
        i++;
    }
    //free the batch data and result arrays
    free(batch_data);
    free(batch_result);
}

//public function to create a load balancer
balancer* balancer_create(int batch_size) {
    balancer* lb = (balancer*) malloc(sizeof(balancer));
    lb->batch_size = batch_size;
    lb->num_jobs = 0;
    lb->jobs = NULL;
    lb->next = NULL;
    return lb;
}

//public function to destroy a load balancer
void balancer_destroy(balancer** lb) {
    //process any outstanding jobs
    if ((*lb)->num_jobs > 0) {
        process_batch(*lb, (*lb)->jobs);
    }
    //free the job nodes
    while ((*lb)->jobs != NULL) {
        struct job_node* temp = (*lb)->jobs;
        (*lb)->jobs = (*lb)->jobs->next;
        job_node_destroy(temp);
    }
    //free the load balancer
    free(*lb);
    *lb = NULL;
}

//public function to add a job to a load balancer
void balancer_add_job(balancer* lb, int user_id, int data, int* data_return) {

    printf("LoadBalancer:Received new job from user# %d to process data= %d and store it at %p.\n",user_id,data,data_return);
    //create a new job node
    struct job_node* node = job_node_create(user_id, data, data_return);
    //add the job node to the end of the list of jobs
    if (lb->jobs == NULL) {
        lb->jobs = node;
    } else {
        struct job_node* temp = lb->jobs;
        while (temp->next != NULL) {
            temp = temp->next;
        }
        temp->next = node;
    }
    lb->num_jobs++;
    //if the batch is full, process it
    if (lb->num_jobs == lb->batch_size) {
        process_batch(lb, lb->jobs);
        lb->num_jobs = 0;
        lb->jobs = NULL;
    }
}


