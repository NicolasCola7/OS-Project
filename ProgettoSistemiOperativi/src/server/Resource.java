package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe rappresentante la risorsa condivisa (lista di topic).
 */

public class Resource {
    private HashMap<String, ArrayList<Message>> topics;
    private List<ClientHandler> subscribers;
    private HashMap<String, AtomicInteger> topicCounters; //Numero di messaggi per ogni topic.
    
    public Resource() {
        this.topics = new HashMap<>();
        this.subscribers = new ArrayList<>();
        this.topicCounters = new HashMap<>();
    }
    
    /**
     * Aggiunge un nuovo topic alla lista.
     * @param topic è il topic aggiunto.
     */
    public synchronized void add(String topic) {
	    ArrayList<Message> value = new ArrayList<>();
	    this.topics.put(topic, value);
	    this.topicCounters.put(topic, new AtomicInteger(0));
    	
    }
    
    /**
     * Mostra i topic esistenti.
     * @return ritorna una stringa rappresentante i topic oppure "Nessun topic esistente".
     */
    public synchronized String show() {
        StringBuilder allTopics = new StringBuilder();
        for (String topic : topics.keySet()) {
            allTopics.append("-" + topic).append("\n");
        }
        return allTopics.isEmpty() ? "Nessun topic esistente" : "TOPICS:\n" + allTopics.toString().trim();
    }
    
    /**
     * Restituisce il numero di messaggi presenti in un topic.
     * @param topic
     * @return numero di messaggi
     */
    public int getPointerByTopic(String topic) {
    	synchronized(topics.get(topic)) {
    		return this.topicCounters.get(topic).get();
    	}
    }

    /**
     * Mostra tutti i messaggi inviati sul topic.
     * @param topic di cui si vogliono visualizzare i messaggi.
     * @return stringa con i messaggi 
     */
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
    
    /**
     * Mostra tutti i messaggi inviati sul topic da un determinato client.
     * @param publisher client di cui si vogliono visualizzare i messaggi.
     * @param topic topic di cui si vogliono visualizzare i messaggi.
     * @return stringa con i messaggi
     */

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
    
    /**
     * Verifica se esiste un determinato topic.
     * @param topic
     * @return true se esiste, false se non esiste
     */
    public synchronized boolean containsTopic(String topic) {
		return this.topics.containsKey(topic);
    }

    /**
     * Aggiunta di un messaggio in un topic.
     * @param topic
     * @param msg è il messaggio da aggiungere
     */
    public void addMessageToTopic(String topic, Message msg){   	
        AtomicInteger counter = this.topicCounters.get(topic);
        int messageId = counter.getAndIncrement();
        msg.setId(messageId);
        
        this.topics.get(topic).add(msg);
        notifySubscribers(topic, msg);    	
    }
    
    /**
     * Aggiunge un subscriber alla lista.
     * @param subscriber
     */
    public synchronized void addSubscriber(ClientHandler subscriber) {
        subscribers.add(subscriber);
    }
    
    /**
     * Rimuove un subscriber dalla lista.
     * @param subscriber
     */
    public synchronized void removeSubscriber(ClientHandler subscriber) {
        subscribers.remove(subscriber);
    }

    /**
     * Notifica tutti i subscribers di un determinato topic una volta aggiunto un nuovo messaggio.
     * @param topic
     * @param msg
     */
    private void notifySubscribers(String topic, Message msg) {
        for (ClientHandler suscriber : subscribers) {
        	suscriber.getMessageAdded(topic, msg);
        }
    }
    
    /**
     * rimuove un messaggio da un determinato topic.
     * @param topic
     * @param id del messaggio da eliminare.
     * @return 1 se il topic viene rimosso, 2 se non esiste quel messaggio, 0 se il topic non contiene messaggi.
     */
    public int remove(String topic, int id) {
	    if(this.topics.get(topic).size() > 0) {
	        Message messaggio = containsMessage(topic, id);
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
    
    /**
     * Verifica se esiste un messaggio all'interno di un topic dato l'id
     * @param topic
     * @param id del messaggio
     * @return il messaggio se esiste, altrimenti null
     */
    public Message containsMessage(String topic, int id) {
	    for (int i = 0; i < topics.get(topic).size(); i++) {
	        if(topics.get(topic).get(i).getId() == id) {
	            return topics.get(topic).get(i);
	        }
	    }
	    return null;
	}
 
}
