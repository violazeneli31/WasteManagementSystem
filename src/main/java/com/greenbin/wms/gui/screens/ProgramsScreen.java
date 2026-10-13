package com.greenbin.wms.gui.screens;

import com.greenbin.wms.gui.FlatButton;
import com.greenbin.wms.gui.Stack;
import com.greenbin.wms.gui.Screen;
import com.greenbin.wms.gui.Tables;
import com.greenbin.wms.gui.Theme;
import com.greenbin.wms.gui.components.BarChart;
import com.greenbin.wms.model.ProgramType;
import com.greenbin.wms.people.Account;
import com.greenbin.wms.people.User;
import com.greenbin.wms.programs.CommunityEvent;
import com.greenbin.wms.programs.EducationalProgram;
import com.greenbin.wms.programs.Program;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FR_15 events and FR_16 educational programs.
 * The manager creates them (FACTORY pattern) and follows attendance and feedback;
 * citizens join them and give feedback.
 */
public class ProgramsScreen extends Screen {
    private final boolean manager;
    private final DefaultTableModel model = Tables.model("Name", "Type", "Date", "Where", "Registered", "Attended", "Rating", "Status");
    private final JTable table = Theme.newTable(model);
    private final List<Program> rows = new ArrayList<>();
    private final JTextArea promo = new JTextArea(7, 30);
    private final DefaultListModel<String> feedback = new DefaultListModel<>();
    private final BarChart attendance = new BarChart();

    // manager form
    private final JRadioButton event = new JRadioButton("Community event", true);
    private final JRadioButton educational = new JRadioButton("Educational program");
    private final JTextField name = new JTextField();
    private final JTextField description = new JTextField();
    private final JTextField location = new JTextField();
    private final JSpinner inDays = new JSpinner(new SpinnerNumberModel(14, 1, 365, 1));
    private final JSpinner capacity = new JSpinner(new SpinnerNumberModel(50, 1, 5000, 5));
    private final JTextField extra = new JTextField();
    private final JLabel extraLabel = muted("Objective of the event");
    private final JComboBox<User> participant = new JComboBox<>();
    private final JTextArea insights = new JTextArea(2, 20);

    // citizen form
    private final JSpinner rating = new JSpinner(new SpinnerNumberModel(5, 1, 5, 1));
    private final JTextField comment = new JTextField(22);

    public ProgramsScreen(Account me) {
        super(me, "Programs & Events", me instanceof User ? "Join clean-ups and lessons, earn a better city - and tell us what you think"
                : "Create events and awareness programs, follow attendance and feedback", "FR_15", "FR_16");
        manager = !(me instanceof User);
        table.getColumnModel().getColumn(7).setCellRenderer(new Theme.StatusRenderer());
        table.getSelectionModel().addListSelectionListener(e -> showSelected());
        promo.setEditable(false);
        promo.setLineWrap(true);
        promo.setWrapStyleWord(true);
        promo.setFont(Theme.NORMAL);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(Theme.card("Programs", Theme.table(table)), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new GridLayout(1, manager ? 3 : 2, 10, 0));
        bottom.setOpaque(false);
        bottom.setPreferredSize(new Dimension(100, 250));
        bottom.add(Theme.card("Promotional material", new JScrollPane(promo)));
        bottom.add(Theme.card("Feedback", new JScrollPane(new JList<>(feedback))));
        if (manager) bottom.add(Theme.card("Attendance rate per program (%)", attendance));
        center.add(bottom, BorderLayout.SOUTH);
        body().add(center, BorderLayout.CENTER);

        body().add(manager ? managerPanel() : citizenPanel(), BorderLayout.EAST);
    }

    private JPanel managerPanel() {
        JPanel form = new Stack();
        form.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        g.add(event);
        g.add(educational);
        event.setOpaque(false);
        educational.setOpaque(false);
        event.addActionListener(e -> extraLabel.setText("Objective of the event"));
        educational.addActionListener(e -> extraLabel.setText("Target audience"));
        form.add(row(event, educational));
        form.add(muted("Name"));
        form.add(name);
        form.add(muted("Description"));
        form.add(description);
        form.add(muted("Location"));
        form.add(location);
        form.add(row(muted("In how many days"), inDays, muted("Capacity"), capacity));
        form.add(extraLabel);
        form.add(extra);
        form.add(FlatButton.primary("Create with the factory & invite citizens", e -> create()));
        JPanel side = new JPanel(new BorderLayout(0, 10));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(360, 100));
        side.add(Theme.card("New program", form), BorderLayout.NORTH);

