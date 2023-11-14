package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.*;

import proto.RequestProtos.*;
import proto.ResponseProtos.*;

class SockBaseServer {
    static String logFilename = "logs.txt";
    private List<Leader> leaderboard = new ArrayList<>();
    ServerSocket socket = null;
    InputStream in = null;
    OutputStream out = null;
    Socket clientSocket = null;
    int port = 9099; // default port
    Game game;
    private static final String LEADERBOARD_FILE = "leaderboard.txt";

    public SockBaseServer(Socket sock, Game game){
        this.clientSocket = sock;
        this.game = game;
        loadLeaderboardFromFile();
        try {
            in = clientSocket.getInputStream();
            out = clientSocket.getOutputStream();
        } catch (Exception e){
            System.out.println("Error in constructor: " + e);
        }
    }

    // Handles the communication right now it just accepts one input and then is done you should make sure the server stays open
    // can handle multiple requests and does not crash when the server crashes
    // you can use this server as based or start a new one if you prefer. 
    public void start() throws IOException {
        String name = "";

        System.out.println("Ready...");
        
        boolean isRunning = true;
        while (isRunning) {
            try {
                // read the proto object and put into new objct
                Request op = Request.parseDelimitedFrom(in);
                String result = null;

                // if the operation is NAME (so the beginning then say there is a commention and greet the client)
                if (op.getOperationType() == Request.OperationType.NAME) {
                    // get name from proto object
                    name = op.getName();

                    // writing a connect message to the log with name and CONNENCT
                    writeToLog(name, Message.CONNECT);
                    System.out.println("Got a connection and a name: " + name);
                    Response response = Response.newBuilder()
                            .setResponseType(Response.ResponseType.HELLO)
                            .setHello("Hello " + name + " and welcome. \nWhat would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - to quit")
                            .build();
                    updateLeaderboard(name, false);
                    response.writeDelimitedTo(out);
                }
                if (op.getOperationType() == Request.OperationType.NEW) {
                    // start a new game
                    game.newGame();

                    // gradually reveal the image
                    while (game.getIdx() < game.getIdxMax()) {
                        // Replace 10 characters in the image
                        System.out.println("LOOP STARTED");
                        replace10percent();
                        
                        System.out.println(game.getIdx());
                        System.out.println(game.getIdxMax());

                        // Send the updated image to the client
                        Response revealResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setImage(game.getImage())
                                .setTask("Revealing image...")
                                .build();
                        revealResponse.writeDelimitedTo(out);
                        out.flush();

                        // Add a delay before revealing the next part of the image
                        Thread.sleep(1000);

                        // Receive the user's guess from the client
                        Request guessRequest = Request.parseDelimitedFrom(in);
                        String userGuess = guessRequest.getAnswer();

                        // Check if the game is completed
                        if (game.getIdx() == game.getIdxMax()) {
                            Response mainMenuResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.BYE)
                                .setTask("What would you like to do? \n 1 - to see the leader board \n 2 - to enter a game \n 3 - to quit")
                                .build();
                            mainMenuResponse.writeDelimitedTo(out);
                            updateLeaderboard(name, true);
                            out.flush();
                        }  
                        System.out.println("LOOP FINISHED");
                    }
                }
                else if (op.getOperationType() == Request.OperationType.LEADERBOARD) {
                    Response.Builder responseBuilder = Response.newBuilder()
                            .setResponseType(Response.ResponseType.LEADERBOARD);

                    if (leaderboard.isEmpty()) {
                        responseBuilder.setMessage("Leaderboard is empty.");
                    } else {
                        List<Leader> sortedLeaderboard = new ArrayList<>(leaderboard);
                        sortedLeaderboard.sort((leader1, leader2) -> leader2.getWins() - leader1.getWins());

                        for (Leader leader : sortedLeaderboard) {
                            responseBuilder.addLeaderboard(leader);
                        }
                    }

                    Response response = responseBuilder.build();
                    response.writeDelimitedTo(out);
                }

                // Example how to start a new game and how to build a response with the image which you could then send to the server
                // LINE 67-108 are just an example for Protobuf and how to work with the differnt types. They DO NOT
                // belong into this code. 
                /* 
                game.newGame(); // starting a new game
                // read the response from the server
                Response response = Response.parseDelimitedFrom(in);
                // print the server response.
                System.out.println(response.getTask());
                System.out.println(response);

                */

                /* 
                // Gradually reveal the image
                while (game.getIdx() < game.getIdxMax()) {
                    // Replace 10 characters in the image
                    System.out.println("LOOP STARTED");
                    replace(10);
                    
                    System.out.println(game.getIdx());
                    System.out.println(game.getIdxMax());

                    // Send the updated image to the client
                    Response revealResponse = Response.newBuilder()
                            .setResponseType(Response.ResponseType.TASK)
                            .setImage(game.getImage())
                            .setTask("Revealing image...")
                            .build();
                    revealResponse.writeDelimitedTo(out);
                    out.flush();

                    // Add a delay before revealing the next part of the image
                    Thread.sleep(1000);

                    // Receive the user's guess from the client
                    Request guessRequest = Request.parseDelimitedFrom(in);
                    String userGuess = guessRequest.getAnswer();

                    // Check if the game is completed
                    if (game.getIdx() == game.getIdxMax()) {
                        game.newGame();

                        // Notify the client that the image revealing is complete and a new game is starting
                        Response newGameResponse = Response.newBuilder()
                                .setResponseType(Response.ResponseType.TASK)
                                .setImage(game.getImage())
                                .setTask("New game starting!")
                                .build();
                        newGameResponse.writeDelimitedTo(out);
                        out.flush();
                    }  
                    System.out.println("LOOP FINISHED");
                }
                */

            } catch (Exception ex) {
                ex.printStackTrace();
                isRunning = false; // stop the loop if an exception occurs
            }
        }

