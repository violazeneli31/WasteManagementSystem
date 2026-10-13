package com.greenbin.wms.gui.screens;

import com.greenbin.wms.finance.FinancialPlan;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.LineChart;
import com.greenbin.wms.gui.components.StatCard;
import com.greenbin.wms.people.Account;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** FR_35: past revenue and costs, the forecast for the next 12 months and the budget. */
public class BudgetScreen extends Screen {
    private final StatCard budget = new StatCard("budget (Lek)", Theme.BLUE);
    private final StatCard revenue = new StatCard("expected revenue, next 12 months", Theme.GREEN);
    private final StatCard expenses = new StatCard("expected expenses, next 12 months", Theme.RED);
    private final StatCard funding = new StatCard("extra funding needed", Theme.ORANGE);
    private final StatCard enough = new StatCard("is the budget enough?", Theme.PURPLE);
    private final LineChart chart = new LineChart();
    private final JSpinner newBudget = new JSpinner(new SpinnerNumberModel(25_000_000.0, 1.0, 1e11, 500_000.0));
    private final DefaultListModel<String> changes = new DefaultListModel<>();

    public BudgetScreen(Account me) {
        super(me, "Budget Forecast", "The next months follow the trend of the past 12 months (last value + average monthly change)", "FR_35");
        JPanel cards = new JPanel(new GridLayout(1, 5, 8, 0));
        cards.setOpaque(false);
        for (StatCard c : new StatCard[]{budget, revenue, expenses, funding, enough}) cards.add(c);
        body().add(cards, BorderLayout.NORTH);
        body().add(Theme.card("Monthly revenue and expenses: 12 months history + 12 months forecast", chart), BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(320, 100));
        side.add(Theme.card("Adjust the budget", row(newBudget, FlatButton.primary("Save", e -> {
            sys.getFinancialPlan().updateBudget((Double) newBudget.getValue(), me.getDisplayName());
            sys.getLog().add("BUDGET", me.getDisplayName() + " set the budget to " + String.format("%,.0f", (Double) newBudget.getValue()));
            refresh();
        }))), BorderLayout.NORTH);
        side.add(Theme.card("Budget changes", new JScrollPane(new JList<>(changes))), BorderLayout.CENTER);
        body().add(side, BorderLayout.EAST);
    }

    @Override
    public void refresh() {
        FinancialPlan p = sys.getFinancialPlan();
        budget.setValue(String.format("%,.0f", p.getTotalBudget()));
        revenue.setValue(String.format("%,.0f", p.getExpectedRevenue()));
        expenses.setValue(String.format("%,.0f", p.getExpectedExpenses()));
        funding.setValue(String.format("%,.0f", p.getFundingNeeded()));
        enough.setValue(p.isBudgetEnough() ? "YES" : "NO");
        newBudget.setValue(p.getTotalBudget());

        List<Double> rev = new ArrayList<>(p.getMonthlyRevenue());
        rev.addAll(p.forecastRevenue(12));
        List<Double> exp = new ArrayList<>(p.getMonthlyExpenses());
        exp.addAll(p.forecastExpenses(12));
        List<String> x = new ArrayList<>();
        YearMonth start = YearMonth.now().minusMonths(p.getMonthlyRevenue().size());
        for (int i = 0; i < rev.size(); i++) x.add(start.plusMonths(i).format(DateTimeFormatter.ofPattern("MMM yy")));
        List<LineChart.Series> s = new ArrayList<>();
        s.add(new LineChart.Series("revenue", Theme.GREEN, rev));
        s.add(new LineChart.Series("expenses", Theme.RED, exp));
        chart.setData(x, s, p.getMonthlyRevenue().size(), "Lek per month");
        changes.clear();
        for (String c : p.getBudgetChanges()) changes.addElement(c);
    }
}
