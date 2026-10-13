package com.greenbin.wms.planning;

import com.greenbin.wms.collection.Schedule;
import com.greenbin.wms.collection.Vehicle;
import com.greenbin.wms.people.GarbageCollectionEmployee;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * FR_02 / FR_03: creates and checks collection schedules.
 * Shifts are 3 hours long. Employees and trucks are shared fairly:
 * the one with the fewest shifts that is free gets the next one.
 */
public class SchedulePlanner {
    public static final LocalTime[] SLOT_STARTS = {
            LocalTime.of(6, 0), LocalTime.of(9, 0), LocalTime.of(12, 0), LocalTime.of(15, 0)
    };
    public static final int SHIFT_HOURS = 3;

    private final List<Schedule> schedules;
    private final List<GarbageCollectionEmployee> employees;
    private final List<Vehicle> vehicles;

    public SchedulePlanner(List<Schedule> schedules, List<GarbageCollectionEmployee> employees, List<Vehicle> vehicles) {
        this.schedules = schedules;
        this.employees = employees;
        this.vehicles = vehicles;
    }

    /** FR_02: a week of schedules (Monday to Saturday) based on the statistics. Not saved yet. */
    public List<Schedule> suggestCollectionSchedules(Statistics statistics, LocalDate weekStart) {
        List<Schedule> suggestion = new ArrayList<>();
        for (String area : statistics.getAreas()) {
            int pickups = Math.min(6, statistics.calculateOptimalPickups(area));
            for (int i = 0; i < pickups; i++) {
                LocalDate day = weekStart.plusDays((long) i * 6 / pickups);    // spread over the week
                suggestion.add(createShift(area, day, suggestion));
            }
        }
        return suggestion;
    }

    /** Creates a shift on a day, in the first slot where an employee and a truck are free. */
    public Schedule createShift(String area, LocalDate day, List<Schedule> alsoPlanned) {
        List<Schedule> all = new ArrayList<>(schedules);
        all.addAll(alsoPlanned);
        Schedule s = new Schedule(area, day, SLOT_STARTS[0], SLOT_STARTS[0].plusHours(SHIFT_HOURS));
        for (LocalTime start : SLOT_STARTS) {
            s.setTimes(start, start.plusHours(SHIFT_HOURS));
            GarbageCollectionEmployee e = pickEmployee(s, all);
            Vehicle v = pickVehicle(s, all);
            if (e != null && v != null) {
                s.setEmployee(e);
                s.setAssignedVehicle(v);
                return s;
            }
        }
        s.setTimes(SLOT_STARTS[0], SLOT_STARTS[0].plusHours(SHIFT_HOURS));
        s.setNote("No free employee - assign manually");
        return s;
    }

    /** The free employee with the fewest shifts (fair workload). */
    public GarbageCollectionEmployee pickEmployee(Schedule shift, List<Schedule> all) {
        GarbageCollectionEmployee best = null;
        for (GarbageCollectionEmployee e : employees) {
            if (!e.isAvailable(shift.getDate()) || isBusy(e, shift, all)) continue;
            if (best == null || countShifts(e, all) < countShifts(best, all)) best = e;
        }
        return best;
    }

    private Vehicle pickVehicle(Schedule shift, List<Schedule> all) {
        for (Vehicle v : vehicles) {
            boolean busy = false;
            for (Schedule s : all) {
                if (s != shift && s.getAssignedVehicle() == v && s.overlapsWith(shift)) busy = true;
            }
            if (!busy) return v;
        }
        return null;
    }

    private boolean isBusy(GarbageCollectionEmployee e, Schedule shift, List<Schedule> all) {
        for (Schedule s : all) {
            if (s != shift && s.getEmployee() == e && s.overlapsWith(shift)) return true;
        }
        return false;
    }

    public int countShifts(GarbageCollectionEmployee e, List<Schedule> all) {
        int count = 0;
        for (Schedule s : all) if (s.getEmployee() == e) count++;
        return count;
    }

    /** FR_03 step 6: every problem with a schedule (empty list = valid). */
    public List<String> findProblems(Schedule s) {
        List<String> problems = new ArrayList<>();
        if (!s.getStartTime().isBefore(s.getEndTime())) problems.add("The shift must start before it ends.");
        if (s.getDate().getDayOfWeek() == DayOfWeek.SUNDAY) problems.add("No collections on Sunday.");
        if (s.getEmployee() == null) {
            problems.add("No employee assigned.");
        } else {
            if (!s.getEmployee().isAvailable(s.getDate())) problems.add(s.getEmployee() + " is on leave that day.");
            if (isBusy(s.getEmployee(), s, schedules)) problems.add(s.getEmployee() + " already has a shift at that time.");
        }
        if (s.getAssignedVehicle() != null) {
            for (Schedule other : schedules) {
                if (other != s && other.getAssignedVehicle() == s.getAssignedVehicle() && other.overlapsWith(s)) {
                    problems.add("Truck " + s.getAssignedVehicle() + " is already used at that time.");
                    break;
                }
            }
        }
        return problems;
    }

    public boolean checkScheduleValidity(Schedule s) { return findProblems(s).isEmpty(); }

    /** The first day after the given one (max one week later) that is not Sunday and passes the test. */
    public LocalDate findNextDay(LocalDate from, java.util.function.Predicate<LocalDate> isGoodDay) {
        for (int i = 1; i <= 7; i++) {
            LocalDate d = from.plusDays(i);
            if (d.getDayOfWeek() != DayOfWeek.SUNDAY && isGoodDay.test(d)) return d;
        }
        return null;
    }

    public List<Schedule> getSchedules() { return schedules; }
    public List<GarbageCollectionEmployee> getEmployees() { return employees; }
}
