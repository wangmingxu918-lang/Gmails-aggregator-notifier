package model;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.*;

public class EmailClient {
    private static EmailClient instance;
    private Store store;
    private Session session;
    private MailFolder inboxFolder;
    private Set<String> knownEmails = new HashSet<>();
    private List<MailFolder> folders = new ArrayList<>();
    private List<SortingRule> rules = new ArrayList<>();
    private List<EmailObserver> observers = new ArrayList<>();

    private EmailClient() {
        inboxFolder = new MailFolder("Inbox");
        folders.add(inboxFolder);
    }

    public static EmailClient getInstance() {
        if (instance == null) {
            instance = new EmailClient();
        }
        return instance;
    }

    public List<MailFolder> getFolders() { return folders; }
    public void addFolder(MailFolder folder) { folders.add(folder); }
    public void addRule(SortingRule rule) { rules.add(rule); }
    public void addObserver(EmailObserver observer) { observers.add(observer); }

    public MailFolder getInboxFolder() { return inboxFolder; }

    private void notifyObservers(Email email, MailFolder folder) {
        for (EmailObserver obs : observers) {
            obs.update(email, folder);
        }
    }

    // Connect to Gmail IMAP
    public void connect(String host, String username, String password) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        session = Session.getDefaultInstance(props, null);
        store = session.getStore("imaps");
        store.connect(host, username, password);
    }

    // Call this after fetchAllEmails() at startup
    public void markExistingEmailsAsKnown() {
        for (MailFolder folder : folders) {
            for (Email email : folder.getEmails()) {
                knownEmails.add(email.getUniqueId());
            }
        }
    }


    // Fetch all emails at startup
    public void fetchAllEmails(String host, String username, String password) {
        try {
            if (store == null || !store.isConnected()) connect(host, username, password);

            Folder imapInbox = store.getFolder("INBOX");
            imapInbox.open(Folder.READ_ONLY);

            Message[] messages = imapInbox.getMessages();
            for (Message msg : messages) {
                String body = getTextFromMessage(msg);
                Email email = new Email(msg.getSubject(), msg.getFrom()[0].toString(), body);
                inboxFolder.addEmail(email);
                knownEmails.add(email.getUniqueId());
            }
            imapInbox.close(false);

            // Apply rules retroactively
            applyRulesToAllEmails();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error in fetchAllEmails: " + e.getMessage());
        }
    }

    // Auto-refresh to check new emails
    public void startAutoRefresh(String host, String username, String password, int intervalMs) {
        new Timer(intervalMs, e -> {
            List<Email> newEmails = fetchNewEmails(host, username, password);
            for (Email email : newEmails) {
                if (!knownEmails.contains(email.getUniqueId())) {
                    knownEmails.add(email.getUniqueId());
                    MailFolder target = applyRules(email);
                    notifyObservers(email, target);
                }
            }
        }).start();
    }

    // Fetch only new emails since last check
    public List<Email> fetchNewEmails(String host, String username, String password) {
        List<Email> newEmails = new ArrayList<>();
        try {
            if (store == null || !store.isConnected()) connect(host, username, password);

            Folder imapInbox = store.getFolder("INBOX");
            imapInbox.open(Folder.READ_ONLY);

            Message[] messages = imapInbox.getMessages();
            for (Message msg : messages) {
                String body = getTextFromMessage(msg);
                Email email = new Email(msg.getSubject(), msg.getFrom()[0].toString(), body);
                if (!knownEmails.contains(email.getUniqueId())) {
                    inboxFolder.addEmail(email); // default folder
                    newEmails.add(email);
                }
            }
            imapInbox.close(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return newEmails;
    }

    // Apply rules to a single email and return the folder it belongs to
    public MailFolder applyRules(Email email) {
        for (SortingRule rule : rules) {
            if (rule.matches(email)) {
                inboxFolder.getChildren().remove(email);
                rule.getTargetFolder().addEmail(email);
                return rule.getTargetFolder();
            }
        }
        return inboxFolder;
    }

    // Apply rules retroactively to all emails
    public void applyRulesToAllEmails() {
        List<Email> allEmails = new ArrayList<>(inboxFolder.getEmails());
        for (Email email : allEmails) {
            applyRules(email);
        }
    }

    // Helper: get text content from message
    public String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain") || message.isMimeType("text/html")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            return getTextFromMimeMultipart((MimeMultipart) message.getContent());
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain") || bodyPart.isMimeType("text/html")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    public MailFolder getFolderByName(String name) {
        for (MailFolder folder : folders) {
            if (folder.getName().equalsIgnoreCase(name)) return folder;
        }
        return inboxFolder;
    }

    public Store getStore() {
        return store;
    }

    public Session getSession() {
        return session;
    }

    public Set<String> getKnownEmails() {
        return knownEmails;
    }

    public List<SortingRule> getRules() {
        return rules;
    }

    public List<EmailObserver> getObservers() {
        return observers;
    }

    
}
