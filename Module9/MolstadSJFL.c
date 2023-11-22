#include <stdio.h>
#include <stdlib.h>

typedef struct {
    int id;
    int tau;
    float alpha;
    int *tn;
} Process;

// Compare function for qsort
int compareSJF(const void *a, const void *b) {
    Process *p1 = (Process *)a;
    Process *p2 = (Process *)b;
    return p1->tn[0] - p2->tn[0];
}

int compareSJFL(const void *a, const void *b) {
    Process *p1 = (Process *)a;
    Process *p2 = (Process *)b;
    return p1->tau - p2->tau;
}

void sjf(Process *processes, int processCount, int ticks) {
    int totalWaitTime = 0, totalTurnaroundTime = 0;
    printf("== Shortest - Job - First ==\n");
    for (int i = 0; i < ticks; i++) {
        printf("Simulating %d th tick of processes @ time %d:\n", i, totalTurnaroundTime);
        qsort(processes, processCount, sizeof(Process), compareSJF);
        for (int j = 0; j < processCount; j++) {
            printf("Process %d took %d.\n", processes[j].id, processes[j].tn[i]);
            if (j != 0) {
                totalWaitTime += processes[j-1].tn[i];
            }
            totalTurnaroundTime += processes[j].tn[i];
        }
    }
    printf("Turnaround time : %d\n", totalTurnaroundTime);
    printf("Waiting time : %d\n", totalWaitTime);
}

void sjfl(Process *processes, int processCount, int ticks) {
    int totalWaitTime = 0, totalTurnaroundTime = 0, totalEstimationError = 0;
    printf("== Shortest - Job - First Live ==\n");
    for (int i = 0; i < ticks; i++) {
        printf("Simulating %d th tick of processes @ time %d:\n", i, totalTurnaroundTime);
        qsort(processes, processCount, sizeof(Process), compareSJFL);
        for (int j = 0; j < processCount; j++) {
            printf("Process %d was estimated for %d and took %d.\n", processes[j].id, processes[j].tau, processes[j].tn[i]);
            totalEstimationError += abs(processes[j].tau - processes[j].tn[i]);
            if (j != 0) {
                totalWaitTime += processes[j-1].tn[i];
            }
            totalTurnaroundTime += processes[j].tn[i];
            processes[j].tau = processes[j].alpha * processes[j].tn[i] + (1 - processes[j].alpha) * processes[j].tau;
        }
    }
    printf("Turnaround time : %d\n", totalTurnaroundTime);
    printf("Waiting time : %d\n", totalWaitTime);
    printf("Estimation Error : %d\n", totalEstimationError);
}

// Main
int main(int argc, char *argv[]) {
    if (argc < 2) {
        printf("Please provide a file name.\n");
        return 1;
    }

    FILE *file = fopen(argv[1], "r");
    if (file == NULL) {
        printf("Failed to open the file.\n");
        return 1;
    }
    int ticks, processCount;
    fscanf(file, "%d", &ticks);
    fscanf(file, "%d", &processCount);

    Process *processes = malloc(processCount * sizeof(Process));
    for (int i = 0; i < processCount; i++) {
        processes[i].id = i;
        fscanf(file, "%d", &processes[i].tau);
        fscanf(file, "%f", &processes[i].alpha);
        processes[i].tn = malloc(ticks * sizeof(int));
        for (int j = 0; j < ticks; j++) {
            fscanf(file, "%d", &processes[i].tn[j]);
        }
    }
    fclose(file);

    // SJF and SJFL algorithms here
    sjf(processes, processCount, ticks);
    sjfl(processes, processCount, ticks);

    for (int i = 0; i < processCount; i++) {
        free(processes[i].tn);
    }
    free(processes);

    return 0;
}

