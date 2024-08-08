package progetto;

//File: Resource.java
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Resource {
	private HashMap<String, ArrayList<Message>> topics;
	private List<ResourceListener> listeners;

 	public Resource() {
 		this.topics = new HashMap<>();
 		this.listeners = new ArrayList<>();
 	}

 	public synchronized void add(String topic) throws InterruptedException {
 		ArrayList<Message> value = new ArrayList<>();
 		this.topics.put(topic, value);
 		notifyAll();
 	}

 	public synchronized String show() {
 		StringBuilder allTopics = new StringBuilder();
 		for (String topic : topics.keySet()) {
 			allTopics.append("-" + topic).append("\n");
 		}
 		return  allTopics.isEmpty() ? "Nessun topic esistente" : "TOPICS:  \n " + allTopics.toString().trim();
 	}

 	public synchronized String listAll(String topic) throws InterruptedException {
 		ArrayList<Message> result = this.topics.get(topic);
 		StringBuilder message = new StringBuilder();
 		if(!topics.get(topic).isEmpty()) {
	 		message.append("MESSAGGI: \n");
	 		for (Message msg : result) {
	 			message.append(msg).append("\n");
	 		}
	 		notifyAll();
 		}
 		return message.isEmpty() ? "Non sono ancora stati inviati messaggi sul topic" : message.toString();
 	}
 
 	public synchronized String list(ArrayList<Message> currentClientMessages) throws InterruptedException {
 		StringBuilder message = new StringBuilder();
 		if(!currentClientMessages.isEmpty()) {
			message.append("MESSAGGI: \n");
			for (Message msg : currentClientMessages) {
				message.append(msg).append("\n");
			}	
			notifyAll();
 		}
 		return  message.isEmpty() ? "Non hai ancora inviato messaggi sul topic" : message.toString();
 	}

 	public synchronized boolean containsTopic(String topic) {
 		return this.topics.containsKey(topic);
 	}

 	public synchronized void addMessageToTopic(String topic, Message msg) throws InterruptedException {

 		this.topics.get(topic).add(msg);
 		notifyAll();
 		notifyListeners(topic, msg); // Notifica i listener quando viene aggiunto un valore
 	}


 	// Aggiungi un listener
 	public synchronized void addListener(ResourceListener listener) {
 		listeners.add(listener);
 	}

 	// Notifica i listener
 	private void notifyListeners(String topic, Message msg) {
 		for (ResourceListener listener : listeners) {
 			listener.onValueAdded(topic, msg);
 		}
 	}
 	public synchronized int getSize(String topic) {
        return this.topics.get(topic).size();
	}
 	public synchronized void remove(String topic, int id) {
 		this.topics.get(topic).remove(id);
 	}	
 
}

//Interfaccia per i listener
interface ResourceListener {
 void onValueAdded(String key, Message value);
}
