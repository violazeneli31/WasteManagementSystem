package com.greenbin.wms.gui.screens;

import com.greenbin.wms.community.Report;
import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.model.Role;
import com.greenbin.wms.people.Account;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** FR_20: citizens and employees send reports; the manager assigns and resolves them. */
public class ReportsScreen extends Screen {
    private final boolean manager;
    private final DefaultTableModel model = Tables.model("Date", "From", "Subject", "Department", "Status", "Assigned to", "Answer");
    private final JTable table = Theme.newTable(model);
    private final List<Report> rows = new ArrayList<>();
    private final JTextField subject = new JTextField();
    private final JTextArea description = new JTextArea(5, 20);
    private final JLabel goesTo = Theme.label("It will go to: -", Theme.BOLD, Theme.GREEN);
    private final JComboBox<String> assignee = new JComboBox<>(new String[]{"Bin maintenance", "Collection team", "Finance office", "Management", "Customer support"});
    private final JTextField answer = new JTextField(20);
    private final BarChart summary = new BarChart();

    public ReportsScreen(Account me) {
        super(me, me.getRole() == Role.MANAGER ? "Reports & Feedback" : "Send a Report",
                me.getRole() == Role.MANAGER ? "Every report is put in a category and sent to the right department automatically"
                        : "Tell us about a problem (overflowing bin, missed collection...) or suggest an improvement", "FR_20");
        manager = me.getRole() == Role.MANAGER;
        table.getColumnModel().getColumn(4).setCellRenderer(new Theme.StatusRenderer());
        body().add(Theme.card(manager ? "All reports" : "My reports", Theme.table(table)), BorderLayout.CENTER);

        if (manager) {
            JPanel actions = new Stack();
            actions.setOpaque(false);
            actions.add(muted("Selected report - assign to:"));
            actions.add(assignee);
            actions.add(FlatButton.secondary("Assign", e -> {
                Report r = selected();
                if (r != null) { sys.assignReport(r, (String) assignee.getSelectedItem()); refresh(); }
            }));
            actions.add(muted("Answer to the sender:"));
            actions.add(answer);
            actions.add(FlatButton.primary("Resolve & notify sender", e -> {
                Report r = selected();
                if (r == null) return;
                if (answer.getText().isBlank()) { error("Write an answer for the sender."); return; }
                sys.resolveReport(r, answer.getText().trim());
                answer.setText("");
                refresh();
            }));
            JPanel side = new JPanel(new BorderLayout(0, 10));
            side.setOpaque(false);
            side.setPreferredSize(new Dimension(340, 100));
            side.add(Theme.card("Handle report", actions), BorderLayout.NORTH);
            side.add(Theme.card("This month's reports by category (FR_20 step 6)", summary), BorderLayout.CENTER);
            body().add(side, BorderLayout.EAST);
        } else {
            JPanel form = new JPanel(new BorderLayout(0, 6));
            form.setOpaque(false);
            JPanel top = new Stack();
            top.setOpaque(false);
            top.add(muted("Subject"));
            top.add(subject);
            top.add(muted("Description"));
            form.add(top, BorderLayout.NORTH);
            description.setLineWrap(true);
            description.setWrapStyleWord(true);
            form.add(new JScrollPane(description), BorderLayout.CENTER);
            JPanel bottom = new Stack();
            bottom.setOpaque(false);
            bottom.add(goesTo);
            bottom.add(FlatButton.primary("Send report", e -> send()));
            form.add(bottom, BorderLayout.SOUTH);
            DocumentListener preview = new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { updatePreview(); }
                public void removeUpdate(DocumentEvent e) { updatePreview(); }
                public void changedUpdate(DocumentEvent e) { updatePreview(); }
            };
            subject.getDocument().addDocumentListener(preview);
            description.getDocument().addDocumentListener(preview);
            JPanel card = Theme.card("New report", form);
            card.setPreferredSize(new Dimension(360, 100));
            body().add(card, BorderLayout.EAST);
        }
    }

    private void updatePreview() {
        goesTo.setText("It will go to: " + Report.Category.categorize(subject.getText() + " " + description.getText()).getDepartment());
    }

    private void send() {
        try {
            Report r = sys.submitReport(me, subject.getText(), description.getText());
            info("Thank you! Your report was sent to: " + r.getCategory().getDepartment() + ".\nYou will be notified when its status changes.");
            subject.setText("");
            description.setText("");
            refresh();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private Report selected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            error("Select a report first.");
            return null;
        }
        return rows.get(r);
    }

    @Override
    public void refresh() {
        rows.clear();
        for (Report r : sys.getReports()) if (manager || r.getAuthor() == me) rows.add(r);
        model.setRowCount(0);
        for (Report r : rows) {
            model.addRow(new Object[]{r.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")), r.getAuthor().getDisplayName(),
                    r.getSubject(), r.getCategory().getDepartment(), r.getStatus(), r.getAssignedTo(), r.getResponse()});
        }
        if (manager) {
            List<String> labels = new ArrayList<>();
            List<Double> values = new ArrayList<>();
            for (Map.Entry<Report.Category, Integer> e : sys.monthlyFeedbackSummary().entrySet()) {
                labels.add(e.getKey().getDepartment().split(" ")[0]);
                values.add((double) e.getValue());
            }
            summary.setData(labels, values, java.util.Collections.singletonList(Theme.BLUE), "reports");
        }
    }
}
