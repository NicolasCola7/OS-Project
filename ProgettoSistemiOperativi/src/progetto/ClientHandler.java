package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.Semaphore;

public class ClientHandler extends Thread implements Runnable, ResourceListener {

    private Socket s;
    public  Resource topics;
    private String chosenTopic;
    private AbstractMap<String,ReentrantReadWriteLock> semaphores; 

    public ClientHandler(Socket s, HashMap<String, ReentrantReadWriteLock> semaphores, Resource resource) {
        this.s = s;
        this.semaphores = semaphores;
        this.topics = resource;
       
      
    }

    @Override
    public void run() {
        try {
        	Scanner from = new Scanner(s.getInputStream());
    		PrintWriter to = new PrintWriter(s.getOutputStream(), true);
     
            System.out.println("Thread " + Thread.currentThread() + " listening...");

            boolean closed = false;
            while (!closed) {
            	if(from.hasNextLine()) {
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
                                chosenTopic = parts[1].trim();
                                
                                if (!topics.containsTopic(parts[1])) {
                                    topics.add(chosenTopic);  
                                    synchronized(semaphores) {
                                    semaphores.put(chosenTopic, new ReentrantReadWriteLock()); // Aggiungi un lock per il topic  
                                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic '" + chosenTopic + "' non precedentemente esistente è stato creato");
                                    }
                                } 
                                
                                else
                                	 to.println("Accesso come Publisher avvenuto con successo. \nIl topic '" + chosenTopic + "' precedentemente esistente");
                                
                                gestisciPublisher();
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
                               
                                if (topics.containsTopic(parts[1])) {
                                	chosenTopic = parts[1];
                                	topics.addSubscriber(this); // Registrazione come listener
                                    to.println("Accesso come Subscriber avvenuto con successo al topic " + chosenTopic);
                                    gestisciSubscriber();
                                } else {
                                    to.println("Accesso come Subscriber fallito, il topic " + chosenTopic + " non esiste");
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
            }

            to.println("quit");
            from.close();
           // s.close();
            System.out.println("Closed");
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (NoSuchElementException e ) {
        	System.err.println("Closed");
        }
    }

    public void gestisciPublisher() throws InterruptedException {
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
                            	if (semaphores.get(chosenTopic).isWriteLocked() && Server.inspectLock.isLocked()) {
                                    to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                                }

                                if (parts.length > 1) {
                                                                 	                                    
                                    try {
                                    	semaphores.get(chosenTopic).writeLock().lock();// Acquisisci il semaforo
                                        String message = "";
                                       
                                        for (int i = 1; i < parts.length; i++) {
                                            message += parts[i] + " ";
                                        }

                                        Message messageFinal = new Message(topics.getPuntatoreByTopic(chosenTopic), message, this.hashCode());
                                        topics.addMessageToTopic(chosenTopic, messageFinal);
                                        currentClientMessages.add(messageFinal);
                                        to.println("Messaggio inviato con successo sul topic");
                                    } finally {
                                        semaphores.get(chosenTopic).writeLock().unlock(); // Rilascia il semaforo
                                    }
                                }                                  
                            });
                            
                            childThread.start(); 
                            break options;                         
                            
                        }

                        
                        case "list":{
                        	Thread childThread = new Thread(() -> {
                           
                        		if (semaphores.get(chosenTopic).isWriteLocked() && Server.inspectLock.isLocked()) 
                        			 to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");                      		                       		
		                        
                        		 try {
                        			 semaphores.get(chosenTopic).readLock().lock();// Acquisisci il semaforo
                        			 String message = topics.list(this, chosenTopic);
                        			 to.println(message.trim());
                        		 } finally {
                        			 semaphores.get(chosenTopic).readLock().unlock(); // Rilascia il semaforo
                        		 }
                        	 });
                        	 
                        	 childThread.start();
                        	 break options;
                        }
                            
                        case "listall":{
                        	 Thread childThread = new Thread(() -> {
                        		 if (semaphores.get(chosenTopic).isWriteLocked() && Server.inspectLock.isLocked()) {
                        			 to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                        		 }
                            
                        		 try {
                        			 semaphores.get(chosenTopic).readLock().lock(); // Acquisisci il semaforo
                        			 String message = topics.listAll(chosenTopic);
                        			 to.println(message.trim());
                        		 } catch (InterruptedException e) {								
                        			 e.printStackTrace();
                        		 } finally {
                        			 semaphores.get(chosenTopic).readLock().unlock(); // Rilascia il semaforo
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
           // s.close();
            System.out.println("Closed");
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        }
    }

    public void gestisciSubscriber() throws InterruptedException {
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
                        		if (semaphores.get(chosenTopic).isWriteLocked() && Server.inspectLock.isLocked()) { 
                        			to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
                        		}
                            
                        		try {
                        			semaphores.get(chosenTopic).readLock().lock(); // Acquisisci il semaforo
                        			to.println(topics.listAll(chosenTopic));
                        		} catch (InterruptedException e) {								
                        			e.printStackTrace();
                        		} finally {
                        			semaphores.get(chosenTopic).readLock().unlock(); // Rilascia il semaforo
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
           // s.close();
            System.out.println("Closed");
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageAdded(String key, Message value) {
        if (key.equals(chosenTopic)) {
            try {
            	PrintWriter to = new PrintWriter(s.getOutputStream(), true);    
                to.println("Nuovo messaggio sul topic " + key + ":\n" + value);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
