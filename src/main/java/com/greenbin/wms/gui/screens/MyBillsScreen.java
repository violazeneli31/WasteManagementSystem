package com.greenbin.wms.gui.screens;

import com.greenbin.wms.finance.Bill;
import com.greenbin.wms.finance.Dispute;
import com.greenbin.wms.finance.PaymentMethod;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.model.BillStatus;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/** FR_29 pay bills with different methods, FR_34 open a dispute. */
public class MyBillsScreen extends Screen {
    private final User user;
    private final DefaultTableModel model = Tables.model("Bill", "Description", "Total", "Due", "Status", "Paid with");
    private final JTable table = Theme.newTable(model);
    private final List<Bill> rows = new ArrayList<>();
    private final JTextArea billText = new JTextArea(7, 26);
    private final JComboBox<PaymentMethod> method = new JComboBox<>(PaymentMethod.values());
    private final JComboBox<Dispute.Type> disputeType = new JComboBox<>(Dispute.Type.values());
    private final JSpinner claimed = new JSpinner(new SpinnerNumberModel(100.0, 0.0, 1e6, 10.0));
    private final JTextField reason = new JTextField();
    private final JLabel evidence = Theme.label("no file attached", Theme.SMALL, Theme.MUTED);
    private final DefaultTableModel disputes = Tables.model("Type", "Claimed", "Status", "Answer");

    public MyBillsScreen(Account me) {
        super(me, "My Bills", "See and pay your bills; if something is wrong, open a dispute", "FR_29", "FR_34");
        user = (User) me;
        table.getColumnModel().getColumn(4).setCellRenderer(new Theme.StatusRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            int r = table.getSelectedRow();
            if (r >= 0 && r < rows.size()) billText.setText(rows.get(r).viewBill());
        });
        JPanel center = new JPanel(new BorderLayout(0, 8));
        center.setOpaque(false);
        center.add(Theme.card("Bills", Theme.table(table)), BorderLayout.CENTER);
        JTable dt = Theme.newTable(disputes);
        dt.getColumnModel().getColumn(2).setCellRenderer(new Theme.StatusRenderer());
        JPanel dcard = Theme.card("My disputes", Theme.table(dt));
        dcard.setPreferredSize(new Dimension(100, 170));
        center.add(dcard, BorderLayout.SOUTH);
        body().add(center, BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(370, 100));
        JPanel pay = new JPanel(new BorderLayout(0, 6));
        pay.setOpaque(false);
        billText.setEditable(false);
        billText.setFont(Theme.MONO);
        pay.add(billText, BorderLayout.CENTER);
        pay.add(row(method, FlatButton.primary("Pay selected bill", e -> payBill())), BorderLayout.SOUTH);
        side.add(Theme.card("Pay (FR_29)", pay), BorderLayout.NORTH);

        JPanel d = new Stack();
        d.setOpaque(false);
        d.add(muted("What is wrong?"));
        d.add(disputeType);
        d.add(row(muted("Amount (Lek, or points)"), claimed));
        d.add(muted("Explain the problem"));
        d.add(reason);
        d.add(row(FlatButton.secondary("Attach evidence...", e -> {
            JFileChooser c = new JFileChooser();
            if (c.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) evidence.setText(c.getSelectedFile().getName());
        }), evidence));
        d.add(FlatButton.danger("Send dispute", e -> sendDispute()));
        side.add(Theme.card("Dispute (FR_34) - for the selected bill or for missing points", d), BorderLayout.CENTER);
        body().add(side, BorderLayout.EAST);
    }

    private void payBill() {
        int r = table.getSelectedRow();
        if (r < 0) { error("Select a bill first."); return; }
        Bill b = rows.get(r);
        if (b.getStatus() == BillStatus.PAID) { error("This bill is already paid."); return; }
        if (!b.isApproved()) { error("This bill is still being checked by the finance office."); return; }
        if (!confirm(String.format("Pay %.2f Lek by %s?", b.getAmount(), method.getSelectedItem()))) return;
        sys.payBill(b, (PaymentMethod) method.getSelectedItem());
        info("Payment done. The receipt is in your notifications.");
        refresh();
    }

    private void sendDispute() {
        Dispute.Type t = (Dispute.Type) disputeType.getSelectedItem();
        int r = table.getSelectedRow();
        Bill b = r >= 0 ? rows.get(r) : null;
        if (t != Dispute.Type.REWARD_POINTS && b == null) { error("Select the bill this dispute is about."); return; }
        try {
            String ev = evidence.getText().equals("no file attached") ? "" : evidence.getText();
            sys.submitDispute(user, t, t == Dispute.Type.REWARD_POINTS ? null : b, reason.getText(), ev, (Double) claimed.getValue());
            info("Dispute sent to the finance office. You will be notified of the answer.");
            reason.setText("");
            evidence.setText("no file attached");
            refresh();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    @Override
    public void refresh() {
        rows.clear();
        rows.addAll(user.getBills());
        rows.sort((a, b) -> b.getGeneratedDate().compareTo(a.getGeneratedDate()));
        model.setRowCount(0);
        for (Bill b : rows) {
            model.addRow(new Object[]{b.shortId(), b.getDescription(), Tables.money(b.getAmount()), b.getDueDate(),
                    b.isApproved() ? b.getStatus().toString() : "BEING CHECKED", b.getPaymentMethod() == null ? "-" : b.getPaymentMethod()});
        }
        disputes.setRowCount(0);
        for (Dispute d : sys.getDisputes()) {
            if (d.getUser() != user) continue;
            disputes.addRow(new Object[]{d.getType(), d.getType() == Dispute.Type.REWARD_POINTS ? (int) d.getClaimedAmount() + " pts"
                    : Tables.money(d.getClaimedAmount()), d.getStatus(), d.getResolutionNote()});
        }
    }
}
