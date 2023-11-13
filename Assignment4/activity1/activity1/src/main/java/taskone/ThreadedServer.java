package taskone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ThreadedServer {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ThreadedServer <port>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        StringList strings = new StringList();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ThreadedServer listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Runnable clientHandler = new ClientHandler(clientSocket, strings);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
