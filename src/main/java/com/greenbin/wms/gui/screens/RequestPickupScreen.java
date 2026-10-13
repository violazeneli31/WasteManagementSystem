package com.greenbin.wms.gui.screens;

import com.greenbin.wms.collection.Pickup;
import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Inputs;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.model.WasteType;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;

/** FR_23 request a pickup, with the price shown before sending (FR_13 / FR_28). */
public class RequestPickupScreen extends Screen {
    private final User user;
    private final JComboBox<WasteType> type = new JComboBox<>(WasteType.values());
    private final JComboBox<LocalDate> date = Inputs.dateCombo(14, true);
    private final JComboBox<LocalTime> time = Inputs.timeCombo();
    private final JTextField address = new JTextField();
    private final JSpinner kg = new JSpinner(new SpinnerNumberModel(10.0, 0.5, 1000.0, 0.5));
    private final JLabel price = Theme.label("<html>Estimated cost<br>-</html>", Theme.H2, Theme.GREEN);
    private final DefaultTableModel model = Tables.model("Waste", "Date", "Time", "Address", "Weight", "Status", "Collector", "Answer / bill");

    public RequestPickupScreen(Account me) {
        super(me, "Request a Pickup", "For big amounts, hazardous waste, or if you can't bring the waste to a bin", "FR_23", "FR_13");
        user = (User) me;
        JPanel form = new Stack();
        form.setOpaque(false);
        form.add(muted("Type of waste"));
        form.add(type);
        form.add(muted("Day and time"));
        form.add(row(date, time));
        form.add(muted("Address"));
        form.add(address);
        form.add(muted("Approximate weight (kg)"));
        form.add(kg);
        form.add(price);
        form.add(FlatButton.primary("Send request", e -> send()));
        type.addActionListener(e -> updatePrice());
        kg.addChangeListener(e -> updatePrice());
        JPanel card = Theme.card("New request", form);
        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.setPreferredSize(new Dimension(360, 100));
        left.add(card, BorderLayout.NORTH);
        body().add(left, BorderLayout.WEST);
        JTable t = Theme.newTable(model);
        t.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());
        body().add(Theme.card("My requests (you are notified when a collector answers)", Theme.table(t)), BorderLayout.CENTER);
    }

    private void updatePrice() {
        double base = sys.getPricing().calculateCost((WasteType) type.getSelectedItem(), (Double) kg.getValue());
        price.setText(String.format("<html>Estimated cost: %.2f Lek<br><font size=-1 color='#6B7A71'>%.0f fee + %.1f kg x %.0f Lek, + 20%% tax</font></html>",
                base * (1 + Bill.TAX_RATE), sys.getPricing().getSpecialPickupFee(), (Double) kg.getValue(),
                sys.getPricing().getPrice((WasteType) type.getSelectedItem())));
    }

    private void send() {
        try {
            Pickup p = sys.requestPickup(user, (WasteType) type.getSelectedItem(), (LocalDate) date.getSelectedItem(),
                    (LocalTime) time.getSelectedItem(), address.getText(), (Double) kg.getValue());
            info("Request sent! The collectors were notified.\nYou will get a confirmation with the date and time: " + p.getDate() + " " + p.getTime());
            refresh();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        if (address.getText().isBlank()) address.setText(user.getAddress());
        updatePrice();
        model.setRowCount(0);
        for (Pickup p : user.getPickups()) {
            String extra = p.getAnswer();
            if (p.getBill() != null) extra = "Bill " + Tables.money(p.getBill().getAmount()) + (p.getBill().isApproved() ? "" : " (being checked)");
            model.addRow(new Object[]{p.getTypeOfWaste(), Inputs.day(p.getDate()), p.getTime(), p.getLocation(), Tables.kg(p.getEstimatedWeightKg()),
                    p.getStatus(), p.getHandledBy() == null ? "-" : p.getHandledBy(), extra});
        }
    }
}
