package model;

import java.util.List;

import javax.swing.JOptionPane;

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

    public String getName() {
        return name;
    }
    
    public void display() {
        System.out.println("Folder: " + name);
        for (EmailComponent c : children) {
            c.display();
        }
    }

    public List<EmailComponent> getChildren() {
        return children;
    }

    private static MailFolder createUserFolder() {
    String folderName = JOptionPane.showInputDialog("Enter folder name:");
    if (folderName == null || folderName.isBlank()) {
        return null; // user cancelled
    }
    return new MailFolder(folderName);
}

}
