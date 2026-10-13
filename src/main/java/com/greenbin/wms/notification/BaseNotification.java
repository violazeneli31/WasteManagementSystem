package com.greenbin.wms.notification;

import com.greenbin.wms.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

/** DECORATOR PATTERN - the concrete component: a plain notification with a subject and text. */
public class BaseNotification implements INotification {
    private final UUID notificationID = UUID.randomUUID();
    private final String subject;
    private final String description;
    private final NotificationCategory category;
    private final NotificationType type;
    private final LocalDateTime date = LocalDateTime.now();

    public BaseNotification(String subject, String description, NotificationCategory category, NotificationType type) {
        this.subject = subject;
        this.description = description;
        this.category = category;
        this.type = type;
    }

    /** Short way to create a notification for a citizen. */
    public static BaseNotification forUser(String subject, String description, NotificationCategory category) {
        return new BaseNotification(subject, description, category, NotificationType.USER_INFORMATIVE);
    }

    /** Short way to create a notification for an employee. */
    public static BaseNotification forEmployee(String subject, String description, NotificationCategory category) {
        return new BaseNotification(subject, description, category, NotificationType.EMPLOYEE_COLLECTION);
    }

    public UUID getNotificationID() { return notificationID; }
    @Override public String getSubject() { return subject; }
    @Override public String getDescription() { return description; }
    @Override public NotificationCategory getCategory() { return category; }
    @Override public NotificationType getType() { return type; }
    @Override public LocalDateTime getDate() { return date; }
}
