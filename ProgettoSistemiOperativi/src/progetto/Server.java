package progetto;

import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
 private static List<ClientHandler> clients = Collections.synchronizedList(new ArrayList<>());

 public static void main(String[] args) {
     int port = 90;

     try (ServerSocket serverSocket = new ServerSocket(port)) {
         System.out.println("Server in ascolto sulla porta " + port);

         while (true) {
             Socket clientSocket = serverSocket.accept();
             ClientHandler clientHandler = new ClientHandler(clientSocket);
             clients.add(clientHandler);
             new Thread(clientHandler).start();
         }
     } catch (IOException e) {
         System.out.println("Errore di avvio del server sulla porta " + port);
         e.printStackTrace();
     }
 }

 static class ClientHandler implements Runnable {
     private Socket socket;
     private BufferedReader in;
     private PrintWriter out;

     public ClientHandler(Socket socket) {
         this.socket = socket;
         try {
             this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             this.out = new PrintWriter(socket.getOutputStream(), true);
         } catch (IOException e) {
             System.out.println("Errore di connessione con il client");
             e.printStackTrace();
         }
     }

     public void run() {
         try {
             String message;
             while ((message = in.readLine()) != null) {
                 System.out.println("Ricevuto dal client " + socket.getInetAddress().getHostAddress() + ": " + message);
                 sendToOtherClients(message);
             }
         } catch (IOException e) {
             System.out.println("Errore di comunicazione con il client");
             e.printStackTrace();
         } finally {
             try {
                 socket.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
     }

     private void sendToOtherClients(String message) {
         synchronized (clients) {
             for (ClientHandler client : clients) {
                 if (client != this) {
                     client.out.println("Messaggio da " + socket.getInetAddress().getHostAddress() + ": " + message);
                 }
             }
         }
     }
 }
}

