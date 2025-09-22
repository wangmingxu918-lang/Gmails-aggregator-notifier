package model;

import java.util.List;

import java.util.ArrayList;

public class MailFolder implements EmailComponent {
    private String name;
    private List<EmailComponent> children = new ArrayList<>();
    
    public MailFolder(String name) {
        this.name = name; 
    }
    
    public void add(EmailComponent c) {
        children.add(c); 
    }

    public void addEmail(Email email) {
        children.add(email);
    }

    public String getName() {
        return name;
    }

    
    public void display() {
        System.out.println("Folder: " + name);
        for (EmailComponent c : children) {
            c.display();
        }
    }

    public List<Email> getEmails() {
    List<Email> result = new ArrayList<>();
    for (EmailComponent child : children) {
        if (child instanceof Email mail) {
            result.add(mail);
        }
    }
    return result;
}
    public List<EmailComponent> getChildren() {
        return children;
    }

}
