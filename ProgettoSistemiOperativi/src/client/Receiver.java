package client;

import java.io.IOException;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Thread per la ricezione di messaggi
 */
public class Receiver implements Runnable {

    Socket socket;
    Thread sender;

    public Receiver(Socket socket, Thread sender) {
        this.socket = socket;
        this.sender = sender;
    }

    @Override
    public void run() {
        try {
            Scanner from = new Scanner(this.socket.getInputStream());
            while (true) {
                String response = from.nextLine();
                System.out.println(response);
                if (response.equals("quit")) {
                	from.close();
                    break;
                }

            }
        } catch (IOException e) {
            System.err.println("IOException caught: " + e);
            e.printStackTrace();
        }
        catch (NoSuchElementException e ) {
        	System.err.println("Closed");
        }
        finally {
            System.out.println("Receiver closed.");
            this.sender.interrupt();
        }
    }
}

    
