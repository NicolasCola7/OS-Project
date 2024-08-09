package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
	
	private static Resource topics = ClientHandler.topics;
	
	  private static void gestisciInspect(String topic, Scanner from) {
	        boolean closed = false;
	        while (!closed) {
	            String request = from.nextLine();
	            System.out.println("Received inspect request: " + request); // Debug
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
	                    try {
	                        String allMessage = topics.listAll(topic);
	                        System.out.println(allMessage);
	                    } catch (InterruptedException e) {
	                        e.printStackTrace();
	                    }
	                    break;
	                case "delete":
	                    if (parts.length > 1) {
	                        try {
	                            int id = Integer.parseInt(parts[1]);
	                            topics.remove(topic, id);
	                            System.out.println("Messaggio eliminato con successo");
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
        args = new String[1];
        args[0] = "9000";
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
            Thread serverThread = new Thread(new SocketListener(serverSocket));
            serverThread.start();

            String input = "";

            while (!input.equals("quit")) {
            	input = userInput.nextLine();
                String[] parts = input.split(" ");
                
                switch(parts[0]) {
                
                case "show":{
                	String allTopic = topics.show();
                    System.out.println(allTopic);
                	break;
                }
                
                case "inspect" :{
                	
                	
                	if (parts.length > 1 && topics.containsTopic(parts[1])) {
                		String topic = parts[1];
                    	gestisciInspect(topic, userInput);
                    	break;
                	}
                	else if (parts.length > 1 && !topics.containsTopic(parts[1])){
                		System.out.println("Topic non esistente");
                		break;
                	}
                	else
                		System.out.println("Necessario specificare il topic da ispezionare");
                	break;
                }
                
                default :
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
