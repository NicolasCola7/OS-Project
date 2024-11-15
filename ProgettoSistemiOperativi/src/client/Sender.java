package client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

/**
 * Thread per l'invio di messaggi
 */
public class Sender implements Runnable {

   Socket socket;

   public Sender(Socket socket) {
       this.socket = socket;
   }

   @Override
   public void run() {
       
	   Scanner userInput = new Scanner(System.in);

       try {
           PrintWriter to = new PrintWriter(this.socket.getOutputStream(), true);
           while (true) {
               String request = userInput.nextLine();
               /*
                * se il thread è stato interrotto mentre leggevamo l'input da tastiera, inviamo
                * "quit" al server e usciamo
                */
               if (Thread.interrupted()) {
                   to.println("quit");
                   break;
               }
               /* in caso contrario proseguiamo e analizziamo l'input inserito */
               to.println(request);
               if (request.equals("quit")) {
                   break;
               }
           }
           System.out.println("Sender closed.");
       } catch (IOException e) {
           System.err.println("IOException caught: " + e);
           e.printStackTrace();
       } finally {
           userInput.close();
       }
   }

}

   