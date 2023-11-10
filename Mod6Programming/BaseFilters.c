/**
* (basic description of the program or class)
*
* Completion time: (estimation of hours spent on this program)
*
* @author (your name), (anyone else, e.g., Acuna, whose code you used)
* @version (a version number or a date)
*/

////////////////////////////////////////////////////////////////////////////////
//INCLUDES
#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

//UNCOMMENT BELOW LINE IF USING SER334 LIBRARY/OBJECT FOR BMP SUPPORT
#include "BmpProcessor.h"


#include <string.h>
#include <math.h>


////////////////////////////////////////////////////////////////////////////////
//MACRO DEFINITIONS

//problem assumptions
#define BMP_HEADER_SIZE 14
#define BMP_DIB_HEADER_SIZE 40
#define MAXIMUM_IMAGE_SIZE 4096
#define THREAD_COUNT 4

//TODO: finish me


////////////////////////////////////////////////////////////////////////////////
//DATA STRUCTURES

//TODO: finish me
	int image_width;
	int image_height;
	unsigned char* input_pixels;
	unsigned char* output_pixels;
	int num_holes;
	int (*hole_centers)[2];
	double* hole_radii;
	int average_radius;

////////////////////////////////////////////////////////////////////////////////
//IMAGE FILTER FUNCTIONS

#include <math.h>
#include <time.h>

// Define the yellow tint function
unsigned char* yellow_tint(unsigned char* pixel) {
    unsigned char* tinted_pixel = malloc(3 * sizeof(unsigned char));
    if (tinted_pixel == NULL) {
        // Handle memory allocation error
        return NULL;
    }

    // Define the tinting values
    int red_tint = 30;
    int green_tint = 30;
    int blue_tint = 0;  // No tint on the blue channel

    // Apply tinting, ensuring that the values don't exceed 255
    tinted_pixel[0] = (pixel[0] + red_tint <= 255) ? pixel[0] + red_tint : 255;
    tinted_pixel[1] = (pixel[1] + green_tint <= 255) ? pixel[1] + green_tint : 255;
    tinted_pixel[2] = (pixel[2] + blue_tint <= 255) ? pixel[2] + blue_tint : 255;

    return tinted_pixel;
}


// Define the draw holes function
// Define the draw holes function
void draw_holes(unsigned char* input_pixels, unsigned char* output_pixels, int image_width, int image_height, int average_radius) {
    // Calculate the number of holes to be drawn based on the smallest side of the image
    int num_holes = round(0.08 * fmin(image_width, image_height));

    // Allocate memory for the hole centers and radii
    int (*hole_centers)[2] = malloc(num_holes * sizeof(int[2]));
    double* hole_radii = malloc(num_holes * sizeof(double));

    // Generate random x and y coordinates for each hole, ensuring that they are uniformly distributed along the x- and y-axis
    for (int i = 0; i < num_holes; i++) {
        hole_centers[i][0] = round((i + 0.5) * image_width / num_holes);
        hole_centers[i][1] = round((i + 0.5) * image_height / num_holes);
    }

    // Generate a random radius for each hole, ensuring that the average radius is most common and smaller or larger radii are less common
    srand(time(NULL));
    for (int i = 0; i < num_holes; i++) {
        double r = ((double)rand() / RAND_MAX);
        if (r < 0.5) {
            hole_radii[i] = average_radius * sqrt(r * 2);
        }
        else {
            hole_radii[i] = average_radius / sqrt((1 - r) * 2);
        }
    }

    // Copy the input image to the output image
    memcpy(output_pixels, input_pixels, image_width * image_height * 3);

    // Draw a black circle with the generated radius at each x and y coordinate
    for (int i = 0; i < num_holes; i++) {
        int x_center = hole_centers[i][0];
        int y_center = hole_centers[i][1];
        double radius = hole_radii[i];

        for (int y = 0; y < image_height; y++) {
            for (int x = 0; x < image_width; x++) {
                int output_index = (y * image_width + x) * 3;

                // Check if the current pixel is within the circle
                if (pow(x - x_center, 2) + pow(y - y_center, 2) <= pow(radius, 2)) {
                    // Set the pixel to black
                    output_pixels[output_index] = 0;
                    output_pixels[output_index + 1] = 0;
                    output_pixels[output_index + 2] = 0;
                }
            }
        }
    }

    // Free the memory allocated for the hole centers and radii
    free(hole_centers);
    free(hole_radii);
}


