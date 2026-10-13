package com.greenbin.wms;

import com.greenbin.wms.notification.BaseNotification;
import com.greenbin.wms.notification.EventReminderNotification;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.notification.NotificationCategory;
import com.greenbin.wms.notification.RewardNotification;
import com.greenbin.wms.notification.UrgentNotification;
import com.greenbin.wms.programs.CommunityEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Decorator and Observer patterns, FR_05 and FR_21. */
class NotificationTest {
    private final TestData d = new TestData();

    @Test
    void decoratorsAddTextWithoutChangingTheBase() {
        INotification base = BaseNotification.forUser("Collection tomorrow", "06:00", NotificationCategory.SCHEDULE);
        CommunityEvent event = new CommunityEvent("Earth Day", "", "Grand Park", LocalDateTime.now().plusDays(5), 50, "clean");
        INotification decorated = new UrgentNotification(new EventReminderNotification(new RewardNotification(base, 10, 110), event));

        assertEquals("[URGENT] Collection tomorrow", decorated.getSubject());
        assertTrue(decorated.getDescription().startsWith("06:00"));
        assertTrue(decorated.getDescription().contains("+10 points"));
        assertTrue(decorated.getDescription().contains("Earth Day"));
        assertEquals(NotificationCategory.SCHEDULE, decorated.getCategory());
        assertEquals("06:00", base.getDescription());     // the original is unchanged
    }

    @Test
    void observersReceiveOnlyCategoriesTheyWant() {
        d.ben.setNotificationPreference(NotificationCategory.REWARDS, false);
        int anaBefore = d.ana.getNotifications().size();
        int benBefore = d.ben.getNotifications().size();
        INotification n = BaseNotification.forUser("Points", "You got points", NotificationCategory.REWARDS);

        assertTrue(d.sys.getNotificationCenter().notify(d.ana, n));
        assertFalse(d.sys.getNotificationCenter().notify(d.ben, n));
        assertEquals(anaBefore + 1, d.ana.getNotifications().size());
        assertEquals(benBefore, d.ben.getNotifications().size());
    }

    @Test
    void broadcastReachesEverySubscribedObserver() {
        int sent = d.sys.getNotificationCenter().broadcast(BaseNotification.forUser("Hello", "", NotificationCategory.EVENTS));
        assertEquals(d.sys.getUsers().size() + d.sys.getEmployees().size(), sent);
    }
}
