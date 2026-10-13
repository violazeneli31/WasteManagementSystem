package com.greenbin.wms.finance;

import java.time.LocalDate;

/** FR_32: an operational expense recorded by the finance officer. */
public class Expense {
    public enum Category {
        VEHICLE_MAINTENANCE("Vehicle maintenance & fuel"),
        SALARIES("Employee salaries"),
        BIN_MANUFACTURING("Bin manufacturing"),
        RECYCLING_INCENTIVES("Recycling incentives / rewards"),
        WASTE_COLLECTION("Other collection costs");

        private final String label;
        Category(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private final Category category;
    private final String description;
    private final double amount;
    private final LocalDate date;

    public Expense(Category category, String description, double amount, LocalDate date) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be more than 0");
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }

    public Category getCategory() { return category; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public LocalDate getDate() { return date; }
}
