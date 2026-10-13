package com.greenbin.wms.finance;

import com.greenbin.wms.people.User;

import java.time.LocalDateTime;
import java.util.UUID;

/** FR_31: every money or points movement is logged as a transaction, so fraud can be found. */
public class Transaction {
    public enum Type { PAYMENT, REFUND, POINTS_AWARD, VOUCHER_REDEMPTION }
    public enum FraudStatus { NOT_CHECKED, OK, SUSPICIOUS, FRAUD, LEGITIMATE }

    private final UUID id = UUID.randomUUID();
    private final User user;
    private final Type type;
    private final double amount;          // Lek for payments/refunds, points for the others
    private final String reference;
    private final LocalDateTime time;
    private FraudStatus fraudStatus = FraudStatus.NOT_CHECKED;
    private String alertReason = "";

    public Transaction(User user, Type type, double amount, String reference, LocalDateTime time) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.reference = reference;
        this.time = time;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Type getType() { return type; }
    public double getAmount() { return amount; }
    public String getReference() { return reference; }
    public LocalDateTime getTime() { return time; }
    public FraudStatus getFraudStatus() { return fraudStatus; }
    public void setFraudStatus(FraudStatus s) { this.fraudStatus = s; }
    public String getAlertReason() { return alertReason; }
    public void setAlertReason(String reason) { this.alertReason = reason; }
}
