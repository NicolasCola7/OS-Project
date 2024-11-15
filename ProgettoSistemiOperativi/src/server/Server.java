package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server {
	
    private static final HashMap<String, ReentrantReadWriteLock> semaphores = new HashMap<>(); 
    private static  Resource topics = new Resource();
    protected static final HashMap<String, Boolean> inspectLocks = new HashMap<>();
    private static Scanner userInput;
    
    /**
     * Processa e gestisce i comandi della sezione di ispezione
     * @param topic è il topic ispezionato
     */
    private static void manageInspect(String topic) {
    	inspectLocks.put(topic, true); 
    	System.out.println("Attivata sessione di ispezione sul topic " + topic);
    	
        boolean inspectEnd = false;
        while (!inspectEnd) {
            String request = userInput.nextLine();
            String[] parts = request.trim().split(" ");
            if (parts.length == 0) {
                System.out.println("Error: No command received");
                continue;
            }
            String command = parts[0];
            switch (command) {
                
            	case "end":
            		inspectEnd = true;
                    inspectLocks.put(topic, false);
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
                case "commands":
                	System.out.println("Comandi sessione inspect:\n"
    						+ "- 'end':\n"
    					    + "\tTermina la sessione di ispezione.\n"
    					    + "- 'listall':\n"
    				        + "\tMostra tutti i messaggi del topic in ispezione.\n"
    					    + "- 'delete <id>':\n"
    					    + "\tElimina un messaggio specifico, identificato da id");
                	break;
                    
                default:
                    System.out.println("Unknown cmd: " + command); 
            }
        }
        System.out.println("Ending Inspect session for key: " + topic);
    }

    /**
     * Si occupa della creazione e gestione del server.
     * @param args sono i parametri di inizializzazione del server, in partiicolare args[0] è il numero di porta.
     */
    public static void main(String[] args) {
       
        if (args.length < 1) {
            System.err.println("Usage: java Server <port>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        userInput = new Scanner(System.in);

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            /*
             * Deleghiamo a un altro thread la gestione di tutte le connessioni client;
             * Nel thread principale gestiamo la console del server.
             */
           
            Thread serverThread = new Thread(new SocketListener(serverSocket, semaphores, topics, inspectLocks));
            serverThread.start();

            processServerCommands();

            try {
                //Rimaniamo in attesa che sender e receiver terminino la loro esecuzione.
                serverThread.interrupt();
                //attendi la terminazione del thread.
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
    
    /**
     * Processa ed esegue i comandi impartiti dal server.
     */
    
    private static void processServerCommands() {
    	String input = "";
    	boolean serverClosed=false;

        while (!serverClosed) {
            input = userInput.nextLine();
            String[] parts = input.split(" ");

            switch (parts[0]) {
            	case "quit":
            		serverClosed=true;
            		return;
            		
                case "show": {
                    System.out.println(topics.show());
                    break;
                }

                case "inspect": {
                    if (parts.length > 1 && topics.containsTopic(parts[1])) {
                        String topic = parts[1];
                          	
                        //Acquisisco lock in scrittura 
                        try {                     
                        	semaphores.get(topic).writeLock().lock();
                            manageInspect(topic);  
                        } catch (Exception e) {
                            e.printStackTrace();
                          //Rilascio lock in scrittura
                        } finally {
                        	semaphores.get(topic).writeLock().unlock();                             
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
                case "commands": 
                	System.out.println("Lista dei comandi:\n"
                						+ "- 'quit':\n"
                						+ "\tTermina l'esecuzione del server.\n"
                						+ "- 'show':\n"
                						+ "\tMostra tutti i topic disponibili nell'applicazione.\n"
                						+ "- 'inspect <topic>': \n"
                						+ "\tAvvia una sessione di ispezione per un topic.\n");
                		break;
                

                default:
                    System.out.println("Unknown cmd");
            }
        }
    	
    }
}
