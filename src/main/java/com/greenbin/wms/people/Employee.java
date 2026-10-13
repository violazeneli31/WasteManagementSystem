package com.greenbin.wms.people;

import com.greenbin.wms.model.Role;
import com.greenbin.wms.notification.INotification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Base class for every employee of the company. Employees are OBSERVERS too. */
public abstract class Employee implements Account {
    private final UUID empID = UUID.randomUUID();
    private final String name;
    private final String surname;
    private final String username;
    private final String passwordHash;
    private final String phoneNumber;
    private final String email;
    private final double wage;
    private final Role role;
    private final Set<LocalDate> daysOff = new TreeSet<>();          // leave / unavailable days
    private final List<LocalDate> attendance = new ArrayList<>();
    private final List<INotification> notifications = new ArrayList<>();

    protected Employee(String name, String surname, String username, String password,
                       String phoneNumber, String email, double wage, Role role) {
        this.name = name;
        this.surname = surname;
        this.username = username;
        this.passwordHash = PasswordHasher.hash(password);
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.wage = wage;
        this.role = role;
    }

    @Override
    public boolean login(String username, String password) {
        return this.username.equals(username) && PasswordHasher.matches(password, passwordHash);
    }

    public void logAttendance(LocalDate date) {
        if (!attendance.contains(date)) attendance.add(date);
    }

    /** availability is "AVAILABLE" or "ON_LEAVE" (as in the class diagram). */
    public void setAvailability(LocalDate date, boolean available) {
        if (available) daysOff.remove(date); else daysOff.add(date);
    }

    public boolean isAvailable(LocalDate date) { return !daysOff.contains(date); }

    @Override
    public void update(INotification notification) { notifications.add(0, notification); }

    @Override public List<INotification> getNotifications() { return notifications; }
    @Override public String getObserverName() { return getDisplayName(); }

    public UUID getEmpID() { return empID; }
    public String getName() { return name; }
    public String getSurname() { return surname; }
    @Override public String getUsername() { return username; }
    @Override public String getDisplayName() { return name + " " + surname; }
    @Override public Role getRole() { return role; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getEmail() { return email; }
    public double getWage() { return wage; }
    public Set<LocalDate> getDaysOff() { return daysOff; }
    public List<LocalDate> getAttendance() { return attendance; }

    @Override
    public String toString() { return getDisplayName(); }
}
