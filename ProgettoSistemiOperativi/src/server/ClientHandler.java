package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ClientHandler implements Runnable {
    private Socket s;
    public Resource topics;
    private String chosenTopic;
    private HashMap<String,ReentrantReadWriteLock> semaphores; 
    private HashMap <String, Boolean> inspectLocks;
    private Scanner from;
    private PrintWriter to;
    private boolean closed;

    public ClientHandler(Socket s, HashMap<String, ReentrantReadWriteLock> semaphores, Resource resource, HashMap <String, Boolean> inspectLocks) {
        this.s = s;
        this.semaphores = semaphores;
        this.topics = resource;
        this.inspectLocks = inspectLocks;
    }

    @Override
    public void run() {
        try {
        	from = new Scanner(s.getInputStream());
    		to = new PrintWriter(s.getOutputStream(), true);
     
            System.out.println("Thread " + Thread.currentThread() + " listening...");
            
            closed = false;
            while (!closed) {
            	if(from.hasNextLine()) {
            		String request = from.nextLine();
                
            		if (!Thread.interrupted()) {
            			processClient(request);
            		} else {
            			to.println("quit");
            			break;
            		}
            	}
            }
            
	        to.println("quit");
	        from.close();
	        s.close();
	        System.out.println("Closed");
	        
        } catch (IOException e) {
            System.err.println("ClientHandler: IOException caught: " + e);
            e.printStackTrace();
        } catch (NoSuchElementException e ) {
        	System.err.println("Closed");
        }
    }
    
    /**
     * Processa e gestisce la scelta della modalità con cui un client si vuole registrare.
     * @param request è il comando scritto dall'utente, composto da "command  topic" o solo "command"
     */
    private void processClient(String request) {
    	System.out.println(request);
        String[] parts = request.trim().split(" ");
        if (parts.length > 0) {
		    switch (parts[0]) {
		       
		    	case "quit":
		            closed = true;
		            break;
		       
		        case "publish":
		            processPublisher(parts);
		            break;
		        
		        case "show":
		            to.println(topics.show());
		            break;
		        
		        case "subscribe":
		            processSubscriber(parts);
		            break;
		        
		        default:
		            to.println("Unknown cmd");
		    }
        }
    }
    
    /**
     * Processa la scelta di registrazione del client come publisher aggiungendo il topic scelto e il semaforo su quest'ultimo.
     * @param parts è l'array rappressentante il comando publish (parts[0]) e il topic scelto (parts[1]).
     */
    private void processPublisher(String[] parts) {
    	if (parts.length > 1) {
            chosenTopic = parts[1].trim();
            
            synchronized(topics) {
                if (!topics.containsTopic(parts[1])) {
                    topics.add(chosenTopic);  
                    semaphores.put(chosenTopic, new ReentrantReadWriteLock());  
                    inspectLocks.put(chosenTopic, false);
                    to.println("Accesso come Publisher avvenuto con successo. \nIl topic '" + chosenTopic + "' non precedentemente esistente e' stato creato");
            	} 
                else
                	 to.println("Accesso come Publisher avvenuto con successo al topic '" + chosenTopic + "'");
            }
            managePublisher();
        }
    }
    
    /**
     * Processa la scelta di registrazione del client come subscriber aggiungendo il client nella lista dei
     * subscriber nella risorsa condivisa.
     * @param parts è l'array rappressentante il comando publish (parts[0]) e il topic scelto (parts[1]).
     */
    private void processSubscriber(String[] parts) {
    	if (parts.length > 1) {
            
            if (topics.containsTopic(parts[1])) {
            	chosenTopic = parts[1];;
            	topics.addSubscriber(this); 
                to.println("Accesso come Subscriber avvenuto con successo al topic " + chosenTopic);
                manageSubscriber();
            } else {
                to.println("Accesso come Subscriber fallito, il topic " + parts[1] + " non esiste");
            }
        }
    }

    /**
     * Gestisce i comandi impartiti da un publisher.
     */
    private void managePublisher() {
        while (!closed) {
            String request = from.nextLine();
            if (!Thread.interrupted()) {
                System.out.println(request);
                String[] parts = request.trim().split(" ");
                
                switch (parts[0]) {
                    
                	case "quit":
                        closed = true;
                        return;
                        
                    case "send": 
                    	executeCommand(() -> sendMessage(parts), true);
                    	break;                         
                    
                    case "list":
                    	executeCommand(() -> listCurrentClientMessages(), false);  
                    	break;
                    
                    case "listall":
                    	executeCommand(() -> listAllMessages(), false);
                    	break;     
                    
                    default:
                        to.println("Unknown cmd");
                }
            } else {
                to.println("quit");
                break;
            }
        }
    }

    /**
     * Gestisce i comandi impartiti da un subscriber.
     */
    public void manageSubscriber() {
        while (!closed) {
            String request = from.nextLine();
            if (!Thread.interrupted()) {
                String[] parts = request.trim().split(" ");
                
                switch (parts[0]) {
                
                    case "quit":
                        closed = true;
                        topics.removeSubscriber(this);
                        return;
                        
                    case "listall":
                    	executeCommand(() -> listAllMessages(), false);
                    	break;  
                    	
                    default:
                        to.println("Unknown cmd");
                }
            } else {
                to.println("quit");
                break;
            }
        }  
    }
    
    /**
     * Esegue i comandi di publisher e subscriber in modo asincrono, eseguendoli su un thread separato
     * in modo da non bloccare l'esecuzione del thread principale.
     * @param command è il metodo da esegure.
     * @param isWrite è un booleano che indica se il comando è un'operazione di lettura o scrittura.
     */
    private void executeCommand(Runnable command, boolean isWrite) {
    	new Thread(() -> {
    		if (semaphores.get(chosenTopic).isWriteLocked() && inspectLocks.get(chosenTopic) == true) 
    			to.println("Sessione di ispezione attiva, il comando verrà eseguito appena terminerà...");
    		
    		try {
    			
    			if (isWrite) {
    				semaphores.get(chosenTopic).writeLock().lock(); 
    			} else {
    				semaphores.get(chosenTopic).readLock().lock(); 
    			}
    			
    			command.run();
    			
    		} finally {
    			
    			if (isWrite) {
    				semaphores.get(chosenTopic).writeLock().unlock(); 
    			} else {
    				semaphores.get(chosenTopic).readLock().unlock(); 
    			}
    		}
		}).start();
    }
    
    /**
     * Invia un messaggio sul topic prescelto.
     * @param parts rappresenta l'array composto da comando send (parts[0]) e messaggio da inviare (parts[1]).
     */
    private void sendMessage(String[] parts) {
    	if (parts.length > 1) {
			String message = "";
		    
		    for (int i = 1; i < parts.length; i++) {
		        message += parts[i] + " ";
		    }
		
		    Message messageFinal = new Message(topics.getPointerByTopic(chosenTopic), message, this.hashCode());
		    topics.addMessageToTopic(chosenTopic, messageFinal);
		    to.println("Messaggio inviato con successo sul topic");
    	}
    }
    
    /**
     * Mostra i messaggi inviati sul topic prescelto dal publisher corrente.
     */
    private void listCurrentClientMessages() {
    	String message = topics.list(this, chosenTopic);
		to.println(message.trim());
    }
	
    /**
     * Mostra tutti i messaggi inviati da tutti i publisher sul topic prescelto.
     */
    private void listAllMessages() {
    	String message = topics.listAll(chosenTopic);
		 to.println(message.trim());
    }
    
    /**
     * Mostra il messaggio appena inviato su un determinato topic a tutti i suoi subscriber.
     * @param topic è il topic su cui è stato ricevuto il messaggio
     * @param msg è il messaggio ricevuto
     */
    public void getMessageAdded(String topic, Message msg) {
        if (topic.equals(chosenTopic)) {
            try {
            	PrintWriter to = new PrintWriter(s.getOutputStream(), true);    
                to.println("Nuovo messaggio sul topic " + topic + ":\n" + msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