        if (out != null)  out.close();
        if (in != null)   in.close();
        if (clientSocket != null) clientSocket.close();
    }

    /**
     * Replaces num characters in the image. I used it to turn more than one x when the task is fulfilled
     * @param num -- number of x to be turned
     * @return String of the new hidden image
     */
    public String replace(int num){
        for (int i = 0; i < num; i++){
            if (game.getIdx()< game.getIdxMax())
                game.replaceOneCharacter();
        }
        return game.getImage();
    }
    
    /**
     * Replaces 10% of the characters in the image
     * @return String of the new hidden image
     */
    public void replace10percent(){
        if (Math.round(game.getIdxMax() / 10) < (game.getIdxMax() - game.getIdx())){
            replace(Math.round(game.getIdxMax() / 10));
        }
        else{
            replace(game.getIdxMax() - game.getIdx());
        }
    }

    /**
     * Method to update the leaderboard
     * @param playerName - Name of the player
     * @param isWin - Boolean to indicate if the player won the game
     */
    private void updateLeaderboard(String playerName, boolean isWin) {
        // Search for the player in the leaderboard
        Leader player = null;
        int playerIndex = -1;
        for (int i = 0; i < leaderboard.size(); i++) {
            Leader leader = leaderboard.get(i);
            if (leader.getName().equals(playerName)) {
                player = leader;
                playerIndex = i;
                break;
            }
        }

        if (player == null) {
            // If the player is not in the leaderboard, create a new entry
            int wins = 0;
            int logins = 1;
            player = Leader.newBuilder()
                    .setName(playerName)
                    .setWins(wins)
                    .setLogins(logins)
                    .build();
            leaderboard.add(player);
        } else {
            // Increment the login count
            int logins = player.getLogins() + 1;
            Leader updatedPlayer = Leader.newBuilder()
                    .mergeFrom(player)
                    .setLogins(logins)
                    .build();
            leaderboard.set(playerIndex, updatedPlayer);

            // Update the wins count
            if (isWin) {
                int wins = player.getWins() + 1;
                updatedPlayer = Leader.newBuilder()
                        .mergeFrom(updatedPlayer)
                        .setWins(wins)
                        .build();
                leaderboard.set(playerIndex, updatedPlayer);
            }
        }

        // Save the updated leaderboard to the file
        saveLeaderboardToFile();
    }


    /**
     * Loading the leaderboard from the file
     */    
    private void loadLeaderboardFromFile() {
        File leaderboardFile = new File(LEADERBOARD_FILE);
        if (leaderboardFile.exists()) {
            try (FileInputStream fileInputStream = new FileInputStream(leaderboardFile)) {
                leaderboard.clear();

                while (true) {
                    Leader leader = Leader.parseDelimitedFrom(fileInputStream);
                    if (leader == null) {
                        break;
                    }
                    leaderboard.add(leader);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Saving the leaderboard to the file
     */
    private void saveLeaderboardToFile() {
        try (FileOutputStream fileOutputStream = new FileOutputStream(LEADERBOARD_FILE)) {
            for (Leader leader : leaderboard) {
                leader.writeDelimitedTo(fileOutputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writing a new entry to our log
     * @param name - Name of the person logging in
     * @param message - type Message from Protobuf which is the message to be written in the log (e.g. Connect) 
     * @return String of the new hidden image
     */
    public static void writeToLog(String name, Message message){
        try {
            // read old log file 
            Logs.Builder logs = readLogFile();

            // get current time and data
            Date date = java.util.Calendar.getInstance().getTime();

            // we are writing a new log entry to our log
            // add a new log entry to the log list of the Protobuf object
            logs.addLog(date.toString() + ": " +  name + " - " + message);

            // open log file
            FileOutputStream output = new FileOutputStream(logFilename);
            Logs logsObj = logs.build();

            // This is only to show how you can iterate through a Logs object which is a protobuf object
            // which has a repeated field "log"

            for (String log: logsObj.getLogList()){

                System.out.println(log);
            }

            // write to log file
            logsObj.writeTo(output);
        }catch(Exception e){
            System.out.println("Issue while trying to save");
        }
    }

    /**
     * Reading the current log file
     * @return Logs.Builder a builder of a logs entry from protobuf
     */
    public static Logs.Builder readLogFile() throws Exception{
        Logs.Builder logs = Logs.newBuilder();

        try {
            // just read the file and put what is in it into the logs object
            return logs.mergeFrom(new FileInputStream(logFilename));
        } catch (FileNotFoundException e) {
            System.out.println(logFilename + ": File not found.  Creating a new file.");
            return logs;
        }
    }


    public static void main (String args[]) throws Exception {
        Game game = new Game();

        if (args.length != 2) {
            System.out.println("Expected arguments: <port(int)> <delay(int)>");
            System.exit(1);
        }
        int port = 9099; // default port
        int sleepDelay = 10000; // default delay
        Socket clientSocket = null;
        ServerSocket socket = null;

        try {
            port = Integer.parseInt(args[0]);
            sleepDelay = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.out.println("[Port|sleepDelay] must be an integer");
            System.exit(2);
        }
        try {
            socket = new ServerSocket(port);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

        clientSocket = socket.accept();
        SockBaseServer server = new SockBaseServer(clientSocket, game);
        server.start();

    }
}









/*
class SockBaseServer {
        static String logFilename = "logs.txt";

        ServerSocket socket = null;
        InputStream in = null;
        OutputStream out = null;
        Socket clientSocket = null;
        int port = 9099; // default port
        Game game;
        HashMap<String, Integer> scores = new HashMap<String, Integer>();

        public SockBaseServer(Socket sock, Game game) {
            this.clientSocket = sock;
            this.game = game;
        }

        public void start() throws IOException {
            try {
                in = clientSocket.getInputStream();
                out = clientSocket.getOutputStream();
                Request req = null;
                while ((req = Request.parseDelimitedFrom(in)) != null) {
                    switch (req.getOperationType()) {
                        case CONNECT:
                            Connect connect = req.getConnect();
                            String name = connect.getName();
                            writeToLog(name, req);
                            if (!scores.containsKey(name)) {
                                scores.put(name, 0);
                            }
                            break;
                        case GUESS:
                            Guess guess = req.getGuess();
                            String guessStr = guess.getGuess();
                            String result = game.guess(guessStr);
                            replace10percent();
                            Response response = Response.newBuilder()
                                    .setResponseType(Response.ResponseType.GUESS)
                                    .setImage(game.getImage())
                                    .setResult(result)
                                    .build();
                            response.writeDelimitedTo(out);
                            break;
                        case LEADERBOARD:
                            getLeaderboard();
                            break;
                        default:
                            break;
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception caught when trying to listen on port "
                        + port + " or listening for a connection");
                System.out.println(e.getMessage());
            } finally {
                in.close();
                out.close();
                clientSocket.close();
                socket.close();
            }
        }

        public String replace(int num) {
            for (int i = 0; i < num; i++) {
                if (game.getIdx() < game.getIdxMax())
                    game.replaceOneCharacter();
            }
            return game.getImage();
        }

        public void replace10percent() {
            if (Math.round(game.getIdxMax() / 10) < (game.getIdxMax() - game.getIdx())) {
                replace(Math.round(game.getIdxMax() / 10));
            } else {
                replace(game.getIdxMax() - game.getIdx());
            }
        }

        public void getLeaderboard() {
            // sort the scores in descending order
            List<Map.Entry<String, Integer>> sortedScores = new ArrayList<>(scores.entrySet());
            sortedScores.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

            // create the leaderboard string
            StringBuilder leaderboardBuilder = new StringBuilder();
            for (Map.Entry<String, Integer> entry : sortedScores) {
                leaderboardBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
            String leaderboard = leaderboardBuilder.toString();

            // send the leaderboard response
            Response response = Response.newBuilder()
                    .setResponseType(Response.ResponseType.LEADERBOARD)
                    .setLeaderboard(leaderboard)
                    .build();
            try {
                response.writeDelimitedTo(out);
            } catch (IOException e) {
                System.out.println("Exception caught when trying to send leaderboard response");
                System.out.println(e.getMessage());
            }
        }
 */