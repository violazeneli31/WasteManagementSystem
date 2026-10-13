package com.greenbin.wms.finance;

import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.model.BillStatus;
import com.greenbin.wms.people.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * FR_28 automated billing and FR_29 bill payment.
 * The total is the base amount + 20% tax. A bill can be paid only after the finance officer approved it.
 */
public class Bill {
    public static final double TAX_RATE = 0.20;

    private final UUID billID = UUID.randomUUID();
    private final User user;
    private final String description;
    private double baseAmount;
    private double taxAmount;
    private double amount;
    private final LocalDate generatedDate;
    private final LocalDate dueDate;
    private BillStatus status = BillStatus.UNPAID;
    private boolean approved;
    private PaymentMethod paymentMethod;
    private LocalDate paidOn;
    private Pickup pickup;

    public Bill(User user, String description, double baseAmount, LocalDate generatedDate, LocalDate dueDate) {
        this.user = user;
        this.description = description;
        this.generatedDate = generatedDate;
        this.dueDate = dueDate;
        setBaseAmount(baseAmount);
    }

    public void setBaseAmount(double base) {
        if (base < 0) throw new IllegalArgumentException("Amount can't be negative");
        this.baseAmount = base;
        calculateOtherBillComponents();
    }

    private void calculateOtherBillComponents() {
        taxAmount = Math.round(baseAmount * TAX_RATE * 100) / 100.0;
        amount = baseAmount + taxAmount;
    }

    /** Is the bill complete? (Missing data is reported to the finance officer.) */
    public boolean checkBill() { return user != null && baseAmount > 0; }

    public void approve() { approved = true; }

    public void updateStatus(LocalDate today) {
        if (status == BillStatus.UNPAID && today.isAfter(dueDate)) status = BillStatus.OVERDUE;
    }

    /** Pays the bill. Returns the payment transaction, or null if it can't be paid. */
    public Transaction payBill(PaymentMethod method) {
        if (status == BillStatus.PAID || !approved) return null;
        status = BillStatus.PAID;
        paymentMethod = method;
        paidOn = LocalDate.now();
        return new Transaction(user, Transaction.Type.PAYMENT, amount, "Bill " + shortId() + " by " + method, LocalDateTime.now());
    }

    public String shortId() { return billID.toString().substring(0, 6).toUpperCase(); }

    public String viewBill() {
        return String.format("Bill %s - %s%nBase: %.2f Lek%nTax (20%%): %.2f Lek%nTotal: %.2f Lek%nDue: %s - %s",
                shortId(), description, baseAmount, taxAmount, amount, dueDate, status);
    }

    public UUID getBillID() { return billID; }
    public User getUser() { return user; }
    public String getDescription() { return description; }
    public double getBaseAmount() { return baseAmount; }
    public double getTaxAmount() { return taxAmount; }
    public double getAmount() { return amount; }
    public LocalDate getGeneratedDate() { return generatedDate; }
    public LocalDate getDueDate() { return dueDate; }
    public BillStatus getStatus() { return status; }
    public void setStatus(BillStatus status) { this.status = status; }
    public boolean isApproved() { return approved; }
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public LocalDate getPaidOn() { return paidOn; }
    public Pickup getPickup() { return pickup; }
    public void setPickup(Pickup pickup) { this.pickup = pickup; }
}
