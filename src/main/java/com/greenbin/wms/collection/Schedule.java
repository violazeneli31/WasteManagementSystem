package com.greenbin.wms.collection;

import com.greenbin.wms.people.GarbageCollectionEmployee;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** One collection shift: an area, a day, a time slot, an employee and a truck. */
public class Schedule {
    public enum Status { PLANNED, CONFIRMED, MOVED, NEEDS_MANAGER, DONE }

    private static int nextScheduleNumber = 1;

    private int scheduleNumber;        // given the first time it is needed, so test shifts don't use numbers
    private String area;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private GarbageCollectionEmployee employee;
    private Vehicle assignedVehicle;
    private boolean special;           // special pickup / full bin, not routine
    private Status status = Status.PLANNED;
    private String note = "";

    public Schedule(String area, LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.area = area;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /** True if both schedules are on the same day and their times cross. */
    public boolean overlapsWith(Schedule other) {
        return date.equals(other.date) && startTime.isBefore(other.endTime) && other.startTime.isBefore(endTime);
    }

    public void moveTo(LocalDate newDate) {
        this.date = newDate;
    }

    public String showSchedule() {
        return "#" + getScheduleNumber() + " " + area + " - " + date.format(DateTimeFormatter.ofPattern("EEE dd MMM"))
                + " " + startTime + "-" + endTime
                + (employee != null ? " (" + employee.getDisplayName() + ")" : " (not assigned)");
    }

    public int getScheduleNumber() {
        if (scheduleNumber == 0) scheduleNumber = nextScheduleNumber++;
        return scheduleNumber;
    }
    public String getArea() { return area; }
    public LocalDate getDate() { return date; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setTimes(LocalTime start, LocalTime end) { this.startTime = start; this.endTime = end; }
    public GarbageCollectionEmployee getEmployee() { return employee; }
    public void setEmployee(GarbageCollectionEmployee employee) { this.employee = employee; }
    public Vehicle getAssignedVehicle() { return assignedVehicle; }
    public void setAssignedVehicle(Vehicle vehicle) { this.assignedVehicle = vehicle; }
    public boolean isSpecial() { return special; }
    public void setSpecial(boolean special) { this.special = special; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    @Override
    public String toString() { return showSchedule(); }
}
