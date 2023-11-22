Chase Molstad Assignment 5

a) 
The program is designed to be run using Gradle commands.
b) 
To run the program, use these Gradle commands

Node:
gradle runNode 

or if you want a faulty node 

gradle runNode -PFaulty=1

Client:
gradle runClient

Leader:
gradle runLeader

c) 
The program is a distributed system that counts the occurrences of a character in a string.
It consists of a leader node and several worker nodes.
The leader node distributes parts of the string to the worker nodes, collects the counts from the worker nodes, and verifies the counts.

d)
The protocol used in the program is a simple request-response protocol.
The leader node sends a request to the worker nodes with a part of the string and a character to count.
The worker nodes respond with the count of the character in the string part.
The leader then shifts the responses to a different node to check, if theyre all yes then it prints the result, if theyre no then it removes the faulty node and retries.

e)
The workflow of the program

The leader node listens for connections from worker nodes and clients.
When a client connects, the leader node receives a string and a character to count from the client.
The leader node divides the string into parts and sends each part along with the character to a worker node.
Each worker node counts the occurrences of the character in the string part and sends the count back to the leader node.
The leader node collects the counts from all worker nodes, shifts the nodes and verifies the counts, and sends the total count back to the client.

f)
I feel Ive fulfilled requirements 1-15, all of them.

g)
https://youtu.be/xVM2P-E55wA
Link to the presentation video
