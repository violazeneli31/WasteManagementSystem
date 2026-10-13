package com.greenbin.wms.finance;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** FR_32: keeps the expenses and finds where costs can be reduced. */
public class ExpenseTracker {
    private final List<Expense> expenses = new ArrayList<>();

    public void inputExpense(Expense e) { expenses.add(e); }

    public List<Expense> getExpenses() { return expenses; }

    public Map<Expense.Category, Double> totalByCategory() {
        Map<Expense.Category, Double> totals = new EnumMap<>(Expense.Category.class);
        for (Expense.Category c : Expense.Category.values()) totals.put(c, 0.0);
        for (Expense e : expenses) totals.merge(e.getCategory(), e.getAmount(), Double::sum);
        return totals;
    }

    public Map<YearMonth, Double> totalByMonth() {
        Map<YearMonth, Double> totals = new TreeMap<>();
        for (Expense e : expenses) totals.merge(YearMonth.from(e.getDate()), e.getAmount(), Double::sum);
        return totals;
    }

    public double total(int year) {
        double sum = 0;
        for (Expense e : expenses) if (e.getDate().getYear() == year) sum += e.getAmount();
        return sum;
    }

    public double total(Expense.Category category, int year) {
        double sum = 0;
        for (Expense e : expenses) {
            if (e.getCategory() == category && e.getDate().getYear() == year) sum += e.getAmount();
        }
        return sum;
    }

    /** Cost trend: categories that went up this month compared to last month. */
    public List<String> findSavingTips(LocalDate today) {
        YearMonth thisMonth = YearMonth.from(today);
        YearMonth lastMonth = thisMonth.minusMonths(1);
        List<String> tips = new ArrayList<>();
        for (Expense.Category c : Expense.Category.values()) {
            double now = 0, before = 0;
            for (Expense e : expenses) {
                if (e.getCategory() != c) continue;
                if (YearMonth.from(e.getDate()).equals(thisMonth)) now += e.getAmount();
                if (YearMonth.from(e.getDate()).equals(lastMonth)) before += e.getAmount();
            }
            if (before > 0 && now > before * 1.10) {
                tips.add(String.format("%s went up %.0f%% this month - check if it can be reduced.", c, 100 * (now - before) / before));
            }
        }
        Map<Expense.Category, Double> byCategory = totalByCategory();
        Expense.Category biggest = null;
        for (Expense.Category c : byCategory.keySet()) {
            if (biggest == null || byCategory.get(c) > byCategory.get(biggest)) biggest = c;
        }
        if (biggest != null && byCategory.get(biggest) > 0) tips.add("Biggest cost overall: " + biggest + ".");
        return tips;
    }
}
