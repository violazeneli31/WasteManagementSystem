package com.greenbin.wms.gui.screens;

import com.greenbin.wms.finance.Expense;
import com.greenbin.wms.finance.TaxReport;
import com.greenbin.wms.gui.Chip;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.gui.components.LineChart;
import com.greenbin.wms.model.Role;
import com.greenbin.wms.people.Account;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** FR_32 expense tracking & cost analysis, FR_33 automatic tax report. */
public class ExpensesScreen extends Screen {
    private final boolean officer;
    private final JComboBox<Expense.Category> category = new JComboBox<>(Expense.Category.values());
    private final JTextField description = new JTextField(16);
    private final JSpinner amount = new JSpinner(new SpinnerNumberModel(50000.0, 1.0, 1e9, 1000.0));
    private final DefaultTableModel model = Tables.model("Date", "Category", "Description", "Amount");
    private final BarChart byCategory = new BarChart();
    private final LineChart byMonth = new LineChart();
    private final DefaultListModel<String> tips = new DefaultListModel<>();

    private final JSpinner year = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2020, 2100, 1));
    private final JTextArea taxText = new JTextArea(14, 50);
    private final Chip taxStatus = new Chip("no report yet", Theme.MUTED);
    private TaxReport tax;

    public ExpensesScreen(Account me) {
        super(me, "Expenses & Tax", "Record costs, see where money goes, and produce the tax report for the authorities",
                "FR_32", "FR_33");
        officer = me.getRole() == Role.FINANCE_OFFICER;
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(Theme.BOLD);

        JPanel exp = new JPanel(new BorderLayout(10, 10));
        exp.setOpaque(false);
        if (officer) {
            exp.add(row(muted("New expense:"), category, description, amount, muted("Lek"),
                    FlatButton.primary("Record expense", e -> addExpense())), BorderLayout.NORTH);
        }
        JPanel charts = new JPanel(new GridLayout(1, 2, 10, 0));
        charts.setOpaque(false);
        charts.add(Theme.card("Total per category", byCategory));
        charts.add(Theme.card("Total per month", byMonth));
        exp.add(charts, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(1, 2, 10, 0));
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(100, 220));
        bottom.add(Theme.card("Expenses", Theme.table(Theme.newTable(model))));
        bottom.add(Theme.card("Where costs can be reduced (cost analysis)", new JScrollPane(new JList<>(tips))));
        exp.add(bottom, BorderLayout.SOUTH);
        tabs.addTab("Expenses (FR_32)", exp);

        JPanel taxPanel = new JPanel(new BorderLayout(0, 8));
        taxPanel.setOpaque(false);
        taxText.setEditable(false);
        taxText.setFont(Theme.MONO.deriveFont(13f));
        taxPanel.add(row(muted("Year"), year, FlatButton.primary("Generate tax report", e -> generateTax()),
                op(FlatButton.secondary("1. Verify calculations", e -> step(tax != null && tax.verify()))),
                op(FlatButton.secondary("2. Submit to authorities", e -> step(tax != null && tax.submit()))),
                op(FlatButton.secondary("3. Government auditor approves", e -> step(tax != null && tax.approveByAuditor()))),
                taxStatus), BorderLayout.NORTH);
        taxPanel.add(Theme.card("Tax report", new JScrollPane(taxText)), BorderLayout.CENTER);
        tabs.addTab("Tax report (FR_33)", taxPanel);
        body().add(tabs, BorderLayout.CENTER);
    }

    private FlatButton op(FlatButton b) {
        b.setEnabled(officer);
        return b;
    }

    private void addExpense() {
        if (description.getText().isBlank()) { error("Write a short description."); return; }
        sys.addExpense((Expense.Category) category.getSelectedItem(), description.getText().trim(), (Double) amount.getValue(), LocalDate.now());
        description.setText("");
        refresh();
    }

    private void generateTax() {
        tax = sys.generateTaxReport((Integer) year.getValue());
        showTax();
    }

    private void step(boolean ok) {
        if (!ok) error(tax == null ? "Generate the report first." : "Do the steps in order: verify, submit, approve.");
        else sys.getLog().add("TAX", "Tax report " + tax.getYear() + " -> " + tax.getStatus());
        showTax();
    }

    private void showTax() {
        if (tax == null) return;
        taxText.setText(tax.showReport());
        taxStatus.setChip(tax.getStatus().toString(), tax.getStatus() == TaxReport.Status.APPROVED_BY_AUDITOR ? Theme.GREEN : Theme.ORANGE);
    }

    @Override
    public void refresh() {
        List<Expense> list = new ArrayList<>(sys.getExpenses().getExpenses());
        list.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        model.setRowCount(0);
        for (Expense e : list) {
            model.addRow(new Object[]{e.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), e.getCategory(), e.getDescription(), Tables.money(e.getAmount())});
        }
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<Expense.Category, Double> e : sys.getExpenses().totalByCategory().entrySet()) {
            labels.add(e.getKey().toString().split(" ")[0]);
            values.add(e.getValue());
        }
        byCategory.setData(labels, values, java.util.Collections.singletonList(Theme.ORANGE), "Lek");
        List<String> months = new ArrayList<>();
        List<Double> totals = new ArrayList<>();
        for (Map.Entry<YearMonth, Double> e : sys.getExpenses().totalByMonth().entrySet()) {
            months.add(e.getKey().format(DateTimeFormatter.ofPattern("MMM")));
            totals.add(e.getValue());
        }
        byMonth.setData(months, java.util.Collections.singletonList(new LineChart.Series("expenses", Theme.RED, totals)), -1, "Lek");
        tips.clear();
        for (String t : sys.getExpenses().findSavingTips(LocalDate.now())) tips.addElement(t);
        if (tax == null && !sys.getTaxReports().isEmpty()) tax = sys.getTaxReports().get(0);
        showTax();
    }
}
