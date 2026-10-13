package com.greenbin.wms.gui.screens;

import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.finance.PaymentMethod;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.gui.components.StatCard;
import com.greenbin.wms.model.BillStatus;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.FinanceOfficer;

import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** FR_13 dynamic pricing, FR_28 automated billing (approve), FR_29 overview of payments. */
public class BillingScreen extends Screen {
    private final DefaultTableModel prices = Tables.model("Waste type", "Price per kg");
    private final JComboBox<WasteType> priceType = new JComboBox<>(WasteType.values());
    private final JSpinner newPrice = new JSpinner(new SpinnerNumberModel(20.0, 1.0, 1000.0, 1.0));
    private final JSpinner fee = new JSpinner(new SpinnerNumberModel(300.0, 0.0, 10000.0, 10.0));
    private final DefaultListModel<String> priceLog = new DefaultListModel<>();

    private final DefaultTableModel bills = Tables.model("Bill", "Customer", "Description", "Base", "Tax 20%", "Total", "Due", "Status", "Approved");
    private final JTable billTable = Theme.newTable(bills);
    private final List<Bill> rows = new ArrayList<>();
    private final JCheckBox onlyWaiting = new JCheckBox("Only bills waiting for approval");
    private final StatCard paid = new StatCard("paid", Theme.GREEN);
    private final StatCard unpaid = new StatCard("unpaid", Theme.ORANGE);
    private final StatCard overdue = new StatCard("overdue", Theme.RED);
    private final StatCard waiting = new StatCard("waiting approval", Theme.BLUE);
    private final BarChart methods = new BarChart();

    public BillingScreen(Account me) {
        super(me, "Billing & Pricing", "Prices per kg, bills calculated automatically after a confirmed pickup, payments",
                "FR_13", "FR_28", "FR_29");
        JPanel cards = new JPanel(new GridLayout(1, 4, 8, 0));
        cards.setOpaque(false);
        for (StatCard c : new StatCard[]{paid, unpaid, overdue, waiting}) cards.add(c);
        body().add(cards, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        billTable.getColumnModel().getColumn(7).setCellRenderer(new Theme.StatusRenderer());
        billTable.getColumnModel().getColumn(8).setCellRenderer(new Theme.StatusRenderer());
        center.add(Theme.table(billTable), BorderLayout.CENTER);
        onlyWaiting.setOpaque(false);
        onlyWaiting.addActionListener(e -> refresh());
        center.add(row(onlyWaiting, FlatButton.primary("Approve & send to customer", e -> approve()),
                FlatButton.secondary("Show bill", e -> {
                    int r = billTable.getSelectedRow();
                    if (r >= 0) info(rows.get(r).viewBill());
                }),
                FlatButton.secondary("Update overdue bills", e -> { sys.updateBillStatuses(); refresh(); })), BorderLayout.SOUTH);
        body().add(Theme.card("Bills", center), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(360, 100));
        JPanel pricing = new JPanel(new BorderLayout(0, 6));
        pricing.setOpaque(false);
        JScrollPane pt = Theme.table(Theme.newTable(prices));
        pt.setPreferredSize(new Dimension(300, 150));
        pricing.add(pt, BorderLayout.NORTH);
        JPanel form = new Stack();
        form.setOpaque(false);
        form.add(row(priceType, newPrice, muted("Lek/kg")));
        form.add(FlatButton.primary("Save price", e -> savePrice()));
        form.add(row(muted("Special pickup fee"), fee, muted("Lek")));
        form.add(FlatButton.primary("Save fee", e -> {
            sys.getPricing().setSpecialPickupFee((Double) fee.getValue(), me.getDisplayName());
            refresh();
        }));
        JScrollPane logScroll = new JScrollPane(new JList<>(priceLog));
        logScroll.setPreferredSize(new Dimension(300, 90));
        JPanel lower = new JPanel(new BorderLayout(0, 6));
        lower.setOpaque(false);
        lower.add(form, BorderLayout.NORTH);
        lower.add(logScroll, BorderLayout.CENTER);
        pricing.add(lower, BorderLayout.CENTER);
        right.add(Theme.card("Prices (logged for audit)", pricing), BorderLayout.CENTER);
        methods.setPreferredSize(new Dimension(300, 170));
        right.add(Theme.card("Payments by method", methods), BorderLayout.SOUTH);
        body().add(right, BorderLayout.EAST);
    }

    private void savePrice() {
        WasteType t = (WasteType) priceType.getSelectedItem();
        int updated = sys.changePrice(t, (Double) newPrice.getValue(), (FinanceOfficer) me);
        info("New " + t + " price saved." + (updated > 0 ? "\n" + updated + " bill(s) not approved yet were recalculated and the customers notified." : ""));
        refresh();
    }

    private void approve() {
        int r = billTable.getSelectedRow();
        if (r < 0) { error("Select a bill first."); return; }
        Bill b = rows.get(r);
        if (b.isApproved()) { error("This bill is already approved."); return; }
        if (sys.approveBill(b)) info("Approved. " + b.getUser().getDisplayName() + " got the bill in the app.");
        refresh();
    }

    @Override
    public void refresh() {
        prices.setRowCount(0);
        for (WasteType t : WasteType.values()) prices.addRow(new Object[]{t, Tables.money(sys.getPricing().getPrice(t))});
        prices.addRow(new Object[]{"Special pickup fee", Tables.money(sys.getPricing().getSpecialPickupFee())});
        priceLog.clear();
        for (String l : sys.getPricing().getChangeLog()) priceLog.addElement(l);

        rows.clear();
        double paidSum = 0, unpaidSum = 0, overdueSum = 0;
        int waitingCount = 0;
        Map<PaymentMethod, Double> byMethod = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod m : PaymentMethod.values()) byMethod.put(m, 0.0);
        for (Bill b : sys.getBills()) {
            if (b.getStatus() == BillStatus.PAID) { paidSum += b.getAmount(); byMethod.merge(b.getPaymentMethod(), b.getAmount(), Double::sum); }
            else if (b.getStatus() == BillStatus.OVERDUE) overdueSum += b.getAmount();
            else unpaidSum += b.getAmount();
            if (!b.isApproved()) waitingCount++;
            if (!onlyWaiting.isSelected() || !b.isApproved()) rows.add(b);
        }
        rows.sort((a, b) -> b.getGeneratedDate().compareTo(a.getGeneratedDate()));
        bills.setRowCount(0);
        for (Bill b : rows) {
            bills.addRow(new Object[]{b.shortId(), b.getUser().getDisplayName(), b.getDescription(), Tables.money(b.getBaseAmount()),
                    Tables.money(b.getTaxAmount()), Tables.money(b.getAmount()), b.getDueDate(), b.getStatus(), b.isApproved() ? "APPROVED" : "WAITING"});
        }
        paid.setValue(String.format("%,.0f L", paidSum));
        unpaid.setValue(String.format("%,.0f L", unpaidSum));
        overdue.setValue(String.format("%,.0f L", overdueSum));
        waiting.setValue(String.valueOf(waitingCount));
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<PaymentMethod, Double> e : byMethod.entrySet()) { labels.add(e.getKey().toString()); values.add(e.getValue()); }
        methods.setData(labels, values, java.util.Collections.singletonList(Theme.GREEN), "Lek");
    }
}
