# Assignment 4 Activity 1
## Description
## Chase Molstad

Each task is run by gradle runTask1, 2, or 3

Task 1:
Completed by adding Display, Delete all, and Replace
run task 1 by typing 
'gradle runTask1'
It should auto open a server on port 8000
You can connect to it with 'gradle runClient -Phost=localhost -Pport=8000 -q --console=plain'

Task 2:
Completed a multi-threaded server to handle concurrent client connections.
The ThreadedServer.java class manages client threads, allowing multiple clients to connect simultaneously.
Created a ClientHandler.java class to handle individual client connections on separate threads.
run task 2 by typing
'gradle runTask2'
It should auto open on port 8000
You can connect to it with 'gradle runClient -Phost=localhost -Pport=8000 -q --console=plain'

Task 3:
Completed a thread pool server to handle client connections.
The ThreadPoolServer.java class uses a fixed-size thread pool to efficiently manage concurrent client requests.
Created a ClientHandler.java class to handle individual client connections on separate threads.
run task 3 by typing
'gradle runTask3'
It should auto open on port 8000 with 5 maxConnections
You can connect to it with 'gradle runClient -Phost=localhost -Pport=8000 -q --console=plain'


Video/Screencast for Activity 1:
https://youtu.be/RIf1IQ8bqwI