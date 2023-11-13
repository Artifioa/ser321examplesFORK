# Assignment 4 Activity 1
## Description
The initial Performer code only has one function for adding strings to an array: 

## Protocol

### General Requests
request: { "selected": <int: 1=add, 2=display, 3=deleteAll, 4=replace,
0=quit>, "data": <thing to send>}

### General Response
success response: {"type": <int>, "ok": true, "data": <thing to return> }

Error message should detail what went wrong and should be a string so that the client can just read this string and
Display it in the console, so the user will know what went wrong
  error response: {"type": <int>, "ok": false, "data": {"error": <message>, "details": <string>}}

Some response messages you should use when appropriate:
- "index out of bounds" (if request index is not in list)
- "unknown request" (when the request int is out of range)
- "required data missing" (when required fields are missing)
- "not specified" (when none of the above but something went wrong)

details:
In this string include details like, which data was missing, which required field was missing, or in "not specified" what went wrong. 
Should be a string so easy to read for any client.

#### add
  Adds the given String to the end of the list
  
  request:
    {"selected": 1, "data": {"string": <string>}}
  
    Example: {"selected": "add", "data": {"string": "NewString"}}
  
  response:
    {"type": 1, "ok": true, "data": Current List as String }

#### display
  Displays the current list, is ok if list is empty.
  
  request:
    {"selected": 2}

    Example: {"selected": 2}
  
  response:
    {"type": 2, "ok": true, "data": Current List as String }


#### deleteAll
  Deletes all elements in the list
  
  request:
    {"selected": 3}
  
      Example: {"selected": 3}
  
  response:
    {"type": 3, "ok": true, "data": "Successfully deleted the list." }


#### replace
  Replaces the string at place <int> in the list, with the given String

  request:
   {"selected": 4, "data": {"index": <int>, "string": <string>}}
  
    Example: {"selected": 4, "data": {"index": 2, "string": "Hello"}} -- replaces string at index 2 in list with "Hello"
  
  response:
   {"type": 4, "ok": true, "data": Current List as String }

#### quit
  Tells the server the user wants to disconnect the client

  request:
    {"selected": 0}

    Example: {"selected": 0} 

  response:
    {"type": 0, "ok": true, "data": "Bye" }


## How to run the program
### Terminal
Base Code, please use the following commands:
```
    For Server, run "gradle runServer -Pport=9099 -q --console=plain"
```
```   
    For Client, run "gradle runClient -Phost=localhost -Pport=9099 -q --console=plain"
```   



