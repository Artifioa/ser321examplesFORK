package client;

import java.net.*;
import java.io.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

import java.util.*;

public class SockBaseClient {

    public static void main(String args[]) throws Exception {
        Socket serverSock = null;
        OutputStream out = null;
        InputStream in = null;
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
            System.out.println("[Port] must be an integer");
            System.exit(2);
        }

        try {
            Scanner scanner = new Scanner(System.in);

            System.out.println("Please provide your name for the server. :-)");
            String playerName = scanner.nextLine();

            while (true) {
                System.out.println("\nMain Menu:");
                System.out.println("1: Leaderboard");
                System.out.println("2: Play Game");
                System.out.println("3: Quit");

                int choice;
                try {
                    System.out.print("Enter your choice: ");
                    choice = scanner.nextInt();
                } catch (Exception e) {
                    System.out.println("Invalid input. Please enter a number.");
                    scanner.nextLine(); // Clear the buffer
                    continue;
                }

                // Build the request object with the player's name
                Request op = Request.newBuilder()
                        .setOperationType(Request.OperationType.NAME)
                        .setName(playerName).build();

                // Connect to the server
                serverSock = new Socket(host, port);

                // Get input and output streams
                out = serverSock.getOutputStream();
                in = serverSock.getInputStream();

                // Send the request to the server
                op.writeDelimitedTo(out);

                // Read the server's response
                Response response = Response.parseDelimitedFrom(in);

                switch (choice) {
                    case 1:
                        // Request and display the leaderboard from the server
                        Request leaderboardRequest = Request.newBuilder()
                                .setOperationType(Request.OperationType.LEADERBOARD)
                                .build();
                        leaderboardRequest.writeDelimitedTo(out);

                        Response leaderboardResponse = Response.parseDelimitedFrom(in);

                        System.out.println("Leaderboard:");
                        for (Leader leader : leaderboardResponse.getLeaderboardList()) {
                            System.out.println(leader.getName() + ": Wins - " + leader.getWins());
                        }
                        break;
                    case 2:
                        System.out.println("Guess the ASCII image! Type your guess and press 'enter'. Type 'q' to quit the game.");

                        // Send initial request to start the image revealing
                        Request revealRequest = Request.newBuilder()
                                .setOperationType(Request.OperationType.NEW)
                                .build();
                        revealRequest.writeDelimitedTo(out);
                        while (true) {
                            // Read the server's response (containing the updated image)
                            Response revealResponse = Response.parseDelimitedFrom(in);
                            System.out.println(revealResponse.getImage());
                        
                            // Check if the game is completed
                            if (revealResponse.getResponseType() == Response.ResponseType.TASK &&
                                    revealResponse.getTask().equals("Game completed!")) {
                                break; // Exit the loop if the game is completed
                            }
                        
                            // Prompt user to guess the ASCII image
                            System.out.print("Your guess: ");
                            String userGuess = scanner.nextLine();
                        
                            // Send the guess to the server
                            Request guessRequest = Request.newBuilder()
                                    .setOperationType(Request.OperationType.ANSWER)
                                    .setAnswer(userGuess)
                                    .build();
                            guessRequest.writeDelimitedTo(out);
                        
                            // Read the server's response (containing the result of the guess)
                            Response guessResponse = Response.parseDelimitedFrom(in);
                            System.out.println(guessResponse.getTask());
                        
                            // Thread.sleep(1000);
                        }

                    case 3:
                        System.out.println("Exiting the game. Goodbye!");
                        scanner.close();
                        System.exit(0);
                    default:
                        System.out.println("Invalid choice. Please enter a valid option.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
            if (serverSock != null) serverSock.close();
        }
    }
}
