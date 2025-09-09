package model;

import java.util.ArrayList;
import java.util.List;

public abstract class Subject {
    protected List<EmailObserver> observers = new ArrayList<>();

    public void addObserver(EmailObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(EmailObserver observer) {
        observers.remove(observer);
    }

    // Notify all observers
    protected void notifyObservers(Email email, MailFolder folder) {
        for (EmailObserver obs : observers) {
            obs.update(email, folder);
        }
    }
}
