package com.greenbin.wms.finance;

import java.time.LocalDate;

/**
 * FR_33: the yearly tax report for the authorities.
 * Data comes from all paid bills and recorded expenses; the finance officer verifies and submits it,
 * then the government auditor approves it.
 */
public class TaxReport {
    public static final double PROFIT_TAX_RATE = 0.15;

    public enum Status { DRAFT, VERIFIED, SUBMITTED, APPROVED_BY_AUDITOR }

    private final int year;
    private final double revenue;
    private final double vatCollected;
    private final double recyclingIncentives;
    private final double operationalExpenses;
    private final LocalDate generatedOn = LocalDate.now();
    private Status status = Status.DRAFT;

    public TaxReport(int year, double revenue, double vatCollected, double recyclingIncentives, double operationalExpenses) {
        this.year = year;
        this.revenue = revenue;
        this.vatCollected = vatCollected;
        this.recyclingIncentives = recyclingIncentives;
        this.operationalExpenses = operationalExpenses;
    }

    public double getProfit() { return revenue - operationalExpenses - recyclingIncentives; }
    public double getProfitTax() { return Math.max(0, getProfit()) * PROFIT_TAX_RATE; }

    public boolean verify() {
        if (status != Status.DRAFT) return false;
        status = Status.VERIFIED;
        return true;
    }

    public boolean submit() {
        if (status != Status.VERIFIED) return false;
        status = Status.SUBMITTED;
        return true;
    }

    public boolean approveByAuditor() {
        if (status != Status.SUBMITTED) return false;
        status = Status.APPROVED_BY_AUDITOR;
        return true;
    }

    public String showReport() {
        return String.format("TAX REPORT %d (generated %s)%n%n"
                        + "Revenue from bills:        %,15.2f Lek%n"
                        + "VAT collected (20%%):       %,15.2f Lek%n"
                        + "Operational expenses:      %,15.2f Lek%n"
                        + "Recycling incentives:      %,15.2f Lek%n"
                        + "Profit:                    %,15.2f Lek%n"
                        + "Profit tax (15%%):          %,15.2f Lek%n%n"
                        + "Status: %s",
                year, generatedOn, revenue, vatCollected, operationalExpenses, recyclingIncentives,
                getProfit(), getProfitTax(), status);
    }

    public int getYear() { return year; }
    public double getRevenue() { return revenue; }
    public double getVatCollected() { return vatCollected; }
    public double getRecyclingIncentives() { return recyclingIncentives; }
    public double getOperationalExpenses() { return operationalExpenses; }
    public Status getStatus() { return status; }
}
