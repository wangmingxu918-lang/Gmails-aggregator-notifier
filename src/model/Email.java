package model;

import java.util.Date;

public class Email implements EmailComponent {
    private String subject;
    private String sender;
    private String body;
    private Date date;
    private String serverId;

    public Email(String subject, String sender, String body, Date date) {
        this.subject = subject;
        this.sender = sender;
        this.body = body;
        this.date = date;
    }

    // Old constructor for backward compatibility
    public Email(String subject, String sender, String body) {
        this(subject, sender, body, new Date());
    }

    @Override
    public void display() {
        System.out.println("From: " + sender + " | Subject: " + subject);
    }

    public String getContent() {
        return subject + " " + body;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Date getDate() {
        return date;
    }

     public void setServerId(String id) {
        this.serverId = id;
    }

    public String getServerId() {
        return this.serverId;
    }
    
}
