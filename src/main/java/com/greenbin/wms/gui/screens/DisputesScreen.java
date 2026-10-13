package com.greenbin.wms.gui.screens;

import com.greenbin.wms.finance.Dispute;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.model.Role;
import com.greenbin.wms.people.Account;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** FR_34: the finance officer resolves disputes (refunds, corrected bills, points back); the manager sees the trends. */
public class DisputesScreen extends Screen {
    private final boolean officer;
    private final DefaultTableModel model = Tables.model("Date", "User", "Type", "Bill", "Claimed", "Status", "Officer", "Resolution");
    private final JTable table = Theme.newTable(model);
    private final JTextArea details = new JTextArea(6, 24);
    private final JTextField note = new JTextField();
    private final BarChart trends = new BarChart();

    public DisputesScreen(Account me) {
        super(me, me.getRole() == Role.FINANCE_OFFICER ? "Disputes & Refunds" : "Disputes",
                "Complaints about bills, missing reward points or failed payments", "FR_34");
        officer = me.getRole() == Role.FINANCE_OFFICER;
        table.getColumnModel().getColumn(5).setCellRenderer(new Theme.StatusRenderer());
        table.getSelectionModel().addListSelectionListener(e -> showSelected());
        body().add(Theme.card("Disputes (assigned to the finance office automatically)", Theme.table(table)), BorderLayout.CENTER);

        JPanel side = new JPanel(new BorderLayout(0, 8));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(360, 100));
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        JPanel det = new JPanel(new BorderLayout(0, 6));
        det.setOpaque(false);
        det.add(details, BorderLayout.CENTER);
        if (officer) {
            JPanel act = new Stack();
            act.setOpaque(false);
            act.add(muted("Note to the user:"));
            act.add(note);
            act.add(FlatButton.primary("Valid: correct it & notify user", e -> resolve(true)));
            act.add(FlatButton.danger("Not valid: reject", e -> resolve(false)));
            det.add(act, BorderLayout.SOUTH);
        }
        side.add(Theme.card("Selected dispute", det), BorderLayout.NORTH);
        side.add(Theme.card("Disputes by type (managers use it to change policies)", trends), BorderLayout.CENTER);
        body().add(side, BorderLayout.EAST);
    }

    private void showSelected() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= sys.getDisputes().size()) return;
        Dispute d = sys.getDisputes().get(r);
        details.setText(d.getUser().getDisplayName() + " - " + d.getType() + "\n\n" + d.getDescription()
                + "\n\nEvidence: " + (d.getEvidence().isEmpty() ? "none" : d.getEvidence())
                + (d.getBill() != null ? "\n\n" + d.getBill().viewBill() : ""));
    }

    private void resolve(boolean valid) {
        int r = table.getSelectedRow();
        if (r < 0) { error("Select a dispute first."); return; }
        Dispute d = sys.getDisputes().get(r);
        if (d.getStatus() == Dispute.Status.APPROVED || d.getStatus() == Dispute.Status.REJECTED) { error("Already resolved."); return; }
        String text = note.getText().isBlank() ? (valid ? "Your claim was correct. We fixed it." : "We checked and the original amount was correct.") : note.getText().trim();
        sys.resolveDispute(d, valid, text);
        note.setText("");
        info(valid ? "Corrected: " + (d.getType() == Dispute.Type.REWARD_POINTS ? (int) d.getClaimedAmount() + " points given back."
                : "refund / bill correction of " + Tables.money(d.getClaimedAmount()) + ".") + " The user was notified." : "Rejected. The user was notified.");
        refresh();
    }

    @Override
    public void refresh() {
        model.setRowCount(0);
        for (Dispute d : sys.getDisputes()) {
            String claimed = d.getType() == Dispute.Type.REWARD_POINTS ? (int) d.getClaimedAmount() + " points" : Tables.money(d.getClaimedAmount());
            model.addRow(new Object[]{d.getSubmittedOn().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), d.getUser().getDisplayName(),
                    d.getType(), d.getBill() == null ? "-" : d.getBill().shortId(), claimed, d.getStatus(),
                    d.getAssignedOfficer() == null ? "-" : d.getAssignedOfficer(), d.getResolutionNote()});
        }
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        for (Map.Entry<Dispute.Type, Integer> e : sys.disputeTrends().entrySet()) {
            labels.add(e.getKey().name().replace('_', ' ').toLowerCase());
            values.add((double) e.getValue());
        }
        trends.setData(labels, values, java.util.Collections.singletonList(Theme.ORANGE), "disputes");
    }
}
