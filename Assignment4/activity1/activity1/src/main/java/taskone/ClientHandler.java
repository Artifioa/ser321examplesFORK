package taskone;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket clientSocket;
    private StringList strings;

    public ClientHandler(Socket clientSocket, StringList strings) {
        this.clientSocket = clientSocket;
        this.strings = strings;
    }

    @Override
    public void run() {
        try {
            System.out.println("Accepted a request from a client...");

            Performer performer = new Performer(clientSocket, strings);
            performer.doPerform();

            System.out.println("Closed socket of the client.");
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
