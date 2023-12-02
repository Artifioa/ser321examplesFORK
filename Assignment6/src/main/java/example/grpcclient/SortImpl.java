// SortImpl.java
package example.grpcclient;

import io.grpc.stub.StreamObserver;
import service.*;
import java.util.Arrays;

class SortImpl extends SortGrpc.SortImplBase {
    @Override
    public void sort(SortRequest req, StreamObserver<SortResponse> responseObserver) {
        int[] data = req.getDataList().stream().mapToInt(i->i).toArray();
        System.out.println("Received from client: " + Arrays.toString(data));
        SortResponse.Builder response = SortResponse.newBuilder();
        try {
            switch (req.getAlgo()) {
                case MERGE:
                    data = mergeSort(data);
                    break;
                case QUICK:
                    quickSort(data, 0, data.length - 1);
                    break;
                case INTERN:
                    Arrays.sort(data);
                    break;
            }
            for (int i : data) {
                response.addData(i);
            }
            response.setIsSuccess(true);
        } catch (Exception e) {
            response.setIsSuccess(false);
            response.setError(e.getMessage());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        System.out.println("Sent to client: " + Arrays.toString(data));
    }

    // Merge sort method
    public static int[] mergeSort(int[] array) {
        if (array.length <= 1) {
            return array;
        }
        
        int midpoint = array.length / 2;
        
        int[] left = new int[midpoint];
        int[] right;
        
        if (array.length % 2 == 0) {
            right = new int[midpoint];
        } else {
            right = new int[midpoint + 1];
        }
        
        for (int i=0; i < midpoint; i++) {
            left[i] = array[i];
        }
        
        for (int j=0; j < right.length; j++) {
            right[j] = array[midpoint+j];
        }
        
        int[] result = new int[array.length];
        
        left = mergeSort(left);
        right = mergeSort(right);
        
        result = merge(left, right);
        
        return result;
    }

    // Merge two sorted arrays
    public static int[] merge(int[] left, int[] right) {
        int[] result = new int[left.length + right.length];
        
        int leftPointer, rightPointer, resultPointer;
        leftPointer = rightPointer = resultPointer = 0;
        
        while(leftPointer < left.length || rightPointer < right.length) {
            if (leftPointer < left.length && rightPointer < right.length) {
                if (left[leftPointer] < right[rightPointer]) {
                    result[resultPointer++] = left[leftPointer++];
                } else {
                    result[resultPointer++] = right[rightPointer++];
                }
            } else if (leftPointer < left.length) {
                result[resultPointer++] = left[leftPointer++];
            } else if (rightPointer < right.length) {
                result[resultPointer++] = right[rightPointer++];
            }
        }
        
        return result;
    }
    // Quick sort method
    public static void quickSort(int[] array, int low, int high) {
        if (low < high) {
            int pivotIndex = partition(array, low, high);
            quickSort(array, low, pivotIndex - 1);
            quickSort(array, pivotIndex + 1, high);
        }
    }

    // Partition method for quick sort
    public static int partition(int[] array, int low, int high) {
        int pivot = array[high];
        int i = (low - 1);
        for (int j = low; j < high; j++) {
            if (array[j] <= pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        int temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;
        return i + 1;
    }
}