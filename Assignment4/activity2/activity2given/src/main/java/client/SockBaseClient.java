package client;

import java.net.*;
import java.io.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

class SockBaseClient {

    public static void main (String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
        int i1=0, i2=0;
        int port = 9099; // default port

        // Make sure two arguments are given
        if (args.length != 2) {
            System.out.println("Expected arguments: <host(String)> <port(int)>");
            System.exit(1);
        }
        String host = args[0];
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port] must be integer");
            System.exit(2);
        }

        // Ask user for username
        System.out.println("Please provide your name for the server. :-)");
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String strToSend = stdin.readLine();

        // Build the first request object just including the name
        Request op = Request.newBuilder()
                .setOperationType(Request.OperationType.NAME)
                .setName(strToSend).build();
        Response response;
        try {
            // connect to the server
            while(true){
            serverSock = new Socket(host, port);

            // write to the server
            out = serverSock.getOutputStream();
            in = serverSock.getInputStream();

            op.writeDelimitedTo(out);

            // read from the server
            response = Response.parseDelimitedFrom(in);

            // print the server response. 
            System.out.println(response.getHello());
            

            // Prompt the user for their choice
            Scanner scanner = new Scanner(System.in);
            int choice = scanner.nextInt();

            // Build the request object based on the user's choice
            
            Request.Builder requestBuilder = Request.newBuilder();
            if (choice == 1){
                requestBuilder.setOperationType(Request.OperationType.LEADERBOARD);
            } else if (choice == 2) {
                requestBuilder.setOperationType(Request.OperationType.NEW);
                Request request = requestBuilder.build();
                request.writeDelimitedTo(out);
                response = Response.parseDelimitedFrom(in);
                if (response.getResponseType() == Response.ResponseType.TASK) {
                    while (true) {
                        // Receive the task from the server
                        System.out.println(response.getTask());

                        // Receive the image from the server
                        String image = response.getImage();
                        System.out.println(image);

                        // Prompt the user for their guess
                        scanner = new Scanner(System.in);
                        Random rand = new Random();
                        int radidid = rand.nextInt(3); // generates a random integer between 0 and 1
                        if (radidid == 0) {
                            System.out.println("Your task is: Type Consonants (Any letter other than A, E, I, O, U)");
                        } else if (radidid == 1) {
                            ArrayList<String> words = new ArrayList<String>();
                                    words.add("apple");
                                    words.add("banana");
                                    words.add("cherry");
                                    words.add("date");
                                    words.add("elderberry");
                                    words.add("fig");
                                    words.add("grape");
                                    words.add("honeydew");
                                    words.add("kiwi");
                                    words.add("lemon");

                                    Random rando = new Random();
                                    String randomWord = words.get(rando.nextInt(words.size()));
                                    System.out.println("Your task is: Type a letter that is not in the word: " + randomWord);
                        } else {
                            System.out.println("Your task is: Type Vowels (A, E, I, O, U)");
                        }
                        String guess = scanner.nextLine();

                        // Build the request object with the user's guess
                        Request guessRequest = Request.newBuilder()
                                .setOperationType(Request.OperationType.ANSWER)
                                .setAnswer(guess)
                                .build();

                        // Send the request to the server
                        guessRequest.writeDelimitedTo(out);

                        // Receive the response from the server
                        response = Response.parseDelimitedFrom(in);

                        // Check if the game is completed
                        if (response.getResponseType() == Response.ResponseType.TASK) {
                            continue;
                        }
                        else if (response.getResponseType() == Response.ResponseType.BYE) {
                            System.out.println("Congratulations, you won a point! Restart? (y/n)");
                            scanner = new Scanner(System.in);
                            String restart = scanner.nextLine();
                            if (restart.equalsIgnoreCase("y")) {
                                break;
                            } else {
                                System.out.println("Goodbye!");
                                System.exit(0);
                            }                            
                        }
                    }
                }
            
            } else if (choice == 3) {
                requestBuilder.setOperationType(Request.OperationType.QUIT);
                Request request = requestBuilder.build();

                // write the request to the server
                request.writeDelimitedTo(out);

                // read the response from the server
                response = Response.parseDelimitedFrom(in);

                // print the server response
                System.out.println(response);

                // check if the user wants to reconnect
                System.out.println("Do you want to reconnect to the server? (y/n)");
                scanner = new Scanner(System.in);
                String reconnectchoice = scanner.nextLine();

                if (reconnectchoice.equalsIgnoreCase("y")) {
                    // close the current connection
                    if (in != null)   in.close();
                    if (out != null)  out.close();
                    if (serverSock != null) serverSock.close();

                    // ask for new host and port
                    System.out.println("Please provide the new host and port for the server.");
                    scanner = new Scanner(System.in);
                    String newHost = scanner.nextLine();
                    int newPort = scanner.nextInt();

                    // reconnect to the server
                    serverSock = new Socket(newHost, newPort);
                    out = serverSock.getOutputStream();
                    in = serverSock.getInputStream();

                    // ask for username again
                    System.out.println("Please provide your name for the server. :-)");
                    stdin = new BufferedReader(new InputStreamReader(System.in));
                    strToSend = stdin.readLine();

                    // build the first request object just including the name
                    op = Request.newBuilder()
                            .setOperationType(Request.OperationType.NAME)
                            .setName(strToSend).build();

                    // write the request to the server
                    op.writeDelimitedTo(out);

                    // read the response from the server
                    response = Response.parseDelimitedFrom(in);

                    // print the server response
                    System.out.println(response.getHello());
                } else {
                    // close the connection and exit
                    if (in != null)   in.close();
                    if (out != null)  out.close();
                    if (serverSock != null) serverSock.close();
                    System.exit(0);
                }
            }
            Request request = requestBuilder.build();

            // write the request to the server
            request.writeDelimitedTo(out);

            // read the response from the server
            response = Response.parseDelimitedFrom(in);

            // print the server response
            System.out.println(response);
           } 

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null)   in.close();
            if (out != null)  out.close();
            if (serverSock != null) serverSock.close();
        }
        
    }

        
    private static void displayLeaderboard(List<Leader> leaderboard) {
        for (Leader leader : leaderboard) {
            System.out.println(leader.getName() + " - Wins: " + leader.getWins() + "\n");
        }
    }

}


