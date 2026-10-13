package com.greenbin.wms.notification;

import com.greenbin.wms.model.NotificationType;

import java.time.LocalDateTime;

/**
 * DECORATOR PATTERN - the abstract decorator.
 * It wraps another notification and passes every call to it.
 * Subclasses only change the parts they want to extend.
 */
public abstract class NotificationDecorator implements INotification {
    protected final INotification wrapped;

    protected NotificationDecorator(INotification wrapped) {
        this.wrapped = wrapped;
    }

    public INotification getWrapped() { return wrapped; }

    @Override public String getSubject() { return wrapped.getSubject(); }
    @Override public String getDescription() { return wrapped.getDescription(); }
    @Override public NotificationCategory getCategory() { return wrapped.getCategory(); }
    @Override public NotificationType getType() { return wrapped.getType(); }
    @Override public LocalDateTime getDate() { return wrapped.getDate(); }
}
