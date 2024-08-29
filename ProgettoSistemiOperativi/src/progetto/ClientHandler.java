package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

public class ClientHandler extends Thread implements Runnable, ResourceListener {

    private Socket s;
    public  Resource topics;
    private boolean subscriberActive = false; // Variabile di stato per il subscriber
    private String subscriberTopic;
    private HashMap<String, Semaphore> semaphores; 

    public ClientHandler(Socket s, HashMap<String, Semaphore> semaphores, Resource resource) {
        this.s = s;
        this.semaphores = semaphores;
        this.topics = resource;
       
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
                       
                        case "quit":{
                            closed = true;
                            break;
                        }
                        
                        case "publish":{
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
                        }
                        
                        case "show":{
                            String allTopics = topics.show();
                            to.println(allTopics);
                            break;
                        }
                        
                        case "subscribe":{
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
                        }
                        
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
                            
                        case "send": {
                            // Creazione del thread figlio
                            Thread childThread = new Thread(() -> {
                                // Codice da eseguire nel thread figlio
                            	if (semaphores.get(topic).availablePermits() == 0) {
                                    to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                                }

                                if (parts.length > 1) {
                                    try {
										semaphores.get(topic).acquire();
									} catch (InterruptedException e) {										
										e.printStackTrace();
									} // Acquisisci il semaforo
                                    try {
                                        String message = "";
                                        for (int i = 1; i < parts.length; i++) {
                                            message += parts[i] + " ";
                                        }

                                        Message messageFinal = new Message(topics.getPuntatoreByTopic(topic), message);
                                        topics.addMessageToTopic(topic, messageFinal);
                                        currentClientMessages.add(messageFinal);
                                        to.println("Messaggio inviato con successo sul topic");
                                    } finally {
                                        semaphores.get(topic).release(); // Rilascia il semaforo
                                    }
                                }                               
                               
                            });
                            childThread.start(); 
                            break options;                         
                            
                        }

                        
                        case "list":{
                        	 Thread childThread = new Thread(() -> {
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            }

                            try {
								semaphores.get(topic).acquire();
							} catch (InterruptedException e) {								
								e.printStackTrace();
							} // Acquisisci il semaforo
                            try {
                                String message = topics.list(currentClientMessages);
                                to.println(message.trim());
                            } finally {
                                semaphores.get(topic).release(); // Rilascia il semaforo
                            }
                        	 });
                        	 childThread.start();
                            break options;
                        }
                            
                        case "listall":{
                        	 Thread childThread = new Thread(() -> {
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            }
                            
                            try {
                            	semaphores.get(topic).acquire(); // Acquisisci il semaforo
                                String message = topics.listAll(topic);
                                to.println(message.trim());
                            } catch (InterruptedException e) {								
								e.printStackTrace();
							} finally {
                                semaphores.get(topic).release(); // Rilascia il semaforo
                            }
                        	 });
                        	 childThread.start();
                            break options;
                        }
                        
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
        subscriberActive = true; // Imposta lo stato attivo per il subscriber
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);

            System.out.println("Thread " + Thread.currentThread() + " listening...");

            boolean closed = false;
            while (!closed) {
                String request = from.nextLine();
                if (!Thread.interrupted()) {
                    String[] parts = request.trim().split(" ");
                    options:
                    switch (parts[0]) {
                    
                        case "quit":
                            closed = true;
                            break;
                            
                        case "listall":
                        	Thread childThread = new Thread(() -> {
                            if (semaphores.get(topic).availablePermits() == 0) {
                                to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                            }
                            
                            try {
                            	semaphores.get(topic).acquire(); // Acquisisci il semaforo
                                to.println(topics.listAll(topic));
                            } catch (InterruptedException e) {								
								e.printStackTrace();
							} finally {
                                semaphores.get(topic).release(); // Rilascia il semaforo
                            }
                        	});
                        	childThread.start();
                            break options;
                            
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
        } finally {
            subscriberActive = false; // Imposta lo stato come inattivo per il subscriber
        }
    }

    @Override
    public void onValueAdded(String key, Message value) {
        if (subscriberActive && key.equals(subscriberTopic)) {
            try {
                PrintWriter to = new PrintWriter(s.getOutputStream(), true);
                to.println("Nuovo messaggio sul topic " + key + ":\n" + value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
