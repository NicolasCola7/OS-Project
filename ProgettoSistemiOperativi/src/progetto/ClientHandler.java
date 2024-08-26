package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class ClientHandler implements Runnable, ResourceListener {

    private Socket s;
    public static Resource topics = new Resource();
    private boolean subscriberActive = false; // Variabile di stato per il subscriber
    private String subscriberTopic;
    private HashMap<String, Semaphore> semaphores; 

    public ClientHandler(Socket s, HashMap<String, Semaphore> semaphores) {
        this.s = s;
        this.semaphores = semaphores;
        topics.addListener(this); // Registrazione come listener
    }

    @Override
    public void run() {
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);

            System.out.println("Thread " + Thread.currentThread() + " listening...");

            boolean closed = false;
            while (!closed) {
                String request = from.nextLine();
                if (!Thread.interrupted()) {
                    System.out.println(request);
                    String[] parts = request.trim().split(" ");
                    switch (parts[0]) {
                        case "quit":
                            closed = true;
                            break;
                        
                        case "publish":
                            if (parts.length > 1) {
                                String topic = parts[1];
                                if (!topics.containsTopic(parts[1])) {
                                    topics.add(topic);
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic '" + topic + "' non precedentemente esistente è stato creato");
                                    semaphores.put(topic, new Semaphore(1)); // Aggiungi un semaforo binario per il topic
                                    gestisciPublisher(topic);
                                } else {
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic '" + topic + "' precedentemente esistente");
                                    gestisciPublisher(topic);
                                }
                            }
                            break;
                        
                        case "show":
                            String allTopics = topics.show();
                            to.println(allTopics);
                            break;
                        
                        case "subscribe":
                            if (parts.length > 1) {
                                String topic = parts[1];
                                if (topics.containsTopic(parts[1])) {
                                    to.println("Accesso come Subscriber avvenuto con successo al topic " + topic);
                                    subscriberTopic = topic;
                                    gestisciSubscriber(topic);
                                } else {
                                    to.println("Accesso come Subscriber fallito, il topic " + topic + " non esiste");
                                }
                            }
                            break;
                        
                        default:
                            to.println("Unknown cmd");
                    }
                } else {
                    to.println("quit");
                    break;
                }
            }

            to.println("quit");
            s.close();
            System.out.println("Closed");
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void gestisciPublisher(String topic) throws InterruptedException {
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);
            ArrayList<Message> currentClientMessages = new ArrayList<Message>();
            System.out.println("Thread " + Thread.currentThread() + " listening...");

            boolean closed = false;
            while (!closed) {
                String request = from.nextLine();
                if (!Thread.interrupted()) {
                    System.out.println(request);
                    String[] parts = request.trim().split(" ");
                    options:
                    switch (parts[0]) {
                        case "quit":
                            closed = true;
                            break;
                            
                        case "send":
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            }

                            if (parts.length > 1) {
                                semaphores.get(topic).acquire(); // Acquisisci il semaforo
                                try {
                                    String message = "";
                                    for (int i = 1; i < parts.length; i++) {
                                        message += parts[i] + " ";
                                    }
                                    
                                    Message messageFinal = new Message(0, message.trim());
                                    topics.addMessageToTopic(topic, messageFinal);
                                    currentClientMessages.add(messageFinal);
                                    to.println("Messaggio inviato con successo");
                                    break options;
                                } finally {
                                    semaphores.get(topic).release(); // Rilascia il semaforo
                                }
                            } else {
                                to.println("Messaggio vuoto");
                            }
                            break;
                            
                        case "list":
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            } else {
                                semaphores.get(topic).acquire(); // Acquisisci il semaforo
                                try {
                                    String allMessages = topics.list(currentClientMessages);
                                    to.println(allMessages);
                                } finally {
                                    semaphores.get(topic).release(); // Rilascia il semaforo
                                }
                            }
                            break;
                            
                        case "delete":
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            }

                            if (parts.length > 1) {
                                semaphores.get(topic).acquire(); // Acquisisci il semaforo
                                try {
                                    int id;
                                    try {
                                        id = Integer.parseInt(parts[1]);
                                    } catch (NumberFormatException e) {
                                        to.println("Id non valido");
                                        break options;
                                    }
                                    
                                    int result = topics.remove(topic, id);
                                    if (result == 1) {
                                        to.println("Messaggio eliminato con successo");
                                    } else if (result == 2) {
                                        to.println("Id non esistente");
                                    } else {
                                        to.println("Nessun messaggio presente nel topic " + topic);
                                    }
                                } finally {
                                    semaphores.get(topic).release(); // Rilascia il semaforo
                                }
                            } else {
                                to.println("Id non inserito");
                            }
                            break;
                        
                        default:
                            to.println("Unknown cmd");
                    }
                } else {
                    to.println("quit");
                    break;
                }
            }

            to.println("quit");
            s.close();
            System.out.println("Closed");
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        }
    }

    public void gestisciSubscriber(String topic) throws InterruptedException {
        subscriberActive = true; // Imposta lo stato del subscriber su attivo
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);
            System.out.println("Thread " + Thread.currentThread() + " listening...");

            boolean closed = false;
            while (!closed && subscriberActive) {
                String request = from.nextLine();
                if (!Thread.interrupted()) {
                    System.out.println(request);
                    String[] parts = request.trim().split(" ");
                    options:
                    switch (parts[0]) {
                        case "quit":
                            closed = true;
                            subscriberActive = false; // Imposta lo stato del subscriber su inattivo
                            break;

                        case "list":
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            } else {
                                semaphores.get(topic).acquire(); // Acquisisci il semaforo
                                try {
                                    String allMessages = topics.listAll(topic);
                                    to.println(allMessages);
                                } finally {
                                    semaphores.get(topic).release(); // Rilascia il semaforo
                                }
                            }
                            break;
                        
                        default:
                            to.println("Unknown cmd");
                    }
                } else {
                    to.println("quit");
                    break;
                }
            }

            to.println("quit");
            s.close();
            System.out.println("Closed");
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onValueAdded(String topic, Message msg) {
        if (subscriberActive && topic.equals(subscriberTopic)) {
            try {
                PrintWriter to = new PrintWriter(s.getOutputStream(), true);
                to.println("Nuovo messaggio sul topic '" + topic + "': " + msg.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