        JPanel att = new Stack();
        att.setOpaque(false);
        att.add(muted("Mark who came to the selected program:"));
        att.add(participant);
        att.add(FlatButton.secondary("Mark attended", e -> markAttended()));
        att.add(muted("Insights for next time (FR_15 step 5):"));
        insights.setLineWrap(true);
        att.add(new JScrollPane(insights));
        att.add(FlatButton.secondary("Save insights", e -> {
            Program p = selected();
            if (p != null) { p.setInsights(insights.getText()); info("Saved."); }
        }));
        side.add(Theme.card("Attendance", att), BorderLayout.CENTER);
        return side;
    }

    private JPanel citizenPanel() {
        JPanel p = new Stack();
        p.setOpaque(false);
        p.add(FlatButton.primary("Join selected program", e -> join()));
        p.add(FlatButton.secondary("Cancel my registration", e -> {
            Program pr = selected();
            if (pr != null && pr.removeParticipant((User) me)) { info("You left " + pr + "."); refresh(); }
        }));
        p.add(muted("Feedback for a program you joined:"));
        p.add(row(muted("Rating"), rating));
        p.add(comment);
        p.add(FlatButton.primary("Send feedback", e -> sendFeedback()));
        JPanel side = new JPanel(new BorderLayout());
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(300, 100));
        side.add(Theme.card("What do you want to do?", p), BorderLayout.NORTH);
        return side;
    }

    private Program selected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            error("Select a program in the table first.");
            return null;
        }
        return rows.get(r);
    }

    private void create() {
        try {
            ProgramType t = event.isSelected() ? ProgramType.EVENT : ProgramType.EDUCATIONAL_PROGRAM;
            LocalDateTime when = LocalDateTime.of(java.time.LocalDate.now().plusDays((Integer) inDays.getValue()), LocalTime.of(10, 0));
            Program p = sys.createProgram(t, name.getText(), description.getText(), location.getText(), when,
                    (Integer) capacity.getValue(), extra.getText());
            info("The factory created a " + p.getClass().getSimpleName() + ".\nAll citizens were invited (decorated with an event reminder).");
            name.setText("");
            description.setText("");
            location.setText("");
            extra.setText("");
            refresh();
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void markAttended() {
        Program p = selected();
        if (p == null || participant.getSelectedItem() == null) return;
        if (!p.markAttendance((User) participant.getSelectedItem())) error("Already marked, or this person did not register.");
        refresh();
    }

    private void join() {
        Program p = selected();
        if (p == null) return;
        if (sys.joinProgram((User) me, p)) info("You are registered for " + p + "! A reminder was sent to your notifications.");
        else error(p.isProgramOver() ? "This program is over." : p.isFull() ? "Sorry, it is full." : "You are already registered.");
        refresh();
    }

    private void sendFeedback() {
        Program p = selected();
        if (p == null) return;
        if (!p.getParticipants().contains(me)) {
            error("You can give feedback only for programs you joined.");
            return;
        }
        p.addFeedback((User) me, (Integer) rating.getValue(), comment.getText().isBlank() ? "(no comment)" : comment.getText());
        comment.setText("");
        info("Thank you for your feedback!");
        refresh();
    }

    private void showSelected() {
        int r = table.getSelectedRow();
        if (r < 0 || r >= rows.size()) return;
        Program p = rows.get(r);
        promo.setText(p.generatePromotionalMaterials() + (p.getInsights().isEmpty() ? "" : "\n\nInsights: " + p.getInsights()));
        promo.setCaretPosition(0);
        feedback.clear();
        for (String f : p.getFeedbacks()) feedback.addElement(f);
        if (manager) {
            participant.removeAllItems();
            for (User u : p.getParticipants()) participant.addItem(u);
            insights.setText(p.getInsights());
        }
    }

    @Override
    public void refresh() {
        int sel = table.getSelectedRow();
        rows.clear();
        rows.addAll(sys.getPrograms());
        rows.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        model.setRowCount(0);
        List<String> labels = new ArrayList<>();
        List<Double> rates = new ArrayList<>();
        for (Program p : rows) {
            String extraInfo = p instanceof CommunityEvent ? ((CommunityEvent) p).getObjective() : ((EducationalProgram) p).getTargetAudience();
            String status = p.isProgramOver() ? "DONE" : p.isFull() ? "FULL" : (me instanceof User && p.getParticipants().contains(me)) ? "JOINED - ACTIVE" : "ACTIVE - CAN JOIN";
            model.addRow(new Object[]{p.getName(), p.getType() + " (" + extraInfo + ")",
                    p.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")), p.getLocation(),
                    p.getNrOfParticipants() + " / " + p.getCapacity(), p.getAttended().size(),
                    p.getFeedbacks().isEmpty() ? "-" : String.format("%.1f / 5", p.getAverageRating()), status});
            labels.add(p.getName());
            rates.add(p.getAttendanceRate());
        }
        attendance.setData(labels, rates, java.util.Collections.singletonList(new Color(0x7CB342)), "% who came");
        if (sel < 0 && !rows.isEmpty()) sel = 0;
        if (sel >= 0 && sel < rows.size()) table.setRowSelectionInterval(sel, sel);
    }
}
