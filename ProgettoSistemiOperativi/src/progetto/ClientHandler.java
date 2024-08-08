package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ClientHandler implements Runnable, ResourceListener {

    private Socket s;
    public static Resource topic = new Resource();
    private boolean subscriberActive = false; // Variabile di stato per il subscriber
    private String subscriberKey;

    public ClientHandler(Socket s) {
        this.s = s;
 
        topic.addListener(this); // Registrazione come listener
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
                    System.out.println("Request: " + request);
                    String[] parts = request.trim().split(" ");
                    switch (parts[0]) {
                        case "quit":
                            closed = true;
                            break;
                        case "publish":
                            if (parts.length > 1) {
                                String key = parts[1];
                                if (!topic.containsKey(parts[1])) {
                                    topic.add(key);
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic: " + key + " non precedentemente esistente è stato creato");
                                    gestisciPublisher(key);
                                } else {
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic: " + key + " precedentemente esistente");
                                    gestisciPublisher(key);
                                }
                            }
                            break;
                        case "show":
                            String allKey = topic.getAllKey();
                            to.println(allKey);
                            break;
                        case "subscribe":
                            if (parts.length > 1) {
                                String key = parts[1];
                                if (topic.containsKey(parts[1])) {
                                    to.println("Accesso come Subscriber avvenuto con successo al topic " + key);
                                    subscriberKey = key;
                                    gestisciSubscriber(key);
                                } else {
                                    to.println("Accesso come Subscriber fallito, il topic " + key + " non esiste");
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

    public void gestisciPublisher(String key) throws InterruptedException {
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);
            ArrayList<Message> currentClientMessages=new ArrayList<Message>();
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
                        case "send":{
                            if (parts.length > 1) {
                            	String message="";
                            	for(int i=1; i<parts.length;i++) {
                            		message+=parts[i];
                            	}
                            	Message messageFinal=new Message(topic.getSize(key)+1,message);
                                topic.addStringToKey(key, messageFinal);
                                currentClientMessages.add(messageFinal);
                                to.println("Messaggio inviato con successo sul topic");
                            }
                            break options;
                        }
                        case "list":{
                            String message = topic.list(currentClientMessages);
                            to.println(message.trim());
                            break options;
                        }
                            
                        case "listall":{
                            String message = topic.listAll(key);
                            to.println(message.trim());
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

    public void gestisciSubscriber(String key) throws InterruptedException {
        subscriberActive = true; // Imposta lo stato attivo per il subscriber
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);

            System.out.println("Thread " + Thread.currentThread() + " listening...");

            boolean closed = false;
            while (!closed) {
                String request = from.nextLine();
                if (!Thread.interrupted()) {
                    System.out.println("Request: " + request);
                    String[] parts = request.trim().split(" ");
                    options:
                    switch (parts[0]) {
                        case "quit":
                            closed = true;
                            break;
                        case "listall":
                            to.println(topic.listAll(key));
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
        if (subscriberActive && key.equals(subscriberKey)) {
            try {
                PrintWriter to = new PrintWriter(s.getOutputStream(), true);
                to.println("Nuovo messaggio sul topic " + key + ":\n" + value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    
    	}
	}

