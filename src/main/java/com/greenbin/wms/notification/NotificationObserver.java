package com.greenbin.wms.notification;

import java.util.List;

/**
 * OBSERVER PATTERN - the observer.
 * Users and employees implement it, so the notification center can update them
 * without knowing what kind of person they are.
 */
public interface NotificationObserver {
    void update(INotification notification);

    String getObserverName();

    List<INotification> getNotifications();

    /** FR_21: an observer can turn categories off. By default everything is received. */
    default boolean wantsNotification(NotificationCategory category) { return true; }
}
