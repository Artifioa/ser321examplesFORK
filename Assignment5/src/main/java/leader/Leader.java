package leader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import node.Node;

public class Leader {
    private List<Socket> nodes;
    private ExecutorService executor;

    public Leader() {
        this.nodes = new ArrayList<>();
        this.executor = Executors.newFixedThreadPool(8); // handle up to 8 nodes
    }

    public void addNode(Socket node) {
        if (nodes.size() < 8) {
            nodes.add(node);
        } else {
            System.out.println("Cannot add more nodes. Maximum limit reached.");
        }
    }

    public void startServer(int nodePort, int clientPort) throws IOException {
        ExecutorService serverExecutor = Executors.newFixedThreadPool(8);
        serverExecutor.execute(() -> {
            try {
                listenForNodes(nodePort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverExecutor.execute(() -> {
            try {
                listenForClients(clientPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
    
    private void listenForNodes(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            Socket socket = serverSocket.accept();
            addNode(socket);
            System.out.println("Node has connected.");
        }
    }
    
    private void listenForClients(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Client has connected.");
            executor.execute(() -> {
                try {
                    handleClient(socket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleClient(Socket socket) throws IOException {
        receiveRequest(socket);
    }

    public void receiveRequest(Socket clientSocket) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
            while (true) {
                if (reader.ready()) {
                    if (nodes.size() < 3) {
                        // Send an error message to the client
                        PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                        clientWriter.println("Error: Counting requires at least 3 nodes in the network.");
                        return;
                    }
                    String str = reader.readLine();
                    char ch = reader.readLine().charAt(0);
                    while (true){
                    int partSize = str.length() / nodes.size();
                    ArrayList<String> parts = new ArrayList<>();
                    List<Future<Integer>> futures = new ArrayList<>();
                    for (int i = 0; i < nodes.size(); i++) {
                        int start = i * partSize;
                        int end = i == nodes.size() - 1 ? str.length() : start + partSize;
                        String part = str.substring(start, end);
                        parts.add(part);
                        final int index = i;
                        Socket nodeSocket = nodes.get(index);
                        PrintWriter nodeWriter = new PrintWriter(nodeSocket.getOutputStream(), true);
                        BufferedReader nodeReader = new BufferedReader(new InputStreamReader(nodeSocket.getInputStream()));
                        Future<Integer> future = executor.submit(() -> {
                            try {
                                nodeWriter.println("count");
                                nodeWriter.println(part);
                                nodeWriter.println(ch);
                                return Integer.parseInt(nodeReader.readLine());
                            } catch (IOException e) {
                                e.printStackTrace();
                                return 0;
                            }
                        });
                        futures.add(future);
                    }
                    List<Integer> results = new ArrayList<>();
                    for (Future<Integer> future : futures) {
                        try {
                            results.add(future.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    // Shift the results and string parts to the next node for verification
                    System.out.println("Sending string part " + parts);
                    List<Future<String>> verificationFutures = new ArrayList<>();
                    for (int i = 0; i < nodes.size(); i++) {
                        final int index = i;
                        System.out.println("Sending 2 string part " + parts);
                        Socket nextNodeSocket = nodes.get((index + 1) % nodes.size());
                        PrintWriter nextNodeWriter = new PrintWriter(nextNodeSocket.getOutputStream(), true);
                        BufferedReader nextNodeReader = new BufferedReader(new InputStreamReader(nextNodeSocket.getInputStream()));
                        Future<String> future = executor.submit(() -> {
                            try {
                                nextNodeWriter.println("verify");  // Send the "verify" signal
                                System.out.println("Sending 3 string part " + parts);
                                nextNodeWriter.println(parts.get(index));
                                nextNodeWriter.println(ch);
                                nextNodeWriter.println(results.get(index));
                                return nextNodeReader.readLine();
                            } catch (IOException e) {
                                e.printStackTrace();
                                return "no";
                            }
                        });
                        verificationFutures.add(future);
                    }

                    List<String> verificationResults = new ArrayList<>();
                    for (Future<String> future : verificationFutures) {
                        try {
                            verificationResults.add(future.get());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (verificationResults.stream().allMatch(result -> result.equals("yes"))) {
                        int totalCount = results.stream().mapToInt(Integer::intValue).sum();
                        writer.println(totalCount);
                        break;
                    } else {
                        for (String result : verificationResults) {
                            System.out.println(result);
                        }
                    
                        if (verificationResults.contains("no")) {
                            // Send a test string and character to all nodes
                            System.out.println("Sending test string and character to all nodes");
                            String testStr = "test string";
                            char testCh = 't';
                            int correctCount = 3;  // The correct count of 't' in "test string"
                            Iterator<Socket> iterator = nodes.iterator();
                            while (iterator.hasNext()) {
                                Socket faultyNodeSocket = iterator.next();
                                PrintWriter faultyNodeWriter = new PrintWriter(faultyNodeSocket.getOutputStream(), true);
                                BufferedReader faultyNodeReader = new BufferedReader(new InputStreamReader(faultyNodeSocket.getInputStream()));
                                try {
                                    faultyNodeWriter.println("count");
                                    faultyNodeWriter.println(testStr);
                                    faultyNodeWriter.println(testCh);
                        
                                    // Compare the count returned by the node with the known correct count
                                    int count = Integer.parseInt(faultyNodeReader.readLine());
                        
                                    if (count != correctCount) {
                                        System.out.println("Faulty node detected. Removing from the list.");
                                        iterator.remove();  // Remove the faulty node from the list
                                    }
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                }
                    }
                }
            }
        }
    }

    /* 
    public void census(Socket clientSocket, List<Integer> results) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
         PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)) {
        System.out.println("Results: " + results);  // Print the received string part
        int totalCount = results.stream().mapToInt(Integer::intValue).sum();
        writer.println(totalCount);
        }
    }*/

    public static void main(String[] args) throws IOException {
        Leader leader = new Leader();
        leader.startServer(8887, 8888);  // Start the server on port 8887 for nodes and 8888 for clients
    }
}