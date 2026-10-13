package com.greenbin.wms.gui.screens;

import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.NotificationList;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.notification.NotificationCategory;
import com.greenbin.wms.people.Account;

import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/** Every notification the logged-in person received (OBSERVER), with the decorators each one has (DECORATOR). */
public class InboxScreen extends Screen {
    private final NotificationList list = new NotificationList();
    private final JComboBox<Object> filter = new JComboBox<>();

    public InboxScreen(Account me) {
        super(me, "Notifications", "Messages sent to you by the system. Purple tags show the decorators added to a notification.",
                "FR_05", "FR_21");
        filter.addItem("All categories");
        for (NotificationCategory c : NotificationCategory.values()) filter.addItem(c);
        filter.addActionListener(e -> refresh());
        body().add(row(muted("Show:"), filter), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getViewport().setBackground(Theme.BG);
        body().add(sp, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        List<INotification> shown = new ArrayList<>();
        for (INotification n : me.getNotifications()) {
            if (!(filter.getSelectedItem() instanceof NotificationCategory) || n.getCategory() == filter.getSelectedItem()) shown.add(n);
        }
        list.setNotifications(shown);
    }
}
