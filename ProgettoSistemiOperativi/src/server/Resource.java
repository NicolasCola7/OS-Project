package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Resource {
    private HashMap<String, ArrayList<Message>> topics;
    private List<ClientHandler> subscribers;
    private ConcurrentHashMap<String, AtomicInteger> topicCounters; // Contatori per ogni topic
    
    public Resource() {
        this.topics = new HashMap<>();
        this.subscribers = new ArrayList<>();
        this.topicCounters = new ConcurrentHashMap<>();
    }
    
    public synchronized void add(String topic) {
	    ArrayList<Message> value = new ArrayList<>();
	    this.topics.put(topic, value);
	    this.topicCounters.put(topic, new AtomicInteger(0));
    	
    }
    
    public synchronized String show() {
        StringBuilder allTopics = new StringBuilder();
        for (String topic : topics.keySet()) {
            allTopics.append("-" + topic).append("\n");
        }
        return allTopics.isEmpty() ? "Nessun topic esistente" : "TOPICS:\n" + allTopics.toString().trim();
    }
    
    public int getPointerByTopic(String topic) {
    	synchronized(topics.get(topic)) {
    		return this.topicCounters.get(topic).get();
    	}
    }

    public String listAll(String topic){
	    ArrayList<Message> result = this.topics.get(topic);
	    StringBuilder message = new StringBuilder();
	    if(!topics.get(topic).isEmpty()) {
	        message.append("MESSAGGI:\n");
	        for (Message msg : result) {
	            message.append(msg).append("\n");
	        }
	    }
	    return message.isEmpty() ? "Non sono ancora stati inviati messaggi sul topic" : message.toString();

    }

    public String list(ClientHandler publisher, String topic) {   	
        StringBuilder message = new StringBuilder();
        ArrayList<Message> result = this.topics.get(topic);
        if(!topics.get(topic).isEmpty()) {
	        message.append("MESSAGGI:\n");
	        for (Message msg : result) {
	        	if(msg.publisherId == publisher.hashCode())
	        		message.append(msg).append("\n");
	        }
	    }
        return message.isEmpty() ? "Non hai ancora inviato messaggi sul topic" : message.toString();   	
    }

    public synchronized boolean containsTopic(String topic) {
		return this.topics.containsKey(topic);
    }

    public void addMessageToTopic(String topic, Message msg){   	
        // Ottieni il contatore per il topic specifico e incrementalo
        AtomicInteger counter = this.topicCounters.get(topic);
        int messageId = counter.getAndIncrement();
        msg.setId(messageId);
        
        this.topics.get(topic).add(msg);
        notifySubscribers(topic, msg);    	
    }
    
    // Aggiungi un subscriber
    public synchronized void addSubscriber(ClientHandler subscriber) {
        subscribers.add(subscriber);
    }
    
    public synchronized void removeSubscriber(ClientHandler subscriber) {
        subscribers.remove(subscriber);
    }

    // Notifica i subscriber
    private void notifySubscribers(String topic, Message msg) {
        for (ClientHandler suscriber : subscribers) {
        	suscriber.getMessageAdded(topic, msg);
        }
    }
    
    public int remove(String topic, int id) {
	    if(this.topics.get(topic).size() > 0) {
	        Message messaggio = containId(topic, id);
	        if(messaggio != null) {
	            int messageIndex = topics.get(topic).indexOf(messaggio);
	            this.topics.get(topic).remove(messageIndex);
	            return 1;
	        } else {
	            return 2;
	        }
	    } else {
	        return 0;
	    }
    }   
    
    public Message containId(String topic, int id) {
	    for (int i = 0; i < topics.get(topic).size(); i++) {
	        if(topics.get(topic).get(i).getId() == id) {
	            return topics.get(topic).get(i);
	        }
	    }
	    return null;
	}
 
}
