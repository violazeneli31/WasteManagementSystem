package com.greenbin.wms.bins;

import java.time.LocalDateTime;
import java.util.UUID;

/** A record of something wrong in a bin (wrong waste, broken sensor...). */
public class WasteReport {
    private final UUID wasteReportID = UUID.randomUUID();
    private final String binName;
    private final String subject;
    private final String description;
    private final LocalDateTime timestamp;

    public WasteReport(String binName, String subject, String description, LocalDateTime timestamp) {
        this.binName = binName;
        this.subject = subject;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String showReport() { return "[" + binName + "] " + subject + ": " + description; }

    public UUID getWasteReportID() { return wasteReportID; }
    public String getBinName() { return binName; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
