package progetto;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ServerHandler implements Runnable {

    private Socket s;
    private Resource topic = ClientHandler.topic;

    public ServerHandler(Socket s) {
        this.s = s;
    }

    @Override
    public void run() {
        try {
            Scanner from = new Scanner(s.getInputStream());
            PrintWriter to = new PrintWriter(s.getOutputStream(), true);

            System.out.println("ServerHandler started for: " + s.getRemoteSocketAddress());

            boolean closed = false;
            while (!closed) {
                String request = from.nextLine();
                System.out.println("Received request: " + request); // Debug
                if (!Thread.interrupted()) {
                    String[] parts = request.split(" ");
                    if (parts.length == 0) {
                        to.println("Error: No command received");
                        continue;
                    }
                    String command = parts[0];
                    switch (command) {
                        case "quit":{
                            closed = true;
                            break;}
                        case "show":{
                            String allKey = topic.getAllKey();
                            to.println(allKey);
                            break;}
                        case "inspect":{
                            if (parts.length > 1) {
                                String key = parts[1];
                                to.println("Starting inspect session for key: " + key);
                                gestisciInspect(key, from, to);
                            } else {
                                to.println("Error: inspect requires a key argument");
                            }
                            break;}
                        default:
                            to.println("Unknown cmd: " + command); // Debug
                    }
                } else {
                    to.println("quit");
                    break;
                }
            }

            to.println("quit");
            s.close();
            System.out.println("ServerHandler closed.");
        } catch (IOException e) {
            System.err.println("ServerHandler: IOException caught: " + e);
            e.printStackTrace();
        }
    }

    private void gestisciInspect(String key, Scanner from, PrintWriter to) {
        boolean closed = false;
        while (!closed) {
            String request = from.nextLine();
            System.out.println("Received inspect request: " + request); // Debug
            String[] parts = request.trim().split(" ");
            if (parts.length == 0) {
                to.println("Error: No command received");
                continue;
            }
            String command = parts[0];
            switch (command) {
                case "end":
                    closed = true;
                    break;
                case "listall":
                    try {
                        String allMessage = topic.printAllStrings(key);
                        to.println(allMessage);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case "delete":
                    if (parts.length > 1) {
                        try {
                            int id = Integer.parseInt(parts[1]);
                            topic.remove(key, id);
                            to.println("Messaggio eliminato con successo");
                        } catch (NumberFormatException e) {
                            to.println("Error: ID must be a number");
                        }
                    } else {
                        to.println("Error: delete requires an id argument");
                    }
                    break;
                default:
                    to.println("Unknown cmd: " + command); // Debug
            }
        }
        to.println("Ending Inspect session for key: " + key);
    }
}
