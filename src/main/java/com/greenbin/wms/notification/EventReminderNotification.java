package com.greenbin.wms.notification;

import com.greenbin.wms.programs.Program;

import java.time.format.DateTimeFormatter;

/** DECORATOR PATTERN - adds a reminder about an event or program to any notification. */
public class EventReminderNotification extends NotificationDecorator {
    private final Program program;

    public EventReminderNotification(INotification wrapped, Program program) {
        super(wrapped);
        this.program = program;
    }

    @Override
    public String getDescription() {
        return wrapped.getDescription() + "\n[Reminder] " + program.getName() + " on "
                + program.getDate().format(DateTimeFormatter.ofPattern("dd MMM, HH:mm")) + " at " + program.getLocation();
    }

    public Program getProgram() { return program; }
}
