package client;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import leader.Leader;  
import node.Node;

public class Client {
    private Leader leader;

    public Client() {
    }

    public void sendRequest(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
             BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            while (true) {
                System.out.println("Enter a string:");
                String str = reader.readLine();
                System.out.println("Enter a character:");
                String charStr = reader.readLine();
                char ch = charStr.charAt(0);
                writer.println(str);
                writer.println(ch);
                String response = serverReader.readLine();
                System.out.println("Response from server: " + response);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.sendRequest("localhost", 8888);  // Connect to the server on localhost:8888
    }
}