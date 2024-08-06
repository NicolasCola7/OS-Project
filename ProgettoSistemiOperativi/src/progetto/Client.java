package progetto;

import java.io.*;
import java.net.*;

public class Client {
 public static void main(String[] args) {
     String hostname = "localhost"; // Indirizzo del server
     int port = 90; // Porta su cui il server è in ascolto

     try (Socket socket = new Socket(hostname, port);
          BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
          BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in))) {

         System.out.println("Connesso al server " + hostname + " sulla porta " + port);
         String userInput;
         while ((userInput = stdIn.readLine()) != null) {
             out.println(userInput);
             System.out.println("Risposta dal server: " + in.readLine());
         }
     } catch (UnknownHostException e) {
         System.out.println("Host sconosciuto: " + hostname);
         e.printStackTrace();
     } catch (IOException e) {
         System.out.println("Errore di I/O per la connessione a " + hostname);
         e.printStackTrace();
     }
 }
}