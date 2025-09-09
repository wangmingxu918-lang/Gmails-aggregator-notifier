package model;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.*;

public class EmailClient {
    private static EmailClient instance;
    private Session session;
    private Store store;
    private MailFolder inboxFolder;
    private List<SortingRule> rules = new ArrayList<>();
    private List<EmailObserver> observers = new ArrayList<>();

    private EmailClient() {
        inboxFolder = new MailFolder("Inbox");
    }

    public static EmailClient getInstance() {
        if (instance == null) {
            instance = new EmailClient();
        }
        return instance;
    }

    public List<SortingRule> getRules() {
        return rules;
    }

    public List<EmailObserver> getObservers() {
        return observers;
    }

    public void addRule(SortingRule rule) {
        rules.add(rule);
    }

    public void addObserver(EmailObserver observer) {
        observers.add(observer);
    }

    private void notifyObservers(Email email, MailFolder folder) {
        for (EmailObserver obs : observers) {
            obs.update(email, folder);
        }
    }

    public MailFolder getInboxFolder() {
        return inboxFolder;
    }

    // ✅ Connect to IMAP server
    public void connect(String host, String username, String password) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");

        session = Session.getDefaultInstance(props, null);
        store = session.getStore("imaps");
        store.connect(host, username, password);
    }

    // ✅ Fetch and convert messages
    public void fetchEmails() throws MessagingException, IOException {
        javax.mail.Folder jxInbox = store.getFolder("INBOX");
        jxInbox.open(javax.mail.Folder.READ_ONLY);

        Message[] messages = jxInbox.getMessages();
        for (Message msg : messages) {
            String subject = msg.getSubject();
            String from = msg.getFrom()[0].toString();
            String body = getTextFromMessage(msg);

            Email email = new Email(from, subject, body);

            // Sort into user-defined folders
            MailFolder targetFolder = inboxFolder;
            for (SortingRule rule : rules) {
                if (subject != null && subject.toLowerCase().contains(rule.getKeyword().toLowerCase())) {
                    rule.getTargetFolder().add(email);
                    notifyObservers(email, rule.getTargetFolder());
                    targetFolder = null;
                    break;
                }
            }

            // If no rule matched → stay in inbox
            if (targetFolder != null) {
                inboxFolder.add(email);
                notifyObservers(email, inboxFolder);
            }
        }
        jxInbox.close(false);
    }

    // ✅ Helper: Extract plain text body from email
    private String getTextFromMessage(Message message) throws MessagingException, IOException {
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString();
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart);
        }
        return "";
    }

    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            }
        }
        return result.toString();
    }
}
