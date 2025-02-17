/**
 * Program to simulate multiple users simultaneously requesting work (a "job")
 * to be carried by a load balancing server and returned to the user. Job is to
 * compute the square of a number.
 * 
 * @author Khan, Acuna
 * @version 1.2
 */
#define _POSIX_C_SOURCE 199506L

#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <unistd.h>
#include "LoadBalancer.h"
#include "InstanceHost.h"
#include <stdint.h>


//forward declarations for internal (private) functions.
void* simulate_user_request(void* user_id);

//variable to store load balancer object
balancer* lb;
host* h;

/**
 * Entry point to simulation.
 * 
 * @return Zero.
 */
int main() {
    int number_of_requests = 10;
    int batch_size = 5;
    printf("Please input number of requests (users): ");
    //scanf("%d", &number_of_requests);
    printf("Please input batch size: ");
    //scanf("%d", &batch_size);

    pthread_t threads[number_of_requests];

    h = host_create();
    lb = balancer_create(batch_size);

    for (int i = 0; i < number_of_requests; i++) {
        printf("creating: %d\n", i);
        pthread_create(&threads[i], NULL, &simulate_user_request, (void*)(intptr_t)i); // Cast i to intptr_t before casting to void*
    }

    for (int i = 0; i < number_of_requests; i++)
        pthread_join(threads[i], NULL);   

    balancer_destroy(&lb);
    host_destroy(&h);
    
    return 0;
}

void* simulate_user_request(void* user_id) {
    intptr_t id = (intptr_t)user_id; // Use intptr_t instead of int
    int data = rand() % 100;
    int* result = (int*)malloc(sizeof(int));
    *result = -1;
    
    int ms = (rand() % 100) * 1000;
    nanosleep((struct timespec[]){{0, ms*1000000}}, NULL);
    
    printf("User #%d: Wants to process data=%d and store it at %p.\n", id, data, result);
    
    balancer_add_job(lb, id, data, result);
    while(*result == -1);
    
    printf("User #%d: Received result from data=%d as result=%d.\n", id, data, *result);
    
    free(result);
    
    pthread_exit(NULL);
}