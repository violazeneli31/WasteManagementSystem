package com.greenbin.wms.gui.screens;

import com.greenbin.wms.finance.FraudDetector;
import com.greenbin.wms.finance.Transaction;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** FR_31: the transaction log, automatic fraud rules, and the officer's decision. */
public class FraudScreen extends Screen {
    private final DefaultTableModel model = Tables.model("Time", "User", "Type", "Amount", "Reference", "Check", "Reason");
    private final JTable table = Theme.newTable(model);
    private final List<Transaction> rows = new ArrayList<>();
    private final JCheckBox onlySuspicious = new JCheckBox("Only suspicious", true);
    private final DefaultTableModel blocked = Tables.model("Blocked user", "Points");
    private final JTable blockedTable = Theme.newTable(blocked);
    private final List<User> blockedRows = new ArrayList<>();

    public FraudScreen(Account me) {
        super(me, "Fraud Detection", "Rules: same payment twice in 10 minutes, more than " + FraudDetector.MAX_POINTS_PER_DAY
                + " points in a day, " + FraudDetector.MAX_VOUCHERS_PER_DAY + "+ vouchers in a day", "FR_31");
        table.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());
        onlySuspicious.setOpaque(false);
        onlySuspicious.addActionListener(e -> refresh());
        body().add(row(FlatButton.primary("Run fraud check now", e -> {
            List<Transaction> found = sys.runFraudCheck();
            info(found.isEmpty() ? "No new suspicious transactions." : found.size() + " new suspicious transaction(s) found.");
            refresh();
        }), onlySuspicious), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);
        center.add(Theme.table(table), BorderLayout.CENTER);
        center.add(row(muted("Investigate the selected transaction:"),
                FlatButton.danger("It is fraud: block user & reverse", e -> decide(true)),
                FlatButton.primary("It is legitimate", e -> decide(false))), BorderLayout.SOUTH);
        body().add(Theme.card("Transaction log", center), BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, 6));
        side.setOpaque(false);
        side.add(Theme.table(blockedTable), BorderLayout.CENTER);
        side.add(FlatButton.secondary("Unblock selected user", e -> {
            int r = blockedTable.getSelectedRow();
            if (r < 0) return;
            blockedRows.get(r).setBlocked(false);
            sys.getLog().add("FRAUD", blockedRows.get(r).getDisplayName() + " unblocked");
            refresh();
        }), BorderLayout.SOUTH);
        JPanel card = Theme.card("Blocked accounts", side);
        card.setPreferredSize(new Dimension(280, 100));
        body().add(card, BorderLayout.EAST);
    }

    private void decide(boolean fraud) {
        int r = table.getSelectedRow();
        if (r < 0) { error("Select a transaction first."); return; }
        Transaction t = rows.get(r);
        if (t.getFraudStatus() != Transaction.FraudStatus.SUSPICIOUS) { error("Only suspicious transactions need a decision."); return; }
        if (fraud) {
            if (!confirm("Block " + t.getUser().getDisplayName() + " and reverse this transaction?")) return;
            sys.confirmFraud(t);
        } else {
            sys.markLegitimate(t);
        }
        refresh();
    }

    @Override
    public void refresh() {
        rows.clear();
        for (int i = sys.getTransactions().size() - 1; i >= 0; i--) {
            Transaction t = sys.getTransactions().get(i);
            boolean interesting = t.getFraudStatus() == Transaction.FraudStatus.SUSPICIOUS || t.getFraudStatus() == Transaction.FraudStatus.FRAUD
                    || t.getFraudStatus() == Transaction.FraudStatus.LEGITIMATE;
            if (!onlySuspicious.isSelected() || interesting) rows.add(t);
        }
        model.setRowCount(0);
        for (Transaction t : rows) {
            String amount = t.getType() == Transaction.Type.PAYMENT || t.getType() == Transaction.Type.REFUND
                    ? Tables.money(t.getAmount()) : (int) t.getAmount() + " points";
            model.addRow(new Object[]{t.getTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), t.getUser().getDisplayName(),
                    t.getType(), amount, t.getReference(), t.getFraudStatus(), t.getAlertReason()});
        }
        blockedRows.clear();
        blocked.setRowCount(0);
        for (User u : sys.getUsers()) {
            if (u.isBlocked()) {
                blockedRows.add(u);
                blocked.addRow(new Object[]{u.getDisplayName(), u.getPoints()});
            }
        }
    }
}
