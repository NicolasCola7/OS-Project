package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientHandler implements Runnable, ResourceListener {

    private Socket s;
    public static Resource topics = new Resource();
    private boolean subscriberActive = false; // Variabile di stato per il subscriber
    private String subscriberTopic;
    private HashMap<String,ReentrantLock> lock; 

    public ClientHandler(Socket s, HashMap<String,ReentrantLock> lock) {
        this.s = s;
        this.lock = lock;
        
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
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic: " + topic + " non precedentemente esistente è stato creato");
                                    lock.put(topic, new ReentrantLock());
                                    gestisciPublisher(topic);
                                } else {
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic: " + topic + " precedentemente esistente");
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
                        	if(lock.get(topic).isLocked()) {
                        		to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                        	}
                        	
                            if (parts.length > 1) {
                            	lock.get(topic).lock();
                            	try {
                            	String message="";
                            	for(int i=1; i<parts.length;i++) {
                            		message+=parts[i]+ " ";
                            	}
                            	
                            	Message messageFinal=new Message(topics.getPuntatore().get(),message);
                                topics.addMessageToTopic(topic, messageFinal);
                                currentClientMessages.add(messageFinal);
                                to.println("Messaggio inviato con successo sul topic");
                            	}
                            	
                            	finally {
                            		lock.get(topic).unlock();
                            	}
                            }
                            
                            break options;
                        }
                        
                        case "list":{
                        	if(lock.get(topic).isLocked()) {
                        		to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                        	}
                        	
                        	lock.get(topic).lock();
                        	try {
                            String message = topics.list(currentClientMessages);
                            to.println(message.trim());
                        	}
                        	
                        	finally {
                        		lock.get(topic).unlock();
                        	}
                            break options;
                        }
                            
                        case "listall":{
                        	if(lock.get(topic).isLocked()) {
                        		to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                        	}
                        	
                        	lock.get(topic).lock();
                        	try {
                            String message = topics.listAll(topic);
                            to.println(message.trim());
                        	}
                        	
                        	finally {
                        		lock.get(topic).unlock();
                        	}
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
                    System.out.println(request);
                    String[] parts = request.trim().split(" ");
                    options:
                    switch (parts[0]) {
                    
                        case "quit":
                            closed = true;
                            break;
                            
                        case "listall":
                        	if(lock.get(topic).isLocked()) {
                        		to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                        	}
                        	
                        	lock.get(topic).lock();
                        	try {
                            to.println(topics.listAll(topic));
                        	}
                        	
                        	finally {
                        		lock.get(topic).unlock();
                        	}
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

