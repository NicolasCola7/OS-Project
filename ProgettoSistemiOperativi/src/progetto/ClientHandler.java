package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    static Socket s;
    /* possiamo avere una hashmap per ogni thread, o condividerla tra tutti */
    HashMap<String, String> information = new HashMap<String, String>();
	static Resource topic =new Resource();
    public ClientHandler(Socket s) {
        this.s = s;
        information.put("important", "Incredibly important bit of information about everything");
        information.put("random", "Random bit of information about something");
        information.put("shadow", "The outer part of a shadow is called the penumbra");
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
                    String[] parts = request.split(" ");
                    switch (parts[0]) {
                        case "quit":
                            closed = true;
                            break;
                        case "publish":
                        	if (parts.length > 1) {
                        		String key=parts[1];
                            	if(!topic.containsKey(parts[1])) {
                            		topic.add(key);
                            		to.println("Accesso come Publisher avvenuto con successo. \nIl topic: "+ key+ " non precedentemente esistente è stato  creato");
                            		gestisciPublisher(key);
                            	}
                            	else {
                            		to.println("Accesso come Publisher avvenuto con successo. \nIl topic: "+ key + "precedentemente esistente");
                            		gestisciPublisher(key);
                            	}
                        	}
                        	break;
                        case "show":
                        	String allKey=topic.getAllKey();
                        	to.println(allKey);
                        	break;
                        case "subscribe":
                        	if (parts.length > 1) {
                        		String key=parts[1];
                            	if(topic.containsKey(parts[1])) {                            		
                            		to.println("Accesso come Subscriber avvenuto con successo al topic "+key);
                            		gestisciSubscriber(key);
                            	}
                            	else {
                            		to.println("Accesso come Subscriber fallito, il topic "+ key + " non esiste");                           		
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

    public static void gestisciPublisher(String key) throws InterruptedException {
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
                    options: switch (parts[0]) {
                        case "quit":
                            closed = true;
                            break;
                        case "send":
                        	if (parts.length > 1) {
                        		topic.addStringToKey(key,parts[1]);
                        		to.println("Messagio inviato con successo sul topic");
                        	}
                        	break options;
                        	
                        	
                        case "list":
                        	String message=topic.printAllStrings(key);
                        	to.println("Messaggi: "+ message.trim());
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
        }
    }
    
    public static void gestisciSubscriber(String key) throws InterruptedException{
    	
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
                    options: switch (parts[0]) {
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
        }
    }
    	
   }

    


    
