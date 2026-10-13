package com.greenbin.wms.community;

import com.greenbin.wms.people.Account;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FR_20: an issue or a feedback sent by a citizen or an employee.
 * The system puts it in a category automatically and sends it to the right department.
 */
public class Report {
    public enum Status { OPEN, ASSIGNED, RESOLVED }

    public enum Category {
        SUGGESTION("Management", "suggest", "idea", "improve", "feature", "would be nice"),
        BILLING("Finance office", "bill", "pay", "price", "charge", "refund", "money"),
        BIN_ISSUE("Bin maintenance", "bin", "overflow", "full", "broken", "sensor", "smell"),
        MISSED_COLLECTION("Collection team", "missed", "collection", "truck", "late", "pickup", "road"),
        OTHER("Customer support");

        private final String department;
        private final String[] keywords;

        Category(String department, String... keywords) {
            this.department = department;
            this.keywords = keywords;
        }

        public String getDepartment() { return department; }

        /** Finds the category from the words used in the report. */
        public static Category categorize(String text) {
            String lower = text.toLowerCase();
            for (Category c : values()) {
                for (String k : c.keywords) if (lower.contains(k)) return c;
            }
            return OTHER;
        }
    }

    private final UUID reportID = UUID.randomUUID();
    private final Account author;
    private final String subject;
    private final String description;
    private final Category category;
    private final LocalDateTime timestamp;
    private Status status = Status.OPEN;
    private String assignedTo = "";
    private String response = "";

    public Report(Account author, String subject, String description, LocalDateTime timestamp) {
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("Please write a subject");
        this.author = author;
        this.subject = subject.trim();
        this.description = description == null ? "" : description.trim();
        this.category = Category.categorize(subject + " " + description);
        this.timestamp = timestamp;
    }

    public String showReport() { return "[" + category.getDepartment() + "] " + subject + " - " + status; }

    public UUID getReportID() { return reportID; }
    public Account getAuthor() { return author; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public Category getCategory() { return category; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getResponse() { return response; }
    public void setResponse(String response) { this.response = response; }
}
