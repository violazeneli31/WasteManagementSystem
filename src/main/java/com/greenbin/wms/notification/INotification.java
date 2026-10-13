package com.greenbin.wms.notification;

import com.greenbin.wms.model.NotificationType;

import java.time.LocalDateTime;

/**
 * DECORATOR PATTERN - the component interface.
 * Both the plain notification and every decorator implement it, so a decorated
 * notification can be used anywhere a normal one is expected.
 */
public interface INotification {
    String getSubject();
    String getDescription();
    NotificationCategory getCategory();
    NotificationType getType();
    LocalDateTime getDate();

    /** The full text shown to the user. */
    default String showNotification() {
        return getSubject() + "\n" + getDescription();
    }
}
