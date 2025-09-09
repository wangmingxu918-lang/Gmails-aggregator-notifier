package model;

import java.util.List;

// Iterator for emails in a folder
public class EmailIterator {
    private List<EmailComponent> emails;
    private int position = 0;

    public EmailIterator(MailFolder folder) {
        this.emails = folder.getChildren();
    }

    public boolean hasNext() {
        return position < emails.size();
    }

    public EmailComponent next() {
        if (!hasNext()) return null;
        return emails.get(position++);
    }

    public boolean hasPrevious() {
        return position > 0;
    }

    public EmailComponent previous() {
        if (!hasPrevious()) return null;
        return emails.get(--position);
    }

    public void reset() {
        position = 0;
    }
}
