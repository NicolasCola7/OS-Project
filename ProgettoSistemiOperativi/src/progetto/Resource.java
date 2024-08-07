package progetto;
import java.util.HashMap;
import java.util.ArrayList;


public class Resource {
    private HashMap<String, ArrayList<String>> information;

    public Resource() {
        this.information = new HashMap<>();
    }

    

    public synchronized void add(String key) throws InterruptedException {
        while (this.information.get(key) != null) {
            wait();
        }
        ArrayList<String> value=new ArrayList<String>();
        this.information.put(key, value);
        notifyAll();
    }

    public synchronized String getAllKey() {
    	String allKey="";
    	for (String key : information.keySet()) {
    		allKey+= key +"\n";
    	}
    	allKey=allKey.trim();
    	return allKey;
    }
   
    public synchronized String listAll(String key) throws InterruptedException {
        while (this.information.get(key).isEmpty()) {
            wait();
        }
        ArrayList<String> result = this.information.get(key);
        String message="";
        for (String s : result) {
        	message+=s+"\n";
        }
        notifyAll();

        return message;
    }
    
    public synchronized boolean containsKey(String key) {
        return this.information.containsKey(key);
    }
    
    
    public synchronized void addStringToKey(String key, String value) throws InterruptedException {
        while (!this.information.containsKey(key)) {
            wait();
        }
        this.information.get(key).add(value);
        notifyAll();
    }
    
    public synchronized String printAllStrings(String key) throws InterruptedException {
    	String x="";
    	while (this.information.get(key).isEmpty()) {
            wait();
        }
        ArrayList<String> result = this.information.get(key);
        for (String s : result) {
        	x+=s +"\n";
        }
        notifyAll();
        return x;
    }
}
