package model;

public class ConsoleUI implements EmailObserver {
    
    @Override
    public void update(Email email, MailFolder folder) {
        System.out.println("📩 New email sorted into folder: " + folder.getName());
        System.out.println("From: " + email.getSender());
        System.out.println("Subject: " + email.getSubject());
        System.out.println("Body: " + email.getBody());
        System.out.println("-----------------------------");
    }
}