// Define the Swiss cheese filter function
void swiss_cheese(unsigned char* input_pixels, unsigned char* output_pixels, int image_width, int image_height) {
	// Define the average radius of the holes
    int average_radius = round(0.08 * fmin(image_width, image_height));
	//Drawing holes
	draw_holes(input_pixels, output_pixels, image_width, image_height, average_radius);
    // Loop through each pixel in the image
	/*
    for (int y = 0; y < image_height; y++) {
        for (int x = 0; x < image_width; x++) {
            // Calculate the index of the current pixel in the input and output pixel arrays
            int input_index = (y * image_width + x) * 3;
            int output_index = (y * image_width + x) * 3;

            // Apply the yellow tint to the original input pixel values and store in the output pixel array
            unsigned char* tinted_pixel = yellow_tint(&input_pixels[input_index]);
            if (tinted_pixel != NULL) {
                output_pixels[output_index] = tinted_pixel[0];
                output_pixels[output_index + 1] = tinted_pixel[1];
                output_pixels[output_index + 2] = tinted_pixel[2];

                // Free the memory allocated for the tinted pixel
                free(tinted_pixel);
            } else {
                // Handle error when memory allocation fails
                printf("Error: Memory allocation failed in yellow_tint\n");
				return;
            }
        }
    }*/

}


// Define a new function that has the correct type for pthread_create()
void* swiss_cheese_thread(void* arg) {
    // Cast the argument to the correct type
    int thread_index = *(int*)arg;
    free(arg);

    // Calculate the row range for this thread
    int row_height = image_height / THREAD_COUNT;
    int row_remainder = image_height % THREAD_COUNT;
    int row_start = thread_index * row_height;
    int row_end = (thread_index + 1) * row_height;
    if (thread_index == THREAD_COUNT - 1) {
        row_end += row_remainder;
    }

    // Call the swiss_cheese() function for each row
    for (int y = row_start; y < row_end; y++) {
        swiss_cheese(input_pixels + y * image_width * 3, output_pixels + y * image_width * 3, image_width, 1);
    }

    // Exit the thread
    pthread_exit(NULL);
}
////////////////////////////////////////////////////////////////////////////////
//MAIN PROGRAM CODE
// Define the box blur filter function
void* blur_filter(void* arg) {
	// Get the thread index
	int thread_index = *(int*)arg;
	free(arg);

	// Calculate the column range for this thread
	int column_width = image_width / THREAD_COUNT;
	int column_remainder = image_width % THREAD_COUNT;
	int column_start = thread_index * column_width;
	int column_end = (thread_index + 1) * column_width;
	if (thread_index == THREAD_COUNT - 1) {
		column_end += column_remainder;
	}

	// Allocate memory for the pixel data array
	unsigned char* pixel_data = (unsigned char*)malloc(image_height * (column_end - column_start) * 3);
	if (pixel_data == NULL) {
		printf("Error: could not allocate memory for pixel data array\n");
		pthread_exit(NULL);
	}

	// Apply the box blur filter to each pixel in the column range
	for (int x = column_start; x < column_end; x++) {
		for (int y = 0; y < image_height; y++) {
			// Initialize the sum of pixel values in the neighborhood
			int sum_red = 0;
			int sum_green = 0;
			int sum_blue = 0;
			int count = 0;

			// Loop through each pixel in the neighborhood
			for (int j = -1; j <= 1; j++) {
				for (int i = -1; i <= 1; i++) {
					// Calculate the coordinates of the current pixel in the neighborhood
					int neighbor_x = x + i;
					int neighbor_y = y + j;

					// Check if the current pixel is within the bounds of the image
					if (neighbor_x >= 0 && neighbor_x < image_width && neighbor_y >= 0 && neighbor_y < image_height) {
						// Calculate the index of the current pixel in the input pixel array
						int input_index = (neighbor_y * image_width + neighbor_x) * 3;

						// Add the pixel values to the sum
						sum_red += input_pixels[input_index];
						sum_green += input_pixels[input_index + 1];
						sum_blue += input_pixels[input_index + 2];
						count++;
					}
				}
			}

			// Calculate the average pixel value in the neighborhood
			int output_index = (y * (column_end - column_start) + (x - column_start)) * 3;
			pixel_data[output_index] = sum_red / count;
			pixel_data[output_index + 1] = sum_green / count;
			pixel_data[output_index + 2] = sum_blue / count;
		}
	}

	// Copy the modified pixel data to the output pixel array
	for (int x = column_start; x < column_end; x++) {
		for (int y = 0; y < image_height; y++) {
			int input_index = (y * (column_end - column_start) + (x - column_start)) * 3;
			int output_index = (y * image_width + x) * 3;
			output_pixels[output_index] = pixel_data[input_index];
			output_pixels[output_index + 1] = pixel_data[input_index + 1];
			output_pixels[output_index + 2] = pixel_data[input_index + 2];
		}
	}

	// Free memory
	free(pixel_data);
	pthread_exit(NULL);
}

