package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    int id;
    LocalDateTime date;
    String text;
    int publisherId;
    
    public Message(int id, String text, int publisherId) {
        this.id = id;
        this.text = text;
        this.date = LocalDateTime.now();
        this.publisherId = publisherId;
    }
    
    public int getId() {
        return id;
    }
    
    public String getText() {
        return text;
    }
    
    public LocalDateTime getDate() {
        return date;
    }
    
    public void setId(int newId) {
        this.id = newId;
    }
    
    public void setText(String newText) {
        this.text = newText;
    }
    
    public void setDate(LocalDateTime newDate) {
        this.date = newDate;
    }
    
    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return "-Id: " + id + "\n" +
               "Testo: " + text + "\n" +
               "Data: " + date.format(formatter) + "\n";
    }
}
