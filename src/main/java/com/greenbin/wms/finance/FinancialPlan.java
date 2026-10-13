package com.greenbin.wms.finance;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * FR_35 Financial forecasting & budget planning.
 * Uses the monthly history to predict the next months: next value = last value + average monthly change.
 */
public class FinancialPlan {
    private final UUID planID = UUID.randomUUID();
    private final int year;
    private final LocalDate createdOn;
    private double totalBudget;
    private final List<Double> monthlyRevenue = new ArrayList<>();
    private final List<Double> monthlyExpenses = new ArrayList<>();
    private final List<String> budgetChanges = new ArrayList<>();

    public FinancialPlan(int year, LocalDate createdOn, double totalBudget) {
        this.year = year;
        this.createdOn = createdOn;
        this.totalBudget = totalBudget;
    }

    public void addMonth(double revenue, double expenses) {
        monthlyRevenue.add(revenue);
        monthlyExpenses.add(expenses);
    }

    public static List<Double> forecast(List<Double> history, int months) {
        List<Double> result = new ArrayList<>();
        if (history.isEmpty()) return result;
        double avgChange = history.size() < 2 ? 0
                : (history.get(history.size() - 1) - history.get(0)) / (history.size() - 1);
        double last = history.get(history.size() - 1);
        for (int i = 1; i <= months; i++) result.add(Math.max(0, last + avgChange * i));
        return result;
    }

    public List<Double> forecastRevenue(int months) { return forecast(monthlyRevenue, months); }
    public List<Double> forecastExpenses(int months) { return forecast(monthlyExpenses, months); }

    public double getExpectedRevenue() { return sum(forecastRevenue(12)); }
    public double getExpectedExpenses() { return sum(forecastExpenses(12)); }

    /** Extra money needed next year if expenses are bigger than revenue (0 if not). */
    public double getFundingNeeded() { return Math.max(0, getExpectedExpenses() - getExpectedRevenue()); }

    /** Is the budget enough for the expected expenses? */
    public boolean isBudgetEnough() { return totalBudget >= getExpectedExpenses(); }

    public void updateBudget(double newBudget, String changedBy) {
        if (newBudget <= 0) throw new IllegalArgumentException("Budget must be more than 0");
        budgetChanges.add(0, changedBy + " changed the budget from " + String.format("%,.0f", totalBudget)
                + " to " + String.format("%,.0f", newBudget) + " Lek");
        totalBudget = newBudget;
    }

    private static double sum(List<Double> values) {
        double s = 0;
        for (double v : values) s += v;
        return s;
    }

    public String viewFinancialPlan() {
        return String.format("Plan %d: budget %,.0f Lek, expected revenue %,.0f, expected expenses %,.0f",
                year, totalBudget, getExpectedRevenue(), getExpectedExpenses());
    }

    public UUID getPlanID() { return planID; }
    public int getYear() { return year; }
    public LocalDate getCreatedOn() { return createdOn; }
    public double getTotalBudget() { return totalBudget; }
    public List<Double> getMonthlyRevenue() { return monthlyRevenue; }
    public List<Double> getMonthlyExpenses() { return monthlyExpenses; }
    public List<String> getBudgetChanges() { return budgetChanges; }
}
