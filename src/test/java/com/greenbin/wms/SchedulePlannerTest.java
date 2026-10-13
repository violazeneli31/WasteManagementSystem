package com.greenbin.wms;

import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.notification.INotification;
import com.greenbin.wms.weather.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** FR_02 suggested schedules, FR_03 schedule & plan, FR_05 notifications, FR_07 weather. */
class SchedulePlannerTest {
    private TestData d;
    private LocalDate monday;

    @BeforeEach
    void setUp() {
        d = new TestData();
        monday = d.sys.nextMonday();
        for (int i = 0; i < 7; i++) {
            d.sys.getStatistics().addData("Blloku", 650);
            d.sys.getStatistics().addData("Kombinat", 240);
        }
    }

    @Test
    void suggestionSkipsEmployeesOnLeaveAndHasNoOverlaps() {
        d.elira.setAvailability(monday, false);
        List<Schedule> week = d.sys.suggestWeek(monday);
        assertEquals(4, week.size());                         // Blloku 3 + Kombinat 1
        for (Schedule s : week) {
            assertNotNull(s.getEmployee());
            if (s.getDate().equals(monday)) assertNotSame(d.elira, s.getEmployee());
            for (Schedule o : week) {
                if (o != s && o.getEmployee() == s.getEmployee()) assertFalse(o.overlapsWith(s));
            }
        }
    }

    @Test
    void confirmingSchedulesNotifiesTheEmployees() {
        int before = d.ardi.getNotifications().size() + d.elira.getNotifications().size();
        int saved = d.sys.confirmSchedules(d.sys.suggestWeek(monday));
        assertEquals(4, saved);
        assertEquals(before + 4, d.ardi.getNotifications().size() + d.elira.getNotifications().size());
    }

    @Test
    void findsProblemsWithAShift() {
        d.sys.addManualSchedule("Blloku", monday, LocalTime.of(6, 0), LocalTime.of(9, 0), d.ardi, d.sys.getVehicles().get(0));
        List<String> overlap = d.sys.addManualSchedule("Kombinat", monday, LocalTime.of(8, 0), LocalTime.of(11, 0), d.ardi, null);
        assertTrue(overlap.stream().anyMatch(p -> p.contains("already has a shift")));

        d.elira.setAvailability(monday, false);
        List<String> leave = d.sys.addManualSchedule("Kombinat", monday, LocalTime.of(9, 0), LocalTime.of(12, 0), d.elira, null);
        assertTrue(leave.stream().anyMatch(p -> p.contains("on leave")));

        List<String> sunday = d.sys.addManualSchedule("Kombinat", monday.plusDays(6), LocalTime.of(9, 0), LocalTime.of(12, 0), d.elira, null);
        assertTrue(sunday.stream().anyMatch(p -> p.contains("Sunday")));
    }

    @Test
    void stormWithManagerPresentWaitsForTheManager() {
        LocalDate wednesday = monday.with(DayOfWeek.WEDNESDAY);      // the simulated weather has storms on Wednesdays
        d.sys.addManualSchedule("Blloku", wednesday, LocalTime.of(6, 0), LocalTime.of(9, 0), d.ardi, null);
        d.manager.setPresent(true);
        d.sys.adjustForWeather();
        assertEquals(Schedule.Status.NEEDS_MANAGER, d.sys.getSchedules().get(0).getStatus());
        assertEquals(wednesday, d.sys.getSchedules().get(0).getDate());
    }

    @Test
    void stormWithManagerAwayIsMovedAutomaticallyAndEmployeeIsTold() {
        LocalDate wednesday = monday.with(DayOfWeek.WEDNESDAY);
        d.sys.addManualSchedule("Blloku", wednesday, LocalTime.of(6, 0), LocalTime.of(9, 0), d.ardi, null);
        d.manager.setPresent(false);
        d.sys.adjustForWeather();
        Schedule s = d.sys.getSchedules().get(0);
        assertEquals(Schedule.Status.MOVED, s.getStatus());
        assertFalse(d.sys.getWeather().isSevereWeatherExpected("Tirana", s.getDate()));
        INotification last = d.ardi.getNotifications().get(0);
        assertTrue(last.getSubject().contains("Storm"));
    }

    @Test
    void managerCannotMoveAShiftIntoAnotherStorm() {
        d.sys.addManualSchedule("Blloku", monday, LocalTime.of(6, 0), LocalTime.of(9, 0), d.ardi, null);
        d.sys.getWeather().setCondition(monday.plusDays(1), WeatherData.Condition.HEAVY_SNOW);
        List<String> problems = d.sys.moveSchedule(d.sys.getSchedules().get(0), monday.plusDays(1));
        assertFalse(problems.isEmpty());
        assertEquals(monday, d.sys.getSchedules().get(0).getDate());
    }

    @Test
    void assigningAnEmployeeChecksAvailability() {
        d.sys.addManualSchedule("Blloku", monday, LocalTime.of(6, 0), LocalTime.of(9, 0), d.ardi, null);
        Schedule s = d.sys.getSchedules().get(0);
        d.elira.setAvailability(monday, false);
        assertFalse(d.sys.assignEmployee(s, d.elira).isEmpty());
        assertSame(d.ardi, s.getEmployee());
        d.elira.setAvailability(monday, true);
        assertTrue(d.sys.assignEmployee(s, d.elira).isEmpty());
        assertSame(d.elira, s.getEmployee());
        assertEquals("Shift removed", d.ardi.getNotifications().get(0).getSubject());
    }
}
