package com.greenbin.wms.system;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Everything important the system does is written here (schedules, weather decisions, prices, alerts...). */
public class ActivityLog {
    /** One line of the log. */
    public static class Entry {
        public final LocalDateTime time;
        public final String category;
        public final String message;

        Entry(LocalDateTime time, String category, String message) {
            this.time = time;
            this.category = category;
            this.message = message;
        }

        @Override
        public String toString() {
            return time.format(DateTimeFormatter.ofPattern("dd/MM HH:mm:ss")) + "  [" + category + "] " + message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void add(String category, String message) {
        entries.add(0, new Entry(LocalDateTime.now(), category, message));
    }

    public List<Entry> getEntries() { return entries; }
}