int main(int argc, char* argv[]) {
	// Read command line arguments
	char* input_file_name;
	char* output_file_name;
	char filter_type;
	if (argc == 7 && strcmp(argv[1], "-i") == 0 && strcmp(argv[3], "-o") == 0 && strcmp(argv[5], "-f") == 0) {
		input_file_name = argv[2];
		output_file_name = argv[4];
		filter_type = argv[6][0];
	} else {
		printf("Usage: %s -i input_file.bmp -o output_file.bmp -f filter_type\n", argv[0]);
		return 1;
	}

	// Open input BMP file and read header and DIB header
	FILE* input_file = fopen(input_file_name, "rb");
	if (input_file == NULL) {
		printf("Error: could not open input file %s\n", input_file_name);
		return 1;
	}
	unsigned char bmp_header[BMP_HEADER_SIZE];
	unsigned char dib_header[BMP_DIB_HEADER_SIZE];
	fread(bmp_header, 1, BMP_HEADER_SIZE, input_file);
	fread(dib_header, 1, BMP_DIB_HEADER_SIZE, input_file);
	image_width = *(int*)(dib_header + 4);
	image_height = *(int*)(dib_header + 8);
	int pixel_data_offset = *(int*)(bmp_header + 10);

	// Allocate memory for pixel data array
	if (image_width > MAXIMUM_IMAGE_SIZE || image_height > MAXIMUM_IMAGE_SIZE) {
		printf("Error: image size exceeds maximum allowed size of %dx%d pixels\n", MAXIMUM_IMAGE_SIZE, MAXIMUM_IMAGE_SIZE);
		return 1;
	}
	input_pixels = (unsigned char*)malloc(image_width * image_height * 3);
	output_pixels = (unsigned char*)malloc(image_width * image_height * 3);
	if (input_pixels == NULL || output_pixels == NULL) {
		printf("Error: could not allocate memory for pixel data array\n");
		return 1;
	}

	// Read pixel data from input file
	fseek(input_file, pixel_data_offset, SEEK_SET);
	fread(input_pixels, 1, image_width * image_height * 3, input_file);
	fclose(input_file);

	// Apply the selected filter to the image data using pthreads
	if (filter_type == 'c') {
		// Divide image into pixel columns
		int column_width = image_width / THREAD_COUNT;
		int column_remainder = image_width % THREAD_COUNT;
		int column_offsets[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++) {
			column_offsets[i] = i * column_width;
		}
		column_offsets[THREAD_COUNT - 1] += column_remainder;

		// Create threads to process pixel columns
		pthread_t threads[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++) {
			int* arg = (int*)malloc(sizeof(int));
			*arg = i;
			pthread_create(&threads[i], NULL, swiss_cheese_thread, arg);
		}

		// Wait for threads to finish and combine pixel columns
		for (int i = 0; i < THREAD_COUNT; i++) {
			pthread_join(threads[i], NULL);
		}
	} else if (filter_type == 'b') {
		// Divide image into pixel columns
		int column_width = image_width / THREAD_COUNT;
		int column_remainder = image_width % THREAD_COUNT;
		int column_offsets[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++) {
			column_offsets[i] = i * column_width;
		}
		column_offsets[THREAD_COUNT - 1] += column_remainder;

		// Create threads to process pixel columns
		pthread_t threads[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++) {
			int* arg = (int*)malloc(sizeof(int));
			*arg = i;
			pthread_create(&threads[i], NULL, blur_filter, arg);
		}

		// Wait for threads to finish
		for (int i = 0; i < THREAD_COUNT; i++) {
			pthread_join(threads[i], NULL);
		}
	}
	else {
		printf("Error: invalid filter type specified\n");
		return 1;
	}

	// Write modified pixel data to output file
	FILE* output_file = fopen(output_file_name, "wb");
	if (output_file == NULL) {
		printf("Error: could not open output file %s\n", output_file_name);
		return 1;
	}
	fwrite(bmp_header, 1, BMP_HEADER_SIZE, output_file);
	fwrite(dib_header, 1, BMP_DIB_HEADER_SIZE, output_file);
	fwrite(output_pixels, 1, image_width * image_height * 3, output_file);
	fclose(output_file);

	// Free memory
	free(input_pixels);
	free(output_pixels);

	return 0;
}