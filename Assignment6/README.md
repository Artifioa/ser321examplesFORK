# Project Title

Chase Molstad Assignment 6 Registry Node Client implementation.

## Description (a)

This project is a gRPC server implemented in Java. It provides several services including Echo, Joke, Sort, Encryption, and Weather. The server is designed to run on a specified port and register its services with a registry.
I feel my project fulfuls everything 
Task 1, Task 2, and Task 3.2.

except Task 3.1, running things with the Registry correctly.

## How to Run (b)

To run the server, use the following command:

gradle runNode 
gradle runClient
gradle runRegistry

Each of them also run with -Phost= and -Pport. Client also has a -Pauto that will automatically display everything.

## Inputs (c)
Client has a 0-7 input system,
0 Closes the program
1 Does the Echo service, which prompts you to enter a sentence
2 Does the Joke service, which prompts you to enter how many jokes you want
3 Does the Sorting service, which prompts a "array" of numbers, and then the sorting algorithm you desire
4 Does the Encryption service, which prompts you to enter a sentence
5 Does the Decryption service, which prompts you to enter a encrypted sentence
6 Does the Weather service, which prompts you to enter a location (Arizona), and then gives you the current weather
7 Does the Weather service as well, which prompts you to enter a location (Arizona), and then gives you the weather forecast

## Screencast (f)
https://youtu.be/agoiL3OQqzo
