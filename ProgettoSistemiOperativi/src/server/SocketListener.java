package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
/**
 * Thred per la gestione di tutte le connessioni client
 */
public class SocketListener implements Runnable {
    private ServerSocket server;
    private ArrayList<Thread> children = new ArrayList<>();
    private HashMap<String, ReentrantReadWriteLock> semaphores; // Mappa per gestire i semafori per i topic
    private Resource sharedResource;
    private HashMap<String, Boolean> inspectLocks;

    public SocketListener(ServerSocket server, HashMap<String, ReentrantReadWriteLock> semaphores, Resource resource, HashMap<String, Boolean> inspectLocks) {
        this.server = server;
        this.semaphores = semaphores; // Inizializzazione dei semafori
        this.sharedResource = resource;
        this.inspectLocks = inspectLocks;
    }

    @Override
    public void run() {
        try {
            this.server.setSoTimeout(5000); // Timeout per la connessione
            System.out.println("Waiting for a new client...");
            while (!Thread.interrupted()) {
                try {
                    Socket socket = this.server.accept();
                    if (!Thread.interrupted()) {
                        System.out.println("Client connected");

                        // Crea un nuovo thread per gestire il client connesso
                        Thread handlerThread = new Thread(new ClientHandler(socket, semaphores, sharedResource, inspectLocks));
                        handlerThread.start();
                        this.children.add(handlerThread);
                    } else {
                        socket.close();
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
