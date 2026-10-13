package com.greenbin.wms;

import com.greenbin.wms.system.GreenBinSystem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The demo data the app starts with loads without problems and covers every part of the system. */
class DemoDataTest {

    @Test
    void demoDataLoads() {
        GreenBinSystem s = GreenBinSystem.resetDemo();
        assertSame(s, GreenBinSystem.getInstance());          // singleton
        assertEquals(6, s.getEmployees().size());
        assertEquals(7, s.getUsers().size());
        assertFalse(s.getSchedules().isEmpty());
        assertFalse(s.getBins().isEmpty());
        assertFalse(s.getPrograms().isEmpty());
        assertFalse(s.getBills().isEmpty());
        assertFalse(s.getFacility().getBatches().isEmpty());
        assertNotNull(s.login("viola", "password"));
        assertFalse(s.getManager().getNotifications().isEmpty());     // start-up alerts
        for (int i = 0; i < 20; i++) s.tick();                        // trucks can drive
    }
}
