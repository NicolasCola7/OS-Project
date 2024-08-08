package progetto;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) {
        args = new String[1];
        args[0] = "9000";
        if (args.length < 1) {
            System.err.println("Usage: java Server <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            /*
             * Deleghiamo a un altro thread la gestione di tutte le connessioni client;
             * Nel thread principale gestiamo la console del server
             */
            Thread serverListenerThread = new Thread(new SocketListener(serverSocket));
            serverListenerThread.start();

            // Socket locale per comunicazione con ServerHandler
            Socket localSocket = new Socket("localhost", port);
            System.out.println("Local connection established for server commands.");

            Thread sender = new Thread(new Sender(localSocket));
            Thread receiver = new Thread(new Receiver(localSocket, sender));

            sender.start();
            receiver.start();

            try {
                // Rimaniamo in attesa che sender e receiver terminino la loro esecuzione
                sender.join();
                receiver.join();
                localSocket.close();
                System.out.println("Local socket closed.");
            } catch (InterruptedException e) {
                return;
            }

            try {
                serverListenerThread.interrupt();
                serverListenerThread.join();
            } catch (InterruptedException e) {
                return;
            }

            System.out.println("Server main thread terminated.");
        } catch (IOException e) {
            System.err.println("IOException caught: " + e);
            e.printStackTrace();
        }
    }
}
