package taskone;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolServer {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: gradle runThreadPoolServer -PmaxConnections=5 -Pport=8888 -q --console=plain");
            System.exit(1);
        }

        int maxConnections = Integer.parseInt(args[0]);
        int port = Integer.parseInt(args[1]);

        StringList strings = new StringList();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("ThreadPoolServer listening on port " + port);

            // Create a thread pool with a specified number of threads
            ExecutorService threadPool = Executors.newFixedThreadPool(maxConnections);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                Runnable clientHandler = new ClientHandler(clientSocket, strings);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
