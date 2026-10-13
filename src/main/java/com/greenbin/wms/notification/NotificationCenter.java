package com.greenbin.wms.notification;

import java.util.ArrayList;
import java.util.List;

/**
 * OBSERVER PATTERN - the subject.
 * Observers subscribe once; when something changes in the system (a schedule moves,
 * a bin is full, a new event...) the center notifies them.
 * The user's preferences (FR_21) are respected before delivering.
 */
public class NotificationCenter {
    private final List<NotificationObserver> observers = new ArrayList<>();
    private final List<String> deliveryLog = new ArrayList<>();

    public void subscribe(NotificationObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    public void unsubscribe(NotificationObserver observer) {
        observers.remove(observer);
    }

    public List<NotificationObserver> getObservers() { return observers; }

    /** Sends a notification to one observer. Returns false if they turned this category off. */
    public boolean notify(NotificationObserver observer, INotification notification) {
        if (!observer.wantsNotification(notification.getCategory())) {
            deliveryLog.add("Skipped " + observer.getObserverName() + " (turned off " + notification.getCategory().getLabel() + ")");
            return false;
        }
        observer.update(notification);
        deliveryLog.add("Sent '" + notification.getSubject() + "' to " + observer.getObserverName());
        return true;
    }

    /** Sends a notification to a group of observers. Returns how many received it. */
    public int notifyAll(List<? extends NotificationObserver> group, INotification notification) {
        int count = 0;
        for (NotificationObserver o : group) {
            if (notify(o, notification)) count++;
        }
        return count;
    }

    /** Sends a notification to every subscribed observer. */
    public int broadcast(INotification notification) {
        return notifyAll(new ArrayList<>(observers), notification);
    }

    public List<String> getDeliveryLog() { return deliveryLog; }
}
