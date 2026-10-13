package com.greenbin.wms.people;

import com.greenbin.wms.model.Role;
import com.greenbin.wms.notification.NotificationObserver;

/** Anyone who can log in: users (citizens, institutions) and employees. */
public interface Account extends NotificationObserver {
    String getUsername();
    String getDisplayName();
    Role getRole();
    boolean login(String username, String password);
}
