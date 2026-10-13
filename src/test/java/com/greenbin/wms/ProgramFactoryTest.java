package com.greenbin.wms;

import com.greenbin.wms.model.ProgramType;
import com.greenbin.wms.notification.EventReminderNotification;
import com.greenbin.wms.programs.CommunityEvent;
import com.greenbin.wms.programs.EducationalProgram;
import com.greenbin.wms.programs.EducationalProgramFactory;
import com.greenbin.wms.programs.EventFactory;
import com.greenbin.wms.programs.Program;
import com.greenbin.wms.programs.ProgramFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** Factory pattern, FR_15 events and FR_16 educational programs. */
class ProgramFactoryTest {
    private final TestData d = new TestData();
    private final LocalDateTime nextWeek = LocalDateTime.now().plusDays(7);

    @Test
    void eachFactoryMakesItsOwnType() {
        assertTrue(new EventFactory().createProgram("Clean-up", "x", "Park", nextWeek, 10, "goal") instanceof CommunityEvent);
        assertTrue(new EducationalProgramFactory().createProgram("Lesson", "x", "School", nextWeek, 10, "kids") instanceof EducationalProgram);
        assertTrue(ProgramFactory.forType(ProgramType.EVENT) instanceof EventFactory);
    }

    @Test
    void factoryChecksTheDataAndFillsDefaults() {
        ProgramFactory f = new EventFactory();
        assertThrows(IllegalArgumentException.class, () -> f.createProgram("", "x", "Park", nextWeek, 10, ""));
        assertThrows(IllegalArgumentException.class, () -> f.createProgram("A", "x", "Park", nextWeek, 0, ""));
        assertThrows(IllegalArgumentException.class, () -> f.createProgram("A", "x", "Park", LocalDateTime.now().minusDays(1), 10, ""));
        CommunityEvent e = (CommunityEvent) f.createProgram("A", "", "Park", nextWeek, 10, "");
        assertEquals("More details soon.", e.getDescription());
        assertEquals("Keep our city clean", e.getObjective());
    }

    @Test
    void creatingAProgramInvitesEveryCitizen() {
        Program p = d.sys.createProgram(ProgramType.EVENT, "Earth Day", "Clean the park", "Grand Park", nextWeek, 2, "500 kg");
        assertTrue(d.ana.getNotifications().get(0) instanceof EventReminderNotification);
        assertTrue(d.sys.joinProgram(d.ana, p));
        assertFalse(d.sys.joinProgram(d.ana, p));        // already registered
        assertTrue(d.sys.joinProgram(d.ben, p));
        assertTrue(p.isFull());
    }

    @Test
    void attendanceAndFeedback() {
        Program p = d.sys.createProgram(ProgramType.EDUCATIONAL_PROGRAM, "Recycling 101", "", "School 21", nextWeek, 30, "pupils");
        d.sys.joinProgram(d.ana, p);
        assertFalse(p.markAttendance(d.ben));            // did not register
        assertTrue(p.markAttendance(d.ana));
        assertEquals(100, p.getAttendanceRate(), 0.001);
        p.addFeedback(d.ana, 4, "good");
        assertEquals(4, p.getAverageRating(), 0.001);
        assertThrows(IllegalArgumentException.class, () -> p.addFeedback(d.ana, 9, "?"));
    }
}
