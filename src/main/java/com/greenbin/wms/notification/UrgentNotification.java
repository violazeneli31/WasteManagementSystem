package com.greenbin.wms.notification;

/** DECORATOR PATTERN - marks a notification as urgent (storms, full bins, fraud...). */
public class UrgentNotification extends NotificationDecorator {

    public UrgentNotification(INotification wrapped) {
        super(wrapped);
    }

    @Override
    public String getSubject() {
        return "[URGENT] " + wrapped.getSubject();
    }
}
