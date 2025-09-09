package model;

public class Email implements EmailComponent {
     private String subject;
    private String sender;
    private String body;

    public Email(String subject, String sender, String body) {
        this.subject = subject;
        this.sender = sender;
        this.body = body;
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
}
