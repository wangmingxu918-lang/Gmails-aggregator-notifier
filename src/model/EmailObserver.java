package model;

public interface EmailObserver {
    void update(Email email, MailFolder folder);
}
