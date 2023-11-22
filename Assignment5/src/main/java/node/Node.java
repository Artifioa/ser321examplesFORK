package node;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Node {
    private boolean isFaulty;

    public Node() {
        String pFault = System.getProperty("Fault");
        isFaulty = "1".equals(pFault);
    }

    public int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (Character.toLowerCase(str.charAt(i)) == Character.toLowerCase(ch)) {
                count++;
            }
        }
        // If the node is faulty, add 1 to the count to make it incorrect
        return isFaulty ? count + 1 : count;
    }
    
    public boolean verifyCount(String str, char ch, int count) {
        // If the node is faulty, always return false to make the verification fail
        return isFaulty ? false : countOccurrences(str, ch) == count;
    }

    public void connectToLeader(String host, int port) throws IOException {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {
            if (isFaulty) {
                System.out.println("Faulty");
            }
            String task;
            while ((task = reader.readLine()) != null) {
                if ("exit".equals(task)) {
                    // Exit the loop when the leader signals the end of communication
                    break;
                }
    
                String str = reader.readLine();
                if (str == null) {
                    break;
                }
                char ch = reader.readLine().charAt(0);
                if ("count".equals(task)) {
                    // Perform the counting task
                    int count = countOccurrences(str, ch);
                    System.out.println("String: " + str + ", char: " + ch);  // Print the verification result
                    writer.println(count);
                    System.out.println("Sent count: " + count);  // Print the sent count
                } else if ("verify".equals(task)) {
                    // Perform the verification task
                    int countveri = Integer.parseInt(reader.readLine());
                    boolean isCorrect = verifyCount(str, ch, countveri);
                    System.out.println("String: " + str + ", char: " + ch + ", count: " + countveri);  // Print the verification result recieved from the leader
                    writer.println(isCorrect ? "yes" : "no");
                    System.out.println("Sent verification result: " + (isCorrect ? "yes" : "no"));  // Print the sent verification result
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Node node = new Node();
        node.connectToLeader("localhost", 8887);  // Connect to the leader on localhost:8888
    }
}




