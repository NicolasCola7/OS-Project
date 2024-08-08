package progetto;

//File: Resource.java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Resource {
 private HashMap<String, ArrayList<Message>> information;
 private List<ResourceListener> listeners;

 public Resource() {
     this.information = new HashMap<>();
     this.listeners = new ArrayList<>();
 }

 public synchronized void add(String key) throws InterruptedException {
     while (this.information.get(key) != null) {
         wait();
     }
     ArrayList<Message> value = new ArrayList<>();
     this.information.put(key, value);
     notifyAll();
 }

 public synchronized String getAllKey() {
     StringBuilder allKey = new StringBuilder();
     for (String key : information.keySet()) {
         allKey.append(key).append("\n");
     }
     return allKey.toString().trim();
 }

 public synchronized String listAll(String key) throws InterruptedException {
     while (this.information.get(key).isEmpty()) {
         wait();
     }
     ArrayList<Message> result = this.information.get(key);
     StringBuilder message = new StringBuilder();
     for (Message s : result) {
         message.append(s).append("\n");
     }
     notifyAll();
     return message.toString();
 }

 public synchronized boolean containsKey(String key) {
     return this.information.containsKey(key);
 }

 public synchronized void addStringToKey(String key, Message value) throws InterruptedException {
     while (!this.information.containsKey(key)) {
         wait();
     }
     this.information.get(key).add(value);
     notifyAll();
     notifyListeners(key, value); // Notifica i listener quando viene aggiunto un valore
 }

 public synchronized String printAllStrings(String key) throws InterruptedException {
     StringBuilder x = new StringBuilder();
     while (this.information.get(key).isEmpty()) {
         wait();
     }
     ArrayList<Message> result = this.information.get(key);
     for (Message s : result) {
         x.append(s.toString()).append("\n");
     }
     notifyAll();
     return x.toString();
 }

 // Aggiungi un listener
 public synchronized void addListener(ResourceListener listener) {
     listeners.add(listener);
 }

 // Notifica i listener
 private void notifyListeners(String key, Message value) {
     for (ResourceListener listener : listeners) {
         listener.onValueAdded(key, value);
     }
 }
 public synchronized int getSize(String key) {
	        return this.information.get(key).size();
	}

 
}

//Interfaccia per i listener
interface ResourceListener {
 void onValueAdded(String key, Message value);
}
