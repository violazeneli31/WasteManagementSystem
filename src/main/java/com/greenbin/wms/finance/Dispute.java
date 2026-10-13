package com.greenbin.wms.finance;

import com.greenbin.wms.people.FinanceOfficer;
import com.greenbin.wms.people.User;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR_34: a complaint about a bill, missing reward points or a failed payment. */
public class Dispute {
    public enum Type { BILLING_ERROR, REWARD_POINTS, FAILED_PAYMENT }
    public enum Status { SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED }

    private final UUID id = UUID.randomUUID();
    private final User user;
    private final Type type;
    private final Bill bill;               // can be null for reward point disputes
    private final String description;
    private final String evidence;
    private final double claimedAmount;    // Lek to refund, or points to give back
    private final LocalDateTime submittedOn = LocalDateTime.now();
    private Status status = Status.SUBMITTED;
    private FinanceOfficer assignedOfficer;
    private String resolutionNote = "";

    public Dispute(User user, Type type, Bill bill, String description, String evidence, double claimedAmount) {
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Please describe the problem");
        if (claimedAmount < 0) throw new IllegalArgumentException("Amount can't be negative");
        this.user = user;
        this.type = type;
        this.bill = bill;
        this.description = description;
        this.evidence = evidence == null ? "" : evidence;
        this.claimedAmount = claimedAmount;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Type getType() { return type; }
    public Bill getBill() { return bill; }
    public String getDescription() { return description; }
    public String getEvidence() { return evidence; }
    public double getClaimedAmount() { return claimedAmount; }
    public LocalDateTime getSubmittedOn() { return submittedOn; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public FinanceOfficer getAssignedOfficer() { return assignedOfficer; }
    public void setAssignedOfficer(FinanceOfficer officer) { this.assignedOfficer = officer; }
    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String note) { this.resolutionNote = note; }
}
