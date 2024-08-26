package progetto;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class SocketListener implements Runnable {
    private ServerSocket server;
    private ArrayList<Thread> children = new ArrayList<>();
    private HashMap<String, Semaphore> semaphores; // Mappa per gestire i semafori per i topic

    public SocketListener(ServerSocket server, HashMap<String, Semaphore> semaphores) {
        this.server = server;
        this.semaphores = semaphores; // Inizializzazione dei semafori
    }

    @Override
    public void run() {
        try {
            this.server.setSoTimeout(5000); // Timeout per la connessione
            System.out.println("Waiting for a new client...");
            while (!Thread.interrupted()) {
                try {
                    Socket s = this.server.accept();
                    if (!Thread.interrupted()) {
                        System.out.println("Client connected");

                        // Crea un nuovo thread per gestire il client connesso
                        Thread handlerThread = new Thread(new ClientHandler(s, semaphores));
                        handlerThread.start();
                        this.children.add(handlerThread);
                    } else {
                        s.close();
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    continue;
                } catch (IOException e) {
                    break;
                }
            }
            this.server.close();
        } catch (IOException e) {
            System.err.println("SocketListener: IOException caught: " + e);
            e.printStackTrace();
        }

        System.out.println("Interrupting children...");
        for (Thread child : this.children) {
            System.out.println("Interrupting " + child + "...");
            child.interrupt();
        }
    }
}
