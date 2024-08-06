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

}
