package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class ClientHandler implements Runnable {

    Socket s;
    /* possiamo avere una hashmap per ogni thread, o condividerla tra tutti */
    HashMap<String, String> information = new HashMap<String, String>();
	Resource topic =new Resource();
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
                        case "info":
                            if (parts.length > 1) {
                                String key = parts[1];
                                String response = information.getOrDefault(key, "Error!");
                                to.println(response);
                            } else {
                                to.println("No key");
                            }
                            break;
                        case "publisher":
                        	to.println("prova");
                        	if (parts.length > 1) {
                            	String key=parts[1];
                            	topic.add(key);
                            	to.println("Debug topic");
                            }
                        	break;
                        case "listAll":
                        	String allKey=topic.getAllKey();
                        	to.println(allKey);
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

}

    
