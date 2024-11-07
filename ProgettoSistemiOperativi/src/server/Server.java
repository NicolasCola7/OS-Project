package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {
	
    private static final HashMap<String, ReentrantReadWriteLock> semaphores = new HashMap<>(); // Semaforo binario per gestire l'accesso ai topic
    private static  Resource topics = new Resource();
    protected static ReentrantLock inspectLock = new ReentrantLock();
    
    private static void manageInspect(String topic, Scanner from) {
        boolean closed = false;
        while (!closed) {
            String request = from.nextLine();
            String[] parts = request.trim().split(" ");
            if (parts.length == 0) {
                System.out.println("Error: No command received");
                continue;
            }
            String command = parts[0];
            switch (command) {
                
            	case "end":
                    closed = true;
                    break;
                    
                case "listall":
					String allMessage = topics.listAll(topic);
					System.out.println(allMessage);
                    break;
                    
                case "delete":
                    if (parts.length > 1) {
                        try {
                            int id = Integer.parseInt(parts[1].trim());
                            int delete = topics.remove(topic, id);
                            if (delete == 1) {
                                System.out.println("Messaggio eliminato con successo");
                            } else if (delete == 2) {
                                System.out.println("ID messaggio non esistente o non trovato");
                            } else {
                                System.out.println("Non sono presenti messaggi sul topic da poter eliminare");
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Error: ID must be a number");
                        }
                    } else {
                        System.out.println("Error: delete requires an id argument");
                    }
                    break;
                    
                default:
                    System.out.println("Unknown cmd: " + command); // Debug
            }
        }
        System.out.println("Ending Inspect session for key: " + topic);
    }

    public static void main(String[] args) {
       
        if (args.length < 1) {
            System.err.println("Usage: java Server <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        Scanner userInput = new Scanner(System.in);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            /*
             * Deleghiamo a un altro thread la gestione di tutte le connessioni client;
             * Nel thread principale gestiamo la console del server
             */
           
            Thread serverThread = new Thread(new SocketListener(serverSocket, semaphores, topics));
            serverThread.start();

            String input = "";

            while (!input.equals("quit")) {
                input = userInput.nextLine();
                String[] parts = input.split(" ");

                switch (parts[0]) {
                    case "show": {
                        String allTopic = topics.show();
                        System.out.println(allTopic);
                        break;
                    }

                    case "inspect": {
                        if (parts.length > 1 && topics.containsTopic(parts[1])) {
                            String topic = parts[1];
                            ReentrantReadWriteLock semaphore = semaphores.get(topic);

                            try {                           	
                                semaphore.writeLock().lock();  // Acquisisce il semaforo per bloccare i client
                                inspectLock.lock();           //Lock che funge da flag per verificare se l'ispezione è attiva
                                manageInspect(topic, userInput);  // Funzione che ispeziona il topic
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                            	inspectLock.unlock();
                                semaphore.writeLock().unlock();  // Rilascia il semaforo                              
                            }
                            break;
                        } else if (parts.length > 1 && !topics.containsTopic(parts[1])) {
                            System.out.println("Topic non esistente");
                            break;
                        } else {
                            System.out.println("Necessario specificare il topic da ispezionare");
                            break;
                        }
                    }

                    default:
                        System.out.println("Unknown cmd");
                }
            }

            try {
                // Rimaniamo in attesa che sender e receiver terminino la loro esecuzione
                serverThread.interrupt();
                /* attendi la terminazione del thread */
                serverThread.join();
                System.out.println("Local socket closed.");
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
